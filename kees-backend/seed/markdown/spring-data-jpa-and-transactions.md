# Spring Data JPA and Transaction Boundaries

Spring Data JPA reduces repository boilerplate, while JPA defines how Java
objects are mapped to relational data and tracked inside a persistence context.
These conveniences are easiest to use when their runtime behavior is explicit.

This document focuses on the questions that tend to matter in real code: when
an entity is managed, when SQL is executed, where transactions should begin,
and what happens when database work is combined with object storage or another
external system.

## Entity identity

An entity represents a row with identity over time. Kees uses two identifiers
for entities exposed by the API:

- an internal numeric `id` used by the database and foreign keys;
- a public UUID used at HTTP and integration boundaries.

The numeric key is compact and efficient for joins. The public UUID does not
reveal row counts, is difficult to guess, and prevents database implementation
details from becoming part of the API contract.

```java
@Entity
@Table(name = "knowledge_collection")
public class KnowledgeCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @Column(nullable = false)
    private String name;

    protected KnowledgeCollection() {
        // Required by JPA.
    }
}
```

The no-argument constructor may be protected because application code should
use a meaningful constructor or factory. JPA still needs a no-argument path for
materializing objects loaded from the database.

Entity equality deserves deliberate treatment. Generated database IDs are null
before persistence and assigned later. Including mutable fields in `equals` or
`hashCode` can break collections when those fields change. There is no single
universal recipe; use the identity semantics of the domain and understand the
entity lifecycle before generating these methods automatically.

## Repositories

A Spring Data repository declares persistence operations as an interface:

```java
public interface SourceRepository extends JpaRepository<Source, Long> {

    Optional<Source> findByPublicId(UUID publicId);

    List<Source> findAllByCollection_PublicIdOrderByCreatedAtDesc(
            UUID collectionPublicId
    );
}
```

Spring Data derives a query from supported method-name patterns. This is useful
for concise, straightforward queries. Once a method name becomes hard to read,
an explicit query or purpose-built repository implementation can communicate
the intent better.

Repositories should expose operations the application actually needs. Extending
`JpaRepository` gives many generic methods, but callers do not need to use every
one. In particular, loading by the private numeric ID at the web boundary would
undermine the public-ID design.

An `Optional` return type describes the repository lookup accurately: the row
may not exist. A service can translate absence into the application-specific
exception used by the HTTP error handler.

## The persistence context

Inside a persistence context, JPA tracks managed entities. If a managed
entity's field changes, JPA can detect that change and generate an update when
the context is flushed. This is called dirty checking.

```java
@Transactional
public void markStored(UUID sourceId) {
    var source = repository.findByPublicId(sourceId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Source", sourceId.toString()
            ));

    source.markStored();
}
```

An explicit `save` is not normally required for this already-managed entity.
The transaction commit causes a flush, and the state change is persisted. Some
teams still call `save` for visual clarity, but it is important to know that
dirty checking—not that extra call—is doing the work.

An entity is detached after its persistence context ends. Changing a detached
object does not automatically update the database. Passing entities across
long-running workflows therefore requires care. Often it is clearer to pass a
public ID and reload the current row inside the next transaction.

## Flush is not commit

A flush synchronizes pending persistence-context changes to the database. A
commit makes the transaction durable. Flushing may happen before commit, for
example before a query whose result depends on pending changes.

This distinction explains why a constraint violation may surface earlier than
expected and why observing emitted SQL does not necessarily mean the transaction
committed. A later exception can still roll the transaction back.

Calling `saveAndFlush` should have a specific reason. It is not a more correct
version of `save`; it simply forces synchronization sooner and may reduce the
database's ability to batch work.

## Transaction boundaries belong around use cases

Transactions protect a coherent database operation. In Spring applications,
the service layer is usually a good place to define that boundary:

```java
@Service
@Transactional(readOnly = true)
public class KnowledgeCollectionService {

    private final KnowledgeCollectionRepository repository;

    public KnowledgeCollectionService(
            KnowledgeCollectionRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public KnowledgeCollection create(String name, String description) {
        return repository.save(
                new KnowledgeCollection(name, description)
        );
    }

    public KnowledgeCollection getByPublicId(UUID publicId) {
        return repository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Knowledge Collection", publicId.toString()
                ));
    }
}
```

The class-level read-only transaction supplies a default for queries. The
create method overrides that default with a read-write transaction. Read-only
is a useful declaration of intent and may enable optimizations, but it should
not be treated as an absolute security boundary against writes.

Avoid opening a transaction in the controller. Transport concerns should not
determine how long database locks and persistence contexts remain active.
Keeping the boundary in a service makes the use case reusable from tests,
scheduled jobs, or a future message consumer.

## Transaction proxies

Spring usually implements declarative transactions with a proxy around the
service bean. A call from another object enters the proxy, which starts or joins
a transaction before invoking the real method.

Self-invocation is a common surprise:

```java
public void processAll() {
    processOne(); // A direct call on this instance.
}

@Transactional
public void processOne() {
    // The annotation may not be intercepted through this self-call.
}
```

Moving the transactional operation to a collaborating service, or placing the
transaction around the outer use case, usually leads to a clearer design than
trying to obtain the proxy manually.

Visibility and proxy mechanism also matter. The safest learning default is a
public transactional service method called from another bean. More advanced
arrangements are possible, but should be chosen with an understanding of the
interception model.

## Rollback behavior

By default, runtime exceptions cause a Spring-managed transaction to roll back.
Checked exceptions may require explicit rollback rules. Do not catch an
exception inside a transaction merely to log it and then continue unless the
operation can genuinely succeed. Swallowing the exception can allow partial
state to commit.

If a failure is translated, keep its cause:

```java
try {
    gateway.performOperation();
} catch (GatewayException exception) {
    throw new SourceProcessingException(
            "Could not process source " + sourceId,
            exception
    );
}
```

Exception translation provides domain context while preserving diagnostic
information.

## Database transactions cannot cover object storage

Kees stores the original Markdown in S3-compatible object storage and metadata
in PostgreSQL. A normal database transaction cannot atomically commit both
systems. Holding a database transaction open during an upload does not change
that fact; it merely keeps a database connection and possibly locks occupied
while waiting for the network.

The upload workflow therefore uses explicit states:

```text
create database source as PENDING_UPLOAD
  -> upload object
  -> mark source as STORED
  -> mark source as PROCESSING
  -> retrieve and parse content
  -> replace chunks
  -> mark source as READY
```

If object storage fails, the source can be marked `FAILED`. This is not a single
distributed transaction. It is a state machine designed so failures are
visible and recoverable.

Each state transition can use a short local database transaction. External I/O
happens between those transactions. This has several advantages:

- database transactions remain short;
- failures have a persisted state;
- retries can restart from an understood point;
- monitoring can identify work stuck in a transient state.

## Idempotency and retries

A retry repeats work after a transient failure. Retrying safely requires the
operation to be idempotent, or at least to detect what already happened.

For source processing, useful protections include:

- a stable source public ID and object key;
- an expected current status for each transition;
- replacement of all chunks in one database transaction;
- a content hash for recognizing identical bytes;
- uniqueness constraints where duplicates would violate the model.

Suppose chunk insertion succeeds but the application crashes before marking the
source ready. A retry that blindly appends chunks creates duplicates. A retry
that deletes or replaces the source's chunks atomically can converge on the
same final state.

Not every exception should be retried. Invalid UTF-8, unsupported file content,
or a violated business rule will not improve with time. Network timeouts and
temporarily unavailable storage may be retryable. Retry policies should use a
bounded attempt count and backoff to avoid overwhelming a struggling service.

## Concurrency

Two requests can act on the same row concurrently. Reading a status and then
updating it is not automatically safe because both transactions may observe the
same original value.

Strategies include:

- optimistic locking with a `@Version` field;
- an update query that includes the expected status in its `WHERE` clause;
- pessimistic locking for rare cases requiring serialization;
- a uniqueness constraint that lets the database arbitrate duplicates.

Optimistic locking is often a good fit when conflicts are uncommon. A version
column changes on update. If another transaction has already changed the row,
the stale update affects no current version and JPA reports a conflict rather
than silently overwriting it.

Concurrency control should protect a concrete invariant. Adding locks without
identifying the race makes the system harder to reason about and can reduce
throughput without solving the real problem.

## Schema migrations and entity mappings

JPA mappings describe how code expects the database to look. Flyway migrations
create and evolve the actual schema. With Hibernate schema validation enabled,
startup fails when those two views disagree.

This division is valuable:

- migrations are versioned, reviewable changes;
- production does not depend on Hibernate guessing schema changes;
- constraints and indexes are explicit;
- the entity remains focused on object mapping.

Once a migration has been applied to a shared or persistent database, editing
the old file changes its checksum. The normal approach is a new migration that
alters the schema. Rewriting an unapplied migration can be convenient during
very early local development, but only while every relevant database can safely
be recreated.

## Query behavior and performance

Object navigation can hide database queries. A lazy relationship may trigger a
query when accessed, while an eager relationship can load far more than the use
case requires. Neither option is universally correct.

For API reads, consider selecting a DTO or using an explicit fetch plan rather
than serializing an entity graph. Returning entities directly can accidentally
initialize relationships, expose private columns, or create recursive JSON.

Watch for the N+1 query pattern: one query loads a list of parent rows, followed
by one additional query for each parent's related rows. Integration tests can
verify results, while SQL logging or observability tools reveal query counts.

Indexes should follow actual lookup patterns. A public UUID used on every API
lookup normally needs a unique index. Foreign-key columns used to retrieve all
chunks for a source normally need an index as well. An index speeds reads at
the cost of storage and additional work on writes, so indexing every column is
not a sensible default.

## Practical review questions

When reviewing a persistence use case, ask:

1. What invariant must be atomic inside PostgreSQL?
2. Where does the transaction start and end?
3. Does the transaction include slow network I/O?
4. Which entities are managed at each point?
5. Can the operation be retried without duplicates?
6. What happens if the process stops between two state transitions?
7. Can two workers process the same source?
8. Which database constraint provides the final line of defense?

The goal is not to make failures impossible. Distributed components will fail.
The goal is to leave enough durable information that the application can
recognize the state and move toward a correct outcome.

## Further reading

- [Spring Data JPA reference](https://docs.spring.io/spring-data/jpa/reference/)
- [Spring Data JPA transactionality](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html)
- [Spring Data JPA query methods](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)
- [Spring Framework declarative transactions](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html)

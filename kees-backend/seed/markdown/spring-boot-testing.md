# Testing a Spring Boot Application

A useful test suite provides confidence at several boundaries without making
every test start the entire application. The central choice is not “unit or
integration.” It is selecting the smallest test scope that can prove the
behavior in question.

## Unit tests

A unit test constructs one class directly and supplies test doubles for its
collaborators. Spring does not need to start.

```java
@ExtendWith(MockitoExtension.class)
class SourceProcessingServiceTest {

    @Mock
    private SourceRepository sourceRepository;

    @Mock
    private ObjectStorage objectStorage;

    @InjectMocks
    private SourceProcessingService service;
}
```

Unit tests work well for parsing, chunk boundaries, hashing, state transitions,
and orchestration decisions. They are fast and failures usually point directly
to the broken rule.

Test observable behavior rather than implementation trivia. Verifying that a
mock method was called is useful when the call itself is the contract, such as
ensuring storage cleanup after a failed workflow. It is less useful when it
merely mirrors every line of the implementation and prevents harmless
refactoring.

## MVC slice tests

`@WebMvcTest` starts the MVC portion of the application and selected controller
beans. Collaborating services are normally mocked. This scope can verify:

- request mappings;
- JSON or multipart binding;
- validation responses;
- status codes and headers;
- controller advice and problem responses.

It does not prove that repositories, migrations, or object storage work. That
is intentional: a slice test focuses on the web contract.

## Repository slice tests

`@DataJpaTest` focuses on JPA repositories and entity mappings. It is suitable
for derived queries, explicit JPQL, constraints, and persistence behavior.

An embedded database is convenient but can differ from PostgreSQL in SQL
syntax, data types, indexes, JSON behavior, and constraint handling. When
production uses PostgreSQL, a PostgreSQL Testcontainer gives stronger evidence
for database-specific behavior.

## Full integration tests

`@SpringBootTest` loads the complete application context. With a random web
port, a test can send real HTTP requests through the server:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SourceUploadIntegrationTest {
    // PostgreSQL and S3-compatible containers are wired into the context.
}
```

This is the right scope for the primary upload journey:

```text
multipart HTTP request
  -> source metadata in PostgreSQL
  -> original bytes in object storage
  -> Markdown parsing and chunking
  -> chunk rows in PostgreSQL
  -> READY response
```

Because this test exercises many moving parts, keep its assertions centered on
important outcomes. Detailed parser edge cases belong in faster unit tests.

## Testcontainers and dynamic properties

Testcontainers starts disposable infrastructure for tests. Spring Boot can
create service connections for supported container types, or a test can
register connection properties dynamically.

The test should wait for services to become usable, not merely for a process to
start. Containers should be isolated enough that a developer's local database
contents cannot affect test results.

Use realistic infrastructure where compatibility matters. A fake object
storage implementation may be ideal for a service unit test, while an
S3-compatible container is more valuable in the end-to-end upload test.

## Transaction traps in web integration tests

Adding `@Transactional` to a test method can roll database changes back after a
repository test. In a random-port web test, however, the HTTP request runs on a
server thread with its own transaction. The test method's transaction does not
wrap the server's work.

Cleanup must therefore be explicit or achieved by disposable infrastructure.
Relying on test rollback in that situation can leave rows behind and make tests
order-dependent.

## Arrange, act, assert

The familiar structure remains useful:

```java
@Test
void rejectsBlankDecodedContent() {
    // Arrange
    var file = markdownFile("blank.md", "   \n\t");

    // Act
    var exception = assertThrows(
            InvalidSourceFileException.class,
            () -> service.upload(collectionId, file)
    );

    // Assert
    assertThat(exception.getMessage())
            .isEqualTo("File content cannot be empty");
}
```

Name a test after behavior, not the method implementation. A descriptive name
acts as documentation when the suite fails months later.

## Selecting valuable cases

For Kees, high-value unit cases include:

- headings start meaningful Markdown blocks;
- code fences remain intact even when their content contains heading markers;
- an oversized block splits without losing text;
- blank UTF-8 content is rejected;
- invalid UTF-8 is rejected;
- a storage failure marks a pending source as failed;
- processing replaces chunks in the expected order;
- a processing failure leaves a visible failed state.

High-value integration cases include:

- a valid upload ends as ready and creates ordered chunk rows;
- the stored object contains exactly the uploaded bytes;
- a missing collection returns the expected problem response;
- invalid content creates neither a stored object nor a source row;
- a duplicate or retry follows the deliberately chosen product rule.

## Avoiding brittle tests

A brittle test fails when implementation changes but behavior does not. Common
causes are asserting every mock interaction, depending on generated timestamps,
assuming database row order without `ORDER BY`, or comparing entire error JSON
when only a few fields form the contract.

Inject a `Clock` when time is part of business behavior. Use stable public IDs
in fixtures when the exact identity matters. Compare unordered collections as
such, and assert ordering only where ordering is a requirement.

Tests should also avoid sharing mutable fixtures. A builder or small factory can
make the relevant differences between cases visible while giving each case its
own objects.

## A practical test pyramid

For this project, a sensible balance is:

- many plain unit tests for Markdown parsing and domain logic;
- a smaller number of MVC or repository slice tests where those boundaries
  contain meaningful behavior;
- a few full integration tests for critical journeys such as source upload.

The shape is not a quota. If a repository query is complex, it deserves more
repository tests. If the web layer is only thin mapping, a full upload test may
already provide enough confidence in its wiring.

## Further reading

- [Testing Spring Boot applications](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)
- [Spring Boot test slices](https://docs.spring.io/spring-boot/appendix/test-auto-configuration/slices.html)
- [Spring Boot testing reference](https://docs.spring.io/spring-boot/reference/testing/)

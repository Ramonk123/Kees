# Building REST APIs with Spring MVC

Spring MVC maps HTTP requests to Java methods and converts request and response
bodies with configured message converters. A well-shaped API keeps HTTP details
at the controller boundary while application services express use cases in
domain language.

## A controller method

```java
@RestController
@RequestMapping("/api/collections/{collectionId}/sources")
class SourceUploadController {

    private final SourceUploadService uploadService;

    SourceUploadController(SourceUploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<SourceResponse> upload(
            @PathVariable UUID collectionId,
            @RequestPart("file") MultipartFile file
    ) {
        var source = uploadService.upload(collectionId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SourceResponse.from(source));
    }
}
```

The annotations describe transport concerns: URL, HTTP method, media type, path
parameter, and multipart field. The service receives Java values and owns the
workflow. It should not need to know about response status codes.

## Request and response DTOs

DTOs protect the API from accidental changes to the persistence model. An
entity may contain an internal database ID, storage key, or relationships that
should never appear in JSON. A response record can expose exactly the intended
contract.

```java
public record SourceResponse(
        UUID id,
        String fileName,
        String status
) {
    static SourceResponse from(Source source) {
        return new SourceResponse(
                source.getPublicId(),
                source.getFileName(),
                source.getStatus().name()
        );
    }
}
```

Mapping code may feel repetitive, but it makes the boundary explicit. A mapper
library becomes useful only when repetition is genuinely costly; it should not
hide important transformations.

## Validation has layers

HTTP validation checks whether transport input satisfies the endpoint contract.
Examples include a required JSON field, a syntactically valid UUID, or the
presence of a multipart part.

Domain validation protects rules whenever an object is created, regardless of
whether the caller is HTTP, a test, a scheduled job, or a message consumer.
Database constraints protect data against every writer and race condition.

These layers can overlap without being identical:

```text
HTTP boundary: useful client error
domain boundary: valid object and state transition
database boundary: durable invariant
```

Duplicating a critical rule at more than one layer is not necessarily wasteful.
Each layer serves a different failure mode. Avoid duplicating formatting-only
rules in the domain if they have no business meaning.

## Exception handling

Controllers should not repeat `try/catch` blocks for shared errors. A
`@RestControllerAdvice` can translate application exceptions into consistent
HTTP error responses.

```java
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(
            ResourceNotFoundException exception
    ) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
        problem.setTitle("Resource not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }
}
```

`ProblemDetail` represents the standard problem-details structure supported by
Spring. The handler is a translation layer: the service reports a meaningful
application failure and the web layer chooses its HTTP representation.

Do not expose raw stack traces, SQL messages, bucket names, or internal storage
keys to clients. Log diagnostic details server-side with a request or entity
identifier, then return a stable public message.

## Choosing status codes

Useful conventions include:

- `200 OK` for a successful read or operation returning a result;
- `201 Created` when a new resource is synchronously created;
- `202 Accepted` when work has been accepted but will finish later;
- `204 No Content` for a successful operation with no response body;
- `400 Bad Request` for malformed or invalid request data;
- `404 Not Found` when the requested public resource does not exist;
- `409 Conflict` when the request conflicts with current resource state;
- `413 Content Too Large` when an uploaded body exceeds the accepted limit.

The exact choice should reflect what happened, not merely which Java exception
was thrown. If a future multi-file import becomes asynchronous, returning an
import-job resource with `202 Accepted` will communicate different semantics
from today's synchronous single-file `201 Created` flow.

## Multipart uploads

Multipart requests carry named parts. In Postman, selecting `form-data`, naming
the field `file`, and changing its type from Text to File creates the part that
`@RequestPart("file")` expects.

Upload validation should not trust only the client-provided content type. A
client can omit or forge it. Kees currently applies rules it can verify itself:

- a filename is present and ends in `.md`;
- the declared and actual byte size are within the limit;
- the bytes decode as valid UTF-8;
- the decoded text is not blank;
- the content contains no null characters.

Reading a bounded upload into a byte array is reasonable when the configured
maximum is small. It simplifies hashing, validation, and retry-free transfer to
object storage. For very large uploads, a streaming design would be more memory
efficient but would need deliberate handling for size enforcement, hashing,
decoding, and cleanup after partial failures.

## Avoid leaking entities through JSON

Returning a JPA entity directly creates several risks:

- lazy relationships may trigger queries during serialization;
- bidirectional relationships can recurse;
- internal fields become part of the public contract;
- changing a database mapping can unexpectedly change the API;
- persistence annotations and JSON annotations become tangled.

A DTO costs a small amount of mapping code and gives the API an independent,
reviewable shape.

## Controller tests and integration tests

A focused MVC test can verify routing, serialization, validation, and exception
translation without starting the complete application. A full integration test
is better when the goal is to prove that HTTP, PostgreSQL, migrations, object
storage, parsing, and persistence work together.

Neither replaces a service unit test. Each test level answers a different
question, and using the smallest level that proves the behavior keeps feedback
fast.

## Further reading

- [Spring MVC annotated controllers](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html)
- [Controller advice](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html)
- [Spring MVC error responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [Spring MVC validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)

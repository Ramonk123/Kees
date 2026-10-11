# Spring Boot Fundamentals

Spring Boot is an opinionated layer around the Spring Framework. Spring itself
provides the application container, dependency injection, web framework,
transaction support, and many other building blocks. Spring Boot makes those
parts easier to assemble by choosing sensible defaults, configuring components
based on the classpath, and providing a conventional way to run and operate an
application.

The important mental model is that Spring Boot does not replace Spring. A Boot
application is still a Spring application. Objects managed by the container are
still beans, dependencies are still injected, and framework behavior still
comes from configuration. Boot primarily reduces the amount of configuration
that must be written by hand.

## Starting an application

A typical entry point looks like this:

```java
package dev.ramon.kees;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KeesBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeesBackendApplication.class, args);
    }
}
```

`SpringApplication.run` creates and refreshes the application context. During
that process Spring discovers bean definitions, resolves dependencies, applies
configuration, runs bean lifecycle callbacks, and starts the embedded web
server when this is a web application.

`@SpringBootApplication` is a convenience annotation. Conceptually, it enables
three things:

- configuration classes and bean definitions;
- component scanning starting at the package of the application class;
- Spring Boot auto-configuration.

This explains why the main application class normally lives in a root package
such as `dev.ramon.kees`. Subpackages such as `source`, `collection`, and
`storage` then fall within the default component scan. Putting the application
class deep inside one feature package can cause beans elsewhere to be missed.

## Beans and dependency injection

A bean is an object whose creation and lifecycle are managed by the Spring
container. A class annotated with `@Service`, `@Repository`, or `@Controller`
is discovered by component scanning. A method annotated with `@Bean` can also
construct an object explicitly.

```java
@Configuration
class ClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
```

Constructor injection makes dependencies explicit:

```java
@Service
class SourceProcessingService {

    private final Clock clock;
    private final ChunkRepository chunkRepository;

    SourceProcessingService(Clock clock, ChunkRepository chunkRepository) {
        this.clock = clock;
        this.chunkRepository = chunkRepository;
    }
}
```

This style has practical benefits. Required dependencies cannot be forgotten,
fields can remain final, and a unit test can instantiate the class directly.
Field injection hides dependencies and makes construction outside the Spring
container unnecessarily difficult.

Spring resolves a constructor parameter primarily by its type. If exactly one
bean implements the requested type, injection is straightforward. If multiple
implementations exist, the application must express which one is primary or
use a qualifier. Depending on an interface lets infrastructure implementations
be replaced without changing the consuming service.

## Stereotype annotations

The common stereotype annotations all make a class eligible for component
scanning, but they communicate different intent:

- `@Controller` handles server-rendered web requests.
- `@RestController` handles HTTP requests and writes response bodies.
- `@Service` represents application or domain orchestration.
- `@Repository` represents persistence access and participates in persistence
  exception translation.
- `@Component` is the general-purpose option when no more specific stereotype
  fits.

The annotations do not automatically create a good architecture. A controller
can still contain too much business logic, and a service can still mix
unrelated concerns. Package structure and class responsibilities remain design
decisions made by the developer.

## Auto-configuration

Auto-configuration reacts to what is available in the application. When MVC
classes are on the classpath, Boot can configure the MVC infrastructure. When a
supported JDBC driver and datasource settings exist, Boot can configure a
datasource. An auto-configuration usually contains conditions such as “a class
is present,” “a property is enabled,” or “the application has not defined its
own bean.”

That last condition is important. Boot commonly backs away when the application
provides a custom bean. This is how defaults remain convenient without making
customization impossible.

Auto-configuration is not magic, even when it feels invisible. If the context
fails to start, useful questions include:

1. Which bean could not be created?
2. Which dependency was missing or ambiguous?
3. Which condition caused an auto-configuration to apply?
4. Did the application define a bean that replaced a default?
5. Is a required property absent or misspelled?

The condition evaluation report, available through debug logging, can explain
why particular auto-configurations matched or did not match.

## Configuration properties

External configuration separates deploy-time values from compiled code. A
database URL, upload limit, or storage bucket name belongs in configuration;
the rules that interpret those values belong in Java.

For a related group of settings, prefer a typed configuration properties
object over several unrelated `@Value` fields:

```java
@ConfigurationProperties(prefix = "kees.chunking")
public record ChunkingProperties(int maxCharacters) {

    public ChunkingProperties {
        if (maxCharacters < 100) {
            throw new IllegalArgumentException(
                    "maxCharacters must be at least 100"
            );
        }
    }
}
```

The matching property could be written as:

```properties
kees.chunking.max-characters=2000
```

Typed properties provide conversion, discoverability, and a natural place to
validate configuration at startup. Failing at startup is normally better than
discovering invalid infrastructure settings halfway through a user request.

Environment variables are useful for secrets and deployment-specific values.
Property placeholders can retain a local default:

```properties
kees.storage.endpoint=${KEES_STORAGE_ENDPOINT:http://localhost:9000}
```

The application sees one logical property. Locally it uses the fallback; in a
deployed environment the environment variable can override it.

## Profiles

Profiles activate beans or configuration for a particular environment. They
are useful when behavior genuinely differs, such as a local-only adapter or a
production monitoring integration. They should not become a substitute for
ordinary configuration.

A common warning sign is business logic that asks whether the active profile
is `prod`. Business rules should usually be stable across environments. The
infrastructure supplied to those rules may differ, but the domain behavior
should remain testable and predictable.

## Bean lifecycle and startup work

Spring creates singleton beans during context startup by default. Constructors
should establish a valid object, not perform slow external work. Network calls
or database mutations in constructors make startup ordering fragile and tests
harder to control.

Spring offers lifecycle hooks and application runners, but startup work should
still be treated cautiously. Ask whether the work is:

- required for every application instance;
- safe to run more than once;
- safe when several instances start simultaneously;
- observable when it fails;
- better represented as a migration or background job.

This is why Kees keeps example Markdown files passive. Manually uploading them
through the HTTP endpoint exercises the real use case without quietly mutating
data every time the application starts.

## Proxies and framework behavior

Several Spring features are applied through proxies. Transactions, method
security, caching, and asynchronous execution often work by wrapping a bean and
intercepting calls made through that wrapper. This affects how annotations
behave.

For example, calling a `@Transactional` public method from another bean normally
passes through the transactional proxy. Calling that method from another
method on the same instance may bypass the proxy. The Java call succeeds, but
the expected framework behavior might not be applied.

The broader lesson is to understand whether an annotation is merely metadata or
whether infrastructure interprets it at runtime. When surprising behavior
appears, identify the mechanism behind the annotation instead of adding more
annotations experimentally.

## Boundaries in a feature package

Kees uses feature-oriented packages. A source feature can contain its
controller, service, entity, repository, and DTOs. This keeps code that changes
together near each other and avoids large technical-layer packages containing
every controller or every service in the application.

A request generally travels through these responsibilities:

```text
HTTP request
  -> controller: translate transport input
  -> application service: coordinate the use case
  -> domain objects: protect important state and rules
  -> repositories and gateways: persist or call infrastructure
  -> controller: translate the result to an HTTP response
```

Not every application needs a separate class for every arrow. The value comes
from keeping decisions in the right place, not from maximizing the number of
layers.

## Practical debugging checklist

When an application context does not start, read the exception from the bottom
up. The large top-level message often says only that context creation failed.
The deepest relevant cause usually names the missing bean, invalid property,
database error, or migration problem.

When a request fails after startup:

1. Verify the request reached the expected controller mapping.
2. Check whether request binding and validation completed.
3. Follow the service boundary and inspect the first changed state.
4. Identify the active transaction boundary.
5. Look for an external call that can fail independently.
6. Confirm the database and object storage reflect a valid final state.

Logs are most useful when they carry stable identifiers such as a source public
UUID. Avoid logging complete uploaded documents, secrets, or authorization
headers.

## Further reading

- [Using the `@SpringBootApplication` annotation](https://docs.spring.io/spring-boot/reference/using/using-the-springbootapplication-annotation.html)
- [Spring Boot auto-configuration](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html)
- [Spring Boot externalized configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [Spring Framework container overview](https://docs.spring.io/spring-framework/reference/core/beans/basics.html)

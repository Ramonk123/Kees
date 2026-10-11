# Configuration and Profiles in Spring Boot

Configuration lets the same application artifact run in different environments
without recompilation. The code defines which settings exist and what they
mean; property files, environment variables, command-line arguments, and other
property sources provide their values.

## Prefer cohesive typed properties

When several values configure one concern, model them together:

```java
@ConfigurationProperties(prefix = "kees.storage")
public record StorageProperties(
        String bucket,
        URI endpoint,
        String region,
        String accessKey,
        String secretKey,
        boolean pathStyle
) {}
```

The prefix keeps configuration names organized:

```properties
kees.storage.bucket=${KEES_STORAGE_BUCKET:kees-sources}
kees.storage.endpoint=${KEES_STORAGE_ENDPOINT:http://localhost:9000}
kees.storage.region=${KEES_STORAGE_REGION:eu-west-1}
```

This makes dependencies clearer than scattering string expressions across
services. The storage client can depend on `StorageProperties`, while domain
services depend on an `ObjectStorage` interface instead of deployment details.

## Validate configuration early

Configuration errors should preferably fail application startup with a precise
message. A negative upload size or missing bucket cannot support a valid
request, so postponing discovery until the first user action adds no value.

Validation can live in a compact record constructor or use Bean Validation
constraints when the properties type is configured for validation. Choose the
approach that produces a clear rule and readable failure.

Secrets should not have realistic defaults committed to source control for
deployed environments. Local development credentials for disposable services
are different from production credentials, but should still be clearly marked
as local.

## Property precedence

Spring Boot can read multiple property sources. Higher-precedence sources can
override lower-precedence defaults. This enables a base `application.properties`
file to describe local conventions while environment variables or deployment
configuration supply environment-specific values.

Override flexibility can also make debugging confusing. When a value is
unexpected, identify both the resolved value and the source that supplied it.
Do not assume the checked-in file won simply because it contains the property.

Use one canonical name per setting. Creating aliases such as both
`MAX_UPLOAD_SIZE` and `KEES_MAX_FILE_SIZE` for the same concern makes precedence
and support harder to understand.

## Profiles select groups of configuration

A profile can activate an additional properties file or conditional bean. This
is appropriate when a coherent environment needs a distinct adapter or group
of settings.

Examples might include:

- a local development adapter for a service unavailable on a laptop;
- production-only metrics export;
- a test configuration that replaces an external gateway.

Profiles are less appropriate for individual feature flags or domain rules.
Those concerns deserve explicit properties or application data. A growing set
of overlapping profiles such as `dev-with-seed`, `dev-no-storage`, and
`production-special-client` quickly makes the effective configuration hard to
predict.

## Configuration classes

Use a configuration class when constructing a third-party client or when bean
creation requires decisions that do not belong in the consuming service:

```java
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
class StorageConfiguration {

    @Bean
    S3Client s3Client(StorageProperties properties) {
        return S3Client.builder()
                .endpointOverride(properties.endpoint())
                .region(Region.of(properties.region()))
                .forcePathStyle(properties.pathStyle())
                .build();
    }
}
```

The exact credentials configuration is omitted here, but the responsibility is
clear: this class translates application configuration into an infrastructure
client. The upload service should not build SDK clients itself.

## Operational guidelines

- Keep secret values out of logs and exception messages.
- Document required properties and useful local defaults.
- Prefer startup validation for settings required by every request.
- Keep domain behavior consistent across environments.
- Use explicit units for durations and data sizes.
- Test custom property binding when conversion or validation is non-trivial.
- Treat configuration names as a public operational interface; rename them
  deliberately.

## Further reading

- [Spring Boot externalized configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [Type-safe configuration properties](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties)
- [Spring Boot profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html)

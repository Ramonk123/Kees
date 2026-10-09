package dev.ramon.kees.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@ConfigurationProperties(prefix = "kees.storage")
public record StorageProperties(
        @NotBlank String bucket,
        @NotBlank String region,
        @NotNull URI endpoint,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        boolean pathStyle
) {
}

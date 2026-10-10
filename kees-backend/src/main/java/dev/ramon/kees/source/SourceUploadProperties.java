package dev.ramon.kees.source;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kees.source-upload")
public record SourceUploadProperties(
        @NotNull DataSize maxFileSize
) {
    public long maxFileSizeInBytes() {
        return maxFileSize.toBytes();
    }
}

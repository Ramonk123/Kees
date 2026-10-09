package dev.ramon.kees.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class S3ObjectStorage implements ObjectStorage{

    private final S3Client client;
    private final StorageProperties properties;


    @Override
    public void store(String key, InputStream content, long contentLength, String contentType) {
        var request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        try {
            client.putObject(
                    request,
                    RequestBody.fromInputStream(content, contentLength)
            );
        } catch (SdkException exception) {
            throw new UploadFailedException(key, exception);
        }
    }

    @Override
    public StoredObject retrieve(String key) {
        var request = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .build();

        try {
            var stream = client.getObject(request);
            var response = stream.response();

            var contentType = response.contentType() != null
                    ? response.contentType()
                    : "application/octet-stream";

            return new StoredObject(
                    stream,
                    response.contentLength(),
                    contentType
            );
        } catch (SdkException exception) {
            throw new RetrieveObjectFailedException(key, exception);
        }
    }

    @Override
    public void delete(String key) {

    }
}

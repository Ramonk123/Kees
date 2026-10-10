package dev.ramon.kees.source;

import dev.ramon.kees.collection.api.CreateKnowledgeCollectionRequest;
import dev.ramon.kees.collection.api.KnowledgeCollectionResponse;
import dev.ramon.kees.source.api.SourceResponse;
import dev.ramon.kees.storage.ObjectStorage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public class SourceUploadIntegrationTest {

    private static final String STORAGE_ACCESS_KEY = "test-key";
    private static final String STORAGE_SECRET_KEY = "test-secret";
    private static final String STORAGE_BUCKET_NAME = "test-bucket";

    @Autowired
    private S3Client s3Client;

    @Autowired
    private SourceRepository repository;

    @Autowired
    private ObjectStorage storage;
    @Autowired
    private RestTestClient restTestClient;
    @Autowired
    private SourcePersistenceService sourcePersistenceService;

    @BeforeAll
    void createBucket() {
        s3Client.createBucket(b -> b.bucket(STORAGE_BUCKET_NAME));
    }

    @Test
    void uploadsMarkdownAndSuccessfullyPersists() throws Exception {
        var content = "Hello world!".getBytes(StandardCharsets.UTF_8);

        var collection = createCollection();

        var multipart = new MultipartBodyBuilder();

        multipart.part(
                "file",
                new ByteArrayResource(content) {
                    @Override
                    public String getFilename() {
                        return "spring-transactions.md";
                    }
                }
        ).contentType(MediaType.TEXT_PLAIN);

        var source = restTestClient
                .post()
                .uri(
                        "/api/collections/{collectionId}/sources",
                        collection.publicId()
                )
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipart.build())
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(SourceResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(source).isNotNull();
        assertThat(source.status()).isEqualTo(SourceStatus.STORED);

        var persistedSource = repository.findByPublicId(source.publicId())
                .orElseThrow();

        try (var storedObject = storage.retrieve(persistedSource.getObjectKey())) {
            assertThat(storedObject.content().readAllBytes()).containsExactly(content);
            assertThat(storedObject.contentType()).isEqualTo("text/markdown; charset=utf-8");
            assertThat(storedObject.contentLength()).isEqualTo(content.length);
        }
    }

    private KnowledgeCollectionResponse createCollection() {
        var request = new CreateKnowledgeCollectionRequest("Test collection", "Test description");

        var collection = restTestClient
                .post()
                .uri("/api/collections")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(KnowledgeCollectionResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(collection).isNotNull();

        return collection;
    }

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18.6"))
                    .withDatabaseName("kees_test")
                    .withUsername("kees")
                    .withPassword("kees");

    @Container
    static final GenericContainer<?> STORAGE =
            new GenericContainer<>(
                    DockerImageName.parse(
                            "docker.io/pgsty/silo:RELEASE.2026-09-16T00-00-00Z"
                    )
            ).withEnv("MINIO_ROOT_USER", STORAGE_ACCESS_KEY)
                    .withEnv("MINIO_ROOT_PASSWORD", STORAGE_SECRET_KEY)
                    .withCommand("server", "/data", "--console-address", ":9001")
                    .withExposedPorts(9000)
                    .waitingFor(
                            Wait.forHttp("/minio/health/ready")
                                    .forPort(9000)
                                    .forStatusCode(200)
                    );

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        if (!STORAGE.isRunning()) {
            STORAGE.start();
        }

        var endpoint = "http://%s:%d".formatted(
                STORAGE.getHost(),
                STORAGE.getMappedPort(9000)
        );

        registry.add(
                "kees.storage.endpoint",
                () -> endpoint
        );

        registry.add(
                "kees.storage.access-key",
                () -> STORAGE_ACCESS_KEY
        );

        registry.add(
                "kees.storage.secret-key",
                () -> STORAGE_SECRET_KEY
        );

        registry.add(
                "kees.storage.bucket",
                () -> STORAGE_BUCKET_NAME
        );

        registry.add(
                "kees.storage.region",
                () -> "eu-west-1"
        );

        registry.add(
                "kees.storage.path-style",
                () -> true
        );
    }
}

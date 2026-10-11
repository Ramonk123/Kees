package dev.ramon.kees.source;

import dev.ramon.kees.collection.KnowledgeCollectionService;
import dev.ramon.kees.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SourcePersistenceService {

    private final SourceRepository sourceRepository;
    private final KnowledgeCollectionService collectionService;

    @Transactional
    public Source createPending(
            UUID sourcePublicId,
            UUID collectionId,
            String fileName,
            String objectKey,
            long size,
            String sha256) {

        var collection = collectionService.getByPublicId(collectionId);

        var source = new Source(
                sourcePublicId,
                collection,
                fileName,
                objectKey,
                SourceContentType.MARKDOWN,
                size,
                sha256);

        return sourceRepository.save(source);
    }

    @Transactional
    public Source markStored(UUID publicId) {

        var source = getByPublicId(publicId);

        source.markStored();

        return source;
    }

    @Transactional
    public Source markFailed(UUID publicId) {
        var source = getByPublicId(publicId);

        source.markFailed();

        return source;
    }

    @Transactional
    public Source markProcessing(UUID publicId) {
        var source = getByPublicId(publicId);

        source.markProcessing();

        return source;
    }

    @Transactional
    public Source markReady(UUID publicId) {
        var source = getByPublicId(publicId);

        source.markReady();

        return source;
    }

    @Transactional
    public Source markProcessingFailed(UUID publicId) {
        var source = getByPublicId(publicId);

        source.markProcessingFailed();

        return source;
    }

    private Source getByPublicId(UUID publicId) {
        return sourceRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("source", publicId.toString()));
    }


}

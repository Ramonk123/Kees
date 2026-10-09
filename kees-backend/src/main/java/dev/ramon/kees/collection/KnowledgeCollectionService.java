package dev.ramon.kees.collection;

import dev.ramon.kees.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KnowledgeCollectionService {

    private final KnowledgeCollectionRepository repository;

    @Transactional
    public KnowledgeCollection create(String name, String description) {

        var collection = new KnowledgeCollection(name, description);

        return repository.save(collection);
    }

    public KnowledgeCollection getByPublicId(UUID publicId) {
        return repository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Knowledge Collection", publicId.toString()
                ));
    }

    public List<KnowledgeCollection> getAll() {
        return repository.findAll();
    }

}

package dev.ramon.kees.collection;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


}

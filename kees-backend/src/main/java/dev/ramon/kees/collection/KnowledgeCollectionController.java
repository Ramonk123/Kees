package dev.ramon.kees.collection;

import dev.ramon.kees.collection.api.CreateKnowledgeCollectionRequest;
import dev.ramon.kees.collection.api.KnowledgeCollectionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
public class KnowledgeCollectionController {

    private final KnowledgeCollectionService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KnowledgeCollectionResponse create(
            @Valid @RequestBody CreateKnowledgeCollectionRequest request) {

        var collection = service.create(request.name(), request.description());

        return KnowledgeCollectionResponse.from(collection);
    }

    @GetMapping
    public List<KnowledgeCollectionResponse> getCollections() {
        return service.getAll()
                .stream()
                .map(KnowledgeCollectionResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public KnowledgeCollectionResponse getCollection(@PathVariable UUID id) {

        var collection = service.getByPublicId(id);

        return KnowledgeCollectionResponse.from(collection);
    }
}

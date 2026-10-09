package dev.ramon.kees.collection;

import dev.ramon.kees.collection.api.CreateKnowledgeCollectionRequest;
import dev.ramon.kees.collection.api.CreateKnowledgeCollectionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
public class KnowledgeCollectionController {

    private final KnowledgeCollectionService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateKnowledgeCollectionResponse create(
            @Valid @RequestBody CreateKnowledgeCollectionRequest request) {

        var collection = service.create(request.name(), request.description());

        return CreateKnowledgeCollectionResponse.from(collection);
    }
}

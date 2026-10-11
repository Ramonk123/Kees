package dev.ramon.kees.chunk;

import dev.ramon.kees.chunk.api.ChunkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sources/{sourceId}/chunks")
@RequiredArgsConstructor
public class ChunkController {
    private final ChunkService service;


    @GetMapping()
    public List<ChunkResponse> getChunksFromSource(@PathVariable UUID sourceId) {
        return service.getChunksFromSource(sourceId)
                .stream()
                .map(ChunkResponse::from)
                .toList();
    }
}

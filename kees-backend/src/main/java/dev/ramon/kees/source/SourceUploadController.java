package dev.ramon.kees.source;

import dev.ramon.kees.source.api.SourceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/collections/{collectionId}/sources")
@RequiredArgsConstructor
public class SourceUploadController {

    private final SourceUploadService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SourceResponse upload(
            @PathVariable UUID collectionId,
            @RequestParam("file") MultipartFile file
    ) {
        var source = service.upload(collectionId, file);
        return SourceResponse.from(source);
    }
}
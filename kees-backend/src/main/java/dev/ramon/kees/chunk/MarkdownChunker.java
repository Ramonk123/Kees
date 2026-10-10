package dev.ramon.kees.chunk;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class MarkdownChunker {

    public List<ChunkDraft> chunk(String markdown) {
        var normalizedInput = normalizeInput(markdown);
        var headingLevels = new String[6];
        var currentHeadingPath = "";

        for (var line : normalizedInput.split("\n", -1)) {
            var heading = parseHeading(line);
            if (heading.isPresent()) {
                currentHeadingPath = updateHeadingPath(headingLevels, heading.get());
            }
        }

        return List.of();
    }

    private String normalizeInput(String markdown) {
        Objects.requireNonNull(markdown, "Markdown cannot be null");

        return markdown
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }

    private Optional<Heading> parseHeading(String line) {
        var level = 0;

        while (level < line.length() && line.charAt(level) == '#') {
            level++;
        }

        if (level == 0 || level > 6) {
            return Optional.empty();
        }

        if (level == line.length() || !Character.isWhitespace(line.charAt(level))) {
            return Optional.empty();
        }

        var title = line.substring(level).strip();
        if (title.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new Heading(level, title));
    }

    private String updateHeadingPath(String[] headingLevels, Heading heading) {
        var headingIndex = heading.level() - 1;

        headingLevels[headingIndex] = heading.title();
        Arrays.fill(headingLevels, headingIndex + 1, headingLevels.length, null);

        return Arrays.stream(headingLevels)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" > "));
    }

    private record Heading(int level, String title) {}
}

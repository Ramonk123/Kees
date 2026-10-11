package dev.ramon.kees.chunk;

import dev.ramon.kees.shared.util.HashContent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class MarkdownChunker {

    private final int maxCharacters;

    public MarkdownChunker(int maxCharacters) {
        if (maxCharacters <= 0) {
            throw new IllegalArgumentException(
                    "Maximum characters must be greater than zero"
            );
        }

        this.maxCharacters = maxCharacters;
    }

    public List<ChunkDraft> chunk(String markdown) {
        var normalizedInput = normalizeInput(markdown);
        var blocks = parseBlocks(normalizedInput);
        var chunkContents = groupBlocks(blocks);

        return toDrafts(chunkContents);
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

    private List<MarkdownBlock> parseBlocks(String input) {
        var blocks = new ArrayList<MarkdownBlock>();
        var headingLevels = new String[6];
        var currentBlock = new StringBuilder();
        String currentHeadingPath = null;
        CodeFence activeFence = null;

        for (var line : input.split("\n", -1)) {
            if (activeFence != null) {
                appendLine(currentBlock, line);

                if (closesFence(line, activeFence)) {
                    finishBlock(blocks, currentBlock, currentHeadingPath);
                    activeFence = null;
                }

                continue;
            }

            var openingFence = detectOpeningFence(line);
            if (openingFence.isPresent()) {
                finishBlock(blocks, currentBlock, currentHeadingPath);
                activeFence = openingFence.get();
                appendLine(currentBlock, line);
                continue;
            }

            var heading = parseHeading(line);

            if (heading.isPresent()) {
                finishBlock(blocks, currentBlock, currentHeadingPath);
                currentHeadingPath = updateHeadingPath(headingLevels, heading.get());
                continue;
            }

            if (line.isBlank()) {
                finishBlock(blocks, currentBlock, currentHeadingPath);
                continue;
            }

            appendLine(currentBlock, line);
        }

        finishBlock(blocks, currentBlock, currentHeadingPath);
        return blocks;
    }

    private Optional<CodeFence> detectOpeningFence(String line) {
        var strippedLine = line.stripLeading();
        if (strippedLine.isEmpty()) {
            return Optional.empty();
        }

        var marker = strippedLine.charAt(0);
        if (marker != '`' && marker != '~') {
            return Optional.empty();
        }

        var length = 0;
        while (length < strippedLine.length() && strippedLine.charAt(length) == marker) {
            length++;
        }

        return length >= 3
                ? Optional.of(new CodeFence(marker, length))
                : Optional.empty();
    }

    private boolean closesFence(String line, CodeFence activeFence) {
        var strippedLine = line.strip();
        if (strippedLine.length() < activeFence.length()) {
            return false;
        }

        for (var index = 0; index < strippedLine.length(); index++) {
            if (strippedLine.charAt(index) != activeFence.marker()) {
                return false;
            }
        }

        return true;
    }

    private void appendLine(StringBuilder currentBlock, String line) {
        if (!currentBlock.isEmpty()) {
            currentBlock.append('\n');
        }

        currentBlock.append(line);
    }

    private void finishBlock(
            List<MarkdownBlock> blocks,
            StringBuilder currentBlock,
            @Nullable String headingPath
    ) {
        if (currentBlock.isEmpty()) {
            return;
        }

        blocks.add(new MarkdownBlock(currentBlock.toString(), headingPath));
        currentBlock.setLength(0);
    }

    private List<ChunkContent> groupBlocks(List<MarkdownBlock> blocks) {
        var chunks = new ArrayList<ChunkContent>();
        var currentContent = new StringBuilder();
        String currentHeadingPath = null;

        for (var block : blocks) {
            if (currentContent.isEmpty()) {
                currentHeadingPath = block.headingPath();
                currentContent.append(block.content());
                continue;
            }

            var headingChanged = !Objects.equals(
                    currentHeadingPath,
                    block.headingPath()
            );
            var combinedLength = currentContent.length()
                    + 2
                    + block.content().length();

            if (headingChanged || combinedLength > maxCharacters) {
                finishChunk(chunks, currentContent, currentHeadingPath);
                currentHeadingPath = block.headingPath();
                currentContent.append(block.content());
                continue;
            }

            currentContent.append("\n\n").append(block.content());
        }

        finishChunk(chunks, currentContent, currentHeadingPath);
        return chunks;
    }

    private void finishChunk(
            List<ChunkContent> chunks,
            StringBuilder currentContent,
            @Nullable String headingPath
    ) {
        if (currentContent.isEmpty()) {
            return;
        }

        chunks.add(new ChunkContent(currentContent.toString(), headingPath));
        currentContent.setLength(0);
    }

    private List<ChunkDraft> toDrafts(List<ChunkContent> chunks) {
        var drafts = new ArrayList<ChunkDraft>(chunks.size());

        for (var sequenceNumber = 0; sequenceNumber < chunks.size(); sequenceNumber++) {
            var chunk = chunks.get(sequenceNumber);
            drafts.add(new ChunkDraft(
                    sequenceNumber,
                    chunk.content(),
                    chunk.headingPath(),
                    HashContent.hash(chunk.content())
            ));
        }

        return List.copyOf(drafts);
    }

    private record Heading(int level, String title) {}

    private record CodeFence(char marker, int length) {}

    private record MarkdownBlock(String content, @Nullable String headingPath) {}

    private record ChunkContent(String content, @Nullable String headingPath) {}
}

package dev.ramon.kees.chunk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkdownChunkerTest {

    @Test
    void rejectsNonPositiveMaximumSize() {
        assertThatThrownBy(() -> new MarkdownChunker(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Maximum characters must be greater than zero");
    }

    @Test
    void returnsNoChunksForBlankMarkdown() {
        var chunker = new MarkdownChunker(2_000);

        assertThat(chunker.chunk(" \n\t\n"))
                .isEmpty();
    }

    @Test
    void createsPersistenceReadyDraft() {
        var chunker = new MarkdownChunker(2_000);

        var chunks = chunker.chunk("# Greeting\n\nhello world");

        assertThat(chunks).containsExactly(
                new ChunkDraft(
                        0,
                        "hello world",
                        "Greeting",
                        "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"
                )
        );
    }

    @Test
    void createsAChunkForEachHeadingPath() {
        var chunker = new MarkdownChunker(2_000);
        var markdown = """
                # Java
                Introduction
                ## Collections
                Collection types
                ### Lists
                List types
                ## Concurrency
                Threading
                """;

        var chunks = chunker.chunk(markdown);

        assertThat(chunks)
                .extracting(ChunkDraft::headingPath)
                .containsExactly(
                        "Java",
                        "Java > Collections",
                        "Java > Collections > Lists",
                        "Java > Concurrency"
                );
        assertThat(chunks)
                .extracting(ChunkDraft::sequenceNumber)
                .containsExactly(0, 1, 2, 3);
    }

    @Test
    void includesParagraphSeparatorInMaximumSize() {
        var markdown = "1234\n\n5678";

        assertThat(new MarkdownChunker(10).chunk(markdown))
                .singleElement()
                .extracting(ChunkDraft::content)
                .isEqualTo("1234\n\n5678");

        assertThat(new MarkdownChunker(9).chunk(markdown))
                .extracting(ChunkDraft::content)
                .containsExactly("1234", "5678");
    }

    @ParameterizedTest
    @ValueSource(strings = {"```", "~~~"})
    void preservesFencedCodeAsOneBlock(String marker) {
        var chunker = new MarkdownChunker(5);
        var codeBlock = marker + "java\n# not a heading\n\nreturn;\n" + marker;
        var markdown = "# Java\n\n" + codeBlock;

        var chunks = chunker.chunk(markdown);

        assertThat(chunks).singleElement().satisfies(chunk -> {
            assertThat(chunk.content()).isEqualTo(codeBlock);
            assertThat(chunk.headingPath()).isEqualTo("Java");
        });
    }

    @Test
    void preservesAnUnclosedCodeFence() {
        var chunker = new MarkdownChunker(5);
        var codeBlock = "```java\n# still code\n\nreturn;";

        var chunks = chunker.chunk(codeBlock);

        assertThat(chunks).singleElement().satisfies(chunk -> {
            assertThat(chunk.content()).isEqualTo(codeBlock);
            assertThat(chunk.headingPath()).isNull();
        });
    }

    @Test
    void normalizesLineEndingsBeforeChunkingAndHashing() {
        var chunker = new MarkdownChunker(2_000);

        var chunks = chunker.chunk("first\r\nsecond\rthird");

        assertThat(chunks)
                .singleElement()
                .extracting(ChunkDraft::content)
                .isEqualTo("first\nsecond\nthird");
    }

    @Test
    void keepsASingleOversizedBlockIntact() {
        var chunker = new MarkdownChunker(5);

        var chunks = chunker.chunk("long paragraph");

        assertThat(chunks)
                .singleElement()
                .extracting(ChunkDraft::content)
                .isEqualTo("long paragraph");
    }
}

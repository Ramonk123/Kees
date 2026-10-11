package dev.ramon.kees.chunk;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ChunkingProperties.class)
public class ChunkingConfiguration {

    @Bean
    MarkdownChunker markdownChunker(ChunkingProperties properties) {}
}

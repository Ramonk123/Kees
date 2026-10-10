package dev.ramon.kees.source;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SourceUploadProperties.class)
public class SourceUploadConfiguration {}

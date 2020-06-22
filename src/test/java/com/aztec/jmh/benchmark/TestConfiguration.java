package com.aztec.jmh.benchmark;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * This is automatically picked up during the @SpringBootTest component scan.
 */
@Configuration
@EnableAutoConfiguration
@EntityScan("com.aztec.jmh.domain")
@EnableJpaRepositories("com.aztec.jmh.repository")
public class TestConfiguration {
}

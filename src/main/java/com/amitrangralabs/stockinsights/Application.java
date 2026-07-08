package com.amitrangralabs.stockinsights;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry point.
 *
 * <p>This is the only class that relies on Spring Boot's component scan. Because no other class in
 * the tree carries a stereotype annotation ({@code @Component}/{@code @Service}/{@code @Repository}/
 * {@code @Controller}), the scan only ever discovers the three explicit {@code @Configuration}
 * classes: {@code DomainConfig}, {@code InboundConfig}, and {@code OutboundConfig}. The entire bean
 * graph is therefore visible by reading those three files — no annotation-driven magic.
 */
@SpringBootApplication
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

package com.amitrangralabs.stockinsights.adapter.in.endpoint;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Health-check endpoint used to verify the app is up.
 *
 * <p>Deliberately a plain class with <strong>no</strong> {@code @Controller} stereotype. It is
 * registered as a bean by hand in {@code InboundConfig}. The type-level {@code @RequestMapping} is
 * enough for Spring MVC's default {@code RequestMappingHandlerMapping} to recognise it as a handler
 * (it treats any bean whose type carries {@code @RequestMapping} as a handler, {@code @Controller}
 * or not). This class is the M0 proof that the "explicit wiring, no stereotypes" convention works
 * end to end with Spring MVC routing.
 */
@RequestMapping
public class HealthEndpoint {

    @GetMapping("/health")
    @ResponseBody
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString());
    }
}

package com.amitrangralabs.stockinsights.adapter.in.config;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * A {@link RequestMappingHandlerMapping} that recognises our stereotype-free endpoints.
 *
 * <p>As of Spring Framework 6.2 (Spring Boot 3.4+), the stock {@code isHandler} only treats beans
 * annotated with {@code @Controller} as request handlers — a type-level {@code @RequestMapping} is
 * no longer sufficient on its own. Since this project forbids stereotype annotations and wires every
 * endpoint explicitly as a {@code @Bean}, we restore the older, broader rule here: any bean whose
 * type carries {@code @RequestMapping} (or {@code @Controller}) is a handler.
 *
 * <p>Wired via a {@code WebMvcRegistrations} bean in {@link InboundConfig}, so Spring Boot uses this
 * as the application's {@code RequestMappingHandlerMapping} while still applying all standard MVC
 * configuration to it.
 */
public class EndpointHandlerMapping extends RequestMappingHandlerMapping {

    @Override
    protected boolean isHandler(Class<?> beanType) {
        return AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class)
                || AnnotatedElementUtils.hasAnnotation(beanType, Controller.class);
    }
}

package com.amitrangralabs.stockinsights.adapter.in.endpoint;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Serves the landing page.
 *
 * <p>Like {@link HealthEndpoint}, this is a plain class with no {@code @Controller} stereotype; it
 * is wired explicitly in {@code InboundConfig}. Returning a view name (not {@code @ResponseBody})
 * lets Thymeleaf resolve {@code templates/index.html}.
 */
@RequestMapping
public class HomeEndpoint {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("appName", "Stock Insights");
        return "index";
    }
}

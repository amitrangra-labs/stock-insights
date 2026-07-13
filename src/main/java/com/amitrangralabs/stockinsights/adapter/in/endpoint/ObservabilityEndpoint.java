package com.amitrangralabs.stockinsights.adapter.in.endpoint;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Serves the in-app observability page at {@code /observability}.
 *
 * <p>The page is a thin shell that <em>embeds</em> the Grafana SLO dashboard (which reads from
 * Prometheus). The SLO maths lives once, as Prometheus recording rules, and is presented once, by
 * Grafana — this page is just a product-facing window onto that single dashboard, so there is no
 * second SLO implementation to keep in sync. See {@code docs/SLO-DASHBOARD.md}.
 *
 * <p>Because Grafana is a separate container that reads Prometheus directly, the canonical SLO view
 * survives even when this app is down (open Grafana directly); this embed is a convenience.
 *
 * <p>Like the other endpoints, a plain class with a type-level {@code @RequestMapping} (no
 * {@code @Controller}), wired by hand in {@code InboundConfig} and routed via
 * {@code EndpointHandlerMapping}.
 */
@RequestMapping
public class ObservabilityEndpoint {

    private final String grafanaUrl;
    private final String dashboardUid;

    public ObservabilityEndpoint(String grafanaUrl, String dashboardUid) {
        this.grafanaUrl = grafanaUrl;
        this.dashboardUid = dashboardUid;
    }

    @GetMapping("/observability")
    public String observability(Model model) {
        // Kiosk mode hides Grafana chrome so the dashboard sits cleanly inside the app page.
        String embedUrl =
                grafanaUrl + "/d/" + dashboardUid + "/" + dashboardUid + "?kiosk&theme=light";
        model.addAttribute("appName", "Stock Insights");
        model.addAttribute("grafanaUrl", grafanaUrl);
        model.addAttribute("embedUrl", embedUrl);
        return "observability";
    }
}

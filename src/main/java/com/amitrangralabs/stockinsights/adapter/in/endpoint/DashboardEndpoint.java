package com.amitrangralabs.stockinsights.adapter.in.endpoint;

import com.amitrangralabs.stockinsights.domain.object.DashboardView;
import com.amitrangralabs.stockinsights.domain.service.DashboardService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Journey B: renders the dashboard page.
 *
 * <p>Stereotype-free (routed via {@code EndpointHandlerMapping}); constructed in
 * {@code InboundConfig} with the {@link DashboardService} it calls directly.
 */
@RequestMapping
public class DashboardEndpoint {

    private final DashboardService dashboardService;

    public DashboardEndpoint(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardView view = dashboardService.getDashboard();
        model.addAttribute("rows", view.rows());
        model.addAttribute("summary", view.summary());
        return "dashboard";
    }
}

package com.micropulse.monitoringengine;

import com.micropulse.monitoringengine.dto.RiskProfile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @GetMapping("/api/risk")
    public List<RiskProfile> getRisk() {
        return riskService.getLatestProfiles();
    }
}
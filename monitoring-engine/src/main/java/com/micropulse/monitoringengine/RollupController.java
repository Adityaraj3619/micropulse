package com.micropulse.monitoringengine;

import com.micropulse.monitoringengine.entity.MetricRollupEntity;
import com.micropulse.monitoringengine.repository.MetricRollupRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RollupController {

    private final MetricRollupRepository metricRollupRepository;

    public RollupController(MetricRollupRepository metricRollupRepository) {
        this.metricRollupRepository = metricRollupRepository;
    }

    @GetMapping("/api/rollups/{serviceId}")
    public List<MetricRollupEntity> getRollups(
            @PathVariable String serviceId,
            @RequestParam(defaultValue = "ONE_MIN") String windowSize) {
        return metricRollupRepository.findByServiceIdAndWindowSizeOrderByWindowStartDesc(serviceId, windowSize);
    }
}
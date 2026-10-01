package com.micropulse.monitoringengine.repository;

import com.micropulse.monitoringengine.entity.MetricRollupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetricRollupRepository extends JpaRepository<MetricRollupEntity, Long> {
    List<MetricRollupEntity> findByServiceIdAndWindowSizeOrderByWindowStartDesc(String serviceId, String windowSize);
}
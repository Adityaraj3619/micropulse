package com.micropulse.monitoringengine.repository;

import com.micropulse.monitoringengine.entity.MetricRollupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetricRollupRepository extends JpaRepository<MetricRollupEntity, Long> {
}
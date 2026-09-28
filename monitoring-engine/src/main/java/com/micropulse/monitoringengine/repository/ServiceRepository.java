package com.micropulse.monitoringengine.repository;

import com.micropulse.monitoringengine.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<ServiceEntity, String> {
}
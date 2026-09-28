package com.micropulse.monitoringengine;

import com.micropulse.monitoringengine.config.TargetServiceProperties;
import com.micropulse.monitoringengine.entity.ServiceEntity;
import com.micropulse.monitoringengine.repository.ServiceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ServiceRegistrationRunner implements CommandLineRunner {

    private final TargetServiceProperties targetServiceProperties;
    private final ServiceRepository serviceRepository;

    public ServiceRegistrationRunner(TargetServiceProperties targetServiceProperties, ServiceRepository serviceRepository) {
        this.targetServiceProperties = targetServiceProperties;
        this.serviceRepository = serviceRepository;
    }

    @Override
    public void run(String... args) {
        for (TargetServiceProperties.Target target : targetServiceProperties.getTargets()) {
            ServiceEntity entity = new ServiceEntity();
            entity.setServiceId(target.getName());
            entity.setServiceName(target.getName());
            entity.setBaseUrl(target.getBaseUrl());
            entity.setRegisteredAt(LocalDateTime.now());
            serviceRepository.save(entity);
        }
    }
}
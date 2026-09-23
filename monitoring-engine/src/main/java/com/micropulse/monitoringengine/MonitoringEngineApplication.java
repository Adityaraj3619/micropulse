package com.micropulse.monitoringengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MonitoringEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(MonitoringEngineApplication.class, args);
	}

}

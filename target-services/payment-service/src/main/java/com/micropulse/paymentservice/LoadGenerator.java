package com.micropulse.paymentservice;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LoadGenerator{
    private static final Logger log = LoggerFactory.getLogger(LoadGenerator.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String TARGET_URL = "http://localhost:8081/api/payment/order?quantity=3";

    @Scheduled(fixedRate = 5000)
    public void generateTraffic(){
        try{
            restTemplate.getForObject(TARGET_URL, String.class);
            log.info("Simulated request sent to {}", TARGET_URL);
        }catch(Exception e){
            log.warn("LoadGenerator call failed service might still be booting up");
        }
    }
}
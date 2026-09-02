package com.micropulse.paymentservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {
    
    @GetMapping("/api/payment/order")
    public String order(@RequestParam int quantity){
        return ("Order successful for "+quantity+" Items");
    }
}

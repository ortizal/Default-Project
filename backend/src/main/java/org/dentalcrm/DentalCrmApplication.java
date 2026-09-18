package org.dentalcrm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DentalCrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(DentalCrmApplication.class, args);
    }
}
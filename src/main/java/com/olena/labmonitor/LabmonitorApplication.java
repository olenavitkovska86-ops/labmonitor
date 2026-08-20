package com.olena.labmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LabmonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabmonitorApplication.class, args);
    }

}

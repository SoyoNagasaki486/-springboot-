package edu.lab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** LabGuard 应用入口。 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class LabSafetyApplication {
    public static void main(String[] args) {
        SpringApplication.run(LabSafetyApplication.class, args);
    }
}

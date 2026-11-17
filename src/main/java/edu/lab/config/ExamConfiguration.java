package edu.lab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

/** 在线考试随机组卷依赖配置。 */
@Configuration
public class ExamConfiguration {
    @Bean
    public RandomGenerator examRandomGenerator() {
        return new SecureRandom();
    }
}

package com.company.leavemanagementsystem.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;



@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.mail.host")
    public JavaMailSender javaMailSender() {
        // Mail configuration will be handled by Spring Boot auto-configuration
        // This bean is created conditionally when mail properties are provided
        return new JavaMailSenderImpl();
    }
}
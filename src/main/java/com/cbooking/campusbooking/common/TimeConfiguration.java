package com.cbooking.campusbooking.common;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class TimeConfiguration {
    @Bean
    Clock businessClock(@Value("${spring.jackson.time-zone}") ZoneId zone) {
        return Clock.system(zone);
    }
}

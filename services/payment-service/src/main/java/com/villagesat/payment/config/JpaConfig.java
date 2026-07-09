package com.villagesat.payment.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = "com.villagesat.payment.adapter.out.persistence.entity")
@EnableJpaRepositories(basePackages = "com.villagesat.payment.adapter.out.persistence")
public class JpaConfig {
}

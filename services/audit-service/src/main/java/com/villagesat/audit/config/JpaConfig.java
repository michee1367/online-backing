package com.villagesat.audit.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = "com.villagesat.audit.adapter.out.persistence.entity")
@EnableJpaRepositories(basePackages = "com.villagesat.audit.adapter.out.persistence")
public class JpaConfig {
}

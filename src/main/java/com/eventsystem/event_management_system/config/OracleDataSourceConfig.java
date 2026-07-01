package com.eventsystem.event_management_system.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "app.oracle.enabled", havingValue = "true")
public class OracleDataSourceConfig {

    @Bean("oracleDataSource")
    public DataSource oracleDataSource(
            @Value("${app.oracle.url}") String url,
            @Value("${app.oracle.username}") String username,
            @Value("${app.oracle.password}") String password
    ) {
        return DataSourceBuilder.create()
                .driverClassName("oracle.jdbc.OracleDriver")
                .url(url)
                .username(username)
                .password(password)
                .build();
    }

    @Bean("oracleJdbcTemplate")
    public JdbcTemplate oracleJdbcTemplate(@Qualifier("oracleDataSource") DataSource oracleDataSource) {
        return new JdbcTemplate(oracleDataSource);
    }
}

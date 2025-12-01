package com.iisl.config;

import javax.sql.DataSource;

import com.iisl.utilities.common.CustomConnectionDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class DataSourceConfig {

    @Value("${db.driverClassName}")
    private String driverClassName;

    @Value("${db.url}")
    private String url;

    @Value("${db.username}")
    private String username;

    @Value("${db.password}")
    private String password;

    /**
     * Plain JDBC DataSource (with encrypted creds).
     * Your CustomConnectionDataSource can decrypt inside if needed,
     * same as old project.
     */
    @Bean
    public DataSource jdbcDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(driverClassName);
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
    }

    /**
     * Main DataSource used by JPA & JdbcTemplate.
     */
    @Bean
    public DataSource dataSource(DataSource jdbcDataSource) {
        return new CustomConnectionDataSource(jdbcDataSource);
    }
}

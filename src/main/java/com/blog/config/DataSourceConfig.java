package com.blog.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceConfig {

    @Bean
    public HikariDataSource dataSource(DataSourceProperties properties) {
        String configuredUrl = properties.getUrl();
        if (configuredUrl == null || configuredUrl.isBlank()) {
            throw new IllegalStateException("Database URL is not configured");
        }

        DatabaseConnection connection = parseConnection(configuredUrl);
        properties.setUrl(connection.jdbcUrl());
        if (connection.username() != null) {
            properties.setUsername(connection.username());
        }
        if (connection.password() != null) {
            properties.setPassword(connection.password());
        }

        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    private DatabaseConnection parseConnection(String configuredUrl) {
        String url = configuredUrl.trim();
        if (url.startsWith("jdbc:")) {
            url = url.substring("jdbc:".length());
        }
        if (!url.startsWith("postgres://") && !url.startsWith("postgresql://")) {
            return new DatabaseConnection("jdbc:" + url, null, null);
        }

        URI uri = URI.create(url);
        String userInfo = uri.getUserInfo();
        String username = null;
        String password = null;
        if (userInfo != null) {
            int separator = userInfo.indexOf(':');
            username = decode(separator >= 0 ? userInfo.substring(0, separator) : userInfo);
            password = separator >= 0 ? decode(userInfo.substring(separator + 1)) : null;
        }

        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost());
        if (uri.getPort() >= 0) {
            jdbcUrl.append(':').append(uri.getPort());
        }
        jdbcUrl.append(uri.getRawPath());
        if (uri.getRawQuery() != null) {
            jdbcUrl.append('?').append(uri.getRawQuery());
        }
        return new DatabaseConnection(jdbcUrl.toString(), username, password);
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record DatabaseConnection(String jdbcUrl, String username, String password) {
    }
}

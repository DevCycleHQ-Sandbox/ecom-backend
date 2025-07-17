package com.shopper.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.shopper.repository.CartItemRepository;
import com.shopper.repository.secondary.SecondaryCartItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String primaryUrl;

    @Value("${spring.datasource.driver-class-name}")
    private String primaryDriverClassName;

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int primaryMaxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int primaryMinIdle;

    @Value("${spring.datasource.hikari.connection-timeout:20000}")
    private long primaryConnectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:300000}")
    private long primaryIdleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1200000}")
    private long primaryMaxLifetime;

    @Value("${secondary.datasource.url:}")
    private String secondaryUrl;

    @Value("${secondary.datasource.username:}")
    private String secondaryUsername;

    @Value("${secondary.datasource.password:}")
    private String secondaryPassword;

    @Value("${secondary.datasource.driver-class-name:org.postgresql.Driver}")
    private String secondaryDriverClassName;

    @Value("${secondary.datasource.hikari.maximum-pool-size:5}")
    private int secondaryMaxPoolSize;

    @Value("${secondary.datasource.hikari.minimum-idle:2}")
    private int secondaryMinIdle;

    @Value("${secondary.datasource.hikari.connection-timeout:30000}")
    private long secondaryConnectionTimeout;

    @Value("${secondary.datasource.hikari.idle-timeout:600000}")
    private long secondaryIdleTimeout;

    @Value("${secondary.datasource.hikari.max-lifetime:1800000}")
    private long secondaryMaxLifetime;

    @Value("${secondary.datasource.hikari.leak-detection-threshold:60000}")
    private long secondaryLeakDetectionThreshold;

    @Bean
    @Primary
    public DataSource primaryDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(primaryUrl);
        config.setDriverClassName(primaryDriverClassName);
        config.setMaximumPoolSize(primaryMaxPoolSize);
        config.setMinimumIdle(primaryMinIdle);
        config.setConnectionTimeout(primaryConnectionTimeout);
        config.setIdleTimeout(primaryIdleTimeout);
        config.setMaxLifetime(primaryMaxLifetime);
        config.setPoolName("PrimaryHikariPool");
        
        return new HikariDataSource(config);
    }

    @Bean
    @ConditionalOnProperty(name = "secondary.datasource.enabled", havingValue = "true")
    public DataSource secondaryDataSource() {
        HikariConfig config = new HikariConfig();
        
        // Parse Neon URL format: postgresql://user:pass@host:port/db?params
        String jdbcUrl = secondaryUrl;
        String username = secondaryUsername;
        String password = secondaryPassword;
        
        if (jdbcUrl.startsWith("postgresql://")) {
            // Extract credentials from URL if present
            String url = jdbcUrl.substring("postgresql://".length());
            if (url.contains("@")) {
                String[] parts = url.split("@", 2);
                String credentials = parts[0];
                String hostAndRest = parts[1];
                
                if (credentials.contains(":")) {
                    String[] credParts = credentials.split(":", 2);
                    username = credParts[0];
                    password = credParts[1];
                }
                
                // Reconstruct URL without credentials
                jdbcUrl = "jdbc:postgresql://" + hostAndRest;
            } else {
                jdbcUrl = "jdbc:" + jdbcUrl;
            }
        }
        
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(secondaryDriverClassName);
        config.setMaximumPoolSize(secondaryMaxPoolSize);
        config.setMinimumIdle(secondaryMinIdle);
        config.setConnectionTimeout(secondaryConnectionTimeout);
        config.setIdleTimeout(secondaryIdleTimeout);
        config.setMaxLifetime(secondaryMaxLifetime);
        config.setLeakDetectionThreshold(secondaryLeakDetectionThreshold);
        config.setPoolName("NeonHikariPool");
        
        // Neon-specific optimizations
        config.addDataSourceProperty("reWriteBatchedInserts", "true");
        config.addDataSourceProperty("prepareThreshold", "1");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        
        return new HikariDataSource(config);
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(primaryDataSource());
        em.setPackagesToScan("com.shopper.entity");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        properties.setProperty("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect");
        properties.setProperty("hibernate.show_sql", "true");
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("logging.level.org.hibernate.SQL", "DEBUG");
        properties.setProperty("logging.level.org.hibernate.type.descriptor.sql.BasicBinder", "TRACE");
        
        // Fix UUID handling for SQLite
        properties.setProperty("hibernate.type.preferred_uuid_jdbc_type", "CHAR");
        properties.setProperty("hibernate.id.uuid_gen_strategy_class", "uuid2");
        
        em.setJpaProperties(properties);
        return em;
    }

    @Bean
    @Primary
    public PlatformTransactionManager primaryTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(primaryEntityManagerFactory().getObject());
        return transactionManager;
    }

    @Bean
    @ConditionalOnProperty(name = "secondary.datasource.enabled", havingValue = "true")
    public LocalContainerEntityManagerFactoryBean secondaryEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(secondaryDataSource());
        em.setPackagesToScan("com.shopper.entity");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.show_sql", "false");
        
        em.setJpaProperties(properties);
        return em;
    }

    @Bean
    @ConditionalOnProperty(name = "secondary.datasource.enabled", havingValue = "true")
    public PlatformTransactionManager secondaryTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(secondaryEntityManagerFactory().getObject());
        return transactionManager;
    }
    
    // Primary database repository configuration
    @Configuration
    @EnableJpaRepositories(
        basePackages = {"com.shopper.repository.primary"},
        entityManagerFactoryRef = "primaryEntityManagerFactory",
        transactionManagerRef = "primaryTransactionManager"
    )
    static class PrimaryRepositoryConfig {
    }
    
    // Secondary database repository configuration
    @ConditionalOnProperty(name = "secondary.datasource.enabled", havingValue = "true")
    @Configuration
    @EnableJpaRepositories(
        basePackages = "com.shopper.repository.secondary",
        entityManagerFactoryRef = "secondaryEntityManagerFactory",
        transactionManagerRef = "secondaryTransactionManager"
    )
    static class SecondaryRepositoryConfig {
    }
    
    // Regular repository configuration (for repositories that don't use dual database)
    @Configuration
    @EnableJpaRepositories(
        basePackages = "com.shopper.repository",
        entityManagerFactoryRef = "primaryEntityManagerFactory",
        transactionManagerRef = "primaryTransactionManager",
        excludeFilters = {
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {CartItemRepository.class}),
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.shopper\\.repository\\.(primary|secondary)\\..*")
        }
    )
    static class RegularRepositoryConfig {
    }
}
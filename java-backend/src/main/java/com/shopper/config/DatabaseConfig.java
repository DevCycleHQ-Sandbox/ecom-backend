package com.shopper.config;

import com.shopper.repository.primary.PrimaryCartItemRepository;
import com.shopper.repository.primary.PrimaryProductRepository;
import com.shopper.repository.primary.PrimaryUserRepository;
import com.shopper.repository.secondary.SecondaryCartItemRepository;
import com.shopper.repository.secondary.SecondaryProductRepository;
import com.shopper.repository.secondary.SecondaryUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
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
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@Slf4j
public class DatabaseConfig {

    // Primary Database (SQLite) Configuration
    @Configuration
    @EnableJpaRepositories(
            basePackages = "com.shopper.repository.primary",
            entityManagerFactoryRef = "primaryEntityManagerFactory",
            transactionManagerRef = "primaryTransactionManager"
    )
    static class PrimaryRepositoryConfig {

        @Bean
        @Primary
        public DataSource primaryDataSource() {
            return DataSourceBuilder.create()
                    .url("jdbc:sqlite:database.sqlite")
                    .driverClassName("org.sqlite.JDBC")
                    .build();
        }

        @Bean
        @Primary
        public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
                @Qualifier("primaryDataSource") DataSource dataSource) {
            
            LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
            em.setDataSource(dataSource);
            em.setPackagesToScan("com.shopper.entity");
            em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

            Map<String, Object> properties = new HashMap<>();
            properties.put("hibernate.hbm2ddl.auto", "update");
            properties.put("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect");
            em.setJpaPropertyMap(properties);

            return em;
        }

        @Bean
        @Primary
        public PlatformTransactionManager primaryTransactionManager(
                @Qualifier("primaryEntityManagerFactory") LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory) {
            return new JpaTransactionManager(primaryEntityManagerFactory.getObject());
        }
    }

    // Secondary Database (Neon PostgreSQL) Configuration
    @Configuration
    @ConditionalOnProperty(name = "secondary.datasource.enabled", havingValue = "true")
    @EnableJpaRepositories(
            basePackages = "com.shopper.repository.secondary",
            entityManagerFactoryRef = "secondaryEntityManagerFactory",
            transactionManagerRef = "secondaryTransactionManager"
    )
    static class SecondaryRepositoryConfig {

        @Bean
        @ConfigurationProperties(prefix = "secondary.datasource")
        public DataSource secondaryDataSource() {
            return DataSourceBuilder.create().build();
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean secondaryEntityManagerFactory(
                @Qualifier("secondaryDataSource") DataSource dataSource) {
            
            LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
            em.setDataSource(dataSource);
            em.setPackagesToScan("com.shopper.entity");
            em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

            Map<String, Object> properties = new HashMap<>();
            properties.put("hibernate.hbm2ddl.auto", "update");
            properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            em.setJpaPropertyMap(properties);

            return em;
        }

        @Bean
        public PlatformTransactionManager secondaryTransactionManager(
                @Qualifier("secondaryEntityManagerFactory") LocalContainerEntityManagerFactoryBean secondaryEntityManagerFactory) {
            return new JpaTransactionManager(secondaryEntityManagerFactory.getObject());
        }
    }

    // Regular repositories (non-primary, non-secondary)
    @Configuration
    @EnableJpaRepositories(
            basePackages = "com.shopper.repository",
            excludeFilters = {
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = PrimaryProductRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = PrimaryUserRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = PrimaryCartItemRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = SecondaryProductRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = SecondaryUserRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = SecondaryCartItemRepository.class)
            },
            entityManagerFactoryRef = "primaryEntityManagerFactory",
            transactionManagerRef = "primaryTransactionManager"
    )
    static class RegularRepositoryConfig {
    }
}
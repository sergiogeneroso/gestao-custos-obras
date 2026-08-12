package com.seegeneroso.gestao_custos_obras.shared.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * Configuração manual do Flyway.
 *
 * ponytail: o Spring Boot 4.1 removeu a FlywayAutoConfiguration do
 * spring-boot-autoconfigure (não há starter equivalente). Sem estes beans,
 * o flyway-core fica no classpath porém inerte — ninguém instancia o migrador
 * nem roda as migrations, e o schema fica só por conta do Hibernate. Aqui o
 * Flyway passa a ser a única fonte de schema (ddl-auto=validate no
 * application.properties), alinhado à ADR-010.
 *
 * A ordem de inicialização é crucial: o Hibernate validate (que roda na criação
 * do EntityManagerFactory) precisa enxergar o schema já migrado. Bean EMF é
 * "entityManagerFactory"; o registryPostProcessor abaixo adiciona o bean
 * "flywayMigrate" às suas dependências, garantindo migrate() antes do validate.
 *
 * Quando o Spring Boot voltar a fornecer auto-config/starter para Flyway,
 * remover esta classe e spring.flyway.enabled=false e usar a config nativa.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }

    /** Roda migrate() na inicialização do bean, antes do EntityManagerFactory. */
    @Bean(name = "flywayMigrate")
    public Flyway migrateRunner(Flyway flyway) throws java.io.IOException {
        Resource[] migrations = new PathMatchingResourcePatternResolver()
                .getResources("classpath:db/migration/V*.sql");
        if (migrations.length > 0) {
            flyway.migrate();
        }
        return flyway;
    }

    /** Força entityManagerFactory a depender de flywayMigrate (migrate antes do validate). */
    @Bean
    public static BeanDefinitionRegistryPostProcessor flywayDependsOnRegistrar() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
                if (registry.containsBeanDefinition("entityManagerFactory")) {
                    BeanDefinition def = registry.getBeanDefinition("entityManagerFactory");
                    def.setDependsOn("flywayMigrate");
                }
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
                // no-op
            }
        };
    }
}

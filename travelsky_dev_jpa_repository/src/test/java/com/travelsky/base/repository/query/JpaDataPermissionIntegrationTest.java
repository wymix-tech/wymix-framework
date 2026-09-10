package com.travelsky.base.repository.query;

import com.travelsky.base.repository.datapermission.annotations.DataPermission;
import com.travelsky.base.repository.datapermission.aop.DataPermissionAspect;
import com.travelsky.base.repository.datapermission.enums.InjectType;
import com.travelsky.base.repository.datapermission.inject.PermissionSqlInjector;
import com.travelsky.base.repository.jpa.api.AbstractBaseServiceImpl;
import com.travelsky.base.repository.jpa.api.BaseRepository;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JPA 数据权限端到端验证（H2 内存库）：
 * 在真实 {@code EntityManagerFactory} 上验证 {@code @DataPermission} 经切面进入上下文后，
 * {@code AbstractBaseServiceImpl#findAll()} 会带上注入器声明的过滤条件。
 *
 * <p>关键点：
 * <ol>
 *   <li>注入器只需是可实例化的实现类（本测试未注册为 Bean，走反射兜底），不再依赖
 *   {@code @EnableDataPermission} 的扫描注册 —— 修复前 JPA 场景会因拿不到 Bean 直接抛异常；</li>
 *   <li>注入器写的是数据库列名 {@code OWNER}，由 {@code SpecificationHelper} 映射为属性名 {@code owner}。</li>
 * </ol>
 */
class JpaDataPermissionIntegrationTest {

    @Entity
    @Table(name = "SECURED_DOC")
    static class SecuredDoc {

        @Id
        private Long id;

        private String owner;

        SecuredDoc() {
        }

        SecuredDoc(Long id, String owner) {
            this.id = id;
            this.owner = owner;
        }
    }

    interface SecuredDocRepository extends BaseRepository<SecuredDoc, Long> {
    }

    /** 注入器：只返回 owner = 'ALICE' 的数据（列名写法）。 */
    static class OwnerInjector implements PermissionSqlInjector {
        @Override
        public Map<String, Object> additionalParameters() {
            return Map.of();
        }

        @Override
        public String injectSql() {
            return "OWNER = 'ALICE'";
        }

        @Override
        public InjectType type() {
            return InjectType.WHERE_ADDITIONAL;
        }
    }

    static class SecuredDocService extends AbstractBaseServiceImpl<SecuredDoc, Long, SecuredDocRepository> {

        SecuredDocService(SecuredDocRepository repository) {
            super(repository);
        }

        @DataPermission(injectors = {OwnerInjector.class})
        public List<SecuredDoc> findAllWithPermission() {
            return this.findAll();
        }
    }

    @Configuration
    @EnableJpaRepositories(basePackages = "com.travelsky.base.repository.query",
            considerNestedRepositories = true)
    @EntityScan(basePackages = "com.travelsky.base.repository.query")
    @EnableTransactionManagement
    static class TestConfiguration {

        @Bean
        DataSource dataSource() {
            // 直接用 H2 内存库（不走 EmbeddedDatabaseBuilder，避免其默认执行 classpath 脚本）
            org.springframework.jdbc.datasource.DriverManagerDataSource dataSource =
                    new org.springframework.jdbc.datasource.DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl("jdbc:h2:mem:data-permission-test-" + System.nanoTime()
                    + ";DB_CLOSE_DELAY=-1;MODE=LEGACY");
            dataSource.setUsername("sa");
            dataSource.setPassword("");
            return dataSource;
        }

        @Bean
        org.springframework.orm.jpa.JpaTransactionManager transactionManager(
                jakarta.persistence.EntityManagerFactory entityManagerFactory) {
            return new org.springframework.orm.jpa.JpaTransactionManager(entityManagerFactory);
        }

        @Bean
        org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean factory =
                    new org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.travelsky.base.repository.query");
            factory.setJpaVendorAdapter(new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter());
            java.util.Properties properties = new java.util.Properties();
            properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            factory.setJpaProperties(properties);
            return factory;
        }

        @Bean
        SecuredDocService securedDocService(SecuredDocRepository repository) {
            return new SecuredDocService(repository);
        }
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withConfiguration(AutoConfigurations.of(
                    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
                    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class));

    @Test
    void shouldAppendPermissionPredicateToFindAll() {
        contextRunner.run(context -> {
            // 同一个上下文内准备数据：每个 run 都会新建独立的 H2 内存库
            SecuredDocRepository repository = context.getBean(SecuredDocRepository.class);
            repository.saveAndFlush(new SecuredDoc(1L, "ALICE"));
            repository.saveAndFlush(new SecuredDoc(2L, "BOB"));

            SecuredDocService target = context.getBean(SecuredDocService.class);
            // 手工套用切面（等价于 JpaConfiguration 注册的 DataPermissionAspect）
            AspectJProxyFactory factory = new AspectJProxyFactory(target);
            // 与框架 core 的 @EnableAsync(proxyTargetClass = true) 保持一致：CGLIB 代理，注解在实现类方法上
            factory.setProxyTargetClass(true);
            factory.addAspect(new DataPermissionAspect());
            SecuredDocService proxy = factory.getProxy();

            assertEquals(2, target.findAll().size(), "无权限上下文时应返回全量");
            List<SecuredDoc> secured = proxy.findAllWithPermission();
            assertEquals(1, secured.size(), "数据权限应只返回 ALICE 的数据");
            assertEquals("ALICE", secured.get(0).owner);
        });
    }
}

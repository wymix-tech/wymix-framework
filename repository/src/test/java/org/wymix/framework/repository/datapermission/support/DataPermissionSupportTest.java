package org.wymix.framework.repository.datapermission.support;

import org.wymix.framework.repository.datapermission.enums.InjectType;
import org.wymix.framework.repository.datapermission.inject.PermissionSqlInjector;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DataPermissionSupport} 单元测试（无 Spring 容器）：
 * injectSql 解析、命名参数/字面量取值、statementIds 白名单判定、注入器实例兜底。
 */
class DataPermissionSupportTest {

    static class ParamInjector implements PermissionSqlInjector {

        private final String sql;

        ParamInjector(String sql) {
            this.sql = sql;
        }

        @Override
        public Map<String, Object> additionalParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("deptId", 42L);
            params.put("tenant", "T1");
            return params;
        }

        @Override
        public String injectSql() {
            return sql;
        }

        @Override
        public InjectType type() {
            return InjectType.WHERE_ADDITIONAL;
        }
    }

    @Test
    void shouldParseSingleLiteralTerm() {
        List<DataPermissionSupport.DataPermissionTerm> terms =
                DataPermissionSupport.parseWhereTerms("CREATED_BY = 'SYSTEM'", new ParamInjector("x = 1"));
        assertEquals(1, terms.size());
        assertEquals("CREATED_BY", terms.get(0).getProperty());
        assertEquals("SYSTEM", terms.get(0).getValue());
    }

    @Test
    void shouldParseMultipleTermsJoinedByAnd() {
        List<DataPermissionSupport.DataPermissionTerm> terms =
                DataPermissionSupport.parseWhereTerms("DEPT_ID = :deptId AND STATUS = 1", new ParamInjector("x = 1"));
        assertEquals(2, terms.size());
        assertEquals("DEPT_ID", terms.get(0).getProperty());
        assertEquals(42L, terms.get(0).getValue());
        assertEquals("STATUS", terms.get(1).getProperty());
        assertEquals(1L, terms.get(1).getValue());
    }

    @Test
    void shouldParseLowerCaseAndAndAliasPrefix() {
        List<DataPermissionSupport.DataPermissionTerm> terms =
                DataPermissionSupport.parseWhereTerms("t.TENANT = :tenant and t.STATUS = '01'", new ParamInjector("x = 1"));
        assertEquals(2, terms.size());
        // 带表别名的字段会被去掉前缀
        assertEquals("TENANT", terms.get(0).getProperty());
        assertEquals("T1", terms.get(0).getValue());
        // '01' 有前导零，不能被误判成数字
        assertEquals("01", terms.get(1).getValue());
    }

    @Test
    void shouldRejectUnsupportedOperator() {
        ParamInjector injector = new ParamInjector("AGE >= 18");
        assertThrows(IllegalArgumentException.class, () -> DataPermissionSupport.parseWhereTerms(injector.injectSql(), injector));
    }

    @Test
    void shouldRejectMissingNamedParameter() {
        ParamInjector injector = new ParamInjector("DEPT_ID = :missing");
        assertThrows(IllegalArgumentException.class, () -> DataPermissionSupport.parseWhereTerms(injector.injectSql(), injector));
    }

    @Test
    void shouldReturnEmptyForBlankSql() {
        assertTrue(DataPermissionSupport.parseWhereTerms("   ", new ParamInjector("x = 1")).isEmpty());
    }

    @Test
    void shouldMatchAllStatementsOnlyWhenAllOrEmpty() {
        assertTrue(DataPermissionSupport.matchesAllStatements(null));
        assertTrue(DataPermissionSupport.matchesAllStatements(new String[]{}));
        assertTrue(DataPermissionSupport.matchesAllStatements(new String[]{"com.x.Mapper.select", "ALL"}));
        // JPA 无 statementId 概念：显式配置具体 id 时不生效（避免「配了白名单却全量生效」）
        assertFalse(DataPermissionSupport.matchesAllStatements(new String[]{"com.x.Mapper.select"}));
    }

    /** 未注册为 Bean 的注入器：无参构造可实例化，用于验证反射兜底。 */
    static class PlainInjector implements PermissionSqlInjector {
        @Override
        public Map<String, Object> additionalParameters() {
            return Map.of();
        }

        @Override
        public String injectSql() {
            return "TENANT = 'T1'";
        }

        @Override
        public InjectType type() {
            return InjectType.WHERE_ADDITIONAL;
        }
    }

    @Test
    void shouldFallbackToReflectionWhenInjectorIsNotABean() {
        // 无 ApplicationContext 时退化为反射实例化并告警，而不是 NPE / NoSuchBeanDefinitionException
        PlainInjector injector = DataPermissionSupport.resolveInjector(PlainInjector.class);
        assertEquals("TENANT = 'T1'", injector.injectSql());
    }

    @Test
    void shouldThrowClearExceptionWhenInjectorCannotBeInstantiated() {
        class NoDefaultConstructorInjector implements PermissionSqlInjector {
            NoDefaultConstructorInjector(String ignored) {
            }

            @Override
            public Map<String, Object> additionalParameters() {
                return Map.of();
            }

            @Override
            public String injectSql() {
                return "";
            }

            @Override
            public InjectType type() {
                return InjectType.WHERE_ADDITIONAL;
            }
        }
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> DataPermissionSupport.resolveInjector(NoDefaultConstructorInjector.class));
        assertTrue(exception.getMessage().contains(NoDefaultConstructorInjector.class.getName()), exception.getMessage());
    }

    @Test
    void shouldSupportNullLiteralAndBoolean() {
        List<DataPermissionSupport.DataPermissionTerm> terms =
                DataPermissionSupport.parseWhereTerms("DELETED = null AND ENABLED = true", new ParamInjector("x = 1"));
        assertNull(terms.get(0).getValue());
        assertEquals(Boolean.TRUE, terms.get(1).getValue());
    }
}

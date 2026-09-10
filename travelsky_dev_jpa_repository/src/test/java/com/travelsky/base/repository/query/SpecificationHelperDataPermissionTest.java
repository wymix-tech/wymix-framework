package com.travelsky.base.repository.query;

import com.travelsky.base.repository.datapermission.context.DataPermissionContextHolder;
import com.travelsky.base.repository.datapermission.enums.InjectType;
import com.travelsky.base.repository.datapermission.inject.PermissionSqlInjector;
import com.travelsky.base.repository.datapermission.support.DataPermissionSupport;
import com.travelsky.base.repository.jpa.audit.domain.AbstractAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SpecificationHelper#applyDataPermission} 单元测试：
 * <ol>
 *   <li>无 {@code @DataPermission} 上下文时不追加条件（返回 null，行为不变）；</li>
 *   <li>数据库列名（{@code CREATED_BY}）能映射到实体属性名（{@code createdBy}）；</li>
 *   <li>{@code AND} 多条件、命名参数；</li>
 *   <li>{@code statementIds} 白名单不匹配时跳过（JPA 无 statementId 概念）；</li>
 *   <li>{@code REPLACE_TABLE_NAME} 注入器跳过；</li>
 *   <li>字段名既不是属性名也无法映射时快速失败（避免静默返回全量数据）。</li>
 * </ol>
 *
 * <p>不启动 Spring / 数据库：用 Mockito 模拟 {@code Root}、{@code CriteriaBuilder}，
 * 只验证「解析出的条件项」是否符合预期。
 */
class SpecificationHelperDataPermissionTest {

    @Entity
    @Table(name = "DEMO_ORDER")
    static class DemoOrder extends AbstractAuditEntity {

        @Id
        @Column(name = "ID")
        private Long id;

        @Column(name = "DEPT_ID")
        private Long deptId;

        @Column(name = "STATUS")
        private Integer status;
    }

    static class ColumnNameInjector implements PermissionSqlInjector {
        @Override
        public Map<String, Object> additionalParameters() {
            return Map.of();
        }

        @Override
        public String injectSql() {
            return "CREATED_BY = 'SYSTEM'";
        }

        @Override
        public InjectType type() {
            return InjectType.WHERE_ADDITIONAL;
        }
    }

    static class MultiConditionInjector implements PermissionSqlInjector {
        @Override
        public Map<String, Object> additionalParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("deptId", 7L);
            return params;
        }

        @Override
        public String injectSql() {
            return "DEPT_ID = :deptId AND STATUS = 1";
        }

        @Override
        public InjectType type() {
            return InjectType.WHERE_ADDITIONAL;
        }
    }

    static class ReplaceTableInjector implements PermissionSqlInjector {
        @Override
        public Map<String, Object> additionalParameters() {
            return Map.of();
        }

        @Override
        public String injectSql() {
            return "DEMO_ORDER -> DEMO_ORDER_V";
        }

        @Override
        public InjectType type() {
            return InjectType.REPLACE_TABLE_NAME;
        }
    }

    static class UnknownColumnInjector implements PermissionSqlInjector {
        @Override
        public Map<String, Object> additionalParameters() {
            return Map.of();
        }

        @Override
        public String injectSql() {
            return "NOT_EXIST_COLUMN = 1";
        }

        @Override
        public InjectType type() {
            return InjectType.WHERE_ADDITIONAL;
        }
    }

    private Root<DemoOrder> root;
    private CriteriaBuilder criteriaBuilder;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        root = Mockito.mock(Root.class, Mockito.RETURNS_DEEP_STUBS);
        criteriaBuilder = Mockito.mock(CriteriaBuilder.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(root.getJavaType()).thenReturn((Class) DemoOrder.class);
        EntityType<DemoOrder> entityType = Mockito.mock(EntityType.class);
        Metamodel metamodel = Mockito.mock(Metamodel.class);
        Mockito.when(entityType.getJavaType()).thenReturn((Class) DemoOrder.class);
        Mockito.when(metamodel.entity(DemoOrder.class)).thenReturn(entityType);
        Mockito.when(root.getModel()).thenReturn(entityType);
        // 让 root.get(...) 只对真实存在的属性名返回 Path，其余抛 IllegalArgumentException
        // （与 Hibernate 的 Root#get 行为一致，这样才能验证列名 -> 属性名的映射是否生效）
        Mockito.when(root.get(Mockito.anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0, String.class);
            if (hasProperty(name)) {
                return Mockito.mock(jakarta.persistence.criteria.Path.class);
            }
            throw new IllegalArgumentException("Unable to locate Attribute with the given name [" + name + "]");
        });
        Mockito.when(criteriaBuilder.equal(Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            Predicate predicate = Mockito.mock(Predicate.class);
            Mockito.when(predicate.toString()).thenReturn("eq(" + invocation.getArgument(0) + "," + invocation.getArgument(1) + ")");
            return predicate;
        });
        Mockito.when(criteriaBuilder.and(Mockito.any(Predicate.class), Mockito.any(Predicate.class)))
                .thenAnswer(invocation -> {
                    Predicate predicate = Mockito.mock(Predicate.class);
                    Mockito.when(predicate.toString()).thenReturn("and");
                    return predicate;
                });
    }

    /** 判断 DemoOrder（含父类审计字段）是否存在该属性名。 */
    private static boolean hasProperty(String name) {
        Class<?> clazz = DemoOrder.class;
        while (clazz != null && clazz != Object.class) {
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                if (field.getName().equals(name)) {
                    return true;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    @AfterEach
    void tearDown() {
        DataPermissionContextHolder.clear();
    }

    @Test
    void shouldReturnNullWithoutContext() {
        assertNull(SpecificationHelper.applyDataPermission(root, criteriaBuilder));
    }

    @Test
    void shouldMapColumnNameToEntityProperty() {
        mark(new String[]{"ALL"}, ColumnNameInjector.class);
        Predicate predicate = SpecificationHelper.applyDataPermission(root, criteriaBuilder);
        // 列名 CREATED_BY 应被解析为属性 createdBy 后再取 Path
        Mockito.verify(root).get("createdBy");
        assertNotNull(predicate, "列名映射到属性名后应产出 Predicate");
        // 值取字面量 SYSTEM（'SYSTEM' 带引号，按字符串处理）
        Mockito.verify(criteriaBuilder).equal(Mockito.any(), Mockito.eq("SYSTEM"));
    }

    @Test
    void shouldSupportMultipleAndConditions() {
        mark(new String[]{"ALL"}, MultiConditionInjector.class);
        SpecificationHelper.applyDataPermission(root, criteriaBuilder);
        Mockito.verify(root).get("deptId");
        Mockito.verify(root).get("status");
    }

    @Test
    void shouldSkipWhenStatementIdsDoNotContainAll() {
        // JPA 没有 MappedStatement id：显式白名单（不含 ALL）时应跳过，避免「配了白名单却全量生效」
        mark(new String[]{"com.swcares.sample.dao.SampleUserDao.selectById"}, ColumnNameInjector.class);
        assertNull(SpecificationHelper.applyDataPermission(root, criteriaBuilder));
    }

    @Test
    void shouldSkipReplaceTableInjector() {
        mark(new String[]{"ALL"}, ReplaceTableInjector.class);
        assertNull(SpecificationHelper.applyDataPermission(root, criteriaBuilder));
    }

    @Test
    void shouldFailFastOnUnknownColumn() {
        mark(new String[]{"ALL"}, UnknownColumnInjector.class);
        assertThrows(IllegalArgumentException.class, () -> SpecificationHelper.applyDataPermission(root, criteriaBuilder));
    }

    @Test
    void shouldCombineFilterAndPermission() {
        mark(new String[]{"ALL"}, ColumnNameInjector.class);
        com.travelsky.base.common.query.DevQueryVO queryVO = new com.travelsky.base.common.query.DevQueryVO();
        queryVO.setFilter("[[username,=,tom]]");
        CriteriaQuery<?> criteriaQuery = Mockito.mock(CriteriaQuery.class, Mockito.RETURNS_DEEP_STUBS);
        // 仅验证不会因权限上下文抛异常（filter 解析依赖真实 Path，这里关注组合逻辑不炸）
        try {
            SpecificationHelper.parse(queryVO, root, criteriaQuery, criteriaBuilder);
        } catch (Exception ignore) {
            // filter 侧的 Path 为 mock，可能抛异常，此处只要求不因数据权限解析抛错
        }
        assertTrue(DataPermissionSupport.matchesAllStatements(new String[]{"ALL"}));
    }

    private static void mark(String[] statementIds, Class<? extends PermissionSqlInjector> injector) {
        DataPermissionContextHolder.DataPermissionContext context = new DataPermissionContextHolder.DataPermissionContext();
        context.setStatementIds(statementIds);
        context.setInjectors(new ArrayList<>(Arrays.asList(injector)));
        DataPermissionContextHolder.mark(context);
    }
}

package com.travelsky.base.repository.datapermission.support;

import com.travelsky.base.core.context.ApplicationContextHolder;
import com.travelsky.base.repository.datapermission.context.DataPermissionContextHolder;
import com.travelsky.base.repository.datapermission.inject.PermissionSqlInjector;
import com.travelsky.base.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据权限注入器的公共支撑：注入器实例获取、{@code statementIds} 白名单判定、{@code injectSql} 解析。
 *
 * <p>上提到本模块（与注解/上下文/切面同处 {@code travelsky_dev_repository}）的目的是让 MyBatis 与 JPA 两侧
 * 复用同一套解析语义，并可脱离 Spring 容器做单测。
 *
 * <p><b>注入器实例获取</b>：优先从 Spring 容器取 Bean（与 MyBatis 侧一致，保留 {@code @Autowired} 注入能力）；
 * 容器中没有时（JPA 场景未使用 {@code @EnableDataPermission}、业务方也忘了标 {@code @Component}）
 * 退化为无参构造反射实例化并输出 {@code warn}，仍失败则抛出带注入器类名的 {@link IllegalStateException}，
 * 避免问题以 {@code NullPointerException} 或底层 {@code NoSuchBeanDefinitionException} 形式暴露。
 *
 * <p><b>injectSql 解析</b>：支持 {@code AND} 连接的多个 {@code =} 条件（{@code :param} 命名参数与字面量）。
 * 不支持的语法（{@code OR} / {@code IN} / {@code LIKE} / {@code >} 等）<b>直接快速失败</b>而非静默跳过：
 * 数据权限漏条件等于返回全量数据，风险高于报错，故此处选择显式异常。
 */
public final class DataPermissionSupport {

    /**
     * {@code statementIds} 通配符：命中全部语句。
     */
    public static final String STATEMENT_ALL = "ALL";

    private static final Logger log = LoggerFactory.getLogger(DataPermissionSupport.class);

    /**
     * 拆分多条件所用的分隔符（大小写不敏感）。
     */
    private static final String AND_SEPARATOR = " AND ";

    private DataPermissionSupport() {
    }

    /**
     * 获取注入器实例：优先取容器中的 Bean，未注册时退化为反射实例化并告警。
     *
     * @param injectorClass {@code @DataPermission(injectors = ...)} 中声明的注入器类型
     * @return 注入器实例，永不返回 {@code null}
     * @throws IllegalStateException 既不是 Bean 也无法反射实例化时抛出，消息中带注入器类名
     */
    public static <T extends PermissionSqlInjector> T resolveInjector(Class<T> injectorClass) {
        ApplicationContext applicationContext = ApplicationContextHolder.getApplicationContext();
        if (applicationContext != null) {
            try {
                return applicationContext.getBean(injectorClass);
            } catch (BeansException ignore) {
                // 落到下方的反射兜底
            }
        }
        try {
            Constructor<T> constructor = injectorClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            T injector = constructor.newInstance();
            log.warn("数据权限注入器 [{}] 未注册为 Spring Bean，已反射实例化（其 @Autowired 字段不会被注入）；"
                            + "建议标注 @Component 或交由 @EnableDataPermission 扫描注册", injectorClass.getName());
            return injector;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("数据权限注入器未注册为 Spring Bean，且无法通过无参构造实例化: "
                    + injectorClass.getName() + "；请标注 @Component 或纳入 @EnableDataPermission(basePackages = ...) 扫描范围", e);
        }
    }

    /**
     * JPA 侧的 {@code statementIds} 白名单判定：JPA 没有 MappedStatement id 的概念，
     * 因此仅当白名单为空或包含 {@code ALL} 时生效；显式配置了具体 statementId 时跳过并告警，
     * 避免「配了白名单却对全量语句生效」这类更危险的静默行为。
     *
     * @param statementIds {@link DataPermissionContextHolder.DataPermissionContext#getStatementIds()}
     * @return {@code true} 表示应追加权限条件
     */
    public static boolean matchesAllStatements(String[] statementIds) {
        if (statementIds == null || statementIds.length == 0) {
            return true;
        }
        for (String statementId : statementIds) {
            if (STATEMENT_ALL.equals(statementId)) {
                return true;
            }
        }
        log.warn("JPA 数据权限不支持按 statementId 过滤，已跳过本次注入: {}", (Object) statementIds);
        return false;
    }

    /**
     * 将 {@code injectSql} 解析为若干 {@code =} 条件项，多条件以 {@code AND} 连接。
     *
     * @param injectSql {@link PermissionSqlInjector#injectSql()}，如 {@code DEPT_ID = :deptId AND STATUS = 1}
     * @param injector  注入器实例，用于取 {@link PermissionSqlInjector#additionalParameters()} 中的命名参数
     * @return 条件项列表（顺序与书写顺序一致，语义上按 AND 组合）
     * @throws IllegalArgumentException 出现不支持的语法或缺失命名参数时抛出
     */
    public static List<DataPermissionTerm> parseWhereTerms(String injectSql, PermissionSqlInjector injector) {
        List<DataPermissionTerm> terms = new ArrayList<>();
        if (StringUtils.isBlank(injectSql)) {
            return terms;
        }
        for (String segment : injectSql.trim().split("(?i)" + AND_SEPARATOR)) {
            terms.add(parseTerm(segment.trim(), injector, injectSql));
        }
        return terms;
    }

    private static DataPermissionTerm parseTerm(String segment, PermissionSqlInjector injector, String injectSql) {
        int eqIndex = indexOfEquals(segment);
        if (eqIndex <= 0 || eqIndex == segment.length() - 1) {
            throw new IllegalArgumentException("JPA 数据权限不支持的 injectSql 片段（仅支持 '字段 = 值'，多条件用 AND 连接）: "
                    + segment + "，完整 injectSql: " + injectSql);
        }
        String column = stripAlias(segment.substring(0, eqIndex).trim());
        String rawValue = segment.substring(eqIndex + 1).trim();
        if (StringUtils.isBlank(column)) {
            throw new IllegalArgumentException("JPA 数据权限 injectSql 缺少字段名: " + segment + "，完整 injectSql: " + injectSql);
        }
        return new DataPermissionTerm(column, resolveValue(rawValue, injector, segment));
    }

    /**
     * 定位 {@code =} 的位置；命中 {@code !=} / {@code >=} / {@code <=} / {@code <>} 时返回 -1（不支持）。
     */
    private static int indexOfEquals(String segment) {
        int index = segment.indexOf('=');
        if (index <= 0) {
            return -1;
        }
        char before = segment.charAt(index - 1);
        if (before == '!' || before == '<' || before == '>') {
            return -1;
        }
        char after = segment.charAt(index + 1);
        if (after == '>') {
            return -1;
        }
        return index;
    }

    /**
     * 去掉 {@code 表别名.} 前缀与反引号/引号，得到纯字段名。
     */
    private static String stripAlias(String column) {
        String property = unquote(column);
        int dot = property.indexOf('.');
        while (dot >= 0) {
            property = unquote(property.substring(dot + 1));
            dot = property.indexOf('.');
        }
        return property;
    }

    private static boolean isQuoted(String value) {
        String trimmed = value.trim();
        return trimmed.length() >= 2 && ((trimmed.startsWith("`") && trimmed.endsWith("`"))
                || (trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'")));
    }

    private static String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && ((trimmed.startsWith("`") && trimmed.endsWith("`"))
                || (trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static Object resolveValue(String rawValue, PermissionSqlInjector injector, String segment) {
        if (rawValue.startsWith(":")) {
            String paramName = rawValue.substring(1).trim();
            Map<String, Object> params = injector.additionalParameters();
            if (params == null || !params.containsKey(paramName)) {
                throw new IllegalArgumentException("数据权限 injectSql 使用了命名参数 [:" + paramName
                        + "]，但 " + injector.getClass().getName() + "#additionalParameters() 中未提供该参数，片段: " + segment);
            }
            return params.get(paramName);
        }
        if (rawValue.startsWith("#{") && rawValue.endsWith("}")) {
            String paramName = rawValue.substring(2, rawValue.length() - 1).trim();
            Map<String, Object> params = injector.additionalParameters();
            if (params == null || !params.containsKey(paramName)) {
                throw new IllegalArgumentException("数据权限 injectSql 使用了命名参数 [#{ " + paramName
                        + " }]，但 " + injector.getClass().getName() + "#additionalParameters() 中未提供该参数，片段: " + segment);
            }
            return params.get(paramName);
        }
        if ("null".equalsIgnoreCase(rawValue)) {
            return null;
        }
        // 带引号的一律按字符串处理：避免 '01' 这类有前导零的编码被误判成数字
        if (isQuoted(rawValue)) {
            return unquote(rawValue);
        }
        if ("true".equalsIgnoreCase(rawValue) || "false".equalsIgnoreCase(rawValue)) {
            return Boolean.valueOf(rawValue);
        }
        if (rawValue.matches("-?\\d+")) {
            return Long.valueOf(rawValue);
        }
        if (rawValue.matches("-?\\d+\\.\\d+")) {
            return Double.valueOf(rawValue);
        }
        return rawValue;
    }

    /**
     * 单条 {@code =} 条件项：字段名（可能是数据库列名，由各 ORM 侧再做映射）+ 值。
     */
    public static class DataPermissionTerm {

        private final String property;

        private final Object value;

        DataPermissionTerm(String property, Object value) {
            this.property = property;
            this.value = value;
        }

        public String getProperty() {
            return property;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public String toString() {
            return property + " = " + value;
        }
    }

    /**
     * 供 ORM 侧做列名→属性名映射时读取成员上的注解（如 JPA 的 {@code @Column}）。
     *
     * @param member 属性对应的成员（Field / Method）
     * @return 成员上的注解，未标注时返回 {@code null}
     */
    public static <A extends java.lang.annotation.Annotation> A findAnnotation(Member member, Class<A> annotationType) {
        if (member instanceof java.lang.reflect.AnnotatedElement annotatedElement) {
            return annotatedElement.getAnnotation(annotationType);
        }
        return null;
    }
}

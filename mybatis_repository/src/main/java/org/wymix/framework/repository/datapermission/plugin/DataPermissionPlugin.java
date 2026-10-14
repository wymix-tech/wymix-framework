package org.wymix.framework.repository.datapermission.plugin;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.util.CollectionUtils;
import org.wymix.framework.repository.datapermission.context.DataPermissionContextHolder;
import org.wymix.framework.repository.datapermission.inject.PermissionSqlInjector;
import org.wymix.framework.repository.datapermission.support.DataPermissionSupport;
import org.wymix.framework.repository.helper.SqlParseHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Intercepts({
    @Signature(
            type = Executor.class,
            method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(
            type = Executor.class,
            method = "query",
            args = {
                MappedStatement.class,
                Object.class,
                RowBounds.class,
                ResultHandler.class,
                CacheKey.class,
                BoundSql.class
            }),
    @Signature(
            type = Executor.class,
            method = "update",
            args = {MappedStatement.class, Object.class}),
})
@org.springframework.core.annotation.Order(
        org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 100)
public class DataPermissionPlugin implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!DataPermissionContextHolder.exists()) {
            return invocation.proceed();
        }
        DataPermissionContextHolder.DataPermissionContext context =
                DataPermissionContextHolder.getContext();
        final Object[] args = invocation.getArgs();
        MappedStatement statement = (MappedStatement) args[0];
        Object parameterObject = args[1];

        // 获取SQL查询的上下文信息
        BoundSql boundSql = statement.getBoundSql(parameterObject);
        String originalSql = boundSql.getSql();

        if (Arrays.stream(context.getStatementIds())
                        .anyMatch(
                                statementId ->
                                        DataPermissionSupport.STATEMENT_ALL.equals(statementId)
                                                || statementId.equals(statement.getId()))
                && !CollectionUtils.isEmpty(context.getInjectors())) {
            // 修改SQL
            List<Class<? extends PermissionSqlInjector>> injectorClasses = context.getInjectors();

            List<PermissionSqlInjector> injectors = new ArrayList<>();

            for (Class<? extends PermissionSqlInjector> injectorClass : injectorClasses) {
                // 与 JPA 侧共用解析入口：容器中没有时退化反射实例化并告警，仍失败则抛明确异常
                injectors.add(DataPermissionSupport.resolveInjector(injectorClass));
            }

            String modifiedSql = modifySql(originalSql, injectors);

            Configuration configuration = statement.getConfiguration();
            BoundSql newBoundSql = bindParameters(configuration, modifiedSql, boundSql, injectors);
            modifiedSql = newBoundSql.getSql();

            MappedStatement newMappedStatement =
                    copyFromMappedStatement(statement, new BoundSqlSource(newBoundSql));
            MetaObject metaObject =
                    MetaObject.forObject(
                            newMappedStatement,
                            new DefaultObjectFactory(),
                            new DefaultObjectWrapperFactory(),
                            new DefaultReflectorFactory());
            metaObject.setValue("sqlSource.boundSql.sql", modifiedSql);
            invocation.getArgs()[0] = newMappedStatement;
            if (args.length == 6) {
                args[4] =
                        ((Executor) invocation.getTarget())
                                .createCacheKey(
                                        newMappedStatement,
                                        parameterObject,
                                        (RowBounds) args[2],
                                        newBoundSql);
                args[5] = newBoundSql;
            }
        }

        return invocation.proceed();
    }

    /** Bind named permission values in SQL order without mutating the caller's parameter object. */
    private static BoundSql bindParameters(
            Configuration configuration,
            String sql,
            BoundSql original,
            List<PermissionSqlInjector> injectors) {
        Map<String, Object> values = new java.util.HashMap<>();
        for (var injector : injectors)
            injector.additionalParameters()
                    .forEach(
                            (key, value) -> {
                                if (values.containsKey(key)
                                        && !java.util.Objects.equals(values.get(key), value)) {
                                    throw new IllegalArgumentException(
                                            "Conflicting permission parameter: " + key);
                                }
                                values.put(key, value);
                            });
        List<ParameterMapping> mappings = new ArrayList<>();
        Map<String, Object> additional =
                new java.util.HashMap<>(original.getAdditionalParameters());
        var originals = original.getParameterMappings().iterator();
        StringBuilder bound = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (quote != 0) {
                bound.append(c);
                if (c == quote) {
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == quote)
                        bound.append(sql.charAt(++i));
                    else quote = 0;
                }
            } else if (c == '\'' || c == '"' || c == '`') {
                quote = c;
                bound.append(c);
            } else if (c == '?') {
                if (!originals.hasNext())
                    throw new IllegalArgumentException("Missing SQL parameter mapping");
                mappings.add(originals.next());
                bound.append(c);
            } else if (c == ':'
                    && i + 1 < sql.length()
                    && Character.isJavaIdentifierStart(sql.charAt(i + 1))
                    && (i == 0 || sql.charAt(i - 1) != ':')) {
                int end = i + 2;
                while (end < sql.length() && Character.isJavaIdentifierPart(sql.charAt(end))) end++;
                String name = sql.substring(i + 1, end);
                if (!values.containsKey(name))
                    throw new IllegalArgumentException("Missing permission parameter: " + name);
                Object value = values.get(name);
                String key = "__wymix_permission_" + mappings.size();
                mappings.add(
                        new ParameterMapping.Builder(
                                        configuration,
                                        key,
                                        value == null ? Object.class : value.getClass())
                                .build());
                additional.put(key, value);
                bound.append('?');
                i = end - 1;
            } else bound.append(c);
        }
        if (originals.hasNext()) throw new IllegalArgumentException("Unused SQL parameter mapping");
        BoundSql result =
                new BoundSql(
                        configuration, bound.toString(), mappings, original.getParameterObject());
        additional.forEach(result::setAdditionalParameter);
        return result;
    }

    private MappedStatement copyFromMappedStatement(
            MappedStatement ms, BoundSqlSource boundSqlSource) {
        MappedStatement.Builder builder =
                new MappedStatement.Builder(
                        ms.getConfiguration(), ms.getId(), boundSqlSource, ms.getSqlCommandType());

        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null)
            builder.keyProperty(String.join(",", ms.getKeyProperties()));
        if (ms.getKeyColumns() != null) builder.keyColumn(String.join(",", ms.getKeyColumns()));
        builder.databaseId(ms.getDatabaseId());
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());

        return builder.build();
    }

    private String modifySql(String originalSql, List<? extends PermissionSqlInjector> injectors)
            throws Exception {
        SqlParseHelper.SqlContext sqlContext = SqlParseHelper.parse(originalSql);
        for (PermissionSqlInjector injector : injectors) {
            switch (injector.type()) {
                case REPLACE_TABLE_NAME:
                    String injectSql = injector.injectSql();
                    String[] temp = injectSql.split("->");
                    originalSql = originalSql.replaceAll(temp[0], "(" + temp[1] + ")");
                    break;
                default:
                    switch (sqlContext.getType()) {
                        case INSERT:
                            // DO NOTHING
                            break;
                        case UPDATE:
                            Update update = (Update) sqlContext.getStatement();
                            if (update.getWhere() == null) {
                                Expression whereExpression =
                                        (Expression)
                                                (CCJSqlParserUtil.parseCondExpression(
                                                        injector.injectSql()));
                                update.setWhere(whereExpression);
                            } else {
                                Expression whereExpression =
                                        (Expression)
                                                (CCJSqlParserUtil.parseCondExpression(
                                                        String.format(
                                                                "(%s) and (%s)",
                                                                update.getWhere(),
                                                                injector.injectSql())));
                                update.setWhere(whereExpression);
                            }
                            originalSql = update.toString();
                            break;
                        case DELETE:
                            Delete delete = (Delete) sqlContext.getStatement();
                            if (delete.getWhere() == null) {
                                Expression whereExpression =
                                        (Expression)
                                                (CCJSqlParserUtil.parseCondExpression(
                                                        injector.injectSql()));
                                delete.setWhere(whereExpression);
                            } else {
                                Expression whereExpression =
                                        (Expression)
                                                (CCJSqlParserUtil.parseCondExpression(
                                                        String.format(
                                                                "(%s) and (%s)",
                                                                delete.getWhere(),
                                                                injector.injectSql())));
                                delete.setWhere(whereExpression);
                            }
                            originalSql = delete.toString();
                            break;
                        default:
                            Select select = (Select) sqlContext.getStatement();
                            PlainSelect plainSelect = select.getPlainSelect();
                            if (plainSelect.getWhere() == null) {
                                Expression whereExpression =
                                        (Expression)
                                                (CCJSqlParserUtil.parseCondExpression(
                                                        injector.injectSql()));
                                plainSelect.setWhere(whereExpression);
                            } else {
                                Expression whereExpression =
                                        (Expression)
                                                (CCJSqlParserUtil.parseCondExpression(
                                                        String.format(
                                                                "(%s) and (%s)",
                                                                plainSelect.getWhere(),
                                                                injector.injectSql())));
                                plainSelect.setWhere(whereExpression);
                            }
                            originalSql = select.toString();
                            break;
                    }
                    break;
            }
        }
        return originalSql;
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    @Override
    public void setProperties(Properties properties) {
        Interceptor.super.setProperties(properties);
    }
}

class BoundSqlSource implements SqlSource {
    private BoundSql boundSql;

    public BoundSqlSource(BoundSql boundSql) {
        this.boundSql = boundSql;
    }

    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        return boundSql;
    }
}

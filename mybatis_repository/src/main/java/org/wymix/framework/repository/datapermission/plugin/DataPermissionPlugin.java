package org.wymix.framework.repository.datapermission.plugin;

import org.wymix.framework.repository.datapermission.context.DataPermissionContextHolder;
import org.wymix.framework.repository.datapermission.support.DataPermissionSupport;
import org.wymix.framework.repository.datapermission.inject.PermissionSqlInjector;
import org.wymix.framework.repository.helper.SqlParseHelper;
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
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
})
public class DataPermissionPlugin implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!DataPermissionContextHolder.exists()) {
            return invocation.proceed();
        }
        DataPermissionContextHolder.DataPermissionContext context = DataPermissionContextHolder.getContext();
        final Object[] args = invocation.getArgs();
        MappedStatement statement = (MappedStatement) args[0];
        Object parameterObject = args[1];

        // 获取SQL查询的上下文信息
        BoundSql boundSql = statement.getBoundSql(parameterObject);
        String originalSql = boundSql.getSql();

        if (Arrays.stream(context.getStatementIds()).anyMatch(statementId -> DataPermissionSupport.STATEMENT_ALL.equals(statementId)
                || statementId.equals(statement.getId())) && !CollectionUtils.isEmpty(context.getInjectors())) {
            // 修改SQL
            List<Class<? extends PermissionSqlInjector>> injectorClasses = context.getInjectors();

            List<PermissionSqlInjector> injectors = new ArrayList<>();

            for (Class<? extends PermissionSqlInjector> injectorClass : injectorClasses) {
                // 与 JPA 侧共用解析入口：容器中没有时退化反射实例化并告警，仍失败则抛明确异常
                injectors.add(DataPermissionSupport.resolveInjector(injectorClass));
            }

            String modifiedSql = modifySql(originalSql, injectors);

            Configuration configuration = statement.getConfiguration();
            BoundSql newBoundSql = new BoundSql(configuration, modifiedSql, boundSql.getParameterMappings(), boundSql.getParameterObject());


            for (PermissionSqlInjector injector : injectors) {
                for (Map.Entry<String, Object> entry : injector.additionalParameters().entrySet()) {
                    ParameterMapping parameterMapping = new ParameterMapping.Builder(configuration, entry.getKey(), entry.getValue().getClass()).build();
                    newBoundSql.getParameterMappings().add(parameterMapping);
                    // 添加新的参数值
                    newBoundSql.setAdditionalParameter(entry.getKey(), entry.getValue());
                    MetaObject metaObject = SystemMetaObject.forObject(newBoundSql.getParameterObject());
                    metaObject.setValue(entry.getKey(), entry.getValue());

                }
            }
            MappedStatement newMappedStatement = copyFromMappedStatement(statement,
                    new BoundSqlSource(newBoundSql));
            MetaObject metaObject = MetaObject.forObject(newMappedStatement, new DefaultObjectFactory(),
                    new DefaultObjectWrapperFactory(), new DefaultReflectorFactory());
            metaObject.setValue("sqlSource.boundSql.sql", modifiedSql);
            invocation.getArgs()[0] = newMappedStatement;
            if (args.length == 6) {
                args[4] = ((Executor) invocation.getTarget()).createCacheKey(statement, parameterObject,
                        (RowBounds) args[2], newBoundSql);
                args[5] = newBoundSql;
            }
        }

        return invocation.proceed();
    }

    private MappedStatement copyFromMappedStatement(MappedStatement ms, BoundSqlSource boundSqlSource) {
        MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), boundSqlSource, ms.getSqlCommandType());

        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());

        return builder.build();
    }

    private String modifySql(String originalSql, List<? extends PermissionSqlInjector> injectors) throws Exception {
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
                                Expression whereExpression = (Expression) (CCJSqlParserUtil
                                        .parseCondExpression(injector.injectSql()));
                                update.setWhere(whereExpression);
                            } else {
                                Expression whereExpression = (Expression) (CCJSqlParserUtil
                                        .parseCondExpression(String.format("(%s) and (%s)", update.getWhere(), injector.injectSql())));
                                update.setWhere(whereExpression);
                            }
                            originalSql = update.toString();
                            break;
                        case DELETE:
                            Delete delete = (Delete) sqlContext.getStatement();
                            if (delete.getWhere() == null) {
                                Expression whereExpression = (Expression) (CCJSqlParserUtil
                                        .parseCondExpression(injector.injectSql()));
                                delete.setWhere(whereExpression);
                            } else {
                                Expression whereExpression = (Expression) (CCJSqlParserUtil
                                        .parseCondExpression(String.format("(%s) and (%s)", delete.getWhere(), injector.injectSql())));
                                delete.setWhere(whereExpression);
                            }
                            originalSql = delete.toString();
                            break;
                        default:
                            Select select = (Select) sqlContext.getStatement();
                            PlainSelect plainSelect = select.getPlainSelect();
                            if (plainSelect.getWhere() == null) {
                                Expression whereExpression = (Expression) (CCJSqlParserUtil
                                        .parseCondExpression(injector.injectSql()));
                                plainSelect.setWhere(whereExpression);
                            } else {
                                Expression whereExpression = (Expression) (CCJSqlParserUtil
                                        .parseCondExpression(String.format("(%s) and (%s)", plainSelect.getWhere(), injector.injectSql())));
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

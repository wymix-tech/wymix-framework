package org.wymix.framework.repository.datachangelog.supports;

import org.wymix.framework.core.context.ApplicationContextHolder;
import org.wymix.framework.repository.datachangelog.annotation.DataChangeLog;
import org.wymix.framework.repository.datachangelog.enums.StatementType;
import org.wymix.framework.repository.datamigration.annotations.DataMigration;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.EmptyInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class ManualDmlDataChangeInterceptor extends EmptyInterceptor {

    @Autowired
    private volatile EntityManager entityManager;

    private Object mutex = new Object();

    public static String replaceQuestionMarks(String sql) {
        if (sql == null || sql.isEmpty()) return sql;

        StringBuilder result = new StringBuilder();
        int counter = 1;

        for (char c : sql.toCharArray()) {
            if (c == '?') {
                result.append(":").append(counter++);
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

//    @Override
//    public String onPrepareStatement(String sql) {
//        if (ManualDmlContext.has()) {
//            String result = replaceQuestionMarks(sql);
//            if (StringUtils.startsWithIgnoreCase(result, "insert")) {
//                doInsert(result);
//            } else if (StringUtils.startsWithIgnoreCase(result, "update")) {
//                try {
//                    doUpdate(result);
//                } catch (Exception e) {
//                }
//            } else if (StringUtils.startsWithIgnoreCase(result, "delete")) {
//                try {
//                    doDelete(result);
//                } catch (Exception e) {
//                }
//            }
//        }
//        return sql;
//    }



    private void doDelete(String result) throws Exception {
        SqlParseHelper.SqlContext sqlContext = SqlParseHelper.parse(result);
        Delete delete = (Delete) sqlContext.getStatement();

        PlainSelect select = new PlainSelect();
        select.withFromItem(delete.getTable());

        select.withWhere(delete.getWhere());
        if (delete.getTable().getAlias() != null) {
            select.setSelectItems(Collections.singletonList(new SelectItem<>(CCJSqlParserUtil.parseCondExpression(delete.getTable().getAlias().getName() + ".*"))));
        } else {
            select.setSelectItems(Collections.singletonList(new SelectItem<>(new AllColumns())));
        }
        if (entityManager == null) {
            synchronized (this.mutex) {
                if (entityManager == null) {
                    entityManager = ApplicationContextHolder.getApplicationContext().getBean(EntityManager.class);
                }
            }
        }

        String tableName = select.getFromItem().toString();
        Optional<Class<?>> entityClassByTableName = getEntityClassByTableName(tableName, StatementType.REMOVE);
        if (!entityClassByTableName.isPresent()) {
            return;
        }
        Query nativeQuery = entityManager.createNativeQuery(select.toString(), entityClassByTableName.get());
        Map<String, Object> map = ManualDmlContext.getParams();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String k = entry.getKey();
            if (select.getWhere().toString().contains(":" + k + " ") || select.getWhere().toString().endsWith(":" + k)) {
                nativeQuery.setParameter(k, entry.getValue());
            }
        }
        List<Object> resultList = nativeQuery.getResultList();
        ManualDmlContext.returnResults(select, entityClassByTableName.get(), resultList);
    }

    private void doUpdate(String result) throws Exception {
        SqlParseHelper.SqlContext sqlContext = SqlParseHelper.parse(result);
        Update update = (Update) sqlContext.getStatement();

        PlainSelect select = new PlainSelect();
        select.withFromItem(update.getTable());
        if (!CollectionUtils.isEmpty(update.getStartJoins())) {
            select.withJoins(update.getStartJoins());
        }
        select.withWhere(update.getWhere());
        if (update.getTable().getAlias() != null) {
            select.setSelectItems(Collections.singletonList(new SelectItem<>(CCJSqlParserUtil.parseCondExpression(update.getTable().getAlias().getName() + ".*"))));
        } else {
            select.setSelectItems(Collections.singletonList(new SelectItem<>(new AllColumns())));
        }
        if (entityManager == null) {
            synchronized (this.mutex) {
                if (entityManager == null) {
                    entityManager = ApplicationContextHolder.getApplicationContext().getBean(EntityManager.class);
                }
            }
        }

        String tableName = select.getFromItem().toString();
        Optional<Class<?>> entityClassByTableName = getEntityClassByTableName(tableName, StatementType.MODIFY);
        if (!entityClassByTableName.isPresent()) {
            return;
        }
        Query nativeQuery = entityManager.createNativeQuery(select.toString(), entityClassByTableName.get());
        Map<String, Object> map = ManualDmlContext.getParams();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String k = entry.getKey();
            if (select.getWhere().toString().contains(":" + k + " ") || select.getWhere().toString().endsWith(":" + k)) {
                nativeQuery.setParameter(k, entry.getValue());
            }
        }
        List<Object> resultList = nativeQuery.getResultList();
        ManualDmlContext.returnResults(select, entityClassByTableName.get(), resultList);
    }

    private void doInsert(String result) {
    }

    public Optional<Class<?>> getEntityClassByTableName(String tableName, StatementType statementType) {
        Metamodel metamodel = entityManager.getMetamodel();
        for (EntityType<?> entityType : metamodel.getEntities()) {
            // 获取实体类
            Class<?> entityClass = entityType.getJavaType();

            // 获取 @Table 注解，如果存在则使用其 name 属性，否则使用类名（JPA 默认策略）
            String entityTableName = entityClass.getAnnotation(jakarta.persistence.Table.class) != null ? entityClass.getAnnotation(jakarta.persistence.Table.class).name() : entityClass.getSimpleName(); // 简化处理，默认表名是类名

            // 比较表名（注意大小写，根据数据库配置可能需要忽略大小写）
            if (((entityClass.isAnnotationPresent(DataChangeLog.class) && ArrayUtils.contains(entityClass.getAnnotation(DataChangeLog.class).record(), statementType)) || entityClass.isAnnotationPresent(DataMigration.class)) && entityTableName.equalsIgnoreCase(tableName)) {
                return Optional.of(entityClass);
            }
        }
        return Optional.empty();
    }
}

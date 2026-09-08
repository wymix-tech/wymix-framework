package com.travelsky.base.repository.datachangelog.plugin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.travelsky.base.common.context.UserContextHolder;
import com.travelsky.base.repository.datachangelog.dto.DataChangeLogDTO;
import com.travelsky.base.repository.datachangelog.enums.StatementType;
import com.travelsky.base.repository.datachangelog.event.DataChangeLogEvent;
import com.travelsky.base.repository.helper.SqlParseHelper;
import com.travelsky.base.utils.GlobalIdGenerator;
import com.travelsky.base.utils.HttpUtils;
import com.travelsky.base.utils.ReflectUtils;
import com.travelsky.base.utils.StringUtils;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeException;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class DataChangeLogPlugin implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(DataChangeLogPlugin.class);

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
        PluginUtils.MPStatementHandler mpSh = PluginUtils.mpStatementHandler(statementHandler);
        MappedStatement ms = mpSh.mappedStatement();
        SqlCommandType sct = ms.getSqlCommandType();
        if (sct == SqlCommandType.INSERT || sct == SqlCommandType.UPDATE || sct == SqlCommandType.DELETE) {
            Connection connection = (Connection) invocation.getArgs()[0];
            try {
                switch (sct) {
                    case INSERT:
                        doInsert(mpSh, ms);
                        break;
                    case UPDATE:
                        doUpdate(mpSh, ms, connection);
                        break;
                    case DELETE:
                        doDelete(mpSh, ms, connection);
                        break;
                }
            } catch (Exception e) {
                log.warn("数据变更记录异常", e);
            }
        }
        return invocation.proceed();
    }

    private void doInsert(PluginUtils.MPStatementHandler mpSh, MappedStatement ms) throws Exception {
        SqlParseHelper.SqlContext sqlContext = SqlParseHelper.parse(mpSh.boundSql().getSql());
        if (sqlContext.getType() != SqlParseHelper.Type.INSERT && !DataChangeLogTableRegistry.exists(sqlContext.getTable().getName())) {
            return;
        }
        Insert insert = (Insert) sqlContext.getStatement();
        genDataChangeLogEvent(null, mpSh, ms, insert.getTable().getName(), StatementType.INSERT);
    }

    private void doUpdate(PluginUtils.MPStatementHandler mpSh, MappedStatement ms, Connection connection) throws Exception {
        SqlParseHelper.SqlContext sqlContext = SqlParseHelper.parse(mpSh.boundSql().getSql());
        if (sqlContext.getType() != SqlParseHelper.Type.UPDATE && !DataChangeLogTableRegistry.exists(sqlContext.getTable().getName())) {
            return;
        }
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
        List<Map<String, Object>> oldObjs = getSelectObjs(ms, connection, select, mpSh);

        genDataChangeLogEvent(oldObjs, mpSh, ms, update.getTable().getName(), StatementType.MODIFY);
    }

    private void doDelete(PluginUtils.MPStatementHandler mpSh, MappedStatement ms, Connection connection) throws Exception {
        SqlParseHelper.SqlContext sqlContext = SqlParseHelper.parse(mpSh.boundSql().getSql());
        if (sqlContext.getType() != SqlParseHelper.Type.DELETE && !DataChangeLogTableRegistry.exists(sqlContext.getTable().getName())) {
            return;
        }
        Delete delete = (Delete) sqlContext.getStatement();

        PlainSelect select = new PlainSelect();
        select.withFromItem(delete.getTable());
        if (!CollectionUtils.isEmpty(delete.getJoins())) {
            select.withJoins(delete.getJoins());
        }
        select.withWhere(delete.getWhere());
        if (delete.getTable().getAlias() != null) {
            select.setSelectItems(Collections.singletonList(new SelectItem<>(CCJSqlParserUtil.parseCondExpression(delete.getTable().getAlias().getName() + ".*"))));
        } else {
            select.setSelectItems(Collections.singletonList(new SelectItem<>(new AllColumns())));
        }
        List<Map<String, Object>> oldObjs = getSelectObjs(ms, connection, select, mpSh);

        genDataChangeLogEvent(oldObjs, mpSh, ms, delete.getTable().getName(), StatementType.REMOVE);
    }


    private void genDataChangeLogEvent(List<Map<String, Object>> oldObjs, PluginUtils.MPStatementHandler mpSh, MappedStatement ms, String tableName, StatementType statementType) {
        BoundSql boundSql = mpSh.boundSql();
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        Configuration configuration = ms.getConfiguration();
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        Map<String, Object> newObj = new HashMap<>();
        if (parameterMappings != null) {
            for (ParameterMapping parameterMapping : parameterMappings) {
                if (parameterMapping.getMode() != ParameterMode.OUT) {
                    Object value;
                    String propertyName = parameterMapping.getProperty();
                    if (boundSql.hasAdditionalParameter(propertyName)) {
                        value = boundSql.getAdditionalParameter(propertyName);
                    } else if (parameterObject == null) {
                        value = null;
                    } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                        value = parameterObject;
                    } else {
                        MetaObject metaObject = configuration.newMetaObject(parameterObject);
                        value = metaObject.getValue(propertyName);
                        try {
                            Object ew = metaObject.getValue("ew");
                            if (ew != null) {
                                UpdateWrapper uw = (UpdateWrapper) ew;
                                String[] sqlSet = uw.getSqlSet().split(",");
                                for (String s : sqlSet) {
                                    if (s.contains(propertyName)) {
                                        propertyName = s.split("=")[0];
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // DO NOTHING
                        }
                    }
                    if (!propertyName.contains("ew")) {
                        if (propertyName.contains(".")) {
                            String[] temp = propertyName.split("\\.");
                            propertyName = temp[temp.length - 1];
                        }
                        if (!newObj.containsKey(propertyName)) {
                            newObj.put(propertyName, value);
                        }
                    }
                }
            }
        }
        ServletRequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes() != null ? (ServletRequestAttributes) RequestContextHolder.getRequestAttributes() : null;
        HttpServletRequest request = requestAttributes != null ? requestAttributes.getRequest() : null;
        Long requestId = GlobalIdGenerator.generateNextId();
        String requestRemoteIp = request == null ? null : HttpUtils.getClientIpAddress(request);
        if (!CollectionUtils.isEmpty(oldObjs)) {
            for (Map<String, Object> oldObj : oldObjs) {
                DataChangeLogDTO dataChangeLogDTO = new DataChangeLogDTO();
                Class entityClass = DataChangeLogTableRegistry.getClass(tableName);
                dataChangeLogDTO.setClassName(entityClass.getName());
                String idName = ReflectUtils.findFieldWithAnnotation(entityClass, TableId.class)[0].getName();
                dataChangeLogDTO.setId(oldObj.get(idName));
                dataChangeLogDTO.setLastModifiedBy(UserContextHolder.getUserContext() == null ? "SYSTEM" : UserContextHolder.getUserContext().getPrincipal());
                dataChangeLogDTO.setLastModifiedDate(LocalDateTime.now());
                dataChangeLogDTO.setStatementType(statementType);
                dataChangeLogDTO.setTableName(tableName);
                dataChangeLogDTO.setUri(request.getRequestURI());
                dataChangeLogDTO.setRequestId(requestId);
                dataChangeLogDTO.setRequestRemoteIp(requestRemoteIp);
                if (statementType == StatementType.MODIFY) {
                    newObj.forEach((k, v) -> {
                        if (!StringUtils.containsAny(k, "createdBy", "createdTime", "lastModifiedBy", "lastModifiedTime"))
                            if (!oldObj.get(k).equals(v)) {
                                dataChangeLogDTO.getFieldInfos().add(new DataChangeLogDTO.FieldInfo(k, oldObj.get(k), v));
                            }
                    });
                } else {
                    oldObj.forEach((k, v) -> {
                        if (!StringUtils.containsAny(k, idName, "createdBy", "createdTime", "lastModifiedBy", "lastModifiedTime"))
                            dataChangeLogDTO.getFieldInfos().add(new DataChangeLogDTO.FieldInfo(k, oldObj.get(k), null));
                    });
                }
                DataChangeLogEvent dataChangeLogEvent = new DataChangeLogEvent(this, dataChangeLogDTO);
                applicationEventPublisher.publishEvent(dataChangeLogEvent);
            }
        } else if (oldObjs == null && !newObj.isEmpty()) {
            DataChangeLogDTO dataChangeLogDTO = new DataChangeLogDTO();
            Class entityClass = DataChangeLogTableRegistry.getClass(tableName);
            dataChangeLogDTO.setClassName(entityClass.getName());
            String idName = ReflectUtils.findFieldWithAnnotation(entityClass, TableId.class)[0].getName();
            dataChangeLogDTO.setId(newObj.get(idName));
            dataChangeLogDTO.setLastModifiedBy(UserContextHolder.getUserContext() == null ? "SYSTEM" : UserContextHolder.getUserContext().getPrincipal());
            dataChangeLogDTO.setLastModifiedDate(LocalDateTime.now());
            dataChangeLogDTO.setStatementType(statementType);
            dataChangeLogDTO.setTableName(tableName);
            dataChangeLogDTO.setUri(request.getRequestURI());
            dataChangeLogDTO.setRequestId(requestId);
            dataChangeLogDTO.setRequestRemoteIp(requestRemoteIp);
            newObj.forEach((k, v) -> {
                if (!StringUtils.containsAny(k, idName, "createdBy", "createdTime", "lastModifiedBy", "lastModifiedTime")) {
                    dataChangeLogDTO.getFieldInfos().add(new DataChangeLogDTO.FieldInfo(k, null, v));
                }
            });
            DataChangeLogEvent dataChangeLogEvent = new DataChangeLogEvent(this, dataChangeLogDTO);
            applicationEventPublisher.publishEvent(dataChangeLogEvent);
        }

    }

    private List<Map<String, Object>> getSelectObjs(MappedStatement ms, Connection connection, PlainSelect select, PluginUtils.MPStatementHandler mpSh) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(select.toString())) {
            setParameters(preparedStatement, mpSh.boundSql(), mpSh.boundSql().getParameterObject(), ms, getParamSize(select));
            return executeSelectSql(preparedStatement);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Map<String, Object>> executeSelectSql(PreparedStatement preparedStatement) {
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            return getResultFromResultSet(resultSet);
        } catch (Exception e) {
            throw ExceptionUtils.mpe("Error: Method executeForTarget execution error of mapping result set to List<%s> : \n %s \n", "Map", e);
        }
    }

    private List<Map<String, Object>> getResultFromResultSet(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        List<Map<String, Object>> ret = new ArrayList<>();
        Map<String, Object> row;
        while (resultSet.next()) {
            row = new HashMap<>();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                row.put(StringUtils.underlineToCamel(metaData.getColumnName(i)), resultSet.getObject(i));
            }
            ret.add(row);
        }
        return ret;
    }

    private void setParameters(PreparedStatement preparedStatement, BoundSql boundSql, Object parameterObject, MappedStatement ms, int paramSize) {
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        Configuration configuration = ms.getConfiguration();
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        if (parameterMappings != null) {
            for (int i = 0; i < paramSize; i++) {
                ParameterMapping parameterMapping = parameterMappings.get(parameterMappings.size() - paramSize + i);
                if (parameterMapping.getMode() != ParameterMode.OUT) {
                    Object value;
                    String propertyName = parameterMapping.getProperty();
                    if (boundSql.hasAdditionalParameter(propertyName)) {
                        value = boundSql.getAdditionalParameter(propertyName);
                    } else if (parameterObject == null) {
                        value = null;
                    } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                        value = parameterObject;
                    } else {
                        MetaObject metaObject = configuration.newMetaObject(parameterObject);
                        value = metaObject.getValue(propertyName);
                    }
                    TypeHandler typeHandler = parameterMapping.getTypeHandler();
                    JdbcType jdbcType = parameterMapping.getJdbcType();
                    if (value == null && jdbcType == null) {
                        jdbcType = configuration.getJdbcTypeForNull();
                    }
                    try {
                        typeHandler.setParameter(preparedStatement, i + 1, value, jdbcType);
                    } catch (TypeException | SQLException e) {
                        throw new TypeException("Could not set parameters for mapping: " + parameterMapping + ". Cause: " + e, e);
                    }
                }
            }
        }
    }

    private int getParamSize(PlainSelect select) {
        char[] sqlChars = select.toString().toCharArray();
        int res = 0;
        for (char c : sqlChars) {
            if (c == '?') {
                res++;
            }
        }
        return res;
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    @Override
    public void setProperties(Properties properties) {
        Interceptor.super.setProperties(properties);
    }
}

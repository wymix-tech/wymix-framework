package org.wymix.framework.repository.datachangelog.plugin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;

import org.apache.ibatis.executor.BatchExecutor;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.wymix.framework.common.context.UserContextHolder;
import org.wymix.framework.repository.datachangelog.annotation.DataChangeLog;
import org.wymix.framework.repository.datachangelog.dto.DataChangeLogDTO;
import org.wymix.framework.repository.datachangelog.enums.StatementType;
import org.wymix.framework.repository.datachangelog.event.DataChangeLogEvent;
import org.wymix.framework.repository.helper.SqlParseHelper;
import org.wymix.framework.utils.GlobalIdGenerator;
import org.wymix.framework.utils.HttpUtils;
import org.wymix.framework.utils.ReflectUtils;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** Captures on the same connection, records successful rows, publishes only after commit. */
@Intercepts({
    @Signature(
            type = Executor.class,
            method = "update",
            args = {MappedStatement.class, Object.class}),
    @Signature(
            type = Executor.class,
            method = "flushStatements",
            args = {}),
    @Signature(
            type = Executor.class,
            method = "commit",
            args = {boolean.class}),
    @Signature(
            type = Executor.class,
            method = "rollback",
            args = {boolean.class}),
    @Signature(
            type = Executor.class,
            method = "close",
            args = {boolean.class})
})
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class DataChangeLogPlugin implements Interceptor {
    @Autowired private ApplicationEventPublisher applicationEventPublisher;
    private final Map<Executor, Buffer> buffers = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Executor executor = (Executor) invocation.getTarget();
        String method = invocation.getMethod().getName();
        if (method.equals("rollback") || method.equals("close")) {
            Buffer buffer = buffers.remove(executor);
            if (buffer != null && method.equals("rollback")) buffer.events.clear();
            return invocation.proceed();
        }
        if (method.equals("flushStatements")) {
            @SuppressWarnings("unchecked")
            List<BatchResult> results = (List<BatchResult>) invocation.proceed();
            finishBatch(executor, results);
            return results;
        }
        if (method.equals("commit")) {
            finishBatch(executor, executor.flushStatements());
            Object result = invocation.proceed();
            Buffer buffer = buffers.get(executor);
            if (buffer != null && !buffer.springTransaction) {
                publish(buffer);
                buffers.remove(executor);
            }
            return result;
        }
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        BoundSql sql = ms.getBoundSql(parameter);
        SqlParseHelper.SqlContext parsed = SqlParseHelper.parse(sql.getSql());
        String table = parsed.getTable().getName();
        Class<?> entity = DataChangeLogTableRegistry.getClass(table);
        if (entity == null) return invocation.proceed();
        StatementType type =
                switch (ms.getSqlCommandType()) {
                    case INSERT -> StatementType.INSERT;
                    case UPDATE -> StatementType.MODIFY;
                    case DELETE -> StatementType.REMOVE;
                    default -> throw new IllegalArgumentException("Unsupported audited statement");
                };
        DataChangeLog annotation = entity.getAnnotation(DataChangeLog.class);
        if (annotation != null && !Arrays.asList(annotation.record()).contains(type))
            return invocation.proceed();
        if (type == StatementType.INSERT) {
            Object inserted = entityParameter(parameter, entity);
            if (inserted == null)
                throw new IllegalArgumentException("Audited inserts require an entity parameter");
            if (ReflectUtils.getFieldValue(inserted, primaryProperty(entity)) == null
                    && ms.getKeyGenerator()
                            instanceof org.apache.ibatis.executor.keygen.NoKeyGenerator) {
                throw new IllegalArgumentException(
                        "Audited inserts require an assigned ID or configured generated-key"
                                + " backfill");
            }
        }
        Connection connection = executor.getTransaction().getConnection();
        if (isBatchExecutor(executor) && connection.getAutoCommit()) {
            throw new IllegalStateException(
                    "Audited batch operations require an explicit transaction (autoCommit=false)");
        }
        List<Map<String, Object>> before =
                type == StatementType.INSERT ? List.of() : beforeRows(ms, sql, parsed, connection);
        Pending pending = new Pending(ms, parameter, table, entity, type, before, metadata());
        int rows = (Integer) invocation.proceed();
        if (rows == 0) return rows;
        Buffer buffer = buffer(executor);
        if (rows == BatchExecutor.BATCH_UPDATE_RETURN_VALUE) {
            buffer.pending.add(pending);
        } else {
            finish(executor, buffer, pending);
            if (!buffer.springTransaction && connection.getAutoCommit()) {
                publish(buffer);
                buffers.remove(executor);
            }
        }
        return rows;
    }

    private static boolean isBatchExecutor(Executor executor) {
        Object current = com.baomidou.mybatisplus.core.toolkit.PluginUtils.realTarget(executor);
        java.util.Set<Object> seen =
                java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        while (current != null && seen.add(current)) {
            if (current instanceof BatchExecutor) return true;
            var meta = org.apache.ibatis.reflection.SystemMetaObject.forObject(current);
            current = meta.hasGetter("delegate") ? meta.getValue("delegate") : null;
        }
        return false;
    }

    private Buffer buffer(Executor executor) {
        Buffer buffer = buffers.computeIfAbsent(executor, key -> new Buffer());
        if (!buffer.springTransaction
                && TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            buffer.springTransaction = true;
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void beforeCommit(boolean readOnly) {
                            try {
                                finishBatch(executor, executor.flushStatements());
                            } catch (Exception ex) {
                                throw new IllegalStateException(
                                        "Cannot flush audited MyBatis batch", ex);
                            }
                        }

                        @Override
                        public void afterCommit() {
                            publish(buffer);
                        }

                        @Override
                        public void afterCompletion(int status) {
                            buffers.remove(executor);
                            buffer.events.clear();
                            buffer.pending.clear();
                        }
                    });
        }
        return buffer;
    }

    private void finishBatch(Executor executor, List<BatchResult> results) throws SQLException {
        Buffer buffer = buffers.get(executor);
        if (buffer == null) return;
        for (BatchResult result : results) {
            int[] counts = result.getUpdateCounts();
            for (int i = 0; i < result.getParameterObjects().size(); i++) {
                Object parameter = result.getParameterObjects().get(i);
                Pending change =
                        buffer.pending.stream()
                                .filter(
                                        p ->
                                                p.ms.getId()
                                                                .equals(
                                                                        result.getMappedStatement()
                                                                                .getId())
                                                        && p.parameter == parameter)
                                .findFirst()
                                .orElse(null);
                if (change == null) continue;
                buffer.pending.remove(change);
                if (i < counts.length && (counts[i] > 0 || counts[i] == Statement.SUCCESS_NO_INFO))
                    finish(executor, buffer, change);
            }
        }
    }

    private void finish(Executor executor, Buffer buffer, Pending pending) throws SQLException {
        String primary = primaryColumn(pending.entity);
        List<Map<String, Object>> oldRows = pending.before;
        if (pending.type == StatementType.INSERT) {
            Object entityObject = entityParameter(pending.parameter, pending.entity);
            Object id =
                    entityObject == null
                            ? null
                            : ReflectUtils.getFieldValue(
                                    entityObject, primaryProperty(pending.entity));
            if (id == null)
                throw new IllegalStateException(
                        "Audited inserts must expose their generated or assigned primary key");
            oldRows = List.of(Collections.singletonMap(primary, id));
        }
        for (Map<String, Object> old : oldRows) {
            Object id = old.get(primary);
            if (id == null)
                throw new IllegalStateException("Missing audited primary key: " + primary);
            if (pending.type == StatementType.REMOVE
                    && !rowById(
                                    executor.getTransaction().getConnection(),
                                    pending.table,
                                    primary,
                                    id)
                            .isEmpty()) {
                continue; // A permission plugin may have narrowed the executed delete.
            }
            Map<String, Object> current =
                    pending.type == StatementType.REMOVE
                            ? Map.of()
                            : rowById(
                                    executor.getTransaction().getConnection(),
                                    pending.table,
                                    primary,
                                    id);
            if (pending.type != StatementType.REMOVE && current.isEmpty())
                throw new IllegalStateException("Audited row no longer exists");
            DataChangeLogDTO dto = pending.metadataCopy();
            dto.setId(id);
            dto.setClassName(pending.entity.getName());
            dto.setTableName(pending.table);
            dto.setStatementType(pending.type);
            Set<String> columns = new LinkedHashSet<>(old.keySet());
            columns.addAll(current.keySet());
            for (String column : columns) {
                if (column.equals(primary)) continue;
                Object before = pending.type == StatementType.INSERT ? null : old.get(column);
                Object after = current.get(column);
                if (!Objects.equals(before, after))
                    dto.getFieldInfos()
                            .add(
                                    new DataChangeLogDTO.FieldInfo(
                                            property(pending.entity, column), before, after));
            }
            if (pending.type != StatementType.MODIFY || !dto.getFieldInfos().isEmpty())
                buffer.events.add(dto);
        }
    }

    private List<Map<String, Object>> beforeRows(
            MappedStatement ms,
            BoundSql bound,
            SqlParseHelper.SqlContext parsed,
            Connection connection)
            throws SQLException {
        String where = parsed.getWhere() == null ? "" : " WHERE " + parsed.getWhere();
        // Audit covers single-table DML. Reject shapes that cannot be snapshotted faithfully.
        if (parsed.getStatement() instanceof net.sf.jsqlparser.statement.update.Update update
                && ((update.getStartJoins() != null && !update.getStartJoins().isEmpty())
                        || update.getLimit() != null)) {
            throw new IllegalArgumentException(
                    "Audited UPDATE joins/limits are not supported; use primary-key operations");
        }
        if (parsed.getStatement() instanceof net.sf.jsqlparser.statement.delete.Delete delete
                && ((delete.getJoins() != null && !delete.getJoins().isEmpty())
                        || delete.getLimit() != null)) {
            throw new IllegalArgumentException(
                    "Audited DELETE joins/limits are not supported; use primary-key operations");
        }
        int count = placeholders(where);
        List<ParameterMapping> mappings = bound.getParameterMappings();
        if (count > mappings.size())
            throw new IllegalArgumentException("Invalid audited SQL parameter mappings");
        BoundSql selectBound =
                new BoundSql(
                        ms.getConfiguration(),
                        "SELECT * FROM " + parsed.getTable() + where,
                        mappings.subList(mappings.size() - count, mappings.size()),
                        bound.getParameterObject());
        bound.getAdditionalParameters().forEach(selectBound::setAdditionalParameter);
        try (PreparedStatement statement = connection.prepareStatement(selectBound.getSql())) {
            new DefaultParameterHandler(ms, bound.getParameterObject(), selectBound)
                    .setParameters(statement);
            try (ResultSet rows = statement.executeQuery()) {
                return readRows(rows);
            }
        }
    }

    private static int placeholders(String sql) {
        int count = 0;
        char quote = 0;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == quote) i++;
                    else quote = 0;
                }
            } else if (c == '\'' || c == '"' || c == '`') quote = c;
            else if (c == '?') count++;
        }
        return count;
    }

    private static Map<String, Object> rowById(
            Connection connection, String table, String key, Object id) throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        "SELECT * FROM "
                                + identifier(table)
                                + " WHERE "
                                + identifier(key)
                                + " = ?")) {
            statement.setObject(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                List<Map<String, Object>> result = readRows(rows);
                return result.isEmpty() ? Map.of() : result.get(0);
            }
        }
    }

    private static String identifier(String value) {
        if (!value.matches("[A-Za-z_][A-Za-z0-9_]*"))
            throw new IllegalArgumentException("Unsupported audit identifier: " + value);
        return value;
    }

    private static List<Map<String, Object>> readRows(ResultSet result) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData metadata = result.getMetaData();
        while (result.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= metadata.getColumnCount(); i++)
                row.put(metadata.getColumnName(i).toLowerCase(Locale.ROOT), result.getObject(i));
            rows.add(row);
        }
        return rows;
    }

    private static Field primaryField(Class<?> entity) {
        Field[] fields = ReflectUtils.findFieldWithAnnotation(entity, TableId.class);
        if (fields.length == 0)
            throw new IllegalArgumentException(
                    "Audited entity requires @TableId: " + entity.getName());
        return fields[0];
    }

    private static String primaryColumn(Class<?> entity) {
        TableInfo info = TableInfoHelper.getTableInfo(entity);
        if (info != null) return info.getKeyColumn().toLowerCase(Locale.ROOT);
        Field field = primaryField(entity);
        String column = field.getAnnotation(TableId.class).value();
        return (column.isBlank()
                        ? org.wymix.framework.utils.StringUtils.camelToUnderline(field.getName())
                        : column)
                .toLowerCase(Locale.ROOT);
    }

    private static String primaryProperty(Class<?> entity) {
        return primaryField(entity).getName();
    }

    private static String property(Class<?> entity, String column) {
        TableInfo info = TableInfoHelper.getTableInfo(entity);
        if (info != null)
            return info.getFieldList().stream()
                    .filter(f -> f.getColumn().equalsIgnoreCase(column))
                    .map(f -> f.getProperty())
                    .findFirst()
                    .orElse(column);
        return org.wymix.framework.utils.StringUtils.underlineToCamel(column);
    }

    private static Object entityParameter(Object parameter, Class<?> entity) {
        if (entity.isInstance(parameter)) return parameter;
        if (parameter instanceof Map<?, ?> values)
            return values.values().stream().filter(entity::isInstance).findFirst().orElse(null);
        return null;
    }

    private static DataChangeLogDTO metadata() {
        DataChangeLogDTO dto = new DataChangeLogDTO();
        dto.setRequestId(GlobalIdGenerator.generateNextId());
        dto.setLastModifiedDate(LocalDateTime.now());
        dto.setLastModifiedBy(
                UserContextHolder.getUserContext() == null
                        ? "SYSTEM"
                        : UserContextHolder.getUserContext().getPrincipal());
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {
            dto.setUri(attributes.getRequest().getRequestURI());
            dto.setRequestRemoteIp(HttpUtils.getClientIpAddress(attributes.getRequest()));
        }
        return dto;
    }

    private void publish(Buffer buffer) {
        List<DataChangeLogDTO> events = new ArrayList<>(buffer.events);
        buffer.events.clear();
        for (DataChangeLogDTO event : events)
            applicationEventPublisher.publishEvent(new DataChangeLogEvent(this, event));
    }

    private static final class Buffer {
        boolean springTransaction;
        final List<Pending> pending = new ArrayList<>();
        final List<DataChangeLogDTO> events = new ArrayList<>();
    }

    private record Pending(
            MappedStatement ms,
            Object parameter,
            String table,
            Class<?> entity,
            StatementType type,
            List<Map<String, Object>> before,
            DataChangeLogDTO metadata) {
        DataChangeLogDTO metadataCopy() {
            DataChangeLogDTO dto = new DataChangeLogDTO();
            dto.setRequestId(metadata.getRequestId());
            dto.setUri(metadata.getUri());
            dto.setRequestRemoteIp(metadata.getRequestRemoteIp());
            dto.setLastModifiedBy(metadata.getLastModifiedBy());
            dto.setLastModifiedDate(metadata.getLastModifiedDate());
            return dto;
        }
    }
}

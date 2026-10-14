package org.wymix.framework.repository.datachangelog.supports;

import org.hibernate.EmptyInterceptor;
import java.util.Optional;
import org.wymix.framework.repository.datachangelog.enums.StatementType;

/** Legacy type retained for linkage only. Hibernate 6 does not call the removed prepare hook. */
@Deprecated(since = "3.0", forRemoval = false)
public class ManualDmlDataChangeInterceptor extends EmptyInterceptor {
    public static String replaceQuestionMarks(String sql) {
        throw new UnsupportedOperationException("JPA manual DML audit has been retired; use mybatis_repository");
    }
    public Optional<Class<?>> getEntityClassByTableName(String tableName, StatementType statementType) {
        throw new UnsupportedOperationException("JPA manual DML audit has been retired; use mybatis_repository");
    }
}

package org.wymix.framework.test.annotations;

import org.wymix.framework.test.enums.Operation;

public @interface DataSet {

    String database() default "test";

    String[] values();

    String[] dataSourceNames() default "dataSource";

    Operation setup() default Operation.INSERT;

    Operation teardown() default Operation.DELETE;

}

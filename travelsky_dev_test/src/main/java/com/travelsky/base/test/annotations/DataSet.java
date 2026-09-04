package com.travelsky.base.test.annotations;

import com.travelsky.base.test.enums.Operation;

public @interface DataSet {

    String database() default "test";

    String[] values();

    String[] dataSourceNames() default "dataSource";

    Operation setup() default Operation.INSERT;

    Operation teardown() default Operation.DELETE;

}

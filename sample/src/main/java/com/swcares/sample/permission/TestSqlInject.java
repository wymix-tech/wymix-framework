//package com.swcares.sample.permission;
//
//import com.travelsky.base.repository.datapermission.enums.InjectType;
//import com.travelsky.base.repository.datapermission.inject.PermissionSqlInjector;
//
//import java.util.Collections;
//import java.util.Map;
//
///*
//不用写@Component,实现PermissionSqlInjector后会自动注入
// */
//public class TestSqlInject implements PermissionSqlInjector {
//    @Override
//    public Map<String, Object> additionalParameters() {
//        Map<String, Object> stringObjectMap = Collections.emptyMap();
//        /*
//        stringObjectMap.put("column1",1);
//        stringObjectMap.put("column2",2);
//         */
//        return stringObjectMap;
//    }
//
//    @Override
//    public String injectSql() {
//        /*
//        type() == REPLACE_TABLE_NAME
//        return "A->B";
//         */
//
//        /*
//        type() == WHERE_ADDITIONAL
//        return "column1=#{column1} and column2=#{column2}";    #{column1} #{column2} 值对应放在additionalParameters中返回的map中 column1=xxx,column2=yyyy
//         */
//
//        return "";
//    }
//
//    @Override
//    public InjectType type() {
//        /*
//        InjectType 分为两种
//        1. REPLACE_TABLE_NAME  修改表名   此时可以在injectSql()方法中按照格式  A -> B  的方式 将执行的sql中的A表替换为B表
//        2. WHERE_ADDITIONAL  附加where条件
//         */
//
//        return null;
//    }
//}

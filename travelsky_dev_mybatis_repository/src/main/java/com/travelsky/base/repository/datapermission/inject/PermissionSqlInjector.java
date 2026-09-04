package com.travelsky.base.repository.datapermission.inject;

import com.travelsky.base.repository.datapermission.enums.InjectType;

import java.util.Map;

/**
 * 实现类若是需要注入某些属性，属性需要有set方法，如 @Autowired private ApplicationContext context; public void setContext(ApplicationContext context){...}
 */
public interface PermissionSqlInjector {

    Map<String, Object> additionalParameters();

    /**
     * 表名替换格式：XXXX->XXXX
     * 新增筛选条件：xxx = :xxx  and yyy='123', :xxx 参数占位符，在additionalParameters()的返回中新增key,value => xxx,ABC
     *
     * @return
     */
    String injectSql();

    InjectType type();
}

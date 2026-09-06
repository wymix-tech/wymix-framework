package com.travelsky.base.repository.datachangelog.annotations;

/**
 * @deprecated 旧包名兼容别名，请迁移到 {@link com.travelsky.base.repository.mybatis.datachangelog.annotations.EnableDataChangeLog}，
 *             计划于下个大版本移除。该注解本身已废弃（forRemoval），新代码请直接依赖自动装配。
 */
@Deprecated(since = "3.x", forRemoval = true)
public @interface EnableDataChangeLog {
}

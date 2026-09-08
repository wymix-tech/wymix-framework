-- sample 示例库表结构（MySQL 8）
--
-- 列说明：
--   ID                  MyBatis 侧由 mybatis-plus 的 IdType.ASSIGN_ID（GlobalIdGenerator 雪花算法）赋值；
--                       JPA 侧使用 GenerationType.IDENTITY，因此需要 AUTO_INCREMENT。
--                       显式赋值时 AUTO_INCREMENT 不生效，故同一张表可同时服务两种 ORM。
--   CREATED_BY / CREATED_TIME / LAST_MODIFIED_BY / LAST_MODIFIED_TIME
--                       审计字段，由框架的 AbstractAuditEntity + AuditFieldFillHandler(MyBatis) /
--                       AuditListener(JPA) 自动填充，取值来自 UserContextHolder（缺省 SYSTEM）。
CREATE DATABASE IF NOT EXISTS `adt_dev` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `adt_dev`;

CREATE TABLE IF NOT EXISTS `SAMPLE_USER`
(
    `ID`                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `USERNAME`            VARCHAR(64) NULL COMMENT '用户名',
    `PASSWORD`            VARCHAR(128) NULL COMMENT '密码',
    `CREATED_BY`          VARCHAR(64) NULL COMMENT '创建人',
    `CREATED_TIME`        DATETIME    NULL COMMENT '创建时间',
    `LAST_MODIFIED_BY`    VARCHAR(64) NULL COMMENT '修改人',
    `LAST_MODIFIED_TIME`  DATETIME    NULL COMMENT '修改时间',
    PRIMARY KEY (`ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='示例用户表';

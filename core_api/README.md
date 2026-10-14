# core_api

轻量公共契约模块，不引入 Feign、LoadBalancer、线程池或安全 starter。

- 保留既有 `org.wymix.framework.core.api.BaseService`、`core.audit.AbstractBaseAuditEntity` 和 `core.context.ApplicationContextHolder` 包名，避免业务 import 迁移。
- 自动注册可覆盖的 ApplicationContextHolder；容器关闭时清理其拥有的静态引用。该兼容入口仍只代表一个当前上下文，多容器应用应使用注入。
- `repository/security/monitor/web` 依赖此模块；需要框架异步、调度、Feign、日志监听器或邮件时另行引入 `core`。

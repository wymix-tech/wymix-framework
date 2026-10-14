# wymix_dev 快速开发框架

> 企业级 Java 快速开发框架 · 基于 Spring Boot 3.5.8 + Java 17 · Maven 多模块架构
>
> Maven 坐标：`org.wymix.framework:dev_parent:${revision}`，当前 `<revision>` = `3.0.0-SNAPSHOT`

---

## 文档导航

| 文档 | 说明 |
|------|------|
| [QUICK_START.md](./QUICK_START.md) | **快速上手指南**：环境、依赖引入、最小 CRUD 示例、配置、FAQ |
| [FRAMEWORK_DESIGN.md](./FRAMEWORK_DESIGN.md) | **总框架设计文档**：分层架构、模块职责、关键设计决策（ADR）、技术栈、已知风险 |
| [FILTER_USAGE_GUIDE.md](./FILTER_USAGE_GUIDE.md) | 查询 DSL 前端使用指南（含 AI 提示词模板） |
| [mybatis_repository/DevQueryVO用法.md](./mybatis_repository/DevQueryVO用法.md) | 查询 DSL 后端视角说明（filter / sorts 语法） |

---

## 框架结构

```
dev_parent
├── 基础层
│   ├── utils            工具类模块
│   ├── threadpool       线程池管理模块
│   └── common           通用类封装模块
├── 核心层
│   └── core             框架核心（启动/异步/Feign/邮件）
├── 数据层
│   ├── repository       数据访问抽象层（ORM 无关）
│   ├── jpa_repository   基于 JPA 的数据库交互模块
│   ├── mybatis_repository  基于 MyBatis 的数据库交互模块
│   ├── dynamic_datasource  动态数据源模块
│   └── data_migration   数据迁移模块
├── 基础设施层
│   ├── security         认证模块
│   ├── cache            缓存模块
│   ├── monitor          监控模块
│   └── decentralization_discovery  去中心化服务发现模块
└── 应用层
    ├── web              WEB 模块
    ├── test             单元测试依赖模块
    └── ai              AI 能力模块（占位/空壳）
```

## 辅助项目

- `code-generator`：基于 Groovy 的代码生成器（数据库表 → 全栈 CRUD 代码）
- `sample`：端到端可运行示例 / 项目脚手架

---

## 各模块 README

| 模块 | README |
|------|--------|
| utils | [utils/README.md](./utils/README.md) |
| threadpool | [threadpool/README.md](./threadpool/README.md) |
| common | [common/README.md](./common/README.md) |
| core | [core/README.md](./core/README.md) |
| repository | [repository/README.md](./repository/README.md) |
| jpa_repository | [jpa_repository/README.md](./jpa_repository/README.md) |
| mybatis_repository | [mybatis_repository/README.md](./mybatis_repository/README.md) |
| dynamic_datasource | [dynamic_datasource/README.md](./dynamic_datasource/README.md) |
| data_migration | [data_migration/README.md](./data_migration/README.md) |
| security | [security/README.md](./security/README.md) |
| cache | [cache/README.md](./cache/README.md) |
| monitor | [monitor/README.md](./monitor/README.md) |
| decentralization_discovery | [decentralization_discovery/README.md](./decentralization_discovery/README.md) |
| web | [web/README.md](./web/README.md) |
| test | [test/README.md](./test/README.md) |
| ai | [ai/README.md](./ai/README.md) |
| code-generator | [code-generator/README.md](./code-generator/README.md) |
| sample | [sample/README.md](./sample/README.md) |

---

## 快速开始

### 1. 引入依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.wymix.framework</groupId>
        <artifactId>web</artifactId>
        <version>${revision}</version>
    </dependency>
    <dependency>
        <groupId>org.wymix.framework</groupId>
        <artifactId>mybatis_repository</artifactId>
        <version>${revision}</version>
    </dependency>
</dependencies>
```

> `web` 的 pom 直接 compile 依赖 `security`，**仅引入 `web` 即会默认开启安全装配**
> （`security.enabled` 默认 `matchIfMissing = true`）。暂不需要认证时请显式配置 `security.enabled: false`。

### 2. 创建启动类

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {
    public static void main(String[] args) {
        // 原生启动即可：框架监听器已由 META-INF/spring.factories 自动注册
        SpringApplication.run(Application.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Application.class);
    }
}
```

> 更详细的「最小 CRUD 示例」与配置说明见 [QUICK_START.md](./QUICK_START.md)。

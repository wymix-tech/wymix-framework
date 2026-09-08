# travelsky_dev 快速开发框架（APD）

> 航信（TravelSky）企业级 Java 快速开发框架 · 基于 Spring Boot 3.5.x + Java 17 · Maven 多模块架构

---

## 文档导航

| 文档 | 说明 |
|------|------|
| [QUICK_START.md](./QUICK_START.md) | **快速上手指南**：环境、依赖引入、最小 CRUD 示例、配置、FAQ |
| [FRAMEWORK_DESIGN.md](./FRAMEWORK_DESIGN.md) | **总框架设计文档**：分层架构、模块职责、关键设计决策（ADR）、技术栈、已知风险 |
| [FILTER_USAGE_GUIDE.md](./FILTER_USAGE_GUIDE.md) | 查询 DSL 前端使用指南（含 AI 提示词模板） |
| [project-evaluation-report.html](./project-evaluation-report.html) | 框架评估报告（功能清单 + 优缺点 + 改进建议） |

---

## 框架结构

```
travelsky_dev_parent
├── 基础层
│   ├── travelsky_dev_utils            工具类模块
│   ├── travelsky_dev_threadpool       线程池管理模块
│   └── travelsky_dev_common           通用类封装模块
├── 核心层
│   └── travelsky_dev_core             框架核心（启动/异步/Feign/邮件）
├── 数据层
│   ├── travelsky_dev_repository       数据访问抽象层（ORM 无关）
│   ├── travelsky_dev_jpa_repository   基于 JPA 的数据库交互模块
│   ├── travelsky_dev_mybatis_repository  基于 MyBatis 的数据库交互模块
│   ├── travelsky_dev_dynamic_datasource  动态数据源模块
│   └── travelsky_dev_data_migration   数据迁移模块
├── 基础设施层
│   ├── travelsky_dev_security         认证模块
│   ├── travelsky_dev_cache            缓存模块
│   ├── travelsky_dev_monitor          监控模块
│   └── travelsky_dev_decentralization_discovery  去中心化服务发现模块
└── 应用层
    ├── travelsky_dev_web              WEB 模块
    ├── travelsky_dev_test             单元测试依赖模块
    └── travelsky_dev_ai              AI 能力模块（占位/空壳）
```

## 辅助项目

- `code-generator`：基于 Groovy 的代码生成器（数据库表 → 全栈 CRUD 代码）
- `sample`：端到端可运行示例 / 项目脚手架

---

## 各模块 README

| 模块 | README |
|------|--------|
| utils | [travelsky_dev_utils/README.md](./travelsky_dev_utils/README.md) |
| threadpool | [travelsky_dev_threadpool/README.md](./travelsky_dev_threadpool/README.md) |
| common | [travelsky_dev_common/README.md](./travelsky_dev_common/README.md) |
| core | [travelsky_dev_core/README.md](./travelsky_dev_core/README.md) |
| repository | [travelsky_dev_repository/README.md](./travelsky_dev_repository/README.md) |
| jpa_repository | [travelsky_dev_jpa_repository/README.md](./travelsky_dev_jpa_repository/README.md) |
| mybatis_repository | [travelsky_dev_mybatis_repository/README.md](./travelsky_dev_mybatis_repository/README.md) |
| dynamic_datasource | [travelsky_dev_dynamic_datasource/README.md](./travelsky_dev_dynamic_datasource/README.md) |
| data_migration | [travelsky_dev_data_migration/README.md](./travelsky_dev_data_migration/README.md) |
| security | [travelsky_dev_security/README.md](./travelsky_dev_security/README.md) |
| cache | [travelsky_dev_cache/README.md](./travelsky_dev_cache/README.md) |
| monitor | [travelsky_dev_monitor/README.md](./travelsky_dev_monitor/README.md) |
| decentralization_discovery | [travelsky_dev_decentralization_discovery/README.md](./travelsky_dev_decentralization_discovery/README.md) |
| web | [travelsky_dev_web/README.md](./travelsky_dev_web/README.md) |
| test | [travelsky_dev_test/README.md](./travelsky_dev_test/README.md) |
| ai | [travelsky_dev_ai/README.md](./travelsky_dev_ai/README.md) |
| code-generator | [code-generator/README.md](./code-generator/README.md) |
| sample | [sample/README.md](./sample/README.md) |

---

## 快速开始

### 1. 引入依赖

```xml
<dependencies>
    <dependency>
        <groupId>com.travelsky.base</groupId>
        <artifactId>travelsky_dev_web</artifactId>
    </dependency>
    <dependency>
        <groupId>com.travelsky.base</groupId>
        <artifactId>travelsky_dev_mybatis_repository</artifactId>
    </dependency>
</dependencies>
```

### 2. 创建启动类

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;

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

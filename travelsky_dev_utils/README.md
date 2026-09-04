# travelsky_dev_utils 基础工具模块

> 全框架最底层的通用工具集，被除自身外所有模块依赖，无自动配置、纯工具类。

## 模块说明

`travelsky_dev_utils` 提供与具体业务无关的通用 Java 工具能力，是 `com.travelsky.base` 体系的基础设施层。它不引入任何 Spring Boot 自动配置，仅以普通依赖方式被上层模块引用，做到「引入即生效」。

## 功能说明

| 能力 | 关键类 | 说明 |
|------|--------|------|
| JSON 序列化 | `NumberSerializer` 等 Jackson 定制序列化器 | 默认将 `Long` 序列化为字符串，规避前端 JS `Number` 精度丢失（与 Web 模块的 Long→String 互相对应） |
| 雪花 ID 生成 | 分布式唯一 ID 生成工具 | 生成全局唯一、趋势递增的 ID |
| 反射工具 | `ReflectionUtils` | `getSuperClassGenericType`（解析泛型实参）、`findFieldWithAnnotation`（按注解找字段）、`newInstance`（实例化） |
| 网络 / 日期工具 | 各类辅助方法 | IP 解析、时间格式化等常见操作 |

## 配置项

本模块没有外部配置项，也不依赖 Spring 容器。

## 使用示例

```java
// 1) 反射解析泛型（框架 CRUD 基类内部大量使用）
Class<?> entityClass = ReflectionUtils.getSuperClassGenericType(this.getClass(), 0);

// 2) 按注解查找实体 ID 字段
Field idField = ReflectionUtils.findFieldWithAnnotation(entityClass, Id.class);

// 3) 实例化泛型实参对应的类
Object vo = ReflectionUtils.newInstance(this.getClass(), 1);

// 4) Long 精度：核心模块已全局注册 Long->String 序列化器，
//    前端收到的 id 为字符串，写回时 Web 模块再转回 Long。
```

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_utils</artifactId>
    <version>${revision}</version>
</dependency>
```

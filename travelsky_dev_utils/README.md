# travelsky_dev_utils 基础工具模块

> 全框架最底层的通用工具集，被其余绝大多数模块依赖，无自动配置、纯工具类。

## 模块说明

`travelsky_dev_utils` 提供与具体业务无关的通用 Java 工具能力，是 `com.travelsky.base` 体系的基础设施层。它不引入任何 Spring Boot 自动配置，仅以普通依赖方式被上层模块引用，做到「引入即生效」。

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_utils</artifactId>
    <version>${revision}</version>
</dependency>
```

## 配置项

本模块没有外部配置项，也不依赖 Spring 容器。

## 集成类

无自动配置。除 `JsonDeserialize` 为需继承并指定泛型实参的基类外，其余均为静态工具类，直接调用即可。

## 工具类清单（调用入口）

| 类 | 说明 |
|----|------|
| `GlobalIdGenerator` | 雪花 ID：`generateNextId()` / `generateNextIdStr()`；支持按 IP/MAC 推导 datacenterId 与 workerId |
| `ReflectUtils` | 反射：`getFieldsDirectly`/`getFieldValue`（字段读取，带缓存）、`getSuperClassGenericType`（解析泛型实参）、`findFieldWithAnnotation`（按注解找字段）、`newInstance`（按泛型位置实例化） |
| `JsonUtils` | JSON 序列化/反序列化：`entityToJson` / `jsonToEntity` / `entityToJsonWithDefaultDateformat` / `getValue`（按属性名提取 JSON 字段值）等 |
| `NumberSerializer` / `DateSerializer` / `DateDeserializer` | Jackson 定制序列化器，数值超过 `100000000000000` 时输出为字符串，规避前端精度丢失 |
| `JsonDeserialize` | 泛型 JSON 反序列化基类（继承时指定泛型实参后调用 `jsonToEntity`） |
| `BeanUtils` | Bean 属性拷贝 |
| `StringUtils` | 字符串工具（含 `generateRandomString` 随机串） |
| `DateUtils` / `LocalDateTimeUtils` | 日期/时间工具 |
| `HttpUtils` | HTTP 工具（如响应写出 `response(...)`） |
| `NetUtils` | 网络工具（IP 解析等） |
| `CompressUtil` | 压缩/解压工具（GZIP + Base64） |
| `CurrentTimestamp` / `PriorityUtils` / `UUID` | 时间戳（基于后台线程定时刷新的缓存时钟，枚举单例）、优先级、UUID 工具 |
| `SafeConcurrentHashMap` / `WeakConcurrentMap` / `ReferenceConcurrentMap` | 并发安全 Map 变体 |

## 继承类 / 扩展点

无继承体系；扩展方式为直接调用上述静态工具。模块内不提供 Jackson `Serializer`/`Deserializer` 的统一注册入口。

## 使用示例

```java
// 1) 雪花 ID
long id = GlobalIdGenerator.generateNextId();
String idStr = GlobalIdGenerator.generateNextIdStr();

// 2) 反射解析泛型
Class<?> entityClass = ReflectUtils.getSuperClassGenericType(this.getClass(), 0);

// 3) 按注解查找实体 ID 字段
Field[] idFields = ReflectUtils.findFieldWithAnnotation(entityClass, Id.class);

// 4) 实例化泛型实参对应的类
Object vo = ReflectUtils.newInstance(this.getClass(), 1);

// 5) JSON 工具
String json = JsonUtils.entityToJson(obj);

// 6) Long 精度：entityToJsonWithDefaultDateformat 内部注册 NumberSerializer，
//    数值超过 100000000000000 时序列化为字符串
```

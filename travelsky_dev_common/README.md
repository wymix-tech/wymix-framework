# travelsky_dev_common 通用模型模块

> 定义全局统一的返回结构、分页查询模型、审计 VO 与安全上下文，是 Web / Repository 层共享的契约。

## 模块说明

`travelsky_dev_common` 提供跨层共享的通用数据结构与上下文工具：统一响应体 `ResultVO`、分页查询模型 `DevQueryVO` / `Page` / `SortVO`、审计 VO 基类 `AbstractAuditVO`、安全上下文 `UserContextHolder` / `Principal`、业务异常 `BusinessException` 等。它是 Web 层与 Repository 层之间的「数据契约」，被两者共同依赖。

## 功能说明

| 能力 | 关键类 | 说明 |
|------|--------|------|
| 统一返回 | `ResultVO<T>` | `statusCode` / `payload` / `errorMessage` / `invalidMessage`，静态工厂 `ResultVO.of(...)` |
| 分页查询模型 | `DevQueryVO` / `Page<T>` / `SortVO` | `DevQueryVO` 携带 filter/sorts/page/limit；`Page` 为分页结果并支持 `map()` VO 转换 |
| 审计 VO | `AbstractAuditVO` | VO 层审计字段基类（createdBy/createdTime/lastModifiedBy/lastModifiedTime） |
| 安全上下文 | `UserContextHolder` / `Principal` / `IgnoreUrlContext` | 持有当前登录人信息与白名单上下文 |
| 异常 | `BusinessException` / `LoginException` | 业务异常与登录异常基类 |

## 配置项

本模块没有外部配置项。

## 使用示例

```java
// 1) 统一返回
return ResultVO.of(userVO);                 // 成功
return ResultVO.of(401, "未登录");           // 错误

// 2) 分页结果转换（Web / Service 基类内部使用）
Page<UserVO> voPage = userPage.map(user -> convertToVO(user));

// 3) 获取当前登录人
Principal principal = UserContextHolder.getPrincipal();
String userId = principal.getUserId();

// 4) 构造查询条件（后端方式，避免手写 filter 字符串）
DevQueryVO query = new DevQueryVO();
query.addFilters(Triple.of("status", OperateSymbol.EQ, "ACTIVE"),
                 Triple.of("age", OperateSymbol.GE, "18"));
```

## 依赖引入

```xml
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_common</artifactId>
    <version>${revision}</version>
</dependency>
```

# travelsky_dev_ai AI 模块

> 框架预留的 AI 能力集成模块（当前为占位/空壳）。

## 模块说明

`travelsky_dev_ai` 是框架为「AI 能力集成」预留的模块，规划用于封装大模型调用、智能助手、AI 辅助查询等能力的统一接入。

> ⚠️ **当前状态：空壳模块**。目前仅包含 `pom.xml` 坐标与基本依赖声明，**没有任何可运行的源代码**，也未注册任何自动配置。请勿在生产中直接依赖本模块，或仅作为后续 AI 能力落地的容器。

## 功能说明

- 规划方向（待实现）：统一的大模型客户端封装、Prompt 模板管理、与 `DevQueryVO` 查询 DSL 结合的「自然语言 → filter」能力、AI 辅助代码/文档生成等。
- 现状：仅骨架，无对外 API。

## 配置项

无（模块暂无实现）。

## 使用示例

暂不可用。建议参考 `FILTER_USAGE_GUIDE.md` 中已提供的「AI / 低代码提示词模板」，该能力目前以文档形式提供，可直接用于让大模型把自然语言转为正确的 `filter` 查询字符串。

## 依赖引入

```xml
<!-- 当前为空壳，引入不会带来任何能力 -->
<dependency>
    <groupId>com.travelsky.base</groupId>
    <artifactId>travelsky_dev_ai</artifactId>
    <version>${revision}</version>
</dependency>
```

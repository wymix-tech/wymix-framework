# catalog —— 脚手架平台配置

四段配置，放 jar 同级 `catalog/` 目录，重启即生效。

| 文件 | 作用 |
|---|---|
| `metadata.yml` | Java / Boot 版本矩阵、框架父 POM 坐标 |
| `starters.yml` | 能力清单，单文件多文档，`---` 分隔，15 段 |
| `templates.yml` | 模板清单，单文件多文档，`---` 分隔，13 段 |
| `conflict-rules.yml` | 冲突规则（当前为空集） |

`starters.yml` 中 `linkages[].source` 与 `templates.yml` 中 `name` 逐字对应，
一一配对，无悬空引用。

# 员工智能助理 A 阶段接口文档

## 适用范围与约定

- 本文只覆盖 A 阶段已实现的接口。营业额、异常入库、面试人数、知识库及业务操作尚无接口。
- 路径均为后端 Controller 路径；通过网关访问时，另加部署环境的 API 前缀。均使用现有管理端登录令牌和租户上下文，不接受客户端自行提交 `tenantId`。
- 管理接口要求当前租户的 `tenant_admin` 或 `super_admin` 角色；对话接口要求已登录，其中除 `availability` 外还要求当前租户开关开启、账号启用并已开通助理。
- 响应沿用项目 `CommonResult<T>`，`data` 字段为下表所列内容；失败时使用项目统一错误结构。分页 `data` 为 `PageResult<T>`，包含 `list` 和 `total`。
- `modelBaseUrl` 是内网 HTTP 的 OpenAI 兼容服务根地址，例如 `http://192.168.1.10:8000/v1`。服务端调用其 `/chat/completions`，禁止公网地址、HTTPS、重定向和代理。

## 管理接口

| 方法与路径 | 用途 | 请求 | `data` |
| --- | --- | --- | --- |
| `GET /assistant/admin/overview` | 当前租户总览 | 无 | `enabled` 布尔值、`modelConfigured` 布尔值、`lastModelTestOutcome` 字符串、`enabledUsers` 数量、`requests7d` 数量、`failures7d` 数量 |
| `GET /assistant/admin/config` | 读取当前租户配置 | 无 | `tenantId`、`enabled`、`modelBaseUrl`、`modelName`、`retentionDays`、`requestsPerMinute`；尚未保存时返回关闭状态及默认值 30 天、10 次/分钟 |
| `PUT /assistant/admin/config` | 保存当前租户配置 | JSON，见下方 | `true` |
| `POST /assistant/admin/model/test` | 测试已保存的内网模型配置 | 无 | 模型返回的测试回答字符串；连接失败返回统一错误 |
| `GET /assistant/admin/users` | 员工开通状态分页 | `pageNo` 默认 1；`pageSize` 默认 20、上限 100；可选 `username`、`deptId` | `list` 项为 `id`、`username`、`nickname`、`deptId`、`status`、`enabled`；另有 `total` |
| `PUT /assistant/admin/users/grant` | 批量开通或关闭 | JSON，见下方 | `true` |
| `GET /assistant/admin/audit` | 最近审计元数据 | `limit` 默认 50、范围 1–200 | 数组项为 `id`、`user_id`、`username`、`nickname`、`action`、`outcome`、`model_name`、`latency_ms`、`create_time`；用户已删除时名称可为空；不含问答正文 |

配置请求：

```json
{
  "enabled": true,
  "modelBaseUrl": "http://192.168.1.10:8000/v1",
  "modelName": "Qwen3-32B",
  "retentionDays": 30,
  "requestsPerMinute": 10
}
```

`retentionDays` 范围 1–365；`requestsPerMinute` 范围 1–120。开启时必须提供地址和模型名称。测试接口使用已经保存的配置，建议保存后测试，再开启总开关。

开通请求：

```json
{"userIds": [101, 102], "enabled": true}
```

一次限 1–100 个当前租户的员工账号 ID；即使已配置开通，账号本身被禁用时仍无法使用助理。`enabled: false` 为关闭权限；管理员关闭后，新问答立即拒绝。

## 员工对话接口

| 方法与路径 | 用途 | 请求 | `data` |
| --- | --- | --- | --- |
| `GET /assistant/chat/availability` | 悬浮窗判断当前员工是否可用 | 无 | 布尔值；未开通返回 `false` |
| `POST /assistant/chat/ask` | 新建或继续会话并提问 | JSON，见下方 | `conversationId` 数字、`answer` 字符串 |
| `GET /assistant/chat/conversations` | 本人最近会话 | 无 | 最多 50 项，每项 `id`、`title`、`create_time`、`update_time` |
| `GET /assistant/chat/messages` | 本人会话消息 | 必填 `conversationId` 数字 | 最多最近 200 项，按时间正序；每项 `id`、`role`、`content`、`create_time` |

新会话提问：

```json
{"question": "请解释什么是库存周转率"}
```

继续会话时增加 `conversationId`。`question` 为非空字符串，最多 1000 字。服务端对已有会话校验当前租户及本人归属；读取历史消息也执行相同校验。每位员工每分钟请求数受本租户配置限制。A 阶段仅是通用模型问答，没有业务数据查询能力。

## 对接与安全边界

1. 前端先调用 `availability`，为 `true` 才显示悬浮窗；不能把悬浮窗隐藏当成授权控制，服务端每次提问和历史读取都会重新校验。
2. 管理页面应使用现有角色和菜单权限；`GET /assistant/admin/audit` 仅供租户管理员查看操作元数据。
3. 管理员调阅员工对话正文未实现；员工只能查看自己的会话。问题和回答保存在助理消息表中，按本租户保存期限清理。
4. 当前模型连接没有凭据字段；部署时需在内网限制模型服务访问，并核对服务端日志与缓存的隔离策略。

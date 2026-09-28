# 员工智能助理 B 阶段：独立知识库接口

## 前提

- 在 A 阶段迁移后执行 `sql/mysql/assistant-stage-b-20260926.sql`，再部署本阶段前后端。
- 接口沿用管理端登录令牌和当前租户上下文，不接收客户端提供的租户 ID。响应沿用 `CommonResult<T>`，下表只描述 `data`。
- 所有 `/assistant/admin/knowledge` 接口仅允许当前租户的 `tenant_admin` 或 `super_admin`。员工通过原有 `/assistant/chat/ask` 使用知识，不直接读取文档或索引。
- 向量模型必须在内网提供 OpenAI 兼容 `/embeddings` 接口；与生成回答的模型分别配置。现有 Qwen 对话模型名称不能直接代替向量模型名称。

## 配置与知识库

| 方法与路径 | 请求 | `data` |
| --- | --- | --- |
| `GET /assistant/admin/knowledge/embedding-config` | 无 | `baseUrl`、`modelName`；尚未配置时均为空 |
| `PUT /assistant/admin/knowledge/embedding-config` | `{"baseUrl":"http://127.0.0.1:8000/v1","modelName":"内网向量模型名"}` | `true` |
| `POST /assistant/admin/knowledge/embedding-test` | 无，使用已保存配置 | `连接成功` 或统一错误 |
| `GET /assistant/admin/knowledge/bases` | 无 | 本租户知识库数组，含 `id`、`name`、`description`、`visibility`、`enabled`、`owner_user_id`、`document_count` 与时间 |
| `POST /assistant/admin/knowledge/bases` | 知识库配置 JSON | 新知识库 ID |
| `PUT /assistant/admin/knowledge/bases/{id}` | 知识库配置 JSON | `true` |

知识库配置示例：

```json
{"name":"员工手册","description":"人事制度与工作流程","visibility":"restricted","enabled":true}
```

`visibility` 为 `all`（当前租户所有已开通员工）或 `restricted`（按授权清单）。即使设为 `all`，未开通助理或已停用账号也不能访问。

## 授权与文档

| 方法与路径 | 请求 | `data` |
| --- | --- | --- |
| `GET /assistant/admin/knowledge/bases/{id}/grants` | 无 | `principal_type` 与 `principal_id` 数组 |
| `PUT /assistant/admin/knowledge/bases/{id}/grants` | 授权清单 JSON，整体替换 | `true` |
| `GET /assistant/admin/knowledge/bases/{id}/documents` | 无 | 文档元数据数组：ID、文件名、版本、启停、索引状态、时间；不返回正文 |
| `POST /assistant/admin/knowledge/bases/{id}/documents` | `multipart/form-data`，`file` 必填；`documentId` 可选，表示更新该文档 | 文档 ID |
| `PUT /assistant/admin/knowledge/bases/{id}/documents/{documentId}/enabled?enabled=false` | `enabled` 布尔值 | `true` |
| `DELETE /assistant/admin/knowledge/bases/{id}/documents/{documentId}` | 无 | `true` |

授权清单示例：

```json
{"userIds":[141],"deptIds":[102],"roleIds":[5]}
```

三类对象均须属于当前租户，单类最多 100 个。`roleIds` 使用现有系统角色作为授权群组，角色停用后不再授予文档访问权。撤权、文档停用、知识库停用后，新检索会拒绝该文档；历史回答带有来源关联，读取时再次校验。

上传支持 UTF-8 TXT、Markdown、可提取文本的 PDF、DOCX；单文件最多 2 MB，解析文本最多 16 万字符。扫描版 PDF 尚不做 OCR。更新文档会增加版本并替换旧索引。向量由内网模型生成，索引和文档正文存于独立助理表。当前使用数据库内的向量和服务端余弦排序，单次检索最多扫描 5000 个已授权片段；超出规模时需更换独立向量索引。

## 员工回答与验收

员工继续使用 `POST /assistant/chat/ask`。命中已授权知识时，回答末尾标出文档名称及版本；无可用依据时不得编造公司制度。知识库文档被当作数据，不能凭文档内容提升权限或要求助理执行操作。

验收至少使用两个租户、不同部门与角色的员工账号：验证同一问题只引用各自可见文档；授权撤销、角色停用、文档停用后新回答和历史读取均不返回受限答案；更新后只引用新版本。真实内网向量模型的准确率与延迟必须在部署环境用实际文档再测，代码编译不能代替这一验收。

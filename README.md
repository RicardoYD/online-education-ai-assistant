# 在线教育 AI 智能助手 (Online Education AI Assistant)

> 面向 IT 领域的在线教育平台，集成视频授课、互动问答、学习数据分析等能力，依托 AI 大模型打造智能学习助手，为学员提供课程咨询、智能推荐、学习陪伴等个性化服务。

## 技术栈

| 类别 | 技术 |
|------|------|
| 微服务框架 | Spring Cloud Alibaba（Nacos, Gateway） |
| 应用框架 | SpringBoot 3.x, Spring AI, Spring AI Alibaba |
| 数据库 | MySQL, Redis |
| 搜索引擎 | Elasticsearch（向量检索） |
| 大模型 | 通义千问 Max（DashScope） |
| AI 能力 | RAG 全链路, Tool Calling, AI Agent 工作流 |
| 协议标准 | MCP（Model Context Protocol） |

## 微服务模块

```
├── tj-gateway      # 统一网关（Gateway）
├── tj-auth         # 认证授权服务
├── tj-user         # 用户服务
├── tj-course       # 课程管理服务
├── tj-learning     # 学习进度服务
├── tj-exam         # 测评考试服务
├── tj-media        # 媒体资源服务（腾讯云 VOD）
├── tj-search       # 搜索服务（Elasticsearch）
├── tj-aigc         # AI 智能助手服务 ⭐
├── tj-message      # 消息通知服务
├── tj-pay          # 支付服务
├── tj-trade        # 交易服务
├── tj-promotion    # 营销促销服务
├── tj-remark       # 评价评论服务
├── tj-data         # 数据统计服务
├── tj-api          # 对外 API
└── tj-common       # 公共模块
```

## 核心亮点

### 1. 微服务架构
- 基于 **Spring Cloud Alibaba** 搭建微服务系统
- **Nacos** 实现服务注册发现与统一配置管理
- **Gateway** 统一网关路由与接口转发

### 2. AI Agent 智能助手
- 基于 **Spring AI** 对接通义千问 Max 大模型
- **Tool Calling** 实现课程查询、学习规划、课程推荐等智能服务
- 构建 **AI Agent 工作流**，自然语言驱动业务功能调用

### 3. RAG 检索增强问答
- 基于 **Elasticsearch** 构建课程知识库
- **向量检索 + 语义召回** 实现 RAG 问答
- **Query Rewrite + 动态 TopK + 相似度阈值过滤** 优化检索链路
- 知识库有效召回率提升 **24%**

### 4. MCP 协议标准化
- 基于 **MCP 协议** 对所有业务工具做标准化封装
- 大模型层与底层业务系统解耦，提升迭代扩展性
- 自动化业务触发（如测评达标 → MCP 邮件通知）

### 5. 会话记忆管理
- **Redis + MySQL 分层存储** 管理用户会话
- Redis 短期上下文 → 快速响应
- MySQL 持久化历史对话 → 长期追踪

## 快速启动

1. 启动 Nacos 注册中心
2. 启动 MySQL、Redis、Elasticsearch
3. 配置各模块 `application-*.yml` 中的数据库连接
4. 配置通义千问 API Key
5. 依次启动 Gateway → 各业务模块

## 项目时间

2026.01 - 2026.04

## 作者

邓钰泽 (RicardoYD)

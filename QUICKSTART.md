# HAI-API 快速启动指南

## 项目概述

HAI-API 是一个企业AI模型网关与令牌管理系统。

## 已完成功能

- ✅ 项目骨架搭建
- ✅ 公共模块开发
- ✅ 实体类设计
- ✅ 数据访问层开发
- ✅ 核心AI转发服务
- ✅ API控制器开发
- ✅ 编译测试通过

## 快速开始

### 1. 初始化数据库

```bash
psql -U postgres -c "CREATE DATABASE haiapi;"
psql -U postgres -d haiapi -f init.sql
```

### 2. 启动Redis

```bash
redis-server --daemonize yes
```

### 3. 编译运行

```bash
mvn clean package -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. 测试接口

```bash
# 健康检查
curl http://localhost:8080/v1/health

# 创建测试渠道
curl -X POST http://localhost:8080/api/test/channel \
  -H "Content-Type: application/json" \
  -d '{"name": "DeepSeek", "type": "deepseek", "apiKey": "your-key"}'

# 创建测试令牌
curl -X POST http://localhost:8080/api/test/token \
  -H "Content-Type: application/json" \
  -d '{"name": "测试令牌"}'

# 调用AI
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer sk-hai-your-token" \
  -d '{"model": "deepseek-chat", "messages": [{"role": "user", "content": "Hello!"}]}'
```

## 文档

- [项目结构说明](PROJECT-STRUCTURE.md)
- [开发进度报告](Dev-Progress/2026-04-07.md)

## 版本

当前版本: 1.0.0-SNAPSHOT
开发进度: 25% (第一阶段 + 第二阶段完成)

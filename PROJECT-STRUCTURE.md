# HAI-API 项目结构说明

## 项目概览

```
hai-api/
├── pom.xml                          # 父POM
├── README.md                        # 项目说明
├── init.sql                        # 数据库初始化脚本
├── QUICKSTART.md                   # 快速启动
│
├── hai-api-common/                 # 公共模块
│   └── src/main/java/com/haiapi/common/
│       ├── result/                 # 统一响应
│       ├── constant/               # 常量
│       ├── exception/             # 异常
│       └── util/                   # 工具类
│
└── hai-api-web/                    # 主应用
    └── src/main/
        ├── java/com/haiapi/
        │   ├── HaiApiApplication.java  # 启动类
        │   ├── config/               # 配置
        │   ├── controller/          # 控制器
        │   ├── service/             # 服务
        │   ├── mapper/              # 数据访问
        │   └── entity/              # 实体
        └── resources/               # 配置
```

## 技术栈

- Java 17 + Spring Boot 3.2.5
- Spring Security + JWT
- MyBatis-Plus + PostgreSQL
- Redis缓存

## 代码统计

- Java文件: 28个
- 代码行数: ~2,100行
- 开发进度: 25%

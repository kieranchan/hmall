# HMall - 黑马商城微服务项目

## 项目概览

**HMall (黑马商城)** 是一个综合性的电商平台，旨在演示企业级 Java 开发。该项目的一个独特之处在于它包含了同一系统的**两种架构实现**：

1.  **单体架构 (Monolithic)**：位于 `hm-service` 模块中。所有的业务逻辑（用户、商品、购物车、订单、支付）都包含在一个 Spring Boot 应用中。
2.  **微服务架构 (Microservices)**：系统被拆分为多个分布式服务（`user-service`, `item-service` 等），并通过 Spring Cloud Alibaba 组件进行协调。

这种结构通常用于教学目的，演示从单体应用到微服务架构的重构过程。

## 技术栈

*   **开发语言**: Java 11
*   **核心框架**: Spring Boot 2.7.x
*   **微服务框架**: Spring Cloud Alibaba 2021.0.x
    *   **Nacos**: 服务注册与发现、配置中心
    *   **Gateway**: Spring Cloud Gateway 网关
    *   **OpenFeign**: 服务间调用
    *   **Sentinel**: 流量控制与熔断降级 (隐含)
    *   **Seata**: 分布式事务 (可能在 `trade-service` 中使用)
*   **数据库**: MySQL 8.0
*   **ORM 框架**: MyBatis Plus 3.5
*   **消息队列**: RabbitMQ
*   **搜索引擎**: Elasticsearch (根据 `SearchController` 推断)
*   **构建工具**: Maven

## 模块结构说明

| 模块名称 | 说明 |
| :--- | :--- |
| **`hm-service`** | **单体应用核心**。包含所有 Controller 和业务逻辑。默认运行在 `8080` 端口。 |
| `hm-common` | 公共模块：包含工具类 (Utils)、全局异常处理、公共 DTO/Entity。 |
| `hm-api` | API 模块：包含微服务架构下服务间调用的 Feign Client 和 DTO。 |
| `hm-gateway` | 网关服务：微服务架构的流量入口。 |
| `cart-service` | 购物车微服务。 |
| `item-service` | 商品微服务。 |
| `pay-service` | 支付微服务。 |
| `trade-service` | 交易/订单微服务。 |
| `user-service` | 用户微服务。 |

## 快速开始

### 环境要求

*   **JDK 11**
*   **Maven 3.8+**
*   **Docker Desktop** (或远程 Docker 主机)
*   **MySQL 8.0**
*   **Nacos Server 2.x**

### 选项 1: 运行单体架构 (`hm-service`)

适用于简单的功能测试或开发，无需复杂的分布式基础设施。

1.  **配置数据库**: 修改 `hm-service/src/main/resources/application.yaml` 中的 MySQL 连接信息。
2.  **构建**: `mvn clean install`
3.  **运行**: 启动 `hm-service` 模块中的 `HMallApplication` 类。
    *   **端口**: `8080`
    *   **接口文档 (Knife4j)**: `http://localhost:8080/doc.html`

### 选项 2: 运行微服务架构

适用于模拟真实的分布式生产环境。

1.  **启动基础设施**:
    *   启动 **Nacos** (单机模式 Standalone)。
    *   启动 **RabbitMQ**。
    *   启动 **MySQL**。
2.  **配置 Nacos**:
    *   确保各服务的配置文件（如 `application-dev.yaml`）已发布到 Nacos 配置中心，或存在于本地 `src/main/resources` 中。
3.  **启动网关**: 运行 `hm-gateway`。
4.  **启动各个微服务**: 依次运行 `user-service`, `item-service`, `cart-service`, `trade-service`, `pay-service`。
5.  **验证**:
    *   查看 Nacos 控制台 (`http://localhost:8848/nacos`) 确认所有服务已注册。
    *   通过网关访问: `http://localhost:8080` (注意：请确保 **停止** 单体应用 `hm-service`，因为它也占用 8080 端口)。

## 开发规范

*   **数据库**: 使用 MyBatis Plus。实体类 (Entity) 通常直接映射数据库表。
*   **数据传输对象 (DTO)**: 输入/输出对象通常与数据库实体分离。
*   **认证**: 基于 JWT 的认证机制。密钥文件 `hmall.jks` 位于资源目录下。
*   **代码风格**: 标准的 Spring Boot 分层结构 (`Controller` -> `Service` -> `Mapper`)。

## 关键配置文件

*   `pom.xml`: 根项目的依赖管理。
*   `hm-service/src/main/resources/application.yaml`: 单体应用的配置。
*   `hm-gateway/src/main/resources/application.yml`: 网关路由规则配置 (需检查是否被注释)。
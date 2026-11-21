# Matrix 项目开发规范

本文件记录团队确认的硬性约定，所有新增 / 修改代码必须遵守。框架能力统一放 `boot-scaffold`，业务项目 `matrix` 只做业务装配与实现。

## 1. 查询：三条通路，全链路统一，禁止混用（强制）

涉及**列表查询、分页、多表关联检索**的接口，一律走 `scaffold-starter-query`。该 starter 提供三条**互相独立、不可交叉**的查询通路。每条通路从「检索模型 → 参数协议 → 执行入口」必须**全程自洽**，禁止一半用 A、一半用 B。

| 通路 | 检索模型（注解） | 参数协议 | 执行入口 | 结果类 |
|---|---|---|---|---|
| ① 原生 bean-searcher | `@SearchBean(tables="...")` + `@DbField("...")` | 原生 `字段_op=值`（如 `username-cn=张`） | 注入原生 `BeanSearcher`，`beanSearcher.search(Bean.class, request.getParameterMap())` | 检索 Bean 本身 |
| ② QueryX 基础 | **JPA 实体**（`@Table` / `@Column`），不写检索注解 | X 前缀 `OP_字段=值`（`EQ_status=1`），OR 用 `\|\|`，多值用逗号 | `QueryUtilsX.parse(request.getParameterMap())` → `doQuery/doListQuery/doCount(filters, pageable, 实体.class)` | **JPA 实体** |
| ③ QueryPlan | 计划类 `@TableX` + `@JoinTableX` + `@ColumnX`（+ `@FixedFilter` 固定条件） | 同通路②，另加 `plan=计划类名` 选计划 | `@QueryPlanX` 白名单 + `QueryUtilsX.resolvePlan(...)` → `doQuery(..., planClass)` | 计划类本身 |

### 1.1 硬性禁止

- **禁止混用**：通路①不得调 `QueryUtilsX` / `QueryFilterX`；通路②③不得用 `@SearchBean` / 原生 `op` 协议；三种注解（`@SearchBean` / `@Column` / `@TableX`）不得出现在同一个类上。
- 禁止 JPA `findAll()` / `JpaRepository` 派生查询直接返回列表；禁止 service 手写拼 SQL 检索；禁止 controller 一长串 `@RequestParam` 堆查询条件。
- 通路②的结果类**必须是 JPA 实体**：`ScaffoldMetaResolver` 对非 `@TableX` 类走 `synthesizeEntity()`，按 `@Table` / `@Column` 合成元数据，普通 POJO 不适用。
- 原生 SQL 仅兜底：用 `QueryUtilsX.doNativeQuery(...)`，必须 `?` 占位传参，禁止字符串拼接。

### 1.2 参数来源（统一）

controller 的查询参数**直接从 `HttpServletRequest` 提取 map**，不逐个绑定：

- 通路② `QueryUtilsX.parse(request.getParameterMap())` 已兼容 value 为 `String[]`（单值取首个，多值靠 `||` / 逗号表达），无需再拍平。
- 通路①原生协议由 bean-searcher 自身处理 `getParameterMap()`。
- 不要自造 request→map 拍平工具：bean-searcher 的 `ARRAY_KEYS` 是其内部随机常量，只有它自己的 `ArrayValueParamFilter` 认识，照搬一份到框架无法互通。

### 1.3 数据权限

query 通路的租户 / 部门过滤由 starter 的 `ParamFilter` / `QueryInterceptor` 自动注入，业务不在检索模型里手写 `tenant_id` / `dept_id`。

## 2. Controller 规范（强制）

- **只用 GET 和 POST**，不用 PUT / DELETE / PATCH。
- **GET** 承载查询：从 request 直接提取参数 map 走 query starter（见 1.2）。
- **POST** 承载写操作与动作：请求体**必须是 JSON**，用 `@RequestBody` + DTO（带 `@Valid` 校验），不用 `@RequestParam` 堆参数。

## 3. 认证与 Spring Security 边界

- scaffold 只用 Spring Security 做**授权边界**：`SecurityFilterChain` + `anyRequest().authenticated()` 兜底、`@Anonymous` → permitAll、`TokenAuthenticationFilter` → `SecurityContextHolder`、`BCryptPasswordEncoder`、401/403 统一 JSON。
- 认证**动作**按业务实现（与芋道 ruoyi-vue-pro 同构，不用 `AuthenticationManager` / `DaoAuthenticationProvider` / 表单登录），但必须：
  - 多登录方式归一到统一的认证策略接口（`AuthenticationStrategy`），再走公用的登录装配 `LoginAssembler`。
  - 当前用户一律用 `LoginContext.current()`，禁止裸用 `SecurityContextHolder` 取业务身份。
  - `accountType` 由认证策略决定，禁止在 token 装配处写死覆盖。

## 4. 分层与模块边界

- 能力下沉：可复用的通用能力放 `boot-scaffold`，matrix 只写业务实现与 SPI 实现。
- 模块不过度拆分；数据权限等纯契约 / 算法留 `scaffold-common`，不额外建 core 模块。
- 建表统一 Liquibase，JPA 实体只保留 `@Column(name=...)`，不写 `nullable` / `length`。
- starter 之间允许单向无环依赖；需要共享的无 Spring 依赖契约放 common。
- starter 只承载「必须依赖 Spring 容器」的能力（自动装配 / AOP / 监听器 / Bean 替换）；纯 Java 可 `new` 的能力降级为与 common 平级的顶层模块（如 `scaffold-http`）。

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

### 2.1 通用 CRUD 分层（纯单表资源优先复用）

平台基础层已提供自下而上的通用 CRUD 骨架，纯单表维护型资源**不再手写** list/get/save/delete：

| 层 | 位置 | 职责 |
|---|---|---|
| `BaseService<E extends BaseEntity>` | scaffold-starter-data-jpa | `get`（逻辑删除过滤，查不到返回 null）/`exists`/`list`/`page`/`count`（走 query 通路，实体泛型自动解析）/`save`/`delete`（逻辑删除）；写操作内部用 `require(id)` 兜 NOT_FOUND，差异方法留子类 |
| `DeletedParamFilter` / `@IncludeDeleted` | scaffold-starter-query | query 通路自动注入 `deleted=false`（检索类含 `deleted` 字段才生效），前端不可篡改；查已删除数据须服务端标 `@IncludeDeleted`，与 `@TenantIgnore` 正交可叠加 |
| `BaseCrudController<E,D,S>` | scaffold-starter-web | 固定 list/get/save/delete 四个端点的 HTTP 契约与 Service 编排；子类构造器传 Service 与实体 Class 即零方法接入；三处可覆盖点 `filtersOf`（改写条件）/`toEntity`（装配实体）/`beforeDelete`（删除前钩子）；`/list` 走子类实体、`/list/{plan}` 走 `@QueryPlanX` 白名单检索模型 |

- **系统字段命名固定**：租户字段一律 `tenantId`（列 `tenant_id`）、逻辑删除标记一律 `deleted`（列 `deleted`），三条通路（findById 兜底 / Hibernate Filter / query ParamFilter）统一按字段名探测，不再支持自定义字段名（框架已移除 `@TenantIdField`）。
- 页码基准**全平台统一 1 基**（对齐 common 的 `PageParam`/`PageResult`），禁止再用 `PageRequest.of(page,size)` 的 0 基裸语义。
- `BaseCrudController` 的 plan/entity 派发是**派发器**而非通路混用：命中哪条通路该通路全程自洽，默认实体由后端写死、plan 只能选 `@QueryPlanX` 白名单内的类。
- **强业务特化资源不接基类**：如 config（加密/分组/明文副本）、dict（平台兜底/幂等 upsert）、iam 授权动作——它们保留各自的 Service/Controller 实现，只让 Service 继承 `BaseService` 复用 `get/delete` 骨架，Controller 保持特化端点不动。判定标准：写操作带业务规则（加密/兜底/唯一键/复杂装配）即不套 `save` 模板。

## 3. 认证与 Spring Security 边界

- scaffold 只用 Spring Security 做**授权边界**：`SecurityFilterChain` + `anyRequest().authenticated()` 兜底、`@Anonymous` → permitAll、`TokenAuthenticationFilter` → `SecurityContextHolder`、`BCryptPasswordEncoder`、401/403 统一 JSON。

### 3.1 授权：URL 拦截（强制，不用注解）

- **授权按 URL + HTTP 方法裁决，禁止在 controller 方法上贴权限注解**（旧的 `@RequirePermission`/`PermissionInterceptor` 已移除）。
- scaffold 提供 `UrlPermissionProvider` 契约与 `UrlPermissionInterceptor`（Spring `AntPathMatcher`，支持 `?`/`*`/`**`，多条命中取最具体）。业务实现 Provider 提供「URL 模式 → 权限点」规则：`@Anonymous` 与 `permit-urls` 白名单短路放行、命中规则则 any-of 校验无权 403、**无规则命中**由开关决定（见下）。
- **`scaffold.security.url-permission-strict` 开关**（默认 `false`）：`false`（本地开发）= 未命中规则的 URL 放行，等价于隐式「登录即可」，不必给每个接口配 pattern；`true`（生产，**必须打开**）= 未命中规则的 URL 一律 403，强制每个受控端点补齐 pattern，fail-closed。matrix 用 `URL_PERMISSION_STRICT` 环境变量注入，生产配置显式设为 `true`。无论取值，`@Anonymous` 与 `permit-urls` 始终放行，命中规则正常鉴权。
- matrix 侧 `MenuUrlPermissionProvider` 从 `sys_menu.api_pattern` 加载规则：多个模式逗号分隔，可带 `METHOD:` 前缀区分读写（如 `GET:/api/dict/**,POST:/api/dict/**` 分别落到 list/save 权限点）。**改权限=改菜单表数据，不发版。**
- 平台菜单由 `PlatformInitializer.seedPlatformMenus` 幂等播种 api_pattern；新增受控端点必须同步补一条菜单 URL 规则——**严格模式下漏配会直接 403**，非严格模式下会「登录即可访问」形成权限缺口。
- **禁止引入 actuator / 健康检查 / 指标暴露端点**（`/actuator/**` 等）：安全审计不允许，框架已移除 observability starter 及相关埋点；系统不对外暴露任何自带状态接口。
- 认证**动作**按业务实现（与芋道 ruoyi-vue-pro 同构，不用 `AuthenticationManager` / `DaoAuthenticationProvider` / 表单登录），但必须：
  - 多登录方式归一到统一的认证策略接口（`AuthenticationStrategy`），再走公用的登录装配 `LoginAssembler`。
  - 当前用户一律用 `LoginContext.current()`，禁止裸用 `SecurityContextHolder` 取业务身份。
  - `accountType` 由认证策略决定，禁止在 token 装配处写死覆盖。

## 4. 分层与模块边界

- 能力下沉：可复用的通用能力放 `boot-scaffold`，matrix 只写业务实现与 SPI 实现。
- 模块不过度拆分；数据权限等纯契约 / 算法留 `scaffold-common`，不额外建 core 模块。
- 建表统一 Liquibase，JPA 实体只保留 `@Column(name=...)`，不写 `nullable` / `length`。
- changelog 组织规范（includeAll 统一目录）：
  - 各模块把建表脚本放自己 `src/main/resources/db/changelog/`，打包后 classpath 合并为同一目录；宿主 `master.xml` 仅一行 `<includeAll path="db/changelog/"/>`，新增模块只丢脚本、master 永不改。
  - 文件名按「模块内序号递增、跨模块独立」：`001-iam-schema.xml`、`002-iam-schema.xml`、`001-support-schema.xml`；同模块靠序号保证有序，跨模块禁止外键依赖（各自为政，互不写对方表）。includeAll 按完整文件名字典序加载，序号务必补零对齐。
  - 该目录只放建表脚本，保持平铺（4.9.1 的 includeAll 不递归子目录，子目录会被静默忽略）；不要放 master 或中转 include 文件，避免自引用/重复加载。
  - 开发阶段改表结构：直接改建表脚本 + drop 库重建，不写 addColumn 增量脚本；投入生产后库不可删，才用新增 changeSet 做增量迁移。
- starter 之间允许单向无环依赖；需要共享的无 Spring 依赖契约放 common。
- starter 只承载「必须依赖 Spring 容器」的能力（自动装配 / AOP / 监听器 / Bean 替换）；纯 Java 可 `new` 的能力降级为与 common 平级的顶层模块（如 `scaffold-http`）。

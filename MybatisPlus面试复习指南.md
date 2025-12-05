# MybatisPlus 面试复习指南

> 基于 hmall 项目实战总结 | MybatisPlus 3.4.3 | Spring Boot 2.7.12

---

## 目录

1. [MybatisPlus 基础概念](#1-mybatisplus-基础概念)
2. [核心注解详解](#2-核心注解详解)
3. [CRUD 操作](#3-crud-操作)
4. [条件构造器](#4-条件构造器)
5. [Service 层封装](#5-service-层封装)
6. [自定义 SQL](#6-自定义-sql)
7. [插件机制](#7-插件机制)
8. [分页查询](#8-分页查询)
9. [批量操作](#9-批量操作)
10. [乐观锁与并发控制](#10-乐观锁与并发控制)
11. [常见面试题](#11-常见面试题)
12. [最佳实践与注意事项](#12-最佳实践与注意事项)

---

## 1. MybatisPlus 基础概念

### 1.1 什么是 MybatisPlus?

MybatisPlus (简称 MP) 是一个 MyBatis 的增强工具，在 MyBatis 的基础上只做增强不做改变，为简化开发、提高效率而生。

**核心特性:**
- 无侵入：只做增强不做改变，引入它不会对现有工程产生影响
- 损耗小：启动即会自动注入基本 CURD，性能基本无损耗
- 强大的 CRUD 操作：内置通用 Mapper、通用 Service，仅仅通过少量配置即可实现单表大部分 CRUD 操作
- 支持 Lambda 形式调用：通过 Lambda 表达式，方便的编写各类查询条件，无需再担心字段写错
- 支持主键自动生成：支持多达 4 种主键策略，可自由配置
- 支持 ActiveRecord 模式：支持 ActiveRecord 形式调用
- 支持自定义全局通用操作：支持全局通用方法注入（Write once, use anywhere）
- 内置代码生成器：采用代码或者 Maven 插件可快速生成 Mapper 、 Model 、 Service 、 Controller 层代码
- 内置分页插件：基于 MyBatis 物理分页，开发者无需关心具体操作
- 分页插件支持多种数据库：支持 MySQL、MariaDB、Oracle、DB2、H2、HSQL、SQLite、Postgre、SQLServer 等多种数据库
- 内置性能分析插件：可输出 Sql 语句以及其执行时间，建议开发测试时启用该功能
- 内置全局拦截插件：提供全表 delete 、 update 操作智能分析阻断

### 1.2 MybatisPlus 架构

```
┌─────────────────────────────────────┐
│        Controller 层                 │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│        Service 层                    │
│  IService<T> (接口)                  │
│  ServiceImpl<M,T> (实现)             │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│        Mapper 层                     │
│  BaseMapper<T>                       │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│        实体层 (PO)                   │
│  @TableName / @TableId / @TableField │
└─────────────────────────────────────┘
```

### 1.3 项目中的配置

**位置:** `hm-common/src/main/java/com/hmall/common/config/MyBatisConfig.java`

```java
@Configuration
@ConditionalOnClass({MybatisPlusInterceptor.class, BaseMapper.class})
public class MyBatisConfig {
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页拦截器
        PaginationInnerInterceptor paginationInnerInterceptor =
            new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInnerInterceptor.setMaxLimit(1000L);  // 最大分页限制

        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }
}
```

**面试点:**
- MybatisPlusInterceptor 是插件的核心类
- 通过 addInnerInterceptor() 添加各种内置拦截器
- 常见的拦截器：分页拦截器、乐观锁拦截器、防全表更新删除拦截器等

---

## 2. 核心注解详解

### 2.1 @TableName - 表名注解

**作用:** 指定实体类对应的数据库表名

**项目示例:**

```java
// Order.java - 使用反引号转义SQL关键字
@TableName("`order`")
public class Order implements Serializable {
    // ...
}

// Item.java - 普通表名
@TableName("item")
public class Item implements Serializable {
    // ...
}
```

**面试点:**
- 当实体类名与表名不一致时使用
- 可以使用反引号转义 SQL 关键字（如 order、user、group 等）
- 支持动态表名（不常用）

### 2.2 @TableId - 主键注解

**作用:** 指定主键字段及其生成策略

**项目中的三种使用方式:**

#### (1) AUTO - 数据库自增

```java
// User.java
@TableId(value = "id", type = IdType.AUTO)
private Long id;
```

**适用场景:** MySQL/MariaDB 的 AUTO_INCREMENT，PostgreSQL 的 SERIAL

#### (2) ASSIGN_ID - 雪花算法（默认）

```java
// Order.java - 分布式ID生成
@TableId(value = "id", type = IdType.ASSIGN_ID)
private Long id;
```

**适用场景:**
- 分布式系统，需要全局唯一ID
- Long 类型（19位）或 String 类型
- 无需依赖数据库，性能更好

#### (3) INPUT - 手动赋值

```java
// OrderLogistics.java - 使用订单ID作为主键
@TableId(value = "order_id", type = IdType.INPUT)
private Long orderId;
```

**适用场景:**
- 一对一关系，使用外键作为主键
- 业务主键（如身份证号、订单号等）

**IdType 枚举全览:**

| 值 | 描述 |
|---|---|
| AUTO | 数据库自增 |
| NONE | 无状态，该类型为未设置主键类型（注解里等于跟随全局，全局里约等于 INPUT） |
| INPUT | 用户输入 |
| ASSIGN_ID | 分配ID（主键类型为 Number(Long) 或 String，使用雪花算法） |
| ASSIGN_UUID | 分配UUID（主键类型为 String） |

### 2.3 @TableField - 字段注解

**作用:** 指定实体类字段与数据库列的映射关系

**项目示例:**

```java
// Item.java - Java驼峰命名与数据库列名不一致
@TableField("isAD")
private Boolean isAD;
```

**常用属性:**

```java
@TableField(
    value = "column_name",      // 数据库字段名
    exist = true,               // 是否为数据库表字段
    fill = FieldFill.INSERT     // 字段自动填充策略
)
```

**面试点:**
- exist = false：标记非数据库字段（如 VO 中的额外字段）
- fill：配合 MetaObjectHandler 实现字段自动填充（如创建时间、更新时间）

---

## 3. CRUD 操作

### 3.1 BaseMapper 提供的方法

MybatisPlus 的 Mapper 接口继承 `BaseMapper<T>` 后，自动拥有以下方法：

**Insert:**
- `int insert(T entity)` - 插入一条记录

**Delete:**
- `int deleteById(Serializable id)` - 根据 ID 删除
- `int deleteBatchIds(Collection<? extends Serializable> idList)` - 批量删除
- `int delete(Wrapper<T> wrapper)` - 根据条件删除

**Update:**
- `int updateById(T entity)` - 根据 ID 更新
- `int update(T entity, Wrapper<T> updateWrapper)` - 根据条件更新

**Select:**
- `T selectById(Serializable id)` - 根据 ID 查询
- `List<T> selectBatchIds(Collection<? extends Serializable> idList)` - 批量查询
- `List<T> selectList(Wrapper<T> queryWrapper)` - 条件查询
- `T selectOne(Wrapper<T> queryWrapper)` - 查询单条
- `Integer selectCount(Wrapper<T> queryWrapper)` - 查询总数
- `IPage<T> selectPage(IPage<T> page, Wrapper<T> queryWrapper)` - 分页查询

### 3.2 项目中的 Mapper 示例

```java
// CartMapper.java
public interface CartMapper extends BaseMapper<Cart> {
    // 自定义SQL：购物车数量+1
    @Update("UPDATE cart SET num = num + 1 WHERE user_id = #{userId} AND item_id = #{itemId}")
    void updateNum(@Param("itemId") Long itemId, @Param("userId") Long userId);
}
```

**面试点:**
- Mapper 接口无需添加 @Mapper 注解（通过包扫描自动注入）
- 可以在 Mapper 中定义自定义方法，不影响基础 CRUD

---

## 4. 条件构造器

MybatisPlus 提供了强大的条件构造器，避免手写 SQL 的错误。

### 4.1 Wrapper 类型对比

| Wrapper | 说明 | 使用场景 |
|---------|------|---------|
| QueryWrapper | 查询条件构造器 | 复杂查询，字符串字段名 |
| UpdateWrapper | 更新条件构造器 | 复杂更新，字符串字段名 |
| LambdaQueryWrapper | Lambda查询构造器 | 类型安全，防止字段名写错 |
| LambdaUpdateWrapper | Lambda更新构造器 | 类型安全的更新操作 |

### 4.2 项目中的 QueryWrapper 示例

#### 示例 1: 多条件删除

**代码位置:** `CartServiceImpl.java:106-114`

```java
@Override
public void removeByItemIds(Collection<Long> itemIds) {
    // 构建删除条件
    QueryWrapper<Cart> queryWrapper = new QueryWrapper<>();
    queryWrapper.lambda()
        .eq(Cart::getUserId, UserContext.getUser())  // WHERE user_id = ?
        .in(Cart::getItemId, itemIds);               // AND item_id IN (?, ?, ?)

    remove(queryWrapper);  // 执行删除
}
```

**等价 SQL:**
```sql
DELETE FROM cart
WHERE user_id = ? AND item_id IN (?, ?, ?)
```

### 4.3 LambdaQuery 链式调用（推荐）

**ServiceImpl 提供的 Lambda 快捷方法:**
- `lambdaQuery()` - 返回 LambdaQueryChainWrapper
- `lambdaUpdate()` - 返回 LambdaUpdateChainWrapper

#### 示例 1: 单条件查询列表

**代码位置:** `CartServiceImpl.java:68`

```java
@Override
public List<CartVO> queryMyCarts() {
    // Lambda链式查询
    List<Cart> carts = lambdaQuery()
        .eq(Cart::getUserId, UserContext.getUser())  // WHERE user_id = ?
        .list();  // 返回 List<Cart>

    // 后续处理...
    return vos;
}
```

#### 示例 2: 条件查询单条

**代码位置:** `UserServiceImpl.java`

```java
@Override
public UserLoginVO login(LoginFormDTO loginDTO) {
    String username = loginDTO.getUsername();

    // 查询单条记录
    User user = lambdaQuery()
        .eq(User::getUsername, username)
        .one();  // 返回单条或 null

    Assert.notNull(user, "用户名错误");
    // ...
}
```

#### 示例 3: 条件计数

**代码位置:** `CartServiceImpl.java:117-120`

```java
private void checkCartsFull(Long userId) {
    // 统计购物车数量
    int count = lambdaQuery()
        .eq(Cart::getUserId, userId)
        .count();  // 返回 int

    if (count >= 10) {
        throw new BizIllegalException("购物车商品不能超过10个");
    }
}
```

#### 示例 4: 多条件查询

**代码位置:** `CartServiceImpl.java:123-129`

```java
private boolean checkItemExists(Long itemId, Long userId) {
    // 多个等于条件
    int count = lambdaQuery()
        .eq(Cart::getUserId, userId)
        .eq(Cart::getItemId, itemId)
        .count();

    return count > 0;
}
```

### 4.4 LambdaUpdate 链式更新

#### 示例 1: 条件更新多个字段

**代码位置:** `OrderServiceImpl.java:138-143`

```java
@Override
public void markOrderPaySuccess(Long orderId) {
    // Lambda 条件更新
    lambdaUpdate()
        .set(Order::getPayTime, LocalDateTime.now())  // SET pay_time = ?
        .set(Order::getStatus, 2)                     // SET status = ?
        .eq(Order::getId, orderId)                    // WHERE id = ?
        .eq(Order::getStatus, 1)                      // AND status = 1 (乐观锁)
        .update();
}
```

**等价 SQL:**
```sql
UPDATE `order`
SET pay_time = ?, status = ?
WHERE id = ? AND status = 1
```

**面试重点: 乐观锁实现**
- 通过 `.eq(Order::getStatus, 1)` 实现乐观锁
- 更新时校验状态，防止并发修改
- 无需额外的 @Version 注解

#### 示例 2: 多状态乐观锁

**代码位置:** `PayOrderServiceImpl.java`

```java
public boolean markPayOrderSuccess(Long id, LocalDateTime successTime) {
    return lambdaUpdate()
        .set(PayOrder::getStatus, PayStatus.TRADE_SUCCESS.getValue())
        .set(PayOrder::getPaySuccessTime, successTime)
        .eq(PayOrder::getId, id)
        .in(PayOrder::getStatus,                         // 只有这些状态才能更新
            PayStatus.NOT_COMMIT.getValue(),
            PayStatus.WAIT_BUYER_PAY.getValue())
        .update();
}
```

### 4.5 常用条件方法总结

**比较操作:**
```java
eq(字段, 值)         // =
ne(字段, 值)         // !=
gt(字段, 值)         // >
ge(字段, 值)         // >=
lt(字段, 值)         // <
le(字段, 值)         // <=
between(字段, v1, v2) // BETWEEN v1 AND v2
```

**范围操作:**
```java
in(字段, 集合)       // IN (值1, 值2, ...)
notIn(字段, 集合)    // NOT IN (值1, 值2, ...)
```

**模糊查询:**
```java
like(字段, 值)       // LIKE '%值%'
likeLeft(字段, 值)   // LIKE '%值'
likeRight(字段, 值)  // LIKE '值%'
```

**空值判断:**
```java
isNull(字段)        // IS NULL
isNotNull(字段)     // IS NOT NULL
```

**逻辑操作:**
```java
and()               // AND
or()                // OR
```

**排序:**
```java
orderByAsc(字段)    // ORDER BY 字段 ASC
orderByDesc(字段)   // ORDER BY 字段 DESC
```

---

## 5. Service 层封装

### 5.1 IService<T> 接口

MybatisPlus 提供的 Service 层接口，包含常用的业务方法。

**继承方式:**
```java
public interface ICartService extends IService<Cart> {
    // 自定义业务方法
    void addItem2Cart(CartFormDTO cartFormDTO);
    List<CartVO> queryMyCarts();
}
```

### 5.2 ServiceImpl<M, T> 实现类

**继承方式:**
```java
@Service
@RequiredArgsConstructor
public class CartServiceImpl
    extends ServiceImpl<CartMapper, Cart>   // 泛型: Mapper, 实体类
    implements ICartService {

    // 可以直接使用 baseMapper
    // 可以调用 IService 提供的所有方法
}
```

### 5.3 IService 常用方法

**保存:**
```java
boolean save(T entity)                           // 插入一条记录
boolean saveBatch(Collection<T> entityList)      // 批量插入
boolean saveOrUpdate(T entity)                   // 存在则更新，否则插入
```

**删除:**
```java
boolean removeById(Serializable id)              // 根据ID删除
boolean removeByIds(Collection<? extends Serializable> idList)  // 批量删除
boolean remove(Wrapper<T> queryWrapper)          // 条件删除
```

**更新:**
```java
boolean updateById(T entity)                     // 根据ID更新
boolean update(Wrapper<T> updateWrapper)         // 条件更新
boolean updateBatchById(Collection<T> entityList) // 批量更新
```

**查询:**
```java
T getById(Serializable id)                       // 根据ID查询
List<T> listByIds(Collection<? extends Serializable> idList)  // 批量查询
List<T> list(Wrapper<T> queryWrapper)            // 条件查询
T getOne(Wrapper<T> queryWrapper)                // 查询单条
int count(Wrapper<T> queryWrapper)               // 统计数量
```

**链式调用:**
```java
LambdaQueryChainWrapper<T> lambdaQuery()         // Lambda查询
LambdaUpdateChainWrapper<T> lambdaUpdate()       // Lambda更新
```

### 5.4 项目示例

**代码位置:** `CartServiceImpl.java:42-63`

```java
@Override
public void addItem2Cart(CartFormDTO cartFormDTO) {
    Long userId = UserContext.getUser();

    // 判断是否已存在
    if(checkItemExists(cartFormDTO.getItemId(), userId)){
        // 存在则更新数量（调用Mapper自定义方法）
        baseMapper.updateNum(cartFormDTO.getItemId(), userId);
        return;
    }

    // 判断购物车是否已满
    checkCartsFull(userId);

    // 新增购物车条目
    Cart cart = BeanUtils.copyBean(cartFormDTO, Cart.class);
    cart.setUserId(userId);

    // 调用 IService 的 save 方法
    save(cart);
}
```

**面试点:**
- `save()` 来自 IService 接口
- `baseMapper` 可以访问 Mapper 的所有方法（包括自定义方法）
- Service 层负责业务逻辑，Mapper 层负责数据访问

---

## 6. 自定义 SQL

虽然 MybatisPlus 提供了强大的 CRUD 功能，但复杂业务仍需自定义 SQL。

### 6.1 使用注解方式

#### 示例 1: @Update - 库存扣减

**代码位置:** `ItemMapper.java`

```java
public interface ItemMapper extends BaseMapper<Item> {

    // 扣减库存
    @Update("UPDATE item SET stock = stock - #{num} WHERE id = #{itemId}")
    void updateStock(OrderDetailDTO orderDetail);

    // 恢复库存
    @Update("UPDATE item SET stock = stock + #{num} WHERE id = #{itemId}")
    void restockItemByItemIdsAndNums(OrderDetailDTO orderDetail);
}
```

**注意事项:**
- 参数对象的字段名需要与 SQL 中的占位符一致
- 使用 `#{}` 防止 SQL 注入（推荐）

#### 示例 2: @Update - 余额扣款

**代码位置:** `UserMapper.java`

```java
public interface UserMapper extends BaseMapper<User> {

    // 扣减余额
    @Update("update user set balance = balance - ${totalFee} where id = #{userId}")
    void updateMoney(@Param("userId") Long userId, @Param("totalFee") Integer totalFee);
}
```

**面试重点: #{} 与 ${} 的区别**

| 符号 | 处理方式 | SQL 注入风险 | 使用场景 |
|------|---------|------------|---------|
| `#{}` | 预编译（PreparedStatement） | 无 | 普通参数绑定（推荐） |
| `${}` | 字符串替换 | 有 | 动态表名、列名、ORDER BY |

**项目中的使用:**
```java
// 正确用法: 数值计算可以使用 ${}
balance = balance - ${totalFee}

// 错误用法: WHERE 条件应使用 #{}
WHERE id = ${userId}  // ❌ 有SQL注入风险
WHERE id = #{userId}  // ✅ 安全
```

#### 示例 3: 多参数传递

```java
// 方式1: 使用 @Param 注解
@Update("UPDATE cart SET num = num + 1 WHERE user_id = #{userId} AND item_id = #{itemId}")
void updateNum(@Param("itemId") Long itemId, @Param("userId") Long userId);

// 方式2: 使用对象属性
@Update("UPDATE item SET stock = stock - #{num} WHERE id = #{itemId}")
void updateStock(OrderDetailDTO orderDetail);
```

### 6.2 使用 XML 方式

虽然项目中未使用 XML，但面试常问。

**Mapper 接口:**
```java
public interface ItemMapper extends BaseMapper<Item> {
    List<Item> selectByCondition(@Param("category") String category, @Param("minPrice") Integer minPrice);
}
```

**XML 配置:**
```xml
<!-- resources/mapper/ItemMapper.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.hmall.mapper.ItemMapper">

    <select id="selectByCondition" resultType="com.hmall.domain.po.Item">
        SELECT * FROM item
        <where>
            <if test="category != null and category != ''">
                AND category = #{category}
            </if>
            <if test="minPrice != null">
                AND price >= #{minPrice}
            </if>
        </where>
    </select>

</mapper>
```

---

## 7. 插件机制

### 7.1 插件架构

MybatisPlus 通过 `MybatisPlusInterceptor` 实现插件功能。

**核心类:**
```java
MybatisPlusInterceptor              // 插件容器
└── InnerInterceptor                // 内置拦截器接口
    ├── PaginationInnerInterceptor  // 分页插件
    ├── OptimisticLockerInnerInterceptor  // 乐观锁插件
    ├── BlockAttackInnerInterceptor       // 防全表更新删除插件
    └── ...
```

### 7.2 项目中的插件配置

**代码位置:** `MyBatisConfig.java`

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

    // 添加分页插件
    PaginationInnerInterceptor paginationInnerInterceptor =
        new PaginationInnerInterceptor(DbType.MYSQL);
    paginationInnerInterceptor.setMaxLimit(1000L);  // 最大单页限制

    interceptor.addInnerInterceptor(paginationInnerInterceptor);

    return interceptor;
}
```

### 7.3 常用插件

#### (1) 分页插件

```java
// 配置
PaginationInnerInterceptor paginationInterceptor =
    new PaginationInnerInterceptor(DbType.MYSQL);
paginationInterceptor.setMaxLimit(1000L);       // 单页最大数量
paginationInterceptor.setOverflow(false);       // 溢出总页数后是否进行处理
interceptor.addInnerInterceptor(paginationInterceptor);

// 使用
IPage<User> page = new Page<>(1, 10);  // 第1页，每页10条
IPage<User> result = userMapper.selectPage(page, null);
List<User> records = result.getRecords();  // 数据
long total = result.getTotal();            // 总数
```

#### (2) 乐观锁插件

```java
// 配置
interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

// 实体类
public class Product {
    @TableId
    private Long id;

    private String name;

    @Version  // 版本号字段
    private Integer version;
}

// 使用
Product product = productMapper.selectById(1L);
product.setName("新名称");
// UPDATE product SET name=?, version=version+1 WHERE id=? AND version=?
productMapper.updateById(product);
```

#### (3) 防全表更新删除插件

```java
// 配置
interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

// 效果: 以下操作会抛出异常
mapper.delete(null);    // 全表删除，抛异常
mapper.update(entity, null);  // 全表更新，抛异常
```

### 7.4 面试常见问题

**Q: MybatisPlus 插件的执行顺序?**

A: 按照添加顺序执行，建议顺序:
1. 多租户插件
2. 动态表名插件
3. 分页插件
4. 乐观锁插件
5. SQL 性能规范插件
6. 防全表更新删除插件

**Q: 为什么分页插件要指定数据库类型?**

A: 不同数据库的分页语法不同:
- MySQL: `LIMIT offset, size`
- Oracle: `ROWNUM`
- SQL Server: `OFFSET ... FETCH NEXT`

---

## 8. 分页查询

### 8.1 分页的基本使用

虽然项目中未显式使用分页，但配置已就绪。

**Mapper 方法:**
```java
public interface UserMapper extends BaseMapper<User> {
    // BaseMapper 已提供
    IPage<User> selectPage(IPage<User> page, @Param("ew") Wrapper<User> queryWrapper);
}
```

**Service 调用:**
```java
@Override
public IPage<User> getUserPage(int pageNum, int pageSize, String keyword) {
    // 创建分页对象
    Page<User> page = new Page<>(pageNum, pageSize);

    // 条件构造
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    wrapper.like(StringUtils.isNotBlank(keyword), User::getUsername, keyword);

    // 执行分页查询
    IPage<User> result = page(page, wrapper);

    return result;
}
```

**返回结果:**
```java
result.getRecords()     // List<User> 当前页数据
result.getTotal()       // long 总记录数
result.getSize()        // long 每页大小
result.getCurrent()     // long 当前页码
result.getPages()       // long 总页数
```

### 8.2 自定义 SQL 分页

**Mapper 接口:**
```java
public interface UserMapper extends BaseMapper<User> {
    // 第一个参数必须是 IPage 类型
    IPage<User> selectUserWithRole(IPage<?> page, @Param("roleId") Long roleId);
}
```

**XML 配置:**
```xml
<select id="selectUserWithRole" resultType="com.hmall.domain.po.User">
    SELECT u.* FROM user u
    LEFT JOIN user_role ur ON u.id = ur.user_id
    WHERE ur.role_id = #{roleId}
</select>
```

**面试重点:**
- 自定义分页方法第一个参数必须是 `IPage` 类型
- 不需要手写 LIMIT 语句，插件会自动添加
- 返回值可以是 `IPage<T>` 或 `List<T>`（推荐 IPage）

---

## 9. 批量操作

### 9.1 批量插入

```java
// IService 提供的方法
boolean saveBatch(Collection<T> entityList);
boolean saveBatch(Collection<T> entityList, int batchSize);  // 指定批次大小

// 使用示例
List<Item> items = new ArrayList<>();
// ... 添加数据
itemService.saveBatch(items);  // 默认批次1000条
itemService.saveBatch(items, 500);  // 每批500条
```

### 9.2 批量更新

```java
boolean updateBatchById(Collection<T> entityList);
boolean updateBatchById(Collection<T> entityList, int batchSize);
```

### 9.3 批量执行自定义 SQL

**项目示例 - 库存批量扣减:**

**代码位置:** `ItemServiceImpl.java:30-47`

```java
@Override
public void deductStock(List<OrderDetailDTO> items) {
    // 自定义SQL的完全限定名
    String sqlStatement = "com.hmall.item.mapper.ItemMapper.updateStock";

    // 批量执行
    boolean r = executeBatch(items, new BiConsumer<SqlSession, OrderDetailDTO>() {
        @Override
        public void accept(SqlSession sqlSession, OrderDetailDTO entity) {
            sqlSession.update(sqlStatement, entity);
        }
    });

    if (!r) {
        throw new BizIllegalException("库存不足！");
    }
}
```

**对应的 Mapper 方法:**
```java
@Update("UPDATE item SET stock = stock - #{num} WHERE id = #{itemId}")
void updateStock(OrderDetailDTO orderDetail);
```

**面试重点:**
- `executeBatch` 是 ServiceImpl 提供的方法
- 参数1: 实体集合
- 参数2: BiConsumer<SqlSession, Entity> 函数式接口
- sqlStatement 格式: `Mapper全限定名.方法名`

**Lambda 简化写法:**
```java
boolean r = executeBatch(items, (sqlSession, entity) -> {
    sqlSession.update(sqlStatement, entity);
});
```

### 9.4 批量操作的性能优化

**面试问题: saveBatch 的实现原理?**

答: MybatisPlus 的 `saveBatch` 并非真正的批量 INSERT，而是:
1. 开启批处理模式（JDBC executeBatch）
2. 循环执行单条 INSERT
3. 每 batchSize 条提交一次

**真正的批量 INSERT (性能更好):**
```xml
<!-- Mapper XML -->
<insert id="batchInsert">
    INSERT INTO item (name, price, stock) VALUES
    <foreach collection="list" item="item" separator=",">
        (#{item.name}, #{item.price}, #{item.stock})
    </foreach>
</insert>
```

---

## 10. 乐观锁与并发控制

### 10.1 业务乐观锁（项目中使用）

**原理:** 通过业务字段（如状态）判断是否允许更新

**示例 1: 订单支付状态更新**

**代码位置:** `OrderServiceImpl.java:138-143`

```java
@Override
public void markOrderPaySuccess(Long orderId) {
    lambdaUpdate()
        .set(Order::getPayTime, LocalDateTime.now())
        .set(Order::getStatus, 2)  // 已支付
        .eq(Order::getId, orderId)
        .eq(Order::getStatus, 1)   // ⭐ 只有未支付状态才能更新
        .update();
}
```

**等价 SQL:**
```sql
UPDATE `order`
SET pay_time = ?, status = 2
WHERE id = ? AND status = 1
```

**并发场景:**
- 线程A、B 同时调用 `markOrderPaySuccess(1)`
- 两个线程都读到 status=1
- 线程A 先执行 UPDATE，影响行数=1，成功
- 线程B 再执行 UPDATE，此时 status=2，WHERE 条件不满足，影响行数=0，失败

**示例 2: 支付单状态更新**

**代码位置:** `PayOrderServiceImpl.java`

```java
public boolean markPayOrderSuccess(Long id, LocalDateTime successTime) {
    return lambdaUpdate()
        .set(PayOrder::getStatus, PayStatus.TRADE_SUCCESS.getValue())
        .set(PayOrder::getPaySuccessTime, successTime)
        .eq(PayOrder::getId, id)
        .in(PayOrder::getStatus,  // ⭐ 只有这两个状态才能更新
            PayStatus.NOT_COMMIT.getValue(),
            PayStatus.WAIT_BUYER_PAY.getValue())
        .update();
}
```

**面试重点:**
- 业务乐观锁不需要额外的 version 字段
- 通过状态机模式控制状态流转
- 更新失败时，`update()` 返回 false

### 10.2 @Version 注解乐观锁

虽然项目中未使用，但面试常问。

**配置插件:**
```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

    // 添加乐观锁插件
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

    return interceptor;
}
```

**实体类:**
```java
@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private Integer stock;

    @Version  // 版本号字段
    private Integer version;
}
```

**使用示例:**
```java
// 线程A
Product product = productMapper.selectById(1L);  // version=1
product.setStock(product.getStock() - 1);
// UPDATE product SET stock=?, version=2 WHERE id=1 AND version=1
int rows = productMapper.updateById(product);  // 成功，rows=1

// 线程B (同时执行)
Product product = productMapper.selectById(1L);  // version=1
product.setStock(product.getStock() - 1);
// UPDATE product SET stock=?, version=2 WHERE id=1 AND version=1
int rows = productMapper.updateById(product);  // 失败，rows=0 (version已变成2)
```

**面试问题: @Version 只支持哪些类型?**

答: Integer、Long、Date、Timestamp、LocalDateTime

**面试问题: 乐观锁失败如何处理?**

答:
1. 重试机制（适用于库存扣减等场景）
2. 返回错误提示用户（适用于数据编辑冲突）
3. 转为悲观锁（SELECT ... FOR UPDATE）

---

## 11. 常见面试题

### 11.1 基础概念类

**Q1: MybatisPlus 和 MyBatis 的区别?**

A:
- MyBatis: 半自动 ORM 框架，需要手写 SQL
- MybatisPlus: 在 MyBatis 基础上增强
  - 内置 BaseMapper，提供单表 CRUD
  - 提供条件构造器，避免手写 SQL
  - 提供代码生成器
  - 提供分页、乐观锁等插件
  - 支持 Lambda 查询

**Q2: MybatisPlus 如何避免全表更新和删除?**

A: 使用 `BlockAttackInnerInterceptor` 插件
```java
interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
```

**Q3: MybatisPlus 的主键生成策略有哪些?**

A:
- AUTO: 数据库自增
- ASSIGN_ID: 雪花算法（默认）
- ASSIGN_UUID: UUID
- INPUT: 用户输入
- NONE: 无状态

### 11.2 使用技巧类

**Q4: 如何实现动态查询条件?**

A: 使用条件构造器的链式调用
```java
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper
    .like(StringUtils.isNotBlank(name), User::getUsername, name)
    .ge(minAge != null, User::getAge, minAge)
    .le(maxAge != null, User::getAge, maxAge);
List<User> users = userMapper.selectList(wrapper);
```

**Q5: 如何实现多表关联查询?**

A: MybatisPlus 主要针对单表操作，多表查询建议:
1. 使用自定义 SQL (注解或 XML)
2. 分步查询后在代码中组装
3. 使用 MyBatis 原生功能

**Q6: LambdaQuery 和 QueryWrapper 的区别?**

A:
- LambdaQuery: 类型安全，使用方法引用，编译期检查字段名
- QueryWrapper: 使用字符串字段名，容易写错

```java
// LambdaQuery (推荐)
lambdaQuery().eq(User::getUsername, "admin")

// QueryWrapper
new QueryWrapper<User>().eq("username", "admin")  // 字段名可能写错
```

### 11.3 性能优化类

**Q7: MybatisPlus 如何优化批量插入性能?**

A:
1. 使用 saveBatch 并指定合理的 batchSize
2. 使用自定义 SQL 的批量 INSERT (更快)
3. 关闭二级缓存
4. 调整 JDBC 参数 (rewriteBatchedStatements=true)

**Q8: 分页查询如何优化?**

A:
1. 添加合适的索引
2. 避免深分页（使用 ID 范围查询代替 OFFSET）
3. 设置 maxLimit 限制单页最大数量
4. 使用覆盖索引减少回表

**Q9: MybatisPlus 的性能是否有损耗?**

A:
- 几乎无损耗，底层仍然是 MyBatis
- 条件构造器在启动时就完成了 SQL 拼接
- 运行时只是普通的 JDBC 调用

### 11.4 高级特性类

**Q10: MybatisPlus 如何实现逻辑删除?**

A: 使用 @TableLogic 注解
```java
public class User {
    @TableId
    private Long id;

    @TableLogic  // 逻辑删除字段
    private Integer deleted;  // 0-未删除, 1-已删除
}

// 配置 (application.yml)
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

// 使用
userMapper.deleteById(1L);
// 实际执行: UPDATE user SET deleted=1 WHERE id=1

userMapper.selectById(1L);
// 实际执行: SELECT * FROM user WHERE id=1 AND deleted=0
```

**Q11: MybatisPlus 如何实现自动填充?**

A: 使用 @TableField 和 MetaObjectHandler
```java
// 实体类
public class User {
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}

// 配置类
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

**Q12: MybatisPlus 如何实现多租户?**

A: 使用 TenantLineInnerInterceptor
```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

    // 多租户插件
    TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
    tenantInterceptor.setTenantLineHandler(new TenantLineHandler() {
        @Override
        public Expression getTenantId() {
            // 从上下文获取租户ID
            return new LongValue(TenantContext.getTenantId());
        }

        @Override
        public String getTenantIdColumn() {
            return "tenant_id";  // 租户字段名
        }

        @Override
        public boolean ignoreTable(String tableName) {
            // 哪些表不需要多租户隔离
            return "user".equals(tableName);
        }
    });

    interceptor.addInnerInterceptor(tenantInterceptor);
    return interceptor;
}

// 效果: 所有SQL自动添加 WHERE tenant_id = ?
```

---

## 12. 最佳实践与注意事项

### 12.1 代码规范

#### ✅ 推荐做法

1. **优先使用 Lambda 查询**
```java
// ✅ 推荐
lambdaQuery().eq(User::getUsername, username).one()

// ❌ 不推荐
new QueryWrapper<User>().eq("username", username)
```

2. **Service 层继承 ServiceImpl**
```java
// ✅ 推荐
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements IUserService {
    // 可使用 lambdaQuery()、lambdaUpdate() 等快捷方法
}

// ❌ 不推荐
@Service
public class UserServiceImpl implements IUserService {
    @Autowired
    private UserMapper userMapper;
    // 需要手动调用 Mapper 方法
}
```

3. **使用业务乐观锁保证并发安全**
```java
// ✅ 推荐 - 状态机模式
lambdaUpdate()
    .set(Order::getStatus, 2)
    .eq(Order::getId, orderId)
    .eq(Order::getStatus, 1)  // 通过状态判断
    .update()

// ❌ 不推荐 - 先查后改
Order order = getById(orderId);
if (order.getStatus() == 1) {  // 存在并发问题
    order.setStatus(2);
    updateById(order);
}
```

4. **自定义 SQL 使用 #{} 防止注入**
```java
// ✅ 推荐
@Update("UPDATE user SET balance = balance - #{amount} WHERE id = #{id}")
void deduct(@Param("id") Long id, @Param("amount") Integer amount);

// ❌ 不推荐 - 有SQL注入风险
@Update("UPDATE user SET balance = balance - ${amount} WHERE id = ${id}")
```

5. **批量操作指定合理的 batchSize**
```java
// ✅ 推荐
saveBatch(items, 500);  // 每批500条

// ❌ 不推荐
saveBatch(items);  // 默认1000条，可能导致内存问题
```

### 12.2 性能优化

1. **避免 N+1 查询**
```java
// ❌ 不推荐 - N+1问题
List<Order> orders = orderMapper.selectList(null);
for (Order order : orders) {
    // 循环查询，产生N次SQL
    User user = userMapper.selectById(order.getUserId());
    order.setUser(user);
}

// ✅ 推荐 - 批量查询
List<Order> orders = orderMapper.selectList(null);
Set<Long> userIds = orders.stream().map(Order::getUserId).collect(Collectors.toSet());
List<User> users = userMapper.selectBatchIds(userIds);  // 一次SQL
Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
orders.forEach(order -> order.setUser(userMap.get(order.getUserId())));
```

2. **分页查询优化**
```java
// ❌ 不推荐 - 深分页性能差
Page<User> page = new Page<>(10000, 10);  // OFFSET 100000

// ✅ 推荐 - 使用ID范围
lambdaQuery()
    .gt(User::getId, lastId)  // WHERE id > ?
    .orderByAsc(User::getId)
    .last("LIMIT 10")
```

3. **只查询需要的字段**
```java
// ❌ 不推荐 - 查询所有字段
List<User> users = lambdaQuery().list();

// ✅ 推荐 - 只查询需要的字段
List<User> users = lambdaQuery()
    .select(User::getId, User::getUsername)
    .list();
```

### 12.3 常见坑点

#### 坑1: updateById 不会更新 null 值

```java
User user = new User();
user.setId(1L);
user.setUsername(null);  // 想要清空用户名
updateById(user);
// 实际SQL: UPDATE user SET id=1 WHERE id=1 (username字段被忽略)

// 解决方案: 使用 LambdaUpdate
lambdaUpdate()
    .set(User::getUsername, null)  // 明确设置为 null
    .eq(User::getId, 1L)
    .update();
// 实际SQL: UPDATE user SET username=NULL WHERE id=1
```

#### 坑2: one() 方法查询多条会报错

```java
// ❌ 查询到多条记录会抛异常
User user = lambdaQuery()
    .eq(User::getStatus, 1)
    .one();  // TooManyResultsException

// ✅ 使用 list() 或添加 LIMIT
List<User> users = lambdaQuery()
    .eq(User::getStatus, 1)
    .list();

// 或
User user = lambdaQuery()
    .eq(User::getStatus, 1)
    .last("LIMIT 1")
    .one();
```

#### 坑3: 自动填充不生效

```java
// 原因1: 没有添加 MetaObjectHandler
// 原因2: 使用了自定义SQL
// 原因3: 使用了 updateById(entity) 但字段为 null

// 解决方案:
// 1. 配置 MetaObjectHandler
// 2. 自定义SQL手动设置时间
// 3. 使用 LambdaUpdate 明确设置
```

#### 坑4: 逻辑删除后仍能查到数据

```java
// 原因: 自定义SQL不会自动拼接逻辑删除条件

// ❌ 自定义SQL
@Select("SELECT * FROM user WHERE id = #{id}")
User selectById(Long id);  // 会查到已删除的数据

// ✅ 手动添加条件
@Select("SELECT * FROM user WHERE id = #{id} AND deleted = 0")
User selectById(Long id);
```

#### 坑5: 条件构造器使用 or() 的坑

```java
// ❌ 错误写法
lambdaQuery()
    .eq(User::getStatus, 1)
    .or()
    .eq(User::getType, 2)
    .eq(User::getLevel, 3)
// SQL: WHERE status = 1 OR type = 2 AND level = 3
// 等价于: WHERE status = 1 OR (type = 2 AND level = 3)

// ✅ 正确写法 - 使用嵌套
lambdaQuery()
    .eq(User::getStatus, 1)
    .or(wrapper -> wrapper
        .eq(User::getType, 2)
        .eq(User::getLevel, 3))
// SQL: WHERE status = 1 OR (type = 2 AND level = 3)
```

### 12.4 项目总结

**hmall 项目中的 MybatisPlus 使用特点:**

1. **配置简洁**
   - 仅配置分页插件
   - 使用默认配置（如雪花算法ID生成）

2. **Lambda 优先**
   - 大量使用 `lambdaQuery()` 和 `lambdaUpdate()`
   - 代码更简洁，类型安全

3. **业务乐观锁**
   - 通过状态字段控制并发
   - 不使用 @Version 注解

4. **自定义 SQL**
   - 使用注解方式（@Update）
   - 处理复杂业务（库存扣减、余额变动）

5. **批量操作**
   - 使用 `executeBatch` 批量执行自定义SQL
   - 提高性能

6. **微服务架构**
   - 每个服务独立配置 MybatisPlus
   - 通过 hm-common 共享配置

---

## 附录: 快速查询表

### 常用注解速查

| 注解 | 作用 | 示例 |
|------|------|------|
| @TableName | 指定表名 | `@TableName("user")` |
| @TableId | 指定主键及生成策略 | `@TableId(type = IdType.AUTO)` |
| @TableField | 指定字段映射 | `@TableField("user_name")` |
| @Version | 乐观锁版本号 | `@Version` |
| @TableLogic | 逻辑删除字段 | `@TableLogic` |

### 条件构造器速查

| 方法 | SQL | 示例 |
|------|-----|------|
| eq | = | `eq(User::getAge, 18)` |
| ne | != | `ne(User::getAge, 18)` |
| gt | > | `gt(User::getAge, 18)` |
| ge | >= | `ge(User::getAge, 18)` |
| lt | < | `lt(User::getAge, 18)` |
| le | <= | `le(User::getAge, 18)` |
| between | BETWEEN | `between(User::getAge, 18, 30)` |
| like | LIKE '%值%' | `like(User::getName, "张")` |
| likeLeft | LIKE '%值' | `likeLeft(User::getName, "三")` |
| likeRight | LIKE '值%' | `likeRight(User::getName, "张")` |
| in | IN | `in(User::getId, 1, 2, 3)` |
| isNull | IS NULL | `isNull(User::getEmail)` |
| isNotNull | IS NOT NULL | `isNotNull(User::getEmail)` |
| orderByAsc | ORDER BY ASC | `orderByAsc(User::getAge)` |
| orderByDesc | ORDER BY DESC | `orderByDesc(User::getCreateTime)` |
| and | AND | `and(w -> w.eq(...))` |
| or | OR | `or(w -> w.eq(...))` |

### Service 常用方法速查

| 方法 | 说明 |
|------|------|
| save(entity) | 插入一条记录 |
| saveBatch(list) | 批量插入 |
| saveOrUpdate(entity) | 存在则更新，否则插入 |
| removeById(id) | 根据ID删除 |
| removeByIds(ids) | 批量删除 |
| remove(wrapper) | 条件删除 |
| updateById(entity) | 根据ID更新 |
| update(wrapper) | 条件更新 |
| getById(id) | 根据ID查询 |
| listByIds(ids) | 批量查询 |
| list(wrapper) | 条件查询 |
| getOne(wrapper) | 查询单条 |
| count(wrapper) | 统计数量 |
| lambdaQuery() | Lambda查询 |
| lambdaUpdate() | Lambda更新 |

---

## 总结

### 面试准备重点

1. **基础概念** (必问)
   - MybatisPlus 与 MyBatis 的区别
   - 三层架构 (Mapper/Service/Controller)
   - 主键生成策略

2. **条件构造器** (高频)
   - Lambda 方式的优势
   - 常用条件方法
   - or() 的正确使用

3. **并发控制** (高频)
   - 业务乐观锁的实现
   - @Version 注解乐观锁
   - 悲观锁 (SELECT FOR UPDATE)

4. **性能优化** (高频)
   - 批量操作的优化
   - 分页查询的优化
   - N+1 问题的解决

5. **插件机制** (中频)
   - 分页插件的配置
   - 乐观锁插件
   - 防全表更新删除插件

6. **高级特性** (加分项)
   - 逻辑删除
   - 自动填充
   - 多租户

### 项目实战经验

基于 hmall 项目，你可以这样回答面试官:

**"在我的项目中，我们使用了 MybatisPlus 作为持久层框架。主要使用了以下特性:"**

1. **Lambda 条件构造器**: 大量使用 `lambdaQuery()` 和 `lambdaUpdate()`，代码简洁且类型安全

2. **业务乐观锁**: 在订单支付、库存扣减等场景，通过状态字段实现并发控制，避免了超卖等问题

3. **批量操作**: 使用 `executeBatch` 批量执行自定义 SQL，提高了库存扣减的性能

4. **自定义 SQL**: 对于复杂的业务逻辑（如库存增减、余额变动），使用 @Update 注解编写原生 SQL

5. **分页插件**: 配置了分页插件，并设置了 maxLimit 防止大数据量查询

**"通过 MybatisPlus，我们显著减少了代码量，提高了开发效率，同时保证了代码的可维护性和性能。"**

---

**祝你面试顺利! 🎉**

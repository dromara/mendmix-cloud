**黄金位置放个小广告→**欢迎加交流群：230192763（不错，这是一个新群来的）
## Introduction
**jeesuite-libs**是整理的近几年封装的一些基础组件包，计划陆续整理成一个java后台服务开发套件，包括整合dubbo服务化，rest API接口发布等。目前这些模块可用于生产环境，1.0版在千万级会员、日UV200万某互联网公司稳定运行。

**原则**
- 不造轮子、全部基于主流的框架如：mybatis、kafka、jedis等封装
- 只做增强不修改依赖框架本身、可自由升级依赖框架版本
- 封装的目标：更简单、更灵活。
---
**release版已经上传maven中心仓库**
* [sonatype](https://oss.sonatype.org/content/repositories/releases/com/jeesuite/) 
* [http://mvnrepository.com/search?q=jeesuite](http://mvnrepository.com/search?q=jeesuite)

## doc
* jeesuite-common
* jeesuite-common2
* [jeesuite-kafka](./docs/kafka.md) 
* [jeesuite-cache](./docs/cache.md) 
* [jeesuite-scheduler](./docs/scheduler.md)
* [jeesuite-mybatis](./docs/mybatis.md) 
* [jeesuite-rest](./docs/rest.md) 
* [jeesuite-confcenter](./docs/confcenter.md)

--- 
## Version Plan
### V1.0.3 
#### cache模块
- 针对某些情况哨兵模式jedis切换的问题做了兼容处理

### mybatis模块
- 支持批量更新缓存支持

#### scheduler模块
- 支持增加集群节点重新分配job执行节点

---

## Version History
### V1.0.2 
#### cache模块
- 修复1.0.1本地缓存更新的逻辑问题
- 考虑多个应用使用同一缓存服务器，支持配置bcastScope区别不同应用缓存更新事件

#### kafka模块
- 生产者端数据监控优化提供对外API
- consumer offsetcheker 兼容新旧版consumer API

### mybatis模块
- mybatis插件注册机制修改及重构
- @cahce标注支持聚合函数结果自动缓存
- 提供EntityCacheHelper工具类，支持方法内手动写入缓存并自动缓存管理

#### scheduler模块
- 兼容增加job新旧版本同时运行逻辑
- JobContext 增加getActiveNodes方法

---
### V1.0.1
#### kafka模块
- 兼容遗留kafka系统、支持发送和接收无封装的消息
- 生产者端监控采集支持
- new consumer API优化

#### cache模块
- 一级缓存增加支持ehcache、原使用guava cache
- 增加事务回滚缓存同步回滚接口
- 一级缓存增加分布式开关(distributedMode 默认true)


### mybatis模块
- 支持自定义配置文件名
- 增加@CacheEvictCascade注解支持
- 简化@Cache注解选项
- 优化缓存key生成规则

### rest模块
- 优化异常处理
- 优化通用日志filter

#### scheduler模块
- 增加当节点执行逻辑判断
- 增加兼容注册中心不可用逻辑
- 增加支持控制台强制更新下次执行时间

---
### V1.0
#### kafka模块
- 基于spring封装简化配置和调用方式
- 基于配置新旧两版Consumer API兼容支持
- 支持二阶段处理，即：fetch线程同步处理和process线程异步处理
- 消费成功业务处理失败自动重试或自定义重试支持
- process线程池采用LinkedTransferQueue，支持线程回收和队列大小限制，确保系统崩溃等不会有数据丢失。
- 支持特殊场景发送有状态的消息（如：同一个用户的消息全部由某一个消费节点处理）
- 消费者端监控采集（通过jeesuite-admin-web展现）

#### cache模块
- 基于配置支持单机、哨兵、分片、集群模式自由切换
- 更加简单的操作API封装
- 一级缓存支持（本地缓存）、分布式场景多节点自动通知
- 多组缓存配置同时支持

#### mybatis模块
- 代码生成、自动CRUD、可无缝对接mybaits增强框架Mapper
- 基于properties配置多数据源支持，无需修改XML
- 读写分离，强制读主库等
- 基于注解自动缓存管理
- 简单分库路由（不支持join等跨库操作）

#### scheduler模块
- 支持分布式保证单节点执行
- 支持失败节点转移
- 支持多节点下并行计算
- 支持控制台(jeesuite-admin-web)任务监控、开停、动态修改调度时间策略、手动触发执行

#### confcenter模块
- 应用启动加载远程配置
- 多环境多应用版本支持
- 本地远程配置自由切换
- 应用私有配置、全局配置同步支持

#### rest模块
- 自动resonse封装（xml、json）
- 自动友好错误

#### common2模块
- 分布式锁
- 分布式全局ID生成器

---
### 你可以下载集成了所有模块的demo[jeesuite-demo](https://github.com/vakinge/jeesuite-demo) 

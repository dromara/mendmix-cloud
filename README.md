## Introduction
**jeesuite-libs**是整理的近几年封装的一些基础组件包，计划陆续整理成一个java后台服务开发套件，包括整合dubbo服务化，rest API接口发布等。目前这些模块都在千万级会员、日UV百万线上稳定运行，可用于生产环境。
---
## doc
* jeesuite-common
* jeesuite-common2
* [jeesuite-kafka](./docs/kafka.md) 
* [jeesuite-cache](./docs/cache.md) 
* [jeesuite-scheduler](./docs/scheduler.md)
* [jeesuite-mybatis](./docs/mybatis.md) 
* [jeesuite-rest](./docs/rest.md) 
* [jeesuite-confcenter](./docs/confcenter.md)
* [jeesuite-admin-web](./docs/admin.md) 

---
## Version History
### V1.0.2 
#### cache模块
- 修复1.0.1本地缓存更新的逻辑问题
- 考虑多个应用使用同一缓存服务器，支持配置bcastScope区别不同应用缓存更新事件

---
### V1.0.1
#### kafka模块
- 兼容遗留kafka系统、支持发送和接收无封装的消息
- 生产者端监控采集支持
- new consumer API优化及其大量测试

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

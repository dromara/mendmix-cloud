**黄金位置放个小广告→**欢迎加交流群：230192763 （不限于讨论该框架热爱技术就行）
## 简介
**jeesuite-libs**是整理的近几年封装的一些基础组件包，计划陆续整理成一个java后台服务开发套件，包括整合dubbo服务化，rest API接口发布等。目前这些模块可用于生产环境，1.0版在千万级会员、日UV200万某互联网公司稳定运行。

**原则**
- 不造轮子、全部基于主流的框架如：mybatis、kafka、jedis等封装
- 只做增强不修改依赖框架本身、可自由升级依赖框架版本
- 封装的目标：更简单、更灵活。

**补充说明**
- 类似于配置中心、定时任务为什么不集成成熟的同类框架如：disconf、elastic-job、xxl-job等呢？
因为以上各种框架功能繁多、在这个功能上做的比较深入同时就带来了逻辑复杂、不便于自行定制修改、另外依赖包众多、让项目臃肿不堪。所以就自己开发
类似功能、只做核心功能、让依赖和代码量尽量的少。

---
**release版已经上传maven中心仓库**
* [sonatype](https://oss.sonatype.org/content/repositories/releases/com/jeesuite/) 
* [http://mvnrepository.com/search?q=jeesuite](http://mvnrepository.com/search?q=jeesuite)

## 总体功能模块图&roadmap
![image](http://7xq7jj.com1.z0.glb.clouddn.com/jeesuite.png?1)

## 技术栈（包括已实现和计划实现的部分）
- 缓存：redis、ehcache、guava cache
- 数据库:mysql、mybatis
- 消息系统：kafka
- rest：jesery、Jackson
- 定时任务：zookeepr、quartz
- 文件服务： qiniu、fastFDS、netty
- 基础应用框架：spring、dubbo
- 日志：log4j2、kafka、flume、elasticsearch、kibana，spark
- 性能监控：
  1. Pinpoint
  2. [jeesuite-monitor](http://git.oschina.net/vakinge/jeesuite-monitor) `(轻量级方案)`
- 链路跟踪：Zipkin/brave 
- API网关：
  1. kong
  2. [jeesuite-apikeeper](http://git.oschina.net/vakinge/jeesuite-apikeeper) `(轻量级方案)`

## 文档
* [jeesuite-common2](./docs/common.md)
* [jeesuite-common2](./docs/common2.md)
* [jeesuite-kafka](./docs/kafka.md) 
* [jeesuite-cache](./docs/cache.md) 
* [jeesuite-scheduler](./docs/scheduler.md)
* [jeesuite-mybatis](./docs/mybatis.md) 
* [jeesuite-rest](./docs/rest.md) 
* [jeesuite-confcenter](./docs/confcenter.md)
* [jeesuite-filesystem](./docs/filesystem.md)
* [jeesuite-log](./docs/log.md)

--- 
## 计划
#### cache模块（1.0.3） 
- sentinel模式支持读写分离
- 应用级namespace支持
- 对象压缩

#### confcenter（1.0.1） 
- 基于ZK配置更新通知

#### rest模块  (1.0.1)
- 自定义异常处理支持
- 日志统一采集支持

---

## 功能列表
#### kafka模块 (1.0.2)
- 基于spring封装简化配置和调用方式
- 基于配置新旧两版Consumer API兼容支持
- 支持二阶段处理，即：fetch线程同步处理和process线程异步处理
- 消费成功业务处理失败自动重试或自定义重试支持
- process线程池采用`LinkedTransferQueue`，支持线程回收和队列大小限制，确保系统崩溃等不会有数据丢失。
- 支持特殊场景发送有状态的消息（如：同一个用户的消息全部由某一个消费节点处理）
- producer、consumer端监控数据采集，由（[jeesuite-admin](http://git.oschina.net/vakinge/jeesuite-admin)）输出
- 兼容遗留kafka系统、支持发送和接收无封装的消息

#### cache模块 (1.0.2)
- 基于配置支持单机、哨兵、分片、集群模式自由切换
- 更加简单的操作API封装
- 一级缓存支持（ehcache & guava cache）、分布式场景多节点自动通知
- 多组缓存配置同时支持 （一个应用多个redis server）
- 分布式模式开关

#### mybatis模块 (1.0.3)
- 代码生成、自动CRUD、可无缝对接mybaits增强框架Mapper
- 基于properties配置多数据源支持，无需修改XML
- 读写分离，强制读主库等
- 基于注解自动缓存管理（所有查询方法结果自动缓存、自动更新，事务回滚缓存同步回滚机制）
- 简单分库路由（不支持join等跨库操作）

#### scheduler模块 (1.0.6)
- 支持分布式保证单节点执行（按节点平均分配job）
- 支持failvoer，自动切换故障节点
- 支持多节点下并行计算
- 支持无注册中心单机模式
- 支持自定义重试策略
- 支持配置持久化（启动加载、变更保存）
- 支持控制台（[jeesuite-admin](http://git.oschina.net/vakinge/jeesuite-admin)）任务监控、开停、动态修改调度时间策略、手动触发执行

#### confcenter模块  (1.0.0)
- 应用启动加载远程配置
- 多环境多应用版本支持
- 本地远程配置自由切换
- 应用私有配置、全局配置同步支持

#### rest模块  (1.0.1)
- 自动resonse封装（xml、json）
- i18n
- request、response日志记录
- 自动友好错误
- 校验框架

#### common2模块  (1.0.0)
- 分布式锁
- 分布式全局ID生成器
- excel导入导出（支持大文件操作）

#### filesystem模块  (1.0.0)
- 七牛文件服务支持
- fastDFS文件系统支持
- 支持spring集成

---
### 你可以下载集成了所有模块的demo[jeesuite-demo](http://git.oschina.net/vakinge/jeesuite-demo) 
### jeesuite统一管理平台[jeesuite-admin](http://git.oschina.net/vakinge/jeesuite-admin) 
#### 集成实践项目：[https://gitee.com/vakinge/oneplatform](https://gitee.com/vakinge/oneplatform)
**交流群(提供开发视频教程)**：230192763🈵 ，61859839

## 简介
**jeesuite-libs**分布式架构开发套件。包括缓存(一二级缓存、自动缓存管理)、队列、分布式定时任务、文件服务(七牛、阿里云OSS、fastDFS)、日志、搜索、代码生成、API网关、配置中心、统一认证平台、分布式锁、分布式事务、集成dubbo、spring boot支持、统一监控等。所有release版都经过严格测试并在生产环境稳定运行2年+。


##官网
[http://www.jeesuite.com/](http://www.jeesuite.com/) 

## 文档
[http://www.jeesuite.com/docs](http://www.jeesuite.com/docs/index.html) 

## 愿景
服务中小企业、减低架构成本、整体方案开箱即用。
## 原则
 - 不造轮子、全部基于成熟主流开发框架封装。
 - 只做增强不修改依赖框架本身，可自由升级版本。
 - 持续更新、所有release版本经过严格测试和线上验证。
 - 贴近业务场景、只做有用的功能。
 - 高度灵活、每个模块可以独立使用。

---
## 版本
* [sonatype](https://oss.sonatype.org/content/repositories/releases/com/jeesuite/) 
* [http://mvnrepository.com/search?q=jeesuite](http://mvnrepository.com/search?q=jeesuite)

## 关联项目
 - 配置中心
  - [http://git.oschina.net/vakinge/jeesuite-config](http://git.oschina.net/vakinge/jeesuite-config)
  - [https://github.com/vakinge/jeesuite-config](https://github.com/vakinge/jeesuite-config)
 - 应用开发平台
  - [https://gitee.com/vakinge/oneplatform](https://gitee.com/vakinge/oneplatform)
  - [https://github.com/vakinge/oneplatform](https://github.com/vakinge/oneplatform)

---

## 功能列表
#### cache模块
- 基于配置支持单机、哨兵、分片、集群模式自由切换
- 更加简单的操作API封装
- 一级缓存支持（ehcache & guava cache）、分布式场景多节点自动通知
- 多组缓存配置同时支持 （一个应用多个redis server）
- 分布式模式开关

#### kafka模块 
- 基于spring封装简化配置和调用方式
- 基于配置新旧两版Consumer API兼容支持
- 支持二阶段处理，即：fetch线程同步处理和process线程异步处理
- 消费成功业务处理失败自动重试或自定义重试支持
- process线程池采用`LinkedTransferQueue`，支持线程回收和队列大小限制，确保系统崩溃等不会有数据丢失。
- 支持特殊场景发送有状态的消息（如：同一个用户的消息全部由某一个消费节点处理）
- producer、consumer端监控数据采集，由（[jeesuite-admin](http://git.oschina.net/vakinge/jeesuite-admin)）输出
- 兼容遗留kafka系统、支持发送和接收无封装的消息


#### mybatis模块
- 代码生成、自动CRUD、可无缝对接mybaits增强框架Mapper
- 基于properties配置多数据源支持，无需修改XML
- 读写分离，事务内操作强制读主库
- 基于注解自动缓存管理（所有查询方法结果自动缓存、自动更新，事务回滚缓存同步回滚机制）
- 自动缓存实现基于`jeesuite-cache`和`spring-data-redis`
- 分页组件
- 简单分库路由（不支持join等跨库操作）

#### scheduler模块
- 支持分布式保证单节点执行（按节点平均分配job）
- 支持failvoer，自动切换故障节点
- 支持多节点下并行计算
- 支持无注册中心单机模式
- 支持自定义重试策略
- 支持配置持久化（启动加载、变更保存）
- 支持控制台（[jeesuite-admin](http://git.oschina.net/vakinge/jeesuite-admin)）任务监控、开停、动态修改调度时间策略、手动触发执行


#### rest模块
- 自动resonse封装（xml、json）
- i18n
- request、response日志记录
- 自动友好错误
- 校验框架

#### filesystem模块
- 七牛文件服务支持
- 阿里云OSS文件服务支持
- fastDFS文件系统支持
- 支持spring集成
- 配置式切换服务提供商


#### common模块
- 一些常用工具类

#### common2模块（需要依赖一些组件或者基础设置）
- 分布式锁
- 分布式全局ID生成器
- excel导入导出

#### jeesuite-springboot-starter模块
- springboot集成支持




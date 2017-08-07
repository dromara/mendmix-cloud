**黄金位置放个小广告→**欢迎加交流群：230192763 （不限于讨论该框架热爱技术就行）
## 简介
**jeesuite-libs**是整理的近几年封装的一些基础组件包，计划陆续整理成一个java后台服务开发套件，包括缓存、消息队列、db操作(读写分离、分库路由、自动crud)、定时任务、文件系统、api网关、配置中心、分布式锁、搜索、日志、统一监控、集成dubbo、springboot等。所有release版都经过严格测试并在生产环境稳定运行。[项目模板jeesuite-bestpl](http://git.oschina.net/vakinge/jeesuite-bestpl) 

## 愿景
服务中小企业、减低架构成本、整体方案开箱即用。
## 原则
 - 不造轮子、全部基于成熟主流开发框架封装。
 - 只做增强不修改依赖框架本身，可自由升级版本。
 - 持续更新、所有release版本经过严格测试和线上验证。
 - 贴近业务场景、只做有用的功能。
 - 高度灵活、每个模块可以独立使用。


**补充说明**
- 类似于配置中心、定时任务为什么不集成成熟的同类框架如：disconf、elastic-job、xxl-job等呢？
因为以上各种框架功能繁多、在这个功能上做的比较深入同时就带来了逻辑复杂、不便于自行定制修改、另外依赖包众多、让项目臃肿不堪。所以就自己开发
类似功能、只做核心功能、让依赖和代码量尽量的少。
- 版本问题
各个模块版本升级都保证向下严格兼容，所以已经使用的可以无缝升级新的release版本。

---
**release版已经上传maven中心仓库**
* [sonatype](https://oss.sonatype.org/content/repositories/releases/com/jeesuite/) 
* [http://mvnrepository.com/search?q=jeesuite](http://mvnrepository.com/search?q=jeesuite)

## 总体功能模块图&roadmap
![image](http://7xq7jj.com1.z0.glb.clouddn.com/jeesuite.png?1)

## 文档
请查看各个子模块readme文件，详细文档还在完善中。。


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

#### jeesuite-confcenter-client模块
- 应用基础配置客户端

#### filesystem模块
- 七牛文件服务支持
- fastDFS文件系统支持
- 支持spring集成


#### common模块
- 一些常用工具类

#### common2模块（需要依赖一些组件或者基础设置）
- 分布式锁
- 分布式全局ID生成器
- excel导入导出

#### jeesuite-springboot-starter模块
- springboot集成支持

---
### 你可以下载集成了所有模块的demo[jeesuite-bestpl](http://git.oschina.net/vakinge/jeesuite-bestpl) 
### jeesuite统一管理平台[jeesuite-admin](http://git.oschina.net/vakinge/jeesuite-admin) 
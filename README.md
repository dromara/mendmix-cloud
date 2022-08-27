<div align=center><img src="https://jeesuite-1251121413.cos.ap-guangzhou.myqcloud.com/logo.png" width="400" height="86" /></div>

## 资源索引
 - [快速开始](https://www.jeesuite.com/guide/getting-started.html)
 - [文档列表](https://www.jeesuite.com/docments/)
 - [快速上手-mendmix-tutorial](https://gitee.com/vakinge/mendmix-tutorial)
 - [集成项目-oneplatform](https://gitee.com/vakinge/oneplatform)
 
## 简介
`Mendmix`定位是一站式分布式开发架构开源解决方案及云原生架构技术底座。`Mendmix`提供了数据库、缓存、消息中间件、分布式定时任务、安全框架、网关以及主流产商云服务快速集成能力。基于`Mendmix`可以不用关注技术细节快速搭建高并发高可用基于微服务的分布式架构。

## 发展历程`jeesuite-libs`→`Mendmix`
2015年发布第一版取名叫`jeesuite-libs`一直沿用至今，定位是工具型软件，以各个模块能完全独立使用为前提。经过近8年的发展以及在多家大型公司技术中台、数字化转型、企业上云过程中锤炼，定制化越来越高，`jeesuite-libs`从原来一个个离散的点变成了一个逻辑完备的面，也就是形成了一整套分布式架构及云原生架构的解决方案。基于这种转变`jeesuite-libs`不再适合我们的发展方向，因此现在更名为`Mendmix`。

 
## 关于Mendmix
 - &#x2705;**寓意**：Mend+Mix，解决各种框架整合、各种场景的融合的问题、形成一整套完全自洽的解决方案
 - &#x2705;**理念**：融合、增强、包容、自洽
 - &#x2705;**原则**：最小依赖可运行；只做增强不修改依赖框架本；贴近业务场景只做有用的功能

## 最小运行依赖
>以下是包含了消息队列、定时任务、存储、全局锁、缓存、Mybatis增强等各种场景最小化运行依赖。如果需要支持分布式仅仅只需要增加必要的配置和第三方依赖包即可。
 - Spring + Springcloud必要组件
 - Mybatis，mybatis-spring,Druid
 - Quartz
 - 以及一些诸如guava,jackson的工具类库
 
## 功能图谱

模块 | 核心功能说明 | 其他说明
---|---|---
mendmix-common | http、json、加解密、异步、GUID等工具类以及整体架构一些规范性定义|
mendmix-common2 | 分布式锁、轻量级定时任务、全局workerId生成等依赖中间件的通用组件 | 
mendmix-spring | Spring工厂以及一些相关工具类、配置二次处理、一些运行机制规范定义 | 
mendmix-cache | 缓存中间件适配、多redis实例快速注册及管理 | 
mendmix-scheduler | 分布式定时任务，基于quartz、redis、zookeeper实现，支持日志上报、重试、多租户、并行处理，提供管理API |支持redis或zookeeper分布式协调 
mendmix-mybatis | CRUD增强、通用字段自动处理、自动缓存、读写分离、软删除、乐观锁、数据权限、安全审计 | 兼容Mapper3增强框架
mendmix-security | 认证和鉴权（接口权限）、session管理器、支持oauth2.0等 | 不依赖任何第三方权限框架
mendmix-logging | 多日志厂商适配、日志采集上报、应用日志动态刷新机制 |
mendmix-springweb | 接口规范定义，通用拦截器、mock用户、请求响应增强插件机制 | 
mendmix-springcloud-support | 组件自动注册、springcloud 组件增强 | 
mendmix-gateway | 认证、接口权限、审计日志、openAPI、响应统一包装、限流、防重复提交、命中缓存降级访问 | 默认集成mendmix-security模块
mendmix-amqp-adapter | 消息中间件适配，目前支持：内存队列、redis、kafka、rocketMQ、腾讯云及阿里云的云厂商MQ产品 | 
mendmix-cos-adapter | 文件存储适配，目前支持：minIO、七牛、阿里云、腾讯云、华为云、AWS的文件存储服务 | 


---
## 版本
* [sonatype](https://oss.sonatype.org/content/repositories/releases/com/mendmix/) 
* [https://search.maven.org/search?q=mendmix](https://search.maven.org/search?q=mendmix)

## 关于作者
 - 15年IT互联网老兵，熟悉微服务、k8s、云原生架构及各种分布式架构；
 - DDD领域驱动早期实践者,2012-2014年作为开源项目[koala企业级开发平台](https://gitee.com/openkoala/koala)核心开发主推领域驱动设计理念；
 - 荣获[2021年度海纳奖—分布式数据库十大优秀实践人物](https://baijiahao.baidu.com/s?id=1723175607837258012)

<img src="https://jeesuite.oss-cn-guangzhou.aliyuncs.com/2021-hainajiang.jpeg" width="350" height="360" />
 
 
## 🚀🚀知识星球🚀🚀
>欢迎加入我的知识星球。提供mendmix各种问题交流，定期分享架构实践、架构案例、面试技巧等。

<img src="https://www.jeesuite.com/assets/images/ads/zsxq-002.png" width="290" height="465" />

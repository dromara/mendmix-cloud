## Introduction
**jeesuite-libs**是整理的近几年封装的一些基础组件包，计划陆续整理成一个java后台服务开发套件，包括整合dubbo服务化，rest API接口发布等。目前这些模块都在线上稳定运行，可用于生产环境。


## moudules
* [jeesuite-common](http://git.oschina.net/vakinge/jeesuite-libs/wikis/jeesuite-common) 一些工具类，包括部分从别的项目分离出来的。
* [jeesuite-kafka](http://git.oschina.net/vakinge/jeesuite-libs/wikis/jeesuite-kafka) 基于kakfa高阶API封装，已经用于生产日处理消息2亿+
* [jeesuite-cache](http://git.oschina.net/vakinge/jeesuite-libs/wikis/jeesuite-cache) 基于jedis对redis命令分类封装。实现配置驱动单机、分片、集群模式自由切换、二级缓存支持(自动更新)、资源自动管理。
* [jeesuite-scheduler](http://git.oschina.net/vakinge/jeesuite-libs/wikis/jeesuite-scheduler) 定时任务。支持分布式环境单一节点执行、failover、动态开关、修改执行策略等
* [jeesuite-mybatis](http://git.oschina.net/vakinge/jeesuite-libs/wikis/jeesuite-mybatis) 基于mybatis封装，实现自动crud、读写分离、应用级透明分表分库路由、自动缓存及维护。所有特性已经稳定用于生产环境。(待发布)
* [jeesuite-spring](http://git.oschina.net/vakinge/jeesuite-libs/wikis/jeesuite-spring) spring工厂等
* [jeesuite-monitor-web](http://git.oschina.net/vakinge/jeesuite-libs/wikis/jeesuite-monitor-web) 以上组件监控管理页面(待发布)
* 持续增加中....
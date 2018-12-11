### 1.3.1
#### jeesuite-security
`jeesuite-security`是新增模块：可替代shiro或spring-security等安全框架，可以理解是shiro的轻量级版本，配置更简单，更加贴近我们业务场景。
 - 配置简单(初始化一个类即可)
 - 满足认证授权基本需求
 - 更加贴近日常使用业务场景
 - 可选本地session和共享session
 - 可选是否支持多端同时登录
 - dubbo、springboot跨服务登录状态传递支持
 
#### jeesuite-mybaits
 - 重构mybaits增强插件注册逻辑简化配置
 - 开放自定义mybatis插件hander接口
 - 增加敏感操作拦截mybatis插件hander
 - 支持无缝集成CRUD增强框架mapper
 - 升级mybatis版本去掉自动CRUD过期代码
 
#### jeesuite-kafka
 - 修复json消息反序列化漏处理header字段
 
#### jeesuite-scheduler
 - 新增单机模式任务监控支持

#### jeesuite-springboot-starter
 - 重构mybaits模块注册逻辑
 - scheduler模块注册兼容springboot2.x
 

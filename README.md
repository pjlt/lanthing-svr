# lanthing-svr
[![build](https://github.com/pjlt/lanthing-svr/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/pjlt/lanthing-svr/actions/workflows/maven.yml)

编译出输出两个文件：`ltsvr.jar`和`ltsig.jar`，默认不把配置文件打包进去，需要启动时指定参数`--spring.config.location=/path/to/spring.config.yml`。
配置文件的模板在`./config`。

`ltsvr.jar`还需要一个sqlite的数据库文件，用于读写、查询设备ID。数据库的格式可以参考`./resources/lanthing-svr.sqlite.tpl`，或者使用以下语句创建需要的表：
```sql
CREATE TABLE "unused_device_ids" (
	"id"	INTEGER NOT NULL UNIQUE,
	"deviceID"	INTEGER NOT NULL UNIQUE,
	PRIMARY KEY("id" AUTOINCREMENT)
)

CREATE TABLE "used_device_ids" (
	"id"	INTEGER NOT NULL UNIQUE,
	"deviceID"	INTEGER NOT NULL UNIQUE,
	PRIMARY KEY("id" AUTOINCREMENT)
)
```
后面会寻找更合理的方式管理设备ID。
# lanthing-svr
[![Java CI with Maven](https://github.com/pjlt/lanthing-svr/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/pjlt/lanthing-svr/actions/workflows/maven.yml)

`Lanthing`的服务器。

## 编译

标准的Maven构建。

## 部署

这是一件非常麻烦的事，如果只是希望"画面数据"、"声音数据"、"控制数据"使用自己的服务器，[自建中继服务器](https://github.com/pjlt/relay)即可。

没有特殊需求，比如二次开发，不建议自己部署lanthing-svr，这个过程只有沮丧。

部署步骤如下，不会列出每一条命令，只说明做什么和为什么：

1. 编译`lanthing-svr`，输出两个包`ltsvr.jar`和`ltsig.jar`
	- 只从`lanthing-svr`看，输出两个包是一件很奇怪的事：为什么不能合成一个`lanthing-svr.jar`？
	- 需要区分`ltsvr.jar`和`ltsig.jar`是`lanthing-pc`的架构决定的，即便是合成一个`lanthing-svr.jar`，内部也是分成两个独立的角色，分别监听自己的TCP端口。
2. 配置`config/ltsig.yml`，主要是`socket-svr.ip`和`socket-svr.port`两项。
	- `socket-svr`下`enable-ssl`以及后面的配置项不建议配置。内置的TLS支持更多是用于开发测试，真正搭服务时，应该使用一个反向代理去做这件事，比如nginx。
3. 启动`ltsig.jar`
	- java -jar -Dspring.config.location=/path/to/ltsig.yml /path/to/ltsig.jar
4. 生成设备ID
	- `ltsvr.jar`使用预先生成ID的方式，为所有新连接上来的设备分配ID。
	- ID使用`sqlite3`数据库存储，建表SQL如下：
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
	- 你可以手动插入自己想要的ID，或者使用[idgenerator](https://github.com/pjlt/idgenerator)自动生成大约9亿个乱序ID，生成的ID每20万存一个文件。
5. 配置`conf/ltsvr.yml`，配置项比较多，重点如下：
	- `spring.datasource.url`：指向sqlite3数据库文件
	- `server-address.relays`：中继服务器，非必须
	- `server-address.reflexes`：反射服务器，必须，且要有3个或以上。当前只支持STUN协议，未来会有自定义协议
	- `controlled-socket-svr`：port和ip必填，其它跟TLS相关的不要使用，理由同上
	- `controlling-socket-svr`：port和ip必填，其它跟TLS相关的不要使用，理由同上
	- `signaling`：指向你的`ltsig.jar`对外暴露的端口和IP，必填
6. 启动`ltsvr.jar`
	- java -jar -Dspring.config.location=/path/to/ltsvr.yml /path/to/ltsvr.jar
7. 配置反向代理
	- 这部分知识超出了这个README的范围，只提几个重点
	- 代理的是TCP/TLS长连接，而不是HTTP/HTTPS
	- 需要代理三个端口，`ltsig.jar`一个，`ltsvr.jar`两个
	- 你可能需要申请一个域名，或者直接使用IP
	- 你可能需要申请一张证书，或者使用自签证书
8. 哈哈，没完呢
9. 转到项目`lanthing-pc`，配置服务器地址、端口和证书
	- 将`lanthing-pc/certs/ISRG-Root.cert`里的内容，替换成你要用的证书
	- 修改`options-default.cmake`或`options-user.cmake`里的
		- LT_SERVER_ADDR：你的服务器地址
		- LT_SERVER_SVC_PORT：你的`controlled-socket-svr`对外暴露的端口
		- LT_SERVER_APP_PORT：你的`controlling-socket-svr`对外暴露的端口
10. 编译`lanthing-pc`，完成！
	- 你成功自建服务器
	- 要使用你的服务器，必须使用你编译的`lanthing-pc`
	- 你编译的`lanthing-pc`只能跟你编译的`lanthing-pc`通信，与官方GitHub下载的无法互通

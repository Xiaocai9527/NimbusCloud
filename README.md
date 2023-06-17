# NimbusCloud/雨云介绍
File server /Yunpan by netty  

开始这个项目最初的理由是我妈想看我的婚礼视频,我妈不怎么会用软件,不太可能用百度网盘分享给她.所以我想着能不能把我电脑里存放的视频直接通过 url 的方式用微信分享给她,然后点击 url 就可以观看了.

所以就开始编写代码了,计划编写一个文件服务器.我最开始是使用的最原始的 java   server socket 实现了第一个版本,后来因为工作原因接触到了 netty,觉得基于     java nio 的 netty 可能更适合我的需求.

服务器代码开发完后,面临的第二个问题是如何让外网也可以访问我的电脑资源呢?由于在中国的网络运营商是不会给你提供公网 ip的,所以必须要想其他方法.网上搜索了下,大都是内网穿透方案.一开始我并不想在这方面花费很多时间,所以我直接使用的免费的 ngork 方案.但是免费的使用过程中会发现,各种不稳定,带宽限制,而且每次断开重新启动后,ngrok 会生成一个不一样的域名.反正就是用着各种不爽,促使我想整一个云服务器.因为之前没有买过云服务器,查看之后发现还真贵.一番合计后,决定使用新用户身份购买一个轻量级的云服务器实现内网穿透,服务还是在家里电脑上.


## How to start/开始
* git clone git@github.com:xiaokun19931126/NimbusCloud.git
* ServerStarter class , fill in USER_NAME and PASSWORD
* ServerStarter class , getPath() method , return your file root path
* ./gradlew startJava 
* chrome open http://127.0.0.1:8080/, can visit your root file


## Show results /效果展示

不怎么会前端,索性直接通过服务端把 h5 内容生成后返回给浏览器渲染即可,如下首页.

![首页](/pictures/1.png)

浏览器自带的通过 http 协议可以播放 mp4 文件,如下,顺便还可以当一个家庭影院,平时上班的时候跑一些脚本自动下载电影.

![视频](/pictures/2.png)

而且还实现了将文件拖动到网页上传文件的功能,我有时候觉得一些需要收藏或者后期要阅读的 pdf 可以上传到电脑上,以便后期随时查看.chrome 有 pdf 的解析功能很好用.

![pdf](/pictures/3.png)
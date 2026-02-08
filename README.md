注:该版本是旧版，最新版未开源

# ShaoLiChengUHC
This is a Spigot/Paper Minecraft plugin: a UHC (Hardcore Survival Battle Royale) game plugin inspired by the Hoplite UHC gamemode.

ShaoLiChengUHC插件
这是一个适用于 Minecraft Spigot/Paper 服务器（Minecraft 版本：1.21.4）的服务端极限生存大逃杀(UHC)的游戏插件，实现了基础而丰富的功能。

📋 必需依赖项
要正常运行此插件，必须在您的服务器上安装以下 JAR 文件（请将它们放置在 plugins/ 目录中）：
以下部分插件的配置文件很重要并且比较复杂，可直接下载插件文件：

BetterStructures.jar （该插件可为游戏增加自定义建筑以及箱子里的奖励池，如想修改自定义建筑需自行查找相关资料，）

CustomStructures-1.9.1.jar

DecentHolograms-2.8.16.jar

ItemsAdder_4.0.9.jar （配置文件的材质包，需自行配置相关设置如端口）

LoneLibs_1.0.65.jar

PlaceholderAPI-2.11.6.jar

ProtocolLib.jar

TAB v5.0.7.jar

Terra-bukkit-6.6.1-BETA+83bc2c902-shaded.jar （可修改群系和修改矿物生成概率，对仿制hoplite起了重要作用）

worldedit-bukkit-7.3.11.jar


功能介绍
已实现hoplite的核心基础：UHC物品，群系，丰饶之角，死斗等等。  详细内容请看： https://afdian.com/item/8fbce15c2efa11f0b4c552540025c377

其他说明
插件灵感来自油管的百万粉丝SpeedSilver创建的服务器Hoplite的游戏玩法 此插件经过几十局和朋友的游玩后，可以保证游戏体验很好，感谢!

一些非必要的说明
1.修改服务端根目录下的bukkit.yml  在文件最后换行粘贴：
worlds:
  world:
    generator: Terra:OVERWORLD
这将使得服务器通过terra插件创建世界

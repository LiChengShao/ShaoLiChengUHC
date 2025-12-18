package com.shaolian.uhc;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class LobbyWorld {

    private Main plugin;

    public LobbyWorld(Main plugin) {
        this.plugin = plugin;
    }

    private World lobbyWorld = Bukkit.getWorld("lobby");


    public void createOrLoadLobbyWorld() {

        if (lobbyWorld == null) {
            plugin.getLogger().info("正在创建lobby世界......");
            WorldCreator creator = new WorldCreator("lobby");
            creator.environment(World.Environment.NORMAL);
            creator.type(WorldType.FLAT);
            lobbyWorld = creator.createWorld();

        } else {
            plugin.getLogger().info("Lobby已经存在，正在加载......");
            // 设置大厅世界的游戏规则
            lobbyWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
            lobbyWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            lobbyWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            lobbyWorld.setTime(12200); // 设置时间为黄昏
            lobbyWorld.setDifficulty(Difficulty.PEACEFUL);
            // 清除现有掉落物
            lobbyWorld.getEntities().stream()
                    .filter(entity -> entity.getType() == EntityType.ITEM)
                    .forEach(entity -> entity.remove());
        }

    }

    public void teleportToLobby(Player player) {
        if (lobbyWorld != null) {
            Location loc0 = new Location(lobbyWorld, 0, -56, 0);
            player.teleport(loc0);
        } else {
            plugin.getLogger().warning("无法将玩家传送至大厅，因为大厅世界不存在。");
        }
    }



}

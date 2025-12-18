package com.shaolian.uhc;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Random;

public class FinalWorld {

    private Main plugin;

    public FinalWorld(Main plugin) {
        this.plugin = plugin;
        createFinalWorld();
        createOrLoadFinalWorldCopy();
    }

    private World FinalWorld = Bukkit.getWorld("FinalWorld");
    private World FinalWorldCopy = Bukkit.getWorld("FinalWorldCopy");



    // 添加新方法：创建FinalWorld虚空世界
    public void createFinalWorld() {
        World FinalWorld = Bukkit.getWorld("FinalWorld");
        if (FinalWorld == null) {
            plugin.getLogger().info("正在创建FinalWorld世界......");
            // 创建虚空世界
            WorldCreator creator = new WorldCreator("FinalWorld")
                    .generator(new FinalWorld.VoidGenerator())
                    .environment(World.Environment.NORMAL);
            FinalWorld = creator.createWorld();

            // 设置世界规则
            FinalWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            FinalWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            FinalWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            FinalWorld.setTime(6000); // 设置时间为中午
            plugin.getLogger().info("FinalWorld虚空世界创建成功");
        }
    }


    public void createOrLoadFinalWorld() {
        if (FinalWorld == null) {
            plugin.getLogger().info("正在创建FinalWorld世界......");
            WorldCreator creator = new WorldCreator("FinalWorld");
            creator.environment(World.Environment.NORMAL);
            FinalWorld = creator.createWorld();
        } else {
            plugin.getLogger().info("FinalWorld已经存在，正在加载......");
            // 设置大厅世界的游戏规则
            FinalWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            FinalWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            FinalWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            FinalWorld.setTime(12200); // 设置时间为黄昏
            FinalWorld.setDifficulty(Difficulty.EASY);
            // 清除现有掉落物
            FinalWorld.getEntities().stream()
                    .filter(entity -> entity.getType() == EntityType.ITEM)
                    .forEach(entity -> entity.remove());
        }
    }

    // 添加虚空世界生成器
    private class VoidGenerator extends ChunkGenerator {
        @Override
        public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
            return createChunkData(world);
        }
    }
    public void createOrLoadFinalWorldCopy() {

        if (FinalWorldCopy == null) {
            plugin.getLogger().info("正在创建FinalWorldCopy世界......");
            WorldCreator creator = new WorldCreator("FinalWorldCopy");
            creator.environment(World.Environment.NORMAL);
            FinalWorldCopy = creator.createWorld();

        } else {
            plugin.getLogger().info("FinalWorldCopy已经存在，正在加载......");
            // 设置大厅世界的游戏规则
            FinalWorldCopy.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            FinalWorldCopy.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            FinalWorldCopy.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            FinalWorldCopy.setTime(12200); // 设置时间为黄昏
            FinalWorldCopy.setDifficulty(Difficulty.EASY);
            // 清除现有掉落物
            FinalWorldCopy.getEntities().stream()
                    .filter(entity -> entity.getType() == EntityType.ITEM)
                    .forEach(entity -> entity.remove());
        }
    }


    public void teleportTofinalWorld(Player player) {
        // 确保FinalWorld已初始化
        createOrLoadFinalWorld();

        if (FinalWorld != null) {
            // 获取玩家的队伍信息
            String teamName = GameManager.playerTeams.get(player.getUniqueId());
            // 定义不同队伍的传送位置
            Location locationPink = new Location(FinalWorld, -114, 54, 0); // 粉队位置
            Location locationAqua = new Location(FinalWorld, 114, 54, 0); // 蓝队位置
            Location locationBlack = new Location(FinalWorld, 0 , 54 ,114); // 黑队位置
            Location locationGreen = new Location(FinalWorld, 0, 54, -114); // 绿队位置
            // 根据队伍信息传送玩家
            if (teamName != null) {
                switch (teamName) {
                    case "pink":
                        player.teleport(locationPink);
                        break;
                    case "aqua":
                        player.teleport(locationAqua);
                        break;
                    case "black":
                        player.teleport(locationBlack);
                        break;
                    case "green":
                        player.teleport(locationGreen);
                        break;
                    default:
                        player.teleport(locationGreen); // 默认传送到世界出生点
                        break;
                }

                //给予短暂的黑暗效果
                giveDarknessEffect(player);
                //全局对所有玩家播放末影龙的叫声
                for (Player player1 : Bukkit.getOnlinePlayers()) {
                    player1.playSound(player1.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                }


        } else {
            plugin.getLogger().warning("无法将玩家传送至FinalWorld，因为FinalWorld == null");
            }
        }
    }




    public void giveDarknessEffect(Player player) {
        // 初始黑暗效果
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 3 * 20 , 4));

        // 使用BukkitRunnable实现渐变恢复
        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 20 * 3;

            @Override
            public void run() {
                if (ticks >= duration) {
                    this.cancel();
                    return;
                }

                // 计算当前效果强度（从1逐渐减弱到0）
                float strength = 1 - (float)ticks / duration;

                // 更新黑暗效果
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20,(int)(strength * 3)
                ));

                ticks = ticks + 20;
            }
        }.runTaskTimer(plugin, 3 * 20L, 20L);
    }





}

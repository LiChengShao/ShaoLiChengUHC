package com.shaolian.uhc;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class Border {

    private Main plugin;
    private FinalWorld finalWorld;

    public Border(Main plugin) {
        this.plugin = plugin;
        finalWorld = new FinalWorld(plugin);
    }

    //边界大小
    public static int overWorldSize = 0;
    public static int currentSize = 0;
    private Map<Player, Long> playersOutsideBorder = new HashMap<>();

    public void setWorldBorder(int size) {
        World world = Bukkit.getWorld("world");
        if (world != null) {
            WorldBorder border = world.getWorldBorder();
            Location worldLoc = new Location(world, 0, 0, 0);
            border.setCenter(worldLoc);
            border.setSize(size);
            border.setWarningDistance(5);
            border.setWarningTime(15);

            new BukkitRunnable() {
                @Override
                public void run() {
                    overWorldSize = (int)border.getSize();
                }
            }.runTaskTimer(plugin,  0L, 20L);

            //XXmin之后开始收缩,收缩XX分钟
            new BukkitRunnable() {
                @Override
                public void run() {
                    border.setSize(200, 40 * 60);
                }
            }.runTaskLater(plugin, 12 * 60 * 20 + 30L);


            plugin.getLogger().info("世界边界已设置! 大小: " + size);
        } else {
            plugin.getLogger().warning("无法设置世界边界。world为null");
        }
    }

    public void setNetherBorder(int size) {
        World nether = Bukkit.getWorld("world_nether");

        if (nether != null) {
            WorldBorder border = nether.getWorldBorder();
            Location netherLoc = new Location(nether, 0, 0, 0);
            border.setCenter(netherLoc);
            border.setSize(size);
            border.setWarningDistance(5);
            border.setWarningTime(15);

            // 60分钟后收缩到0
            new BukkitRunnable() {
                @Override
                public void run() {
                    border.setSize(100, 40 * 60);
                }
            }.runTaskLater(plugin, 12 * 60 * 20 + 30L);

            plugin.getLogger().info("地狱的世界边界已设置! 大小: " + size);
        } else {
            plugin.getLogger().warning("无法设置世界边界：nether为null");
        }
    }

    //设置FinalWorld的边界
    public void teleportTofinalWorld(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for(Player player : Bukkit.getOnlinePlayers()){
                    finalWorld.teleportTofinalWorld(player);
                }
                setFinalWorldBorder(300);//设置FinalWorld边界
            }
        }.runTaskLater(plugin,  (52 * 60 + 30) * 20L);

        World finalWorld1 = Bukkit.getWorld("FinalWorld");
        shrinkParticleBorder(finalWorld1, 1, 10 * 60 * 20);
    }

    public void setFinalWorldBorder(int size) {
        currentSize = size;
        World world = Bukkit.getWorld("FinalWorld");
        if (world != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    displayParticleBorder(world, currentSize);
                    checkPlayersOutOfBorder(world, currentSize);
                }
            }.runTaskTimer(plugin, 0L, 10L);


            plugin.getLogger().info("世界边界已设置! 大小: " + size);
        } else {
            plugin.getLogger().warning("无法设置世界边界。world为null");
        }
    }

    private void displayParticleBorder(World world, int size) {
        Location center = new Location(world, 0, 0, 0);
        double radius = size / 2.0;
        int numParticles = 180;

        for (int i = 0; i < numParticles; i++) {
            double angle = 2 * Math.PI * i / numParticles;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            for (int y = -64; y <= 255; y += 10) {
                Location particleLocation = new Location(world, x, y, z);
                world.spawnParticle(Particle.DUST, particleLocation, 1,
                        new Particle.DustOptions(Color.fromRGB(255, 0, 0), 10));
            }
        }
    }

    public void shrinkParticleBorder(World world, int targetSize, long duration) {
        double currentRadius = currentSize / 2.0;
        double targetRadius = targetSize / 2.0;
        //每秒移动多少格
        double step = (currentRadius - targetRadius) / (duration / 20.0);

        new BukkitRunnable() {
            private double currentRadius = currentSize / 2.0;

            @Override
            public void run() {
                if (currentRadius <= targetRadius) {
                    this.cancel();
                    currentSize = targetSize * 2;
                    return;
                }

                currentRadius -= step;
                currentSize = (int) (currentRadius * 2);
                displayParticleBorder(world, (int) (currentRadius * 2));
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void checkPlayersOutOfBorder(World world, int size) {
        Location center = new Location(world, 0, 0, 0);
        double radius = size / 2.0;
        double buffer = 3.0; // 缓冲区

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(world)) {
                double distance = player.getLocation().distance(center);
                if (distance > radius + buffer) {
                    // 玩家超出边界
                    player.sendTitle("", ChatColor.RED + "§l你超出了边界", 5, 10, 5);
                    long currentTime = System.currentTimeMillis();
                    if (!playersOutsideBorder.containsKey(player)) {
                        playersOutsideBorder.put(player, currentTime);
                    } else {
                        long timeOutside = currentTime - playersOutsideBorder.get(player);
                        if (timeOutside > 3000) {
                            // 超出边界超过3秒，每2秒扣1点生命值
                            player.damage(1.0);
                           // player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1, 1);
                        }
                    }
                } else {
                    // 玩家回到边界内
                    playersOutsideBorder.remove(player);

                }
            }
        }
    }






}

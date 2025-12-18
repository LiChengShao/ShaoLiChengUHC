package com.shaolian.uhc;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.block.Barrel;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AirDrop {
    private final Main plugin;
    private Location dropLocation;
    private Location dropLocation2;
    private Entity barrelEntity;
    private Block fenceBlock;
    private ItemFrame itemFrame;
    private boolean isDropping = false;
    public static int kongtouX;
    public static int kongtouZ;

    World world = Bukkit.getWorld("world");

    public AirDrop(Main plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("AirDrop 初始化完成");
    }

    public void spawnAirDrop() {
        Random random = new Random();
        // 在500格范围内随机位置
        int x = random.nextInt(1000) - 500;
        int z = random.nextInt(1000) - 500;
        int y = world.getHighestBlockYAt(x, z) + 50;

        kongtouX = x;
        kongtouZ = z;

        dropLocation = new Location(world, x, y, z);
        dropLocation2 = new Location(world, x, y, z);

        // 广播空投预告
        Bukkit.broadcastMessage(ChatColor.GOLD + "空投将在" + ChatColor.DARK_PURPLE + "3分钟" + ChatColor.GOLD  + "后降落在"
        + ChatColor.DARK_PURPLE + dropLocation.getBlockX() + ", " + ChatColor.DARK_PURPLE + dropLocation.getBlockZ());

        new BukkitRunnable() {
            @Override
            public void run() {
                // 生成木桶（作为FallingBlock）
                FallingBlock barrel = world.spawnFallingBlock(dropLocation, Material.BARREL.createBlockData());

                // 取消重力，以便后续手动控制下落速度
                barrel.setGravity(false);

                // 使用Bukkit的调度器来实现缓慢下落
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // 获取当前木桶的位置
                        Location currentLocation = barrel.getLocation();
                        Block belowBlock = currentLocation.subtract(0, 1, 0).getBlock();
                        Block belowBlock2 = currentLocation.subtract(0, 2, 0).getBlock();

                        // 生成烟雾粒子效果
                        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, currentLocation.clone().add(0, 1, 0),
                                5, 0.1, 0.3, 0.1, 0.02);

                        // 检查下方是否有方块，如果有则停止下落
                        if (belowBlock2.getType().isSolid()) {
                            this.cancel();
                            // 移除下落方块实体
                            barrel.remove();
                            // 在当前位置放置一个实际的木桶方块
                            Location barrelLocation = currentLocation.add(0, 1, 0);
                            barrelLocation.getBlock().setType(Material.BARREL);
                            Barrel barrel = (Barrel) barrelLocation.getBlock().getState();

                            // 生成随机战利品并放入木桶
                            ItemStack[] loot = generateWW2Loot();
                            for (ItemStack item : loot) {
                                barrel.getInventory().addItem(item);
                            }

                            // 在落地位置添加爆炸粒子效果
                            world.spawnParticle(Particle.EXPLOSION, currentLocation, 10, 0.5, 0.5, 0.5, 0.1);
                            // 在落地位置播放铁砧砸地的音效
                            world.playSound(currentLocation, Sound.BLOCK_ANVIL_LAND, 0.5f, 0.5f);
                            // 生成冲击波效果
                            world.createExplosion(currentLocation, 0f, false, false);

                            return;
                        }

                        // 缓慢下落，通过设置一个较小的向下速度向量
                        barrel.setVelocity(new Vector(0, -0.1, 0));

                    }
                }.runTaskTimer(plugin, 0L, 2L); // 每2刻（0.1秒）执行一次
            }
        }.runTaskLater(plugin, 3 * 60 * 20L);
    }



    private ItemStack[] generateWW2Loot() {
        Random random = new Random();
        List<ItemStack> loot = new ArrayList<>();

        // 随机添加战利品
        loot.add(new ItemStack(Material.IRON_INGOT, random.nextInt(7) + 5)); // 铁锭
        loot.add(new ItemStack(Material.GOLD_INGOT, random.nextInt(9) + 5)); // 金锭
        loot.add(new ItemStack(Material.DIAMOND, random.nextInt(2) + 2)); // 钻石
        loot.add(new ItemStack(Material.BREAD, random.nextInt(5) + 1)); // 面包
        loot.add(new ItemStack(Material.LEATHER, random.nextInt(3) + 1)); // 皮革
        loot.add(new ItemStack(Material.GUNPOWDER, random.nextInt(3) + 1)); // 火药
        loot.add(new ItemStack(Material.PAPER, random.nextInt(5) + 1)); // 纸张
        loot.add(new ItemStack(Material.COAL, random.nextInt(5) + 1)); // 煤炭

        // 随机添加稀有物品
        if (random.nextDouble() < 0.2) {
            loot.add(new ItemStack(Material.DIAMOND, random.nextInt(3)+2)); // 钻石
        }
        if (random.nextDouble() < 0.1) {
            loot.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1)); // 附魔金苹果
        }
        if (random.nextDouble() < 0.05) {
            loot.add(new ItemStack(Material.NETHERITE_CHESTPLATE, 1)); // 下界合金胸甲
        }

        return loot.toArray(new ItemStack[0]);
    }


}






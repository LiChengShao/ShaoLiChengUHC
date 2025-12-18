package com.shaolian.uhc.items;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Sniffer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SnifferItem implements Listener {
    private final JavaPlugin plugin;
    private final ItemStack specialItem;
    private final List<Material> possibleLoot = Arrays.asList(
            Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT, Material.IRON_INGOT,
            Material.TORCHFLOWER_SEEDS, Material.PITCHER_POD, Material.SNIFFER_EGG, Material.COAL,
            Material.AMETHYST_SHARD, Material.GLOW_BERRIES
    );
    private final Random random = new Random();

    public SnifferItem(JavaPlugin plugin) {
        this.plugin = plugin;
        this.specialItem = createSnifferItem();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("嗅探兽宝宝来了.");
    }

    public ItemStack createSnifferItem() {
        ItemStack snifferItem = new ItemStack(Material.BONE);
        ItemMeta meta = snifferItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "嗅探兽召唤物");
            snifferItem.setItemMeta(meta);
        }
        return snifferItem;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 检查玩家是否手持特殊物品
        if (item == null) {
            return;
        }
        // 检查玩家是否手持特殊物品
        if (item.isSimilar(specialItem)) {
            spawnMagicalSniffer(player);
        }
    }



    private void spawnMagicalSniffer(Player player) {
        World world = player.getWorld();

        // 计算生成位置：玩家前方2格，在地面上
        Location playerEyeLoc = player.getEyeLocation();
        Vector direction = playerEyeLoc.getDirection().normalize();
        Location targetLoc = playerEyeLoc.add(direction.multiply(2.0));

        Block highestBlock = world.getHighestBlockAt(targetLoc.getBlockX(), targetLoc.getBlockZ());
        Location spawnLocation = highestBlock.getLocation().add(0.5, 1.0, 0.5); // 方块中心，表面上方一格

        // 如果计算出的地面过高或过低 (例如玩家在悬崖边)，则调整到玩家附近
        if (Math.abs(spawnLocation.getY() - player.getLocation().getY()) > 5 && highestBlock.getY() < player.getLocation().getY() -2) {
            // 备用方案：在玩家前方，与玩家同Y轴高度
            spawnLocation = player.getLocation().clone().add(player.getLocation().getDirection().setY(0).normalize().multiply(2.0));
            // 确保不在方块内 (简单处理)
            if (spawnLocation.getBlock().getType().isSolid() || spawnLocation.getBlock().getRelative(org.bukkit.block.BlockFace.UP).getType().isSolid()){
                spawnLocation.add(0,1,0);
            }
            if (spawnLocation.getBlock().getRelative(org.bukkit.block.BlockFace.DOWN).getType().isAir()){ //如果脚下是空气，尝试降低
                spawnLocation.setY(player.getLocation().getY());
            }
        }

        Sniffer sniffer = (Sniffer) world.spawnEntity(spawnLocation, EntityType.SNIFFER);

        // 设置嗅探兽属性
        sniffer.setAI(false);             // 禁止移动
        sniffer.setSilent(true);          // 静音
        sniffer.setInvulnerable(true);    // 无敌
        sniffer.setPersistent(false);     // 服务器重启或区块卸载时不保存此实体
        sniffer.setRotation(player.getLocation().getYaw(), 0); // 使其朝向与玩家相同

        final float initialYaw = sniffer.getLocation().getYaw(); // 保存初始朝向

        // --- 动画和行为序列 ---
        new BukkitRunnable() { // 步骤 1: 短暂延迟后开始动作
            @Override
            public void run() {
                if (!sniffer.isValid()) return; // 检查嗅探兽是否仍然有效

                // 步骤 2: 嗅探兽抬头
                Location currentLoc = sniffer.getLocation();
                // 创建一个新的Location对象来改变pitch，同时保持yaw和其他坐标不变
                sniffer.teleport(new Location(currentLoc.getWorld(), currentLoc.getX(), currentLoc.getY(), currentLoc.getZ(), initialYaw, -40f)); // -40度代表抬头

                new BukkitRunnable() { // 步骤 3: 抬头后延迟执行低头
                    @Override
                    public void run() {
                        if (!sniffer.isValid()) return;

                        // 步骤 4: 嗅探兽低头
                        Location currentLoc = sniffer.getLocation();
                        sniffer.teleport(new Location(currentLoc.getWorld(), currentLoc.getX(), currentLoc.getY(), currentLoc.getZ(), initialYaw, 40f)); // 40度代表低头

                        // 在嗅探兽附近播放一些粒子效果，模拟找到东西
                        world.spawnParticle(Particle.COMPOSTER, sniffer.getLocation().add(0, 0.5, 0), 30, 0.3, 0.3, 0.3, 0.05);
                        world.spawnParticle(Particle.HAPPY_VILLAGER, sniffer.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);

                        new BukkitRunnable() { // 步骤 5: 低头后延迟生成物品
                            @Override
                            public void run() {
                                if (!sniffer.isValid()) return;

                                // 步骤 6: 生成随机物品
                                Material randomMaterial = possibleLoot.get(random.nextInt(possibleLoot.size()));
                                ItemStack itemToDrop = new ItemStack(randomMaterial);

                                // 在嗅探兽脚下生成物品
                                world.dropItemNaturally(sniffer.getLocation(), itemToDrop);

                                new BukkitRunnable() { // 步骤 7: 生成物品后延迟消失
                                    @Override
                                    public void run() {
                                        if (sniffer.isValid()) {
                                            // 步骤 8: 嗅探兽消失
                                            sniffer.remove();
                                            //粒子效果
                                            world.spawnParticle(Particle.EXPLOSION, sniffer.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
                                            world.spawnParticle(Particle.SMOKE, sniffer.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                                        }
                                    }
                                }.runTaskLater(plugin, 20L); // 物品掉落1秒后消失 (20 ticks = 1 second)
                            }
                        }.runTaskLater(plugin, 30L); // 低头动画1.5秒后掉落物品 (30 ticks)
                    }
                }.runTaskLater(plugin, 20L); // 抬头动画1秒后执行低头 (20 ticks)
            }
        }.runTaskLater(plugin, 10L); // 生成嗅探兽0.5秒后开始动作 (10 ticks)
    }


}




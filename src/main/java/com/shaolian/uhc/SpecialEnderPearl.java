package com.shaolian.uhc;

import jdk.jfr.Label;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import dev.lone.itemsadder.api.CustomStack;

import java.util.Arrays;

public class SpecialEnderPearl implements Listener {
    //做特殊的末影珍珠，一键返回地表
    //我觉得这个东西可以放在职业购买里面
    private Main plugin;

    public SpecialEnderPearl(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

//        meta.setHideTooltip(true);
//        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
//        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
//        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
//        meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
//        meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
    public ItemStack createSpecialPearl() {
        ItemStack teleportEnder = CustomStack.getInstance("test:teleport_ender").getItemStack();
        return teleportEnder;
    }



    @EventHandler
    public void onPearlUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.isSimilar(createSpecialPearl())) {
            event.setCancelled(true);

            World world = player.getWorld();
            int x = player.getLocation().getBlockX();
            int z = player.getLocation().getBlockZ();
            int y = world.getHighestBlockYAt(x, z) + 1;

            Location surface = new Location(world, x, y, z);
            player.teleport(surface);

            // 消耗一个珍珠
            if (player.getInventory().getItemInMainHand().equals(item)) {
                player.swingMainHand();
                player.getInventory().setItemInMainHand(null);
            } else if (player.getInventory().getItemInOffHand().equals(item)) {
                player.swingOffHand();
                player.getInventory().setItemInOffHand(null);
            }
            //player.getInventory().remove(item);

            //给予短暂的黑暗效果
            giveDarknessEffect(player);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }
    }

    public void giveDarknessEffect(Player player) {
        // 初始黑暗效果
        //player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20 * 3, 4));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 , 4));

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
        }.runTaskTimer(plugin, 0L, 20L);
    }

}

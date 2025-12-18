package com.shaolian.uhc.items;

import com.sun.tools.javac.Main;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.event.Listener;

import java.util.*;
import org.bukkit.plugin.java.JavaPlugin;

public class SpecialDiamondSword implements Listener {

    private JavaPlugin plugin;
    public SpecialDiamondSword(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // 注册合成配方
//        registerRecipes14();
//        registerRecipes15();
//        registerRecipes16();
    }

    private static final long COOLDOWN = 9000; // 9秒冷却时间
    private final Map<UUID, Long> cooldowns = new HashMap<>();



    public ItemStack createSpecialDiamondSword1() {

        ItemStack sword = CustomStack.getInstance("test:midas_sword").getItemStack();;
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName("§6亚瑟王之剑");
        meta.setLore(Arrays.asList(
                "§7这是一种传说中的武器，每次击杀都会变得更强",
                "§e每击杀一个玩家会提升一级锋利",
                "§a当前锋利等级: 1"
        ));
        meta.setCustomModelData(1);
        sword.setItemMeta(meta);
        return sword;
    }

    private void registerRecipes14() {
        // 获取亚瑟王之剑
        ItemStack specialDiamondSword1 = createSpecialDiamondSword1();

        // 创建合成配方
        NamespacedKey key = new NamespacedKey(plugin, "specialDiamondSword1"); // 使用唯一标识符
        ShapedRecipe recipe = new ShapedRecipe(key, specialDiamondSword1)
                .shape(" E ", "QDQ", " G ")  // 合成形状
                .setIngredient('E', Material.ENCHANTED_GOLDEN_APPLE)
                .setIngredient('Q', Material.QUARTZ)
                .setIngredient('D', Material.DIAMOND_SWORD)
                .setIngredient('G', Material.GOLDEN_APPLE);
        // 注册配方
        plugin.getServer().addRecipe(recipe);
    }

    public static ItemStack upgradeSword(ItemStack sword) {
        ItemMeta meta = sword.getItemMeta();
        List<String> lore = meta.getLore();

        // 获取当前锋利等级
        int currentLevel = 1;
        if (lore != null && lore.size() > 2) {
            String levelLine = lore.get(2);
            //获取第10个字符而不包括第11个字符，并转换成int类型的整数
            currentLevel = Integer.parseInt(levelLine.substring(10, 11));
        }

        // 更新锋利等级
        currentLevel++;
        lore.set(2, "§a当前锋利等级: " + currentLevel);
        meta.setLore(lore);
        sword.setItemMeta(meta);

        // 添加锋利附魔
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, currentLevel);
        return sword;
    }

    @EventHandler
    public void onPlayerKill(EntityDeathEvent event) {
        // 检查死亡实体是否是玩家
        if (event.getEntity() instanceof Player) {
            Player killedPlayer = (Player) event.getEntity();

            // 检查是否有击杀者
            if (killedPlayer.getKiller() != null) {
                Player killer = killedPlayer.getKiller();
                ItemStack sword = killer.getInventory().getItemInMainHand();

                // 检查是否是特殊钻石剑
                if (isSpecialDiamondSword(sword)) {
                    // 升级剑
                    upgradeSword(sword);
                    killer.sendMessage("§a你的亚瑟王之剑变得更加强大了！");
                }
            }
        }
    }

    private boolean isSpecialDiamondSword(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_SWORD) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 1;
    }



    public ItemStack createSpecialDiamondSword2() {
        ItemStack sword = CustomStack.getInstance("test:dragon_katana").getItemStack();;
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName("§6龙武士刀");
        meta.setLore(Arrays.asList(
                "§7这是一种传说中的武器，利用了巨龙的敏捷性",
                "§e右击可向你的方向传送",
                "§a冷却时间: 9s"
        ));
        meta.setCustomModelData(2);
        sword.setItemMeta(meta);
        return sword;
    }

    private void registerRecipes15() {
        // 获取龙武士刀
        ItemStack specialDiamondSword2 = createSpecialDiamondSword2();
        NamespacedKey key = new NamespacedKey(plugin, "specialDiamondSword2");
        ShapedRecipe recipe = new ShapedRecipe(key, specialDiamondSword2)
                .shape(" E ", "DSD", " C ")
                .setIngredient('E', Material.ENDER_PEARL)
                .setIngredient('D', Material.DIAMOND)
                .setIngredient('C', Material.END_CRYSTAL)
                .setIngredient('S', Material.DIAMOND_SWORD);
        plugin.getServer().addRecipe(recipe);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        // 检查是否手持绿宝石剑并蹲下右键
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = event.getItem();

            if (isDragonKatana(item)) {
                // 检查冷却时间
                if (isOnCooldown(player)) {
                    long remaining = (cooldowns.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000;
                    player.sendMessage("§c技能冷却中，剩余时间: " + remaining + "秒");
                    return;
                }

                // 执行瞬移
                teleportPlayer(player);

                // 设置冷却时间
                cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + COOLDOWN);
            }
            if (item != null && item.getType() == Material.DIAMOND_SWORD) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 3) {
                    // 检查玩家是否在潜行
                    if (event.getPlayer().isSneaking()) {
                        // 播放动画效果
                        player.swingMainHand(); // 使用 swingMainHand 代替 playAnimation
                        openEmeraldUpgradeGUI(event.getPlayer(), item);
                    }
                }
            }
        }
    }

    private boolean isDragonKatana(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_SWORD) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 2;
    }

    private boolean isOnCooldown(Player player) {
        return cooldowns.containsKey(player.getUniqueId()) &&
                cooldowns.get(player.getUniqueId()) > System.currentTimeMillis();
    }

    private void teleportPlayer(Player player) {
        // 获取玩家朝向
        Location location = player.getLocation();
        Vector direction = location.getDirection().normalize().multiply(8); // 8格距离

        // 计算目标位置
        Location target = location.add(direction);
        target.setY(target.getY() + 1); // 稍微抬高一点防止卡进方块

        // 播放动画和音效
        player.swingMainHand();
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        // 粒子效果
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);

        // 平滑瞬移
        player.teleport(target);

        // 瞬移后效果
        player.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.FLAME, target, 20, 0.5, 0.5, 0.5, 0.1);
    }

    public void playCreepySound(Player player) {
        // 创建一个音效列表
        List<Sound> creepySounds = Arrays.asList(
                Sound.AMBIENT_CAVE,
                Sound.AMBIENT_BASALT_DELTAS_ADDITIONS,
                Sound.AMBIENT_CRIMSON_FOREST_ADDITIONS,
                Sound.AMBIENT_NETHER_WASTES_ADDITIONS,
                Sound.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS,
                Sound.BLOCK_ANCIENT_DEBRIS_BREAK,
                Sound.ENTITY_ENDERMAN_AMBIENT,
                Sound.ENTITY_GHAST_AMBIENT,
                Sound.ENTITY_WITHER_AMBIENT,
                Sound.MUSIC_NETHER_SOUL_SAND_VALLEY
        );

        // 随机选择一个音效
        Sound selectedSound = creepySounds.get(new Random().nextInt(creepySounds.size()));

        // 播放音效
        player.playSound(player.getLocation(), selectedSound, 1.0f, 0.5f);

        // 可选：为其他附近的玩家也播放这个音效
        for (Player nearbyPlayer : player.getWorld().getPlayers()) {
            if (nearbyPlayer != player && nearbyPlayer.getLocation().distance(player.getLocation()) <= 30) {
                nearbyPlayer.playSound(player.getLocation(), selectedSound, 0.5f, 0.5f);
            }
        }
    }

    public ItemStack createSpecialDiamondSword3() {
        ItemStack sword = CustomStack.getInstance("test:emerald_sword").getItemStack();;
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName("§6绿宝石剑");
        meta.setLore(Arrays.asList(
                "§7这是一种传说中的武器，利用了巨龙的敏捷性",
                "§e手持绿宝石剑下蹲放入足够数量的绿宝石触发升级",
                "§a16个绿宝石可以提升一级锋利"
        ));
        meta.setCustomModelData(3);
        sword.setItemMeta(meta);
        return sword;
    }

    private void registerRecipes16() {
        ItemStack emeraldSword = createSpecialDiamondSword3();
        // 创建合成配方
        NamespacedKey key = new NamespacedKey(plugin, "emeraldSword"); // 使用唯一标识符
        ShapedRecipe recipe = new ShapedRecipe(key, emeraldSword)
                .shape(" G ", "EDE", " B ")  // 修改形状
                .setIngredient('E',Material.EMERALD_BLOCK)
                .setIngredient('G', Material.GOLDEN_CARROT)
                .setIngredient('B', Material.BELL)
                .setIngredient('D', Material.DIAMOND_SWORD);
        // 注册配方
        plugin.getServer().addRecipe(recipe);
    }

    private void openEmeraldUpgradeGUI(Player player, ItemStack sword) {
        Inventory gui = Bukkit.createInventory(null, 27, "§6绿宝石剑升级");
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals("§6绿宝石剑升级")) {
            Player player = (Player) event.getPlayer();
            ItemStack sword = player.getInventory().getItemInMainHand();

            if (sword != null && sword.getType() == Material.DIAMOND_SWORD) {
                ItemMeta meta = sword.getItemMeta();
                if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 3) {
                    int emeraldCount = 0;
                    Inventory inventory = event.getInventory();

                    // 计算绿宝石数量并处理其他物品
                    for (ItemStack item : inventory.getContents()) {
                        if (item != null) {
                            if (item.getType() == Material.EMERALD) {
                                emeraldCount += item.getAmount();
                            } else {
                                // 返回非绿宝石物品
                                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                                if (!leftover.isEmpty()) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                                }
                            }
                        }
                    }

                    // 计算并升级锋利等级
                    if (emeraldCount > 0) {
                        int currentSharpness = meta.getEnchantLevel(Enchantment.SHARPNESS);
                        int levelsToAdd = emeraldCount / 16;
                        int remainingEmeralds = emeraldCount % 16;

                        // 更新锋利等级
                        meta.addEnchant(Enchantment.SHARPNESS, currentSharpness + levelsToAdd, true);
                        sword.setItemMeta(meta);

                        // 返回剩余的绿宝石
                        if (remainingEmeralds > 0) {
                            ItemStack remaining = new ItemStack(Material.EMERALD, remainingEmeralds);
                            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(remaining);
                            if (!leftover.isEmpty()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), remaining);
                            }
                        }

                        player.sendMessage("§a成功升级！当前锋利等级: " + (currentSharpness + levelsToAdd));
                    }
                }
            }
        }
    }




}
package com.shaolian.uhc;

import com.shaolian.uhc.items.ControlledRavagerManager;
import com.shaolian.uhc.items.SnifferItem;
import com.shaolian.uhc.items.Tim;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.attribute.Attribute;




import java.util.*;

public class GameListener implements Listener {
    private Main plugin;
    private PlayerData playerData;


    private SnifferItem snifferItem;
    private Tim tim;
    private ControlledRavagerManager controlledRavagerManager;

    private GameManager gameManager;
    private final double SCALE_FACTOR = 2.0; // 新的缩放因子
    private final Map<UUID, Set<Material>> eatenFoods = new HashMap<>();
    public static Map<UUID, Integer> playerKills = new HashMap<>();

    public GameListener(GameManager gameManager, Main plugin, PlayerData playerData) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.playerData = playerData;


//        snifferItem = new SnifferItem(plugin);
//        tim = new Tim(plugin);
//        controlledRavagerManager = new ControlledRavagerManager(plugin);

    }


    //玩家进入
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        Player player = event.getPlayer();
        gameManager.playerJoin(playerId);
        playerKills.putIfAbsent(playerId, 0);

        //test
//        player.getInventory().addItem(snifferItem.createSnifferItem());
//        player.getInventory().addItem(tim.createTimSummoner());
//        player.getInventory().addItem(controlledRavagerManager.createRavagerSummonerItem());
    }

    //玩家退出
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        gameManager.playerQuit(playerId);
    }

    //维度传送
    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            Location from = event.getFrom();
            World fromWorld = from.getWorld();
            World toWorld;

            if (fromWorld.getEnvironment() == World.Environment.NORMAL) {
                // 从主世界到下界
                toWorld = event.getTo().getWorld();
                if (toWorld.getEnvironment() != World.Environment.NETHER) return;

                double newX = from.getX() / SCALE_FACTOR;
                double newZ = from.getZ() / SCALE_FACTOR;

                Location newLocation = new Location(toWorld, newX, from.getY(), newZ, from.getYaw(), from.getPitch());
                event.setTo(newLocation);
            } else if (fromWorld.getEnvironment() == World.Environment.NETHER) {
                // 从下界到主世界
                toWorld = event.getTo().getWorld();
                if (toWorld.getEnvironment() != World.Environment.NORMAL) return;

                double newX = from.getX() * SCALE_FACTOR;
                double newZ = from.getZ() * SCALE_FACTOR;

                Location newLocation = new Location(toWorld, newX, from.getY(), newZ, from.getYaw(), from.getPitch());
                event.setTo(newLocation);
            }
        }
    }

    //树叶破坏
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        World world = event.getBlock().getWorld();
        if (world.getName().equals("FinalWorld")) {
            // 如果方块不是玩家放置的，不允许破坏
            if (!event.getBlock().hasMetadata("player-placed")) {
                event.setCancelled(true);
            }
        }

        Material blockType = event.getBlock().getType();
        // 检查是否是沙砾
        if (blockType == Material.GRAVEL) {
            Random random = new Random();
            if (random.nextDouble() < 0.20) {
                event.setDropItems(true); // 强制掉落物品
            }
        }
        // 检查是否是树叶
        if (isLeaf(blockType)) {
            Random random = new Random();
            // 10% 概率掉落苹果
            if (random.nextDouble() < 0.10) {
                event.getBlock().getWorld().dropItemNaturally(
                        event.getBlock().getLocation(),
                        new ItemStack(Material.APPLE)
                );
            }
        }
    }


    // 辅助方法：判断是否是树叶
    private boolean isLeaf(Material material) {
        return material.name().endsWith("_LEAVES");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        String killerName = player.getKiller() != null ? player.getKiller().getName() : null;
        UUID killerId = player.getKiller() != null ? player.getKiller().getUniqueId() : null;


        //掉落头颅
        createDeathMarker2(player);

        // 将死亡玩家设置为旁观者模式
        player.setGameMode(GameMode.SPECTATOR);

        // 从readyPlayers中移除玩家
        GameManager.readyPlayers.remove(playerId);
        if (GameManager.playerTeams.containsKey(playerId)) {
            GameManager.playerTeams.remove(playerId);
        }

        // 更新死亡玩家的死亡数
        playerData.updatePlayerStats(playerName, 0, 1);

        // 更新击杀者的击杀数
        if (killerName != null) {
            playerData.updatePlayerStats(killerName, 1, 0);
        }

        //统计本局游戏的击杀数
        if (killerId != null) {
            // 初始化击杀者的击杀数（如果还没有）
            playerKills.putIfAbsent(killerId, 0);
            // 增加击杀数
            playerKills.put(killerId, playerKills.get(killerId) + 1);
        }


        // 检查游戏是否结束
        gameManager.checkGameEnd1();
    }

    public void createDeathMarker(Player player) {
        Location deathLocation = player.getLocation();

        // 确保死亡位置是空气（防止覆盖其他方块）
        Block fenceBlock = deathLocation.getBlock();
        if (fenceBlock.getType() != Material.AIR) {
            deathLocation.setY(deathLocation.getWorld().getHighestBlockYAt(deathLocation) + 1);
            fenceBlock = deathLocation.getBlock();
        }

        // 放置木栅栏
        fenceBlock.setType(Material.OAK_FENCE);

        // 在木栅栏上方放置玩家头颅
        Block skullBlock = fenceBlock.getRelative(0, 1, 0);
        skullBlock.setType(Material.PLAYER_HEAD);

        // 设置头颅的朝向和玩家信息
        if (skullBlock.getState() instanceof Skull) {
            Skull skull = (Skull) skullBlock.getState();
            skull.setRotation(getClosestBlockFace(player.getLocation().getYaw()));
            skull.setOwningPlayer(player);
            skull.update();
        }

        // 记录日志
        plugin.getLogger().info(player.getName() + " 死亡，在 " +
                formatLocation(deathLocation) + " 创建了死亡标记。");
    }

    public void createDeathMarker2(Player player) {
        Location deathLocation = player.getLocation();

        // 创建玩家头颅物品
        ItemStack skullItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skullItem.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullItem.setItemMeta(skullMeta);
        }

        // 在死亡位置掉落头颅
        player.getWorld().dropItemNaturally(deathLocation, skullItem);

        // 记录日志
        plugin.getLogger().info(player.getName() + " 死亡，在 " +
                formatLocation(deathLocation) + " 掉落了头颅。");
    }


    private BlockFace getClosestBlockFace(float yaw) {
        // 将yaw转换为0-360度范围
        yaw = (yaw % 360 + 360) % 360;
        // 根据yaw返回最接近的BlockFace
        if (yaw < 45 || yaw >= 315) return BlockFace.SOUTH;
        if (yaw < 135) return BlockFace.WEST;
        if (yaw < 225) return BlockFace.NORTH;
        return BlockFace.EAST;
    }

    private String formatLocation(Location loc) {
        return String.format("(世界: %s, X: %d, Y: %d, Z: %d)",
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ());
    }


    //吃东西事件
    @EventHandler
    public void onPlayerConsumeItem(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 检查是否可以被咀嚼
        if (item.getType().isEdible()) {
            event.setCancelled(false); // 强制允许吃掉食物
            onPlayerConsumeFood(player, item.getType());
        }
    }

    public void onPlayerConsumeFood(Player player, Material foodType) {
        UUID playerId = player.getUniqueId();

        // 初始化玩家的食物记录
        //putIfAbsent如果不存在就添加
        //这是 Map 接口的一个方法，用于在指定的键不存在时，将键值对插入到 Map 中
        //如果键已经存在，则不会覆盖原有的值，而是直接返回原有的值。
        eatenFoods.putIfAbsent(playerId, new HashSet<>());

        // 如果玩家第一次吃这种食物
        if (!eatenFoods.get(playerId).contains(foodType)) {
            // 播放治疗粒子效果
            player.getWorld().spawnParticle(
                    Particle.HEART, // 粒子类型
                    player.getLocation().add(0, 1, 0), // 粒子位置（玩家头顶）
                    10, // 粒子数量
                    0.5, 0.5, 0.5, // 粒子偏移
                    0.1 // 粒子速度
            );

            // 添加生命恢复效果
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0)); // 3秒生命恢复1

            // 记录这种食物已经被吃过
            eatenFoods.get(playerId).add(foodType);

            // 提示玩家
            player.sendMessage(ChatColor.GREEN + "你第一次食用了这种物品，获得了生命恢复效果！");
        } else {

        }
    }



    //防止旁观者跑图
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) {
            World world = player.getWorld();
            WorldBorder border = world.getWorldBorder();
            Location playerLoc = player.getLocation();

            if (!isWithinBorder(playerLoc, border)) {
                Location safeLocation = getSafeLocation(playerLoc, border);
                player.teleport(safeLocation);
                player.sendMessage("§c你不能超出世界边界！");
            }
        }
    }

    private boolean isWithinBorder(Location location, WorldBorder border) {
        double size = border.getSize() / 2.0;
        double x = location.getX() - border.getCenter().getX();
        double z = location.getZ() - border.getCenter().getZ();
        return Math.abs(x) <= size && Math.abs(z) <= size;
    }

    private Location getSafeLocation(Location playerLoc, WorldBorder border) {
        double x = playerLoc.getX();
        double z = playerLoc.getZ();
        double y = playerLoc.getY();

        double borderSize = border.getSize() / 2.0;
        double centerX = border.getCenter().getX();
        double centerZ = border.getCenter().getZ();

        x = Math.max(centerX - borderSize + 1, Math.min(centerX + borderSize - 1, x));
        z = Math.max(centerZ - borderSize + 1, Math.min(centerZ + borderSize - 1, z));

        return new Location(playerLoc.getWorld(), x, y, z, playerLoc.getYaw(), playerLoc.getPitch());
    }


    // 添加世界切换监听器
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().getName().equals("lobby") ||
                player.getWorld().getName().equals("game")) {
            player.setGameMode(GameMode.ADVENTURE);
        }
    }
    

//    //限制8个蜘蛛网
//    @EventHandler
//    public void onInventoryClick(InventoryClickEvent event) {
//        checkPlayerInventory((Player) event.getWhoClicked());
//    }
//
//    @EventHandler
//    public void onInventoryOpen(InventoryOpenEvent event) {
//        checkPlayerInventory((Player) event.getPlayer());
//    }
//
//    @EventHandler
//    public void onInventoryClose(InventoryCloseEvent event) {
//        checkPlayerInventory((Player) event.getPlayer());
//    }
//
//    @EventHandler
//    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
//        Player player = event.getPlayer();
//        ItemStack item = event.getItem().getItemStack();
//
//        // 检查玩家捡起的物品是否是蜘蛛网
//        if (item.getType() == Material.COBWEB) {
//            plugin.getLogger().info("玩家要捡的物品是蜘蛛网");
//            int cobwebCount = 0;
//            // 统计玩家背包中蜘蛛网的数量
//            for (ItemStack inventoryItem : player.getInventory().getContents()) {
//                if (inventoryItem != null && inventoryItem.getType() == Material.COBWEB) {
//                    cobwebCount += inventoryItem.getAmount();
//                }
//            }
//
//            // 如果蜘蛛网数量超过8个
//            if (cobwebCount > 8) {
//                int toRemove = cobwebCount - 8;
//                // 移除多余的蜘蛛网
//                for (ItemStack inventoryItem : player.getInventory().getContents()) {
//                    if (inventoryItem != null && inventoryItem.getType() == Material.COBWEB) {
//                        int removeAmount = Math.min(toRemove, inventoryItem.getAmount());
//                        inventoryItem.setAmount(inventoryItem.getAmount() - removeAmount);
//                        toRemove -= removeAmount;
//                        if (toRemove <= 0) break;
//                    }
//                }
//                player.sendMessage(ChatColor.RED + "你最多只能携带8个蜘蛛网！");
//            }
//        }
//    }
//
//
//    private void checkPlayerInventory(Player player) {
//        // 这里可以添加对玩家背包的检查逻辑
//        // 例如：检查玩家背包中是否有违禁物品，或者物品数量是否超过限制等
//        // 示例：检查玩家背包中是否有蜘蛛网
//        int cobwebCount = 0;
//        for (ItemStack item : player.getInventory().getContents()) {
//            if (item != null && item.getType() == Material.COBWEB) {
//                cobwebCount += item.getAmount();
//            }
//        }
//
//        // 如果蜘蛛网数量超过8个
//        if (cobwebCount > 8) {
//            int toRemove = cobwebCount - 8;
//            for (ItemStack item : player.getInventory().getContents()) {
//                if (item != null && item.getType() == Material.COBWEB) {
//                    int removeAmount = Math.min(toRemove, item.getAmount());
//                    item.setAmount(item.getAmount() - removeAmount);
//                    toRemove -= removeAmount;
//                    if (toRemove <= 0) break;
//                }
//            }
//            player.sendMessage(ChatColor.RED + "你最多只能携带8个蜘蛛网！");
//        }
//    }



    // 处理方块放置事件
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        World world = event.getBlock().getWorld();
        if (world.getName().equals("FinalWorld")) {
            // 给玩家放置的方块添加标记
            event.getBlock().setMetadata("player-placed",
                    new FixedMetadataValue(plugin, true));
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 获取玩家队伍
        String team = GameManager.playerTeams.get(playerId);

        // 根据队伍设置玩家名称颜色
        String playerName = player.getName();
        if (team != null) {
            switch (team) {
                case "pink":
                    event.setFormat(ChatColor.RED + player.getName() + ChatColor.WHITE + ": " + event.getMessage());
                    break;
                case "aqua":
                    event.setFormat(ChatColor.AQUA + player.getName() + ChatColor.WHITE + ": " + event.getMessage());
                    break;
                case "green":
                    event.setFormat(ChatColor.GREEN + player.getName() + ChatColor.WHITE + ": " + event.getMessage());
                    break;
                case "black":
                    event.setFormat(ChatColor.BLACK + player.getName() + ChatColor.WHITE + ": " + event.getMessage());
                    break;
                default:
                    event.setFormat(ChatColor.WHITE + player.getName() + ChatColor.WHITE + ": " + event.getMessage());
            }
        }
    }

    //监听丢弃物品事件
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        ItemMeta meta = item.getItemMeta();

        //如果游戏最终模式开启，返回
        if(GameManager.isGameFinalRunning){
            return;
        }

        // 检查物品是否为特殊物品
        if (meta != null && meta.hasDisplayName()) {
            String displayName = meta.getDisplayName();
            // 判断是否为特殊物品
            if (displayName.equals("§6为爱发电") ||
             displayName.equals("§6自定义头盔")
             ) {
                event.setCancelled(true); // 取消丢弃
            }
        }
    }


    @EventHandler
    public void onPlayerAchievement(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();

        // 检查是否达成成就"隔墙有眼"
        if (advancement.getKey().getKey().equals("story/follow_ender_eye")) {
            ItemStack enderEye = new ItemStack(Material.ENDER_EYE, 12);

            // 尝试将末影之眼添加到玩家背包
            HashMap<Integer, ItemStack> remainingItems = player.getInventory().addItem(enderEye);

            // 如果背包满了，掉落在玩家脚下
            if (!remainingItems.isEmpty()) {
                for (ItemStack item : remainingItems.values()) {
                    player.getWorld().dropItem(player.getLocation(), item);
                }
            }
        }
    }

    // 监听玩家点击背包事件
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        checkHelmet(player);
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null) return; // 添加 null 检查
        //如果游戏没开始，点击帽子无效
        if(!GameManager.isGameRunning) {
            if (event.getSlotType() == InventoryType.SlotType.ARMOR && event.getSlot() == 39) {
                event.setCancelled(true);
            }
        }

    }

    // 监听玩家拖动背包事件
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        checkHelmet(player);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(GameManager.isGameFinalRunning){
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;
        checkHelmet(player);
    }

    private final NamespacedKey armorKey = new NamespacedKey("your_plugin_name", "extra_armor");
    // 检查玩家头盔槽位的物品
    private void checkHelmet(Player player) {
        // 获取玩家头盔槽位的物品
        ItemStack helmet = player.getInventory().getHelmet();
        // 获取当前头盔带来的护甲值
        double currentHelmetArmor = player.getPersistentDataContainer().getOrDefault
                (armorKey, PersistentDataType.DOUBLE, 0.0);

        // 重置头盔带来的护甲值
        player.getAttribute(Attribute.ARMOR).setBaseValue(player.getAttribute(Attribute.ARMOR)
                .getBaseValue() - currentHelmetArmor);
        player.getPersistentDataContainer().set(armorKey, PersistentDataType.DOUBLE, 0.0);


        if (helmet == null || helmet.getType() == Material.AIR) {
            // 移除护甲值效果
            double currentArmor = player.getPersistentDataContainer().getOrDefault(armorKey, PersistentDataType.DOUBLE, 0.0);
            player.getPersistentDataContainer().set(armorKey, PersistentDataType.DOUBLE, Math.max(0.0, currentArmor - 3.0));
            player.getAttribute(Attribute.ARMOR).setBaseValue(Math.max(0.0, player.getAttribute(Attribute.ARMOR).getBaseValue() - 3.0));
        }

        else  {
            // 给玩家增加护甲值
            double currentArmor = player.getPersistentDataContainer().getOrDefault(armorKey, PersistentDataType.DOUBLE, 0.0);
            player.getPersistentDataContainer().set(armorKey, PersistentDataType.DOUBLE, currentArmor + 3.0);
            player.getAttribute(Attribute.ARMOR).setBaseValue(player.getAttribute(Attribute.ARMOR).getBaseValue() + 3.0);
        }
    }













}
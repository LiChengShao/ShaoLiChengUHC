package com.shaolian.uhc;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.Listener;

import java.io.File;
import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.IOException;

import org.bukkit.block.Chest;

public class ChestLevel implements Listener {

    private final Main plugin;

    private Random random = new Random();
    // 定义物品及其权重
    //Map<ItemStack, Integer> weightedItems = new HashMap<>();
    Map<ItemStack, Integer> commonItems = new HashMap<>();
    private final List<Location> chestLocations = new ArrayList<>(); // 存储箱子位置
    private final File chestLocationsFile;
    // 添加一个 Set 来存储启用了箱子放置的玩家
    public static final Set<UUID> enabledPlayers = new HashSet<>();


    public ChestLevel() {
        plugin = Main.getPlugin(Main.class);
        // 初始化抽奖池
        // 确保插件的数据文件夹存在
        if (!plugin.getDataFolder().exists()) {
            plugin.getLogger().info("不存在插件文件夹，正在创建......");
            plugin.getDataFolder().mkdirs();
        }
        chestLocationsFile = new File(plugin.getDataFolder(), "chest_locations.yml");
        plugin.getLogger().info("箱子位置文件路径: " + chestLocationsFile.getAbsolutePath());
        loadChestLocations(); // 加载保存的箱子位置
        initializeLootTables();
        resetChests();
    }


    // 添加箱子等级枚举
    public enum Level {
        COMMON,    // 普通
        RARE,      // 珍贵
        DESERT,    // 沙漠
        JUNGLE,    // 丛林
        SNOW,      // 雪山
        LAVA       // 岩浆
    }


    // 添加带权重的箱子管理器
    private final Map<Level, Map<ItemStack, Integer>> weightedLootTables = new EnumMap<>(Level.class);

    // 初始化抽奖池
    private void initializeLootTables() {
        // 普通箱子物品及其权重
        commonItems.put(new ItemStack(Material.ROTTEN_FLESH, 1), 15); // 腐肉
        commonItems.put(new ItemStack(Material.SAND, 1), 5); // 沙子
        commonItems.put(new ItemStack(Material.IRON_INGOT, 1), 8); // 铁锭
        commonItems.put(new ItemStack(Material.FIRE_CHARGE, 1), 3); // 火焰弹
        commonItems.put(new ItemStack(Material.BREAD, 1), 5); // 面包
        commonItems.put(new ItemStack(Material.APPLE, 1), 5); // 苹果
        commonItems.put(new ItemStack(Material.BONE_MEAL, 1), 5); // 骨粉
        commonItems.put(new ItemStack(Material.ENDER_PEARL, 1), 6); // 末影珍珠
        commonItems.put(new ItemStack(Material.MUSIC_DISC_CAT, 1), 4); // 音乐唱片（猫）
        commonItems.put(new ItemStack(Material.SEAGRASS, 1), 5); // 海草
        commonItems.put(new ItemStack(Material.KELP, 1), 17); // 海带
        commonItems.put(new ItemStack(Material.BONE, 1), 3); // 骨头
        commonItems.put(new ItemStack(Material.REDSTONE, 1), 7); // 红石
        commonItems.put(new ItemStack(Material.DIAMOND, 1), 3); // 钻石
        commonItems.put(new ItemStack(Material.GOLD_INGOT, 1), 6); // 金子
        commonItems.put(new ItemStack(Material.GUNPOWDER, 1), 1); // 火药
        commonItems.put(new ItemStack(Material.GOLD_INGOT, 1), 3); // 金锭
        commonItems.put(new ItemStack(Material.STRING, 1), 5); // 线
        commonItems.put(new ItemStack(Material.LEATHER, 1), 15); // 皮革
        commonItems.put(new ItemStack(Material.STICK, 1), 1); // 木棍
        commonItems.put(new ItemStack(Material.FEATHER, 1), 5); // 羽毛
        commonItems.put(new ItemStack(Material.SUGAR_CANE, 1), 5); // 甘蔗
        commonItems.put(new ItemStack(Material.WHEAT, 1), 5); // 小麦
        commonItems.put(new ItemStack(Material.CARROT, 1), 5); // 胡萝卜
        commonItems.put(new ItemStack(Material.POTATO, 1), 5); // 马铃薯
        commonItems.put(new ItemStack(Material.QUARTZ, 1), 5); // 石英
        commonItems.put(new ItemStack(Material.LAPIS_LAZULI, 1), 5); // 青金石
        commonItems.put(new ItemStack(Material.BAMBOO, 1), 5); // 竹子
        commonItems.put(new ItemStack(Material.BOOK, 1), 15); // 书
        commonItems.put(new ItemStack(Material.FLINT, 1), 5); // 燧石
        commonItems.put(new ItemStack(Material.COBWEB, 4), 5); // 蜘蛛网
        commonItems.put(new ItemStack(Material.DIRT, 13), 32); // 泥土
        commonItems.put(new ItemStack(Material.WOODEN_SWORD, 1), 4); // 木剑
        commonItems.put(new ItemStack(Material.STONE_SWORD, 1), 2); // 石剑
        commonItems.put(new ItemStack(Material.WOODEN_PICKAXE, 1), 3); // 木镐
        commonItems.put(new ItemStack(Material.WOODEN_SHOVEL, 1), 5); // 木锹
        commonItems.put(new ItemStack(Material.WOODEN_AXE, 1), 4); // 木斧
        commonItems.put(new ItemStack(Material.ARROW, 2), 16); // 箭
        commonItems.put(new ItemStack(Material.BOW, 1), 5); // 弓
        commonItems.put(new ItemStack(Material.CROSSBOW, 1), 4); // 弩
        commonItems.put(new ItemStack(Material.FISHING_ROD, 1), 6); // 钓鱼竿
        commonItems.put(new ItemStack(Material.FLINT_AND_STEEL, 1), 6); // 打火石
        //commonItems.put(new ItemStack(Material.ENDER_EYE, 1), 6); // 末影之眼
        commonItems.put(new ItemStack(Material.BLAZE_ROD, 1), 6); // 烈焰棒
        commonItems.put(new ItemStack(Material.GOLDEN_BOOTS, 1), 2); // 金靴子
        commonItems.put(new ItemStack(Material.GOLDEN_LEGGINGS, 1), 2); // 金护腿
        commonItems.put(new ItemStack(Material.GOLDEN_CHESTPLATE, 1), 1); // 金胸甲
        commonItems.put(new ItemStack(Material.GOLDEN_HELMET, 1), 3); // 金头盔
        commonItems.put(new ItemStack(Material.IRON_BOOTS, 1), 1); // 铁靴子
        commonItems.put(new ItemStack(Material.IRON_HELMET, 1), 1); // 铁头盔
        commonItems.put(new ItemStack(Material.LEATHER_BOOTS, 1), 4); // 皮革靴子
        commonItems.put(new ItemStack(Material.LEATHER_LEGGINGS, 1), 5); // 皮革护腿
        commonItems.put(new ItemStack(Material.LEATHER_CHESTPLATE, 1), 1); // 皮革胸甲
        commonItems.put(new ItemStack(Material.LEATHER_HELMET, 1), 3); // 皮革头盔
        commonItems.put(new ItemStack(Material.CHAINMAIL_BOOTS, 1), 3); // 锁链靴子
        commonItems.put(new ItemStack(Material.CHAINMAIL_LEGGINGS, 1), 1); // 锁链护腿
        commonItems.put(new ItemStack(Material.CHAINMAIL_CHESTPLATE, 1), 1); // 锁链胸甲
        commonItems.put(new ItemStack(Material.CHAINMAIL_HELMET, 1), 3); // 锁链头盔
        commonItems.put(new ItemStack(Material.TURTLE_HELMET, 1), 6); // 海龟壳
        commonItems.put(new ItemStack(Material.WIND_CHARGE, 1), 10); // 风弹


        weightedLootTables.put(Level.COMMON, commonItems);


        // 珍贵箱子
        Map<ItemStack, Integer> rareItems = new HashMap<>();
        rareItems.put(new ItemStack(Material.DIAMOND, 1), 1);
        rareItems.put(new ItemStack(Material.GOLDEN_APPLE, 1), 9);
        // 添加更多物品及其权重...
        weightedLootTables.put(Level.RARE, rareItems);



    }

    // 获取随机物品
    public ItemStack getRandomItem(Level level) {

        // 获取对应等级的物品集合以及权重，用weightedItems接收
        Map<ItemStack, Integer> weightedItems = weightedLootTables.get(level);
        if (weightedItems == null || weightedItems.isEmpty()) return null;

        // 计算总权重
        //weightedItems 是一个 Map<ItemStack, Integer>，其中：
        //键（Key）：ItemStack，表示物品。
        //值（Value）：Integer，表示该物品的权重。
        int totalWeight = weightedItems.values().stream().mapToInt(Integer::intValue).sum();

        // 生成随机数
        int randomNumber = new Random().nextInt(totalWeight);

        // 根据权重选择物品
        int currentWeight = 0;
        for (Map.Entry<ItemStack, Integer> entry : weightedItems.entrySet()) {
            currentWeight += entry.getValue();
            if (randomNumber < currentWeight) {
                return entry.getKey();
            }
        }

        return null;
    }

    // 填充箱子,这个方法在输入指令后被执行
    public void fillChest(Chest chest, Level level) {
        // 清空箱子
        chest.getBlockInventory().clear();

        // 随机填充物品
        Random random = new Random();
        int itemCount = 6 + random.nextInt(8); // 每个箱子7-13件物品
        for (int i = 0; i < itemCount; i++) {
            //使用getRandomItem(level)获得随机物品
            ItemStack item = getRandomItem(level);
            if (item != null) {
                //在随机位置放ite,数量为itemCount
                chest.getBlockInventory().setItem(random.nextInt(27), item);
            }
        }
    }

    // 在插件启动时重置箱子
    public void resetChests() {
        for (Location location : chestLocations) {
            if (location.getBlock().getState() instanceof Chest) {
                Chest chest = (Chest) location.getBlock().getState();
                Inventory chestInventory = chest.getBlockInventory();
                chestInventory.clear(); // 清空箱子
                // 添加自定义物品到箱子
                //chestInventory.addItem(new ItemStack(Material.DIAMOND, 1)); // 示例：添加钻石

                // 随机填充物品
                Random random = new Random();
                int itemCount = 3 + random.nextInt(5); // 每个箱子3-7件物品
                for (int i = 0; i < itemCount; i++) {
                    // 使用默认的 COMMON 等级获取随机物品
                    ItemStack item = getRandomItem(Level.COMMON);
                    if (item != null) {
                        //在随机位置放item,数量为itemCount
                        chest.getBlockInventory().setItem(random.nextInt(27), item);
                    }
                }

            }
        }
    }

    //saveChestLocations 方法只在 onDisable 中调用，而 onDisable 是在服务器关闭时触发的。
    // 如果你没有正常关闭服务器（例如直接关闭控制台），saveChestLocations 方法可能没有执行。
    //解决方法： 在 onBlockPlace 事件中立即保存箱子位置：
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // 检查玩家是否在 enabledPlayers 集合中
        if (!enabledPlayers.contains(event.getPlayer().getUniqueId())) {
            return; // 如果玩家未启用箱子放置，直接返回
        }
        // 检查放置的方块是否是箱子
        if (event.getBlock().getType() == Material.CHEST) {
            // 获取箱子的位置
            Location chestLocation = event.getBlock().getLocation();
            // 将箱子位置添加到列表中
            addChestLocation(chestLocation);
            // 立即保存箱子位置
            saveChestLocations();
        }
    }



    // 添加箱子位置到列表
    public void addChestLocation(Location location) {
        if (location != null && location.getBlock().getState() instanceof Chest) {
            chestLocations.add(location);
        }
    }

    // 保存箱子位置到文件
    public void saveChestLocations() {
        YamlConfiguration config = new YamlConfiguration();
        for (int i = 0; i < chestLocations.size(); i++) {
            config.set("chests." + i, chestLocations.get(i));
        }
        try {
            // 确保文件夹存在
            if (!chestLocationsFile.getParentFile().exists()) {
                chestLocationsFile.getParentFile().mkdirs();
            }
            config.save(chestLocationsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 从文件加载箱子位置
    public void loadChestLocations() {
        if (!chestLocationsFile.exists()) {
            plugin.getLogger().info("chest_locations.yml 文件不存在，跳过加载。");
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(chestLocationsFile);
        if (config.getConfigurationSection("chests") == null) {
            plugin.getLogger().info("chest_locations.yml 文件中没有箱子位置数据。");
            return;
        }
        for (String key : config.getConfigurationSection("chests").getKeys(false)) {
            chestLocations.add((Location) config.get("chests." + key));
            plugin.getLogger().info("已加载箱子位置: " + config.get("chests." + key));
        }
    }


    // 在服务器关闭时保存箱子位置
    public void onDisable() {
        //服务器关闭的时候执行这个方法
        saveChestLocations();
    }



}

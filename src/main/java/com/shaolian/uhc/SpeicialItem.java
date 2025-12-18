package com.shaolian.uhc;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.Listener;
import org.checkerframework.checker.units.qual.C;

import java.util.*;
import java.util.stream.Stream;

public class SpeicialItem implements Listener {

    private Main plugin;

    private static final ItemStack TRACK_COMPASS = createTrackCompass();

    private final NamespacedKey easyGoldAppleKey;
    private final NamespacedKey special_bone;
    private final NamespacedKey craftCountKey1;
    private final NamespacedKey craftCountKey2;
    private final Map<UUID, Integer> craftCounterApple = new HashMap<>();
    private final Map<UUID, Integer> craftCounterBone = new HashMap<>();
    private final int MAX_CRAFT_LIMIT1 = 3;
    private final int MAX_CRAFT_LIMIT2 = 1;
    private static final int MAX_BLOCKS = 10;
    private final Map<UUID, BukkitTask> trackingTasks = new HashMap<>();//追踪任务
    private final Map<UUID, ItemStack[]> playerStorageContents = new HashMap<>(); // 新增：存储每个玩家的收纳袋内容


    // 熔炉稿
    public static final ItemStack furnacePickaxe = SpeicialItem.createFurnacePickaxe();

    // 伐木斧
    public static final ItemStack treeAxe = SpeicialItem.createTreeAxe();


    //简易金苹果
    public static final ItemStack easyGoldApple = new ItemStack(Material.GOLDEN_APPLE);

    //金头
    public static final ItemStack goldHead = SpeicialItem.createGoldHead();

    //抗火药水
    public static final ItemStack fireResistancePotion = SpeicialItem.createFireResistancePotion();

    //铁砧
    public static final ItemStack anvil = new ItemStack(Material.ANVIL);

    //箭
    public static final ItemStack arrow = new ItemStack(Material.ARROW,16);

    //黑曜石
    public static final ItemStack obsidian = new ItemStack(Material.OBSIDIAN);

    //追踪指南针
    public static final ItemStack trackCompass = SpeicialItem.getTrackCompass();

    //附魔金苹果
    public static final ItemStack enchantGoldApple = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);

    //狼狗
    public static final ItemStack specialBone = SpeicialItem.createSpecialBone();

    //收纳袋
    public static final ItemStack storageBag = SpeicialItem.createStorageBag();

    //短剑
    public static final ItemStack shortSword = SpeicialItem.createShortSword();

    //火弩
    public static final ItemStack fireCrossBow = SpeicialItem.createFireCrossBow();

    //图腾
    public static final ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);


    public SpeicialItem(Main plugin) {
        this.plugin = plugin;
        this.easyGoldAppleKey = new NamespacedKey(plugin, "easy_golden_apple");
        this.special_bone = new NamespacedKey(plugin, "special_bone");
        this.craftCountKey1 = new NamespacedKey(plugin, "easy_golden_apple_craft_count");
        this.craftCountKey2 = new NamespacedKey(plugin, "special_bone_craft_count");
        plugin.getServer().getPluginManager().registerEvents(this, plugin); // 注册事件监听器
    }

    public static ItemStack createFurnacePickaxe() {
        ItemStack furnacePickaxe = CustomStack.getInstance("test:furnace_pickaxe").getItemStack();
        // 创建熔炉稿
        ItemMeta meta = furnacePickaxe.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d熔炉稿");
            meta.setLore(Arrays.asList("§7直接获得矿物成品", ""));
            // 添加效率 1 附魔
            meta.addEnchant(Enchantment.EFFICIENCY, 1, true);
            //添加耐久 1 附魔
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            //meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            furnacePickaxe.setItemMeta(meta);
        }
        return furnacePickaxe;
    }

    public static ItemStack createTreeAxe() {
        ItemStack treeAxe = CustomStack.getInstance("test:tree_axe").getItemStack();
        // 创建伐木斧
        ItemMeta meta = treeAxe.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d伐木斧");meta.setLore(Arrays.asList("§7可以连锁砍树", ""));
            // 隐藏附魔信息和其他可能的标志
            //meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            treeAxe.setItemMeta(meta);
        }
        return treeAxe;
    }

    public static ItemStack createGoldHead() {
        // 创建金头
        ItemStack goldHead = CustomStack.getInstance("test:gold_head").getItemStack();
        ItemMeta meta = goldHead.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d金头");
            meta.setLore(Arrays.asList("§7右键可直接食用", ""));
            goldHead.setItemMeta(meta);
        }
        return goldHead;
    }

    public static ItemStack createFireResistancePotion() {
        // 创建抗火药水
        ItemStack fireResistancePotion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) fireResistancePotion.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6抗火药水");
            meta.setLore(Arrays.asList("§7提供3分钟的抗火效果"));
            // 设置药水效果
            meta.addCustomEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 180 * 20, 0), true); // 3分钟抗火效果
            fireResistancePotion.setItemMeta(meta);
        }
    return fireResistancePotion;
    }

    public static ItemStack createTrackCompass() {
        // 创建追踪指南针
        ItemStack trackCompass = new ItemStack(Material.COMPASS);
        ItemMeta meta = trackCompass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d追踪指南针");
            meta.setLore(Arrays.asList("§7右键可追踪在线的任何玩家", "§8UHCCompass"));
            meta.setUnbreakable(true);
            trackCompass.setItemMeta(meta);
        }
        return trackCompass;
    }

    public static ItemStack getTrackCompass() {
        return TRACK_COMPASS;
    }

    public static ItemStack createSpecialBone() {
        // 创建特殊骨头
        ItemStack specialBone = CustomStack.getInstance("test:special_bone").getItemStack();
        ItemMeta meta = specialBone.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d布鲁斯骨头");
            meta.setLore(Arrays.asList("§7右键可召唤五只为你战斗的猎犬", ""));
            specialBone.setItemMeta(meta);
        }
        return specialBone;
    }

    public static ItemStack createStorageBag() {
        ItemStack storageBag = CustomStack.getInstance("test:storage_bag").getItemStack();
        // 创建特殊收纳袋
        //ItemStack storageBag = new ItemStack(Material.PAPER);
        ItemMeta meta = storageBag.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d收纳袋");
            meta.setLore(Arrays.asList("§7右键可打开收纳袋", ""));
            storageBag.setItemMeta(meta);
        }
        return storageBag;
    }

    public static ItemStack createShortSword() {
        ItemStack shortSword = CustomStack.getInstance("test:short_sword").getItemStack();
        ItemMeta meta = shortSword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d短剑");
            meta.addEnchant(Enchantment.SHARPNESS, 2, true);
            shortSword.setItemMeta(meta);
        }
        return shortSword;
    }

    public static ItemStack createFireCrossBow() {
        ItemStack fireCrossBow = CustomStack.getInstance("test:fire_crossbow").getItemStack();
        ItemMeta meta = fireCrossBow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d火弩");
            meta.addEnchant(Enchantment.QUICK_CHARGE, 1, true);
            meta.addEnchant(Enchantment.FLAME, 1, true);
            fireCrossBow.setItemMeta(meta);
        }
        return fireCrossBow;
    }


    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        // 检查是否是简易金苹果的配方
        if (event.getRecipe() instanceof ShapedRecipe) {
            ShapedRecipe recipe = (ShapedRecipe) event.getRecipe();
            if (recipe.getKey().equals(easyGoldAppleKey)) {
                if (event.getWhoClicked() instanceof Player) {
                    Player player = (Player) event.getWhoClicked();
                    UUID playerUUID = player.getUniqueId();

                    // 获取玩家已合成的数量
                    int craftCount = getCraftCountApple(playerUUID);

                    // 计算本次合成会生产多少个物品
                    int amountCrafted;
                    if (event.isShiftClick()) {
                        // 如果是Shift+点击，计算能生产多少个
                        amountCrafted = calculateShiftClickAmount(event);
                    } else {
                        amountCrafted = 1;
                    }

                    // 检查是否超过限制
                    if (craftCount + amountCrafted > MAX_CRAFT_LIMIT1) {
                        event.setCancelled(true);
                        player.sendMessage("§c你最多只能合成" + MAX_CRAFT_LIMIT1 + "个简易金苹果！");
                        return;
                    }

                    // 更新计数
                    setCraftCountApple(playerUUID, craftCount + amountCrafted);

                    // 通知玩家
                    player.sendMessage("§a你已合成" + (craftCount + amountCrafted) + "/" + MAX_CRAFT_LIMIT1 + "个简易金苹果");
                }
            }if (recipe.getKey().equals(special_bone)) {
                if (event.getWhoClicked() instanceof Player) {
                    Player player = (Player) event.getWhoClicked();
                    UUID playerUUID = player.getUniqueId();

                    // 获取玩家已合成的数量
                    int craftCount = getCraftCountBone(playerUUID);

                    // 计算本次合成会生产多少个物品
                    int amountCrafted;
                    if (event.isShiftClick()) {
                        // 如果是Shift+点击，计算能生产多少个
                        amountCrafted = calculateShiftClickAmount(event);
                    } else {
                        amountCrafted = 1;
                    }

                    // 检查是否超过限制
                    if (craftCount + amountCrafted > MAX_CRAFT_LIMIT2) {
                        event.setCancelled(true);
                        player.sendMessage("§c你最多只能合成" + MAX_CRAFT_LIMIT2 + "个布鲁斯骨头！");
                        return;
                    }

                    // 更新计数
                    setCraftCountBone(playerUUID, craftCount + amountCrafted);

                    // 通知玩家
                    player.sendMessage("§a你已合成" + (craftCount + amountCrafted) + "/" + MAX_CRAFT_LIMIT2 + "个布鲁斯骨头");
                }
            }
        }
    }


    // 计算Shift点击能生产多少物品
    private int calculateShiftClickAmount(CraftItemEvent event) {
        int amountPerCraft = event.getRecipe().getResult().getAmount();
        int maxStackSize = event.getRecipe().getResult().getType().getMaxStackSize();

        // 查找配方中数量最少的材料
        int minItems = Integer.MAX_VALUE;
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && item.getType() != Material.AIR) {
                minItems = Math.min(minItems, item.getAmount());
            }
        }

        // 计算最多能合成多少个
        return Math.min(minItems * amountPerCraft, maxStackSize);
    }

    private int getCraftCountApple(UUID playerUUID) {
        return craftCounterApple.getOrDefault(playerUUID, 0);
    }

    private void setCraftCountApple(UUID playerUUID, int count) {
        craftCounterApple.put(playerUUID, count);
    }

    private int getCraftCountBone(UUID playerUUID) {
        return craftCounterBone.getOrDefault(playerUUID, 0);
    }

    private void setCraftCountBone(UUID playerUUID, int count) {
        craftCounterBone.put(playerUUID, count);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if(item == null){
                return;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return;
            }
            if (item.getType() == Material.PLAYER_HEAD) {
                event.setCancelled(true);//取消事件
                //吃普通头
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 4 * 20, 2)); // 4s生命恢复
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10 * 20, 1)); // 10s速度
                // 播放打嗝音效
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
                // 消耗一个普通头
                item.setAmount(item.getAmount() - 1);
            }
            if (item.equals(goldHead)) {
                // 吃金头
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 8 * 20, 2)); // 8s生命恢复
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10 * 20, 1)); // 10s速度
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 120 * 20, 2)); // 120s吸收效果

                // 播放打嗝音效
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
                // 消耗一个金头
                item.setAmount(item.getAmount() - 1);
            }
            if(meta.hasLore() && meta.getLore().contains("§8UHCCompass")){
                openTrackingGUI(player);
               // event.setCancelled(true); // 取消事件，防止其他插件处理
            }
            if (item.isSimilar(specialBone)) {
                callWolves(player);
                item.setAmount(item.getAmount() - 1);
            }
            if (item.isSimilar(storageBag)) {
                openStorageGUI(player);
            }
            if (item.getType() == Material.ECHO_SHARD) {
                if (meta.hasCustomModelData() && meta.getCustomModelData() == 1002) {
                    // 播放动画效果
                    player.swingMainHand();
                    player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0f, 1.0f);
                    // 在玩家位置召唤铁傀儡
                    player.getWorld().spawn(player.getLocation(), IronGolem.class);
                    // 消耗一个回声碎片
                    item.setAmount(item.getAmount() - 1);
                }
            }
        }
    }

    private void callWolves(Player player) {
        Wolf[] wolves = new Wolf[5];
        // 召唤五只狼
        for (int i = 0; i < 5; i++) {
            Wolf wolf = (Wolf) player.getWorld().spawnEntity(player.getLocation(), EntityType.WOLF);
            wolf.setCustomName(player.getName() + "的狼");
            wolf.setCustomNameVisible(true);
            wolf.setOwner(player);
            // 狼不会攻击该玩家
            wolf.setTarget(null);
            wolves[i] = wolf;
        }
    }

    private void openStorageGUI(Player player) {
        // 获取或创建玩家的收纳袋内容
        ItemStack[] contents = playerStorageContents.computeIfAbsent(
                player.getUniqueId(),
                k -> new ItemStack[27] // 27格初始为空
        );

        // 创建收纳袋GUI
        Inventory inventory = Bukkit.createInventory(null, 27, "§b收纳袋 - " + player.getName());
        inventory.setContents(contents);
        player.openInventory(inventory);
    }

    // 新增：监听收纳袋关闭事件以保存内容
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().startsWith("§b收纳袋 - ")) {
            Player player = (Player) event.getPlayer();
            playerStorageContents.put(player.getUniqueId(), event.getInventory().getContents());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        ItemMeta meta = tool.getItemMeta();
        if(tool.getItemMeta() == null){
            return;
        }
        if (meta.hasLore() && meta.getLore().contains("§7直接获得矿物成品")) {
            // block是被破坏的方块
            Block block = event.getBlock();
            //blockType 是被破坏的方块的类型
            Material blockType = block.getType();
            // dropMaterial 是被破坏的方块的熔炼结果
            Material dropMaterial = getSmeltedMaterial(blockType);
            // 如果没有下面所有的矿物类型，就会返回null,那么dropMaterial == null，就不会取消原版掉落
            if (dropMaterial != null) {
                event.setDropItems(false); // 取消原版掉落
                if(dropMaterial == Material.REDSTONE || dropMaterial == Material.LAPIS_LAZULI) {
                    //如果是是红石，那么就会掉落5个红石
                    block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(dropMaterial, 6));
                }
                else if(dropMaterial == Material.GOLD_NUGGET || dropMaterial == Material.COPPER_INGOT){
                    //如果是金粒，那么就会掉落4个
                    block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(dropMaterial, 4));
                }else{
                    block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(dropMaterial));
                }
            }
        } else if (meta.hasLore() && meta.getLore().contains("§7可以连锁砍树")) {
            Block block = event.getBlock();
            if (isLog(block.getType())) {
                // 取消原版破坏事件
                event.setCancelled(true);
                // 执行连锁砍树逻辑
                breakTree(block, 0);
            }
        }
    }

    // 获取矿物的熔炼结果
    private Material getSmeltedMaterial(Material material) {
        switch (material) {
            //煤炭
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
                return Material.COAL;
            //铁
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
                return Material.IRON_INGOT;
            //铜
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
                return Material.COPPER_INGOT;
            //金
            case RAW_GOLD://未熔炼的金
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
                return Material.GOLD_INGOT;
            //红石
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
                return Material.REDSTONE;
            //绿宝石
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                return Material.EMERALD;
            //青金石
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                return Material.LAPIS_LAZULI;
            //钻石
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                return Material.DIAMOND;
            //下界金矿
            case NETHER_GOLD_ORE:
                return Material.GOLD_NUGGET;
            //下界石英
            case NETHER_QUARTZ_ORE:
                return Material.QUARTZ;
            //粗铁块
            case RAW_IRON_BLOCK:
                return Material.IRON_BLOCK;
            //粗铜块
            case RAW_COPPER_BLOCK:
                return Material.COPPER_BLOCK;
            //粗金块
            case RAW_GOLD_BLOCK:
                return Material.GOLD_BLOCK;
            //沙子
            case SAND:
                return Material.GLASS;
            //熔炉


            default:
                return null;
        }
    }


    private void breakTree(Block startBlock, int count) {
        if (count >= MAX_BLOCKS) return;

        // 破坏当前木头
        startBlock.breakNaturally();
        // 播放木头破坏声音
        startBlock.getWorld().playSound(startBlock.getLocation(), startBlock.getBlockData().
                getSoundGroup().getBreakSound(), 1.0f, 1.0f);

        // 破坏周围的树叶
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                Block leaf = startBlock.getRelative(x, 0, z);
                if (isLeaf(leaf.getType())) {
                    leaf.breakNaturally();
                    // 播放树叶破坏声音
                    leaf.getWorld().playSound(leaf.getLocation(), leaf.getBlockData().getSoundGroup().
                            getBreakSound(), 1.5f, 1.5f);
                    // 添加15%概率掉落苹果
                    Random random = new Random();
                    if (random.nextDouble() < 0.15) {
                        leaf.getWorld().dropItemNaturally(
                                leaf.getLocation(),
                                new ItemStack(Material.APPLE)
                        );
                    }

                }
            }
        }

        // 检查上方的方块
        Block above = startBlock.getRelative(0, 1, 0);
        if (isLog(above.getType())) {
            // 延迟执行，创建动画效果
            new BukkitRunnable() {
                @Override
                public void run() {
                    breakTree(above, count + 1);
                }
            }.runTaskLater(plugin, 5L); // 0.25秒延迟
        }
    }

    private boolean isLog(Material material) {
        return material.name().endsWith("_LOG") || material.name().endsWith("_WOOD");
    }

    private boolean isLeaf(Material material) {
        return material.name().endsWith("_LEAVES");
    }




    public void openTrackingGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§6追踪指南针");
        int slot = 0;

        // 遍历所有玩家
        for (UUID playerId : GameManager.readyPlayers) {
            // 跳过自己
            if (playerId.equals(player.getUniqueId())) {
                continue;
            }

            // 获取玩家对象
            Player targetPlayer = Bukkit.getPlayer(playerId);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                // 创建队友头颅
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                meta.setOwningPlayer(targetPlayer);

                head.setItemMeta(meta);
                gui.setItem(slot, head);
                slot++;
            }
        }
        player.openInventory(gui);
    }

    //玩家点击GUI的头颅
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§6追踪指南针")) {
            event.setCancelled(true);//取消原版的交互
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() == Material.PLAYER_HEAD) {
                Player player = (Player) event.getWhoClicked();
                ItemMeta itemMeta = clickedItem.getItemMeta();

                if (itemMeta instanceof SkullMeta) {
                    SkullMeta skullMeta = (SkullMeta) itemMeta;
                    if (skullMeta.getOwningPlayer() != null) {
                        Player target = Bukkit.getPlayer(skullMeta.getOwningPlayer().getUniqueId());
                        if (target != null && target.isOnline()) {
                            startTracking(player, target);
                            player.closeInventory();
                            player.sendMessage("§a开始追踪: " + target.getName());
                        }
                    }
                }
            }
        }
    }

    private void startTracking(Player player, Player target) {
        // 取消之前的追踪任务（如果存在）
        stopTracking(player);

        // 立即更新指南针
        updateCompass(player, target);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // 检查游戏状态和玩家状态
                if (!player.isOnline() || !target.isOnline() ||
                        (!GameManager.readyPlayers.contains(target.getUniqueId()))) {
                    player.sendMessage(ChatColor.RED + "追踪取消: 目标" + target.getName() + "离线");
                    this.cancel();
                    return;
                }

                updateCompass(player, target);
            }
        }.runTaskTimer(plugin, 0L, 20L);
        // 保存新的追踪任务
        trackingTasks.put(player.getUniqueId(), task);
    }

    private void stopTracking(Player player) {
        BukkitTask task = trackingTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private void updateCompass(Player player, Player target) {
        ItemStack compass = null;
        ItemStack[] allContents = Stream.of(
                player.getInventory().getContents(),
                player.getInventory().getExtraContents()
        ).flatMap(Arrays::stream).toArray(ItemStack[]::new);

        for (ItemStack item : allContents) {
            if (item != null && item.getItemMeta().hasLore() && item.getItemMeta().getLore().contains("§8UHCCompass")) {
                compass = item;
                break;
            }
        }

        if (compass != null) {
            CompassMeta meta = (CompassMeta) compass.getItemMeta();
            if (meta != null) {
                // 设置指南针指向目标位置
                Location targetLoc = target.getLocation();
                Location hunterLoc = player.getLocation();
                if (hunterLoc.getWorld().equals(targetLoc.getWorld())) {
                    meta.setLodestone(targetLoc);
                    meta.setLodestoneTracked(false);
                    // 计算并更新距离
                    int distance = (int) player.getLocation().distance(targetLoc);
                    meta.setDisplayName("§c追踪: " + target.getName() + " §7(距离: " +
                            distance + "米)");

                }else {
                    // 不同维度时显示提示
                    meta.setDisplayName("§c无法追踪: " + target.getName() + " §7(不同维度)");
                }


                // 确保更新Lore
//                List<String> lore = new ArrayList<>();
//                lore.add("§7左击 指南针可选择要追踪的玩家");
//                lore.add("§8Compass");
//                meta.setLore(lore);

                meta.setLore(Arrays.asList("§7右键可追踪在线的任何玩家", "§8UHCCompass"));

                compass.setItemMeta(meta);
            }
        }
    }


}

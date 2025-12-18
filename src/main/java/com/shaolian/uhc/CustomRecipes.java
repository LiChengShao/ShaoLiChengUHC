package com.shaolian.uhc;

import com.shaolian.uhc.items.SpecialDiamondSword;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.PotionMeta;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;
import java.util.logging.Level; // 需要这个 import

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CustomRecipes  implements Listener, CommandExecutor {
    private Main plugin;
    private SpecialDiamondSword specialDiamondSword;

    public CustomRecipes(Main plugin) {
        //不写会导致 NullPointerException，因为 plugin 变量没有被初始化
        //这行代码的作用是将当前插件实例（this）赋值给类的成员变量 plugin+
        //在Java中，this关键字代表当前类的实例
        this.plugin = plugin;
        specialDiamondSword = new SpecialDiamondSword(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("recipe").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            openRecipeGUI(player);
            return true;
        }
        return false;
    }

    public ItemStack createRecipeBook() {
        ItemStack recipeBook = new ItemStack(Material.BOOK);
        ItemMeta meta = recipeBook.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6特殊配方书");
            meta.setLore(Arrays.asList("§7右键查看特殊配方"));
            meta.setUnbreakable(true);
            meta.setCustomModelData(1002);
            // 添加虚拟附魔效果
            meta.addEnchant(Enchantment.EFFICIENCY, 1, true);
            // 隐藏附魔信息
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            recipeBook.setItemMeta(meta);
        }
        return recipeBook;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        if (item != null && item.getType() == Material.BOOK) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 1002) {
                event.setCancelled(true);
                //与书交互会执行openRecipeGUI
                openRecipeGUI(player);
                player.sendMessage("§a§l输入/recipe查看合成配方");
            }
        }
    }



    public void openRecipeGUI666(Player player) {
        FileConfiguration recipeCfg = plugin.getRecipeConfig();

        String guiTitle = ChatColor.translateAlternateColorCodes('&', recipeCfg.getString("gui.title", "&6特殊配方"));
        int guiSize = recipeCfg.getInt("gui.size", 36);

        if (guiSize <= 0 || guiSize % 9 != 0) {
            plugin.getLogger().warning("recipe.yml 中的 GUI 大小 (" + guiSize + ") 无效。已重置为36。");
            guiSize = 36;
        }

        Inventory gui = plugin.getServer().createInventory(null, guiSize, guiTitle);
        ConfigurationSection itemsSection = recipeCfg.getConfigurationSection("items");

        if (itemsSection == null) {
            player.sendMessage(ChatColor.RED + "配方物品未配置，请检查 recipe.yml 文件。");
            plugin.getLogger().warning("recipe.yml 中缺少 'items' 部分。");
            return;
        }

        for (String itemKey : itemsSection.getKeys(false)) { // itemKey 是 YAML 中的键名，例如 "furnace_pickaxe_from_config"
            ConfigurationSection itemConfig = itemsSection.getConfigurationSection(itemKey);

            if (itemConfig == null || !itemConfig.getBoolean("enabled", true)) {
                continue;
            }

            int slot = itemConfig.getInt("slot", -1);
            if (slot < 0 || slot >= guiSize) {
                plugin.getLogger().warning("recipe.yml 中物品 '" + itemKey + "' 的 slot (" + slot + ") 无效。已跳过。");
                continue;
            }

            ItemStack displayItem = null;
            String itemsAdderId = itemConfig.getString("itemsadder_id"); // 例如 "test:furnace_pickaxe"

            // 1. 尝试从 ItemsAdder 获取物品 (如果提供了 itemsadder_id)
            if (itemsAdderId != null && !itemsAdderId.isEmpty()) {
                if (Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) { // 检查 ItemsAdder 是否启用
                    try {
                        // 直接使用 ItemsAdder API 获取物品
                        // 你需要将 ItemsAdder.jar 添加到你的项目依赖中 (scope: provided)
                        displayItem = dev.lone.itemsadder.api.CustomStack.getInstance(itemsAdderId).getItemStack();

                        if (displayItem == null) {
                            plugin.getLogger().warning("ItemsAdder 返回了 null 给 ID: '" + itemsAdderId + "' (配置键: " + itemKey + ")。可能该 ItemsAdder 物品不存在。");
                        }
                    } catch (Exception e) { //捕获所有可能的异常，例如 ItemsAdder API 内部的错误或物品不存在的特定异常
                        plugin.getLogger().log(Level.WARNING, "从 ItemsAdder 获取物品 '" + itemsAdderId + "' (配置键: " + itemKey + ") 时发生错误: " + e.getMessage());
                        // e.printStackTrace(); // 调试时可以取消注释这行来获得更详细的错误堆栈
                        displayItem = null; // 确保获取失败时 displayItem 为 null
                    }
                } else {
                    plugin.getLogger().warning("recipe.yml 中为 '" + itemKey + "' 指定了 ItemsAdder ID ('" + itemsAdderId + "')，但 ItemsAdder 插件未加载或未启用。");
                }
            }

            // 2. 如果不是 ItemsAdder 物品 (没有 itemsadder_id 或获取失败)，则尝试作为普通 Minecraft 物品创建
            if (displayItem == null && itemConfig.contains("material")) {
                String materialName = itemConfig.getString("material").toUpperCase();
                Material material = Material.matchMaterial(materialName);

                if (material != null) {
                    int amount = itemConfig.getInt("amount", 1); // 默认数量为1
                    displayItem = new ItemStack(material, amount);
                } else {
                    plugin.getLogger().warning("recipe.yml 中物品 '" + itemKey + "' 的材质 '" + materialName + "' 无效。");
                }
            }

            // 3. 如果到这里 displayItem 仍然是 null，说明无法创建物品，跳过
            if (displayItem == null) {
                plugin.getLogger().warning("无法为 recipe.yml 中的条目 '" + itemKey + "' 创建物品。请检查配置（例如 itemsadder_id 或 material 是否正确）。");
                continue;
            }

            // 4. 应用配置文件中定义的 Meta 数据 (显示名称, Lore, 药水效果等)
            // 这一步会覆盖从 ItemsAdder 获取的物品的默认名称/Lore，或为普通物品设置名称/Lore
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) { // 正常情况下，有效的 ItemStack 都会有 ItemMeta

                // 设置显示名称 (如果配置中存在)
                if (itemConfig.contains("display_name")) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemConfig.getString("display_name")));
                }

                // 设置 Lore (如果配置中存在)
                if (itemConfig.contains("lore")) {
                    List<String> lore = itemConfig.getStringList("lore").stream()
                            .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                            .collect(Collectors.toList());
                    meta.setLore(lore);
                }

                // 设置 CustomModelData (如果配置中存在)
                // 注意: 如果物品来自 ItemsAdder, 它本身就有模型数据。这里的配置会覆盖它。
                // 通常情况下，如果物品来自 ItemsAdder，你可能不需要在 recipe.yml 中再指定 custom_model_data。
                if (itemConfig.contains("custom_model_data")) {
                    meta.setCustomModelData(itemConfig.getInt("custom_model_data"));
                }

                // 处理药水相关的 Meta (如果物品是药水类型)
                if (meta instanceof PotionMeta && displayItem.getType() == Material.POTION) {
                    PotionMeta potionMeta = (PotionMeta) meta; // 直接使用上面获取的 meta 转型

                    // 优先处理 potion_data (用于基础类型和外观)
                    if (itemConfig.isConfigurationSection("potion_data")) {
                        ConfigurationSection potionDataSection = itemConfig.getConfigurationSection("potion_data");
                        String baseTypeName = potionDataSection.getString("base_type", "WATER").toUpperCase();
//                        boolean extended = potionDataSection.getBoolean("extended", false);
//                        boolean upgraded = potionDataSection.getBoolean("upgraded", false);

                        try {
                            PotionType potionType = PotionType.valueOf(baseTypeName);
                            potionMeta.setBasePotionType(potionType);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("recipe.yml 中物品 '" + itemKey + "' 的 potion_data.base_type '" + baseTypeName + "' 无效。");
                        }
                    }

                    // 然后处理 potion_effects (可以添加或覆盖 potion_data 的效果)
                    if (itemConfig.isList("potion_effects")) {
                        List<String> effectStrings = itemConfig.getStringList("potion_effects");
                        // 如果希望 potion_effects 完全替换 potion_data 的效果，可以在这里清除
                        // potionMeta.clearCustomEffects(); // 但通常 BasePotionData 带来的效果我们是想要的

                        for (String effectString : effectStrings) {
                            String[] parts = effectString.split(",");
                            if (parts.length == 3) {
                                try {
                                    PotionEffectType type = PotionEffectType.getByName(parts[0].trim().toUpperCase());
                                    int duration = Integer.parseInt(parts[1].trim()); // ticks
                                    int amplifier = Integer.parseInt(parts[2].trim()); // 等级 (0 = I, 1 = II)

                                    if (type != null) {
                                        potionMeta.addCustomEffect(new PotionEffect(type, duration, amplifier), true); // true 表示覆盖
                                    } else {
                                        plugin.getLogger().warning("recipe.yml 中物品 '" + itemKey + "' 的 potion_effects 定义了无效的效果类型: " + parts[0]);
                                    }
                                } catch (NumberFormatException e) {
                                    plugin.getLogger().warning("recipe.yml 中物品 '" + itemKey + "' 的 potion_effects 定义的持续时间或等级不是有效数字: " + effectString);
                                } catch (Exception e) { // 其他潜在错误
                                    plugin.getLogger().warning("recipe.yml 中物品 '" + itemKey + "' 的 potion_effects '" + effectString + "' 处理时出错: " + e.getMessage());
                                }
                            } else {
                                plugin.getLogger().warning("recipe.yml 中物品 '" + itemKey + "' 的 potion_effects 定义格式错误 (应为 '类型,持续时间,等级'): " + effectString);
                            }
                        }
                    }
                    // 注意：由于 potionMeta 是 meta 的引用，这里的修改已经作用于 meta
                }
                // 将修改后的 Meta 应用回 ItemStack
                displayItem.setItemMeta(meta);
            } else {
                plugin.getLogger().warning("无法获取物品 '" + itemKey + "' (类型: " + displayItem.getType() + ") 的 ItemMeta。");
            }

            // 5. 设置物品数量 (如果配置中指定)
            // 这一步应该在所有 Meta 操作之后，以防 Meta 操作重置了数量
            if (itemConfig.contains("amount")) {
                int configuredAmount = itemConfig.getInt("amount");
                // 对于堆叠上限为1的物品(比如工具、药水)，确保数量不超过1，除非是明确要多个独立的
                if (displayItem.getMaxStackSize() == 1 && configuredAmount > 1) {
                    // 你可以选择忽略配置中的数量，或者记录一个警告
                    // plugin.getLogger().warning("物品 '" + itemKey + "' 最大堆叠为1, 但配置中数量为 " + configuredAmount + ". 将使用1.");
                    // displayItem.setAmount(1); // 通常药水、工具这类物品应该保持数量为1
                } else {
                    displayItem.setAmount(configuredAmount);
                }
            }

            gui.setItem(slot, displayItem);
        }
        player.openInventory(gui);
    }


    private void openRecipeGUI(Player player) {
        Inventory gui = plugin.getServer().createInventory(null, 45, "§6特殊配方");


        //下一页
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta meta13 = feather.getItemMeta();
        if (meta13 != null) {
            meta13.setDisplayName("§d下一页");
            meta13.setLore(Arrays.asList("§7点击查看下一页", ""));
            feather.setItemMeta(meta13);
        }

        gui.setItem(10, SpeicialItem.furnacePickaxe);
        gui.setItem(11, SpeicialItem.treeAxe);
        gui.setItem(12, SpeicialItem.easyGoldApple);
        gui.setItem(13, SpeicialItem.goldHead);
        gui.setItem(14, SpeicialItem.fireResistancePotion);
        gui.setItem(15, SpeicialItem.anvil);
        gui.setItem(16, SpeicialItem.arrow);
        gui.setItem(19, SpeicialItem.obsidian);
        gui.setItem(20, SpeicialItem.trackCompass);
        gui.setItem(21, SpeicialItem.enchantGoldApple);
        gui.setItem(22, SpeicialItem.specialBone);
        gui.setItem(23, SpeicialItem.storageBag);
        gui.setItem(24, SpeicialItem.shortSword);
        gui.setItem(25, SpeicialItem.fireCrossBow);
        gui.setItem(28, SpeicialItem.totem);
        //gui.setItem(44, feather);

        player.openInventory(gui);
    }

    private void openRecipeGUI2(Player player) {
        Inventory gui = plugin.getServer().createInventory(null, 36, "§6传说武器配方");

        ItemStack specialDiamondSword1 = specialDiamondSword.createSpecialDiamondSword1();

        ItemStack specialDiamondSword2 = specialDiamondSword.createSpecialDiamondSword2();

        ItemStack specialDiamondSword3 = specialDiamondSword.createSpecialDiamondSword3();


//        gui.setItem(10, specialDiamondSword1);
//        gui.setItem(11, specialDiamondSword2);
//         gui.setItem(12, specialDiamondSword3);


        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§6特殊配方")) {
            event.setCancelled(true); // 防止玩家移动物品

            if (event.getSlot() == 10) {
                Player player = (Player) event.getWhoClicked();
                // 玩家点击了熔炉稿
                player.sendMessage("§a查看熔炉稿合成配方");
                // 打开熔炉稿合成配方GUI
                showRecipeGUI1(player);
            }
            if (event.getSlot() == 11) {
                Player player = (Player) event.getWhoClicked();
                // 玩家点击了伐木斧
                player.sendMessage("§a查看伐木斧合成配方");
                showRecipeGUI2(player);
            }
            if (event.getSlot() == 12) {
                Player player = (Player) event.getWhoClicked();
                // 玩家点击了简易金苹果
                player.sendMessage("§a查看简易金苹果合成配方");
                showRecipeGUI3(player);
            }
            if (event.getSlot() == 13) {
                Player player = (Player) event.getWhoClicked();
                // 玩家点击了金头
                player.sendMessage("§a查看金头合成配方");
                showRecipeGUI4(player);
            }
            if (event.getSlot() == 14) {
                Player player = (Player) event.getWhoClicked();
                player.sendMessage("§a查看抗火药水合成配方");
                showRecipeGUI5(player);
            }
            if (event.getSlot() == 15) {
                Player player = (Player) event.getWhoClicked();
                player.sendMessage("§a查看简易铁砧合成配方");
                showRecipeGUI6(player);
            }
            if (event.getSlot() == 16) {
                Player player = (Player) event.getWhoClicked();
                player.sendMessage("§a查看简易箭合成配方");
                showRecipeGUI7(player);
            }
            if (event.getSlot() == 19) {
                Player player = (Player) event.getWhoClicked();
                player.sendMessage("§a查看黑曜石合成配方");
                showRecipeGUI8(player);
            }
            if (event.getSlot() == 20) {
                Player player = (Player) event.getWhoClicked();
                player.sendMessage("§a查看追踪指南针合成配方");
                showRecipeGUI9(player);
            }
            if (event.getSlot() == 21) {
                Player player = (Player) event.getWhoClicked();
                player.sendMessage("§a查看附魔金苹果合成配方");
                showRecipeGUI10(player);
            }
            if (event.getSlot() == 22) {
                Player player = (Player) event.getWhoClicked();
                player.sendMessage("§a查看布鲁斯骨头合成配方");
                showRecipeGUI11(player);
            }
            if (event.getSlot() == 23) {
                Player player = (Player) event.getWhoClicked();
                player.sendMessage("§a查看收纳袋合成配方");
                showRecipeGUI12(player);
            }
            if (event.getSlot() == 24) {
                Player player = (Player) event.getWhoClicked();
                player.sendMessage("§a查看短剑合成配方");
                showRecipeGUI13(player);
            }
            if (event.getSlot() == 25) {
                Player player = (Player) event.getWhoClicked();
                player.sendMessage("§a查看火弩合成配方");
                showRecipeGUI14(player);
            }
            if (event.getSlot() == 28) {
                Player player = (Player) event.getWhoClicked();
                player.sendMessage("§a查看图腾合成配方");
                showRecipeGUI15(player);
            }
            if (event.getSlot() == 44) {
                Player player = (Player) event.getWhoClicked();
                // player.sendMessage("§a查看下一页");
                // openRecipeGUI2(player);
            }
        }
        if (event.getView().getTitle().equals("§6传说武器配方")) {
            event.setCancelled(true); // 防止玩家移动物品
            if (event.getSlot() == 10) {
                Player player = (Player) event.getWhoClicked();
                showRecipeGUI16(player);
            }
            if (event.getSlot() == 11) {
                Player player = (Player) event.getWhoClicked();
                showRecipeGUI17(player);
            }
            if (event.getSlot() == 12) {
                Player player = (Player) event.getWhoClicked();
                showRecipeGUI18(player);
            }



        }




    }


    private void showRecipeGUI1(Player player) {
        Inventory recipeGui = plugin.getServer().createInventory(null, 27, "§6熔炉稿合成配方");

        // 设置配方形状
        recipeGui.setItem(1, new ItemStack(Material.RAW_IRON));
        recipeGui.setItem(2, new ItemStack(Material.RAW_IRON));
        recipeGui.setItem(3, new ItemStack(Material.RAW_IRON));
        recipeGui.setItem(10, new ItemStack(Material.COAL));
        recipeGui.setItem(11, new ItemStack(Material.STICK));
        recipeGui.setItem(12, new ItemStack(Material.COAL));
        recipeGui.setItem(20, new ItemStack(Material.STICK));

        //放玻璃
        recipeGui.setItem(0, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(4, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(9, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(13, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(18, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(22, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));

        // 设置返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backButton.setItemMeta(backMeta);
        recipeGui.setItem(26, backButton);
        player.openInventory(recipeGui);
    }

    private void showRecipeGUI2(Player player) {
        Inventory recipeGui = plugin.getServer().createInventory(null, 27, "§6伐木斧合成配方");

        // 设置配方形状
        recipeGui.setItem(1, new ItemStack(Material.FLINT));
        recipeGui.setItem(2, new ItemStack(Material.IRON_INGOT));
        recipeGui.setItem(3, new ItemStack(Material.IRON_INGOT));
        recipeGui.setItem(12, new ItemStack(Material.IRON_INGOT));
        recipeGui.setItem(11, new ItemStack(Material.STICK));
        recipeGui.setItem(20, new ItemStack(Material.STICK));

        //放玻璃
        recipeGui.setItem(0, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(4, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(9, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(13, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(18, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(22, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));

        // 设置返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backButton.setItemMeta(backMeta);
        recipeGui.setItem(26, backButton);
        player.openInventory(recipeGui);
    }

    private void showRecipeGUI3(Player player) {
        Inventory recipeGui = plugin.getServer().createInventory(null, 27, "§6简易金苹果合成配方");

        // 设置配方形状
        recipeGui.setItem(2, new ItemStack(Material.GOLD_INGOT));
        recipeGui.setItem(10, new ItemStack(Material.GOLD_INGOT));
        recipeGui.setItem(11, new ItemStack(Material.APPLE));
        recipeGui.setItem(12, new ItemStack(Material.GOLD_INGOT));
        recipeGui.setItem(20, new ItemStack(Material.GOLD_INGOT));

        //放玻璃
        recipeGui.setItem(0, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(4, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(9, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(13, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(18, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(22, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));

        // 设置返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backButton.setItemMeta(backMeta);
        recipeGui.setItem(26, backButton);
        player.openInventory(recipeGui);
    }

    private void showRecipeGUI4(Player player) {
        Inventory recipeGui = plugin.getServer().createInventory(null, 27, "§6金头合成配方");

        // 设置配方形状
        recipeGui.setItem(1, new ItemStack(Material.GOLD_INGOT));
        recipeGui.setItem(2, new ItemStack(Material.GOLD_INGOT));
        recipeGui.setItem(3, new ItemStack(Material.GOLD_INGOT));
        recipeGui.setItem(10, new ItemStack(Material.GOLD_INGOT));
        recipeGui.setItem(12, new ItemStack(Material.GOLD_INGOT));
        recipeGui.setItem(19, new ItemStack(Material.GOLD_INGOT));
        recipeGui.setItem(20, new ItemStack(Material.GOLD_INGOT));
        recipeGui.setItem(21, new ItemStack(Material.GOLD_INGOT));
        recipeGui.setItem(11, new ItemStack(Material.PLAYER_HEAD));

        //放玻璃
        recipeGui.setItem(0, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(4, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(9, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(13, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(18, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(22, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));

        // 设置返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backButton.setItemMeta(backMeta);
        recipeGui.setItem(26, backButton);
        player.openInventory(recipeGui);
    }

    public ItemStack createWaterBottle() {
        ItemStack waterBottle = new ItemStack(Material.POTION, 1); // 数量为1
        ItemMeta meta = waterBottle.getItemMeta();
        if (meta instanceof PotionMeta) {
            PotionMeta potionMeta = (PotionMeta) meta;
            potionMeta.setBasePotionType(PotionType.WATER);
            waterBottle.setItemMeta(potionMeta);
        }
        return waterBottle;
    }


    //抗火药水
    private void showRecipeGUI5(Player player) {
        Inventory recipeGui = plugin.getServer().createInventory(null, 27, "§6抗火药水配方");

        // 设置配方形状
        recipeGui.setItem(2, new ItemStack(Material.LAVA_BUCKET));
        recipeGui.setItem(11, createWaterBottle());
        recipeGui.setItem(20, new ItemStack(Material.BLAZE_POWDER));

        //放玻璃
        recipeGui.setItem(0, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(4, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(9, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(13, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(18, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(22, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));

        // 设置返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backButton.setItemMeta(backMeta);
        recipeGui.setItem(26, backButton);
        player.openInventory(recipeGui);
    }

    //铁砧
    private void showRecipeGUI6(Player player) {
        Inventory recipeGui = plugin.getServer().createInventory(null, 27, "§6简易铁砧配方");

        // 设置配方形状
        recipeGui.setItem(1, new ItemStack(Material.IRON_INGOT));
        recipeGui.setItem(2, new ItemStack(Material.IRON_INGOT));
        recipeGui.setItem(3, new ItemStack(Material.IRON_INGOT));
        recipeGui.setItem(11, new ItemStack(Material.IRON_BLOCK));
        recipeGui.setItem(19, new ItemStack(Material.IRON_INGOT));
        recipeGui.setItem(20, new ItemStack(Material.IRON_INGOT));
        recipeGui.setItem(21, new ItemStack(Material.IRON_INGOT));

        //放玻璃
        recipeGui.setItem(0, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(4, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(9, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(13, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(18, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(22, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));

        // 设置返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backButton.setItemMeta(backMeta);
        recipeGui.setItem(26, backButton);
        player.openInventory(recipeGui);
    }

    private void showRecipeGUI7(Player player) {
        Inventory recipeGui = plugin.getServer().createInventory(null, 27, "§6简易箭配方");

        // 设置配方形状
        recipeGui.setItem(1, new ItemStack(Material.FLINT));
        recipeGui.setItem(2, new ItemStack(Material.FLINT));
        recipeGui.setItem(3, new ItemStack(Material.FLINT));
        recipeGui.setItem(10, new ItemStack(Material.STICK));
        recipeGui.setItem(11, new ItemStack(Material.STICK));
        recipeGui.setItem(12, new ItemStack(Material.STICK));
        recipeGui.setItem(19, new ItemStack(Material.FEATHER));
        recipeGui.setItem(20, new ItemStack(Material.FEATHER));
        recipeGui.setItem(21, new ItemStack(Material.FEATHER));

        //放玻璃
        recipeGui.setItem(0, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(4, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(9, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(13, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(18, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(22, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));

        // 设置返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backButton.setItemMeta(backMeta);
        recipeGui.setItem(26, backButton);
        player.openInventory(recipeGui);
    }

    private void showRecipeGUI8(Player player) {
        Inventory recipeGui = plugin.getServer().createInventory(null, 27, "§6黑曜石配方");

        // 设置配方形状
        recipeGui.setItem(1, new ItemStack(Material.WATER_BUCKET));
        recipeGui.setItem(2, new ItemStack(Material.LAVA_BUCKET));

        //放玻璃
        recipeGui.setItem(0, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(4, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(9, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(13, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(18, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(22, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));

        // 设置返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backButton.setItemMeta(backMeta);
        recipeGui.setItem(26, backButton);
        player.openInventory(recipeGui);
    }

    private void showRecipeGUI9(Player player) {
        Inventory recipeGui = plugin.getServer().createInventory(null, 27, "§6追踪指南针配方");

        // 设置配方形状
        recipeGui.setItem(2, new ItemStack(Material.COPPER_INGOT));
        recipeGui.setItem(11, new ItemStack(Material.REDSTONE));
        recipeGui.setItem(10, new ItemStack(Material.COPPER_INGOT));
        recipeGui.setItem(12, new ItemStack(Material.COPPER_INGOT));
        recipeGui.setItem(20, new ItemStack(Material.COPPER_INGOT));

        //放玻璃
        recipeGui.setItem(0, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(4, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(9, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(13, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(18, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(22, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));

        // 设置返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backButton.setItemMeta(backMeta);
        recipeGui.setItem(26, backButton);
        player.openInventory(recipeGui);
    }

    private void showRecipeGUI10(Player player) {
        Inventory recipeGui = plugin.getServer().createInventory(null, 27, "§6附魔金苹果配方");

        // 设置配方形状
        recipeGui.setItem(11, new ItemStack(Material.APPLE));
        recipeGui.setItem(10, new ItemStack(Material.GOLD_BLOCK));
        recipeGui.setItem(12, new ItemStack(Material.GOLD_BLOCK));
        recipeGui.setItem(19, new ItemStack(Material.GOLD_BLOCK));
        recipeGui.setItem(20, new ItemStack(Material.GOLD_BLOCK));
        recipeGui.setItem(21, new ItemStack(Material.GOLD_BLOCK));
        recipeGui.setItem(1, new ItemStack(Material.GOLD_BLOCK));
        recipeGui.setItem(2, new ItemStack(Material.GOLD_BLOCK));
        recipeGui.setItem(3, new ItemStack(Material.GOLD_BLOCK));

        //放玻璃
        recipeGui.setItem(0, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(4, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(9, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(13, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(18, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(22, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));

        // 设置返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backButton.setItemMeta(backMeta);
        recipeGui.setItem(26, backButton);
        player.openInventory(recipeGui);
    }

    private void showRecipeGUI11(Player player) {
        Inventory recipeGui = plugin.getServer().createInventory(null, 27, "§6布鲁斯骨头配方");

        // 设置配方形状
        recipeGui.setItem(11, new ItemStack(Material.COPPER_BLOCK));
        recipeGui.setItem(10, new ItemStack(Material.BONE));
        recipeGui.setItem(12, new ItemStack(Material.BONE));
        recipeGui.setItem(2, new ItemStack(Material.BONE));
        recipeGui.setItem(20, new ItemStack(Material.BONE));

        //放玻璃
        recipeGui.setItem(0, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(4, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(9, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(13, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(18, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(22, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));

        // 设置返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backButton.setItemMeta(backMeta);
        recipeGui.setItem(26, backButton);
        player.openInventory(recipeGui);
    }

    private void showRecipeGUI12(Player player) {
        Inventory recipeGui = plugin.getServer().createInventory(null, 27, "§6收纳袋配方");

        // 设置配方形状
        recipeGui.setItem(11, new ItemStack(Material.CHEST));
        recipeGui.setItem(10, new ItemStack(Material.LEATHER));
        recipeGui.setItem(12, new ItemStack(Material.LEATHER));
        recipeGui.setItem(2, new ItemStack(Material.LEATHER));
        recipeGui.setItem(20, new ItemStack(Material.LEATHER));

        //放玻璃
        recipeGui.setItem(0, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(4, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(9, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(13, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(18, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(22, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));

        // 设置返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backButton.setItemMeta(backMeta);
        recipeGui.setItem(26, backButton);
        player.openInventory(recipeGui);
    }

    private void showRecipeGUI13(Player player) {
        Inventory recipeGui = plugin.getServer().createInventory(null, 27, "§6短剑配方");

        // 设置配方形状
        recipeGui.setItem(1, new ItemStack(Material.COPPER_INGOT));
        recipeGui.setItem(2, new ItemStack(Material.COPPER_INGOT));
        recipeGui.setItem(3, new ItemStack(Material.COPPER_INGOT));
        recipeGui.setItem(10, new ItemStack(Material.COPPER_INGOT));
        recipeGui.setItem(11, new ItemStack(Material.IRON_SWORD));
        recipeGui.setItem(12, new ItemStack(Material.COPPER_INGOT));
        recipeGui.setItem(19, new ItemStack(Material.COPPER_INGOT));
        recipeGui.setItem(20, new ItemStack(Material.COPPER_INGOT));
        recipeGui.setItem(21, new ItemStack(Material.COPPER_INGOT));

        //放玻璃
        recipeGui.setItem(0, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(4, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(9, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(13, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(18, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(22, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));

        // 设置返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backButton.setItemMeta(backMeta);
        recipeGui.setItem(26, backButton);
        player.openInventory(recipeGui);
    }

    private void showRecipeGUI14(Player player) {
        Inventory recipeGui = plugin.getServer().createInventory(null, 27, "§6火弩配方");

        // 设置配方形状
        recipeGui.setItem(2, new ItemStack(Material.MAGMA_CREAM));
        recipeGui.setItem(10, new ItemStack(Material.BLAZE_POWDER));
        recipeGui.setItem(11, new ItemStack(Material.CROSSBOW));
        recipeGui.setItem(12, new ItemStack(Material.BLAZE_POWDER));
        recipeGui.setItem(20, new ItemStack(Material.MAGMA_CREAM));

        //放玻璃
        recipeGui.setItem(0, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(4, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(9, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(13, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(18, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(22, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));

        // 设置返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backButton.setItemMeta(backMeta);
        recipeGui.setItem(26, backButton);
        player.openInventory(recipeGui);
    }

    private void showRecipeGUI15(Player player) {
        Inventory recipeGui = plugin.getServer().createInventory(null, 27, "§6不死图腾配方");

        // 设置配方形状
        recipeGui.setItem(2, new ItemStack(Material.GOLD_INGOT));
        recipeGui.setItem(10, new ItemStack(Material.GOLD_INGOT));
        recipeGui.setItem(11, new ItemStack(Material.GHAST_TEAR));
        recipeGui.setItem(12, new ItemStack(Material.GOLD_INGOT));
        recipeGui.setItem(20, new ItemStack(Material.GOLD_INGOT));

        //放玻璃
        recipeGui.setItem(0, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(4, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(9, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(13, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(18, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(22, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));

        // 设置返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backButton.setItemMeta(backMeta);
        recipeGui.setItem(26, backButton);
        player.openInventory(recipeGui);
    }


    private void showRecipeGUI16(Player player) {
        Inventory recipeGui = plugin.getServer().createInventory(null, 27, "§6亚瑟王之剑配方");

        // 设置配方形状
        recipeGui.setItem(2, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));
        recipeGui.setItem(11, new ItemStack(Material.DIAMOND_SWORD));
        recipeGui.setItem(10, new ItemStack(Material.QUARTZ));
        recipeGui.setItem(12, new ItemStack(Material.QUARTZ));
        recipeGui.setItem(20, new ItemStack(Material.GOLDEN_APPLE));

        //放玻璃
        recipeGui.setItem(0, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(4, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(9, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(13, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(18, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(22, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));

        // 设置返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backButton.setItemMeta(backMeta);
        recipeGui.setItem(26, backButton);
        player.openInventory(recipeGui);
    }

    private void showRecipeGUI17(Player player) {
        Inventory recipeGui = plugin.getServer().createInventory(null, 27, "§6龙武士刀配方");

        // 设置配方形状
        recipeGui.setItem(2, new ItemStack(Material.ENDER_PEARL));
        recipeGui.setItem(11, new ItemStack(Material.DIAMOND_SWORD));
        recipeGui.setItem(10, new ItemStack(Material.DIAMOND));
        recipeGui.setItem(12, new ItemStack(Material.DIAMOND));
        recipeGui.setItem(20, new ItemStack(Material.END_CRYSTAL));

        //放玻璃
        recipeGui.setItem(0, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(4, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(9, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(13, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(18, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(22, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));

        // 设置返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backButton.setItemMeta(backMeta);
        recipeGui.setItem(26, backButton);
        player.openInventory(recipeGui);
    }

    private void showRecipeGUI18(Player player) {
        Inventory recipeGui = plugin.getServer().createInventory(null, 27, "§6绿宝石剑配方");

        // 设置配方形状
        recipeGui.setItem(2, new ItemStack(Material.GOLDEN_CARROT));
        recipeGui.setItem(11, new ItemStack(Material.DIAMOND_SWORD));
        recipeGui.setItem(10, new ItemStack(Material.EMERALD_BLOCK));
        recipeGui.setItem(12, new ItemStack(Material.EMERALD_BLOCK));
        recipeGui.setItem(20, new ItemStack(Material.BELL));

        //放玻璃
        recipeGui.setItem(0, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(4, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(9, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(13, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(18, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        recipeGui.setItem(22, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));

        // 设置返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backButton.setItemMeta(backMeta);
        recipeGui.setItem(26, backButton);
        player.openInventory(recipeGui);
    }



    @EventHandler
    public void onRecipeGUIClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (title.equals("§6熔炉稿合成配方") || title.equals("§6伐木斧合成配方") ||
        title.equals("§6简易金苹果合成配方") || title.equals("§6金头合成配方") ||
                title.equals("§6抗火药水配方") ||
                title.equals("§6简易铁砧配方") || title.equals("§6简易箭配方")
        || title.equals("§6黑曜石配方") || title.equals("§6追踪指南针配方")
        || title.equals("§6附魔金苹果配方") || title.equals("§6布鲁斯骨头配方")
                || title.equals("§6收纳袋配方")  || title.equals("§6亚瑟王之剑配方")
        || title.equals("§6龙武士刀配方") || title.equals("§6绿宝石剑配方")
                || title.equals("§6短剑配方") || title.equals("§6火弩配方")
                || title.equals("§6不死图腾配方")
               ) {
          //   || title.equals("§6传说武器配方")
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            // 处理返回按钮
            if (clicked.getType() == Material.ARROW &&
                    clicked.getItemMeta() != null &&
                    clicked.getItemMeta().getDisplayName().equals("§c返回")) {
                openRecipeGUI((Player) event.getWhoClicked());
            }

    }
    }


}
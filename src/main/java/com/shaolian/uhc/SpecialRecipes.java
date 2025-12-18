package com.shaolian.uhc;

import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.java.JavaPlugin;//!!!
import org.bukkit.potion.PotionType;

import java.util.*;


public class  SpecialRecipes  {

    private final JavaPlugin plugin;


    public SpecialRecipes(JavaPlugin plugin) {
        this.plugin = plugin;

        disableVanillaBundleRecipe(); // 禁用原版收纳袋配方
        disableVanillaBundleRecipe2(); // 禁用盾牌
        registerRecipes();//熔炉稿
        registerRecipes2();//简易金苹果  3次
        registerRecipes3();//金头
        registerRecipes4();//斧头
        registerRecipes5();//狼狗  1次
        registerRecipes6();//特殊收纳袋
        registerRecipes7();//抗火
        registerRecipes8();//铁砧
        registerRecipes9();//简易箭
        registerRecipes10();//黑曜石
        registerRecipes11();//指南针
        registerRecipes12();//附魔金苹果
        //registerRecipes12();//绿宝石剑
        registerRecipes13();//铁傀儡
        registerRecipes14();//短剑
        registerRecipes15();//火弩
        registerRecipes16();//图腾



    }

    private void disableVanillaBundleRecipe() {
        // 禁用原版收纳袋的合成配方
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();
            if (recipe != null && recipe.getResult().getType() == Material.BUNDLE) {
                it.remove();
            }
        }
    }

    private void disableVanillaBundleRecipe2() {
        // 禁用盾牌的合成配方
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();
            if (recipe != null && recipe.getResult().getType() == Material.SHIELD) {
                it.remove();
            }
        }
    }


    private void registerRecipes() {
        ItemStack furnacePickaxe = SpeicialItem.furnacePickaxe;
        // 创建合成配方
        NamespacedKey key = new NamespacedKey(plugin, "furnace_pickaxe"); // 使用唯一标识符
        ShapedRecipe recipe = new ShapedRecipe(key, furnacePickaxe) // 绑定唯一标识符
                .shape("III", "CSC", " S ")  // 修改形状
                .setIngredient('I', Material.RAW_IRON)
                .setIngredient('C', Material.COAL)
                .setIngredient('S', Material.STICK);

        // 注册配方
        plugin.getServer().addRecipe(recipe);
    }


    private void registerRecipes2() {
        ItemStack easyGoldApple = new ItemStack(Material.GOLDEN_APPLE);
        // 创建合成配方
        NamespacedKey key = new NamespacedKey(plugin, "easy_golden_apple"); // 使用唯一标识符
        ShapedRecipe recipe = new ShapedRecipe(key, easyGoldApple) // 绑定唯一标识符
                .shape(" I ", "ISI", " I ")  // 修改形状
                .setIngredient('I', Material.GOLD_INGOT)
                .setIngredient('S', Material.APPLE);

        // 注册配方
        plugin.getServer().addRecipe(recipe);
    }

    private void registerRecipes3() {
        // 创建金头
        ItemStack goldHead = SpeicialItem.goldHead;
        // 创建合成配方
        NamespacedKey key = new NamespacedKey(plugin, "golden_head"); // 使用唯一标识符
        ShapedRecipe recipe = new ShapedRecipe(key, goldHead) // 绑定唯一标识符
                .shape("III", "ISI", "III")  // 修改形状
                .setIngredient('I', Material.GOLD_INGOT)
                .setIngredient('S', Material.PLAYER_HEAD);

        // 注册配方
        plugin.getServer().addRecipe(recipe);
    }

    private void registerRecipes4() {
        // 创建伐木斧
        ItemStack treeAxe = SpeicialItem.treeAxe;
        // 创建合成配方
        NamespacedKey key = new NamespacedKey(plugin, "treeAxe"); // 使用唯一标识符
        ShapedRecipe recipe = new ShapedRecipe(key, treeAxe) // 绑定唯一标识符
                .shape("ASS", " IS", " I ")  // 修改形状
                .setIngredient('A', Material.FLINT)
                .setIngredient('I', Material.STICK)
                .setIngredient('S', Material.IRON_INGOT);

        // 注册配方
        plugin.getServer().addRecipe(recipe);
    }

    private void registerRecipes5() {
        // 创建一个特殊骨头的物品堆
        ItemStack specialBone = SpeicialItem.specialBone;
        // 创建合成配方
        NamespacedKey key = new NamespacedKey(plugin, "special_bone");
        ShapedRecipe recipe = new ShapedRecipe(key, specialBone)
                .shape(" B ", "BCB", " B ")  // 修改形状
                .setIngredient('B', Material.BONE) // 骨头
                .setIngredient('C', Material.COPPER_BLOCK); // 铜块

        // 注册配方
        plugin.getServer().addRecipe(recipe);
    }

    private void registerRecipes6() {
        // 创建一个特殊的收纳袋
        ItemStack specialBone = SpeicialItem.storageBag;
        // 创建合成配方
        NamespacedKey key = new NamespacedKey(plugin, "special_bundle");
        ShapedRecipe recipe = new ShapedRecipe(key, specialBone)
                .shape(" B ", "BCB", " B ")  // 修改形状
                .setIngredient('B', Material.LEATHER)
                .setIngredient('C', Material.CHEST);

        // 注册配方
        plugin.getServer().addRecipe(recipe);
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

    private void registerRecipes7() {
        // 创建抗火药水
        ItemStack fireResistancePotion = SpeicialItem.fireResistancePotion;

        // 创建合成配方
        NamespacedKey key = new NamespacedKey(plugin, "fire_resistance_potion"); // 使用唯一标识符
        ShapedRecipe recipe = new ShapedRecipe(key, fireResistancePotion) // 绑定唯一标识符
                .shape(" L ", " P ", " D ")  // 修改形状
                .setIngredient('P', new RecipeChoice.ExactChoice(createWaterBottle())) // 水瓶
                .setIngredient('L', Material.LAVA_BUCKET) // 岩浆桶
                .setIngredient('D', Material.BLAZE_POWDER); // 烈焰粉

        // 注册配方
        plugin.getServer().addRecipe(recipe);
    }

    private void registerRecipes8() {
        // 创建铁砧
        ItemStack anvil = new ItemStack(Material.ANVIL);

        // 创建合成配方
        NamespacedKey key = new NamespacedKey(plugin, "easy_anvil"); // 使用唯一标识符
        ShapedRecipe recipe = new ShapedRecipe(key, anvil) // 绑定唯一标识符
                .shape("III", " B ", "III")  // 修改形状
                .setIngredient('I', Material.IRON_INGOT)
                .setIngredient('B', Material.IRON_BLOCK);

        // 注册配方
        plugin.getServer().addRecipe(recipe);
    }


    private void registerRecipes9() {
        // 创建16只箭
        ItemStack arrow = new ItemStack(Material.ARROW, 16);

        // 创建合成配方
        NamespacedKey key = new NamespacedKey(plugin, "arrow"); // 使用唯一标识符
        ShapedRecipe recipe = new ShapedRecipe(key,arrow)
                .shape("FFF", "SSS", "YYY")
                .setIngredient('F', Material.FLINT)
                .setIngredient('S', Material.STICK)
                .setIngredient('Y', Material.FEATHER);
        // 注册配方
        plugin.getServer().addRecipe(recipe);
    }

    private void registerRecipes10() {
        // 合成黑曜石
        ItemStack obsidian = new ItemStack(Material.OBSIDIAN);

        // 创建合成配方
        NamespacedKey key = new NamespacedKey(plugin, "obsidian"); // 使用唯一标识符
        ShapelessRecipe recipe = new ShapelessRecipe(key, obsidian)
                .addIngredient(Material.WATER_BUCKET)
                .addIngredient(Material.LAVA_BUCKET);

        // 注册配方
        plugin.getServer().addRecipe(recipe);
    }


    private void registerRecipes11() {
        // 合成特殊指南针
        ItemStack trackCompass = SpeicialItem.getTrackCompass();
        // 创建合成配方
        NamespacedKey key = new NamespacedKey(plugin, "trackCompass"); // 使用唯一标识符
        ShapedRecipe recipe = new ShapedRecipe(key, trackCompass)
                .shape(" C ", "CRC", " C ")  // 修改形状
                .setIngredient('C',Material.COPPER_INGOT)
                .setIngredient('R',Material.REDSTONE);


        // 注册配方
        plugin.getServer().addRecipe(recipe);
    }

    private void registerRecipes12() {
        ItemStack enchantGoldApple = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);
        // 创建合成配方
        NamespacedKey key = new NamespacedKey(plugin, "enchantGoldApple"); // 使用唯一标识符
        ShapedRecipe recipe = new ShapedRecipe(key, enchantGoldApple)
                .shape("GGG", "GAG", "GGG")  // 修改形状
                .setIngredient('G',Material.GOLD_BLOCK)
                .setIngredient('A',Material.APPLE);


        // 注册配方
        plugin.getServer().addRecipe(recipe);
    }




    private void registerRecipes13() {
        // 创建召唤铁傀儡的echo_shard
        ItemStack summonEcho = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = summonEcho.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6召唤铁傀儡的拴绳");
            meta.setLore(Arrays.asList("§7右键召唤铁傀儡"));
            meta.setCustomModelData(1002); // 使用自定义模型数据
            summonEcho.setItemMeta(meta);
        }

        // 创建合成配方
        NamespacedKey key = new NamespacedKey(plugin, "echo_shard");
        ShapedRecipe recipe = new ShapedRecipe(key, summonEcho)
                .shape("III", "S S", " I ")
                .setIngredient('I', Material.IRON_INGOT)
                .setIngredient('S', Material.STRING);

        plugin.getServer().addRecipe(recipe);
    }

    private void registerRecipes14() {
        ItemStack shortSword = SpeicialItem.createShortSword();
        NamespacedKey key = new NamespacedKey(plugin, "short_sword");
        ShapedRecipe recipe = new ShapedRecipe(key, shortSword)
                .shape("CCC", "CIC", "CCC")
                .setIngredient('C', Material.COPPER_INGOT)
                .setIngredient('I', Material.IRON_SWORD);

        plugin.getServer().addRecipe(recipe);
    }

    private void registerRecipes15() {
        ItemStack shortSword = SpeicialItem.createFireCrossBow();
        NamespacedKey key = new NamespacedKey(plugin, "fire_crossbow");
        ShapedRecipe recipe = new ShapedRecipe(key, shortSword)
                .shape(" M ", "BCB", " M ")
                .setIngredient('M', Material.MAGMA_CREAM)
                .setIngredient('B', Material.BLAZE_POWDER)
                .setIngredient('C', Material.CROSSBOW);

        plugin.getServer().addRecipe(recipe);
    }

    private void registerRecipes16() {
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        NamespacedKey key = new NamespacedKey(plugin, "totem");
        ShapedRecipe recipe = new ShapedRecipe(key, totem)
                .shape(" G ", "GTG", " G ")
                .setIngredient('G', Material.GOLD_INGOT)
                .setIngredient('T', Material.GHAST_TEAR);

        plugin.getServer().addRecipe(recipe);
    }








}

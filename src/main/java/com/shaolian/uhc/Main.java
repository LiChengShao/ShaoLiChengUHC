package com.shaolian.uhc;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;


import org.bukkit.*;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;

import java.io.*;

//vault
//import net.milkbowl.vault.economy.Economy;
//import net.milkbowl.vault.economy.Economy;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.ChunkGenerator;


import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;


public class Main extends JavaPlugin implements Listener {

    // 添加静态变量
    private GameManager gameManager;
    private PlayerData playerData;
    private GameListener gameListener;
    private ChestLevel chestLevel;
    private SpecialRecipes specialRecipes;
    private World NEOWorld;
    private SpeicialItem specialItem;
    private File pregenFlagFile;



    String serverIP = "mc.mooncookie.cn";
    private FileConfiguration recipeConfig = null;
    private File recipeConfigFile = null;


    public Main() {

    }


//比如实现worldedit插件作为依赖？

    @Override
    public void onEnable() {

        saveDefaultConfig(); // 加载或创建 config.yml

        // 初始化和加载 recipe.yml
        initializeRecipeConfig();

        // 注册自定义占位符
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CustomPlaceholders(this).register();
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TabSidebar(this, serverIP).register();
        }

        loadAndPasteSchematics(); // 新增：加载并粘贴 Schematic 文件

        //生成或者初始化NEO
        createOrLoadNEOWorld();

        //先把中央区域复制过来再说(再加载箱子)
        worldEdit();

        // 插件启用时的逻辑
        gameManager = new GameManager();//会先执行这个类里面的构造方法，再往下执行
        playerData = new PlayerData(this);
        gameListener = new GameListener(gameManager, this, playerData);
        chestLevel = new ChestLevel();
        specialRecipes = new SpecialRecipes(this);
        specialItem = new SpeicialItem(this);



        Bukkit.getPluginManager().registerEvents(gameManager, this);
        Bukkit.getPluginManager().registerEvents(gameListener, this);
        Bukkit.getPluginManager().registerEvents(chestLevel, this);


//        saveRecipeYML();
        gotoNEO();
        gotoGameWorld();
        getChestCommand();
        onPlaceChestCommand();

        gotoFinalWorld();

        //复制schematic文件
        loadAndPasteSchematics2();


        //区块预加载
        QuKuaiPreload();



    }


    @Override
    public void onDisable() {
        chestLevel.onDisable(); // 保存箱子位置
    }

    // --- recipe.yml 相关方法 ---
    public void initializeRecipeConfig() {
        //如果插件文件夹下不存在recipe.yml文件，则从jar包中的recipe.yml复制到插件文件夹
        recipeConfigFile = new File(getDataFolder(), "recipe.yml");
        if (!recipeConfigFile.exists()) {
            saveResource("recipe.yml", false); // 从 src/main/resources 复制到插件文件夹
        }
        reloadRecipeConfig(); // 加载配置到内存
    }

    //返回插件文件下的recipe.yml文件配置
    public FileConfiguration getRecipeConfig() {
        if (recipeConfig == null) {
            reloadRecipeConfig();
        }
        return recipeConfig;
    }

    //
    public void reloadRecipeConfig() {
        if (recipeConfigFile == null) {
            recipeConfigFile = new File(getDataFolder(), "recipe.yml");
        }
        recipeConfig = YamlConfiguration.loadConfiguration(recipeConfigFile);

        // 设置默认值 (从JAR包中的 recipe.yml 读取)
        InputStream defaultConfigStream = getResource("recipe.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig =
                    YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));
            recipeConfig.setDefaults(defaultConfig);
        }
    }

    //将内存中当前加载的 recipeConfig 对象保存到磁盘文件 recipe.yml 中
    //如果你在服务器运行时，手动修改了磁盘上的 recipe.yml 文件，此时内存中的 recipeConfig 对象仍然是旧的、未修改过的版本
    public void saveRecipeConfig() { // 如果你允许在游戏内修改并保存 recipe.yml
        if (recipeConfig == null || recipeConfigFile == null) {
            getLogger().severe("recipeConfigFile 为 null");
            return;
        }
        try {
            // 在保存之前，确保所有默认值都被考虑（如果希望新添加的默认项被写入）
            // recipeConfig.options().copyDefaults(true); // 可选：如果你想把jar中新增的默认配置项写入用户文件
            getRecipeConfig().save(recipeConfigFile); // 保存当前内存中的 recipeConfig 到文件
            getLogger().info("recipe.yml 已保存到磁盘。");
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "无法保存 recipe.yml 到 " + recipeConfigFile, ex);
        }
    }


    public void gotoNEO(){
        // 注册命令
        this.getCommand("gotoneo").setExecutor((sender, command, label, args) -> {
            if (!sender.isOp()) {
                sender.sendMessage("§c你没有权限使用此命令！");
                return true;
            }

            org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
            World neoWorld = Bukkit.getWorld("NEO");

            if (neoWorld == null) {
                sender.sendMessage("§cNEO世界未找到！");
                return true;
            }

            Location location = new Location(neoWorld, 0, 92, 0);
            player.teleport(location);
            sender.sendMessage("§a已传送到NEO世界的 (0, 92, 0) 位置！");
            return true;
        });
    }

    public void gotoGameWorld(){
        // 注册命令
        this.getCommand("gotogameworld").setExecutor((sender, command, label, args) -> {
            if (!sender.isOp()) {
                sender.sendMessage("§c你没有权限使用此命令！");
                // Bukkit 的命令处理中，return true 表示命令已成功处理，Bukkit 不会再将此命令传递给其他插件
                return true;
            }

            org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
            World gameWorld = Bukkit.getWorld("gameWorld");

            if (gameWorld == null) {
                sender.sendMessage("§cgameWorld世界未找到！");
                return true;
            }

            Location location = new Location(gameWorld, 0, 64, 0);
            player.teleport(location);
            sender.sendMessage("§a已传送到gameWorld世界的 (0, 64, 0) 位置！");
            return true;
        });
    }


    public void getChestCommand() {
        // 注册命令
        this.getCommand("getchest").setExecutor((sender, command, label, args) -> {
            if (!sender.isOp()) {
                sender.sendMessage("§c你没有权限使用此命令！");
                return true;
            }

            if (args.length != 1) {
                sender.sendMessage("§c用法: /getchest <等级>");
                return true;
            }

            try {
                Player player = (Player) sender;
                ChestLevel.Level level = ChestLevel.Level.valueOf(args[0].toUpperCase());

                // 在玩家位置生成箱子
                Location loc = player.getLocation();
                Location loc2 = new Location(loc.getWorld(), loc.getX(), loc.getY()-1, loc.getZ());
                loc2.getBlock().setType(Material.CHEST);  // 先设置方块类型
                Chest chest = (Chest) loc2.getBlock().getState();  // 再获取Chest对象

                // 填充箱子
                chestLevel.fillChest(chest, level);
                sender.sendMessage("§a成功生成 " + level + " 等级的箱子！");
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§c无效的箱子等级！可用等级: COMMON, RARE, DESERT, JUNGLE, SNOW, LAVA");
            }
            return true;
        });
    }

    public void gotoFinalWorld(){
        // 注册命令
        this.getCommand("gotoFinalWorldCopy").setExecutor((sender, command, label, args) -> {
            if (!sender.isOp()) {
                sender.sendMessage("§c你没有权限使用此命令！");
                return true;
            }

            org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
            World finalWorldCopy = Bukkit.getWorld("FinalWorldCopy");

            if (finalWorldCopy == null) {
                sender.sendMessage("§cFinalWorldCopy世界未找到！");
                return true;
            }

            Location location = new Location(finalWorldCopy, 0, 64, 0);
            player.teleport(location);
            sender.sendMessage("§a已传送到FinalWorldCopy世界的 (0, 64, 0) 位置！");
            return true;
        });
    }


    // 添加一个方法来处理 /placechest 指令
    public void onPlaceChestCommand() {
        // 注册命令
        this.getCommand("placechest").setExecutor((sender, command, label, args) -> {
            if (!sender.isOp()) {
                sender.sendMessage("§c你没有权限使用此命令！");
                return true;
            }

            Player player = (Player) sender;
            UUID playerId = player.getUniqueId();
            if (ChestLevel.enabledPlayers.contains(playerId)) {
                ChestLevel.enabledPlayers.remove(playerId); // 如果已启用，则禁用
                player.sendMessage("箱子放置功能已禁用。");
            } else {
                ChestLevel.enabledPlayers.add(playerId); // 如果未启用，则启用
                player.sendMessage("箱子放置功能已启用。");
            }
            return true;
        });
    }






    //初始化NEO地图
    public void createOrLoadNEOWorld() {
        NEOWorld = Bukkit.getWorld("NEO");
        if ( NEOWorld == null) {
            getLogger().info("显示NEOworld == null");
            // 检查世界文件夹是否存在
            File worldFolder = new File(Bukkit.getWorldContainer(), "NEO");
            if (worldFolder.exists() && worldFolder.isDirectory()) {
                // 如果文件夹存在，尝试强制加载
                getLogger().info("检测到NEO世界文件夹，正在加载...");
                WorldCreator creator = new WorldCreator("NEO");
                //creator.generateStructures(false); // 禁用结构生成
                NEOWorld = creator.createWorld();
            } else {
                // 如果文件夹不存在，创建新世界
                getLogger().info("未找到NEO世界文件夹!!!!!");
            }
        }

        else {
            getLogger().info("NEO已经存在，正在加载......");
        }

        if ( NEOWorld != null) {
            getLogger().info("显示NEOworld != null");
            // 设置世界规则
            NEOWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
            NEOWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            NEOWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            NEOWorld.setTime(12200); // 设置时间为黄昏
            NEOWorld.setDifficulty(Difficulty.PEACEFUL);
        } else {
            getLogger().severe("未能创建或加载NEO");
        }
    }


    public void worldEdit() {
        // 获取WorldEdit插件实例
        WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        if (worldEdit == null) {
            getLogger().severe("WorldEdit插件未找到！");
            return;
        }
        getLogger().info("成功加载WorldEdit插件");

        try {
            // 获取NEO世界和主世界
            World neoWorld = Bukkit.getWorld("NEO");
            World vanillaWorld = Bukkit.getWorlds().get(0);
            World world = Bukkit.getWorld("world");

            if (neoWorld == null) {
                getLogger().severe("NEO世界未找到！");
                return;
            }
            if (vanillaWorld == null) {
                getLogger().severe("主世界未找到！");
                return;
            }
            getLogger().info("成功加载NEO世界和主世界");

            // 将Bukkit World转换为WorldEdit World
            BukkitWorld worldEditNeoWorld = new BukkitWorld(neoWorld);
            BukkitWorld worldEditVanillaWorld = new BukkitWorld(world);
            getLogger().info("成功转换世界为WorldEdit格式");


            int minX = -34, maxX = 34;
            int minZ = -34, maxZ = 34;
            int chunkSize = 200;

            // 分区域处理
            for (int x = minX; x < maxX; x += chunkSize) {
                for (int z = minZ; z < maxZ; z += chunkSize) {
                    // 定义当前小区域
                    //?????TODO
                    BlockVector3 minPoint = BlockVector3.at(x, 50, z);
                    BlockVector3 maxPoint = BlockVector3.at(
                            Math.min(x + chunkSize, maxX),
                            94,
                            Math.min(z + chunkSize, maxZ)
                    );

                    // 创建编辑会话
                    EditSession editSession = WorldEdit.getInstance().getEditSessionFactory()
                            .getEditSession(worldEditNeoWorld, -1);
                    getLogger().info("成功创建NEO世界编辑会话");

                    // 定义复制区域
                    BlockVector3 originPoint = BlockVector3.at(0, 1, 0);
                    CuboidRegion region = new CuboidRegion(minPoint, maxPoint);
                    getLogger().info("定义复制区域：从 " + minPoint + " 到 " + maxPoint);

                    // 创建剪贴板
                    Clipboard clipboard = new BlockArrayClipboard(region);
                    clipboard.setOrigin(originPoint); // 设置原点为最小点
                    getLogger().info("成功创建剪贴板");


                   // 执行复制操作
                   ForwardExtentCopy copy = new ForwardExtentCopy(
                           editSession, region, clipboard, minPoint);
                   copy.setCopyingEntities(true); // 启用实体复制
                   copy.setCopyingBiomes(false);   // 不需要复制生物群系


                   Operations.complete(copy);
                   getLogger().info("成功复制区域到剪贴板");

                    // 粘贴到主世界
                    EditSession pasteSession = WorldEdit.getInstance().getEditSessionFactory()
                            .getEditSession(new BukkitWorld(world), -1);
                    getLogger().info("成功创建主世界编辑会话");

                    // 计算目标位置
                    // BlockVector3 targetPoint = BlockVector3.at(0, 0, 0); // 目标位置
                    // BlockVector3 offset = targetPoint.subtract(minPoint); // 计算偏移量

                    // 修改粘贴操作
                    ClipboardHolder holder = new ClipboardHolder(clipboard);
                    Operation paste = holder.createPaste(pasteSession)
                            .to(originPoint)
                            .ignoreAirBlocks(false)
                            .build();
                    Operations.complete(paste);
                    getLogger().info("成功粘贴区域到主世界");

                    // 强制保存更改
                    pasteSession.flushSession();
                    getLogger().info("已强制保存更改");

                    // 验证粘贴结果
//            BlockVector3 testPoint = targetPoint.add(offset); // 验证点考虑偏移量
//            BlockState neoBlock = worldEditNeoWorld.getBlock(testPoint);
//            BlockState vanillaBlock = worldEditVanillaWorld.getBlock(testPoint);
//            getLogger().info("验证点 " + testPoint + " 的方块：NEO世界=" + neoBlock + ", 主世界=" + vanillaBlock);

                    getLogger().info("地图拼接完成！");
                }}} catch (Exception e) {
            getLogger().severe("地图拼接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 新增方法：加载并粘贴 Schematic 文件
    public void loadAndPasteSchematics() {
        World world = Bukkit.getWorld("world");
        if (world == null) {
            getLogger().severe("world 世界未找到！");
            return;
        }

        try {
            Location loc6 = new Location(world, 0, 75, 0);
            loadAndPasteSchematic(loc6, "main6.6.schem");

            getLogger().info("所有 Schematic 文件已加载并粘贴完成！");
        } catch (Exception e) {
            getLogger().severe("加载并粘贴 Schematic 文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadAndPasteSchematics2() {
        World FinalWorld = Bukkit.getWorld("FinalWorld");
        if (FinalWorld == null) {
            getLogger().severe("FinalWorld 世界未找到！");
            return;
        }

        try {
            Location loc7 = new Location(FinalWorld, 0, 64, 0);
            loadAndPasteSchematic(loc7, "FinalWorld.schem");
            getLogger().info("所有 Schematic 文件已加载并粘贴完成！");
        } catch (Exception e) {
            getLogger().severe("加载并粘贴 Schematic 文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 辅助方法：加载并粘贴单个 Schematic 文件
    private void loadAndPasteSchematic(Location location, String schematicName) throws Exception {
        World world = location.getWorld();


        if (world == null) return;

        // 将 Bukkit World 转换为 WorldEdit World
        BukkitWorld worldEditWorld = new BukkitWorld(world);

        // 创建编辑会话
        EditSession editSession = WorldEdit.getInstance().getEditSessionFactory()
                .getEditSession(worldEditWorld, -1);

//        // 加载 Schematic 文件
//        Clipboard clipboard = ClipboardFormat.SCHEMATIC.load(
//                new File("plugins/WorldEdit/schematics/" + schematicName)
//        );

        // 修改后的加载 Schematic 文件代码
        ClipboardFormat format = ClipboardFormats.findByFile(new File("plugins/WorldEdit/schematics/" + schematicName));
        if (format == null) {
            throw new Exception("无法识别 Schematic 文件格式: " + schematicName);
        }
        Clipboard clipboard = format.getReader(new FileInputStream("plugins/WorldEdit/schematics/" + schematicName)).read();

        // 创建 ClipboardHolder
        ClipboardHolder holder = new ClipboardHolder(clipboard);

        // 计算目标位置
        BlockVector3 targetPoint = BlockVector3.at(
                location.getX(), location.getY(), location.getZ()
        );

        // 执行粘贴操作，忽略空气方块
        Operation paste = holder.createPaste(editSession)
                .to(targetPoint)
                .ignoreAirBlocks(true)
                .build();
        Operations.complete(paste);

        // 强制保存更改
        editSession.flushSession();
    }


    // 新增方法：禁用海洋生成
    private void disableOceanGeneration() {
        World world = Bukkit.getWorld("world");
        if (world != null) {
            // 获取世界生成器设置
            WorldCreator worldCreator = new WorldCreator(world.getName());
            worldCreator.generator(new ChunkGenerator() {
                @Override
                public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
                    ChunkData chunkData = createChunkData(world);

                    // 将所有海洋生物群系替换为平原
                    for (int i = 0; i < 16; i++) {
                        for (int j = 0; j < 16; j++) {
                            Biome currentBiome = biome.getBiome(i, j);
                            if (currentBiome == Biome.OCEAN || currentBiome == Biome.DEEP_OCEAN ||
                                    currentBiome == Biome.FROZEN_OCEAN || currentBiome == Biome.DEEP_FROZEN_OCEAN ||
                                    currentBiome == Biome.WARM_OCEAN || currentBiome == Biome.LUKEWARM_OCEAN ||
                                    currentBiome == Biome.COLD_OCEAN || currentBiome == Biome.DEEP_LUKEWARM_OCEAN ||
                                    currentBiome == Biome.DEEP_COLD_OCEAN ) {
                                biome.setBiome(i, j, Biome.PLAINS);
                            }
                        }
                    }
                    return chunkData;
                }
            });

            // 重新生成世界
            Bukkit.createWorld(worldCreator);
            getLogger().info("已成功禁用海洋生成");
        }
    }






    // 添加新方法：复制FinalWorldCopy区域到FinalWorld
    public void copyFinalWorldRegion() {
        World finalWorldCopy = Bukkit.getWorld("FinalWorldCopy");
        World finalWorld = Bukkit.getWorld("FinalWorld");

        if (finalWorldCopy == null || finalWorld == null) {
            getLogger().severe("FinalWorldCopy或FinalWorld世界未找到！");
            return;
        }

        try {
            // 获取WorldEdit插件实例
            WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
            if (worldEdit == null) {
                getLogger().severe("WorldEdit插件未找到！");
                return;
            }

            // 定义复制区域（分批次处理）
            int chunkSize = 200; // 每次处理50x50的区域
            for (int x = -400; x < 400; x += chunkSize) {
                for (int z = -400; z < 400; z += chunkSize) {
                    BlockVector3 minPoint = BlockVector3.at(x, 0, z);
                    BlockVector3 maxPoint = BlockVector3.at(
                            Math.min(x + chunkSize, 100),
                            255,
                            Math.min(z + chunkSize, 100)
                    );

                    // 创建编辑会话
                    EditSession copySession = WorldEdit.getInstance().getEditSessionFactory()
                            .getEditSession(new BukkitWorld(finalWorldCopy), -1);

                    // 创建剪贴板
                    CuboidRegion region = new CuboidRegion(minPoint, maxPoint);
                    Clipboard clipboard = new BlockArrayClipboard(region);

                    // 执行复制操作
                    ForwardExtentCopy copy = new ForwardExtentCopy(
                            copySession, region, clipboard, minPoint);
                    copy.setCopyingEntities(true);
                    copy.setCopyingBiomes(true);
                    Operations.complete(copy);

                    // 创建粘贴会话
                    EditSession pasteSession = WorldEdit.getInstance().getEditSessionFactory()
                            .getEditSession(new BukkitWorld(finalWorld), -1);

                    // 执行粘贴操作
                    ClipboardHolder holder = new ClipboardHolder(clipboard);
                    Operation paste = holder.createPaste(pasteSession)
                            .to(minPoint)
                            .ignoreAirBlocks(false)
                            .build();
                    Operations.complete(paste);

                    // 强制保存更改
                    pasteSession.flushSession();
                    getLogger().info("已复制区域: " + minPoint + " 到 " + maxPoint);
                }
            }
            getLogger().info("FinalWorldCopy区域复制到FinalWorld成功！");
        } catch (Exception e) {
            getLogger().severe("区域复制失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 添加虚空世界生成器
    private class VoidGenerator extends ChunkGenerator {
        @Override
        public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
            return createChunkData(world);
        }
    }

    public void QuKuaiPreload(){
        // ------------ 尝试自动触发Chunky预加载（请谨慎使用） ------------
        Plugin chunkyPlugin = getServer().getPluginManager().getPlugin("Chunky");
        if (chunkyPlugin != null && chunkyPlugin.isEnabled()) {
            getLogger().info("检测到 Chunky 插件。");

            pregenFlagFile = new File(getDataFolder(), "pregen_done.flag");


                getLogger().info("首次启动或未找到预加载标志，准备尝试使用 Chunky 预加载区域。");
                getLogger().warning("警告：大规模区块预加载将导致服务器启动时间显著延长！");

                // 延迟执行，给服务器一点时间完成基本加载
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        getLogger().info("开始执行Chunky预加载命令...");
                        // 确保你的世界名正确，以及范围参数符合Chunky的用法
                        // 注意：直接执行Chunky start可能会立即开始大量消耗资源
                        // 更好的做法是分批或使用Chunky的API（如果它提供的话）进行更细致的控制
                        // 但这里为了简单，直接用命令
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "chunky world world");      // 设置世界
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "chunky shape square");   // 设置形状
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "chunky center 0 0");    // 设置中心
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "chunky radius 800");      // 设置半径
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "chunky silent"); // 尝试静默模式，减少控制台输出
                        //Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "chunky start");          // 开始预加载

                        // 直接使用confirm命令而不是start
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "chunky confirm");

                        // 轮询Chunky的进度，并在完成后创建标志文件
                        // 这是一个简化的轮询，实际Chunky可能有事件或API来判断完成
                        new BukkitRunnable() {
                            int checks = 0;
                            final int maxChecks = 60 * 20; // 大约每5分钟检查一次，最多检查20小时 (这是一个很大的时间，根据你的区域调整)
                            // Chunky的 /chunky progress 可能不会直接返回一个可以简单解析的状态
                            // chunky task list 可以查看任务
                            // 你可能需要找到一种更可靠的方式来判断Chunky是否完成
                            // 比如，Chunky完成后该区域是否就不会再有新的区块生成了。
                            // 目前直接执行start命令，并不能简单地知道它何时完成。
                            // Chunky 可能有自己的任务管理，这里只是触发了它。

                            // 更简单的做法：执行完start后，就认为“任务已提交给Chunky”，然后创建标志文件。
                            // 缺点是如果Chunky因为某些原因失败了，下次还会尝试。

                            @Override
                            public void run() {
                                // 因为我们无法直接从 `chunky start` 命令得知何时完成，
                                // 所以我们假设一旦命令发出，Chunky就会处理。
                                // 我们在这里只是为了演示“完成后标记”的逻辑，但实际完成的判断很困难。
                                // 最好的做法是，Chunky start之后，就认为已经“尝试”过了，然后标记。
                                // 或者，你需要在Chunky完成后手动创建一个标记文件。

                                // 我们在这里简化：一旦发出 chunky start 命令，就创建标志文件
                                // 这样下次启动就不会再尝试执行这一系列Chunky命令了。
                                try {
                                    if(pregenFlagFile.createNewFile()) {
                                        getLogger().info("Chunky 预加载命令已发送，并创建了 pregen_done.flag 标志文件。请通过 /chunky progress 查看实际进度。");
                                    }
                                } catch (IOException e) {
                                    getLogger().severe("无法创建预加载标志文件: " + e.getMessage());
                                }
                                cancel(); // 取消这个内部的 BukkitRunnable
                            }
                        }.runTaskLater(Main.this, 20L * 10); // 假设10秒后Chunky的命令应该已经被接受和处理

                    }
                }.runTaskLater(this, 20L * 10); // 延迟10秒执行，给其他插件和服务器一点时间



        } else {
            getLogger().warning("未找到 Chunky 插件或未启用，无法进行区块预加载。请安装并启用 Chunky。");
        }
        // ------------ 自动触发Chunky结束 ------------
    }







}
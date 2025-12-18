 //现在开始我要制作UHC的插件了
//倒计时的那些逻辑不变
//两人一队，可以选队伍
//自制神器
//关于自定义结构，还需要多多学习
//配方

package com.shaolian.uhc;

import com.shaolian.uhc.profession.Profession;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.event.Listener;
import org.bukkit.Particle;
import org.bukkit.Color;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;


public class GameManager implements Listener {
    private Main plugin;
    private LobbyWorld lobbyWorld;
    private Pay pay;
    private CustomRecipes customRecipes;
    private CustomPlaceholders customPlaceholders;
    private FeedAuthorPlugin feedAuthorPlugin;
    private Hats hats;
    private Hats2 hats2;
    private AirDrop airDrop;
    private SpecialEnderPearl specialEnderPearl;
    private Border border;


    public GameManager() {
        plugin = Main.getPlugin(Main.class);
        pay = new Pay(plugin);
        customRecipes = new CustomRecipes(plugin);
        customPlaceholders = new CustomPlaceholders(plugin);
        feedAuthorPlugin = new FeedAuthorPlugin(plugin);
        hats = new Hats(plugin);
        hats2 = new Hats2(plugin);
        airDrop = new AirDrop(plugin);
        specialEnderPearl = new SpecialEnderPearl(plugin);
        border = new Border(plugin);


        // 确保lobbyWorld正确初始化
        this.lobbyWorld = new LobbyWorld(plugin);
        //生成或者初始化lobby
        lobbyWorld.createOrLoadLobbyWorld();


        //初始化队伍
        initializeTeams();
        //清空掉落物
        clerarDropItems();

        for(World world : Bukkit.getWorlds()) {
            world.setPVP(false);
        }

        // 启动玩家数量监听
        //monitorPlayerCount();
    }



    public static boolean isGameRunning = false;
    public static boolean isGameFinalRunning = false;
    private boolean isCountdownRunning = false;
    private int countdownTask;
    public static final Set<UUID> readyPlayers = new HashSet<>();
    private Scoreboard scoreboard;
    private Team pinkTeam;
    private Team aquaTeam;
    private Team blackTeam;
    private Team greenTeam;

    public static Map<UUID, String> playerTeams = new HashMap<>(); // 存储玩家ID和队伍名称的映射
    private static final String[] TEAM_NAMES = {"pinkTeam", "aquaTeam", "blackTeam", "greenTeam"};
    private int time = 0;

//    //边界大小
//    private int currentSize = 0;
//    private Map<Player, Long> playersOutsideBorder = new HashMap<>();


    //存储这些玻璃方块
    List<Block> glassBlocks = new java.util.ArrayList<>();

    private Team winningTeam = null; // 添加获胜队伍变量
    //private boolean blockBreakEnabled = false; // 默认不允许破坏方块
    private boolean damageProtectEnabled = true; // 默认启用伤害保护

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if(damageProtectEnabled) {
            // 检查受伤的实体是否是玩家
            if (event.getEntity() instanceof Player) {
                // 取消所有伤害
                event.setCancelled(true);
            }
        }
    }



    // 添加控制伤害保护的方法
    public void setDamageProtectEnabled(boolean enabled) {
        this.damageProtectEnabled = enabled;
        plugin.getLogger().info("玩家受到伤害已" + (enabled ? "启用" : "禁用"));
    }



    public void playerJoin(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        //如果游戏没开始，将玩家加入到准备玩家列表中
        if(!isGameRunning){

            readyPlayers.add(playerId);
            player.setGameMode(GameMode.ADVENTURE);
            player.setMaxHealth(40.0);
            player.setHealth(40);
            player.setFoodLevel(20);

            //玩家传送到大厅
            lobbyWorld.teleportToLobby(player);

            //检查游戏是否需要开始
            checkGameStart();
            player.sendMessage(ChatColor.LIGHT_PURPLE + "欢迎游玩UHC！建议使用1.21.4游戏版本游玩");
            plugin.getLogger().info(playerId + "为uuid");

            //清空背包
            player.getInventory().clear();
            PlayerInventory inv = player.getInventory();
            //inv.setItem(4,feedAuthorPlugin.createFeedAuthorDiamond());
            //inv.setItem(4,pay.createPayItem());
            //给予特殊物品合成图的书
            inv.setItem(0,customRecipes.createRecipeBook());
//            inv.setItem(4,hats.createCustomHats());
            inv.setItem(4,hats2.createGuiOpenerItem());

        }
        //如果游戏已经开始
        else{
            //如果有身份
            if(readyPlayers.contains(playerId)) {
                // 重新应用队伍设置
                replayPlayerToTeam(playerId);
            }
            //如果没身份
            else{
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
    }

    public void playerQuit(UUID playerId) {
        //当游戏没有开始的时候
        if (!isGameRunning) {
            readyPlayers.remove(playerId);
            //检查游戏是否可以开始
            checkGameStart();
        }

        //如果游戏已经开始
        else{
            //如果退出的玩家有身份
            if (readyPlayers.contains(playerId)) {
                scheduleRoleRemoval(playerId, 60); // 1分钟 = 60秒
                Bukkit.broadcastMessage(ChatColor.RED + "玩家离线1分钟后将会被移除身份");
            }
        }
    }

    private void checkGameStart() {
        //如果游戏正在进行，则不执行任何操作
        if (isGameRunning) return;
        //如果玩家数量少于2，并且没有倒计时正在进行，则开始倒计时
        if (readyPlayers.size() >= 2 && !isCountdownRunning) {
            startCountdown();
            //如果玩家数量少于2，并且倒计时正在进行，则取消倒计时
        } else if (readyPlayers.size() < 2 && isCountdownRunning) {
            cancelCountdown();
        }
    }

    public void monitorPlayerCount() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (isGameRunning && readyPlayers.size() <= 1) {
                    String winningTeamName = null;
                    for (UUID playerId : readyPlayers) {
                        winningTeamName = playerTeams.get(playerId);
                        break;
                    }
                    endGame(winningTeamName);
                }
            }
        }.runTaskTimer(plugin, 0L, 5 * 60 * 20L); // 每5min检查一次
    }


    private void startCountdown() {
        //如果游戏开始了，则不执行任何操作
        if (isGameRunning) {
            return;
        }
        //那么以下就是游戏没开始的时候
        //如果有倒计时在运行，则不执行任何操作
        if (isCountdownRunning) {
            return; // 防止多次启动倒计时
        }

        //那么以下就是游戏没有开始并且没有倒计时在运行的时候
        //开始倒计时
        isCountdownRunning = true;

        countdownTask = new BukkitRunnable() {
            int countdown = 260;

            @Override
            public void run() {
                if (countdown <= 0) {
                    this.cancel(); // 取消当前任务

                    Bukkit.getScheduler().cancelTask(countdownTask);

                    isCountdownRunning = false;

                    startGame();

                    return;
                }
                else {
                    String message = ChatColor.AQUA + "游戏将在 " + ChatColor.GOLD + "§l" + countdown + ChatColor.AQUA + " 秒后开始！";
                    //Bukkit.broadcastMessage(ChatColor.YELLOW + "游戏将在 " + countdown + " 秒后开始！");
                    for(Player player : Bukkit.getOnlinePlayers()) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                    }
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }

    //取消倒计时
    private void cancelCountdown() {
        if (isCountdownRunning) {
            Bukkit.getScheduler().cancelTask(countdownTask);
            isCountdownRunning = false;
            Bukkit.broadcastMessage(ChatColor.RED + "倒计时已取消！人数不足！");
        }
    }

    //离线身份移除
    private void scheduleRoleRemoval(UUID playerId, int seconds) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Player player = Bukkit.getPlayer(playerId);
                //一分钟到了，如果玩家不在线
                if (player == null || !player.isOnline()) {
                    readyPlayers.remove(playerId);
                    playerTeams.remove(playerId);
                    checkGameEnd1();
                }
            }
        }.runTaskLater(plugin, seconds * 20L);
    }


    public void  checkGameEnd1() {
        Map<String, Integer> teamCounts = new HashMap<>();
        for (String teamName : TEAM_NAMES) {
            teamCounts.put(teamName, 0);
        }

        for (UUID playerId : readyPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.getGameMode() != GameMode.SPECTATOR) {
                String teamName = playerTeams.get(playerId);
                if (teamName != null) {
                   // teamCounts.put(teamName, teamCounts.get(teamName) + 1);
                    // 使用 getOrDefault 避免 null
                    teamCounts.put(teamName, teamCounts.getOrDefault(teamName, 0) + 1);
                }
            }
        }

        int aliveTeamCount = 0;
        String potentialWinner = null;
        // 遍历 teamCounts 中的每个键值对（队伍名称和对应的存活玩家数量）
        for (Map.Entry<String, Integer> entry : teamCounts.entrySet()) {
            // 如果当前队伍的存活玩家数量大于0
            if (entry.getValue() > 0) {
                // 增加有存活玩家的队伍计数
                aliveTeamCount++;
                // 将当前队伍设置为潜在的获胜队伍
                potentialWinner = entry.getKey();
            }
        }

        // 如果只有一个队伍有存活玩家，那么该队伍获胜
        if (aliveTeamCount == 1) {
            endGame(potentialWinner);
        }
    }



    private void  startGame() {

        new BukkitRunnable() {
            @Override
            public void run() {
                airDrop.spawnAirDrop();
            }
        }.runTaskTimer(plugin, 5 * 60 * 20L,  10 * 60 * 20L);// 5min之后开始掉落,之后空投间隔为10min

        // 添加每秒更新时间的任务
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            CustomPlaceholders.currentTime++;
        }, 20L, 20L);

        setDamageProtectEnabled(false);//禁用伤害保护

        //设置world世界和nether世界的边界
        border.setWorldBorder(1600);
        border.setNetherBorder(800);
        for(Player player : Bukkit.getOnlinePlayers()){
            border.teleportTofinalWorld(player);
        }

        for(World world : Bukkit.getWorlds()) {
           // world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false); // 禁用昼夜交替
            world.setStorm(false); // 关闭暴风雨
            world.setThundering(false); // 关闭雷暴
            world.setWeatherDuration(Integer.MAX_VALUE); // 设置晴朗天气持续时间
            world.setTime(10000); // 设置时间为10000（正午）
        }


        //设置游戏状态
        isGameRunning = true;
        isCountdownRunning = false;

        // 设置游戏规则：禁止自然恢复生命
        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.NATURAL_REGENERATION, false);
            world.setDifficulty(Difficulty.EASY);
        }

        //分配队伍
        assignRoles();

        //给予状态
        giveHealth();

        //传送玩家
        teleportPlayers1();

        //给特殊东西
        for(UUID playerId : readyPlayers) {
            Player player = Bukkit.getPlayer(playerId);
          //  player.setGameMode(GameMode.SURVIVAL);
            PlayerInventory inv = player.getInventory();
            // 保存装备槽位的物品
            ItemStack[] armorContents = inv.getArmorContents();
            inv.clear(); // 清除背包物品
            // 恢复装备槽位的物品
            inv.setArmorContents(armorContents);

            inv.setItem(0,customRecipes.createRecipeBook());
          //  inv.setItem(4,hats.createCustomHats());
            inv.setItem(4,hats2.createGuiOpenerItem());
         //   inv.setItem(2,Profession.createItems());

        }

        //显示倒计时
        showCountdown();

        // 1. 30s后清除玻璃,设置游戏状态为最终游戏模式,开启PVP,启用方块破坏,播放音效
        new BukkitRunnable() {
            @Override
            public void run() {
                clearGlass();
                isGameFinalRunning = true;
                //清除选择帽子的东西，这时候才给予传送珍珠
                for(Player player : Bukkit.getOnlinePlayers()) {
                    player.getInventory().remove(hats.createCustomHats());
                    player.getInventory().remove(hats2.createGuiOpenerItem());
                }


                for (World world : Bukkit.getWorlds()) {
                    world.setPVP(true);
                }
                for (UUID playerId : readyPlayers) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        player.setGameMode(GameMode.SURVIVAL);
                        player.sendTitle(ChatColor.GOLD + "PVP已开启", ChatColor.GOLD + "PVP将在120s后结束", 0, 21, 0);
                    }
                }
                for (UUID playerId : readyPlayers) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                    }
                }
            }
        }.runTaskLater(plugin, 30 * 20L);



        // 3. 30+120秒后关闭PVP并播放音效
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID playerId : readyPlayers) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        player.sendTitle(ChatColor.GOLD + "", ChatColor.GOLD + "PVP已暂时关闭", 10, 41, 10);
                        new BukkitRunnable() {
                            int duration = 0;
                            @Override
                            public void run() {
                                if (duration >= 5) { // 播放5次
                                    this.cancel();
                                    return;
                                }
                                // 逐渐增加音量
                                float volume = 0.5f + (duration * 0.1f);
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, volume, 0.5f);
                                duration++;
                            }
                        }.runTaskTimer(plugin, 0L, 20L);

                        for (World world : Bukkit.getWorlds()) {
                            world.setPVP(false);
                        }

                        player.setHealth(player.getMaxHealth());
                        //player.getInventory().addItem(specialEnderPearl.createSpecialPearl());
                        player.getInventory().addItem(specialEnderPearl.createSpecialPearl()).values()
                                .forEach(item -> player.getWorld().dropItem(player.getLocation(), item));

                        // 持续音效
                        new BukkitRunnable() {
                            int duration = 0;
                            @Override
                            public void run() {
                                if (duration >= 3) {
                                    this.cancel();
                                    return;
                                }
                                player.playSound(
                                        player.getLocation(),
                                        Sound.BLOCK_NOTE_BLOCK_BASS,
                                        1.5f,
                                        0.6f + (duration * 0.1f)
                                );
                                duration++;
                            }
                        }.runTaskTimer(plugin, 10L, 10L);
                    }
                }
            }
        }.runTaskLater(plugin, 150 * 20L);



        //30s+120s+10min
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID playerId : readyPlayers) {
                   Player player = Bukkit.getPlayer(playerId);
                   player.sendTitle(ChatColor.GOLD + "PVP已开启", ChatColor.GOLD + "边界开始收缩", 10, 51, 10);
                   for(World world : Bukkit.getWorlds()) {
                       world.setPVP(true);
                   }
                    player.setHealth(player.getMaxHealth());
                   //TODO 改成末影龙咆哮
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                }

            }
        }.runTaskLater(plugin, 750* 20L);
    }

    private void endGame(String teamName) {
        //所有人变成旁观者,清空背包
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null) continue; // 跳过null玩家
            player.setGameMode(GameMode.SPECTATOR);
            player.getInventory().clear();

            switch (teamName) {
                //粉队赢
                case "pink":
                    player.sendTitle(ChatColor.LIGHT_PURPLE + "粉队获胜", "§7游戏结束", 10, 70, 20);
                    break;
                //
                case "aqua":
                    player.sendTitle(ChatColor.AQUA + "蓝队获胜", "§7游戏结束", 10, 70, 20);
                    break;
                //
                case "black":
                    player.sendTitle(ChatColor.BLACK + "黑队获胜", "§7游戏结束", 10, 70, 20);
                    break;
                //
                case "green":
                    player.sendTitle(ChatColor.GREEN + "绿队获胜", "§7游戏结束", 10, 70, 20);
                    break;
            }

        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("§a游戏结束，30s后重置世界");
            // 播放烟花音效
            new BukkitRunnable() {
                int duration = 0;
                @Override
                public void run() {
                    if (duration >= 7) { // 播放7次
                        this.cancel();
                        return;
                    }
                    // 随机播放不同的烟花音效
                    float pitch = 0.8f + (duration * 0.05f); // 逐渐提高音高
                    player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, pitch);
                    // 添加爆炸音效
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0f, pitch);
                    }, 10L); // 延迟10 ticks播放爆炸音效
                    // 添加火花音效
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.8f, pitch + 0.2f);
                    }, 15L);
                    duration++;
                }
            }.runTaskTimer(plugin, 0L, 20L); // 每隔20 ticks（1秒）播放一次
            // 延迟一小段时间后播放烟花爆炸音效
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.0f);
            }, 2L);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                //设置游戏结束
                clearAllRoleAndTeam();
                kickAllPlayers();

         //       deleteAllWorldFolders();
                deleteAllWorldFoldersExceptDatapacks();
                Bukkit.shutdown();


            }
        }.runTaskLater(plugin,30 * 20L);
    }

    public void clearAllRoleAndTeam(){
        isGameRunning = false;
        //清除猎人和逃亡者
        playerTeams.clear();
        readyPlayers.clear();

        // 清除队伍
        if (pinkTeam != null) {
            pinkTeam.unregister();
        }
        if (aquaTeam != null) {
            aquaTeam.unregister();
        }
        if (blackTeam != null) {
            blackTeam.unregister();
        }
        if (greenTeam != null) {
            greenTeam.unregister();
        }
    }


    public void kickAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer("§c服务器正在重置，请稍后重新加入。");
        }
        // 对所有在线玩家执行踢出操作
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.kickPlayer(ChatColor.GREEN + "游戏已结束！服务器已重启！");
        });
        // 广播服务器锁定消息
        plugin.getLogger().info("已踢出所有玩家。");

    }



    public void deleteAllWorldFoldersExceptDatapacks() {
        // 获取服务器根目录
        File serverDirectory = Bukkit.getWorldContainer();

        // 定义要删除的世界名称
        String[] worldNames = {"world", "world_nether", "world_the_end", "FinalWorld"};

        for (String worldName : worldNames) {
            // 卸载世界
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                Bukkit.unloadWorld(world, false);
            }

            // 删除世界文件夹
            File worldFolder = new File(serverDirectory, worldName);
            if (worldFolder.exists()) {
                try {
                    // 如果是主世界，保留datapacks文件夹
                    if (worldName.equals("world")) {
                        Files.walk(worldFolder.toPath())
                                .sorted(Comparator.reverseOrder())
                                .filter(path -> !path.startsWith(worldFolder.toPath().resolve("datapacks")))
                                .map(Path::toFile)
                                .forEach(file -> {
                                    if (!file.delete()) {
                                        Bukkit.getLogger().warning("无法删除文件: " + file.getAbsolutePath());
                                    }
                                });
                    } else {
                        // 对于其他世界，删除所有内容
                        Files.walk(worldFolder.toPath())
                                .sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(file -> {
                                    if (!file.delete()) {
                                        Bukkit.getLogger().warning("无法删除文件: " + file.getAbsolutePath());
                                    }
                                });
                    }
                } catch (IOException e) {
                    Bukkit.getLogger().warning("删除世界文件夹时出错: " + worldName);
                    e.printStackTrace();
                }
            }
            // 确保删除 level.dat 和 level.dat_old 文件
//            File levelDat = new File(serverDirectory, worldName + "/level.dat");
//            File levelDatOld = new File(serverDirectory, worldName + "/level.dat_old");
//            levelDat.delete();
//            levelDatOld.delete();
            // 确保删除相关数据文件
            String[] dataFiles = {"scoreboard.dat", "random_sequences.dat", "raids.dat"};
            for (String dataFile : dataFiles) {
                File file = new File(serverDirectory, "world/data/" + dataFile);
                if (file.exists() && !file.delete()) {
                    Bukkit.getLogger().warning("无法删除数据文件: " + file.getAbsolutePath());
                }
            }
        }}


    public void deleteAllWorldFolders() {
        // 获取服务器根目录
        File serverDirectory = Bukkit.getWorldContainer();

        // 定义要删除的世界名称
        String[] worldNames = {"world", "world_nether", "world_the_end", "FinalWorld"};

        for (String worldName : worldNames) {
            // 卸载世界
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                Bukkit.unloadWorld(world, false);
            }

            // 删除世界文件夹
            File worldFolder = new File(serverDirectory, worldName);
            if (worldFolder.exists()) {
                try {
                    Files.walk(worldFolder.toPath())
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                    Bukkit.getLogger().info("成功删除世界文件夹: " + worldName);
                } catch (IOException e) {
                    Bukkit.getLogger().warning("删除世界文件夹时出错: " + worldName);
                    e.printStackTrace();
                }
            }
        }
    }


    public void initializeTeams() {
        //manager为计分板管理器，也是就是一些前缀呀颜色呀啥的
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        scoreboard = manager.getNewScoreboard();

        pinkTeam = scoreboard.registerNewTeam("pink");
        pinkTeam.setColor(ChatColor.LIGHT_PURPLE );
        pinkTeam.setPrefix(ChatColor.LIGHT_PURPLE  + "[粉队] ");
        pinkTeam.setAllowFriendlyFire(false); // 禁止队友互相伤害

        aquaTeam = scoreboard.registerNewTeam("aqua");
        aquaTeam.setColor(ChatColor.AQUA);
        aquaTeam.setPrefix(ChatColor.AQUA + "[蓝队] ");
        aquaTeam.setAllowFriendlyFire(false); // 禁止队友互相伤害

        blackTeam = scoreboard.registerNewTeam("black");
        blackTeam.setColor(ChatColor.BLACK);
        blackTeam.setPrefix(ChatColor.BLACK + "[黑队] ");
        blackTeam.setAllowFriendlyFire(false); // 禁止队友互相伤害

        greenTeam = scoreboard.registerNewTeam("green");
        greenTeam.setColor(ChatColor.GREEN);
        greenTeam.setPrefix(ChatColor.GREEN + "[绿队] ");
        greenTeam.setAllowFriendlyFire(false); // 禁止队友互相伤害
    }

    public void assignRoles() {
        // 分配角色前先清空之前的角色分配
        playerTeams.clear(); // 清空玩家队伍映射

        // 将 readyPlayers 转换为 List 以便打乱顺序
        List<Player> shuffledPlayers = readyPlayers.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Collections.shuffle(shuffledPlayers); // 随机打乱玩家顺序

        int index = 0;
        for (Player player : shuffledPlayers) {
            UUID playerId = player.getUniqueId();
                // 给玩家分配角色
                switch (index % 4) {
                    case 0:
                        playerTeams.put(playerId, "pink");
                        pinkTeam.addEntry(player.getName());
                        player.setScoreboard(scoreboard); // 将计分板应用到玩家
                        setPlayerGroup(player, "pink");
                        player.setCanPickupItems(true); // 允许玩家拾取物品
                        index++;
                        break;
                    case 1:
                        playerTeams.put(playerId, "aqua");
                        aquaTeam.addEntry(player.getName());
                        player.setScoreboard(scoreboard); // 将计分板应用到玩家
                        setPlayerGroup(player, "aqua");
                        player.setCanPickupItems(true); // 允许玩家拾取物品
                        index++;
                        break;
                    case 2:
                        playerTeams.put(playerId, "black");
                        blackTeam.addEntry(player.getName());
                        player.setScoreboard(scoreboard); // 将计分板应用到玩家
                        setPlayerGroup(player, "black");
                        player.setCanPickupItems(true); // 允许玩家拾取物品
                        index++;
                        break;
                    case 3:
                        playerTeams.put(playerId, "green");
                        greenTeam.addEntry(player.getName());
                        player.setScoreboard(scoreboard); // 将计分板应用到玩家
                        setPlayerGroup(player, "green");
                        player.setCanPickupItems(true); // 允许玩家拾取物品
                        index++;
                        break;
            }
        }
    }


    // 新增方法：为玩家设置分组
    public void setPlayerGroup(Player player, String group) {
        if (player == null || group == null) return;

        // 使用异步任务来设置玩家组
        new BukkitRunnable() {
            @Override
            public void run() {
                TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
                if (tabPlayer != null) {
                    try {
                        tabPlayer.setTemporaryGroup(group);
                    } catch (Exception e) {
                        plugin.getLogger().warning("设置玩家组时出错: " + e.getMessage());
                    }
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void replayPlayerToTeam(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (playerTeams.containsKey(playerId)) {
            String teamName = playerTeams.get(playerId);
            teamAndTeamName(teamName, playerId);
            player.setScoreboard(scoreboard);
        }

    }

    public void assignTeam() {
        // 将 readyPlayers 转换为 List 以便打乱顺序
        List<UUID> shuffledPlayers = new ArrayList<>(readyPlayers);
        Collections.shuffle(shuffledPlayers); // 随机打乱玩家顺序


        int index = 0;
        Team[] teams = {pinkTeam, aquaTeam, blackTeam, greenTeam};
        for (UUID playerId : shuffledPlayers) {
            Team currentTeam = teams[index % 4];
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                currentTeam.addEntry(player.getName());
                //计分板未正确应用：即使玩家被添加到队伍中，如果没有将计分板应用到玩家，玩家也不会显示队伍前缀。
                //需要在分配队伍后，将计分板应用到玩家
                player.setScoreboard(scoreboard); // 将计分板应用到玩家
                player.setCanPickupItems(true); // 允许玩家拾取物品
            }
            index++;
        }
    }

    public void printTeamPlayerCounts() {
        plugin.getLogger().info("粉队玩家数量: ");
        plugin.getLogger().info("蓝队玩家数量: ");
        plugin.getLogger().info("黑队玩家数量: ");
        plugin.getLogger().info("绿队玩家数量: ");
    }

    public void teamAndTeamName(String teamName, UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (teamName.equals("pink")) {
            pinkTeam.addEntry(player.getName());
            setPlayerGroup(player, "pink");
        }
        if (teamName.equals("aqua")) {
            aquaTeam.addEntry(player.getName());
            setPlayerGroup(player, "aqua");
        }
        if (teamName.equals("black")) {
            blackTeam.addEntry(player.getName());
            setPlayerGroup(player, "black");
        }
        if (teamName.equals("green")) {
            greenTeam.addEntry(player.getName());
            setPlayerGroup(player, "green");
        }
    }


    private void clerarDropItems() {
        World world = Bukkit.getWorld("world");
        if (world != null) {
            // 获取所有实体
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                // 如果是掉落物
                if (entity instanceof org.bukkit.entity.Item) {
                    entity.remove(); // 移除掉落物
                }
            }
        }
    }



    private void teleportPlayers1() {
        World world = Bukkit.getWorld("world");
        Location locationPink = new Location(world, 15, 76, 15);
        Location locationAqua = new Location(world, -15, 76, -15);
        Location locationBlack = new Location(world, 15, 76, -15);
        Location locationGreen = new Location(world, -15, 76, 15);
        for (UUID playerId : readyPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            Team playerTeam = scoreboard.getEntryTeam(player.getName());
            if (playerTeam != null) {
            if (playerTeam.equals(pinkTeam)) {
                player.teleport(locationPink);
            } else if (playerTeam.equals(aquaTeam)) {
                player.teleport(locationAqua);
            } else if (playerTeam.equals(blackTeam)) {
                player.teleport(locationBlack);
            } else if (playerTeam.equals(greenTeam)) {
                player.teleport(locationGreen);
            }
         }
      }
    }

    //生成玻璃
    private void generateGlass() {

    }

    //清除玻璃
    private void clearGlass() {
       World world = Bukkit.getWorld("world");
       int x1;
       int x2;
       int z1;
       int z2;
       for(x1 = 13; x1 <= 17; x1++) {
           for (z1 = 13; z1 <= 17; z1++) {
               for (int y = 75; y <= 79; y++) {
                   Block block = world.getBlockAt(x1, y, z1);
                   block.setType(Material.AIR);
               }
           }
       }
        for(x1 = -17; x1 <= -13; x1++) {
            for (z1 = -17; z1 <= -13; z1++) {
                for (int y = 75; y <= 79; y++) {
                    Block block = world.getBlockAt(x1, y, z1);
                    block.setType(Material.AIR);
                }
            }
        }
        for(x1 = 13; x1 <= 17; x1++) {
            for (z1 = -17; z1 <= -13; z1++) {
                for (int y = 75; y <= 79; y++) {
                    Block block = world.getBlockAt(x1, y, z1);
                    block.setType(Material.AIR);
                }
            }
        }
        for(x1 = -17; x1 <= -13; x1++) {
            for (z1 = 13; z1 <= 17; z1++) {
                for (int y = 75; y <= 79; y++) {
                    Block block = world.getBlockAt(x1, y, z1);
                    block.setType(Material.AIR);
                }
            }
        }


    }

    //显示倒计时
    private void showCountdown() {
        // 添加15秒倒计时显示
        new BukkitRunnable() {
            int countdown = 30;
            @Override
            public void run() {
                if (countdown <= 0) {
                    // 倒计时结束时播放低音
                    for (UUID playerId : readyPlayers) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                        }
                    }
                    this.cancel();
                } else {
                    for (UUID playerId : readyPlayers) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null) {
                            player.sendTitle(
                                    ChatColor.GREEN + String.valueOf(countdown),
                                    ChatColor.GOLD + "",
                                    0, 21, 0);
                             if(countdown <= 15){
                                 // 播放音符盒音效
                                 float pitch = 1.0f + (countdown * 0.05f); // 音调逐渐升高
                                 player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
                             }
                        }
                    }
                    countdown--;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void giveHealth() {
        for (UUID playerId : readyPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            player.setGameMode(GameMode.ADVENTURE);
            player.setMaxHealth(40.0);
            player.setHealth(40);
            player.setFoodLevel(20);
            player.setFireTicks(0);
            player.setFallDistance(0);


            // 给予吸收状态效果，持续 8 分钟（120 秒），等级为 3（对应 8 点吸收效果）
            int duration = 8 * 60 * 20;
            int amplifier = 3;
            PotionEffect absorptionEffect = new PotionEffect(PotionEffectType.ABSORPTION, duration, amplifier);
            player.addPotionEffect(absorptionEffect);

            // 添加十分钟不饥饿效果
            PotionEffect saturationEffect = new PotionEffect(PotionEffectType.SATURATION, (600+120+30) * 20, 0);
            player.addPotionEffect(saturationEffect);
            // 添加十分钟后恢复正常饥饿效果的定时器
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.setFoodLevel(20); // 重置饱食度为正常值
                }
            }.runTaskLater(plugin, 10 * 60 * 20L); // 10分钟后执行
            // 添加十分钟抗火效果
            PotionEffect fireResistanceEffect = new PotionEffect(PotionEffectType.FIRE_RESISTANCE, (600+120+30) * 20, 0);
            player.addPotionEffect(fireResistanceEffect);
            // 添加100分钟夜视效果
            PotionEffect nightVsionEffect = new PotionEffect(PotionEffectType.NIGHT_VISION, 200* 60 * 20, 0);
            player.addPotionEffect(nightVsionEffect);
        }
    }





















}

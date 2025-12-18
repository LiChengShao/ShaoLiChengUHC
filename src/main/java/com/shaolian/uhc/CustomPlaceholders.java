package com.shaolian.uhc;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class CustomPlaceholders extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private long pvpEndTime = -1; // PVP 结束时间（时间戳）
    private long pvpStartTime = -1; // PVP 开始时间（时间戳）
    private long finalBattleTime = 30+120+600+2400; // 最终决赛时间
    public static long currentTime = 0;

    public CustomPlaceholders(JavaPlugin plugin) {
        this.plugin = plugin;

    }

    @Override
    public String getIdentifier() {
        return "custom";
    }

    @Override
    public String getAuthor() {
        return "YourName";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        // 处理自定义占位符
        switch (identifier) {
            case "x":
                return String.valueOf(player.getLocation().getBlockX());
            case "y":
                return String.valueOf(player.getLocation().getBlockY());
            case "z":
                return String.valueOf(player.getLocation().getBlockZ());
            case "coords":
                return  player.getLocation().getBlockX() + ", " +
                        player.getLocation().getBlockY() + ", " +
                        player.getLocation().getBlockZ();
            case "kongtou":
                return "X:" + AirDrop.kongtouX + " Z:" + AirDrop.kongtouZ;
            case "border":
                if (Border.currentSize == 0) {
                    return "±800";
                }
                return "±" + (Border.overWorldSize/2);
            case "pvp_countdown":
                return getPvpCountdown();
            case "finalBattle_countdown":
                return getFinalBattleCountdown();
            case "time":
                return getCurrentDateTime();
            case "kills":
                return getKills(player);
        }
        return null;
    }

    private String getPvpCountdown() {
        // PVP 开始前
        if (pvpStartTime == -1) {
            pvpEndTime = 150; // 105(15+90) 30+120秒后结束PVP
            pvpStartTime =  150 + 600; // 10分钟后开启PVP
        }

        //游戏开始前
        if (currentTime == 0) {
            return "等待中...";
        }else{
            // PVP 进行中倒计时
            if (currentTime < pvpEndTime) {
                long remaining = (pvpEndTime - currentTime);
                return "PVP结束剩余时间: " + remaining + "秒";
            }
            if (pvpEndTime < currentTime && currentTime < pvpStartTime){
                long remaining = (pvpStartTime - currentTime);
                if(remaining > 60){
                    return "PVP开启剩余时间: " + (remaining / 60) + "分钟";
                }else{
                    return "PVP开启剩余时间: " + remaining + "秒";
                }
            }
        }
        // PVP 永久开启
        return "PVP已永久开启";
    }

    private String getFinalBattleCountdown() {
        //游戏开始前
        if (currentTime <= 0) {
            return " ";
        }else{
            // 游戏已开始
            if(currentTime < finalBattleTime){
                long remaining = (finalBattleTime - currentTime);
                if(remaining > 60){
                    return "决赛剩余时间: " + (remaining / 60) + "分钟";
                }else{
                    return "决赛剩余时间: " + remaining + "秒";
                }
            }
        }
        // PVP 永久开启
        return "决赛时刻";
    }

    /**
     * 获取当前的年月日小时和分钟
     *
     * @return 格式化后的日期时间字符串，格式为 "yyyy-MM-dd HH:mm"
     */
    public String getCurrentDateTime() {
        // 获取当前日期时间
        LocalDateTime now = LocalDateTime.now();
        // 定义日期时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        // 格式化日期时间并返回
        return now.format(formatter);
    }

    private String getKills(Player player) {
        // 获取玩家的击杀数
        UUID playerId = player.getUniqueId();
        int kills = GameListener.playerKills.getOrDefault(playerId, 0);
        return String.valueOf(kills);
    }







}

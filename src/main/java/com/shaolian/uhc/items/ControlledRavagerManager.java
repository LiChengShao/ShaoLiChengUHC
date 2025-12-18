package com.shaolian.uhc.items;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.entity.EntityDismountEvent; // Spigot API for dismount

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class ControlledRavagerManager implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey ravagerSummonerKey;
    private final NamespacedKey controlledRavagerTag;

    // 存储玩家与他们控制的掠夺兽的映射 (玩家UUID -> 掠夺兽UUID)
    private final Map<UUID, UUID> playerControlledRavagers = new HashMap<>();

    public ControlledRavagerManager(JavaPlugin plugin) {
        this.plugin = plugin;
        // 为特殊物品和受控掠夺兽定义命名空间键
        this.ravagerSummonerKey = new NamespacedKey(plugin, "ravager_summoner_item_key");
        this.controlledRavagerTag = new NamespacedKey(plugin, "controlled_ravager_tag");

        initialize();
    }

    /**
     * 初始化管理器，注册事件。
     * 应在插件的 onEnable 方法中调用。
     */
    public void initialize() {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("ControlledRavagerManager initialized and listeners registered.");
    }

    /**
     * 关闭管理器，清理资源。
     * 应在插件的 onDisable 方法中调用。
     */
    public void shutdown() {
        removeAllControlledRavagers();
        plugin.getLogger().info("ControlledRavagerManager shut down and ravagers removed.");
    }

    /**
     * 创建用于召唤和控制掠夺兽的特殊物品。
     * @return 特殊物品 ItemStack
     */
    public ItemStack createRavagerSummonerItem() {
        ItemStack item = new ItemStack(Material.SADDLE); // 使用鞍作为基础物品
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c可控掠夺兽之鞍");
            meta.setLore(Arrays.asList(
                    "§7手持此鞍右键地面,",
                    "§7即可召唤并驾驭一头掠夺兽！",
                    "§7Shift键下坐骑。"
            ));
            meta.addEnchant(Enchantment.LURE, 1, false); // 添加一个无实际效果的附魔使其看起来特殊
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // 添加自定义NBT标签，用于识别这个特殊物品
            meta.getPersistentDataContainer().set(ravagerSummonerKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR || item.getItemMeta() == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        // 检查是否是我们的特殊物品 (通过NBT标签)
        if (container.has(ravagerSummonerKey, PersistentDataType.BYTE)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true); // 取消物品的默认右键行为

                // 检查玩家是否已经有控制的掠夺兽
                if (playerControlledRavagers.containsKey(player.getUniqueId())) {
                    Ravager existingRavager = getControlledRavagerByPlayer(player.getUniqueId());
                    if (existingRavager != null && existingRavager.isValid()) {
                        player.sendMessage("§c你已经有一只掠夺兽了！请先通过Shift键让它消失。");
                        return;
                    } else {
                        // 之前的掠夺兽可能无效了，清理记录
                        playerControlledRavagers.remove(player.getUniqueId());
                    }
                }

                // 确定召唤位置
                Location spawnLocation = player.getLocation();
                if (event.getClickedBlock() != null) {
                    spawnLocation = event.getClickedBlock().getLocation().add(0.5, 1.0, 0.5);
                    // 简单检查上方是否有足够空间
                    if (!spawnLocation.clone().add(0,1,0).getBlock().getType().isAir() ||
                            !spawnLocation.clone().add(0,2,0).getBlock().getType().isAir()) {
                        spawnLocation = player.getLocation().add(player.getLocation().getDirection().multiply(1.5)).add(0,0.5,0);
                        if (spawnLocation.getBlock().getType().isSolid()){ // 若前方还是不行，则尝试在玩家脚边
                            spawnLocation = player.getLocation().add(0,0.1,0);
                        }
                    }
                } else { // 右键空气
                    spawnLocation = player.getLocation().add(player.getLocation().getDirection().multiply(1.5)).add(0,0.5,0);
                    if (spawnLocation.getBlock().getType().isSolid()){
                        spawnLocation = player.getLocation().add(0,0.1,0);
                    }
                }

                Ravager ravager = (Ravager) player.getWorld().spawnEntity(spawnLocation, EntityType.RAVAGER);
                configureRavager(ravager, player);

                // 让玩家骑上掠夺兽
                ravager.addPassenger(player);

                // 记录这只掠夺兽
                addControlledRavager(player, ravager);
                player.sendMessage("§a掠夺兽已召唤！W/A/S/D 控制移动，Shift下坐骑。");
            }
        }
    }

    private void configureRavager(Ravager ravager, Player owner) {
        // --- 重写AI ---
        ravager.setAI(false); // 禁用原生AI。这通常足以让实体停止自主行动，同时允许玩家控制。
        // 如果 setAI(false) 导致玩家无法控制，可以尝试更细致的方法：
        // if (ravager.getPathfinder() != null) {
        //     ravager.getPathfinder().stopPathfinding();
        //     ravager.getPathfinder().removeAllGoals(); // 清除所有寻路目标
        // }

        ravager.setAware(false); // 使其不感知周围环境 (攻击、跟随等)
        ravager.setSilent(true); // 使其安静
        ravager.setPersistent(false); // 不要在区块卸载时保存，防止服务器意外关闭后残留
        ravager.setRemoveWhenFarAway(true); // 当玩家远离时自动移除 (如果玩家下线或坐骑丢失) - 注意与下面Invulnerable的配合

        ravager.setInvulnerable(true); // 使其无敌，避免意外死亡
        // ravager.setCollidable(false); // 可以考虑是否让其可碰撞

        // 标记为受控掠夺兽
        ravager.getPersistentDataContainer().set(controlledRavagerTag, PersistentDataType.BYTE, (byte) 1);

        // 设置名称 (可选)
        ravager.setCustomName("§a" + owner.getName() + "的掠夺兽");
        ravager.setCustomNameVisible(false); // 是否显示名称

        // 掠夺兽默认可以跨过1格方块，可以通过属性确认或设置
        AttributeInstance stepHeight = ravager.getAttribute(Attribute.STEP_HEIGHT);
        if (stepHeight != null) {
            stepHeight.setBaseValue(1.0); // 掠夺兽默认为1.0
        } else {
            plugin.getLogger().warning("Could not access step height attribute for Ravager.");
        }

        // 确保掠夺兽面向玩家召唤时的方向
        ravager.setRotation(owner.getLocation().getYaw(), 0); // Pitch设为0，使其水平

        // 防止加入袭击
        ravager.setCanJoinRaid(false);
    }

    // Spigot API 事件：玩家下坐骑
    @EventHandler
    public void onEntityDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Entity dismountedEntity = event.getDismounted();

            if (dismountedEntity instanceof Ravager) {
                Ravager ravager = (Ravager) dismountedEntity;
                if (isControlledRavager(ravager) && playerControlledRavagers.get(player.getUniqueId()) != null &&
                        playerControlledRavagers.get(player.getUniqueId()).equals(ravager.getUniqueId())) {
                    // 玩家从他们控制的掠夺兽上下来了
                    removeRavagerAndMapping(player.getUniqueId(), ravager.getUniqueId());
                    player.sendMessage("§e掠夺兽已消失。");
                }
            }
        }
    }

    // 玩家退出游戏事件
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID ravagerUUID = playerControlledRavagers.get(player.getUniqueId());
        if (ravagerUUID != null) {
            removeRavagerAndMapping(player.getUniqueId(), ravagerUUID);
        }
    }

    // 掠夺兽死亡事件 (例如被指令杀死或意外死亡)
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Ravager) {
            Ravager ravager = (Ravager) event.getEntity();
            if (isControlledRavager(ravager)) {
                event.getDrops().clear(); // 不掉落任何东西
                event.setDroppedExp(0);   // 不掉落经验

                // 找到控制它的玩家并清除映射
                UUID ownerUUID = null;
                for (Map.Entry<UUID, UUID> entry : playerControlledRavagers.entrySet()) {
                    if (entry.getValue().equals(ravager.getUniqueId())) {
                        ownerUUID = entry.getKey();
                        break;
                    }
                }
                if (ownerUUID != null) {
                    playerControlledRavagers.remove(ownerUUID);
                    Player owner = Bukkit.getPlayer(ownerUUID);
                    if(owner != null && owner.isOnline()){
                        owner.sendMessage("§c你的掠夺兽已死亡。");
                    }
                }
            }
        }
    }

    // 防止受控掠夺兽受到伤害 (如果未设置为无敌)
    @EventHandler
    public void onRavagerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Ravager) {
            Ravager ravager = (Ravager) event.getEntity();
            if (isControlledRavager(ravager) && !ravager.isInvulnerable()) { // 仅当它不是无敌时才取消伤害
                event.setCancelled(true);
            }
        }
    }

    // --- Helper Methods ---

    private void addControlledRavager(Player player, Ravager ravager) {
        playerControlledRavagers.put(player.getUniqueId(), ravager.getUniqueId());
    }

    private void removeRavagerAndMapping(UUID playerUUID, UUID ravagerUUID) {
        Entity entity = Bukkit.getEntity(ravagerUUID);
        if (entity instanceof Ravager && isControlledRavager((Ravager) entity)) {
            entity.remove(); // 从世界中移除掠夺兽
        }
        playerControlledRavagers.remove(playerUUID); // 从映射中移除
    }

    public Ravager getControlledRavagerByPlayer(UUID playerUUID) {
        UUID ravagerUUID = playerControlledRavagers.get(playerUUID);
        if (ravagerUUID != null) {
            Entity entity = Bukkit.getEntity(ravagerUUID);
            if (entity instanceof Ravager && isControlledRavager((Ravager) entity)) {
                return (Ravager) entity;
            } else if (entity == null || !entity.isValid()){
                // 掠夺兽实体已失效，清理引用
                playerControlledRavagers.remove(playerUUID);
            }
        }
        return null;
    }

    private boolean isControlledRavager(Ravager ravager) {
        return ravager != null && ravager.getPersistentDataContainer().has(controlledRavagerTag, PersistentDataType.BYTE);
    }

    private void removeAllControlledRavagers() {
        if (playerControlledRavagers.isEmpty()) return;

        plugin.getLogger().info("Removing all controlled ravagers...");
        // 创建副本以避免 ConcurrentModificationException
        new HashMap<>(playerControlledRavagers).forEach((playerUUID, ravagerUUID) -> {
            Entity entity = Bukkit.getEntity(ravagerUUID);
            if (entity instanceof Ravager) { // 不需要检查 isControlledRavager，因为map里存的就是
                entity.remove();
            }
        });
        playerControlledRavagers.clear();
        plugin.getLogger().info("All controlled ravagers removed.");
    }
}

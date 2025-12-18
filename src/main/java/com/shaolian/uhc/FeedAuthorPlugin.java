package com.shaolian.uhc;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.map.MapView;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FeedAuthorPlugin implements Listener {

    private Main plugin;

   public FeedAuthorPlugin(Main plugin) {
       this.plugin = plugin;
       Bukkit.getPluginManager().registerEvents(this, plugin);
   }

    public static String getRainbowText(String text) {
        StringBuilder rainbowText = new StringBuilder();
        String[] colors = {"§c", "§6", "§e", "§a", "§b", "§9", "§d"}; // 红橙黄绿青蓝紫
        int colorIndex = 0;

        for (char c : text.toCharArray()) {
            rainbowText.append(colors[colorIndex]).append(c);
            colorIndex = (colorIndex + 1) % colors.length; // 循环颜色
        }

        return rainbowText.toString();
    }

    // 创建“投喂作者”的钻石物品
    public ItemStack createFeedAuthorDiamond() {
        ItemStack diamond = new ItemStack(Material.APPLE);
        ItemMeta meta = diamond.getItemMeta();
        String message = getRainbowText("投喂作者");
        meta.setDisplayName(message);
       // meta.setDisplayName("§6投喂作者");
        // 增加 Lore
        List<String> lore = new ArrayList<>();
        lore.add("§c这个UHC插件是作者一个人肝出来的");
        lore.add("§6没有任何报酬");
        lore.add("§e不过最大的报酬就是做好了跟你们一起玩的那些时光");
        lore.add("§2如果你愿意给我投喂");
        lore.add("§a那将是我最大的快乐和荣誉！");
        lore.add("§1投喂的时候可以添加备注一下自己的游戏名");
        lore.add("§5我会为每一位捐款的玩家记下他的每一笔捐赠");
        String message2 = getRainbowText("超级无敌感谢！！！");
        lore.add(message2);
        //lore.add("§7超级无敌感谢！！！");
        meta.setLore(lore);
        meta.setCustomModelData(520);
        diamond.setItemMeta(meta);
        return diamond;
    }

    // 新增：监听玩家食用物品事件
    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item != null && item.isSimilar(createFeedAuthorDiamond())) {
            event.setCancelled(true); // 取消食用
        }
    }

    // 监听玩家右键点击事件
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 检查是否手持“投喂作者”的APLLE
        if (item != null && item.isSimilar(createFeedAuthorDiamond())) {
            event.setCancelled(true); // 取消事件
            openQRCodeImage(player); // 打开图片
        }
    }

    // 打开图片（收款码）
    private void openQRCodeImage(Player player) {
        try {
            // 加载图片
            File imageFile = new File(plugin.getDataFolder(), "qrcode.png");
            BufferedImage image = ImageIO.read(imageFile);

            // 创建地图
            MapView mapView = Bukkit.createMap(player.getWorld());
            mapView.getRenderers().clear(); // 清除默认渲染器

            // 添加自定义渲染器
            mapView.addRenderer(new MapRenderer() {
                @Override
                public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
                    mapCanvas.drawImage(0, 0, MapPalette.resizeImage(image));
                }
            });

            // 发送地图给玩家
            ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
            ItemMeta mapMeta = mapItem.getItemMeta();
            if (mapMeta instanceof org.bukkit.inventory.meta.MapMeta) {
                ((org.bukkit.inventory.meta.MapMeta) mapMeta).setMapId(mapView.getId());
                mapItem.setItemMeta(mapMeta);
            }
            player.getInventory().addItem(mapItem);
            player.sendMessage("§a感谢您的支持！请查看地图中的收款码。");


        } catch (Exception e) {
            player.sendMessage("§c无法加载图片，请联系管理员。");
            e.printStackTrace();
        }
    }

    //玩家进入
//    @EventHandler
//    public void onPlayerJoin(PlayerJoinEvent event) {
//        Player player = event.getPlayer();
//        PlayerInventory inv = player.getInventory();
//        inv.setItem(4,createFeedAuthorDiamond());
//    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        ItemMeta meta = item.getItemMeta();
        Material type = item.getType();
        if(type == Material.APPLE){
            // 检查物品是否为特殊物品
            if (meta.hasCustomModelData() && meta.getCustomModelData() == 520) {
                    event.setCancelled(true); // 取消丢弃
                    // event.getPlayer().sendMessage("§c无法丢弃特殊物品！");
                }
            }
        }



   }





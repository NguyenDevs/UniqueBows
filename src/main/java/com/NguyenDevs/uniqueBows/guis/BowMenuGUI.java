package com.NguyenDevs.uniqueBows.guis;

import com.NguyenDevs.uniqueBows.UniqueBows;
import com.NguyenDevs.uniqueBows.models.CustomBow;
import com.NguyenDevs.uniqueBows.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BowMenuGUI implements Listener {

    private final UniqueBows plugin;
    private final boolean adminMode;
    private static boolean REGISTERED = false; // <-- guard
    private static final int[] BORDER_SLOTS = {0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,37,38,39,40,41,42,43,44};

    private static class BowMenuHolder implements InventoryHolder {
        final boolean admin;
        BowMenuHolder(boolean admin) { this.admin = admin; }
        @Override public Inventory getInventory() { return null; } // không dùng
    }

    public BowMenuGUI(UniqueBows plugin, boolean adminMode) {
        this.plugin = plugin;
        this.adminMode = adminMode;

        if (!REGISTERED) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            REGISTERED = true;
        }
    }

    public void openMenu(Player player) {
        String title = adminMode
                ? ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("admin-menu-title"))
                : ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("menu-title"));

        Inventory inventory = Bukkit.createInventory(new BowMenuHolder(adminMode), 45, title);

        ItemStack borderItem = createBorderItem();
        for (int slot : BORDER_SLOTS) inventory.setItem(slot, borderItem);

        List<CustomBow> bows = new ArrayList<>(plugin.getBowManager().getAllBows());
        bows.sort(Comparator.comparing(CustomBow::getId));

        int slot = 10;
        for (CustomBow bow : bows) {
            if (slot >= 35) break;

            ItemStack bowItem = plugin.getBowManager().createBowItem(bow.getId());
            if (bowItem == null) continue;

            if (adminMode) {
                ItemMeta meta = bowItem.getItemMeta();
                List<String> lore = meta.getLore();
                if (lore == null) lore = new ArrayList<>();
                lore.add("");
                lore.add(ColorUtils.colorize("&e&lInformation:"));
                lore.add(ColorUtils.colorize("&7ID: &f" + bow.getId()));
                lore.add(ColorUtils.colorize("&7Craftable: " + (bow.isCraftable() ? "&aYes" : "&cNo")));
                lore.add(ColorUtils.colorize("&7Unbreakable: " + (bow.isUnbreakable() ? "&aYes" : "&cNo")));
                lore.add(ColorUtils.colorize("&7Delay: &f" + bow.getDelay() + " seconds"));
                lore.add("");
                lore.add(ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("admin-get-bow")));
                meta.setLore(lore);
                bowItem.setItemMeta(meta);
            }

            inventory.setItem(slot, bowItem);
            slot++;
            if (slot == 17) slot = 19;
            else if (slot == 26) slot = 28;
        }

        player.openInventory(inventory);
    }

    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtils.colorize("&7"));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BowMenuHolder holder)) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        Player player = (Player) event.getWhoClicked();

        if (holder.admin && player.hasPermission("ub.admin")) {
            String bowId = plugin.getBowManager().getCustomBowId(clickedItem);
            if (bowId != null) {
                ItemStack bowItem = plugin.getBowManager().createBowItem(bowId);
                if (bowItem != null) {
                    player.getInventory().addItem(bowItem);
                    String message = plugin.getConfigManager().getMessages().getString("bow-received");
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.05f);
                    CustomBow bow = plugin.getBowManager().getCustomBow(bowId);
                    message = message.replace("{bow}", bow.getName());
                    player.sendMessage(ColorUtils.colorize(message));
                }
            }
        }
    }
}

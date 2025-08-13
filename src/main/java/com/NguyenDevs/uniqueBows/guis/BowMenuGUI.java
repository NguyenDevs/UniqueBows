package com.NguyenDevs.uniqueBows.guis;

import com.NguyenDevs.uniqueBows.UniqueBows;
import com.NguyenDevs.uniqueBows.models.CustomBow;
import com.NguyenDevs.uniqueBows.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BowMenuGUI implements Listener {

    private final UniqueBows plugin;
    private final boolean adminMode;
    private static final int[] BORDER_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};

    public BowMenuGUI(UniqueBows plugin, boolean adminMode) {
        this.plugin = plugin;
        this.adminMode = adminMode;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openMenu(Player player) {
        String title = adminMode ?
                ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("admin-menu-title", "&c&lAdmin Bows Menu")) :
                ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("menu-title", "&6&lUnique Bows Menu"));

        Inventory inventory = Bukkit.createInventory(null, 45, title);

        // Add border
        ItemStack borderItem = createBorderItem();
        for (int slot : BORDER_SLOTS) {
            inventory.setItem(slot, borderItem);
        }

        // Add bows
        List<CustomBow> bows = new ArrayList<>(plugin.getBowManager().getAllBows());
        int slot = 10; // Starting slot for bows (first non-border slot)

        for (CustomBow bow : bows) {
            if (slot >= 35) break; // Don't exceed available slots

            ItemStack bowItem = plugin.getBowManager().createBowItem(bow.getId());
            if (bowItem != null) {
                // Add additional lore for admin menu
                if (adminMode) {
                    ItemMeta meta = bowItem.getItemMeta();
                    List<String> lore = meta.getLore();
                    if (lore == null) lore = new ArrayList<>();

                    lore.add("");
                    lore.add(ColorUtils.colorize("&e&lThông tin:"));
                    lore.add(ColorUtils.colorize("&7ID: &f" + bow.getId()));
                    lore.add(ColorUtils.colorize("&7Craftable: " + (bow.isCraftable() ? "&aYes" : "&cNo")));
                    lore.add(ColorUtils.colorize("&7Unbreakable: " + (bow.isUnbreakable() ? "&aYes" : "&cNo")));
                    lore.add(ColorUtils.colorize("&7Delay: &f" + bow.getDelay() + " giây"));
                    lore.add("");
                    lore.add(ColorUtils.colorize("&a&lClick để lấy bow này!"));

                    meta.setLore(lore);
                    bowItem.setItemMeta(meta);
                }

                inventory.setItem(slot, bowItem);
                slot++;

                // Skip to next row if we're at the end of current row
                if (slot == 17) slot = 19;
                else if (slot == 26) slot = 28;
            }
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
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        String menuTitle = ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("menu-title", "&6&lUnique Bows Menu"));
        String adminTitle = ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("admin-menu-title", "&c&lAdmin Bows Menu"));

        if (!inventory.getViewers().contains(player)) return;
        if (!event.getView().getTitle().equals(menuTitle) && !event.getView().getTitle().equals(adminTitle)) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        // Only allow taking items in admin mode
        if (event.getView().getTitle().equals(adminTitle) && player.hasPermission("ub.admin")) {
            String bowId = plugin.getBowManager().getCustomBowId(clickedItem);
            if (bowId != null) {
                ItemStack bowItem = plugin.getBowManager().createBowItem(bowId);
                if (bowItem != null) {
                    player.getInventory().addItem(bowItem);
                    String message = plugin.getConfigManager().getMessages().getString("bow-received", "&aBạn đã nhận được {bow}!");
                    CustomBow bow = plugin.getBowManager().getCustomBow(bowId);
                    message = message.replace("{bow}", bow.getName());
                    player.sendMessage(ColorUtils.colorize(message));
                    player.closeInventory();
                }
            }
        }
    }
}
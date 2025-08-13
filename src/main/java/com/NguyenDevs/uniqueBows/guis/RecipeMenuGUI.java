package com.NguyenDevs.uniqueBows.guis;

import com.NguyenDevs.uniqueBows.UniqueBows;
import com.NguyenDevs.uniqueBows.models.CustomBow;
import com.NguyenDevs.uniqueBows.utils.ColorUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RecipeMenuGUI implements Listener {

    private final UniqueBows plugin;
    private final Map<Player, String> viewingRecipe;

    public RecipeMenuGUI(UniqueBows plugin) {
        this.plugin = plugin;
        this.viewingRecipe = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openMenu(Player player) {
        String title = ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("recipe-menu-title"));
        Inventory inventory = Bukkit.createInventory(null, 45, title);

        ItemStack borderItem = createBorderItem();
        for (int i = 0; i < 45; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, borderItem);
            }
        }

        List<CustomBow> bows = new ArrayList<>();
        for (CustomBow bow : plugin.getBowManager().getAllBows()) {
            if (bow.isCraftable()) {
                bows.add(bow);
            }
        }
        bows.sort(Comparator.comparing(b -> ChatColor.stripColor(ColorUtils.colorize(b.getName()))));
        int slot = 10;
        for (CustomBow bow : bows) {
            if (slot >= 35) break;

            ItemStack bowItem = plugin.getBowManager().createBowItem(bow.getId());
            if (bowItem != null) {
                ItemMeta meta = bowItem.getItemMeta();
                List<String> lore = meta.getLore();
                if (lore == null) lore = new ArrayList<>();

                lore.add("");
                lore.add(ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("recipe-view-lore")));

                meta.setLore(lore);
                bowItem.setItemMeta(meta);
                inventory.setItem(slot, bowItem);

                slot++;
                if (slot == 17) slot = 19;
                else if (slot == 26) slot = 28;
            }
        }

        player.openInventory(inventory);
    }

    public void openRecipeView(Player player, String bowId) {
        ShapedRecipe recipe = plugin.getRecipeManager().getRecipe(bowId);
        if (recipe == null) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("recipe-not-found")));
            return;
        }

        CustomBow bow = plugin.getBowManager().getCustomBow(bowId);
        String title = ColorUtils.colorize("&eRecipe: " + bow.getName());
        Inventory inventory = Bukkit.createInventory(null, 45, title);

        viewingRecipe.put(player, bowId);

        ItemStack borderItem = createBorderItem();
        for (int i = 0; i < 44; i++) {
            inventory.setItem(i, borderItem);
        }

        String[] shape = recipe.getShape();
        Map<Character, ItemStack> ingredientMap = recipe.getIngredientMap();
        int[] craftingSlots = {11, 12, 13, 20, 21, 22, 29, 30, 31};

        ItemStack emptySlot = createEmptySlotItem();
        for (int slot : craftingSlots) {
            inventory.setItem(slot, emptySlot);
        }

        int shapeIndex = 0;
        for (int row = 0; row < 3; row++) {
            String rowPattern = shape[row];
            for (int col = 0; col < 3; col++) {
                if (col < rowPattern.length()) {
                    char ingredient = rowPattern.charAt(col);
                    ItemStack ingredientItem = ingredientMap.get(ingredient);
                    if (ingredientItem != null) {
                        inventory.setItem(craftingSlots[shapeIndex], ingredientItem);
                    }
                }
                shapeIndex++;
            }
        }

        inventory.setItem(24, recipe.getResult());

        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta arrowMeta = arrow.getItemMeta();
        arrowMeta.setDisplayName(ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("recipe-result")));
        arrow.setItemMeta(arrowMeta);
        inventory.setItem(23, arrow);
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("back-button")));
        backButton.setItemMeta(backMeta);
        inventory.setItem(44, backButton);

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.05f);
    }

    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtils.colorize("&7"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEmptySlotItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtils.colorize("&7"));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        String recipeTitle = ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("recipe-menu-title"));

        if (!inventory.getViewers().contains(player)) return;

        if (event.getView().getTitle().equals(recipeTitle)) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

            String bowId = plugin.getBowManager().getCustomBowId(clickedItem);
            if (bowId != null) {
                openRecipeView(player, bowId);
            }
        }
        else if (viewingRecipe.containsKey(player)) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() == Material.BARRIER) {
                viewingRecipe.remove(player);
                openMenu(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.05f);
            }
        }
    }
}
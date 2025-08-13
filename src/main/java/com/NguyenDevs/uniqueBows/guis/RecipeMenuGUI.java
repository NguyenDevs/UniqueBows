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
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeMenuGUI implements Listener {

    private final UniqueBows plugin;
    private final Map<Player, String> viewingRecipe;

    public RecipeMenuGUI(UniqueBows plugin) {
        this.plugin = plugin;
        this.viewingRecipe = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openMenu(Player player) {
        String title = ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("recipe-menu-title", "&e&lBow Recipes"));
        Inventory inventory = Bukkit.createInventory(null, 54, title);

        // Add border
        ItemStack borderItem = createBorderItem();
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, borderItem);
            }
        }

        // Add bow items
        List<CustomBow> bows = new ArrayList<>();
        for (CustomBow bow : plugin.getBowManager().getAllBows()) {
            if (bow.isCraftable()) {
                bows.add(bow);
            }
        }

        int slot = 10;
        for (CustomBow bow : bows) {
            if (slot >= 44) break;

            ItemStack bowItem = plugin.getBowManager().createBowItem(bow.getId());
            if (bowItem != null) {
                ItemMeta meta = bowItem.getItemMeta();
                List<String> lore = meta.getLore();
                if (lore == null) lore = new ArrayList<>();

                lore.add("");
                lore.add(ColorUtils.colorize("&e&lClick để xem công thức!"));

                meta.setLore(lore);
                bowItem.setItemMeta(meta);
                inventory.setItem(slot, bowItem);

                slot++;
                if (slot == 17) slot = 19;
                else if (slot == 26) slot = 28;
                else if (slot == 35) slot = 37;
            }
        }

        player.openInventory(inventory);
    }

    public void openRecipeView(Player player, String bowId) {
        ShapedRecipe recipe = plugin.getRecipeManager().getRecipe(bowId);
        if (recipe == null) {
            player.sendMessage(ColorUtils.colorize("&cKhông tìm thấy công thức cho bow này!"));
            return;
        }

        CustomBow bow = plugin.getBowManager().getCustomBow(bowId);
        String title = ColorUtils.colorize("&eRecipe: " + bow.getName());
        Inventory inventory = Bukkit.createInventory(null, 54, title);

        viewingRecipe.put(player, bowId);

        // Add border
        ItemStack borderItem = createBorderItem();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, borderItem);
        }

        // Add crafting grid (3x3 in center)
        String[] shape = recipe.getShape();
        Map<Character, ItemStack> ingredientMap = recipe.getIngredientMap();

        int[] craftingSlots = {20, 21, 22, 29, 30, 31, 38, 39, 40};
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

        // Add result
        inventory.setItem(24, recipe.getResult());

        // Add arrow pointing to result
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta arrowMeta = arrow.getItemMeta();
        arrowMeta.setDisplayName(ColorUtils.colorize("&a&lKết quả"));
        arrow.setItemMeta(arrowMeta);
        inventory.setItem(23, arrow);

        // Add back button
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ColorUtils.colorize("&c&lQuay lại"));
        backButton.setItemMeta(backMeta);
        inventory.setItem(49, backButton);

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

        String recipeTitle = ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("recipe-menu-title", "&e&lBow Recipes"));

        if (!inventory.getViewers().contains(player)) return;

        // Handle main recipe menu
        if (event.getView().getTitle().equals(recipeTitle)) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

            String bowId = plugin.getBowManager().getCustomBowId(clickedItem);
            if (bowId != null) {
                openRecipeView(player, bowId);
            }
        }
        // Handle recipe view
        else if (viewingRecipe.containsKey(player)) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() == Material.BARRIER) {
                viewingRecipe.remove(player);
                openMenu(player);
            }
        }
    }
}
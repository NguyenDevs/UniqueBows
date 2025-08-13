package com.NguyenDevs.uniqueBows.managers;

import com.NguyenDevs.uniqueBows.UniqueBows;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeManager {

    private final UniqueBows plugin;
    private final Map<String, ShapedRecipe> customRecipes;

    public RecipeManager(UniqueBows plugin) {
        this.plugin = plugin;
        this.customRecipes = new HashMap<>();
    }

    public void loadRecipes() {
        // Clear existing recipes
        clearCustomRecipes();
        customRecipes.clear();

        ConfigurationSection recipesSection = plugin.getConfigManager().getRecipes();

        for (String bowId : recipesSection.getKeys(false)) {
            ConfigurationSection recipeSection = recipesSection.getConfigurationSection(bowId);
            if (recipeSection != null) {
                createRecipe(bowId, recipeSection);
            }
        }

        plugin.getLogger().info("Loaded " + customRecipes.size() + " custom recipes!");
    }

    private void createRecipe(String bowId, ConfigurationSection section) {
        // Check if bow is enabled and craftable
        ConfigurationSection bowSection = plugin.getConfigManager().getBows().getConfigurationSection(bowId);
        if (bowSection == null || !bowSection.getBoolean("enabled", true) || !bowSection.getBoolean("craftable", true)) {
            return;
        }

        ItemStack result = plugin.getBowManager().createBowItem(bowId);
        if (result == null) return;

        NamespacedKey key = new NamespacedKey(plugin, bowId + "_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, result);

        List<String> shape = section.getStringList("shape");
        if (shape.size() != 3) {
            plugin.getLogger().warning("Invalid recipe shape for " + bowId + ". Shape must have 3 rows!");
            return;
        }

        recipe.shape(shape.get(0), shape.get(1), shape.get(2));

        ConfigurationSection ingredients = section.getConfigurationSection("ingredients");
        if (ingredients == null) {
            plugin.getLogger().warning("No ingredients found for recipe " + bowId);
            return;
        }

        for (String ingredientKey : ingredients.getKeys(false)) {
            String materialName = ingredients.getString(ingredientKey);
            Material material = Material.getMaterial(materialName);

            if (material == null) {
                plugin.getLogger().warning("Invalid material '" + materialName + "' in recipe " + bowId);
                continue;
            }

            recipe.setIngredient(ingredientKey.charAt(0), material);
        }

        try {
            Bukkit.addRecipe(recipe);
            customRecipes.put(bowId, recipe);
            plugin.getLogger().info("Added recipe for " + bowId);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to add recipe for " + bowId + ": " + e.getMessage());
        }
    }

    private void clearCustomRecipes() {
        for (ShapedRecipe recipe : customRecipes.values()) {
            try {
                Bukkit.removeRecipe(recipe.getKey());
            } catch (Exception e) {
                // Ignore errors when removing recipes
            }
        }
    }

    public Map<String, ShapedRecipe> getCustomRecipes() {
        return new HashMap<>(customRecipes);
    }

    public ShapedRecipe getRecipe(String bowId) {
        return customRecipes.get(bowId);
    }
}
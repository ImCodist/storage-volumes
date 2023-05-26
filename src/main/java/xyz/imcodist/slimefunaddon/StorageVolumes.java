package xyz.imcodist.slimefunaddon;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import xyz.imcodist.slimefunaddon.items.Drawer;

public class StorageVolumes extends JavaPlugin implements SlimefunAddon, Listener {

    @Override
    public void onEnable() {
        // Load config.
        Config cfg = new Config(this);

        // Create the main category.
        NamespacedKey categoryId = new NamespacedKey(this, "main_category");
        CustomItemStack categoryItem = new CustomItemStack(Material.BARREL, "&2Storage Volumes");

        ItemGroup itemGroup = new ItemGroup(categoryId, categoryItem);

        // Add drawer item.
        SlimefunItem drawerItem = new Drawer(itemGroup);
        drawerItem.register(this);

        // Create the drawers research.
        NamespacedKey drawerResearchID = new NamespacedKey(this, "drawer_research");
        Research drawerResearch = new Research(drawerResearchID, 122120050, "Drawers", 10);
        drawerResearch.addItems(drawerItem);
        drawerResearch.register();
    }

    @Override
    public void onDisable() {
        // Logic for disabling the plugin...
    }

    @Override
    public String getBugTrackerURL() {
        // You can return a link to your Bug Tracker instead of null here
        return null;
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }
}

package cn.nekopixel.bedwars.spawner;

import cn.nekopixel.bedwars.Main;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Iron extends ResourceSpawner {
    public Iron(Main plugin) {
        super(plugin, "iron", 20L); // 20 ticks = 1 second
    }

    @Override
    protected ItemStack getItem() {
        return new ItemStack(Material.IRON_INGOT, 1);
    }
}

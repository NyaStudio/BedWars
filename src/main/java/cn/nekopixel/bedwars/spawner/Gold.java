package cn.nekopixel.bedwars.spawner;

import cn.nekopixel.bedwars.Main;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Gold extends ResourceSpawner {
    public Gold(Main plugin) {
        super(plugin, "gold", 160L); // 160 ticks = 8 seconds
    }

    @Override
    protected ItemStack getItem() {
        return new ItemStack(Material.GOLD_INGOT, 1);
    }
}

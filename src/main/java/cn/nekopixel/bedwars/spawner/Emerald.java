package cn.nekopixel.bedwars.spawner;

import cn.nekopixel.bedwars.Main;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Emerald extends ResourceSpawner {
    private int level = 1;

    public Emerald(Main plugin) {
        super(plugin, "emerald", 1200L); // 20 ticks = 1 second, 1200 ticks = 60 seconds
    }

    @Override
    protected ItemStack getItem() {
        return new ItemStack(Material.EMERALD, 1);
    }

    @Override
    protected Material getMaterial() {
        return Material.EMERALD;
    }

    @Override
    protected int getMaxAmount() {
        return 2;
    }

    public void upgrade() {
        level++;
        switch (level) {
            case 2:
                setSpawnInterval(900L); // 45 secs
                break;
            case 3:
                setSpawnInterval(600L); // 30 secs
                break;
        }
    }
}

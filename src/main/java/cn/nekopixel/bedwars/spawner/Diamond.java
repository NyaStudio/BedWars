package cn.nekopixel.bedwars.spawner;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.EventManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Diamond extends ResourceSpawner {

    public Diamond(Main plugin) {
        super(plugin, "diamond", 400L); // 20 ticks = 1 second, 400 ticks = 20 seconds
    }

    @Override
    protected ItemStack getItem() {
        return new ItemStack(Material.DIAMOND, 1);
    }

    @Override
    protected Material getMaterial() {
        return Material.DIAMOND;
    }

    @Override
    protected int getMaxAmount() {
        return 4;
    }

    public void upgrade() {
        int nextLevel = level + 1;
        if (nextLevel > 3) return;
        
        super.upgrade();
        
        switch (nextLevel) {
            case 2:
                setSpawnInterval(360L); // 18 secs
                break;
            case 3:
                setSpawnInterval(320L); // 16 secs
                break;
        }
        
        GameManager gameManager = Plugin.getInstance().getGameManager();
        if (gameManager != null) {
            EventManager eventManager = gameManager.getEventManager();
            if (eventManager != null) {
                eventManager.onDiamondUpgraded(nextLevel);
            }
        }
    }
    
    public int getLevel() {
        return level;
    }
}

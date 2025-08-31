package cn.nekopixel.bedwars.shop;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.auth.AuthInterceptor;
import cn.nekopixel.bedwars.language.LanguageManager;
import cn.nekopixel.bedwars.spawner.NPCManager;
import cn.nekopixel.bedwars.utils.SoundUtils;
import cn.nekopixel.bedwars.utils.shop.PurchaseUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.util.*;

public class ShopManager implements Listener {
    private final Main plugin;
    private final NPCManager npcManager;
    private final ItemShop itemShop;
    private final UpgradeShop upgradeShop;
    private final Map<UUID, Long> lastPurchaseTime = new HashMap<>();
    private static final double DEFAULT_COOLDOWN_SECONDS = 0.2;
    private static final int MAX_STACK_SIZE = 64;
    private static final String CONFIG_COOLDOWN_PATH = "shop.purchase_cooldown";
    private static final String CONFIG_MAX_STACK_PATH = "shop.max_stack_size";

    private final File itemShopConfigFile;
    private FileConfiguration itemShopConfig;
    private final Map<String, ShopItem> itemShopItems = new HashMap<>();

    private final File quickBuyConfigFile;
    private FileConfiguration quickBuyConfig;
    private final Map<String, ShopItem> quickBuyItems = new HashMap<>();

    public ShopManager(Main plugin, NPCManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        NamespacedKeys.initialize(plugin);
        this.itemShopConfigFile = new File(plugin.getDataFolder(), "item_shop.yml");
        this.quickBuyConfigFile = new File(plugin.getDataFolder(), "quick_buy.yml");
        new ItemCategory(plugin);
        this.itemShop = new ItemShop(plugin);
        this.upgradeShop = new UpgradeShop(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Plugin.getInstance().setShopManager(this);
        loadConfigs();
        loadShopSettings();

        Map<String, ItemCategory.SortCategory> categories = ItemCategory.getInstance().getCategories();
        if (!categories.isEmpty()) {
            String firstCategory = categories.keySet().iterator().next();
            ItemCategory.getInstance().setCurrentCategory(firstCategory);
        }
    }

    private void loadConfigs() {
        loadItemShopConfig();
        loadQuickBuyConfig();
        itemShop.setupShop(itemShopItems);
    }

    private void loadItemShopConfig() {
        if (!itemShopConfigFile.exists()) {
            plugin.saveResource(itemShopConfigFile.getName(), false);
        }
        itemShopConfig = YamlConfiguration.loadConfiguration(itemShopConfigFile);
        loadItemShopItems();
    }

    private void loadItemShopItems() {
        itemShopItems.clear();
        ConfigurationSection itemsSection = itemShopConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            return;
        }

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) {
                continue;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> enchantments = (List<Map<String, Object>>) itemSection.getList("enchantments", List.of());

            String category = itemSection.getString("category", "default");

            ShopItem item = new ShopItem(
                itemSection.getInt("index", 0),
                itemSection.getString("type", ""),
                itemSection.getString("name", ""),
                itemSection.getStringList("lore"),
                itemSection.getString("pricing_type", ""),
                itemSection.getInt("pricing", 0),
                enchantments,
                itemSection.getInt("potion_level", 1),
                itemSection.getInt("potion_duration", 0),
                itemSection.getInt("amount", 1),
                category
            );
            itemShopItems.put(key, item);
        }
    }

    private void loadQuickBuyConfig() {
        if (!quickBuyConfigFile.exists()) {
            plugin.saveResource(quickBuyConfigFile.getName(), false);
        }
        quickBuyConfig = YamlConfiguration.loadConfiguration(quickBuyConfigFile);
        loadQuickBuyItems();
    }

    private void loadQuickBuyItems() {
        quickBuyItems.clear();
        ConfigurationSection itemsSection = quickBuyConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            return;
        }

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) {
                continue;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> enchantments = (List<Map<String, Object>>) itemSection.getList("enchantments", List.of());

            String category = itemSection.getString("category", "quick_buy");

            ShopItem item = new ShopItem(
                itemSection.getInt("index", 0),
                itemSection.getString("type", ""),
                itemSection.getString("name", ""),
                itemSection.getStringList("lore"),
                itemSection.getString("pricing_type", ""),
                itemSection.getInt("pricing", 0),
                enchantments,
                itemSection.getInt("potion_level", 1),
                itemSection.getInt("potion_duration", 0),
                itemSection.getInt("amount", 1),
                category
            );
            quickBuyItems.put(key, item);
        }
    }

    public void reloadConfigs() {
        if (!itemShopConfigFile.exists()) {
            plugin.saveResource(itemShopConfigFile.getName(), false);
        }
        if (!quickBuyConfigFile.exists()) {
            plugin.saveResource(quickBuyConfigFile.getName(), false);
        }
        loadConfigs();
        ItemCategory.getInstance().loadConfig();
    }

    public ItemStack createShopItem(Material material, ShopItem item, Player player) {
        ItemStack itemStack;
        String type = item.getType();
        
        if (type.equals("quick_buy:empty_slot")) {
            itemStack = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName(LanguageManager.getInstance().getMessage("shop.item.empty_slot"));
            List<String> lore = new ArrayList<>();
            lore.add(LanguageManager.getInstance().getMessage("shop.item.quick_buy_slot_description1"));
            lore.add(LanguageManager.getInstance().getMessage("shop.item.quick_buy_slot_description2"));
            meta.setLore(lore);
            
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(NamespacedKeys.getInstance().getShopItemKey(), PersistentDataType.BYTE, (byte) 1);
            data.set(NamespacedKeys.getInstance().getShopTypeKey(), PersistentDataType.STRING, "item");
            
            itemStack.setItemMeta(meta);
            return itemStack;
        }
        
        if (type.equals("pop_tower")) {
            itemStack = new ItemStack(Material.TRAPPED_CHEST);
        } else if (type.startsWith("nekopixel:")) {
            String customType = type.substring(10);
            switch (customType) {
                case "pop_tower" -> itemStack = new ItemStack(Material.TRAPPED_CHEST);
                default -> {
                    plugin.getLogger().warning("Unknown custom item type: " + customType);
                    itemStack = new ItemStack(Material.BARRIER);
                }
            }
        } else if (type.startsWith("minecraft:potion{")) {
            itemStack = new ItemStack(Material.POTION);
        } else if (type.startsWith("minecraft:")) {
            String materialName = type.substring(10).toUpperCase();
            try {
                Material materialType = Material.valueOf(materialName);
                itemStack = new ItemStack(materialType);
            } catch (IllegalArgumentException e) {
                itemStack = new ItemStack(material);
            }
        } else {
            try {
                Material materialType = Material.valueOf(type.toUpperCase());
                itemStack = new ItemStack(materialType);
            } catch (IllegalArgumentException e) {
                itemStack = new ItemStack(material);
            }
        }

        itemStack.setAmount(item.getAmount());

        ItemMeta meta = itemStack.getItemMeta();
        
        Material costMaterial = PurchaseUtils.parseCurrency(item.getPricingType());
        boolean canAfford = false;
        if (player != null && costMaterial != null) {
            int playerAmount = PurchaseUtils.countMaterial(player, costMaterial);
            canAfford = playerAmount >= item.getPricing();
        }
        
        String displayName = canAfford ? "§a" + item.getName() : "§c" + item.getName();
        List<String> processedLore = Placeholders.processPlaceholders(item.getLore(), item, player, canAfford);

        if (item.getEnchantments() != null) {
            for (Map<String, Object> enchantData : item.getEnchantments()) {
                String id = (String) enchantData.get("id");
                if (id != null && id.startsWith("minecraft:")) {
                    id = id.substring(10);
                }
                Enchantment enchantment = Enchantment.getByName(id.toUpperCase());
                if (enchantment != null) {
                    int level = ((Number) enchantData.get("lvl")).intValue();
                    meta.addEnchant(enchantment, level, true);
                }
            }
        }

        if (meta instanceof PotionMeta potionMeta) {
            if (type.startsWith("minecraft:potion{")) {
                String nbt = type.substring("minecraft:potion{".length(), type.length() - 1);
                String[] parts = nbt.split(":");
                if (parts.length >= 2) {
                    String potionType = parts[1];
                    
                    if (potionType.endsWith("2")) {
                        potionType = potionType.substring(0, potionType.length() - 1);
                    }
                    if (potionType.startsWith("long_")) {
                        potionType = potionType.substring(5);
                    }
                    
                    try {
                        String bukkitPotionType = switch (potionType.toLowerCase()) {
                            case "swiftness" -> "SPEED";
                            case "slowness" -> "SLOWNESS";
                            case "strength" -> "STRENGTH";
                            case "weakness" -> "WEAKNESS";
                            case "poison" -> "POISON";
                            case "regeneration" -> "REGEN";
                            case "fire_resistance" -> "FIRE_RESISTANCE";
                            case "water_breathing" -> "WATER_BREATHING";
                            case "invisibility" -> "INVISIBILITY";
                            case "night_vision" -> "NIGHT_VISION";
                            case "healing" -> "INSTANT_HEAL";
                            case "harming" -> "INSTANT_DAMAGE";
                            case "leaping" -> "JUMP";
                            case "slow_falling" -> "SLOW_FALLING";
                            case "luck" -> "LUCK";
                            default -> potionType.toUpperCase();
                        };
                        
                        PotionType potionTypeEnum = PotionType.valueOf(bukkitPotionType);
                        boolean needsCustomHandling = item.getPotionDuration() > 0 || 
                                                     item.getPotionLevel() > 2 || 
                                                     item.getPotionLevel() == 0;
                        
                        if (needsCustomHandling) {
                            try {
                                boolean upgraded = item.getPotionLevel() >= 2;
                                potionMeta.setBasePotionData(new PotionData(potionTypeEnum, false, upgraded));
                            } catch (Exception e) {
                                potionMeta.setBasePotionData(new PotionData(PotionType.WATER));
                            }
                            
                            PersistentDataContainer container = potionMeta.getPersistentDataContainer();
                            container.set(NamespacedKeys.getInstance().getCustomPotionLevel(), PersistentDataType.INTEGER, item.getPotionLevel());
                            container.set(NamespacedKeys.getInstance().getCustomPotionDuration(), PersistentDataType.INTEGER, item.getPotionDuration());
                            container.set(NamespacedKeys.getInstance().getCustomPotionType(), PersistentDataType.STRING, potionTypeEnum.name());
                        } else {
                            boolean extended = false;
                            boolean upgraded = item.getPotionLevel() > 1;
                            
                            if (upgraded && !isUpgradeable(potionTypeEnum)) {
                                plugin.getLogger().warning("Potion type " + potionTypeEnum + " does not support upgrade, but potion_level is set to: " + item.getPotionLevel() + ". Item: " + item.getName());
                            }
                            
                            potionMeta.setBasePotionData(new PotionData(potionTypeEnum, extended, upgraded));
                        }
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        meta.setDisplayName(displayName);
        meta.setLore(processedLore);
        
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_POTION_EFFECTS);
        
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(NamespacedKeys.getInstance().getShopItemKey(), PersistentDataType.BYTE, (byte) 1);
        data.set(NamespacedKeys.getInstance().getPriceKey(), PersistentDataType.INTEGER, item.getPricing());
        data.set(NamespacedKeys.getInstance().getCurrencyKey(), PersistentDataType.STRING, item.getPricingType());
        
        String shopType = "item";
        if (type.equals("pop_tower") || (type.startsWith("nekopixel:") && type.substring(10).equals("pop_tower"))) {
            shopType = "pop_tower";
            data.set(NamespacedKeys.getInstance().getPopTowerKey(), PersistentDataType.BYTE, (byte) 1);
        }
        data.set(NamespacedKeys.getInstance().getShopTypeKey(), PersistentDataType.STRING, shopType);

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private void loadShopSettings() {
        plugin.getConfig().addDefault(CONFIG_COOLDOWN_PATH, DEFAULT_COOLDOWN_SECONDS);
        plugin.getConfig().addDefault(CONFIG_MAX_STACK_PATH, 64);
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) return;
        Player player = event.getPlayer();

        if (npcManager.isShopNPC(villager)) {
            event.setCancelled(true);
            itemShop.openShop(player);
        } else if (npcManager.isUpgradeNPC(villager)) {
            event.setCancelled(true);
            upgradeShop.openShop(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (itemShop.isCategoryItem(clickedItem)) {
            event.setCancelled(true);
            player.setItemOnCursor(null);
            String category = itemShop.getCategoryFromItem(clickedItem);
            if (category != null && !category.equals(ItemCategory.getInstance().getCurrentCategory())) {
                ItemCategory.getInstance().setCurrentCategory(category);
                Inventory inv = event.getView().getTopInventory();
                itemShop.updateInventory(inv, player);
            }
            return;
        }

        if (itemShop.isSeparator(clickedItem)) {
            event.setCancelled(true);
            player.setItemOnCursor(null);
            return;
        }

        if (!itemShop.isShopItem(clickedItem) && !upgradeShop.isShopItem(clickedItem)) return;

        event.setCancelled(true);
        player.setItemOnCursor(null);

        if (!AuthInterceptor.checkAuth()) {
            player.closeInventory();
            AuthInterceptor.obfuscatedCheck();
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();

        if (clickedInventory == null || !clickedInventory.equals(topInventory)) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer data = meta.getPersistentDataContainer();

        int price = data.getOrDefault(NamespacedKeys.getInstance().getPriceKey(), PersistentDataType.INTEGER, 0);
        String currency = data.getOrDefault(NamespacedKeys.getInstance().getCurrencyKey(), PersistentDataType.STRING, "iron");

        if (price == 0) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        Long lastPurchase = lastPurchaseTime.get(player.getUniqueId());
        long cooldown = (long)(plugin.getConfig().getDouble(CONFIG_COOLDOWN_PATH, DEFAULT_COOLDOWN_SECONDS) * 1000);
        if (lastPurchase != null && currentTime - lastPurchase < cooldown) {
            return;
        }

        lastPurchaseTime.put(player.getUniqueId(), currentTime);

        int maxStackSize = plugin.getConfig().getInt(CONFIG_MAX_STACK_PATH, MAX_STACK_SIZE);
        if (!PurchaseUtils.hasEnoughSpace(player, clickedItem, maxStackSize)) {
            player.sendMessage(LanguageManager.getInstance().getMessage("shop.message.inventory_full"));
            SoundUtils.purchaseFailed(player);
            return;
        }
        
        if (!PurchaseUtils.canPurchaseArmor(player, clickedItem)) {
            player.sendMessage(LanguageManager.getInstance().getMessage("shop.message.better_item_owned"));
            SoundUtils.purchaseFailed(player);
            return;
        }

        Material costMaterial = PurchaseUtils.parseCurrency(currency);

        if (costMaterial == null) {
            player.sendMessage(LanguageManager.getInstance().getMessage("shop.message.unknown_currency", "currency", currency));
            SoundUtils.purchaseFailed(player);
            return;
        }

        int playerAmount = PurchaseUtils.countMaterial(player, costMaterial);
        if (playerAmount < price) {
            int needed = price - playerAmount;
            String currencyName = PurchaseUtils.translateCurrency(currency);
            player.sendMessage(LanguageManager.getInstance().getMessage("shop.message.insufficient_currency",
                "currency", currencyName, "amount", String.valueOf(needed)));
            SoundUtils.purchaseFailed(player);
            return;
        }

        PurchaseUtils.removeMaterial(player, costMaterial, price);
        ItemStack reward = PurchaseUtils.createPurchaseItem(clickedItem, player);

        PurchaseUtils.giveItemToPlayer(player, reward);
        String itemName = meta.getDisplayName().replaceAll("§[0-9a-fk-or]", "");
        player.sendMessage(LanguageManager.getInstance().getMessage("shop.message.purchase_success", "item", itemName));
        SoundUtils.purchaseSucceed(player);
        
        updateShopIfOpen(player);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        ItemStack cursorItem = event.getCursor();
        if (cursorItem != null && (itemShop.isShopItem(cursorItem) || upgradeShop.isShopItem(cursorItem))) {
            event.setCancelled(true);
            player.setItemOnCursor(null);
        }
    }

    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateShopIfOpen(player);
        }, 1L);
    }
    
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateShopIfOpen(player);
        }, 1L);
    }
    
    private void updateShopIfOpen(Player player) {
        Inventory openInventory = player.getOpenInventory().getTopInventory();
        String title = player.getOpenInventory().getTitle();
        
        if (title.equals(LanguageManager.getInstance().getMessage("shop.item.shop_title"))) {
            itemShop.updateInventory(openInventory, player);
        }
    }

    public Map<String, ShopItem> getQuickBuyItems() {
        return quickBuyItems;
    }

    private boolean isUpgradeable(PotionType type) {
        return switch (type) {
            case SPEED, SLOWNESS, STRENGTH, JUMP, REGEN, POISON, INSTANT_DAMAGE, INSTANT_HEAL -> true;
            default -> false;
        };
    }

    private boolean isExtendable(PotionType type) {
        return switch (type) {
            case SPEED, SLOWNESS, STRENGTH, WEAKNESS, JUMP, POISON, REGEN, 
                 FIRE_RESISTANCE, WATER_BREATHING, INVISIBILITY, NIGHT_VISION, 
                 SLOW_FALLING -> true;
            default -> false;
        };
    }

    private boolean isInstant(PotionType type) {
        return switch (type) {
            case INSTANT_HEAL, INSTANT_DAMAGE -> true;
            default -> false;
        };
    }
}

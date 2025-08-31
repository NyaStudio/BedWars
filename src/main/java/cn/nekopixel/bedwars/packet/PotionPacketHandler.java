package cn.nekopixel.bedwars.packet;

import cn.nekopixel.bedwars.Main;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTInt;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;

import java.util.List;

public class PotionPacketHandler extends PacketListenerAbstract {
    
    private final Main plugin;
    
    public PotionPacketHandler(Main plugin) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
    }
    
    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);
            ItemStack item = packet.getItem();
            
            if (item != null) {
                ItemStack modifiedItem = processItem(item);
                if (modifiedItem != null) {
                    packet.setItem(modifiedItem);
                    event.markForReEncode(true);
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);
            List<ItemStack> items = packet.getItems();
            boolean modified = false;
            
            for (int i = 0; i < items.size(); i++) {
                ItemStack item = items.get(i);
                if (item != null) {
                    ItemStack modifiedItem = processItem(item);
                    if (modifiedItem != null) {
                        items.set(i, modifiedItem);
                        modified = true;
                    }
                }
            }
            
            if (modified) {
                packet.setItems(items);
                event.markForReEncode(true);
            }
        }
    }
    
    private ItemStack processItem(ItemStack item) {
        if (item.getType() != ItemTypes.POTION) {
            return null;
        }
        
        if (processPotionItem(item)) {
            return item;
        }
        
        return null;
    }
    
    private boolean processPotionItem(ItemStack item) {
        if (item.getType() != ItemTypes.POTION) {
            return false;
        }
        
        NBTCompound nbt = item.getNBT();
        if (nbt == null) {
            nbt = new NBTCompound();
            item.setNBT(nbt);
        }
        
        NBTCompound customData = null;
        if (nbt.getTags().containsKey("PublicBukkitValues")) {
            Object tag = nbt.getTags().get("PublicBukkitValues");
            if (tag instanceof NBTCompound) {
                customData = (NBTCompound) tag;
            }
        }
        
        if (customData == null) {
            return false;
        }
        
        Integer customLevel = getIntFromNBT(customData, "bedwars:custom_potion_level");
        Integer customDuration = getIntFromNBT(customData, "bedwars:custom_potion_duration");
        String customType = getStringFromNBT(customData, "bedwars:custom_potion_type");
        
        if (customLevel != null && customDuration != null && customType != null) {
            modifyPotionDisplay(nbt, customType, customLevel, customDuration);
            return true;
        }
        
        return false;
    }
    
    private void modifyPotionDisplay(NBTCompound nbt, String potionType, int level, int duration) {
        if (nbt.getTags().containsKey("CustomPotionEffects")) {
            nbt.getTags().remove("CustomPotionEffects");
        }
        
        String mcPotionType = getMCPotionType(potionType);
        if (mcPotionType != null) {
            String potionString;
            if (level >= 2 && canBeUpgraded(potionType)) {
                potionString = "minecraft:strong_" + mcPotionType;
            } else if (duration > 180 && canBeExtended(potionType)) {
                potionString = "minecraft:long_" + mcPotionType;
            } else {
                potionString = "minecraft:" + mcPotionType;
            }
            
            nbt.setTag("Potion", new NBTString(potionString));
        }
        
        if (level > 2) {
            int color = getPotionColor(potionType);
            if (color != -1) {
                nbt.setTag("CustomPotionColor", new NBTInt(color));
            }
        }
    }
    
    private Integer getIntFromNBT(NBTCompound compound, String key) {
        if (compound.getTags().containsKey(key)) {
            Object tag = compound.getTags().get(key);
            if (tag instanceof NBTInt) {
                return ((NBTInt) tag).getAsInt();
            }
        }
        return null;
    }
    
    private String getStringFromNBT(NBTCompound compound, String key) {
        if (compound.getTags().containsKey(key)) {
            Object tag = compound.getTags().get(key);
            if (tag instanceof NBTString) {
                return ((NBTString) tag).getValue();
            }
        }
        return null;
    }
    
    private String getMCPotionType(String potionType) {
        return switch (potionType) {
            case "SPEED" -> "swiftness";
            case "SLOWNESS" -> "slowness";
            case "STRENGTH" -> "strength";
            case "WEAKNESS" -> "weakness";
            case "JUMP" -> "leaping";
            case "POISON" -> "poison";
            case "REGEN" -> "regeneration";
            case "FIRE_RESISTANCE" -> "fire_resistance";
            case "WATER_BREATHING" -> "water_breathing";
            case "INVISIBILITY" -> "invisibility";
            case "NIGHT_VISION" -> "night_vision";
            case "INSTANT_HEAL" -> "healing";
            case "INSTANT_DAMAGE" -> "harming";
            case "SLOW_FALLING" -> "slow_falling";
            case "LUCK" -> "luck";
            default -> null;
        };
    }
    
    private int getPotionColor(String potionType) {
        return switch (potionType) {
            case "SPEED" -> 8171462;
            case "SLOWNESS" -> 5926017;
            case "STRENGTH" -> 9643043;
            case "WEAKNESS" -> 4738376;
            case "JUMP" -> 2293580;
            case "POISON" -> 5149489;
            case "REGEN" -> 13458603;
            case "FIRE_RESISTANCE" -> 14981690;
            case "WATER_BREATHING" -> 3035801;
            case "INVISIBILITY" -> 8356754;
            case "NIGHT_VISION" -> 2039713;
            case "INSTANT_HEAL" -> 16262179;
            case "INSTANT_DAMAGE" -> 4393481;
            case "SLOW_FALLING" -> 13565172;
            case "LUCK" -> 3381504;
            default -> -1;
        };
    }
    
    private boolean canBeUpgraded(String potionType) {
        return switch (potionType) {
            case "SPEED", "SLOWNESS", "STRENGTH", "JUMP", 
                 "POISON", "REGEN", "INSTANT_HEAL", "INSTANT_DAMAGE" -> true;
            default -> false;
        };
    }
    
    private boolean canBeExtended(String potionType) {
        return switch (potionType) {
            case "SPEED", "SLOWNESS", "STRENGTH", "WEAKNESS", 
                 "JUMP", "POISON", "REGEN", "FIRE_RESISTANCE", 
                 "WATER_BREATHING", "INVISIBILITY", "NIGHT_VISION", 
                 "SLOW_FALLING" -> true;
            default -> false;
        };
    }
} 
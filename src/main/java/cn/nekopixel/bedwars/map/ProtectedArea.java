package cn.nekopixel.bedwars.map;

import org.bukkit.Location;

public class ProtectedArea {
    private final String name;
    private final Location minLocation;
    private final Location maxLocation;
    private final boolean preventPlace;

    public ProtectedArea(String name, Location minLocation, Location maxLocation, boolean preventPlace) {
        this.name = name;
        this.minLocation = minLocation;
        this.maxLocation = maxLocation;
        this.preventPlace = preventPlace;
    }

    public boolean contains(Location location) {
        if (!location.getWorld().equals(minLocation.getWorld())) {
            return false;
        }

        int x = (int)Math.floor(location.getX());
        int y = (int)Math.floor(location.getY());
        int z = (int)Math.floor(location.getZ());

        int minX = (int)Math.floor(minLocation.getX());
        int minY = (int)Math.floor(minLocation.getY());
        int minZ = (int)Math.floor(minLocation.getZ());

        int maxX = (int)Math.floor(maxLocation.getX());
        int maxY = (int)Math.floor(maxLocation.getY());
        int maxZ = (int)Math.floor(maxLocation.getZ());

        return x >= Math.min(minX, maxX) &&
               x <= Math.max(minX, maxX) &&
               y >= Math.min(minY, maxY) &&
               y <= Math.max(minY, maxY) &&
               z >= Math.min(minZ, maxZ) &&
               z <= Math.max(minZ, maxZ);
    }

    public String getName() {
        return name;
    }

    public Location getMinLocation() {
        return minLocation;
    }

    public Location getMaxLocation() {
        return maxLocation;
    }

    public boolean isPreventPlace() {
        return preventPlace;
    }

    @Override
    public String toString() {
        return "ProtectedArea{" +
                "name='" + name + '\'' +
                ", minLocation=" + minLocation +
                ", maxLocation=" + maxLocation +
                ", preventPlace=" + preventPlace +
                '}';
    }
} 
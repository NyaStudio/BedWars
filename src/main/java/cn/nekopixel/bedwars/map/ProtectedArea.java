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

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return x >= Math.min(minLocation.getX(), maxLocation.getX()) &&
               x <= Math.max(minLocation.getX(), maxLocation.getX()) &&
               y >= Math.min(minLocation.getY(), maxLocation.getY()) &&
               y <= Math.max(minLocation.getY(), maxLocation.getY()) &&
               z >= Math.min(minLocation.getZ(), maxLocation.getZ()) &&
               z <= Math.max(minLocation.getZ(), maxLocation.getZ());
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
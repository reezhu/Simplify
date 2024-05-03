package org.xjcraft.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.xjcraft.exception.BukkitException;

/**
 * Created by Ree on 2017/9/8.
 */
public class LocationGenerator {
    Location area1;
    Location area2;

    Location point1;
    Location point2;

    public LocationGenerator(Location area1, Location area2) throws Exception {
        this.area1 = area1;
        this.area2 = area2;
        randomLocation();
        if (area1.getWorld() != area2.getWorld())
            throw new Exception("必须在同一世界！");

    }

    public void randomLocation() {
        double y = Math.max(area1.getY(), area2.getY());
        if (Math.random() > 0.5) {
            Location temp = area1;
            area1 = area2;
            area2 = temp;
        }
        if (Math.random() > 0.5) {
            point1 = new Location(area1.getWorld(), area1.getX(), y, getMiddle(area1.getZ(), area2.getZ()));
            point2 = new Location(area1.getWorld(), area2.getX(), y, getMiddle(area1.getZ(), area2.getZ()));
        } else {
            point1 = new Location(area1.getWorld(), getMiddle(area1.getX(), area2.getX()), y, area1.getZ());
            point2 = new Location(area1.getWorld(), getMiddle(area1.getX(), area2.getX()), y, area2.getZ());

        }
    }

    private double getMiddle(double a, double b) {
        return getMiddle(a, b, Math.random());
    }

    private double getMiddle(double a, double b, double progress) {
        return (a - b) * progress + b;
    }

    public Location getLocation(double progress) throws BukkitException {
        if (progress < 0 || progress > 1)
            throw new BukkitException("输入必须在0到1之间！");

        return new Location(point1.getWorld(),
                getMiddle(point1.getX(), point2.getX(), progress),
                getMiddle(point1.getY(), point2.getY(), progress),
                getMiddle(point1.getZ(), point2.getZ(), progress),
                0.0f, getDirection());
    }

    private float getDirection() {
//        Location location = point1.subtract(point2);
        double pitch = Math.atan2(point2.getX() - point1.getX(), point2.getZ() - point1.getZ()) / Math.PI * 180;
        return (float) pitch;
    }

    public Location getPoint1() {
        return point1;
    }

    public Location getPoint2() {
        return point2;
    }

    public void teleportPlayer(Player player, double progress) throws BukkitException {
        Location location = getLocation(progress);
        location.setYaw(player.getLocation().getYaw());
        location.setPitch(90);

        player.teleport(location);
    }

}

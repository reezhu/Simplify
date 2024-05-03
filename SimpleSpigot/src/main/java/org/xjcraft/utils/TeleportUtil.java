package org.xjcraft.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;

public class TeleportUtil {

    public static void teleportAll(Plugin plugin, Location location, World world) {
        teleport(plugin, location, world.getPlayers());
    }

    public static void teleport(Plugin plugin, Location location, Collection<Player> players) {
        while (location.getBlock().getRelative(BlockFace.UP).getType() != Material.AIR) {
            location = location.getBlock().getRelative(BlockFace.UP).getLocation();
        }
        final Location loc = location;
        int i = 1;
        for (Player player : players) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                player.teleport(new Location(loc.getWorld(), loc.getX(), loc.getBlockY(), loc.getZ(), player.getLocation().getYaw(), player.getLocation().getPitch()));
            }, i++);
        }


    }
}

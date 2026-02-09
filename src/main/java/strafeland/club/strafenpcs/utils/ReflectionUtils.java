package strafeland.club.strafenpcs.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ReflectionUtils {

    private static final String VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

    /**
     * Dynamically loads a NMS (Net.Minecraft.Server) class based on the server version
     * This allows the plugin to work across different Spigot versions without changing imports
     */
    public static Class<?> getNMSClass(String name) {
        try {
            return Class.forName("net.minecraft.server." + VERSION + "." + name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Class<?> getCraftClass(String name) {
        try {
            return Class.forName("org.bukkit.craftbukkit." + VERSION + "." + name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Sends a raw NMS packet to a specific player using Reflection
     * It accesses the player's 'playerConnection' field to invoke the 'sendPacket' method
     */
    public static void sendPacket(Player player, Object packet) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setValue(Object obj, String name, Object value) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object getValue(Object obj, String name) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
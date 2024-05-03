package org.xjcraft.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

public class HotbarMessager {

    /**
     * These are the Class instances. Use these to get fields or methods for classes.
     */
    private static Class<?> CRAFTPLAYERCLASS;
    private static Class<?> PACKET_PLAYER_CHAT_CLASS;
    private static Class<?> ICHATCOMP;
    private static Class<?> CHATMESSAGE;
    private static Class<?> PACKET_CLASS;

    /**
     * These are the constructors for those classes. You need these to create new objects.
     */
    private static Constructor<?> PACKET_PLAYER_CHAT_CONSTRUCTOR;
    private static Constructor<?> CHATMESSAGE_CONSTRUCTOR;

    /**
     * This is the server version. This is how we know the server version.
     */
    private static final String SERVER_VERSION;

    static {
        /**
         * This gets the server version.
         */
        String name = Bukkit.getServer().getClass().getName();
        name = name.substring(name.indexOf("craftbukkit.") + "craftbukkit.".length());
        name = name.substring(0, name.indexOf("."));
        SERVER_VERSION = name;


        try {
            /**
             * This here sets the class fields.
             */
            CRAFTPLAYERCLASS = Class.forName("org.bukkit.craftbukkit."
                    + SERVER_VERSION + ".entity.CraftPlayer");
            PACKET_PLAYER_CHAT_CLASS = Class.forName("net.minecraft.server."
                    + SERVER_VERSION + ".PacketPlayOutChat");
            PACKET_CLASS = Class.forName("net.minecraft.server."
                    + SERVER_VERSION + ".Packet");
            ICHATCOMP = Class.forName("net.minecraft.server." + SERVER_VERSION
                    + ".IChatBaseComponent");
            PACKET_PLAYER_CHAT_CONSTRUCTOR = Optional.of(
                    PACKET_PLAYER_CHAT_CLASS.getConstructor(ICHATCOMP,
                            byte.class)).get();

            CHATMESSAGE = Class.forName("net.minecraft.server."
                    + SERVER_VERSION + ".ChatMessage");

            /**
             * If it cannot find the constructor one way, we try to get the declared constructor.
             */
            try {
                CHATMESSAGE_CONSTRUCTOR = Optional.of(
                        CHATMESSAGE
                                .getConstructor(String.class, Object[].class))
                        .get();
            } catch (NoSuchMethodException e) {
                CHATMESSAGE_CONSTRUCTOR = Optional.of(
                        CHATMESSAGE.getDeclaredConstructor(String.class,
                                Object[].class)).get();
            }
        } catch (ClassNotFoundException | NoSuchMethodException
                | SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends the hotbar message 'message' to the player 'player'
     *
     * @param player
     * @param message
     */
    public static void sendHotBarMessage(Player player, String message) {
        try {
            //This creates the IChatComponentBase instance
            Object icb = CHATMESSAGE_CONSTRUCTOR.newInstance(message,
                    new Object[0]);

            //This creates the packet
            Object packet = PACKET_PLAYER_CHAT_CONSTRUCTOR.newInstance(icb,
                    (byte) 2);

            //This casts the player to a craftplayer
            Object craftplayerInst = CRAFTPLAYERCLASS.cast(player);

            //This get's the method for craftplayer's handle
            Optional<Method> methodOptional = Optional.of(CRAFTPLAYERCLASS
                    .getMethod("getHandle"));

            //This invokes the method above.
            Object methodhHandle = methodOptional.get().invoke(craftplayerInst);

            //This gets the player's connection
            Object playerConnection = methodhHandle.getClass()
                    .getField("playerConnection").get(methodhHandle);

            //This sends the packet.
            Optional.of(
                    playerConnection.getClass().getMethod("sendPacket",
                            PACKET_CLASS)).get()
                    .invoke(playerConnection, packet);
        } catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException
                | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
package strafeland.club.strafenpcs.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import strafeland.club.strafenpcs.Main;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

public class PacketReader implements Listener {

    private static Main plugin;

    public static void init(Main instance) {
        plugin = instance;
        Bukkit.getPluginManager().registerEvents(new PacketReader(), plugin);
        for (Player p : Bukkit.getOnlinePlayers()) {
            inject(p);
        }
    }

    public static void close() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            uninject(p);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        inject(e.getPlayer());

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Field mapField = plugin.getNpcManager().getClass().getDeclaredField("nameToNpc");
                    mapField.setAccessible(true);
                    Map<String, Object> npcs = (Map<String, Object>) mapField.get(plugin.getNpcManager());
                    for (Object npc : npcs.values()) {
                        plugin.getNpcManager().sendPackets(e.getPlayer(), npc);
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }.runTaskLater(plugin, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        uninject(e.getPlayer());
    }

    public static void inject(Player p) {
        try {
            Object handle = p.getClass().getMethod("getHandle").invoke(p);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            Object networkManager = playerConnection.getClass().getField("networkManager").get(playerConnection);
            Field channelField = null;

            for (Field f : networkManager.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("Channel")) {
                    channelField = f;
                    break;
                }
            }

            if (channelField != null) {
                channelField.setAccessible(true);
                Object channel = channelField.get(networkManager);
                Method getPipelineMethod = channel.getClass().getMethod("pipeline");
                getPipelineMethod.setAccessible(true);
                Object pipeline = getPipelineMethod.invoke(channel);
                Method getMethod = pipeline.getClass().getMethod("get", String.class);

                if (getMethod.invoke(pipeline, "DeluxeNPCReader") == null) {

                    Class<?> inboundHandlerClass = Class.forName("io.netty.channel.ChannelInboundHandler");

                    Object handlerProxy = Proxy.newProxyInstance(
                            PacketReader.class.getClassLoader(),
                            new Class[]{inboundHandlerClass},
                            new InvocationHandler() {
                                @Override
                                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                    String methodName = method.getName();

                                    if (methodName.equals("channelRead")) {
                                        Object packet = args[1];
                                        if (packet.getClass().getSimpleName().equals("PacketPlayInUseEntity")) {

                                            int id = -1;
                                            try {
                                                Field f = packet.getClass().getDeclaredField("a");
                                                f.setAccessible(true);
                                                id = f.getInt(packet);
                                            } catch (Exception ex) {
                                                for (Field f : packet.getClass().getDeclaredFields()) {
                                                    if (f.getType() == int.class) {
                                                        f.setAccessible(true);
                                                        id = f.getInt(packet);
                                                        break;
                                                    }
                                                }
                                            }

                                            if (id != -1) {
                                                String npcName = plugin.getNpcManager().getNameById(id);
                                                if (npcName != null) {
                                                    Bukkit.getScheduler().runTask(plugin, () -> {
                                                        String cmd = plugin.getFileManager().getSaves().getString("npcs." + npcName + ".command");
                                                        if (cmd != null && !cmd.isEmpty()) {
                                                            p.performCommand(cmd);
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    }

                                    try {
                                        Object context = args[0];
                                        Method fireMethod = null;
                                        try {
                                            fireMethod = context.getClass().getMethod("fireChannelRead", Object.class);
                                        } catch(Exception e) {
                                            for(Method m : context.getClass().getMethods()) {
                                                if(m.getName().equals("fireChannelRead")) { fireMethod = m; break; }
                                            }
                                        }
                                        if (fireMethod != null && methodName.equals("channelRead")) {
                                            fireMethod.setAccessible(true);
                                            fireMethod.invoke(context, args[1]);
                                        }
                                    } catch (Exception e) {}
                                    return null;
                                }
                            }
                    );

                    Method addBefore = pipeline.getClass().getMethod("addBefore", String.class, String.class, Class.forName("io.netty.channel.ChannelHandler"));
                    addBefore.setAccessible(true);
                    addBefore.invoke(pipeline, "packet_handler", "DeluxeNPCReader", handlerProxy);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void uninject(Player p) {
    }
}
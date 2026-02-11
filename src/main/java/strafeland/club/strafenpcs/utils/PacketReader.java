package strafeland.club.strafenpcs.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import strafeland.club.strafenpcs.Main;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
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
        long delay = plugin.getFileManager().getConfig().getLong("join-delay-ticks", 40L);
        updateNPCs(e.getPlayer(), delay);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        uninject(e.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        updateNPCs(e.getPlayer(), 20L);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getFrom().getWorld() != e.getTo().getWorld() || e.getFrom().distanceSquared(e.getTo()) > 256) {
            updateNPCs(e.getPlayer(), 20L);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        updateNPCs(e.getPlayer(), 20L);
    }

    private void updateNPCs(Player p, long delay) {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) return;
                try {
                    Field mapField = plugin.getNpcManager().getClass().getDeclaredField("nameToNpc");
                    mapField.setAccessible(true);
                    Map<String, Object> npcs = (Map<String, Object>) mapField.get(plugin.getNpcManager());

                    for (Object npc : npcs.values()) {
                        plugin.getNpcManager().sendPackets(p, npc);
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }.runTaskLater(plugin, delay);
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
                                                            String finalCmd = cmd.replace("%player%", p.getName());

                                                            if (finalCmd.startsWith("/")) {
                                                                finalCmd = finalCmd.substring(1);
                                                            }

                                                            if (finalCmd.startsWith("msg:")) {
                                                                String message = finalCmd.substring(4).trim();
                                                                p.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                                                                return;
                                                            }

                                                            if (finalCmd.startsWith("server ")) {
                                                                String targetServer = finalCmd.split(" ")[1];
                                                                ByteArrayDataOutput out = ByteStreams.newDataOutput();out.writeUTF("Connect");out.writeUTF(targetServer);

                                                                p.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
                                                                return;
                                                            }

                                                            String executor = plugin.getFileManager().getSaves().getString("npcs." + npcName + ".executor", "CONSOLE");

                                                            if (executor.equalsIgnoreCase("PLAYER")) {
                                                                p.performCommand(finalCmd);
                                                            } else {
                                                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                                                            }
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
package strafeland.club.strafenpcs.manager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import strafeland.club.strafenpcs.Main;
import strafeland.club.strafenpcs.utils.ReflectionUtils;

import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class NPCManager {

    private final Main plugin;
    private final Map<Integer, String> idToName = new HashMap<>();
    private final Map<String, Object> nameToNpc = new HashMap<>();
    private final Map<UUID, String> editorMap = new HashMap<>();

    public NPCManager(Main plugin) {
        this.plugin = plugin;
    }

    public void setEditor(Player p, String npcName) { editorMap.put(p.getUniqueId(), npcName); }

    public String getEditor(Player p) { return editorMap.get(p.getUniqueId()); }

    public void removeEditor(Player p) { editorMap.remove(p.getUniqueId()); }

    public boolean exists(String name) { return nameToNpc.containsKey(name); }

    public String getNameById(int id) { return idToName.get(id); }

    // Creates the NMS EntityPlayer instance using Reflection
    public void createNPC(String name, Location loc) {
        try {
            Object server = ReflectionUtils.getCraftClass("CraftServer").cast(Bukkit.getServer()).getClass().getMethod("getServer").invoke(Bukkit.getServer());
            Object world = ReflectionUtils.getCraftClass("CraftWorld").cast(loc.getWorld()).getClass().getMethod("getHandle").invoke(loc.getWorld());
            GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name);
            Class<?> interactManagerClass = ReflectionUtils.getNMSClass("PlayerInteractManager");
            Constructor<?> interactManagerConst = interactManagerClass.getConstructor(ReflectionUtils.getNMSClass("World"));
            Object interactManager = interactManagerConst.newInstance(world);
            Class<?> entityPlayerClass = ReflectionUtils.getNMSClass("EntityPlayer");

            Constructor<?> entityPlayerConst = entityPlayerClass.getConstructor(
                    ReflectionUtils.getNMSClass("MinecraftServer"),
                    ReflectionUtils.getNMSClass("WorldServer"),
                    GameProfile.class,
                    interactManagerClass
            );

            Object npc = entityPlayerConst.newInstance(server, world, gameProfile, interactManager);
            Method setLocation = npc.getClass().getMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
            setLocation.invoke(npc, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

            nameToNpc.put(name, npc);
            Method getId = npc.getClass().getMethod("getId");
            idToName.put((int) getId.invoke(npc), name);

            saveLocationToConfig(name, loc);
            spawnNPC(npc);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void lookAt(Player p, String npcName) {
        if (!nameToNpc.containsKey(npcName)) return;
        Object npc = nameToNpc.get(npcName);

        Location npcLoc = getNPCLocation(npcName);
        Location playerLoc = p.getLocation();

        Vector dir = playerLoc.toVector().subtract(npcLoc.toVector());
        Location directionLoc = npcLoc.clone().setDirection(dir);

        float yaw = directionLoc.getYaw();
        float pitch = directionLoc.getPitch();

        teleportNPC(npcName, directionLoc);
    }

    // Hides the NPC's nametag by adding it to a Scoreboard Team with NameTagVisibility.NEVER
    private void hideName(Player p, String npcName) {
        Scoreboard sb = p.getScoreboard();
        if (sb == null) sb = Bukkit.getScoreboardManager().getMainScoreboard();

        Team team = sb.getTeam("NHIDE");
        if (team == null) {
            team = sb.registerNewTeam("NHIDE");
            team.setNameTagVisibility(NameTagVisibility.NEVER); // Hide name
        }
        if (!team.hasEntry(npcName)) {
            team.addEntry(npcName);
        }
    }

    public void deleteNPC(String name) {
        if (!nameToNpc.containsKey(name)) return;
        Object npc = nameToNpc.get(name);
        despawnNPC(npc);
        try {
            Method getId = npc.getClass().getMethod("getId");
            idToName.remove((int) getId.invoke(npc));
        } catch (Exception e) { e.printStackTrace(); }
        nameToNpc.remove(name);
        plugin.getFileManager().getSaves().set("npcs." + name, null);
        plugin.getFileManager().saveSaves();
    }

    // Fetches skin data from Mojang API asynchronously and updates the NPC's GameProfile
    public void changeSkin(Player admin, String npcName, String skinName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URL url_0 = new URL("https://api.mojang.com/users/profiles/minecraft/" + skinName);
                    InputStreamReader reader_0 = new InputStreamReader(url_0.openStream());
                    String uuid = new JsonParser().parse(reader_0).getAsJsonObject().get("id").getAsString();

                    URL url_1 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
                    InputStreamReader reader_1 = new InputStreamReader(url_1.openStream());
                    JsonObject textureProperty = new JsonParser().parse(reader_1).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();

                    String texture = textureProperty.get("value").getAsString();
                    String signature = textureProperty.get("signature").getAsString();

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!nameToNpc.containsKey(npcName)) return;
                            Object npc = nameToNpc.get(npcName);
                            try {
                                Method getProfile = npc.getClass().getMethod("getProfile");
                                GameProfile profile = (GameProfile) getProfile.invoke(npc);
                                profile.getProperties().removeAll("textures");
                                profile.getProperties().put("textures", new Property("textures", texture, signature));
                                String path = "npcs." + npcName + ".skin";
                                plugin.getFileManager().getSaves().set(path + ".value", texture);
                                plugin.getFileManager().getSaves().set(path + ".signature", signature);
                                plugin.getFileManager().saveSaves();
                                respawnNPC(npc);
                                admin.sendMessage(plugin.getFileManager().getMsg("chat-messages.skin-updated"));
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }.runTask(plugin);
                } catch (Exception e) {
                    new BukkitRunnable() { @Override public void run() { admin.sendMessage(plugin.getFileManager().getMsg("chat-messages.skin-error")); } }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public Location getNPCLocation(String name) {
        if (!nameToNpc.containsKey(name)) return null;
        try {
            Object npc = nameToNpc.get(name);
            Method getBukkitEntity = npc.getClass().getMethod("getBukkitEntity");
            Object entity = getBukkitEntity.invoke(npc);
            return (Location) entity.getClass().getMethod("getLocation").invoke(entity);
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    // Teleports an existing NPC to a new location
    public void teleportNPC(String name, Location newLoc) {
        if (!nameToNpc.containsKey(name)) return;
        Object npc = nameToNpc.get(name);
        despawnNPC(npc);
        try {
            Method setLocation = npc.getClass().getMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
            setLocation.invoke(npc, newLoc.getX(), newLoc.getY(), newLoc.getZ(), newLoc.getYaw(), newLoc.getPitch());

            Object nmsWorld = ReflectionUtils.getCraftClass("CraftWorld").cast(newLoc.getWorld()).getClass().getMethod("getHandle").invoke(newLoc.getWorld());
            Object npcWorld = npc.getClass().getField("world").get(npc);
            if (npcWorld != nmsWorld) {
                Method spawnIn = npc.getClass().getMethod("spawnIn", ReflectionUtils.getNMSClass("World"));
                spawnIn.invoke(npc, nmsWorld);
            }
            saveLocationToConfig(name, newLoc);
            spawnNPC(npc);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void setCommand(String npcName, String command) {
        plugin.getFileManager().getSaves().set("npcs." + npcName + ".command", command);
        plugin.getFileManager().saveSaves();
    }

    private void spawnNPC(Object npc) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendPackets(p, npc);
        }
    }

    private void despawnNPC(Object npc) {
        try {
            Method getId = npc.getClass().getMethod("getId");
            int id = (int) getId.invoke(npc);
            Object packet = ReflectionUtils.getNMSClass("PacketPlayOutEntityDestroy").getConstructor(int[].class).newInstance(new int[]{id});
            for (Player p : Bukkit.getOnlinePlayers()) { ReflectionUtils.sendPacket(p, packet); }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void respawnNPC(Object npc) {
        despawnNPC(npc);
        spawnNPC(npc);
    }

    // Constructs and sends all necessary packets to render the NPC for a player
    public void sendPackets(Player p, Object npc) {
        try {
            Class<?> infoClass = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo");
            Class<?> actionClass = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction");
            Class<?> entityPlayerClass = ReflectionUtils.getNMSClass("EntityPlayer");
            Class<?> spawnClass = ReflectionUtils.getNMSClass("PacketPlayOutNamedEntitySpawn");
            Class<?> rotationClass = ReflectionUtils.getNMSClass("PacketPlayOutEntityHeadRotation");
            Class<?> entityClass = ReflectionUtils.getNMSClass("Entity");

            Object action = Enum.valueOf((Class<Enum>) actionClass, "ADD_PLAYER");
            Object array = Array.newInstance(entityPlayerClass, 1);
            Array.set(array, 0, npc);

            Constructor<?> infoConst = infoClass.getConstructor(actionClass, array.getClass());
            Object infoPacket = infoConst.newInstance(action, array);

            Constructor<?> spawnConst = spawnClass.getConstructor(ReflectionUtils.getNMSClass("EntityHuman"));
            Object spawnPacket = spawnConst.newInstance(npc);

            Constructor<?> rotConst = rotationClass.getConstructor(entityClass, byte.class);
            float yaw = (float) npc.getClass().getField("yaw").get(npc);
            Object rotPacket = rotConst.newInstance(npc, (byte) (yaw * 256 / 360));

            ReflectionUtils.sendPacket(p, infoPacket);
            ReflectionUtils.sendPacket(p, spawnPacket);
            ReflectionUtils.sendPacket(p, rotPacket);

            Method getProfile = npc.getClass().getMethod("getProfile");
            GameProfile profile = (GameProfile) getProfile.invoke(npc);
            hideName(p, profile.getName());

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    Object removeAction = Enum.valueOf((Class<Enum>) actionClass, "REMOVE_PLAYER");
                    Object removePacket = infoConst.newInstance(removeAction, array);
                    ReflectionUtils.sendPacket(p, removePacket);
                } catch (Exception e) { e.printStackTrace(); }
            }, 5L);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void despawnAll() {
        for (Object npc : nameToNpc.values()) { despawnNPC(npc); }
        nameToNpc.clear(); idToName.clear();
    }

    // Loads all saved NPCs from 'saves.yml' when the server starts
    public void loadNPCs() {
        if (!plugin.getFileManager().getSaves().contains("npcs")) return;
        for (String key : plugin.getFileManager().getSaves().getConfigurationSection("npcs").getKeys(false)) {
            String path = "npcs." + key;
            World world = Bukkit.getWorld(plugin.getFileManager().getSaves().getString(path + ".location.world"));
            if (world == null) continue;
            double x = plugin.getFileManager().getSaves().getDouble(path + ".location.x");
            double y = plugin.getFileManager().getSaves().getDouble(path + ".location.y");
            double z = plugin.getFileManager().getSaves().getDouble(path + ".location.z");
            float yaw = (float) plugin.getFileManager().getSaves().getDouble(path + ".location.yaw");
            float pitch = (float) plugin.getFileManager().getSaves().getDouble(path + ".location.pitch");
            Location loc = new Location(world, x, y, z, yaw, pitch);
            createNPC(key, loc);
            if (plugin.getFileManager().getSaves().contains(path + ".skin.value")) {
                try {
                    String val = plugin.getFileManager().getSaves().getString(path + ".skin.value");
                    String sig = plugin.getFileManager().getSaves().getString(path + ".skin.signature");
                    Object npc = nameToNpc.get(key);
                    Method getProfile = npc.getClass().getMethod("getProfile");
                    GameProfile profile = (GameProfile) getProfile.invoke(npc);
                    profile.getProperties().put("textures", new Property("textures", val, sig));
                    respawnNPC(npc);
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    public void setExecutorType(String npcName, String type) {
        plugin.getFileManager().getSaves().set("npcs." + npcName + ".executor", type.toUpperCase());
        plugin.getFileManager().saveSaves();
    }

    public Set<String> getNPCNames() {
        return nameToNpc.keySet();
    }

    private void saveLocationToConfig(String name, Location loc) {
        String path = "npcs." + name + ".location";
        plugin.getFileManager().getSaves().set(path + ".world", loc.getWorld().getName());
        plugin.getFileManager().getSaves().set(path + ".x", loc.getX());
        plugin.getFileManager().getSaves().set(path + ".y", loc.getY());
        plugin.getFileManager().getSaves().set(path + ".z", loc.getZ());
        plugin.getFileManager().getSaves().set(path + ".yaw", loc.getYaw());
        plugin.getFileManager().getSaves().set(path + ".pitch", loc.getPitch());
        plugin.getFileManager().saveSaves();
    }
}
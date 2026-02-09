package strafeland.club.strafenpcs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import strafeland.club.strafenpcs.commands.NPCCommand;
import strafeland.club.strafenpcs.manager.FileManager;
import strafeland.club.strafenpcs.manager.NPCManager;
import strafeland.club.strafenpcs.utils.PacketReader;

public class Main extends JavaPlugin {

    private static Main instance;
    private FileManager fileManager;
    private NPCManager npcManager;

    @Override
    public void onEnable() {
        instance = this;

        // Load files
        this.fileManager = new FileManager(this);
        this.fileManager.loadFiles();

        // Load managers
        this.npcManager = new NPCManager(this);

        // Register commands
        getCommand("npc").setExecutor(new NPCCommand(this));
        getCommand("npc").setTabCompleter(new strafeland.club.strafenpcs.commands.NPCTabCompleter(this));

        // Load PacketReader
        PacketReader.init(this);

        // Load NPCs
        npcManager.loadNPCs();

        // Notify Console
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_AQUA + "StrafeNPCS" + ChatColor.DARK_GRAY + "]" + ChatColor.GREEN + " Plugin has been enabled successfully. Created by zNotHydr0 :)");
    }

    @Override
    public void onDisable() {
        if (npcManager != null) npcManager.despawnAll();
        PacketReader.close();
    }

    public static Main getInstance() { return instance; }
    public FileManager getFileManager() { return fileManager; }
    public NPCManager getNpcManager() { return npcManager; }
}
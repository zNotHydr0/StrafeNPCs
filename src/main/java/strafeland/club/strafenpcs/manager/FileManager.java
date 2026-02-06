package strafeland.club.strafenpcs.manager;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import strafeland.club.strafenpcs.Main;

import java.io.File;
import java.io.IOException;

public class FileManager {

    private final Main plugin;
    private File messagesFile, savesFile, configFile;
    private FileConfiguration messages, saves, config;

    public FileManager(Main plugin) {
        this.plugin = plugin;
    }

    public void loadFiles() {
        // config.yml
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) plugin.saveResource("config.yml", false);
        config = YamlConfiguration.loadConfiguration(configFile);

        // messages.yml
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) plugin.saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // saves.yml
        savesFile = new File(plugin.getDataFolder(), "saves.yml");
        if (!savesFile.exists()) {
            try { savesFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        saves = YamlConfiguration.loadConfiguration(savesFile);
    }

    public FileConfiguration getMessages() { return messages; }
    public FileConfiguration getSaves() { return saves; }
    public FileConfiguration getConfig() { return config; }

    public void saveSaves() {
        try { saves.save(savesFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public String getMsg(String path) {
        String prefix = messages.getString("chat-messages.prefix", "&8[&NPC&8] ");
        String msg = messages.getString(path, "&cMensaje no configurado: " + path);
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }
}
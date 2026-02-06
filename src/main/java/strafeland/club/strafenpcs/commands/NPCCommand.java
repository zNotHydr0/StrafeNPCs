package strafeland.club.strafenpcs.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import strafeland.club.strafenpcs.Main;

public class NPCCommand implements CommandExecutor {

    private final Main plugin;

    public NPCCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (args.length < 1) {
            p.sendMessage(plugin.getFileManager().getMsg("chat-messages.usage"));
            return true;
        }

        String sub = args[0].toLowerCase();

        // Create
        if (sub.equals("create")) {
            if (args.length < 2) {
                p.sendMessage(plugin.getFileManager().getMsg("chat-messages.usage"));
                return true;
            }
            String name = args[1];
            if (plugin.getNpcManager().exists(name)) {
                p.sendMessage(plugin.getFileManager().getMsg("chat-messages.npc-exists").replace("%name%", name));
                return true;
            }
            plugin.getNpcManager().createNPC(name, p.getLocation());
            p.sendMessage(plugin.getFileManager().getMsg("chat-messages.npc-created").replace("%name%", name));
            plugin.getNpcManager().setEditor(p, name);
            return true;
        }

        // Delete
        if (sub.equals("delete")) {
            if (args.length < 2) {
                p.sendMessage(plugin.getFileManager().getMsg("chat-messages.usage"));
                return true;
            }
            String name = args[1];
            if (!plugin.getNpcManager().exists(name)) {
                p.sendMessage(plugin.getFileManager().getMsg("chat-messages.npc-not-found").replace("%name%", name));
                return true;
            }
            plugin.getNpcManager().deleteNPC(name);
            p.sendMessage(plugin.getFileManager().getMsg("chat-messages.npc-deleted").replace("%name%", name));
            return true;
        }

        // Edit
        if (sub.equals("edit")) {
            if (args.length < 2) {
                p.sendMessage(plugin.getFileManager().getMsg("chat-messages.usage"));
                return true;
            }
            String name = args[1];
            if (!plugin.getNpcManager().exists(name)) {
                p.sendMessage(plugin.getFileManager().getMsg("chat-messages.npc-not-found").replace("%name%", name));
                return true;
            }
            plugin.getNpcManager().setEditor(p, name);
            p.sendMessage(plugin.getFileManager().getMsg("chat-messages.npc-selected").replace("%name%", name));
            return true;
        }

        // Skin
        if (sub.equals("skin")) {
            String target = plugin.getNpcManager().getEditor(p);

            if (target == null) {
                p.sendMessage(plugin.getFileManager().getMsg("chat-messages.not-editing"));
                return true;
            }

            if (!plugin.getNpcManager().exists(target)) {
                p.sendMessage(plugin.getFileManager().getMsg("chat-messages.npc-editing-not-found").replace("%name%", target));
                plugin.getNpcManager().removeEditor(p);
                return true;
            }

            if (args.length < 2) {
                p.sendMessage(plugin.getFileManager().getMsg("chat-messages.usage"));
                return true;
            }

            String skinName = args[1];
            p.sendMessage(plugin.getFileManager().getMsg("chat-messages.skin-updating").replace("%skin%", skinName));
            plugin.getNpcManager().changeSkin(p, target, skinName);
            return true;
        }

        // NPC Command
        if (sub.equals("cmd")) {
            String target = plugin.getNpcManager().getEditor(p);

            if (target == null) {
                p.sendMessage(plugin.getFileManager().getMsg("chat-messages.not-editing"));
                return true;
            }

            if (!plugin.getNpcManager().exists(target)) {
                p.sendMessage(plugin.getFileManager().getMsg("chat-messages.npc-editing-not-found").replace("%name%", target));
                plugin.getNpcManager().removeEditor(p);
                return true;
            }

            if (args.length < 2) {
                p.sendMessage(plugin.getFileManager().getMsg("chat-messages.usage"));
                return true;
            }

            StringBuilder cmdBuilder = new StringBuilder();
            for(int i = 1; i < args.length; i++) {
                cmdBuilder.append(args[i]).append(" ");
            }
            String cmdString = cmdBuilder.toString().trim();

            plugin.getNpcManager().setCommand(target, cmdString);
            p.sendMessage(plugin.getFileManager().getMsg("chat-messages.command-set").replace("%cmd%", cmdString));
            return true;
        }

        // Teleport
        if (sub.equals("tp")) {
            if (args.length < 2) {
                p.sendMessage(plugin.getFileManager().getMsg("chat-messages.usage"));
                return true;
            }
            String name = args[1];

            if (!plugin.getNpcManager().exists(name)) {
                p.sendMessage(plugin.getFileManager().getMsg("chat-messages.npc-not-found").replace("%name%", name));
                return true;
            }

            Location npcLoc = plugin.getNpcManager().getNPCLocation(name);
            if (npcLoc != null) {
                p.teleport(npcLoc);
                p.sendMessage(plugin.getFileManager().getMsg("chat-messages.npc-teleported").replace("%name%", name));
            }
            return true;
        }

        // Teleport here
        if (sub.equals("tphere")) {
            String targetName;

            if (args.length >= 2) {
                targetName = args[1];
            } else {
                targetName = plugin.getNpcManager().getEditor(p);
                if (targetName == null) {
                    p.sendMessage(plugin.getFileManager().getMsg("chat-messages.usage"));
                    return true;
                }
            }

            if (!plugin.getNpcManager().exists(targetName)) {
                p.sendMessage(plugin.getFileManager().getMsg("chat-messages.npc-not-found").replace("%name%", targetName));
                return true;
            }

            plugin.getNpcManager().teleportNPC(targetName, p.getLocation());
            p.sendMessage(plugin.getFileManager().getMsg("chat-messages.npc-moved").replace("%name%", targetName));
            return true;
        }

        // Look
        if (sub.equals("look")) {
            String targetName;

            if (args.length >= 2) {
                targetName = args[1];
            } else {
                targetName = plugin.getNpcManager().getEditor(p);
                if (targetName == null) {
                    p.sendMessage(plugin.getFileManager().getMsg("chat-messages.usage"));
                    return true;
                }
            }

            if (!plugin.getNpcManager().exists(targetName)) {
                p.sendMessage(plugin.getFileManager().getMsg("chat-messages.npc-not-found").replace("%name%", targetName));
                return true;
            }

            plugin.getNpcManager().lookAt(p, targetName);
            p.sendMessage(plugin.getFileManager().getMsg("chat-messages.npc-looked").replace("%name%", targetName));
            return true;
        }

        return true;
    }
}
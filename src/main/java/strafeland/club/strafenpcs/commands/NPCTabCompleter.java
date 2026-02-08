package strafeland.club.strafenpcs.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import strafeland.club.strafenpcs.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NPCTabCompleter implements TabCompleter {

    private final Main plugin;

    public NPCTabCompleter(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("create", "delete", "skin", "cmd", "tp", "tphere", "edit", "look");
            StringUtil.copyPartialMatches(args[0], subcommands, completions);
            Collections.sort(completions);
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();

            if (sub.equals("delete") || sub.equals("tp") || sub.equals("tphere") || sub.equals("edit") || sub.equals("look")) {
                List<String> npcNames = new ArrayList<>(plugin.getNpcManager().getNPCNames());
                StringUtil.copyPartialMatches(args[1], npcNames, completions);
                Collections.sort(completions);
                return completions;
            }

            if (sub.equals("skin")) {
                return null;
            }

            return new ArrayList<>();
        }

        return new ArrayList<>();
    }
}
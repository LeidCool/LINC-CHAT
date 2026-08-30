package com.leidcool.lincchat.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Adapts a {@link UniCommand} to Bukkit {@link CommandExecutor} (Paper 1.20.1). */
final class BukkitCommandAdapter implements CommandExecutor, TabCompleter {

    private final UniCommand delegate;

    BukkitCommandAdapter(UniCommand delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        delegate.execute(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        Collection<String> suggestions = delegate.suggest(sender, args);
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                filtered.add(suggestion);
            }
        }
        return filtered;
    }
}

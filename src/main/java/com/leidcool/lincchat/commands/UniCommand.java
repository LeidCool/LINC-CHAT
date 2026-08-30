package com.leidcool.lincchat.commands;

import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;

/**
 * Version-agnostic command handler. Paper 1.21 wraps this as {@code BasicCommand};
 * Paper 1.20.1 binds it through Bukkit {@code PluginCommand}.
 */
public interface UniCommand {

    void execute(CommandSender sender, String[] args);

    default Collection<String> suggest(CommandSender sender, String[] args) {
        return List.of();
    }

    /**
     * Top-level permission node, or {@code null} if the command checks permissions itself
     * (e.g. {@code /unichat} subcommands).
     */
    default String permission() {
        return null;
    }
}

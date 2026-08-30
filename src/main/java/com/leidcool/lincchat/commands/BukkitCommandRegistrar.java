package com.leidcool.lincchat.commands;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Registers {@link UniCommand} handlers through Bukkit {@code plugin.yml} commands
 * (Paper 1.20.1 does not have {@code LifecycleEvents.COMMANDS} / {@code BasicCommand}).
 */
public final class BukkitCommandRegistrar {

    private BukkitCommandRegistrar() {
    }

    public static void registerAll(JavaPlugin plugin, List<CommandBindings.Binding> bindings) {
        for (CommandBindings.Binding binding : bindings) {
            PluginCommand command = plugin.getCommand(binding.name());
            if (command == null) {
                plugin.getLogger().warning("Command '" + binding.name() + "' is missing from plugin.yml");
                continue;
            }
            BukkitCommandAdapter adapter = new BukkitCommandAdapter(binding.command());
            command.setExecutor(adapter);
            command.setTabCompleter(adapter);
            command.setDescription(binding.description());
            if (binding.command().permission() != null) {
                command.setPermission(binding.command().permission());
            }
            if (!binding.aliases().isEmpty()) {
                command.setAliases(binding.aliases());
            }
        }
    }
}

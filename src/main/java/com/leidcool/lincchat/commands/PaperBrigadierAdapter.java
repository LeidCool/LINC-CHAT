package com.leidcool.lincchat.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.Collection;

/** Adapts a {@link UniCommand} to Paper 1.20.6+/1.21 {@link BasicCommand}. */
final class PaperBrigadierAdapter implements BasicCommand {

    private final UniCommand delegate;

    PaperBrigadierAdapter(UniCommand delegate) {
        this.delegate = delegate;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        delegate.execute(source.getSender(), args);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        return delegate.suggest(source.getSender(), args);
    }

    @Override
    public String permission() {
        return delegate.permission();
    }
}

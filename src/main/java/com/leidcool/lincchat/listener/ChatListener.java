package com.leidcool.lincchat.listener;

import com.leidcool.lincchat.channel.Channel;
import com.leidcool.lincchat.channel.ChannelManager;
import com.leidcool.lincchat.channel.impl.LocalRadiusResolver;
import com.leidcool.lincchat.channel.impl.TradeChannel;
import com.leidcool.lincchat.color.ColorProfile;
import com.leidcool.lincchat.color.ColorResolver;
import com.leidcool.lincchat.config.MainConfig;
import com.leidcool.lincchat.config.MessagesProvider;
import com.leidcool.lincchat.format.FormatEngine;
import com.leidcool.lincchat.integration.EconomyProvider;
import com.leidcool.lincchat.integration.SchedulerProvider;
import com.leidcool.lincchat.moderation.AdvertisingFilter;
import com.leidcool.lincchat.moderation.AntiCapsFilter;
import com.leidcool.lincchat.moderation.AntiSpamService;
import com.leidcool.lincchat.moderation.ChatLogService;
import com.leidcool.lincchat.moderation.ChatPauseState;
import com.leidcool.lincchat.moderation.IgnoreService;
import com.leidcool.lincchat.moderation.SwearFilterService;
import com.leidcool.lincchat.storage.PlayerProfileCache;
import com.leidcool.lincchat.storage.PlayerProfileData;
import com.leidcool.lincchat.util.DurationFormatter;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Collection;
import java.util.Optional;

/**
 * Wires the whole "player sent a chat message" pipeline together (TOR sections 4/7/8/11/12):
 * channel resolution (shortcuts, active channel) -&gt; permission/pause/mute gates -&gt;
 * anti-spam/anti-caps/swear/advertising filters -&gt; optional Trade cost -&gt; {@link FormatEngine}
 * rendering (item-links, mentions, colours) -&gt; manual delivery to the resolved recipients.
 * <p>
 * {@link AsyncChatEvent} fires off the main thread; this listener immediately cancels vanilla
 * delivery and re-dispatches the whole pipeline onto the global sync thread via
 * {@link SchedulerProvider}, since recipient resolution (nearby-entity queries, inventory
 * access for item-links) is not safe to run concurrently with the server tick.
 * <p>
 * Both chat events are handled at {@link EventPriority#LOWEST} so channel shortcuts such as
 * {@code !} are snapshotted before plugins like hChatGame / EZColors can strip or rewrite the
 * prefix. The event is cancelled immediately so later listeners cannot broadcast a second copy.
 */
public final class ChatListener implements Listener {

    private final SchedulerProvider scheduler;
    private final ChannelManager channelManager;
    private final PlayerProfileCache profiles;
    private final ColorResolver colorResolver;
    private final FormatEngine formatEngine;
    private final AntiSpamService antiSpam;
    private final SwearFilterService swearFilter;
    private final IgnoreService ignoreService;
    private final ChatLogService chatLog;
    private final ChatPauseState chatPause;
    private final MainConfig config;
    private final MessagesProvider messages;
    private final EconomyProvider economy;

    public ChatListener(SchedulerProvider scheduler,
                         ChannelManager channelManager,
                         PlayerProfileCache profiles,
                         ColorResolver colorResolver,
                         FormatEngine formatEngine,
                         AntiSpamService antiSpam,
                         SwearFilterService swearFilter,
                         IgnoreService ignoreService,
                         ChatLogService chatLog,
                         ChatPauseState chatPause,
                         MainConfig config,
                         MessagesProvider messages,
                         EconomyProvider economy) {
        this.scheduler = scheduler;
        this.channelManager = channelManager;
        this.profiles = profiles;
        this.colorResolver = colorResolver;
        this.formatEngine = formatEngine;
        this.antiSpam = antiSpam;
        this.swearFilter = swearFilter;
        this.ignoreService = ignoreService;
        this.chatLog = chatLog;
        this.chatPause = chatPause;
        this.config = config;
        this.messages = messages;
        this.economy = economy;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        event.setCancelled(true);
        event.viewers().clear();
        scheduler.runGlobalSync(() -> handle(sender, rawMessage));
    }

    /**
     * EssentialsChat, hChatGame and similar plugins still listen to the legacy Bukkit chat event.
     * Cancel it at {@link EventPriority#LOWEST} so they cannot strip channel shortcuts ({@code !})
     * or broadcast a second, differently-formatted line after we already handled
     * {@link AsyncChatEvent}.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        event.getRecipients().clear();
    }

    private void handle(Player sender, String rawMessage) {
        if (!sender.isOnline()) {
            return;
        }
        PlayerProfileData profile = profiles.getOrCreate(sender.getUniqueId());

        Channel channel;
        String message = rawMessage;
        Optional<ChannelManager.ShortcutMatch> shortcut = channelManager.matchShortcut(rawMessage);
        if (shortcut.isPresent()) {
            channel = shortcut.get().channel();
            message = shortcut.get().remainder();
        } else {
            // Routing is purely shortcut-driven (no /ch channel switching): a message with no
            // recognised prefix always goes to the configured default channel (`channels.yml`
            // `default: true`), regardless of any previously-persisted "active channel".
            channel = channelManager.defaultChannel().orElse(null);
        }

        if (channel == null || !channel.isEnabled()) {
            sender.sendMessage(messages.get("channel-disabled"));
            return;
        }
        if (!channelManager.canUse(sender, channel, true)) {
            sender.sendMessage(messages.get("channel-no-permission-speak",
                    Placeholder.unparsed("channel", channel.displayName())));
            return;
        }

        boolean pauseBypass = sender.hasPermission("unichat.pause") || sender.hasPermission("unichat.bypass.mute");
        if (chatPause.isPaused() && !pauseBypass) {
            sender.sendMessage(messages.get("chat-paused-notice"));
            return;
        }

        if (!sender.hasPermission("unichat.bypass.mute")) {
            if (profile.isMuted()) {
                sender.sendMessage(messages.get("muted",
                        Placeholder.unparsed("time", DurationFormatter.formatRemaining(profile.muteExpiryMillis()))));
                return;
            }
            if (profile.isMuted(channel.id())) {
                sender.sendMessage(messages.get("muted",
                        Placeholder.unparsed("time", DurationFormatter.formatRemaining(profile.muteExpiryMillis(channel.id())))));
                return;
            }
        }

        if (message == null || message.isBlank()) {
            return;
        }

        AntiSpamService.Result spamResult = antiSpam.checkWithMinimumCooldown(sender, channel, message, chatPause.slowModeSeconds());
        if (spamResult.verdict() == AntiSpamService.Verdict.COOLDOWN) {
            sender.sendMessage(messages.get("cooldown-wait", Placeholder.unparsed("time", String.valueOf(spamResult.remainingCooldownSeconds()))));
            return;
        }
        if (spamResult.verdict() == AntiSpamService.Verdict.TOO_SIMILAR) {
            sender.sendMessage(messages.get("message-too-similar"));
            return;
        }

        boolean bypassCaps = sender.hasPermission("unichat.bypass.caps");
        AntiCapsFilter.Result capsResult = AntiCapsFilter.process(message, config, bypassCaps);
        message = capsResult.text();
        if (capsResult.modified()) {
            sender.sendMessage(messages.get("caps-warning"));
        }

        boolean bypassFilter = sender.hasPermission("unichat.bypass.filter");
        SwearFilterService.Result swearResult = swearFilter.check(message, bypassFilter);
        if (swearResult.verdict() == SwearFilterService.Verdict.BLOCKED) {
            sender.sendMessage(messages.get("swear-blocked"));
            return;
        }
        message = swearResult.text();

        AdvertisingFilter.Result adResult = AdvertisingFilter.check(message, config, bypassFilter);
        if (adResult.verdict() == AdvertisingFilter.Verdict.BLOCK) {
            sender.sendMessage(messages.get("advertising-blocked"));
            return;
        }
        message = adResult.text();

        if (channel instanceof TradeChannel trade && trade.messageCost() > 0 && economy.isEnabled()
                && !economy.has(sender, trade.messageCost())) {
            sender.sendMessage(messages.get("trade-insufficient-funds", Placeholder.unparsed("cost", economy.format(trade.messageCost()))));
            return;
        }

        ColorProfile colors = colorResolver.resolve(sender, profile);
        FormatEngine.RenderResult renderResult = formatEngine.renderChannelMessage(sender, channel, colors, message);
        if (renderResult.blocked()) {
            String detail = renderResult.blockDetail() == null ? "" : renderResult.blockDetail();
            sender.sendMessage(messages.get(renderResult.blockReasonKey(), Placeholder.unparsed("slot", detail)));
            return;
        }

        if (channel instanceof TradeChannel trade && trade.messageCost() > 0 && economy.isEnabled()) {
            economy.withdraw(sender, trade.messageCost());
        }

        antiSpam.recordAccepted(sender, channel, message);

        Collection<? extends Player> candidates = channel.candidateRecipients(sender, channelManager.context());
        int otherRecipients = 0;
        for (Player viewer : candidates) {
            if (!viewer.equals(sender)) {
                if (!channelManager.canUse(viewer, channel, false)) {
                    continue;
                }
                PlayerProfileData viewerProfile = profiles.getOrCreate(viewer.getUniqueId());
                if (!viewerProfile.isListening(channel.id())) {
                    continue;
                }
                if (ignoreService.shouldHideFromPublicChannel(viewerProfile, sender.getUniqueId(), config)) {
                    continue;
                }
                otherRecipients++;
            }
            viewer.sendMessage(renderResult.component());
        }

        notifyIfNobodyHeard(sender, channel, otherRecipients);

        if (config.mentionsEnabled()) {
            for (Player mentioned : renderResult.mentionedPlayers()) {
                if (mentioned.equals(sender)) {
                    continue;
                }
                PlayerProfileData mentionedProfile = profiles.getOrCreate(mentioned.getUniqueId());
                if (mentionedProfile.mentionsEnabled()) {
                    mentioned.playSound(Sound.sound(Key.key(config.mentionsSound()), Sound.Source.PLAYER,
                            config.mentionsSoundVolume(), config.mentionsSoundPitch()));
                }
            }
        }

        chatLog.log(channel.id(), sender.getName(), message);
    }

    /**
     * Optional {@code channel-empty-radius} notice for range-limited channels (TOR section
     * 6.3): lets the sender know nobody actually received their message, e.g. because no one
     * else is within the local chat radius. Silently skipped for server-wide channels
     * ({@code range: 0}), when disabled per-channel via {@code notify-if-empty}, or when the
     * sender's effective radius is unlimited (a permission/meta override).
     */
    private void notifyIfNobodyHeard(Player sender, Channel channel, int otherRecipients) {
        if (otherRecipients > 0 || channel.definition().range() <= 0 || !channel.definition().notifyIfEmpty()) {
            return;
        }
        int radius = LocalRadiusResolver.effectiveRadius(sender, channel.definition(), channelManager.context());
        if (radius <= 0) {
            return;
        }
        sender.sendMessage(messages.get("channel-empty-radius", Placeholder.unparsed("radius", String.valueOf(radius))));
    }
}

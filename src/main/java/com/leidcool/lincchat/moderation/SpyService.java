package com.leidcool.lincchat.moderation;

import com.leidcool.lincchat.storage.PlayerProfileCache;
import com.leidcool.lincchat.storage.PlayerProfileData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** {@code /socialspy} support: lets staff see other players' private messages (TOR section 11). */
public final class SpyService {

    private final PlayerProfileCache profiles;

    public SpyService(PlayerProfileCache profiles) {
        this.profiles = profiles;
    }

    public void toggle(PlayerProfileData data) {
        data.socialSpyEnabled(!data.socialSpyEnabled());
    }

    /** Online staff who should additionally see this private message via a {@code [Spy]} tag. */
    public List<Player> spyAudience(Player sender, Player target) {
        List<Player> spies = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(sender) || online.equals(target)) {
                continue;
            }
            if (!online.hasPermission("unichat.socialspy")) {
                continue;
            }
            if (profiles.getOrCreate(online.getUniqueId()).socialSpyEnabled()) {
                spies.add(online);
            }
        }
        return spies;
    }
}

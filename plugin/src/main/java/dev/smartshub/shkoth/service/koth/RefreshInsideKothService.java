package dev.smartshub.shkoth.service.koth;

import dev.smartshub.shkoth.api.koth.Koth;
import dev.smartshub.shkoth.api.location.Area;
import dev.smartshub.shkoth.api.location.Corner;
import dev.smartshub.shkoth.registry.KothRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class RefreshInsideKothService {

    private final KothRegistry kothRegistry;

    public RefreshInsideKothService(KothRegistry kothRegistry) {
        this.kothRegistry = kothRegistry;
    }

    /**
     * 對每個運作中的 KOTH，使用 world.getNearbyEntities 直接以區域中心為原點抓玩家，
     * 避免 O(玩家數 × KOTH 數) 的雙層迴圈。
     */
    public void refreshInsideKoth() {
        for (Koth koth : kothRegistry.getRunning()) {
            try {
                refreshSingle(koth);
            } catch (Throwable t) {
                JavaPlugin.getProvidingPlugin(RefreshInsideKothService.class).getLogger().log(
                        Level.SEVERE,
                        "刷新 KOTH '" + koth.getId() + "' 區域內玩家時拋出例外",
                        t);
            }
        }
    }

    private void refreshSingle(Koth koth) {
        Area area = koth.getArea();
        if (area == null) return;

        World world = Bukkit.getWorld(area.worldName());
        if (world == null) {
            clearInside(koth);
            return;
        }

        Set<UUID> previousInside = new HashSet<>(koth.getPlayersInside());
        Set<UUID> currentlyEligible = collectEligiblePlayers(koth, world, area);

        // 進入：之前不在、現在符合
        for (UUID id : currentlyEligible) {
            if (!previousInside.contains(id)) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) {
                    koth.playerEnter(player);
                }
            }
        }

        // 離開：之前在、現在不符合（包括離線、跨世界、走出範圍、不可佔領、不可繼續停留）
        for (UUID id : previousInside) {
            if (!currentlyEligible.contains(id)) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) {
                    koth.playerLeave(player);
                } else {
                    koth.removePlayerDirectly(id);
                }
            }
        }
    }

    private Set<UUID> collectEligiblePlayers(Koth koth, World world, Area area) {
        Location center = area.getCenter();
        double halfX = Math.abs(area.corner1().x() - area.corner2().x()) / 2.0 + 1;
        double halfY = Math.abs(area.corner1().y() - area.corner2().y()) / 2.0 + 1;
        double halfZ = Math.abs(area.corner1().z() - area.corner2().z()) / 2.0 + 1;

        Collection<Player> candidates = world.getNearbyEntitiesByType(Player.class, center, halfX, halfY, halfZ);

        Set<UUID> eligible = new HashSet<>(Math.max(8, candidates.size()));
        for (Player player : candidates) {
            if (!player.isOnline()) continue;
            if (!koth.isInsideArea(player)) continue;
            if (!koth.canPlayerCapture(player)) continue;
            if (koth.getPlayersInside().contains(player.getUniqueId())
                    && !koth.isPlayerEligibleToStay(player)) {
                continue;
            }
            eligible.add(player.getUniqueId());
        }
        return eligible;
    }

    private void clearInside(Koth koth) {
        List<UUID> snapshot = new ArrayList<>(koth.getPlayersInside());
        for (UUID id : snapshot) {
            koth.removePlayerDirectly(id);
        }
    }
}

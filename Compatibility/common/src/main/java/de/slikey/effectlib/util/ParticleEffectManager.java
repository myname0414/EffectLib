package de.slikey.effectlib.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public interface ParticleEffectManager {
    void onError(String message, Throwable ex);
    void onError(Throwable ex);
    void onError(String message);
    boolean isVisiblePlayer(Player player, Location center, double distanceSquared);
    boolean isPlayerIgnored(Player player);
    boolean getForceShow();
    Plugin getOwningPlugin();
}

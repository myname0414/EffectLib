package de.slikey.effectlib.util.versions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import de.slikey.effectlib.util.ParticleDisplay;
import de.slikey.effectlib.util.ParticleOptions;

public class ParticleDisplay_13 extends ParticleDisplay {

    protected void displayFakeBlock(final Player player, Location center, ParticleOptions options) {
        if (options.blockData == null) return;
        if (!center.getBlock().isPassable() && !center.getBlock().isEmpty()) return;

        BlockData blockData = Bukkit.createBlockData(options.blockData.toLowerCase());
        final Location b = center.getBlock().getLocation().clone();
        player.sendBlockChange(b, blockData);

        Bukkit.getScheduler().runTaskLaterAsynchronously(manager.getOwningPlugin(), new Runnable() {
            @Override
            public void run() {
                player.sendBlockChange(b, b.getBlock().getBlockData());
            }
        }, options.blockDuration);
    }

}

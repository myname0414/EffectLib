package de.slikey.effectlib.util;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Vibration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.util.versions.ParticleDisplay_12;
import de.slikey.effectlib.util.versions.ParticleDisplay_13;
import de.slikey.effectlib.util.versions.ParticleDisplay_21_4;

public abstract class ParticleDisplay {

    protected EffectManager manager;

    protected static boolean hasColorTransition = false;
    protected static boolean hasColorDataType = false;

    protected static Particle SPELL_MOB;
    protected static Particle SPELL_MOB_AMBIENT;
    protected static Particle ITEM_CRACK;
    protected static Particle BLOCK_CRACK;
    protected static Particle BLOCK_DUST;
    protected static Particle FALLING_DUST;
    protected static Particle REDSTONE;
    protected static Particle DUST_COLOR_TRANSITION;
    protected static Particle VIBRATION;
    protected static Particle SHRIEK;
    protected static Particle SCULK_CHARGE;

    protected static void initializeConstants() {
        if (SPELL_MOB != null) return;
        SPELL_MOB = ParticleUtil.getParticle("SPELL_MOB");
        SPELL_MOB_AMBIENT = ParticleUtil.getParticle("SPELL_MOB_AMBIENT");
        ITEM_CRACK = ParticleUtil.getParticle("ITEM_CRACK");
        BLOCK_CRACK = ParticleUtil.getParticle("BLOCK_CRACK");
        BLOCK_DUST = ParticleUtil.getParticle("BLOCK_DUST");
        FALLING_DUST = ParticleUtil.getParticle("FALLING_DUST");
        REDSTONE = ParticleUtil.getParticle("REDSTONE");
        DUST_COLOR_TRANSITION = ParticleUtil.getParticle("DUST_COLOR_TRANSITION");
        VIBRATION = ParticleUtil.getParticle("VIBRATION");
        SHRIEK = ParticleUtil.getParticle("SHRIEK");
        SCULK_CHARGE = ParticleUtil.getParticle("SCULK_CHARGE");
    }

    public void display(Particle particle, ParticleOptions options, Location center, double range, List<Player> targetPlayers) {
        initializeConstants();

        // Legacy colorizeable particles
        // 1.20.5 has removed Particle#SPELL_MOB_AMBIENT and SPELL_MOB is now ENTITY_EFFECT (handled by ParticleUtil)
        if (options.color != null && particle == SPELL_MOB) {
            displayLegacyColored(particle, options, center, range, targetPlayers);
            return;
        }

        if (particle == ITEM_CRACK) {
            displayItem(particle, options, center, range, targetPlayers);
            return;
        }

        if (particle == BLOCK_DUST || particle == FALLING_DUST) {
            Material material = options.material;
            if (material == null || material.name().contains("AIR")) return;
            try {
                options.data = material.createBlockData();
            } catch (Exception ex) {
                manager.onError("Error creating block data for " + material, ex);
            }
            if (options.data == null) return;
        }

        if (particle == REDSTONE) {
            // color is required
            if (options.color == null) options.color = Color.RED;
            options.data = new Particle.DustOptions(options.color, options.size);
        }

        if (particle == DUST_COLOR_TRANSITION) {
            if (options.color == null) options.color = Color.RED;
            if (options.toColor == null) options.toColor = options.color;
            options.data = new Particle.DustTransition(options.color, options.toColor, options.size);
        }

        if (particle == VIBRATION) {
            if (options.target == null) return;

            Vibration.Destination destination;
            Entity targetEntity = options.target.getEntity();
            if (targetEntity != null) destination = new Vibration.Destination.EntityDestination(targetEntity);
            else {
                Location targetLocation = options.target.getLocation();
                if (targetLocation == null) return;

                destination = new Vibration.Destination.BlockDestination(targetLocation);
            }

            options.data = new Vibration(center, destination, options.arrivalTime);
        }

        if (particle == SHRIEK) {
            if (options.shriekDelay < 0) options.shriekDelay = 0;
            options.data = options.shriekDelay;
        }

        if (particle == SCULK_CHARGE) {
            options.data = options.sculkChargeRotation;
        }

        spawnParticle(particle, options, center, range, targetPlayers);
    }

    protected void spawnParticle(Particle particle, ParticleOptions options, Location center, double range, List<Player> targetPlayers) {
        try {
            if (targetPlayers == null) {
                double squared = range * range;
                for (final Player player : Bukkit.getOnlinePlayers()) {
                    if (!manager.isVisiblePlayer(player, center, squared)) continue;

                    if (hasColorDataType && particle == SPELL_MOB) {
                        player.spawnParticle(particle, center, options.amount, options.offsetX, options.offsetY, options.offsetZ, options.speed, options.color == null ? Color.WHITE : options.color);
                    } else {
                        player.spawnParticle(particle, center, options.amount, options.offsetX, options.offsetY, options.offsetZ, options.speed, options.data);
                    }

                    displayFakeBlock(player, center, options);
                }
                return;
            }

            for (final Player player : targetPlayers) {
                if (manager.isPlayerIgnored(player)) continue;
                player.spawnParticle(particle, center, options.amount, options.offsetX, options.offsetY, options.offsetZ, options.speed, options.data);
                displayFakeBlock(player, center, options);
            }

        } catch (Exception ex) {
            if (manager != null) manager.onError(ex);
        }
    }

    protected void displayFakeBlock(final Player player, Location center, ParticleOptions options) {
        // Implemented in 1.13+
    }

    @SuppressWarnings({"deprecation"})
    protected void displayItem(Particle particle, ParticleOptions options, Location center, double range, List<Player> targetPlayers) {
        Material material = options.material;
        if (material == null || material.isAir()) return;

        ItemStack item = new ItemStack(material);
        item.setDurability(options.materialData);
        options.data = item;
        spawnParticle(particle, options, center, range, targetPlayers);
    }

    protected void displayLegacyColored(Particle particle, ParticleOptions options, Location center, double range, List<Player> targetPlayers) {
        // Colored particles can't have a speed of 0.
        Color color = options.color;
        if (color == null) color = Color.RED;
        if (options.speed == 0) options.speed = 1;
        // Amount = 0 is a special flag that means use the offset as color
        options.amount = 0;

        float offsetX = (float) color.getRed() / 255;
        float offsetY = (float) color.getGreen() / 255;
        float offsetZ = (float) color.getBlue() / 255;

        // The redstone particle reverts to red if R is 0!
        if (offsetX < Float.MIN_NORMAL) offsetX = Float.MIN_NORMAL;

        options.offsetX = offsetX;
        options.offsetY = offsetY;
        options.offsetZ = offsetZ;

        spawnParticle(particle, options, center, range, targetPlayers);
    }

    public void setManager(EffectManager manager) {
        this.manager = manager;
    }

    public static ParticleDisplay newInstance() {
        ParticleDisplay display;

        try {
            // @NotNull Particle var1, @NotNull Location var2, int var3, double var4, double var6, double var8, double var10, @Nullable T var12, boolean var13
            // particle, center, options.amount, options.offsetX, options.offsetY, options.offsetZ, options.speed, options.data, options.forceShow
            Player.class.getMethod("spawnParticle", Particle.class, Location.class, Integer.TYPE, Double.TYPE, Double.TYPE, Double.TYPE, Double.TYPE, Object.class, Boolean.TYPE);
            display = new ParticleDisplay_21_4();
            hasColorTransition = true;
            hasColorDataType = true;
        } catch (Throwable not21_4) {
            // TODO: This could all be cleaned up a bit.
            try {
                Particle.valueOf("DUST");
                display = new ParticleDisplay_13();
                hasColorTransition = true;
                hasColorDataType = true;
            } catch (Throwable not20_5) {
                try {
                    Particle.valueOf("SHRIEK");
                    display = new ParticleDisplay_13();
                    hasColorTransition = true;
                } catch (Throwable not19) {
                    try {
                        Particle.valueOf("VIBRATION");
                        display = new ParticleDisplay_13();
                        hasColorTransition = true;
                    } catch (Throwable not17) {
                        try {
                            Particle.valueOf("SQUID_INK");
                            display = new ParticleDisplay_13();
                        } catch (Throwable not13) {
                            display = new ParticleDisplay_12();
                        }
                    }
                }
            }
        }

        return display;
    }

    public static boolean hasColorTransition() {
        return hasColorTransition;
    }

}

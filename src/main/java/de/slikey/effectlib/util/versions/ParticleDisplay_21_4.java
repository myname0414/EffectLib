package de.slikey.effectlib.util.versions;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Vibration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import de.slikey.effectlib.util.ParticleDisplay;
import de.slikey.effectlib.util.ParticleOptions;

public class ParticleDisplay_21_4 extends ParticleDisplay {

	@Override
	public void display(Particle particle, ParticleOptions options, Location center, double range, List<Player> targetPlayers) {
		// Legacy colorizeable particles
		// 1.20.5 has removed Particle#SPELL_MOB_AMBIENT and SPELL_MOB is now ENTITY_EFFECT
		if (options.color != null && (particle == Particle.valueOf("ENTITY_EFFECT"))) {
			displayLegacyColored(particle, options, center, range, targetPlayers);
			return;
		}

		if (particle == Particle.valueOf("ITEM")) {
			displayItem(particle, options, center, range, targetPlayers);
			return;
		}

		// 1.20.5 has removed Particle#BLOCK_DUST
		if (particle == Particle.valueOf("BLOCK") || particle == Particle.FALLING_DUST) {
			Material material = options.material;
			if (material == null || material.name().contains("AIR")) return;
			try {
				options.data = material.createBlockData();
			} catch (Exception ex) {
				manager.onError("Error creating block data for " + material, ex);
			}
			if (options.data == null) return;
		}

		if (particle == Particle.valueOf("DUST")) {
			// color is required
			if (options.color == null) options.color = Color.RED;
			options.data = new Particle.DustOptions(options.color, options.size);
		}

		if (particle == Particle.DUST_COLOR_TRANSITION) {
			if (options.color == null) options.color = Color.RED;
			if (options.toColor == null) options.toColor = options.color;
			options.data = new Particle.DustTransition(options.color, options.toColor, options.size);
		}

		if (particle == Particle.VIBRATION) {
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

		if (particle == Particle.SHRIEK) {
			if (options.shriekDelay < 0) options.shriekDelay = 0;
			options.data = options.shriekDelay;
		}

		if (particle == Particle.SCULK_CHARGE) {
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

					if (hasColorDataType && particle == Particle.valueOf("ENTITY_EFFECT")) {
						player.spawnParticle(particle, center, options.amount, options.offsetX, options.offsetY, options.offsetZ, options.speed, options.color == null ? Color.WHITE : options.color, options.forceShow);
					} else {
						player.spawnParticle(particle, center, options.amount, options.offsetX, options.offsetY, options.offsetZ, options.speed, options.data, options.forceShow);
					}

					displayFakeBlock(player, center, options);
				}
				return;
			}

			for (final Player player : targetPlayers) {
				if (manager.isPlayerIgnored(player)) continue;
				player.spawnParticle(particle, center, options.amount, options.offsetX, options.offsetY, options.offsetZ, options.speed, options.data, options.forceShow);
				displayFakeBlock(player, center, options);
			}

		} catch (Exception ex) {
			if (manager != null) manager.onError(ex);
		}
	}
}

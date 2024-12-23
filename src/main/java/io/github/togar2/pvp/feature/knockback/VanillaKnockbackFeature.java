package io.github.togar2.pvp.feature.knockback;

import io.github.togar2.pvp.events.EntityKnockbackEvent;
import io.github.togar2.pvp.events.LegacyKnockbackEvent;
import io.github.togar2.pvp.feature.FeatureType;
import io.github.togar2.pvp.feature.config.DefinedFeature;
import io.github.togar2.pvp.feature.config.FeatureConfiguration;
import io.github.togar2.pvp.legacy.LegacyKnockbackSettings;
import io.github.togar2.pvp.player.CombatPlayer;
import io.github.togar2.pvp.utils.CombatVersion;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.network.packet.server.play.HitAnimationPacket;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Vanilla implementation of {@link KnockbackFeature}
 */
public class VanillaKnockbackFeature implements KnockbackFeature {
	public static final DefinedFeature<VanillaKnockbackFeature> DEFINED = new DefinedFeature<>(
			FeatureType.KNOCKBACK, VanillaKnockbackFeature::new,
			FeatureType.VERSION
	);
	
	private final FeatureConfiguration configuration;
	
	private CombatVersion version;
	
	public VanillaKnockbackFeature(FeatureConfiguration configuration) {
		this.configuration = configuration;
	}
	
	@Override
	public void initDependencies() {
		this.version = configuration.get(FeatureType.VERSION);
	}
	
	@Override
	public boolean applyDamageKnockback(Damage damage, LivingEntity target) {
		Entity attacker = damage.getAttacker();
		Entity source = damage.getSource();
		
		double dx = attacker.getPosition().x() - target.getPosition().x();
		double dz = attacker.getPosition().z() - target.getPosition().z();
		
		// Randomize direction
		ThreadLocalRandom random = ThreadLocalRandom.current();
		while (dx * dx + dz * dz < 0.0001) {
			dx = random.nextDouble(-1, 1) * 0.01;
			dz = random.nextDouble(-1, 1) * 0.01;
		}
		
		// Set the velocity
		if (version.legacy()) {
			if (!applyLegacyDamageKnockback(target, attacker, source, false, 1, dx, dz)) return false;
		} else {
			if (!applyModernKnockback(target, attacker, source,
					EntityKnockbackEvent.KnockbackType.DAMAGE, 0.4f, dx, dz)) return false;
		}
		
		// Send player a packet with its hurt direction
		if (target instanceof Player player) {
			float hurtDir = (float) (Math.toDegrees(Math.atan2(dz, dx)) - player.getPosition().yaw());
			player.sendPacket(new HitAnimationPacket(player.getEntityId(), hurtDir));
		}
		
		return true;
	}
	
	protected boolean applyModernKnockback(LivingEntity target, Entity attacker, @Nullable Entity source,
	                                       EntityKnockbackEvent.KnockbackType type, float strength,
	                                       double dx, double dz) {
		EntityKnockbackEvent knockbackEvent = new EntityKnockbackEvent(
				target, source == null ? attacker : source,
				type, strength
		);
		EventDispatcher.call(knockbackEvent);
		if (knockbackEvent.isCancelled()) return false;
		
		target.takeKnockback(knockbackEvent.getStrength(), dx, dz);
		return true;
	}
	
	protected boolean applyLegacyDamageKnockback(LivingEntity target, Entity attacker, @Nullable Entity source,
	                                             boolean extra, int knockback, double dx, double dz) {
		LegacyKnockbackEvent legacyKnockbackEvent = new LegacyKnockbackEvent(
				target, source == null ? attacker : source, extra);
		EventDispatcher.call(legacyKnockbackEvent);
		if (legacyKnockbackEvent.isCancelled()) return false;
		
		LegacyKnockbackSettings settings = legacyKnockbackEvent.getSettings();
		
		double kbResistance = target.getAttributeValue(Attribute.KNOCKBACK_RESISTANCE);
		double horizontal = settings.horizontal() * (1 - kbResistance) * knockback;
		double vertical = settings.vertical() * (1 - kbResistance) * knockback;
		Vec horizontalModifier = new Vec(dx, dz).normalize().mul(horizontal);
		
		Vec velocity = target.getVelocity();
		//TODO divide by 2 at y component or not?
		target.setVelocity(new Vec(
				velocity.x() / 2d - horizontalModifier.x(),
				target.isOnGround() ? Math.min(
						settings.verticalLimit(), velocity.y() + vertical) : velocity.y(),
				velocity.z() / 2d - horizontalModifier.z()
		));
		
		return true;
	}
	
	@Override
	public boolean applyAttackKnockback(LivingEntity attacker, LivingEntity target, int knockback) {
		if (knockback <= 0) return false;
		
		// If legacy, attacker velocity is reduced before the knockback
		if (version.legacy() && attacker instanceof CombatPlayer custom)
			custom.afterSprintAttack();
		
		double dx = Math.sin(Math.toRadians(attacker.getPosition().yaw()));
		double dz = -Math.cos(Math.toRadians(attacker.getPosition().yaw()));
		
		if (version.legacy()) {
			if (!applyLegacyDamageKnockback(
					target, attacker, attacker,
					true, knockback,
					dx, dz
			)) return false;
		} else {
			if (!applyModernKnockback(
					target, attacker, attacker,
					EntityKnockbackEvent.KnockbackType.ATTACK, knockback * 0.5f,
					dx, dz
			)) return false;
		}
		
		// If not legacy, attacker velocity is reduced after the knockback
		if (version.modern() && attacker instanceof CombatPlayer custom)
			custom.afterSprintAttack();
		
		attacker.setSprinting(false);
		return true;
	}
	
	@Override
	public boolean applySweepingKnockback(LivingEntity attacker, LivingEntity target) {
		double dx = Math.sin(Math.toRadians(attacker.getPosition().yaw()));
		double dz = -Math.cos(Math.toRadians(attacker.getPosition().yaw()));
		
		return applyModernKnockback(
				target, attacker, null,
				EntityKnockbackEvent.KnockbackType.SWEEPING,
				0.4f, dx, dz
		);
	}
}

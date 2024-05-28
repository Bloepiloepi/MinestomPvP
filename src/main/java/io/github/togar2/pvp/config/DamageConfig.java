package io.github.togar2.pvp.config;

import io.github.togar2.pvp.feature.config.CombatConfiguration;
import io.github.togar2.pvp.feature.config.CombatFeatures;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.EntityInstanceEvent;

/**
 * Creates an EventNode with damage events.
 * This includes armor, shields, damage invulnerability, and fall damage.
 * It only reduces damage based on armor attribute,
 * to change that attribute for different types of armor you need {@code ArmorToolConfig}.
 */
public class DamageConfig extends ElementConfig<EntityInstanceEvent> {
	public static final DamageConfig DEFAULT = defaultBuilder().build();
	public static final DamageConfig LEGACY = legacyBuilder().build();
	
	private final boolean
			fallDamageEnabled, equipmentDamageEnabled, shieldEnabled,
			legacyShieldMechanics, armorEnabled, exhaustionEnabled,
			legacyKnockback, soundsEnabled, damageAnimation, performDamage,
			deathMessagesEnabled;
	private final int invulnerabilityTicks;
	
	public DamageConfig(boolean legacy,
	                    boolean fallDamageEnabled, boolean equipmentDamageEnabled,
	                    boolean shieldEnabled, boolean legacyShieldMechanics, int invulnerabilityTicks,
	                    boolean armorEnabled, boolean exhaustionEnabled, boolean legacyKnockback,
	                    boolean soundsEnabled, boolean damageAnimation, boolean performDamage, boolean deathMessages) {
		super(legacy);
		this.fallDamageEnabled = fallDamageEnabled;
		this.equipmentDamageEnabled = equipmentDamageEnabled;
		this.shieldEnabled = shieldEnabled;
		this.legacyShieldMechanics = legacyShieldMechanics;
		this.invulnerabilityTicks = invulnerabilityTicks;
		this.armorEnabled = armorEnabled;
		this.exhaustionEnabled = exhaustionEnabled;
		this.legacyKnockback = legacyKnockback;
		this.soundsEnabled = soundsEnabled;
		this.damageAnimation = damageAnimation;
		this.performDamage = performDamage;
		this.deathMessagesEnabled = deathMessages;
	}
	
	public boolean isFallDamageEnabled() {
		return fallDamageEnabled;
	}
	
	public boolean isEquipmentDamageEnabled() {
		return equipmentDamageEnabled;
	}
	
	public boolean isShieldEnabled() {
		return shieldEnabled;
	}
	
	public boolean isLegacyShieldMechanics() {
		return legacyShieldMechanics;
	}
	
	public int getInvulnerabilityTicks() {
		return invulnerabilityTicks;
	}
	
	public boolean isArmorDisabled() {
		return !armorEnabled;
	}
	
	public boolean isExhaustionEnabled() {
		return exhaustionEnabled;
	}
	
	public boolean isLegacyKnockback() {
		return legacyKnockback;
	}
	
	public boolean isSoundsEnabled() {
		return soundsEnabled;
	}
	
	public boolean isDamageAnimation() {
		return damageAnimation;
	}
	
	public boolean shouldPerformDamage() {
		return performDamage;
	}
	
	public boolean isDeathMessagesEnabled() {
		return deathMessagesEnabled;
	}
	
	@Override
	public EventNode<EntityInstanceEvent> createNode() {
		return new CombatConfiguration().legacy(isLegacy())
				.add(CombatFeatures.VANILLA_DAMAGE)
				.add(CombatFeatures.VANILLA_BLOCK)
				.add(CombatFeatures.VANILLA_ARMOR)
				.add(CombatFeatures.VANILLA_TOTEM)
				.add(CombatFeatures.VANILLA_EXHAUSTION)
				.add(CombatFeatures.VANILLA_KNOCKBACK)
				.add(CombatFeatures.VANILLA_DEATH_MESSAGE)
				.add(CombatFeatures.VANILLA_ITEM_DAMAGE)
				.add(CombatFeatures.VANILLA_FALL)
				.build().createNode();
	}
	
	/**
	 * Creates a builder with the default options.
	 *
	 * @return A builder with default options
	 */
	public static DamageConfigBuilder defaultBuilder() {
		return new DamageConfigBuilder(false).defaultOptions();
	}
	
	/**
	 * Creates a builder with the legacy options.
	 *
	 * @return A builder with legacy options
	 */
	public static DamageConfigBuilder legacyBuilder() {
		return new DamageConfigBuilder(true).legacyOptions();
	}
	
	/**
	 * Creates an empty builder which has everything disabled.
	 *
	 * @return An empty builder
	 */
	public static DamageConfigBuilder emptyBuilder(boolean legacy) {
		return new DamageConfigBuilder(legacy);
	}
}

package io.github.togar2.pvp;

import io.github.togar2.pvp.enchantment.CombatEnchantments;
import io.github.togar2.pvp.feature.CombatFeatures;
import io.github.togar2.pvp.feature.config.CombatFeatureRegistry;
import io.github.togar2.pvp.player.CombatPlayer;
import io.github.togar2.pvp.player.CombatPlayerImpl;
import io.github.togar2.pvp.potion.effect.CombatPotionEffects;
import io.github.togar2.pvp.potion.item.CombatPotionTypes;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeInstance;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.EntityInstanceEvent;

/**
 * The main class of MinestomPvP, which contains the {@link MinestomPvP#init()} method.
 * <p>
 * It can also be used to set legacy attack for a player, see {@link MinestomPvP#setLegacyAttack(Player, boolean)}.
 */
public class MinestomPvP {
	/**
	 * Equivalent to creating a new event node from {@link CombatFeatures#modernVanilla()}
	 *
	 * @return the event node with all modern vanilla feature listeners attached
	 */
	public static EventNode<EntityInstanceEvent> events() {
		return CombatFeatures.modernVanilla().createNode();
	}
	
	/**
	 * Equivalent to creating a new event node from {@link CombatFeatures#legacyVanilla()}
	 *
	 * @return the event node with all legacy (pre-1.9) vanilla feature listeners attached
	 */
	public static EventNode<EntityInstanceEvent> legacyEvents() {
		return CombatFeatures.legacyVanilla().createNode();
	}
	
	/**
	 * Disables or enables legacy attack for a player.
	 * With legacy attack, the player has no attack speed.
	 *
	 * @param player the player
	 * @param legacyAttack {@code true} if legacy attack should be enabled
	 */
	public static void setLegacyAttack(Player player, boolean legacyAttack) {
		AttributeInstance speed = player.getAttribute(Attribute.ATTACK_SPEED);
		if (legacyAttack) {
			speed.setBaseValue(100);
		} else {
			speed.setBaseValue(speed.attribute().defaultValue());
		}
	}
	
	/**
	 * Initializes the PvP library registries,
	 * and then registers a custom player implementation to Minestom.
	 */
	public static void init() {
		CombatEnchantments.registerAll();
		CombatPotionEffects.registerAll();
		CombatPotionTypes.registerAll();
		
		CombatFeatureRegistry.init();
		
		MinecraftServer.getConnectionManager().setPlayerProvider(CombatPlayerImpl::new);
		CombatPlayer.init(MinecraftServer.getGlobalEventHandler());
	}
}

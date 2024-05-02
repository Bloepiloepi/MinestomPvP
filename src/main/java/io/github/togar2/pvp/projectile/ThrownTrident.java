package io.github.togar2.pvp.projectile;

import io.github.togar2.pvp.enchantment.EnchantmentUtils;
import io.github.togar2.pvp.entity.EntityGroup;
import io.github.togar2.pvp.entity.EntityUtils;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.projectile.ThrownTridentMeta;
import net.minestom.server.item.Enchantment;
import net.minestom.server.item.ItemStack;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ThrownTrident extends AbstractArrow {
	private final boolean legacy;
	private final ItemStack tridentItem;
	private boolean damageDone;
	private boolean hasStartedReturning;
	
	public ThrownTrident(@Nullable Entity shooter, boolean legacy, ItemStack tridentItem) {
		super(shooter, EntityType.TRIDENT);
		this.legacy = legacy;
		this.tridentItem = tridentItem;
		
		ThrownTridentMeta meta = ((ThrownTridentMeta) getEntityMeta());
		meta.setLoyaltyLevel(
				EnchantmentUtils.getLevel(Enchantment.LOYALTY, tridentItem));
		meta.setHasEnchantmentGlint(!tridentItem.meta().getEnchantmentMap().isEmpty());
	}
	
	@Override
	public void update(long time) {
		if (stuckTime > 4) damageDone = true;
		
		Entity shooter = getShooter();
		int loyalty = ((ThrownTridentMeta) getEntityMeta()).getLoyaltyLevel();
		if (loyalty > 0 && (damageDone || isNoClip()) && shooter != null) {
			if (shooter.isRemoved() || (shooter instanceof LivingEntity living && living.isDead())
					|| (shooter instanceof Player player && player.getGameMode() == GameMode.SPECTATOR)) {
				if (pickupMode == PickupMode.ALLOWED)
					EntityUtils.spawnItemAtLocation(this, tridentItem, 0.1);
				remove();
			} else {
				// Move towards owner
				setNoClip(true);
				setNoGravity(true);
				Vec vector = shooter.getPosition().add(0, shooter.getEyeHeight(), 0).asVec().sub(position);
				refreshPosition(position.add(0, vector.y() * 0.015 * loyalty, 0));
				setVelocity(velocity.mul(0.95).add(vector.normalize().mul(0.05 * loyalty)
						.mul(ServerFlag.SERVER_TICKS_PER_SECOND)));
				
				if (!hasStartedReturning) {
					getViewersAsAudience().playSound(Sound.sound(
							SoundEvent.ITEM_TRIDENT_RETURN, Sound.Source.NEUTRAL,
							10.0f, 1.0f
					), position.x(), position.y(), position.z());
					hasStartedReturning = true;
				}
			}
		}
		
		super.update(time);
	}
	
	@Override
	protected boolean canHit(Entity entity) {
		return !damageDone && super.canHit(entity);
	}
	
	@Override
	public boolean onHit(@NotNull Entity entity) {
		if (damageDone) return false;
		if (!(entity instanceof LivingEntity living)) return false;
		Entity shooter = getShooter();
		
		float damage = 8.0f + EnchantmentUtils.getAttackDamage(tridentItem, EntityGroup.ofEntity(living), legacy);
		Damage damageObj = new Damage(DamageType.TRIDENT, this, shooter == null ? this : shooter, null, damage);
		if (living.damage(damageObj) && shooter instanceof LivingEntity livingShooter) {
			EnchantmentUtils.onUserDamaged(living, livingShooter);
			EnchantmentUtils.onTargetDamaged(livingShooter, living);
		}
		damageDone = true;
		
		setVelocity(velocity.mul(-0.01, -0.1, -0.01));
		getViewersAsAudience().playSound(Sound.sound(
				SoundEvent.ITEM_TRIDENT_HIT, Sound.Source.NEUTRAL,
				1.0f, 1.0f
		), position.x(), position.y(), position.z());
		
		return false;
	}
	
	@Override
	public boolean canBePickedUp(@Nullable Player player) {
		if (player == null) return true;
		if (getShooter() == player || getShooter() == null) {
			return super.canBePickedUp(player);
		} else return false;
	}
	
	@Override
	public boolean pickup(Player player) {
		return super.pickup(player)
				|| (isNoClip() && getShooter() == player && player.getInventory().addItemStack(tridentItem));
	}
	
	@Override
	protected void tickRemoval() {
		int loyalty = ((ThrownTridentMeta) getEntityMeta()).getLoyaltyLevel();
		if (pickupMode != PickupMode.ALLOWED || loyalty <= 0)
			super.tickRemoval();
	}
	
	@Override
	protected SoundEvent getDefaultSound() {
		return SoundEvent.ITEM_TRIDENT_HIT_GROUND;
	}
	
	@Override
	protected ItemStack getPickupItem() {
		return tridentItem;
	}
}

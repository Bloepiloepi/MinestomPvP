package io.github.togar2.pvp.entity.projectile;

import io.github.togar2.pvp.feature.effect.EffectFeature;
import io.github.togar2.pvp.feature.enchantment.EnchantmentFeature;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.PotionContents;
import net.minestom.server.potion.CustomPotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

public class Arrow extends AbstractArrow {
	public static final ItemStack DEFAULT_ARROW = ItemStack.of(Material.ARROW);
	
	private final EffectFeature effectFeature;
	
	private ItemStack itemStack = DEFAULT_ARROW;
	
	public Arrow(@Nullable Entity shooter, EffectFeature effectFeature, EnchantmentFeature enchantmentFeature) {
		super(shooter, EntityType.ARROW, enchantmentFeature);
		this.effectFeature = effectFeature;
	}
	
	@Override
	public void update(long time) {
		super.update(time);
		
		if (onGround && stuckTime >= 600 && (!itemStack.has(ItemComponent.POTION_CONTENTS)
				|| !Objects.equals(itemStack.get(ItemComponent.POTION_CONTENTS), PotionContents.EMPTY))) {
			triggerStatus((byte) 0);
			itemStack = DEFAULT_ARROW;
		}
	}
	
	@Override
	protected ItemStack getPickupItem() {
		return itemStack;
	}
	
	public void setItemStack(ItemStack itemStack) {
		this.itemStack = itemStack;
	}
	
	@Override
	protected void onHurt(LivingEntity entity) {
		effectFeature.addArrowEffects(entity, this);
	}
	
	public @NotNull PotionContents getPotion() {
		return itemStack.get(ItemComponent.POTION_CONTENTS, PotionContents.EMPTY);
	}
	
	public void setPotion(@NotNull PotionContents potion) {
		this.itemStack = ItemStack.of(Material.TIPPED_ARROW).with(ItemComponent.POTION_CONTENTS, potion);
	}
	
	public void addArrowEffect(CustomPotionEffect effect) {
		this.itemStack = itemStack.with(ItemComponent.POTION_CONTENTS, (UnaryOperator<PotionContents>) potionContents -> {
			List<CustomPotionEffect> list = new ArrayList<>(potionContents.customEffects());
			list.add(effect);
			return new PotionContents(potionContents.potion(), potionContents.customColor(), list);
		});
	}
}

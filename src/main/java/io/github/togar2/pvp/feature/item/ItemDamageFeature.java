package io.github.togar2.pvp.feature.item;

import io.github.togar2.pvp.feature.CombatFeature;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.DamageType;

public interface ItemDamageFeature extends CombatFeature {
	void damageEquipment(LivingEntity entity, EquipmentSlot slot, int amount);
	
	void damageArmor(LivingEntity entity, DamageType damageType, float damage, EquipmentSlot... slots);
}

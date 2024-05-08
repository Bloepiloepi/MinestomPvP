package io.github.togar2.pvp.enchantment;

import io.github.togar2.pvp.damage.DamageTypeInfo;
import io.github.togar2.pvp.entity.EntityGroup;
import io.github.togar2.pvp.enums.ArmorMaterial;
import io.github.togar2.pvp.feature.CombatVersion;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.item.Enchantment;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.MathUtils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public class EnchantmentUtils {
	public static short getLevel(Enchantment enchantment, ItemStack stack) {
		Map<Enchantment, Short> enchantmentMap = stack.meta().getEnchantmentMap();
		return enchantmentMap.containsKey(enchantment)
				? (short) MathUtils.clamp(enchantmentMap.get(enchantment), 0, 255)
				: 0;
	}
	
	public static short getEquipmentLevel(CustomEnchantment customEnchantment, LivingEntity entity) {
		if (customEnchantment == null) return 0;
		
		Iterator<ItemStack> iterator = customEnchantment.getEquipment(entity).values().iterator();
		
		short highest = 0;
		while (iterator.hasNext()) {
			ItemStack itemStack = iterator.next();
			short level = getLevel(customEnchantment.getEnchantment(), itemStack);
			if (level > highest) {
				highest = level;
			}
		}
		
		return highest;
	}
	
	public static Map.Entry<EquipmentSlot, ItemStack> pickRandom(LivingEntity entity, CustomEnchantment enchantment) {
		Map<EquipmentSlot, ItemStack> equipmentMap = enchantment.getEquipment(entity);
		if (equipmentMap.isEmpty()) return null;
		
		List<Map.Entry<EquipmentSlot, ItemStack>> possibleStacks = new ArrayList<>();
		
		for (Map.Entry<EquipmentSlot, ItemStack> entry : equipmentMap.entrySet()) {
			ItemStack itemStack = entry.getValue();
			
			if (!itemStack.isAir() && getLevel(enchantment.getEnchantment(), itemStack) > 0) {
				possibleStacks.add(entry);
			}
		}
		
		return possibleStacks.isEmpty() ? null :
				possibleStacks.get(ThreadLocalRandom.current().nextInt(possibleStacks.size()));
	}
	
	public static void forEachEnchantment(Iterable<ItemStack> stacks, BiConsumer<CustomEnchantment, Short> consumer) {
		for (ItemStack itemStack : stacks) {
			Set<Enchantment> enchantments = itemStack.meta().getEnchantmentMap().keySet();
			
			for (Enchantment enchantment : enchantments) {
				CustomEnchantment customEnchantment = CustomEnchantments.get(enchantment);
				consumer.accept(customEnchantment, itemStack.meta().getEnchantmentMap().get(enchantment));
			}
		}
	}
	
	public static int getProtectionAmount(Iterable<ItemStack> equipment, DamageTypeInfo typeInfo) {
		AtomicInteger result = new AtomicInteger();
		forEachEnchantment(equipment, (enchantment, level) -> result.addAndGet(enchantment.getProtectionAmount(level, typeInfo)));
		return result.get();
	}
	
	public static float getAttackDamage(ItemStack stack, EntityGroup group, CombatVersion version) {
		AtomicReference<Float> result = new AtomicReference<>((float) 0);
		stack.meta().getEnchantmentMap().forEach((enchantment, level) -> {
			CustomEnchantment customEnchantment = CustomEnchantments.get(enchantment);
			result.updateAndGet(v -> v + customEnchantment.getAttackDamage(level, group, version));
		});
		
		return result.get();
	}
	
	public static boolean shouldNotBreak(ItemStack item, int unbreakingLevel) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		if (ArmorMaterial.fromMaterial(item.material()) != null && random.nextFloat() < 0.6f) {
			return false;
		} else {
			return random.nextInt(unbreakingLevel + 1) > 0;
		}
	}
	
	public static double getExplosionKnockback(LivingEntity entity, double strength) {
		short level = getEquipmentLevel(CustomEnchantments.get(Enchantment.BLAST_PROTECTION), entity);
		if (level > 0) {
			strength -= Math.floor((strength * (double) (level * 0.15f)));
		}
		
		return strength;
	}
	
	public static void onUserDamaged(LivingEntity user, LivingEntity attacker) {
		if (user != null) {
			forEachEnchantment(Arrays.asList(
					user.getBoots(), user.getLeggings(),
					user.getChestplate(), user.getHelmet(),
					user.getItemInMainHand(), user.getItemInOffHand()
			), (enchantment, level) -> enchantment.onUserDamaged(user, attacker, level));
		}
	}
	
	public static void onTargetDamaged(LivingEntity user, Entity target) {
		if (user != null) {
			forEachEnchantment(Arrays.asList(
					user.getBoots(), user.getLeggings(),
					user.getChestplate(), user.getHelmet(),
					user.getItemInMainHand(), user.getItemInOffHand()
			), (enchantment, level) -> enchantment.onTargetDamaged(user, target, level));
		}
	}
	
	public static short getKnockback(LivingEntity entity) {
		return getEquipmentLevel(CustomEnchantments.get(Enchantment.KNOCKBACK), entity);
	}
	
	public static short getSweeping(LivingEntity entity) {
		return getEquipmentLevel(CustomEnchantments.get(Enchantment.SWEEPING), entity);
	}
	
	public static short getFireAspect(LivingEntity entity) {
		return getEquipmentLevel(CustomEnchantments.get(Enchantment.FIRE_ASPECT), entity);
	}
	
	public static short getBlockEfficiency(LivingEntity entity) {
		return getEquipmentLevel(CustomEnchantments.get(Enchantment.EFFICIENCY), entity);
	}
}

package io.github.togar2.pvp.feature.food;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import net.minestom.server.event.item.PlayerFinishItemUseEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.togar2.pvp.feature.FeatureType;
import io.github.togar2.pvp.feature.RegistrableFeature;
import io.github.togar2.pvp.feature.config.DefinedFeature;
import io.github.togar2.pvp.feature.config.FeatureConfiguration;
import io.github.togar2.pvp.feature.cooldown.ItemCooldownFeature;
import io.github.togar2.pvp.utils.PotionFlags;
import io.github.togar2.pvp.utils.ViewUtil;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerPreEatEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.trait.EntityInstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.Consumable;
import net.minestom.server.item.component.ConsumeEffect;
import net.minestom.server.item.component.ConsumeEffect.ApplyEffects;
import net.minestom.server.item.component.ConsumeEffect.ClearAllEffects;
import net.minestom.server.item.component.ConsumeEffect.RemoveEffects;
import net.minestom.server.item.component.ConsumeEffect.TeleportRandomly;
import net.minestom.server.item.component.ConsumeEffect.PlaySound;
import net.minestom.server.item.component.Food;
import net.minestom.server.item.component.SuspiciousStewEffects;
import net.minestom.server.potion.CustomPotionEffect;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.potion.TimedPotion;
import net.minestom.server.registry.ObjectSet;
import net.minestom.server.sound.SoundEvent;

/**
 * Vanilla implementation of {@link FoodFeature}
 * <p>
 * This also includes eating of food items.
 */
public class VanillaFoodFeature implements FoodFeature, RegistrableFeature {
	public static final DefinedFeature<VanillaFoodFeature> DEFINED = new DefinedFeature<>(
			FeatureType.FOOD, VanillaFoodFeature::new,
			FeatureType.ITEM_COOLDOWN
	);
	
	private final FeatureConfiguration configuration;
	
	private ItemCooldownFeature itemCooldownFeature;
	
	public VanillaFoodFeature(FeatureConfiguration configuration) {
		this.configuration = configuration;
	}
	
	@Override
	public void initDependencies() {
		this.itemCooldownFeature = configuration.get(FeatureType.ITEM_COOLDOWN);
	}
	
	@Override
	public void init(EventNode<EntityInstanceEvent> node) {
		node.addListener(PlayerPreEatEvent.class, event -> {
			if (event.getItemStack().material() != Material.MILK_BUCKET
					&& !(event.getItemStack().has(ItemComponent.FOOD) || event.getItemStack().has(ItemComponent.CONSUMABLE)))
				return;
			@Nullable Food foodComponent = event.getItemStack().get(ItemComponent.FOOD);
			@Nullable Consumable consumableComponent = event.getItemStack().get(ItemComponent.CONSUMABLE);
			
			// If the players hunger is full and the food is not always edible, cancel
			// For some reason vanilla doesn't say honey is always edible but just overrides the method to always consume it
			boolean alwaysEat = foodComponent == null || foodComponent.canAlwaysEat() || event.getItemStack().material() == Material.HONEY_BOTTLE;
			if (event.getPlayer().getGameMode() != GameMode.CREATIVE
					&& !alwaysEat && event.getPlayer().getFood() == 20) {
				event.setCancelled(true);
				return;
			}
			
			if (consumableComponent != null) event.setEatingTime(getUseTime(event.getItemStack().material(), consumableComponent));
		});
		
		node.addListener(PlayerFinishItemUseEvent.class, event -> {
			if (event.getItemStack().material() != Material.MILK_BUCKET
					&& !(event.getItemStack().has(ItemComponent.FOOD) || event.getItemStack().has(ItemComponent.CONSUMABLE)))
				return;
			
			onFinishEating(event.getPlayer(), event.getItemStack(), event.getHand());
		});
		
		node.addListener(PlayerTickEvent.class, event -> {
			Player player = event.getPlayer();
			if (player.isSilent() || !player.isEating()) return;
			
			tickEatingSounds(player);
		});
	}
	
	protected void onFinishEating(Player player, ItemStack stack, PlayerHand hand) {
		this.eat(player, stack);
		
		Food component = stack.get(ItemComponent.FOOD);
		Consumable consumable = stack.get(ItemComponent.CONSUMABLE);
		assert component != null;
		assert consumable != null;
		ThreadLocalRandom random = ThreadLocalRandom.current();
		
		triggerEatingSound(player, stack.material());
		
		if (stack.material() != Material.MILK_BUCKET) {
			ViewUtil.viewersAndSelf(player).playSound(Sound.sound(
					SoundEvent.ENTITY_PLAYER_BURP, Sound.Source.PLAYER,
					0.5f, random.nextFloat() * 0.1f + 0.9f
			), player);
		}
		
		List<ConsumeEffect> effectList = consumable.effects();
		
		for (ConsumeEffect effect : effectList) {
			switch (effect) {
				case ApplyEffects(List<CustomPotionEffect> effects, float probability) -> {
					if (random.nextFloat() >= probability) continue;
					for (CustomPotionEffect potionEffect : effects) {
						player.addEffect(new Potion(
								potionEffect.id(), (byte)potionEffect.amplifier(),
								potionEffect.duration(),
								PotionFlags.create(
										potionEffect.isAmbient(),
										potionEffect.showParticles(),
										potionEffect.showIcon()
								)
						));
					}
					return;
				}
				case RemoveEffects(ObjectSet<PotionEffect> potionEffects) -> {
					player.getActiveEffects().stream().map(TimedPotion::potion).map(Potion::effect).filter(potionEffects::contains).forEach(player::removeEffect);
					return;
				}
				case ClearAllEffects clearEffects -> {
					player.clearEffects();
					return;
				}
				case TeleportRandomly(float diameter) -> {
					final Pos pos = player.getPosition();
					Instance instance = player.getInstance();
					assert instance != null;
					List<Pos> potentials = new ArrayList<Pos>();
					for (float y = diameter; y >= -diameter; y--) {
						for (float x = diameter; x >= -diameter; x--) {
							for (float z = diameter; z >= -diameter; z--) {
								Pos potential = pos.add(x, y, z);
								if (!instance.getBlock(potential).isSolid() && !instance.getBlock(potential.add(0, 1, 0)).isSolid() && instance.getBlock(potential.add(0, -1, 0)).isSolid()) {
									potentials.add(potential);
								} else if (potentials.size() >= 1024) {
									break;
								}
							}
						}
					}
					if (potentials.isEmpty()) return; // TODO: do some fail over?
					player.teleport(potentials.get(ThreadLocalRandom.current().nextInt(0, potentials.size())));
					return;
				}
				case PlaySound(SoundEvent sound) -> {
					ViewUtil.viewersAndSelf(player).playSound(Sound.sound().type(sound).build(), player);
					return;
				}
				default -> throw new IllegalArgumentException("Unexpected value: " + effect);
			}
		}
		
		if (stack.has(ItemComponent.SUSPICIOUS_STEW_EFFECTS)) {
			SuspiciousStewEffects effects = stack.get(ItemComponent.SUSPICIOUS_STEW_EFFECTS);
			assert effects != null;
			for (SuspiciousStewEffects.Effect effect : effects.effects()) {
				player.addEffect(new Potion(effect.id(), (byte) 0, effect.durationTicks(), PotionFlags.defaultFlags()));
			}
		}
		
//		ItemStack leftOver = consumable.usingConvertsTo();
//		
//		onEat(player, stack);
//		if (leftOver.isAir()) leftOver = getUsingConvertsTo(stack);
//		
//		if (player.getGameMode() != GameMode.CREATIVE) {
//			if (leftOver != null && !leftOver.isAir()) {
//				if (stack.amount() == 1) {
//					player.setItemInHand(hand, leftOver);
//				} else {
//					player.setItemInHand(hand, stack.withAmount(stack.amount() - 1));
//					player.getInventory().addItemStack(leftOver);
//				}
//			} else {
//				player.setItemInHand(hand, stack.withAmount(stack.amount() - 1));
//			}
//		}
	}
	
	@Override
	public void addFood(Player player, int food, float saturation) {
		player.setFood(Math.min(food + player.getFood(), 20));
		player.setFoodSaturation(Math.min(player.getFoodSaturation() + saturation, player.getFood()));
	}
	
	@Override
	public void eat(Player player, int food, float saturationModifier) {
		addFood(player, food, (float) food * saturationModifier * 2.0f);
	}
	
	@Override
	public void eat(Player player, ItemStack stack) {
		Food foodComponent = stack.get(ItemComponent.FOOD);
		if (foodComponent == null) return;
		addFood(player, foodComponent.nutrition(), foodComponent.saturationModifier());
	}
	
	@Override
	public void applySaturationEffect(Player player, int amplifier) {
		eat(player, amplifier + 1, 1.0f);
	}
	
	protected void tickEatingSounds(Player player) {
		ItemStack stack = player.getItemInHand(Objects.requireNonNull(player.getItemUseHand()));
		
		Consumable component = stack.get(ItemComponent.CONSUMABLE);
		if (component == null) return;
		
		long useTime = getUseTime(stack.material(), component);
		long usedTicks = player.getCurrentItemUseTime();
		long remainingUseTicks = useTime - usedTicks;
		
		boolean canTrigger = component.consumeTicks() < 32 || remainingUseTicks <= useTime - 7;
		boolean shouldTrigger = canTrigger && remainingUseTicks % 4 == 0;
		if (!shouldTrigger) return;
		
		triggerEatingSound(player, stack.material());
	}
	
	protected void triggerEatingSound(Player player, Material material) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		
		if (material == Material.HONEY_BOTTLE || material == Material.MILK_BUCKET) { // Drinking
			SoundEvent soundEvent = material == Material.HONEY_BOTTLE ?
					SoundEvent.ITEM_HONEY_BOTTLE_DRINK : SoundEvent.ENTITY_GENERIC_DRINK;
			player.getViewersAsAudience().playSound(Sound.sound(
					soundEvent, Sound.Source.PLAYER,
					0.5f, random.nextFloat() * 0.1f + 0.9f
			), player);
		} else { // Eating
			player.getViewersAsAudience().playSound(Sound.sound(
					SoundEvent.ENTITY_GENERIC_EAT, Sound.Source.PLAYER,
					0.5f + 0.5f * random.nextInt(2),
					(random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f
			), player);
		}
	}
	
	protected static final ItemStack EMPTY_BUCKET = ItemStack.of(Material.BUCKET);
	protected static final ItemStack EMPTY_BOTTLE = ItemStack.of(Material.GLASS_BOTTLE);
	
	protected @Nullable ItemStack getUsingConvertsTo(ItemStack stack) {
		// Only applies to items of which this has not been defined in the registry
		if (stack.material() == Material.MILK_BUCKET) {
			return EMPTY_BUCKET;
		} else if (stack.material() == Material.HONEY_BOTTLE) {
			return EMPTY_BOTTLE;
		}
		
		return null;
	}
	
	protected void onEat(Player player, ItemStack stack) {
		if (stack.material() == Material.MILK_BUCKET) {
			player.clearEffects();
		} else if (stack.material() == Material.HONEY_BOTTLE) {
			player.removeEffect(PotionEffect.POISON);
		} else if (stack.material() == Material.CHORUS_FRUIT) {
			ChorusFruitUtil.tryChorusTeleport(player, itemCooldownFeature);
		}
	}
	
	protected int getUseTime(@NotNull Material material, @NotNull Consumable component) {
		if (material == Material.HONEY_BOTTLE) return 40;
		return component.consumeTicks();
	}
}

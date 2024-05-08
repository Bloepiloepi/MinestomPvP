package io.github.togar2.pvp.food;

import io.github.togar2.pvp.config.FoodConfig;
import io.github.togar2.pvp.config.PvPConfig;
import io.github.togar2.pvp.entity.EntityUtils;
import io.github.togar2.pvp.entity.PvpPlayer;
import io.github.togar2.pvp.entity.Tracker;
import io.github.togar2.pvp.utils.ViewUtil;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.item.ItemUpdateStateEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerPreEatEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class FoodListener {
	
	public static EventNode<PlayerInstanceEvent> events(FoodConfig config) {
		EventNode<PlayerInstanceEvent> node = EventNode.type("food-events", PvPConfig.PLAYER_INSTANCE_FILTER);
		
		node.addListener(PlayerTickEvent.class, event -> {
			if (event.getPlayer().isOnline()) HungerManager.update(event.getPlayer(), config);
		});
		
		node.addListener(EventListener.builder(PlayerPreEatEvent.class).handler(event -> {
			if (!config.isFoodEnabled()) {
				event.setCancelled(true);
				return;
			}
			
			FoodComponent foodComponent = FoodComponents.fromMaterial(event.getItemStack().material());
			
			//If no food, or if the players hunger is full and the food is not always edible, cancel
			if (foodComponent == null || (!event.getPlayer().isCreative()
					&& !foodComponent.isAlwaysEdible() && event.getPlayer().getFood() == 20)) {
				event.setCancelled(true);
				return;
			}
			
			event.setEatingTime((long) getUseTime(foodComponent) * MinecraftServer.TICK_MS);
		}).filter(event -> event.getItemStack().material().isFood()).build());
		
		node.addListener(EventListener.builder(ItemUpdateStateEvent.class).handler(event -> {
			if (!event.getPlayer().isEating()) return; // Temporary hack, waiting on Minestom PR #2128

			Player player = event.getPlayer();
			ItemStack stack = event.getItemStack();
			HungerManager.eat(player, stack.material());
			
			FoodComponent component = FoodComponents.fromMaterial(stack.material());
			assert component != null;
			ThreadLocalRandom random = ThreadLocalRandom.current();
			
			if (config.isFoodSoundsEnabled()) {
				triggerEatingSound(player, component);
				
				if (!component.isDrink() || event.getItemStack().material() == Material.HONEY_BOTTLE) {
					ViewUtil.viewersAndSelf(player).playSound(Sound.sound(
							SoundEvent.ENTITY_PLAYER_BURP, Sound.Source.PLAYER,
							0.5f, random.nextFloat() * 0.1f + 0.9f
					), player);
				}
			}
			
			List<FoodComponent.FoodEffect> effectList = component.getFoodEffects();
			
			for (FoodComponent.FoodEffect effect : effectList) {
				if (random.nextFloat() < effect.chance()) {
					player.addEffect(effect.potion());
				}
			}
			
			if (component.getBehaviour() != null) component.getBehaviour().onEat(player, stack);
			
			if (!player.isCreative()) {
				ItemStack leftOver = component.getBehaviour() != null ? component.getBehaviour().getLeftOver() : null;
				if (leftOver != null) {
					if (stack.amount() == 1) {
						player.setItemInHand(event.getHand(), leftOver);
					} else {
						player.setItemInHand(event.getHand(), stack.withAmount(stack.amount() - 1));
						player.getInventory().addItemStack(leftOver);
					}
				} else {
					event.getPlayer().setItemInHand(event.getHand(), stack.withAmount(stack.amount() - 1));
				}
			}
		}).filter(event -> event.getItemStack().material().isFood()).build()); //May also be a potion
		
		if (config.isFoodSoundsEnabled()) node.addListener(PlayerTickEvent.class, event -> {
			Player player = event.getPlayer();
			if (player.isSilent() || !player.isEating()) return;
			
			tickEatingSounds(player);
		});
		
		if (config.isBlockBreakExhaustionEnabled()) node.addListener(EventListener.builder(PlayerBlockBreakEvent.class)
				.handler(event -> EntityUtils.addExhaustion(event.getPlayer(), config.isLegacy() ? 0.025F : 0.005F))
				.build());
		
		node.addListener(EventListener.builder(PlayerMoveEvent.class).handler(event -> {
			Player player = event.getPlayer();
			
			double xDiff = event.getNewPosition().x() - player.getPosition().x();
			double yDiff = event.getNewPosition().y() - player.getPosition().y();
			double zDiff = event.getNewPosition().z() - player.getPosition().z();
			
			//Check if movement was a jump
			if (yDiff > 0.0D && player.isOnGround()) {
				if (config.isMoveExhaustionEnabled()) {
					if (player.isSprinting()) {
						EntityUtils.addExhaustion(player, config.isLegacy() ? 0.8F : 0.2F);
					} else {
						EntityUtils.addExhaustion(player, config.isLegacy() ? 0.2F : 0.05F);
					}
				}
				
				if (player instanceof PvpPlayer custom)
					custom.jump(); //Velocity change
			}
			
			if (config.isMoveExhaustionEnabled()) {
				if (player.isOnGround()) {
					int l = (int) Math.round(Math.sqrt(xDiff * xDiff + zDiff * zDiff) * 100.0F);
					if (l > 0) {
						EntityUtils.addExhaustion(player, (player.isSprinting() ? 0.1F : 0.0F) * (float) l * 0.01F);
					}
				} else {
					if (Objects.requireNonNull(player.getInstance()).getBlock(player.getPosition()) == Block.WATER) {
						int l = (int) Math.round(Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff) * 100.0F);
						if (l > 0) {
							EntityUtils.addExhaustion(player, 0.01F * (float) l * 0.01F);
						}
					}
				}
			}
		}).build());
		
		return node;
	}
	
	public static void tickEatingSounds(Player player) {
		ItemStack stack = player.getItemInHand(Objects.requireNonNull(player.getEatingHand()));
		
		FoodComponent component = FoodComponents.fromMaterial(stack.material());
		if (component == null) return;
		
		long useTime = getUseTime(component);
		long usedDuration = System.currentTimeMillis() - player.getTag(Tracker.ITEM_USE_START_TIME);
		long usedTicks = usedDuration / MinecraftServer.TICK_MS;
		long remainingUseTicks = useTime - usedTicks;
		
		boolean canTrigger = component.isSnack() || remainingUseTicks <= useTime - 7;
		boolean shouldTrigger = canTrigger && remainingUseTicks % 4 == 0;
		if (!shouldTrigger) return;
		
		triggerEatingSound(player, component);
	}
	
	public static void triggerEatingSound(Player player, @Nullable FoodComponent component) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		
		if (component == null || component.isDrink()) { // null = potion
			SoundEvent soundEvent = component != null ? component.getDrinkingSound() : SoundEvent.ENTITY_GENERIC_DRINK;
			player.getViewersAsAudience().playSound(Sound.sound(
					soundEvent, Sound.Source.PLAYER,
					0.5f, random.nextFloat() * 0.1f + 0.9f
			));
		} else {
			player.getViewersAsAudience().playSound(Sound.sound(
					component.getEatingSound(), Sound.Source.PLAYER,
					0.5f + 0.5f * random.nextInt(2),
					(random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f
			));
		}
	}
	
	private static int getUseTime(@NotNull FoodComponent foodComponent) {
		if (foodComponent.getMaterial() == Material.HONEY_BOTTLE) return 40;
		return foodComponent.isSnack() ? 16 : 32;
	}
}

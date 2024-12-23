package io.github.togar2.pvp.test;

import io.github.togar2.pvp.MinestomPvP;
import io.github.togar2.pvp.feature.CombatFeatures;
import io.github.togar2.pvp.feature.FeatureType;
import io.github.togar2.pvp.test.commands.Commands;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.EntityDamage;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.*;
import net.minestom.server.extras.lan.OpenToLAN;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.common.KeepAlivePacket;
import net.minestom.server.network.packet.server.play.*;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.utils.time.TimeUnit;
import net.minestom.server.world.DimensionType;

import java.util.Optional;

public class PvpTest {
	public static void main(String[] args) {
		MinecraftServer server = MinecraftServer.init();
		MinestomPvP.init();
		//MinestomFluids.init();
		//VelocityProxy.enable("tj7MulOtnIDe");
		
		DimensionType fullbright = DimensionType.builder().ambientLight(1.0f).respawnAnchorWorks(true).build();
		DynamicRegistry.Key<DimensionType> fullbrightKey =
				MinecraftServer.getDimensionTypeRegistry().register(NamespaceID.from("idk"), fullbright);
		
		Instance instance = MinecraftServer.getInstanceManager().createInstanceContainer(fullbrightKey);
		instance.setGenerator(new DemoGenerator());
		instance.enableAutoChunkLoad(true);
		
		Pos spawn = new Pos(0, 60, 0);
		MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
			event.setSpawningInstance(instance);
			event.getPlayer().setRespawnPoint(spawn);
			
			EntityCreature entity = new EntityCreature(EntityType.ZOMBIE);
			entity.setInstance(instance, spawn);
			entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(500);
			entity.heal();
			
			MinecraftServer.getSchedulerManager().buildTask(() -> {
				Optional<Player> player = MinecraftServer.getConnectionManager()
						.getOnlinePlayers().stream()
						.filter(p -> p.getDistanceSquared(entity) < 6)
						.findAny();
				
				if (player.isPresent()) {
					if (!player.get().damage(new EntityDamage(entity, 1.0f)))
						return;
					
//					LegacyKnockbackEvent legacyKnockbackEvent = new LegacyKnockbackEvent(player.get(), entity, true);
//					EventDispatcher.callCancellable(legacyKnockbackEvent, () -> {
//						LegacyKnockbackSettings settings = legacyKnockbackEvent.getSettings();
//
//						player.get().setVelocity(player.get().getVelocity().add(
//								-Math.sin(entity.getPosition().yaw() * 3.1415927F / 180.0F) * 1 * settings.extraHorizontal(),
//								settings.extraVertical(),
//								Math.cos(entity.getPosition().yaw() * 3.1415927F / 180.0F) * 1 * settings.extraHorizontal()
//						));
//					});
//					EntityKnockbackEvent entityKnockbackEvent = new EntityKnockbackEvent(player.get(), entity, true, false, 1 * 0.5F);
//					EventDispatcher.callCancellable(entityKnockbackEvent, () -> {
//						float strength = entityKnockbackEvent.getStrength();
//						player.get().takeKnockback(strength, Math.sin(Math.toRadians(entity.getPosition().yaw())), -Math.cos(Math.toRadians(entity.getPosition().yaw())));
//					});
//
//					if (player.get() instanceof CustomPlayer customPlayer)
//						customPlayer.sendImmediateVelocityUpdate();
				}
				
				//event.getPlayer().setFood(20);
			}).repeat(20, TimeUnit.SERVER_TICK).schedule();
		});
		
		MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, event -> {
			event.getPlayer().setGameMode(GameMode.SURVIVAL);
			//PvpExtension.setLegacyAttack(event.getPlayer(), true);
			
			event.getPlayer().setPermissionLevel(4);
			//event.getPlayer().addEffect(new Potion(PotionEffect.REGENERATION, (byte) 10, CustomPotionEffect.PERMANENT));
		});
		
		MinecraftServer.getGlobalEventHandler().addListener(PlayerPacketOutEvent.class, event -> {
			if (event.getPacket() instanceof ActionBarPacket) return;
			if (event.getPacket() instanceof UpdateHealthPacket) return;
			if (event.getPacket() instanceof TimeUpdatePacket) return;
			if (event.getPacket() instanceof PlayerInfoUpdatePacket) return;
			if (event.getPacket() instanceof ChunkDataPacket) return;
			if (event.getPacket() instanceof KeepAlivePacket) return;
			if (event.getPacket() instanceof SetSlotPacket) return;
			if (event.getPacket() instanceof WindowItemsPacket) return;
			if (event.getPacket() instanceof EntitySoundEffectPacket) return;
			if (event.getPacket() instanceof CollectItemPacket) return;
			if (event.getPacket() instanceof DestroyEntitiesPacket) return;
			//System.out.println(event.getPacket());
		});
		
		MinecraftServer.getCommandManager().register(new Command("shoot") {{
			setDefaultExecutor((sender, args) -> {
				if (sender instanceof Player player) {
					EntityProjectile projectile = new EntityProjectile(player, EntityType.ARROW);
					projectile.setInstance(player.getInstance(), player.getPosition().add(0, player.getEyeHeight(), 0));
					projectile.shoot(player.getPosition().add(player.getPosition().direction()).add(0, 2, 0), 1, 0);
					player.sendMessage("oui");
				}
			});
		}});
		
//		LegacyKnockbackSettings settings = LegacyKnockbackSettings.builder()
//				.horizontal(0.35)
//				.vertical(0.4)
//				.verticalLimit(0.4)
//				.extraHorizontal(0.45)
//				.extraVertical(0.1)
//				.build();
//		MinecraftServer.getGlobalEventHandler().addListener(LegacyKnockbackEvent.class,
//				event -> event.setSettings(settings));
		
		instance.setExplosionSupplier(CombatFeatures.legacyVanilla().get(FeatureType.EXPLOSION).getExplosionSupplier());
		
		GlobalEventHandler eventHandler = MinecraftServer.getGlobalEventHandler();
		eventHandler.addChild(CombatFeatures.legacyVanilla().createNode());
		//eventHandler.addChild(PvPConfig.defaultBuilder()
		//		//.potion(PotionConfig.legacyBuilder().drinking(false))
		//		.build().createNode()
		//);
		
		//eventHandler.addChild(MinestomFluids.events());
		
		eventHandler.addListener(PlayerUseItemOnBlockEvent.class, event -> {
			if (event.getItemStack().material() == Material.WATER_BUCKET) {
				event.getInstance().setBlock(event.getPosition().relative(event.getBlockFace()), Block.WATER);
			}
			event.getPlayer().getInventory().update();
		});
		
		MinecraftServer.getCommandManager().register(new Command("test") {{
			setDefaultExecutor((sender, args) -> {
				if (sender instanceof Player player) {
					player.refreshOnGround(true);
					player.takeKnockback(0.4f, -0.00825, -0.00716738);
				}
			});
		}});
		
		eventHandler.addListener(PlayerTickEvent.class, event -> {
			event.getPlayer().sendActionBar(Component.text(event.getPlayer().getVelocity().toString()));
		});
		
		Commands.init();
		
		OpenToLAN.open();
		
		server.start("localhost", 25565);
	}
}

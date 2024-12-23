package io.github.togar2.pvp.test.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentEnum;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

public class GameModeCommand extends Command {
	
	public GameModeCommand() {
		super("gamemode", "gm");
		
		ArgumentEntity playerArgument = ArgumentType.Entity("player").onlyPlayers(true).singleEntity(true);
		ArgumentEnum<GameMode> mode = ArgumentType.Enum("gamemode", GameMode.class);
		mode.setFormat(ArgumentEnum.Format.LOWER_CASED);
		
		addSyntax((sender, args) -> {
			if (!(sender instanceof Player player)) return;
			player.setGameMode(args.get(mode));
		}, mode);
		
		addSyntax((sender, args) -> {
			Player player = args.get(playerArgument).findFirstPlayer(sender);
			
			if (player == null) {
				sender.sendMessage(Component.text("That player does not exist.", NamedTextColor.RED));
			} else {
				player.setGameMode(args.get(mode));
			}
		}, mode, playerArgument);
	}
}

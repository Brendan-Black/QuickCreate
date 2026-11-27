package black.quickcreate;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

/**
 * The /qc commands, for operators:
 *
 * <pre>
 * /qc spawn &lt;mob&gt; [count] [players]               a crowd around each player
 * /qc fountain &lt;mob&gt; [count] [players]            one a tick, thrown up from above each player
 * /qc pets cats|dogs [count] [players] [babies]   tamed to each player, immortal, every coat
 * /qc immortal [players]
 * /qc heal [players]
 * </pre>
 *
 * Players default to whoever runs the command.
 */
final class QcCommands {
	private static final String MOB = "mob", COUNT = "count", PLAYERS = "players", KIND = "kind";

	/** The commands under /qc, by the word a player types. */
	enum Subcommand {
		SPAWN, FOUNTAIN, PETS, IMMORTAL, HEAL;

		String word() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	private QcCommands() {}

	static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
		dispatcher.register(Commands.literal(CommandInstall.ROOT)
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal(Subcommand.SPAWN.word()).then(mobCount(context, QcCommands::spawn)))
			.then(Commands.literal(Subcommand.FOUNTAIN.word()).then(mobCount(context, QcCommands::fountain)))
			.then(Commands.literal(Subcommand.PETS.word()).then(Commands.argument(KIND, StringArgumentType.word())
				.suggests((_, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Pets.Kind.values()).map(Pets.Kind::word).toList(), b))
				.executes(c -> pets(c, 10, self(c), false))
				.then(Commands.argument(COUNT, IntegerArgumentType.integer(1, Layout.MAX_COUNT))
					.executes(c -> pets(c, count(c), self(c), false))
					.then(Commands.literal("babies").executes(c -> pets(c, count(c), self(c), true)))
					.then(Commands.argument(PLAYERS, EntityArgument.players())
						.executes(c -> pets(c, count(c), EntityArgument.getPlayers(c, PLAYERS), false))
						.then(Commands.literal("babies").executes(c -> pets(c, count(c), EntityArgument.getPlayers(c, PLAYERS), true)))))))
			.then(playersOnly(Subcommand.IMMORTAL, Buffs::immortal, "immortal for good (the void and /kill still kill)"))
			.then(playersOnly(Subcommand.HEAL, Buffs::heal, "healed and fed")));
	}

	/** What a mob command does with its mob, count and players. */
	@FunctionalInterface
	private interface MobAction {
		int run(CommandContext<CommandSourceStack> c, EntityType<?> type, int count, Collection<ServerPlayer> players) throws CommandSyntaxException;
	}

	private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ?> mobCount(CommandBuildContext context, MobAction action) {
		return Commands.argument(MOB, ResourceArgument.resource(context, Registries.ENTITY_TYPE))
			.executes(c -> action.run(c, mob(c), 1, self(c)))
			.then(Commands.argument(COUNT, IntegerArgumentType.integer(1, Layout.MAX_COUNT))
				.executes(c -> action.run(c, mob(c), count(c), self(c)))
				.then(Commands.argument(PLAYERS, EntityArgument.players())
					.executes(c -> action.run(c, mob(c), count(c), EntityArgument.getPlayers(c, PLAYERS)))));
	}

	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> playersOnly(Subcommand name, Consumer<ServerPlayer> buff,
		String done) {
		return Commands.literal(name.word())
			.executes(c -> buff(c, self(c), buff, done))
			.then(Commands.argument(PLAYERS, EntityArgument.players()).executes(c -> buff(c, EntityArgument.getPlayers(c, PLAYERS), buff, done)));
	}

	private static int spawn(CommandContext<CommandSourceStack> c, EntityType<?> type, int count, Collection<ServerPlayer> players)
		throws CommandSyntaxException {
		allowed(count, players.size());
		int made = 0;
		for (ServerPlayer p : players) {
			made += Spawner.crowd(p.level(), type, p.position(), count);
		}
		int total = made;
		c.getSource().sendSuccess(() -> Component.literal("Spawned " + total + " " + name(type) + " around " + who(players)), true);
		return total;
	}

	private static int fountain(CommandContext<CommandSourceStack> c, EntityType<?> type, int count, Collection<ServerPlayer> players)
		throws CommandSyntaxException {
		allowed(count, players.size());
		for (ServerPlayer p : players) {
			ServerLevel level = p.level();
			Spawner.fountain(level, p, _ -> Spawner.one(level, type, p.blockPosition()), count);
		}
		c.getSource().sendSuccess(() -> Component.literal("A fountain of " + count + " " + name(type) + " over " + who(players)), true);
		return count * players.size();
	}

	private static int pets(CommandContext<CommandSourceStack> c, int count, Collection<ServerPlayer> players, boolean babies) throws CommandSyntaxException {
		String typed = StringArgumentType.getString(c, KIND);
		Pets.Kind kind = Pets.Kind.named(typed).orElseThrow(() -> refuse("pets are cats or dogs, not " + typed));
		allowed(count, players.size());
		for (ServerPlayer p : players) {
			ServerLevel level = p.level();
			Spawner.fountain(level, p, n -> Pets.make(level, kind, p, babies, n, level.getRandom()), count);
		}
		String what = count + " " + (babies ? kind.babies() : kind.word());
		c.getSource().sendSuccess(() -> Component.literal("A fountain of " + what + ", tamed and immortal, for " + who(players)), true);
		return count * players.size();
	}

	private static int buff(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players, Consumer<ServerPlayer> buff, String done) {
		players.forEach(buff);
		c.getSource().sendSuccess(() -> Component.literal(who(players) + ": " + done), true);
		return players.size();
	}

	private static EntityType<?> mob(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		return ResourceArgument.getSummonableEntityType(c, MOB).value();
	}

	private static int count(CommandContext<CommandSourceStack> c) {
		return IntegerArgumentType.getInteger(c, COUNT);
	}

	private static Collection<ServerPlayer> self(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		return List.of(c.getSource().getPlayerOrException());
	}

	private static void allowed(int count, int players) throws CommandSyntaxException {
		var refusal = Layout.refusal(count, players);
		if (refusal.isPresent()) {
			throw refuse(refusal.get());
		}
	}

	private static CommandSyntaxException refuse(String why) {
		return new SimpleCommandExceptionType(Component.literal(why)).create();
	}

	private static String name(EntityType<?> type) {
		return type.getDescription().getString();
	}

	private static String who(Collection<ServerPlayer> players) {
		return players.size() == 1 ? players.iterator().next().getName().getString() : players.size() + " players";
	}
}

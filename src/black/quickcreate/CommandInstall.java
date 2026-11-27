package black.quickcreate;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

/**
 * Puts /qc into the server's commands, and back again after /reload, which rebuilds them without it. Graft has no
 * command event yet, so this is the one place that knows; when Graft gets one, only this class changes.
 */
final class CommandInstall {
	static final String ROOT = "qc";

	private CommandInstall() {}

	/** Adds /qc if the server's commands lack it, and sends players the new command list. True if it added it. */
	static boolean ensure(MinecraftServer server) {
		CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
		if (dispatcher.getRoot().getChild(ROOT) != null) {
			return false;
		}
		CommandBuildContext context = CommandBuildContext.simple(server.registryAccess(), server.getWorldData().enabledFeatures());
		QcCommands.register(dispatcher, context);
		// While the server is starting there's no player list yet, and nobody to tell; after a /reload, players need the
		// new command list for /qc to complete in their chat.
		PlayerList players = server.getPlayerList();
		if (players != null) {
			for (ServerPlayer player : players.getPlayers()) {
				server.getCommands().sendCommands(player);
			}
		}
		return true;
	}
}

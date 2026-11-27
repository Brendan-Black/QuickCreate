package black.quickcreate;

import black.graft.Mod;
import black.graft.Setup;
import black.graft.event.ServerStarting;
import black.graft.event.ServerStopping;
import black.graft.event.ServerTick;

/**
 * QuickCreate: the /qc commands, on the server only. Players' games don't need it, since everything it makes is
 * vanilla.
 */
public final class QuickCreate implements Mod {
	@Override
	public void initialize(Setup setup) {
		setup.events().listen(ServerStarting.class, e -> CommandInstall.ensure(e.server()));
		// After /reload the server's commands are rebuilt without /qc; the next tick puts it back.
		setup.events().listen(ServerTick.class, e -> {
			CommandInstall.ensure(e.server());
			Spawner.tick(e.server());
		});
		setup.events().listen(ServerStopping.class, _ -> Spawner.clear());
		QuickCreateTests.declare(setup);
	}
}

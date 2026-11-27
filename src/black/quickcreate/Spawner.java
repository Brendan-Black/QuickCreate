package black.quickcreate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Making many mobs: a crowd, all at once, in places of their own around a spot; or a fountain, one per tick, thrown up
 * from above a player. Each mob is made on its own, never by copying others, so a count is exactly that many.
 */
public final class Spawner {
	/** A fountain running: what it makes, who it's on, and how many are left. */
	private static final class Fountain {
		final ServerLevel level;
		final Entity source;
		final IntFunction<Entity> make;
		final int total;
		int made;

		Fountain(ServerLevel level, Entity source, IntFunction<Entity> make, int total) {
			this.level = level;
			this.source = source;
			this.make = make;
			this.total = total;
		}
	}

	private static final List<Fountain> fountains = new ArrayList<>();

	private Spawner() {}

	/**
	 * {@code count} mobs of {@code type} around {@code centre}, each in a place of its own ({@link Layout#crowd}).
	 *
	 * @return how many Minecraft actually made (a spot inside a wall can refuse one)
	 */
	public static int crowd(ServerLevel level, EntityType<?> type, Vec3 centre, int count) {
		int made = 0;
		for (Layout.Offset o : Layout.crowd(count)) {
			BlockPos pos = BlockPos.containing(centre.x + o.x(), centre.y, centre.z + o.z());
			if (type.spawn(level, pos, EntitySpawnReason.COMMAND) != null) {
				made++;
			}
		}
		return made;
	}

	/** A new mob of {@code type}, set up as a natural one would be (gear, variants), not yet in the world. */
	public static Entity one(ServerLevel level, EntityType<?> type, BlockPos at) {
		Entity e = type.create(level, EntitySpawnReason.COMMAND);
		if (e == null) {
			throw new IllegalStateException(type + " can't be made");
		}
		e.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
		if (e instanceof Mob mob) {
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.COMMAND, null);
		}
		return e;
	}

	/**
	 * Starts a fountain over {@code source}: one entity a tick, {@code make.apply(n)} for the nth, until {@code total}.
	 * It follows {@code source} as it moves, and stops early if {@code source} leaves.
	 */
	public static void fountain(ServerLevel level, Entity source, IntFunction<Entity> make, int total) {
		fountains.add(new Fountain(level, source, make, total));
	}

	/** Runs the fountains: one entity from each, every server tick. */
	public static void tick(MinecraftServer server) {
		fountains.removeIf(f -> {
			if (f.source.isRemoved() || f.level.getServer() != server) {
				return true;
			}
			Entity e = f.make.apply(f.made);
			RandomSource random = f.level.getRandom();
			Layout.Launch v = Layout.launch(new java.util.Random(random.nextLong()));
			e.snapTo(f.source.getX(), f.source.getY() + 2.5, f.source.getZ(), random.nextFloat() * 360f, 0f);
			e.setDeltaMovement(v.x(), v.y(), v.z());
			f.level.addFreshEntity(e);
			f.made++;
			return f.made >= f.total;
		});
	}

	/** Stops every fountain, when the server stops. */
	public static void clear() {
		fountains.clear();
	}

	/** How many fountains are running. */
	static int running() {
		return fountains.size();
	}
}

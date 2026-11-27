package black.quickcreate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;

/**
 * Tamed pets: cats or dogs, tamed to their owner, immortal, with every coat in turn and a random collar. Babies stay
 * babies. Making one doesn't add it to the world; the caller places it.
 */
public final class Pets {
	/** The two kinds of pet, with what their adults and their babies are called. */
	public enum Kind {
		CATS("kittens"), DOGS("puppies");

		private final String babies;

		Kind(String babies) {
			this.babies = babies;
		}

		/** The word a player types: cats, dogs. */
		String word() {
			return name().toLowerCase(Locale.ROOT);
		}

		/** What its babies are called: kittens, puppies. */
		String babies() {
			return babies;
		}

		/** The kind a player typed; empty for anything else. */
		static Optional<Kind> named(String typed) {
			return Arrays.stream(values()).filter(k -> k.word().equalsIgnoreCase(typed)).findFirst();
		}
	}

	private Pets() {}

	/**
	 * A new pet of {@code kind}, tamed to {@code owner}. {@code n} picks its coat, so consecutive pets wear different ones.
	 */
	public static TamableAnimal make(ServerLevel level, Kind kind, Player owner, boolean baby, int n, RandomSource random) {
		TamableAnimal pet = switch (kind) {
			case CATS -> {
				Cat cat = EntityTypes.CAT.create(level, EntitySpawnReason.COMMAND);
				if (cat == null) {
					throw new IllegalStateException("Minecraft made no cat");
				}
				cat.setComponent(DataComponents.CAT_VARIANT, nth(level, Registries.CAT_VARIANT, n));
				cat.setComponent(DataComponents.CAT_COLLAR, collar(random));
				yield cat;
			}
			case DOGS -> {
				Wolf wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND);
				if (wolf == null) {
					throw new IllegalStateException("Minecraft made no wolf");
				}
				wolf.setComponent(DataComponents.WOLF_VARIANT, nth(level, Registries.WOLF_VARIANT, n));
				wolf.setComponent(DataComponents.WOLF_COLLAR, collar(random));
				yield wolf;
			}
		};
		pet.tame(owner);
		pet.setPermanentlyInvulnerable(true);
		pet.setPersistenceRequired();
		if (baby) {
			pet.setAge(AgeableMob.BABY_START_AGE);
			lockAge(pet);
		}
		return pet;
	}

	private static DyeColor collar(RandomSource random) {
		DyeColor[] colors = DyeColor.values();
		return colors[random.nextInt(colors.length)];
	}

	/**
	 * Keeps a baby a baby. Minecraft 26.3 sets AgeLocked only through its own item use, so this merges it into the
	 * entity's saved data, as /data merge does.
	 */
	private static void lockAge(TamableAnimal pet) {
		EntityDataAccessor data = new EntityDataAccessor(pet);
		CompoundTag tag = data.getData();
		tag.putBoolean("AgeLocked", true);
		try {
			data.setData(tag);
		} catch (CommandSyntaxException e) {
			// setData refuses only players; a pet is never one.
			throw new IllegalStateException("couldn't lock a pet's age", e);
		}
	}

	/** The {@code n}th entry of a registry, wrapping around, in the registry's own order. */
	private static <T> Holder<T> nth(ServerLevel level, ResourceKey<Registry<T>> registry, int n) {
		List<Holder.Reference<T>> all = level.registryAccess().lookupOrThrow(registry).listElements().toList();
		if (all.isEmpty()) {
			throw new IllegalStateException(registry.identifier() + " is empty");
		}
		return all.get(Math.floorMod(n, all.size()));
	}
}

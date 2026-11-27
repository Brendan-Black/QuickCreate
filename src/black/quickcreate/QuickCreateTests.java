package black.quickcreate;

import black.graft.ContentName;
import black.graft.Setup;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * QuickCreate's game tests, run in a real world by {@code gradle gameTest}. Each has its test instance in
 * {@code data/quickcreate/test_instance/}.
 */
final class QuickCreateTests {
	private static final BlockPos SPOT = new BlockPos(0, 1, 0);

	private QuickCreateTests() {}

	static void declare(Setup setup) {
		// The server starting put /qc in; putting it in again changes nothing.
		setup.gameTest(new ContentName("commands_installed"), helper -> {
			var server = helper.getLevel().getServer();
			CommandInstall.ensure(server);
			var qc = server.getCommands().getDispatcher().getRoot().getChild(CommandInstall.ROOT);
			helper.assertTrue(qc != null, "/qc is a command");
			for (QcCommands.Subcommand sub : QcCommands.Subcommand.values()) {
				helper.assertTrue(qc != null && qc.getChild(sub.word()) != null, "/qc " + sub.word() + " is a command");
			}
			helper.assertFalse(CommandInstall.ensure(server), "a second install adds nothing");
			helper.succeed();
		});

		// A crowd is exactly the count asked for, all in places of their own.
		setup.gameTest(new ContentName("crowd_count"), helper -> {
			ServerLevel level = helper.getLevel();
			Vec3 centre = Vec3.atBottomCenterOf(helper.absolutePos(SPOT));
			int made = Spawner.crowd(level, EntityTypes.PIG, centre, 12);
			helper.assertValueEqual(12, made, "pigs made");
			List<Pig> pigs = level.getEntitiesOfClass(Pig.class, new AABB(centre, centre).inflate(16));
			helper.assertValueEqual(12, pigs.size(), "pigs around the spot");
			helper.succeed();
		});

		// Pets: tamed to their owner, immortal, babies that stay babies, and no two of eleven cats alike.
		setup.gameTest(new ContentName("pets_tamed_varied"), helper -> {
			ServerLevel level = helper.getLevel();
			Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
			Set<String> coats = new HashSet<>();
			for (int n = 0; n < 11; n++) {
				TamableAnimal pet = Pets.make(level, Pets.Kind.CATS, owner, true, n, level.getRandom());
				helper.assertTrue(pet.isTame() && pet.isOwnedBy(owner), "a pet is tamed to its owner");
				helper.assertTrue(pet.isInvulnerable(), "a pet is immortal");
				helper.assertTrue(pet.isBaby() && pet.isAgeLocked(), "a baby stays a baby");
				coats.add(((Cat) pet).getVariant().getRegisteredName());
			}
			helper.assertValueEqual(11, coats.size(), "different coats for eleven cats");
			helper.succeed();
		});

		// A fountain makes one a tick until it has made them all, then stops.
		setup.gameTest(new ContentName("fountain_one_a_tick"), helper -> {
			ServerLevel level = helper.getLevel();
			Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
			owner.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(SPOT)));
			Spawner.fountain(level, owner, n -> Pets.make(level, Pets.Kind.DOGS, owner, false, n, level.getRandom()), 5);
			helper.succeedWhen(() -> {
				var dogs = level.getEntitiesOfClass(TamableAnimal.class, owner.getBoundingBox().inflate(32), p -> p.isOwnedBy(owner));
				helper.assertValueEqual(5, dogs.size(), "dogs from the fountain");
				helper.assertValueEqual(0, Spawner.running(), "fountains still running");
			});
		});

		// Immortal: Resistance V for good, and the rest of the set.
		setup.gameTest(new ContentName("immortal_buffs"), helper -> {
			Player player = helper.makeMockPlayer(GameType.SURVIVAL);
			Buffs.immortal(player);
			MobEffectInstance resistance = player.getEffect(MobEffects.RESISTANCE);
			helper.assertTrue(resistance != null && resistance.getAmplifier() == 4 && resistance.isInfiniteDuration(), "Resistance V, forever");
			helper.assertTrue(player.hasEffect(MobEffects.FIRE_RESISTANCE) && player.hasEffect(MobEffects.REGENERATION), "fire resistance and regeneration");
			player.setHealth(1f);
			Buffs.heal(player);
			helper.assertValueEqual(player.getMaxHealth(), player.getHealth(), "health after a heal");
			helper.succeed();
		});
	}
}

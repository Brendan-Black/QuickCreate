package black.quickcreate;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** Player buffs: immortality that lasts, and a full heal. */
public final class Buffs {
	private record Buff(Holder<MobEffect> effect, int amplifier) {}

	/** Resistance V stops nearly all damage; the rest covers fire, hunger, what still gets through, and drowning. */
	private static final List<Buff> IMMORTAL = List.of(
		new Buff(MobEffects.RESISTANCE, 4),
		new Buff(MobEffects.FIRE_RESISTANCE, 0),
		new Buff(MobEffects.SATURATION, 0),
		new Buff(MobEffects.REGENERATION, 1),
		new Buff(MobEffects.WATER_BREATHING, 0));

	private Buffs() {}

	/** Makes {@code player} as good as immortal, for good. The void and /kill still kill. */
	public static void immortal(Player player) {
		for (Buff b : IMMORTAL) {
			player.addEffect(new MobEffectInstance(b.effect(), MobEffectInstance.INFINITE_DURATION, b.amplifier(), false, false));
		}
	}

	/** Full health, a full food bar, and no more fire. */
	public static void heal(Player player) {
		player.setHealth(player.getMaxHealth());
		player.getFoodData().setFoodLevel(20);
		player.getFoodData().setSaturation(20f);
		player.clearFire();
	}
}

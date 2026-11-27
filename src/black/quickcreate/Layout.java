package black.quickcreate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Where a crowd stands and how a fountain throws, without any Minecraft types, so it can be tested on its own. Every
 * mob in a crowd gets a place of its own: no two places are closer than {@link #SPACING}.
 */
public final class Layout {
	/** The most of anything one command makes for one player. */
	public static final int MAX_COUNT = 500;
	/** The most one command makes in all, across every player it names. */
	public static final int MAX_TOTAL = 1000;
	/** Blocks between neighbours in a crowd. */
	public static final double SPACING = 1.25;

	private Layout() {}

	/** An offset from a centre, in blocks. */
	public record Offset(double x, double z) {
		double distance() {
			return Math.hypot(x, z);
		}
	}

	/** A launch speed, in blocks per tick. */
	public record Launch(double x, double y, double z) {}

	/**
	 * Places for {@code count} mobs: the centre, then rings around it, each ring as full as {@link #SPACING} allows.
	 *
	 * @throws IllegalArgumentException if count is below 1 or above {@link #MAX_COUNT}
	 */
	public static List<Offset> crowd(int count) {
		if (count < 1 || count > MAX_COUNT) {
			throw new IllegalArgumentException("a crowd is 1 to " + MAX_COUNT + " mobs, not " + count);
		}
		List<Offset> places = new ArrayList<>(count);
		places.add(new Offset(0, 0));
		for (int ring = 1; places.size() < count; ring++) {
			double radius = ring * SPACING;
			int slots = (int) Math.floor(2 * Math.PI * radius / SPACING);
			for (int i = 0; i < slots && places.size() < count; i++) {
				double a = 2 * Math.PI * i / slots;
				places.add(new Offset(radius * Math.cos(a), radius * Math.sin(a)));
			}
		}
		return List.copyOf(places);
	}

	/** A fountain's throw: mostly up, a little to any side. */
	public static Launch launch(RandomGenerator random) {
		return new Launch(random.nextDouble(-0.35, 0.35), random.nextDouble(0.8, 1.1), random.nextDouble(-0.35, 0.35));
	}

	/**
	 * Checks what one command asks for: {@code count} each for {@code players} players.
	 *
	 * @return empty when it's allowed, else why not, for the player to read
	 */
	public static Optional<String> refusal(int count, int players) {
		if (players < 1) {
			return Optional.of("name at least one player");
		}
		if (count < 1 || count > MAX_COUNT) {
			return Optional.of("the count is 1 to " + MAX_COUNT);
		}
		long total = (long) count * players;
		if (total > MAX_TOTAL) {
			return Optional.of(count + " each for " + players + " players is " + total + "; one command makes at most " + MAX_TOTAL);
		}
		return Optional.empty();
	}
}

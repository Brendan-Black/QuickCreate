package black.quickcreate;

import static black.quickcreate.Check.eq;
import static black.quickcreate.Check.fails;
import static black.quickcreate.Check.ok;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/** Crowd places, fountain throws and command limits. */
final class LayoutTest {
	private LayoutTest() {}

	static void run() {
		for (int count : new int[] {1, 2, 7, 50, Layout.MAX_COUNT}) {
			List<Layout.Offset> places = Layout.crowd(count);
			eq(count, places.size(), "a crowd of " + count + " has that many places");
			double closest = Double.MAX_VALUE;
			for (int i = 0; i < places.size(); i++) {
				for (int j = i + 1; j < places.size(); j++) {
					closest = Math.min(closest, Math.hypot(places.get(i).x() - places.get(j).x(), places.get(i).z() - places.get(j).z()));
				}
			}
			ok(count == 1 || closest >= Layout.SPACING * 0.99, "no two of " + count + " closer than the spacing (closest " + closest + ")");
		}
		eq(0.0, Layout.crowd(1).get(0).distance(), "a crowd of one stands on the spot");
		ok(Layout.crowd(Layout.MAX_COUNT).stream().allMatch(o -> o.distance() < 20), "even the biggest crowd fits within 20 blocks");
		fails(IllegalArgumentException.class, () -> Layout.crowd(0), "a crowd of none");
		fails(IllegalArgumentException.class, () -> Layout.crowd(Layout.MAX_COUNT + 1), "a crowd over the limit");

		Random random = new Random(7);
		for (int i = 0; i < 200; i++) {
			Layout.Launch v = Layout.launch(random);
			ok(v.y() >= 0.8 && v.y() < 1.1 && Math.abs(v.x()) < 0.35 && Math.abs(v.z()) < 0.35, "a throw goes up, a little sideways");
		}

		eq(Optional.empty(), Layout.refusal(100, 4), "100 each for 4 players is allowed");
		eq(Optional.of("name at least one player"), Layout.refusal(5, 0), "no players");
		eq(Optional.of("the count is 1 to 500"), Layout.refusal(0, 1), "a count of none");
		eq(Optional.of("400 each for 3 players is 1200; one command makes at most 1000"), Layout.refusal(400, 3), "too many in all");
	}
}

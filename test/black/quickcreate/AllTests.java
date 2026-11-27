package black.quickcreate;

/** Runs every test group; exits non-zero if any check failed. The game tests run separately, with `gradle gameTest`. */
public final class AllTests {
	private AllTests() {}

	public static void main(String[] args) {
		Check.group("layout (crowd places, fountain throws, limits)", LayoutTest::run);
		System.exit(Check.summary());
	}
}

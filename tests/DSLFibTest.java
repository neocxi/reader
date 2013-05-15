import org.junit.*;
import static org.junit.Assert.*;
import helloworld.*;

public class FibTest {

	@Test
	public void testPass() {
		``
		`Fib.calculateFib(10)` should:
			call Fib#calculateFib(int);
			return `55`.
		``
	}

	@Test
	public void testFail() {
		``
		`Fib.calculateFib(8)` should:
			return `55`.

		Class Fib should:
			be public;
			have static int calculateFib(int).
		``
	}


	public static void main(String ignored[]) {
		org.junit.runner.JUnitCore.main("FibTest");
	}
}
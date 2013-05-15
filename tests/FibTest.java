import org.junit.*;
import static org.junit.Assert.*;
import helloworld.*;

public class FibTest {
	public int xx = 0;

	@Test
	public void testPass() {
		``
		`Fib.calculateFib(10)` should:
			not call Fib#calculateFib(int);
			return `55`.
		``
	}

	@Test
	public void testFail() {
		``
		`Fib.calculateFib(8)` should:
			return `55`.
		Class Fib should:
			be private;
			have public static int inter(int);
			have public int ff;
			extend Exception;
			implement Interface.
		``
	}

	public static void main(String ignored[]) {
		org.junit.runner.JUnitCore.main("FibTest");
	}
}
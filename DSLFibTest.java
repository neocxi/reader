package tests;

import org.junit.*;
import static org.junit.Assert.*;
import helloworld.*;

public class FibTest {

	@Test
	public void testPass() {
		LogHelper.setupEnvironment("<Common.Test object at 0x10d302f10>", new String[] { "Fib#calculateFib(int)", "Fib#foo()" });
assertEquals(Fib.calculateFib(10), 55);
LogHelper.destroyEnvironment("<Common.Test object at 0x10d302f10>");

	}

	@Test
	public void testFail() {
		LogHelper.setupEnvironment("<Common.Test object at 0x10d304090>", new String[] {  });
assertEquals(Fib.calculateFib(8), 55);
LogHelper.destroyEnvironment("<Common.Test object at 0x10d304090>");

	}

	public static void main(String ignored[]) {
		org.junit.runner.JUnitCore.main("tests.FibTest");
	}
}
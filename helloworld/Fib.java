package helloworld;

public class Fib {
	public int peter = 1;
	public static int calculateFib(int nth) {
		// sanitize
		if (nth < 0) {
			return -1;
		}

		if (nth == 0) {
			return 0;
		} else if (nth == 1) {
			return 1;
		}

		return calculateFib(nth - 1) + calculateFib(nth - 2);
	}

	public static void main(String argvs[]) {
		System.out.println("Fib(1) = " + calculateFib(1));
		System.out.println("Fib(10) = " + calculateFib(10));
	}
}
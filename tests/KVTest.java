import org.junit.*;
import static org.junit.Assert.*;
import cs162.*;

public class KVTest {

	private KVServer ks;

	@Before
	public void setup() {
		this.ks = new KVServer(10, 10);
	}


	@Test
	public void dynamicTestSuccessful() throws KVException {
		this.ks.put("someKey", "someValue");

		``
		define pattern cachedGetPattern(backend):
			call KVCache#get(String);
			not call backend#get(String).

		define pattern cachedFastDelete(backend):
			follow pattern cachedGetPattern(backend);
			call KVCache#del(String) before backend#del(String);
			call backend#del(String).

		`this.ks.get("someKey")` should:
			follow pattern cachedGetPattern(KVStore);
			return `"someValue"`.

		`this.ks.put("someKey", "someNewVal")` should:
			call KVCache#put(String,String) before KVStore#put(String,String);
			call KVStore#put(String,String) less than 2 times.

		`this.ks.del("someKey")` should:
			follow pattern cachedFastDelete(KVStore).
		``
	}

	@Test
	public void staticTest() throws KVException {
		``
		Class KVException should:
			be public;
			have static final long garbageField;
			have public final KVMessage getMsg().
		``
	}





	public static void main(String ignored[]) {
		org.junit.runner.JUnitCore.main("KVTest");
	}
}
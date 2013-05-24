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

		LogHelper.setupEnvironment("this.ks.get(someKey)#2", new String[] { "KVCache#get(String)", "KVStore#get(String)" });
assertEquals(this.ks.get("someKey"), "someValue");
LogHelper.destroyEnvironment("this.ks.get(someKey)#2");
LogHelper.setupEnvironment("this.ks.put(someKey, someNewVal)#3", new String[] { "KVCache#put(String,String)", "KVStore#put(String,String)", "KVStore#put(String,String)" });
this.ks.put("someKey", "someNewVal");
LogHelper.destroyEnvironment("this.ks.put(someKey, someNewVal)#3");
LogHelper.setupEnvironment("this.ks.del(someKey)#4", new String[] { "KVCache#get(String)", "KVStore#get(String)", "KVCache#del(String)", "KVStore#del(String)", "KVStore#del(String)" });
this.ks.del("someKey");
LogHelper.destroyEnvironment("this.ks.del(someKey)#4");

	}

	@Test
	public void staticTest() throws KVException {
		
	}





	public static void main(String ignored[]) {
		org.junit.runner.JUnitCore.main("KVTest");
	}
}
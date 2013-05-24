package cs162;

import java.io.IOException;
import java.util.ArrayList;

public class AutoGrader {
	
	private static long STORE_DELAY = 1;
	private static long CACHE_DELAY = 1;
	
	private static KVStore dataStore = null;
	private static KVCache dataCache = null;
	
	private static int currentOp = 0;
	
	public static void registerKVServer(KVStore dataStore, KVCache dataCache) {
		AutoGrader.dataStore = dataStore;
		AutoGrader.dataCache = dataCache;
	}

	public static void agCachePutStarted(String key, String value) {
		
	}
	
	public static void agCachePutFinished(String key, String value) {
		
	}

	public static void agCacheGetStarted(String key) {
		
	}
	
	public static void agCacheGetFinished(String key) {
		
	}

	public static void agCacheDelStarted(String key) {
		
	}
	
	public static void agCacheDelFinished(String key) {
		
	}

	public static void agStorePutStarted(String key, String value) {
		
	}
	
	public static void agStorePutFinished(String key, String value) {
		
	}

	public static void agStoreGetStarted(String key) {
		
	}
	
	public static void agStoreGetFinished(String key) {
		
	}

	public static void agStoreDelStarted(String key) {
		
	}

	public static void agStoreDelFinished(String key) {
		
	}

	public static void agKVServerPutStarted(String key, String value) {
		
	}
	
	public static void agKVServerPutFinished(String key, String value) {
		
	}

	public static void agKVServerGetStarted(String key) {
		
	}
	
	public static void agKVServerGetFinished(String key) {
		
	}
	
	public static void agKVServerDelStarted(String key) {
		
	}

	public static void agKVServerDelFinished(String key) {
		
	}

	public static void agCachePutDelay() {
		delay(CACHE_DELAY);
	}

	public static void agCacheGetDelay() {
		delay(CACHE_DELAY);
	}
	
	public static void agCacheDelDelay() {
		delay(CACHE_DELAY);
	}

	/**
	 * KVStore will sleep for STORE_DELAY milliseconds  
	 */
	public static void agStoreDelay() {
		delay(STORE_DELAY);
	}
	
	/**
	 * Helper method to put the current thread to sleep for sleepTime duration
	 * @param sleepTime time to sleep in milliseconds
	 */
	private static void delay(long sleepTime) {
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}

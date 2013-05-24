/**
 * Implementation of a set-associative cache.
 * 
 * @author Mosharaf Chowdhury (http://www.mosharaf.com)
 * @author Prashanth Mohan (http://www.cs.berkeley.edu/~prmohan)
 * 
 * Copyright (c) 2012, University of California at Berkeley
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of University of California, Berkeley nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *    
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package cs162;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.*;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.net.*;
import java.io.*;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.SAXException;
import org.w3c.dom.NodeList;


/**
 * A set-associate cache which has a fixed maximum number of sets (numSets).
 * Each set has a maximum number of elements (MAX_ELEMS_PER_SET).
 * If a set is full and another entry is added, an entry is dropped based on the eviction policy.
 */
public class KVCache {	
	private int numSets = 100;
	private int maxElemsPerSet = 10;

	private Map<String, CacheEntry>[] sets;
	private LinkedList<CacheEntry>[] chanceLists;
	private ReentrantReadWriteLock[] locks;


	/**
	 * Creates a new LRU cache.
	 * @param cacheSize	the maximum number of entries that will be kept in this cache.
	 */
	public KVCache(int numSets, int maxElemsPerSet) {
		this.numSets = numSets;
		this.maxElemsPerSet = maxElemsPerSet;     

		this.sets = new Map[numSets];
		this.chanceLists = new LinkedList[numSets];
		this.locks = new ReentrantReadWriteLock[numSets];

		for (int i = 0; i < numSets; ++i) {
			this.sets[i] = new HashMap<String, CacheEntry>();
			this.chanceLists[i] = new LinkedList<CacheEntry>();
			// populate with empty invalid entries
			for (int j = 0; j < this.maxElemsPerSet; ++j) {
				this.chanceLists[i].add(new CacheEntry("", ""));
			}
			this.locks[i] = new ReentrantReadWriteLock();
		}
	}

	/**
	 * Retrieves an entry from the cache.
	 * Assumes the corresponding set has already been locked for writing.
	 * @param key the key whose associated value is to be returned.
	 * @return the value associated to this key, or null if no value with this key exists in the cache.
	 */
	public String get(String a) {String args = "";args += String.valueOf(a);LogHelper.invocationBegin("KVCache#get(String)", args);String ret = getPrime(a);LogHelper.invocationEnd("KVCache#get(String)", String.valueOf(ret));return ret;} 	public String getPrime(String key) {
		// Must be called before anything else
		AutoGrader.agCacheGetStarted(key);
		AutoGrader.agCacheGetDelay();

		try {
			KVClient.sanitize(key);
		} catch (KVException e) {
			// error handling
			return null;
		}

		if (this.getSet(key).containsKey(key)) {
			CacheEntry e = this.getSet(key).get(key);
			e.isReferenced = true;

			AutoGrader.agCacheGetFinished(key);

			return e.isValid ? e.value : null;	
		} else {
			AutoGrader.agCacheGetFinished(key);

			return null;
		}
		
	}

	/**
	 * Adds an entry to this cache.
	 * If an entry with the specified key already exists in the cache, it is replaced by the new entry.
	 * If the cache is full, an entry is removed from the cache based on the eviction policy
	 * Assumes the corresponding set has already been locked for writing.
	 * @param key	the key with which the specified value is to be associated.
	 * @param value	a value to be associated with the specified key.
	 * @return true is something has been overwritten 
	 */
	public void put(String a,String b) {String args = "";args += String.valueOf(a);args += ",";args += String.valueOf(b);LogHelper.invocationBegin("KVCache#put(String,String)", args);putPrime(a,b);LogHelper.invocationEnd("KVCache#put(String,String)", "void");} 	public void putPrime(String key, String value) {
		// Must be called before anything else
		AutoGrader.agCachePutStarted(key, value);
		AutoGrader.agCachePutDelay();

		try {
			KVClient.sanitize(key, value);
		} catch (KVException e) {
			// error handling
			return;
		}

		this.del(key);

		CacheEntry e = new CacheEntry(key, value);
		e.isValid = true;
		// second chance algorithm

		CacheEntry toEvict = null;
		while (toEvict == null) {
			CacheEntry evictCandidate = this.getList(key).poll();

			if (evictCandidate.isValid == false ||
				evictCandidate.isReferenced == false) {
				toEvict = evictCandidate;
			} else {
				evictCandidate.isReferenced = false;
				this.getList(key).add(evictCandidate);
			}
		}

		this.getSet(key).remove(toEvict.key);

		this.getSet(key).put(key, e);
		this.getList(key).add(e);
	}

	/**
	 * Removes an entry from this cache.
	 * Assumes the corresponding set has already been locked for writing.
	 * @param key	the key with which the specified value is to be associated.
	 */
	public void del(String a) {String args = "";args += String.valueOf(a);LogHelper.invocationBegin("KVCache#del(String)", args);delPrime(a);LogHelper.invocationEnd("KVCache#del(String)", "void");} 	public void delPrime(String key) {
		// Must be called before anything else
		AutoGrader.agCacheGetStarted(key);
		AutoGrader.agCacheDelDelay();
		
		try {
			KVClient.sanitize(key);
		} catch (KVException e) {
			// error handling
			return;
		}

		if (this.getSet(key).containsKey(key)) {
			CacheEntry e = this.getSet(key).get(key);
			e.isValid = false;
			this.getSet(key).remove(key);
		}

		AutoGrader.agCacheDelFinished(key);
	}
	
	/**
	 * @param key
	 * @return	the write lock of the set that contains key.
	 */
	public WriteLock getWriteLock(String key) {
		return this.locks[this.getSetId(key)].writeLock();
	}
	
	/**
	 * 
	 * @param key
	 * @return	set of the key
	 */
	private int getSetId(String key) {
		return Math.abs(key.hashCode()) % numSets;
	}

	private Map<String, CacheEntry> getSet(String key) {
		return this.sets[this.getSetId(key)];
	}

	private LinkedList<CacheEntry> getList(String key) {
		return this.chanceLists[this.getSetId(key)];
	}

	private ReentrantReadWriteLock getLock(String key) {
		return this.locks[this.getSetId(key)];
	}

	protected void readLock(String key) {
		this.getLock(key).readLock().lock();
	}

	protected void readRelease(String key) {
		this.getLock(key).readLock().unlock();
	}

	protected void writeLock(String key) {
		this.getLock(key).writeLock().lock();
	}

	protected void writeRelease(String key) {
		this.getLock(key).writeLock().unlock();
	}
	
	public String toXML() {
		try {
            // first prepare a doc
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			DOMImplementation implementation = builder.getDOMImplementation();
			Document doc = implementation.createDocument("", "KVCache", null);
			doc.setXmlStandalone(true);
			Element root = doc.getDocumentElement();

			for (int i = 0; i < this.numSets; ++i) {
				Element set = doc.createElement("Set");
				root.appendChild(set);

				set.setAttribute("Id", Integer.toString(i));
				for (CacheEntry e : this.chanceLists[i]) {
					Element entry = doc.createElement("CacheEntry");
					set.appendChild(entry);

					entry.setAttribute("isReferenced",
						e.isReferenced ? "true" : "false");
					entry.setAttribute("isValid",
						e.isValid ? "true" : "false");

					Element key = doc.createElement("Key");
					entry.appendChild(key);
					key.appendChild(doc.createTextNode(e.key));

					Element value = doc.createElement("Value");
					entry.appendChild(value);
					value.appendChild(doc.createTextNode(e.value));
				}
			}

            // write to a string
			Source source = new DOMSource(doc);
			StringWriter sw = new StringWriter();
			StreamResult result = new StreamResult(new BufferedWriter(sw));

			TransformerFactory tfactory = TransformerFactory.newInstance();
			Transformer transformer = tfactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");            
			transformer.transform(source, result); 

			return sw.toString();
		} catch (Exception ignored) {
            // TODO: 
			System.err.println("Error while generating XML");

			return null; 
		}
	}

	private class CacheEntry {
		public String key;
		public String value;
    	// TODO: is this true
		public boolean isReferenced = false;
		public boolean isValid = false;

		public CacheEntry(String key, String value) {
			this.key = key;
			this.value = value;
		}
	}
}

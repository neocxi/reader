/**
 * Persistent Key-Value storage layer. Current implementation is transient, 
 * but assume to be backed on disk when you do your project.
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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;

/**
 * This is a dummy KeyValue Store. Ideally this would go to disk, 
 * or some other backing store. For this project, we simulate the disk like 
 * system using a manual delay.
 *
 */
public class KVStore {
	protected Dictionary<String, String> store 	= null;
	
	public KVStore() {
		resetStore();
	}

	protected void resetStore() {
		store = new Hashtable<String, String>();
	}
	
	public void put(String key, String value) throws KVException {
		AutoGrader.agStorePutStarted(key, value);
		
        KVClient.sanitize(key, value);
		try {
			putDelay();
			store.put(key, value);
        } catch (Exception ignored) {
            throw new KVException("IO Error");
		} finally {
			AutoGrader.agStorePutFinished(key, value);
		}
	}
	
	public String get(String key) throws KVException {
		AutoGrader.agStoreGetStarted(key);
		
        KVClient.sanitize(key);
		try {
			getDelay();
			String retVal = this.store.get(key);
			if (retVal == null) {
			    KVMessage msg = new KVMessage("resp", "Does not exist");
			    throw new KVException(msg);
			}
			return retVal;
        } catch (KVException e) {
            throw e;
		} catch (Exception ignored) {
            throw new KVException("IO Error");
        } finally {
			AutoGrader.agStoreGetFinished(key);
		}
	}
	
	public void del(String key) throws KVException {
		AutoGrader.agStoreDelStarted(key);

        KVClient.sanitize(key);
        // ensure the key exists
        this.get(key);
		try {
			delDelay();
			if(key != null) {
				this.store.remove(key);
            }
        } catch (Exception ignored) {
            throw new KVException("IO Error");
		} finally {
			AutoGrader.agStoreDelFinished(key);
		}
	}
	
	private void getDelay() {
		AutoGrader.agStoreDelay();
	}
	
	private void putDelay() {
		AutoGrader.agStoreDelay();
	}
	
	private void delDelay() {
		AutoGrader.agStoreDelay();
	}
	
    public String toXML() {
        try {
            StringWriter sw = new StringWriter();
            XMLStreamWriter writer = Lib.newXMLStreamWriter(sw);
            try {
                writer.writeStartDocument("UTF-8", "1.0");
                writer.writeStartElement("KVStore");
                for (Enumeration keys = store.keys(); keys.hasMoreElements(); ) {
                    String key = (String) keys.nextElement();
                    writer.writeStartElement("KVPair");
                    writer.writeStartElement("Key");
                    writer.writeCharacters(key);
                    writer.writeEndElement();
                    String value = (String) store.get(key);
                    writer.writeStartElement("Value");
                    writer.writeCharacters(value);
                    writer.writeEndElement();
                    writer.writeEndElement();
                }
                writer.writeEndElement();
                writer.writeEndDocument();
                return Lib.prettifyXML(sw.toString());
            } catch (XMLStreamException e) {
                throw new KVException("IO Error");
            } finally {
                try {
                    sw.close();
                } catch(IOException e) {
                    throw new KVException("IO Error");
                }
            }
        } catch (KVException e) {
            try {
                System.err.println("toXML error. ");
                KVMessage rc = new KVMessage("resp", "IO Error");
                return Lib.prettifyXML(rc.toXML());
            } catch (KVException ex) {
                return null;
            }
        }
    }        

    private static void assertFormatValid(boolean cond) throws KVException {
        if (!cond) {
            throw new KVException("IO Error");
        }
    }

    private void fromXML(InputStream input) throws KVException {
        resetStore();
        Document dom = Lib.getDOM(input); 
        Element root = dom.getDocumentElement();
        assertFormatValid(root.getTagName().equals("KVStore"));
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            Node item = children.item(i);
            short nodeType = item.getNodeType();
            if (nodeType == Node.TEXT_NODE && item.getNodeValue().trim().length() == 0) {
                continue;
            } else if (nodeType != Node.ELEMENT_NODE) {
                assertFormatValid(false);
            }
            Element child = (Element) item;
            assertFormatValid(child.getTagName().equals("KVPair"));
            assertFormatValid(Lib.isOnlyChildren(child, new String[] { "Key", "Value" }));
            String key = Lib.getChildValue(child, "Key");
            String value = Lib.getChildValue(child, "Value");
            // Doesn't allow a dumped file to have two same keys
            assertFormatValid(store.get(key) == null);
            store.put(key, value);
        }
    }

    public void dumpToFile(String fileName) {
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(this.toXML());
            bw.close();
        } catch (Exception e) {
            // throw new KVException("IO Error");
            System.err.println("dumpToFile error"); 
        }
    }

    public void restoreFromFile(String fileName) {
        try {
            FileInputStream input = new FileInputStream(fileName);
            fromXML(input);
            input.close();
        } catch (Exception e) {
            // throw new KVException("IO Error");
            System.err.println();
        }
    }

    private static void testParser(String filePath) {
        InputStream input;
        System.out.println("Started parsing " + filePath);
        try {
            KVStore s = new KVStore();
            s.restoreFromFile(filePath);
            System.out.println(s.toXML());
        // } catch (KVException e) {
        //     System.out.println(e.getMsg().getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String rootPath = KVMessage.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        // Test parser
        File xmlFolder = new File(rootPath + "../test/fixtures/kvstore/xml/");
        File[] files = xmlFolder.listFiles();
        for (int i = 0; i < files.length; ++i) {
            if (files[i].isFile()) {
                if (files[i].getName().endsWith(".xml")) {
                    testParser(files[i].getPath());
                }
            }
        }
    }
}
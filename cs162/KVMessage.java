/**
 * XML Parsing library for the key-value store
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
 * This is the object that is used to generate messages the XML based messages 
 * for communication between clients and servers. 
 */
public class KVMessage {
    private String msgType = null;
    private String key = null;
    private String value = null;
    private String status = null;
    private String message = null;
    
    public final String getKey() {
        return key;
    }

    public final void setKey(String key) {
        this.key = key;
    }

    public final String getValue() {
        return value;
    }

    public final void setValue(String value) {
        this.value = value;
    }

    public final String getStatus() {
        return status;
    }

    public final void setStatus(String status) {
        this.status = status;
    }

    public final String getMessage() {
        return message;
    }

    public final void setMessage(String message) {
        this.message = message;
    }

    public String getMsgType() {
        return msgType;
    }

    /* Solution from http://weblogs.java.net/blog/kohsuke/archive/2005/07/socket_xml_pitf.html */
    private class NoCloseInputStream extends FilterInputStream {
        public NoCloseInputStream(InputStream in) {
            super(in);
        }
        
        public void close() {} // ignore close
    }
    
    /***
     * 
     * @param msgType
     * @throws KVException of type "resp" with message "Message format incorrect" if msgType is unknown
     */
    public KVMessage(String msgType) throws KVException {
        // make sure msgType is a recognizable type
        if (msgType.equals("getreq") ||
            msgType.equals("putreq") ||
            msgType.equals("delreq") ||
            msgType.equals("resp")) {
            this.msgType = msgType;
        } else {
            throw new KVException("Message format incorrect");
        }
    }
    
    public KVMessage(String msgType, String message) throws KVException {
        //TODO: should we make sure message is of some form
        this(msgType);
        this.message = message;
    }
    
     /***
     * Parse KVMessage from incoming network connection
     * @param sock
     * @throws KVException if there is an error in parsing the message. The exception should be of type "resp and message should be :
     * a. "XML Error: Received unparseable message" - if the received message is not valid XML.
     * b. "Network Error: Could not receive data" - if there is a network error causing an incomplete parsing of the message.
     * c. "Message format incorrect" - if there message does not conform to the required specifications. Examples include incorrect message type. 
     */
     public KVMessage(InputStream input) throws KVException {
        input = new NoCloseInputStream(input);
        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            document = parser.parse(input);
        } catch (IOException ignored) {
            throw new KVException("Network Error: Could not receive data");
        } catch (SAXException ignored) {
            throw new KVException("XML Error: Received unparseable message");
        } catch (Exception e) {
            // screw it
            throw new KVException("Unknown Error: " + e);
        }

        Element root = document.getDocumentElement();
        if (root.hasAttribute("type")) {
            this.msgType = root.getAttribute("type");
        } else {
            throw new KVException("Message format incorrect");
        }

        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            Node child = children.item(i);
            String type = child.getNodeName();

            if (type.equals("Key")) {
                this.key = child.getTextContent();
            } else if (type.equals("Value")) {
                this.value = child.getTextContent();
            } else if (type.equals("Message")) {
                this.message = child.getTextContent();
            } else if (type.equals("#text")) {
                // ignored text node
            } else {
                // unknown node
               throw new KVException("Message format incorrect");
            }
        }

        if (this.message == null &&
            this.key     == null &&
            this.value   == null) {
            throw new KVException("Message format incorrect");
        }
     }
     
    /**
     * Generate the XML representation for this message.
     * @return the XML String
     * @throws KVException if not enough data is available to generate a valid KV XML message
     */
    // <?xml version="1.0" encoding="UTF-8"?>
    // <KVMessage type="getreq">
    // <Key>key</Key>
    // </KVMessage>
    public String toXML() throws KVException {
        try {
            // first prepare a doc
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            DOMImplementation implementation = builder.getDOMImplementation();
            Document doc = implementation.createDocument("", "KVMessage", null);
            doc.setXmlStandalone(true);
            Element root = doc.getDocumentElement();
            root.setAttribute("type", this.msgType);

            if (this.key != null) {
                Element key = doc.createElement("Key");
                key.appendChild(doc.createTextNode(this.key));
                root.appendChild(key);
            }

            if (this.value != null) {
                Element value = doc.createElement("Value");
                value.appendChild(doc.createTextNode(this.value));
                root.appendChild(value);
            }

            if (this.message != null) {
                Element message = doc.createElement("Message");
                message.appendChild(doc.createTextNode(this.message));
                root.appendChild(message);
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
            throw new KVException("Error while generating XML");
        }
    }
    
    public void sendMessage(Socket sock) throws KVException {
        try {
            OutputStream out = sock.getOutputStream();
            PrintWriter pout = new PrintWriter(out, true);
            pout.print(this.toXML());
            pout.flush();
            sock.shutdownOutput();
        } catch (IOException ignored) {
            throw new KVException("Network Error: Could not send data");
        }
    }

    public static void main(String[] args) {
        try { 
            System.out.println("===KVMessage with type"); 
            System.out.println((new KVMessage("resp")).toXML());

            System.out.println("===KVMessage with type and message");
            System.out.println((new KVMessage("resp", "In hell we trust")).toXML());
            
            System.out.println("===KVMessage as get request");
            KVMessage temp = new KVMessage("getreq");
            temp.setKey("lmf");
            System.out.println(temp.toXML());

            System.out.println("===KVMessage as put request");
            temp = new KVMessage("putreq");
            temp.setKey("lmf");
            temp.setValue("hk hiphop");
            System.out.println(temp.toXML());

            System.out.println("===KVMessage constructed from stream: " + args[0]);
            temp = new KVMessage(new FileInputStream(args[0]));
            System.out.println(temp.toXML());

        } catch (KVException e) {
            System.out.println("die with dignity");
            try {
                System.out.println(e.getMsg().toXML());
            } catch (Exception ee) {
                System.out.println("whatever");
            }
        } catch (Exception e) {
            System.out.println("die without dignit" + e);
        }
    }
}

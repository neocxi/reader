package cs162;

import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class AssertionFailureError extends Error {
    AssertionFailureError() {
        super();
    }

    AssertionFailureError(String message) {
        super(message);
    }
}

class Lib {
    public static Document getDOM(InputStream input) throws KVException {
        try {
            return DocumentBuilderFactory.newInstance()
                                         .newDocumentBuilder()
                                         .parse(input);
        } catch (ParserConfigurationException e) {
            throw new KVException("Unknown Error: " + e.getMessage());
        } catch (SAXException e) {
            throw new KVException("XML Error: Received unparseable message");
        } catch (IOException e) {
            throw new KVException("Network Error: Could not receive data");
        }
    }

    public static XMLStreamWriter newXMLStreamWriter(StringWriter sw) throws KVException {
        try {
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            return factory.createXMLStreamWriter(sw);
        } catch (XMLStreamException e) {
            throw new KVException("Unknown Error: " + e.getMessage());
        }
    }

    public static String prettifyXML(String input) throws KVException {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter formattedStringWriter = new StringWriter();
            transformer.transform(new StreamSource(new StringReader(input)), new StreamResult(formattedStringWriter));
            return formattedStringWriter.toString();
        } catch (TransformerConfigurationException e) {
            throw new KVException("Unknown Error: " + e.getMessage());
        } catch (TransformerException e) {
            throw new KVException("Unknown Error: " + e.getMessage());
        }
    }

    public static boolean isOnlyChildren(Element e, String[] tags) {
        // Check that all children's tag names are included in tags[] and that
        // all children are leaf nodes (i.e. the only children is text)
        NodeList children = e.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            Node item = children.item(i);
            short nodeType = item.getNodeType();
            if (nodeType == Node.TEXT_NODE && item.getNodeValue().trim().length() == 0) {
                continue;
            } else if (nodeType != Node.ELEMENT_NODE) {
                return false;    
            }
            Element child = (Element) item;
            if (!isIncluded(child.getTagName(), tags)
             || child.getChildNodes().getLength() != 1
             || child.getFirstChild().getNodeType() != Node.TEXT_NODE) {
                return false;
            }
        }
        // Check that all tags in the tags[] array appear in the element
        // exactly once
        for (int i = 0; i < tags.length; ++i) {
            NodeList nl = e.getElementsByTagName(tags[i]);
            if (nl == null || nl.getLength() != 1) {
                return false;
            }
        }
        return true;
    }

    public static String getChildValue(Element e, String tagName) {
        NodeList nl = e.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            return nl.item(0).getFirstChild().getNodeValue();
        } else {
            return null;
        }
    }

    public static boolean isIncluded(String s, String[] list) {
        for (int i = 0; i < list.length; ++i) {
            if (s.equals(list[i])) {
                return true;
            }
        }
        return false;
    }

    public static void throwKVException(String msg) throws KVException {
        throw new KVException(msg);
    }

    public static void throwIfReached(String msg) throws KVException {
        throwKVException(msg);
    }

    public static void assertNotReached() {
        assertTrue(false, "This line of code should not be reached");
    }

    public static void assertTrue(boolean cond) {
        if (!cond) {
            throw new AssertionFailureError();
        }
    }

    public static void assertTrue(boolean cond, String msg) {
        if (!cond) {
            throw new AssertionFailureError(msg);
        }
    }
}
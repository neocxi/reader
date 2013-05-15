
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


public class LogHelper {

    private static class LogEntry {
        public String methodName = "";
        public String parameters = "";
        public String startTime  = "";
        public String endTime    = "";
        public String returnValue= "";
        public String threadId   = "";
    }

    private static String testName = null;

    private static Set<String> dependencySet = 
                new HashSet<String>();

    private static List<LogEntry> logList =
                new ArrayList<LogEntry>();

    private static List<LogEntry> incomepleteEntryList =
                new ArrayList<LogEntry>();

    private static DateFormat dateFormat =
                new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public static void setupEnvironment(String test, String dependencies[]) {
        for (String method : Arrays.asList(dependencies)) {
            dependencySet.add(method);
        }

        testName = test;
    }

    public static void destroyEnvironment(String test) {
        // things are wrong when they don't match
        assert test.equals(testName);

        writeToXML(test);

        // clean up state
        testName = null;
        dependencySet = new HashSet<String>();
        logList = new ArrayList<LogEntry>();
        incomepleteEntryList = new ArrayList<LogEntry>();
    }

    public static void invocationBegin(String methodName, String parameters) {
        // System.out.println(methodName + ", " + parameters);
        if (!dependencySet.contains(methodName)) {
            return;
        }

        LogEntry incomepleteEntry = new LogEntry();

        incomepleteEntry.methodName = methodName;
        incomepleteEntry.parameters = parameters;
        incomepleteEntry.startTime  = dateFormat.format(new Date());
        // No multi threading support for now
        incomepleteEntry.threadId   = "0";

        incomepleteEntryList.add(incomepleteEntry);
    }

    public static void invocationEnd(String methodName, String returnValue) {
        if (!dependencySet.contains(methodName)) {
            return;
        }

        assert !incomepleteEntryList.isEmpty();

        // They should correspond to the same call
        LogEntry incomepleteEntry = incomepleteEntryList.remove(
                                        incomepleteEntryList.size() - 1);
        assert incomepleteEntry.methodName == methodName;

        incomepleteEntry.returnValue = returnValue;
        incomepleteEntry.endTime     = dateFormat.format(new Date());

        logList.add(incomepleteEntry);
    }

    public static void writeToXML(String outputFile) {
        try {
            DocumentBuilderFactory docFactory = 
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
     
            Document doc = docBuilder.newDocument();
            Element history = doc.createElement("history");
            doc.appendChild(history);

            for (LogEntry entry : logList) {

                String methodName = entry.methodName;
                String parameters = entry.parameters;
                String startTime  = entry.startTime;
                String endTime    = entry.endTime;
                String returnValue= entry.returnValue;
                String threadId   = entry.threadId;

                Element rootElement = doc.createElement("call");
                history.appendChild(rootElement);

                Element methodNameE = doc.createElement("methodName");
                methodNameE.appendChild(doc.createTextNode(methodName));
                rootElement.appendChild(methodNameE);

                Element parametersE = doc.createElement("parameters");
                parametersE.appendChild(doc.createTextNode(parameters));
                rootElement.appendChild(parametersE);

                Element startTimeE = doc.createElement("startTime");
                startTimeE.appendChild(doc.createTextNode(startTime));
                rootElement.appendChild(startTimeE);

                Element endTimeE = doc.createElement("endTime");
                endTimeE.appendChild(doc.createTextNode(endTime));
                rootElement.appendChild(endTimeE);

                Element returnValueE = doc.createElement("returnValue");
                returnValueE.appendChild(doc.createTextNode(returnValue));
                rootElement.appendChild(returnValueE);

                Element threadIdE = doc.createElement("threadId");
                threadIdE.appendChild(doc.createTextNode(threadId));
                rootElement.appendChild(threadIdE);

            }

            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(domSource, result);
            
            writeTextFile(outputFile, writer.toString());
            //System.out.println(writer.toString());
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static void writeTextFile(String fileName, String s) {
        try { 
            File file = new File(fileName);
 
            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }
 
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(s);
            bw.close();
 
            // System.out.println(s);
 
        } catch (IOException e) {
            e.printStackTrace();
        }
      }
    
    public static void main (String[] args) {
        setupEnvironment("test", new String[] {"testMethod", "test2"});
        invocationBegin("testMethod", "pa1, pa2");
        invocationEnd("testMethod", "rett");

        invocationBegin("should not enter", "asd");
        invocationEnd("should not enter", "rssett");

        invocationBegin("testMethod", "pa1, pa2");
        invocationEnd("testMethod", "rett");

        invocationBegin("test2", "other");
        invocationEnd("test2", "another retval");
        destroyEnvironment("test");

        invocationBegin("wrong", "asd");
        invocationEnd("wrong", "nothing");

        setupEnvironment("test2", new String[] {"testMethod", "test2"});
        invocationBegin("testMethod", "pa1, pa2");
        invocationEnd("testMethod", "rett");

        invocationBegin("should not enter", "asd");
        invocationEnd("should not enter", "rssett");

        invocationBegin("testMethod", "pa1, pa2");
        invocationEnd("testMethod", "rett");

        invocationBegin("test2", "other");
        invocationEnd("test2", "another retval");
        destroyEnvironment("test2");
    }
}

package io.github.maybeec.sit.tools;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class PropertiesManager {
    
    private static PropertiesManager instance = new PropertiesManager();
    private String propertyFileName = System.getProperty("user.home") + System.getProperty("file.separator") + "csr_properties.xml";
    private File propertyFile;
    private Document document;
    
    public static final String UNDEFINED = "undefined";
    
    public static final String NOTEPAD = "notepad";
    public static final String IGNORED_FOLDERS = "ignored_folders";
    public static final String ONLY_STRING_SEARCH = "onlyString_SearchFilter";
    private static final String IGNORED_LINE_STARTUPS = "ignoredLineStartups";
    private static final String LINE_STARTUP_ENTRY = "entry";
    private static final String LINE_STARTUP_FILES = "files";
    private static final String LINE_STARTUP_SNIPPETS = "snippets";
    
    public static final String SEARCH_FOLDER = "search_folder";
    public static final String FILE_FILTER = "file_filter";
    public static final String SEARCH_TEXT = "search_text";
    public static final String REPLACEMENT = "replacement";
    public static final String CASE_SENSITIVE = "case_sensitive";
    public static final String USE_REGEX = "useRegex";
    
    private Node properties;
    
    public static synchronized PropertiesManager getInstance() {
        return instance;
    }
    
    private PropertiesManager() {
        init();
    }
    
    private void init() {
        propertyFile = new File(propertyFileName);
        if (!propertyFile.exists()) {
            createPropertiesFile();
        } else {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                document = builder.parse(propertyFile);
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void createPropertiesFile() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.newDocument();
            Node rootNode = document.createElement("CodeStringReplacement");
            document.appendChild(rootNode);
            
            properties = document.createElement("properties");
            
            rootNode.appendChild(properties);
            
            writeFile();
            
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }
    
    private boolean writeFile() {
        DOMSource domSource = new DOMSource(document.getFirstChild());
        File fileOutput = new File(propertyFileName);
        StreamResult streamResult = new StreamResult(fileOutput);
        
        try {
            Transformer serializer = TransformerFactory.newInstance().newTransformer();
            serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.transform(domSource, streamResult);
        } catch (TransformerConfigurationException e) {
            return false;
        } catch (TransformerException e) {
            return false;
        }
        return true;
    }
    
    public String getProperty(String arg) {
        String result = UNDEFINED;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(propertyFile);
            
            Node tmp = doc.getElementsByTagName(arg).item(0);
            if (tmp != null) {
                if (tmp.getTextContent() != "") {
                    result = tmp.getTextContent();
                }
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    public boolean setProperty(String arg, String value) {
        
        if (this.getProperty(arg).equals(UNDEFINED)) {
            Node properties = document.getElementsByTagName("properties").item(0);
            properties.appendChild(document.createElement(arg));
        }
        
        document.getElementsByTagName(arg).item(0).setTextContent(value);
        return writeFile();
    }
    
    public boolean hasEntries(String prop) {
        boolean result = false;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(propertyFile);
            
            Node tmp = doc.getElementsByTagName(prop).item(0);
            if (tmp != null && tmp.hasChildNodes()) {
                result = true;
            }
            
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    public boolean setLineStartups(Map<String, String> entries) {
        HashSet<String> valid = new HashSet<String>();
        
        //create root node if necessary
        if (document.getElementsByTagName(IGNORED_LINE_STARTUPS).getLength() == 0) {
            Node properties = document.getElementsByTagName("properties").item(0);
            properties.appendChild(document.createElement(IGNORED_LINE_STARTUPS));
        }
        
        Node rootNode = document.getElementsByTagName(IGNORED_LINE_STARTUPS).item(0);
        NodeList nodeEntries = document.getElementsByTagName(LINE_STARTUP_ENTRY);
        
        Set<Node> toDelete = new HashSet<Node>();
        for (int i = 0; i < nodeEntries.getLength(); i++) {
            Node files = nodeEntries.item(i).getAttributes().getNamedItem(LINE_STARTUP_FILES);
            Node snippets = nodeEntries.item(i).getAttributes().getNamedItem(LINE_STARTUP_SNIPPETS);
            if (entries.containsKey(files.getTextContent())) {
                if (!entries.get(files.getTextContent()).equals(snippets.getTextContent())) {
                    snippets.setTextContent(entries.get(files.getTextContent()));
                }
                valid.add(files.getTextContent());
            } else {
                toDelete.add(nodeEntries.item(i));
            }
        }
        
        //delete invalid entries from file
        for (Node n : toDelete) {
            rootNode.removeChild(n);
        }
        
        //delete all valid entries of entries to be added
        entries.keySet().removeAll(valid);
        
        //add all remaining
        for (String key : entries.keySet()) {
            Element e = document.createElement(LINE_STARTUP_ENTRY);
            e.setAttribute(LINE_STARTUP_FILES, key);
            e.setAttribute(LINE_STARTUP_SNIPPETS, entries.get(key));
            rootNode.appendChild(e);
        }
        
        return writeFile();
    }
    
    public Map<String, String> getLineStartupsToIgnore() {
        HashMap<String, String> map = new HashMap<String, String>();
        
        NodeList nodeEntries = document.getElementsByTagName(LINE_STARTUP_ENTRY);
        for (int i = 0; i < nodeEntries.getLength(); i++) {
            Node files = nodeEntries.item(i).getAttributes().getNamedItem(LINE_STARTUP_FILES);
            Node snippets = nodeEntries.item(i).getAttributes().getNamedItem(LINE_STARTUP_SNIPPETS);
            map.put(files.getTextContent(), snippets.getTextContent());
        }
        
        return map;
    }
}
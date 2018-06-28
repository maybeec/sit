package io.github.maybeec.sit.logic;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import io.github.maybeec.sit.model.LineCompare;
import io.github.maybeec.sit.tools.PropertiesManager;
import io.github.maybeec.sit.tools.Tools;
import io.github.maybeec.sit.ui.MainWindow;

public class Parser {
    
    private static String SINGE_QUOTES = "'";
    private static String DOUBLE_QUOTES = "\"";
    private static String NO_RESTRICTION = "";
    
    private final PropertiesManager pm = PropertiesManager.getInstance();
    private final MainWindow ui;
    private final UIController uic;
    private volatile Integer searchedFiles = 0;
    private volatile HashSet<String> ignoredFiles;
    private HashMap<HashSet<String>, String[]> ignoredLineStartups;
    private HashSet<String> onlyStringSearchFilters;
    
    private volatile List<File> filesWithUnkownEncoding;
    private volatile List<LineCompare> finds;
    
    private ThreadManager threadManager;
    
    public Parser(MainWindow ui, UIController uic) {
        this.ui = ui;
        this.uic = uic;
        loadSettings();
    }
    
    public List<File> getFilesWithUnknownEncoding() {
        return filesWithUnkownEncoding;
    }
    
    public int getSearchedFolders() {
        return searchedFiles;
    }
    
    public void searchingFinished() {
        uic.setResults(finds, searchedFiles);
    }
    
    public void search(String[] searchString, String[] files, String[] fileFilters) throws IOException {
        
        filesWithUnkownEncoding = Collections.synchronizedList(new LinkedList<File>());
        finds = Collections.synchronizedList(new LinkedList<LineCompare>());
        
        threadManager = new ThreadManager(this, new ProgressMonitor(ui, "Searching...", "", 0, 100));
        
        for (String s : files) {
            File folder = new File(s);
            if (!folder.exists()) {
                JOptionPane.showMessageDialog(ui, "The file/folder " + folder.getAbsolutePath() + "does not exist!", "An error occured",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                prepareFoldersToBeSearched(searchString, folder, fileFilters);
            }
        }
        
        threadManager.start();
    }
    
    private void prepareFoldersToBeSearched(final String[] searchString, final File folder, final String[] fileFilters) {
        boolean existsFiles = false;
        for (File f : folder.listFiles()) {
            if (!matches(f, ignoredFiles) && matches(f, fileFilters)) {
                if (f.isDirectory()) {
                    prepareFoldersToBeSearched(searchString, f, fileFilters);
                } else if (f.isFile()) {
                    existsFiles = true;
                }
            }
        }
        
        if (existsFiles) {
            threadManager.addTask(new Runnable() {
                @Override
                public void run() {
                    searchInFiles(searchString, folder, fileFilters);
                }
            });
        }
    }
    
    public void searchInFiles(String[] searchString, File folder, String[] fileFilters) {
        for (File f : folder.listFiles()) {
            if (f.isFile() && !matches(f, ignoredFiles) && matches(f, fileFilters)) {
                try {
                    List<LineCompare> tmp = searchString(f, searchString);
                    if (tmp.size() > 0) {
                        finds.addAll(tmp);
                    }
                    synchronized (searchedFiles) {
                        searchedFiles++;
                    }
                } catch (IOException e) {
                    //do nothing for the moment
                }
            }
        }
    }
    
    private List<LineCompare> searchString(File f, String[] searchStrings) throws IOException {
        List<LineCompare> finds = new LinkedList<LineCompare>();
        
        boolean onlyStrings = false;
        if (matches(f, onlyStringSearchFilters)) {
            onlyStrings = true;
        }
        
        FileOperator fo = new FileOperator(f);
        
        String strLine;
        boolean initialized = false;
        int lineNr = 1;
        while ((strLine = fo.readLine()) != null) {
            String[] linestartsToIgnore = matches(f, ignoredLineStartups);
            if (linestartsToIgnore == null || !startsWithOneOf(strLine, linestartsToIgnore)) {
                LinkedList<Integer[]> occurences = new LinkedList<Integer[]>();
                for (String s : searchStrings) {
                    if (onlyStrings) {
                        initialized = searchFor(SINGE_QUOTES, strLine, s, initialized, finds, lineNr, f, occurences);
                        initialized = searchFor(DOUBLE_QUOTES, strLine, s, initialized, finds, lineNr, f, occurences);
                    } else {
                        initialized = searchFor(NO_RESTRICTION, strLine, s, initialized, finds, lineNr, f, occurences);
                    }
                }
                if (occurences.size() != 0) {
                    finds.add(new LineCompare(f, strLine, occurences, lineNr));
                }
            }
            lineNr++;
        }
        fo.finishReading();
        
        return finds;
    }
    
    private boolean searchFor(String kind, String strLine, String searchString, boolean initialized, List<LineCompare> finds, int lineNr, File f,
            LinkedList<Integer[]> occurences) {
        String tmp = "";
        int index;
        
        boolean sourceStringClosed = false;
        boolean noRestrictionSearch;
        if (kind.equals(NO_RESTRICTION)) {
            noRestrictionSearch = true;
            index = 0;
        } else {
            noRestrictionSearch = false;
            index = getIndexOfNextRealQuote(strLine, kind, 0);
        }
        
        Pattern p;
        String patternSearchString = searchString;
        if (!ui.isRegex()) {
            patternSearchString = Pattern.quote(searchString);
        }
        if (!ui.isCaseSensitive()) {
            p = Pattern.compile(patternSearchString, Pattern.CASE_INSENSITIVE);
        } else {
            p = Pattern.compile(patternSearchString);
        }
        Matcher m;
        
        while (index != -1) {
            int oldIndex = index;
            index = getIndexOfNextRealQuote(strLine, kind, index + 1);
            
            if (index != -1) {
                sourceStringClosed = !sourceStringClosed;
                
                tmp = strLine.substring(oldIndex, index);
                int shift = 0;
                
                m = p.matcher(tmp);
                while (m.find()) {
                    
                    int indexOf;
                    int searchStringLength;
                    if (ui.isRegex()) {
                        indexOf = m.start(1);
                        searchStringLength = m.group(1).length();
                    } else {
                        searchStringLength = searchString.length();
                        if (ui.isCaseSensitive()) {
                            indexOf = tmp.indexOf(searchString);
                        } else {
                            indexOf = tmp.toLowerCase().indexOf(searchString.toLowerCase());
                        }
                    }
                    
                    if (sourceStringClosed || noRestrictionSearch) {
                        if (!initialized) {
                            finds.add(new LineCompare(f));
                            initialized = true;
                        }
                        
                        Integer[] tmpArr = new Integer[] { ui.isRegex() ? oldIndex + indexOf : oldIndex + shift + indexOf, searchStringLength };
                        if (!Tools.listContains(occurences, tmpArr)) {
                            occurences.add(tmpArr);
                        }
                    }
                    shift = indexOf + searchStringLength;
                    tmp = tmp.substring(shift);
                }
            }
        }
        
        return initialized;
    }
    
    private static int getIndexOfNextRealQuote(String strLine, String kind, int offset) {
        
        if (kind.equals(NO_RESTRICTION)) {
            if (offset >= strLine.length()) {
                return -1;
            } else {
                return strLine.length();
            }
        } else {
            int index = strLine.indexOf(kind, offset);
            
            //check for escaped quotes
            char[] dst = new char[1];
            if (index > 0) {
                strLine.getChars(index - 1, index, dst, 0);
                if (dst[0] != '\\') {
                    return index;
                } else {
                    return getIndexOfNextRealQuote(strLine, kind, index + 1);
                }
            } else {
                return index;
            }
        }
    }
    
    private void loadSettings() {
        String prop;
        ignoredFiles = new HashSet<String>();
        if (!(prop = pm.getProperty(PropertiesManager.IGNORED_FOLDERS)).equals(PropertiesManager.UNDEFINED)) {
            ignoredFiles.addAll(Arrays.asList(prop.split(",")));
        }
        onlyStringSearchFilters = new HashSet<String>();
        if (!(prop = pm.getProperty(PropertiesManager.ONLY_STRING_SEARCH)).equals(PropertiesManager.UNDEFINED)) {
            onlyStringSearchFilters.addAll(Arrays.asList(prop.split(",")));
        }
        
        //load line startups to ignore
        ignoredLineStartups = new HashMap<HashSet<String>, String[]>();
        Map<String, String> ignoredLineStartups = pm.getLineStartupsToIgnore();
        for (String key : ignoredLineStartups.keySet()) {
            this.ignoredLineStartups.put(new HashSet<String>(Arrays.asList(key.split(","))), ignoredLineStartups.get(key).split(","));
        }
    }
    
    private boolean startsWithOneOf(String strLine, String[] linestartsToIgnore) {
        for (String s : linestartsToIgnore) {
            if (strLine.trim().startsWith(s)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean matches(File file, String[] fileFilters) {
        return matches(file, new HashSet<String>(Arrays.asList(fileFilters)));
    }
    
    private boolean matches(File file, Set<String> ignoredFiles) {
        for (String p : ignoredFiles) {
            if (file.getName().matches(p.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*"))) {
                return true;
            } else if (file.getName().toLowerCase().matches(p.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*").toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    private String[] matches(File f, HashMap<HashSet<String>, String[]> setOfSets) {
        for (Set<String> key : setOfSets.keySet()) {
            if (matches(f, key)) {
                return setOfSets.get(key);
            }
        }
        return null;
    }
}

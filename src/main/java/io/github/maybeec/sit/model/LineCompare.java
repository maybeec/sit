package io.github.maybeec.sit.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.maybeec.sit.tools.Tools;

public class LineCompare {
    
    public enum Action {
        ACCEPT, IGNORE;
    }
    
    private String annotation;
    private File file;
    private String oldString;
    private String oldStringDisplay;
    private String newString;
    private String newStringDisplay;
    private List<Integer[]> occurences = new LinkedList<Integer[]>();
    private List<Integer[]> toBeMarked = new LinkedList<Integer[]>();
    private int lineNr;
    private int drawOffsetNewString;
    private int drawOffsetOldString;
    private Action action = Action.IGNORE;
    
    private PropertyChangeSupport support = new PropertyChangeSupport(this);
    
    public LineCompare(File file) {
        this.file = file;
    }
    
    public LineCompare(File f, String strLine, List<Integer[]> occurences, int lineNr) {
        this.file = f;
        this.oldString = strLine;
        this.lineNr = lineNr;
        
        for (Integer[] i : occurences) {
            this.occurences.add(new Integer[] { i[0], i[0] + i[1] });
        }
    }
    
    public LineCompare(String string) {
        annotation = string;
    }
    
    public List<Integer[]> getHighlightingRanges() {
        return toBeMarked;
    }
    
    public File getFile() {
        return file;
    }
    
    public int getDrawOffsetNewString() {
        return drawOffsetNewString;
    }
    
    public int getDrawOffsetOldString() {
        return drawOffsetOldString;
    }
    
    public String getNewString() {
        return newString;
    }
    
    public int getLine() {
        return lineNr;
    }
    
    public void setReplacementString(String text) {
        if (oldString == null || occurences.size() == 0) return;
        
        toBeMarked.clear();
        Collection<Integer[]> sortedOcc = Tools.sort(occurences);
        
        //create new original string
        StringBuilder sb = new StringBuilder(oldString);
        int strDiff = 0;
        
        for (Integer[] i : sortedOcc) {
            try {
                sb.delete(i[0] + strDiff, i[1] + strDiff);
                sb.insert(i[0] + strDiff, text);
                strDiff += text.length() - i[1] + i[0];
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        
        newString = sb.toString();
        
        //create strings to be displayed
        List<Integer[]> adaptedOcc = createStringsToDisplay(sortedOcc);
        sb = new StringBuilder(oldStringDisplay);
        
        strDiff = 0;
        for (Integer[] i : adaptedOcc) {
            
            sb.delete(i[0] + strDiff, i[1] + strDiff);
            toBeMarked.add(new Integer[] { 0, i[0], i[1] }); //use 0 for oldString
            
            sb.insert(i[0] + strDiff, text);
            toBeMarked.add(new Integer[] { 1, i[0] + strDiff, i[0] + strDiff + text.length() }); //use 1 for newString
            
            strDiff += text.length() - i[1] + i[0];
        }
        
        newStringDisplay = sb.toString();
    }
    
    private List<Integer[]> createStringsToDisplay(Collection<Integer[]> sortedOcc) {
        int min = Tools.getMinOffset(occurences);
        int max = Tools.getMaxOffset(occurences);
        List<Integer[]> result = new LinkedList<Integer[]>(sortedOcc);
        
        int minOffset = 0;
        if (min > 100) {
            result = Tools.reduceOffsets(min - 50, occurences);
            minOffset = min - 50 + 4;
        }
        
        if (max + 20 <= oldString.length() && oldString.length() > 300) {
            oldStringDisplay = ((minOffset == 0) ? "" : "... ") + oldString.substring(minOffset, max + 20) + " ...";
        } else {
            oldStringDisplay = ((minOffset == 0) ? "" : "... ") + oldString.substring(minOffset);
        }
        return result;
    }
    
    public void setAction(Action action) {
        if (oldString != null || newString != null) {
            Action old = this.action;
            this.action = action;
            support.firePropertyChange(new PropertyChangeEvent(this, "action", old, action));
        }
    }
    
    public Action getAction() {
        return action;
    }
    
    private int countBeginningWhitespaces(String in) {
        Pattern p = Pattern.compile("(\\s*)");
        Matcher m = p.matcher(in);
        if (m.find()) {
            return m.group(1).length();
        }
        return 0;
    }
    
    public boolean isFoundEntity() {
        if (newString != null) {
            return true;
        } else {
            return false;
        }
    }
    
    public void addPropertyChangeListener(PropertyChangeListener l) {
        support.addPropertyChangeListener(l);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener l) {
        support.removePropertyChangeListener(l);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LineCompare) {
            
            LineCompare o = (LineCompare) obj;
            List<Integer[]> oRanges = o.getHighlightingRanges();
            
            if (file.equals(o.file) && lineNr == o.lineNr) {
                if (oRanges.size() == occurences.size()) {
                    boolean result = true;
                    for (int i = 0; i < occurences.size(); i++) {
                        if (!Arrays.equals(occurences.get(i), oRanges.get(i))) {
                            result = false;
                            break;
                        }
                    }
                    return result;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    
    @Override
    public String toString() {
        if (oldStringDisplay != null && newStringDisplay != null) {
            String first = "Line " + lineNr + ":\t";
            String second = oldStringDisplay.trim() + "\n\t";
            drawOffsetOldString = first.length() - countBeginningWhitespaces(oldStringDisplay);
            drawOffsetNewString = first.length() + second.length() - countBeginningWhitespaces(newStringDisplay);
            return first + second + newStringDisplay.trim();
        } else if (file != null) {
            return file.getAbsolutePath() + ":";
        } else {
            return annotation;
        }
    }
}
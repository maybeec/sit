package io.github.maybeec.sit.logic;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.filechooser.FileFilter;

import io.github.maybeec.sit.exceptions.NoCharsetDeterminedException;
import io.github.maybeec.sit.model.LineCompare;
import io.github.maybeec.sit.model.LineCompare.Action;
import io.github.maybeec.sit.tools.PropertiesManager;
import io.github.maybeec.sit.ui.MainWindow;
import io.github.maybeec.sit.ui.Settings;

public class UIController implements ActionListener, MouseListener, KeyListener {
    
    private PropertiesManager pm = PropertiesManager.getInstance();
    private MainWindow ui;
    private Settings settings;
    
    public UIController(MainWindow gui) {
        ui = gui;
    }
    
    @Override
    public void actionPerformed(ActionEvent ev) {
        // UI -> Browse
        if (ev.getActionCommand().equals("browse")) {
            uiBrowse();
            
            // UI -> Search
        } else if (ev.getActionCommand().equals("search")) {
            uiSearch();
            
            // UI -> Replace
        } else if (ev.getActionCommand().equals("replaceAll")) {
            
            uiReplace();
            
            // UI -> Settings
        } else if (ev.getActionCommand().equals("settings")) {
            settings = new Settings(this);
            
            // SETTINGS -> Browse notepad++.exe
        } else if (ev.getActionCommand().equals("settings_browseNotepad")) {
            settingsBrowse();
            
            // SETTINGS -> Cancel
        } else if (ev.getActionCommand().equals("settings_cancel")) {
            settings.dispose();
            settings = null;
            
            // SETTINGS -> Save
        } else if (ev.getActionCommand().equals("settings_save")) {
            settingsSave();
        }
    }
    
    private void uiBrowse() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if (!new File(ui.getFolder()).exists()) {
            fc.setCurrentDirectory(new File("."));
        } else {
            fc.setCurrentDirectory(new File(ui.getFolder()));
        }
        
        fc.setAcceptAllFileFilterUsed(false);
        
        int state = fc.showOpenDialog(ui);
        if (state == JFileChooser.APPROVE_OPTION) {
            String folders = "";
            for (File f : fc.getSelectedFiles()) {
                folders += (folders.isEmpty() ? "" : ",") + f.getAbsolutePath();
            }
            ui.setFolder(folders);
        }
    }
    
    private void uiSearch() {
        ui.clearList();
        
        String[] folders = ui.getFolder().split(",");
        Parser parser = new Parser(ui, this);
        
        try {
            parser.search(ui.getSearchTexts(), folders, ui.getFileFilter().split(","));
        } catch (Throwable ex) {
            JOptionPane.showMessageDialog(ui, ex.toString(), "Unknown Error", JOptionPane.ERROR_MESSAGE);
        }
        
        //save search data
        pm.setProperty(PropertiesManager.SEARCH_FOLDER, ui.getFolder());
        pm.setProperty(PropertiesManager.FILE_FILTER, ui.getFileFilter());
        pm.setProperty(PropertiesManager.SEARCH_TEXT, ui.getSearchText());
        pm.setProperty(PropertiesManager.REPLACEMENT, ui.getReplacement());
        pm.setProperty(PropertiesManager.CASE_SENSITIVE, "" + ui.isCaseSensitive());
        pm.setProperty(PropertiesManager.USE_REGEX, "" + ui.isRegex());
    }
    
    public void setResults(List<LineCompare> result, int searchedFolders) {
        System.out.println("setResults");
        try {
            if (result.size() == 0) {
                ui.addListItem(new LineCompare("No Matches!"));
            } else {
                ProgressMonitor monitor = new ProgressMonitor(ui, "Generating ...", "Replacements", 0, result.size());
                for (int i = 0; i < result.size(); i++) {
                    result.get(i).setReplacementString(ui.getReplacement());
                    ui.addListItem(result.get(i));
                    monitor.setProgress(i + 1);
                }
            }
            JOptionPane.showMessageDialog(ui, countFinds(result) + " matches in " + searchedFolders + " files.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Throwable ex) {
            JOptionPane.showMessageDialog(ui, ex.toString(), "Unknown Error", JOptionPane.ERROR_MESSAGE);
        }
        ui.updateList();
    }
    
    private void uiReplace() {
        if (JOptionPane.showConfirmDialog(ui, "Do you really want to replace all selected occurences?") == JOptionPane.OK_OPTION) {
            List<LineCompare> worklist = new LinkedList<LineCompare>();
            for (LineCompare lc : ui.getListItems()) {
                if (lc.getAction().equals(Action.ACCEPT)) {
                    worklist.add(lc);
                }
            }
            
            try {
                new ReplaceOperator(ui).replace(worklist);
                JOptionPane.showMessageDialog(ui, worklist.size() + " occurences replaced successfully.");
            } catch (NoCharsetDeterminedException e) {
                JOptionPane.showMessageDialog(ui, "Encoding could not be determined for\n" + e.getFile().getAbsolutePath() + "\n"
                        + "This should not occur, please try again or report this bug!", "Unknown file encoding", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    @SuppressWarnings("rawtypes")
    private void settingsSave() {
        pm.setProperty(PropertiesManager.NOTEPAD, settings.getNotepadFolder());
        pm.setProperty(PropertiesManager.IGNORED_FOLDERS, settings.getFilesToIgnore());
        pm.setProperty(PropertiesManager.ONLY_STRING_SEARCH, settings.getOnlyStringSearchFilter());
        
        Vector source = settings.getLineStartupsToIgnore();
        HashMap<String, String> target = new HashMap<String, String>();
        
        for (Object o : source) {
            if (o instanceof Vector) {
                if (((Vector) o).get(0) != null && ((Vector) o).get(1) != null) {
                    target.put(((Vector) o).get(0).toString(), ((Vector) o).get(1).toString());
                }
            }
        }
        pm.setLineStartups(target);
        
        settings.dispose();
        settings = null;
    }
    
    private void settingsBrowse() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new FileFilter() {
            
            @Override
            public String getDescription() {
                return "notepad++.exe";
            }
            
            @Override
            public boolean accept(File f) {
                if (f.isFile() && f.getName().equalsIgnoreCase("notepad++.exe") || f.isDirectory()) {
                    return true;
                } else {
                    return false;
                }
            }
        });
        
        if (!new File(settings.getNotepadFolder()).exists()) {
            fc.setCurrentDirectory(new File("."));
        } else {
            fc.setCurrentDirectory(new File(settings.getNotepadFolder()));
        }
        
        int state = fc.showOpenDialog(settings);
        if (state == JFileChooser.APPROVE_OPTION) {
            settings.setNotepadFolder(fc.getSelectedFile().getAbsolutePath());
        }
    }
    
    private int countFinds(List<LineCompare> list) {
        int result = 0;
        for (LineCompare lc : list) {
            if (lc.isFoundEntity()) {
                result++;
            }
        }
        return result;
    }
    
    private void openFirstInEditor(List<LineCompare> objects) {
        if (pm.getProperty(PropertiesManager.NOTEPAD).equals(PropertiesManager.UNDEFINED)) {
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().edit(objects.get(0).getFile());
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(ui, "An error occured while opening the file using the system editor!", "An error occured",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(ui, "Cannot open file with system editor!\nTry open files using Notepad++.", "An error occured",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            try {
                Runtime.getRuntime().exec("\"" + pm.getProperty(PropertiesManager.NOTEPAD) + "\" -n" + objects.get(0).getLine() + " \"" + objects.get(
                        0).getFile().getAbsolutePath() + "\"");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(ui,
                        "Cannot open file using Notepad++!\nCheck whether the settings refer to an valid notepad++.exe which is executable.\n"
                                + "Error: " + e.getLocalizedMessage(), "An error occured", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    @Override
    public void mouseClicked(MouseEvent me) {
        List<LineCompare> objects = ui.getSelectedListItems();
        
        if (objects.size() > 1) return;
        
        //double click
        if (me.getClickCount() == 2) {
            openFirstInEditor(objects);
        }
    }
    
    @Override
    public void mouseEntered(MouseEvent arg0) {
    }
    
    @Override
    public void mouseExited(MouseEvent arg0) {
    }
    
    @Override
    public void mousePressed(MouseEvent arg0) {
    }
    
    @Override
    public void mouseReleased(MouseEvent arg0) {
    }
    
    @Override
    public void keyPressed(KeyEvent arg0) {
        
        if (arg0.isControlDown() && arg0.getKeyCode() == KeyEvent.VK_X) {
            for (LineCompare sc : ui.getSelectedListItems()) {
                sc.setAction(Action.ACCEPT);
                ui.refreshListSelection(ui.getSelectedListItems());
            }
        }
        
        if (arg0.isControlDown() && arg0.getKeyCode() == KeyEvent.VK_Y) {
            for (LineCompare sc : ui.getSelectedListItems()) {
                sc.setAction(Action.IGNORE);
                ui.refreshListSelection(ui.getSelectedListItems());
            }
        }
        
        if (arg0.getKeyCode() == KeyEvent.VK_ENTER) {
            List<LineCompare> objects = ui.getSelectedListItems();
            openFirstInEditor(objects);
        }
    }
    
    @Override
    public void keyReleased(KeyEvent arg0) {
        
    }
    
    @Override
    public void keyTyped(KeyEvent arg0) {
        
    }
}

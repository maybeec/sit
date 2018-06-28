package io.github.maybeec.sit.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.util.Arrays;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import io.github.maybeec.sit.logic.UIController;
import io.github.maybeec.sit.tools.PropertiesManager;

public class Settings extends JFrame {
    
    private static final long serialVersionUID = 7778891077333540306L;
    
    private JTextField notepad = new JTextField();
    private JTextField foldersToIgnore = new JTextField();
    private JTextField onlyStringFileFilter = new JTextField();
    private JTable ignoredLineStartupTable = new JTable();
    private DefaultTableModel tableModel;
    
    public Settings(UIController uic) {
        super("Settings");
        Dimension frameSize = new Dimension(500, 500);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int top = (screenSize.height - frameSize.height) / 2;
        int left = (screenSize.width - frameSize.width) / 2;
        setSize(frameSize);
        setLocation(left, top);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);
        createUI(uic);
        loadProperties();
        setVisible(true);
    }
    
    private void loadProperties() {
        PropertiesManager pm = PropertiesManager.getInstance();
        
        String prop;
        if (!(prop = pm.getProperty(PropertiesManager.NOTEPAD)).equals(PropertiesManager.UNDEFINED)) {
            notepad.setText(prop);
        }
        if (!(prop = pm.getProperty(PropertiesManager.IGNORED_FOLDERS)).equals(PropertiesManager.UNDEFINED)) {
            foldersToIgnore.setText(sort(prop));
        }
        if (!(prop = pm.getProperty(PropertiesManager.ONLY_STRING_SEARCH)).equals(PropertiesManager.UNDEFINED)) {
            onlyStringFileFilter.setText(sort(prop));
        }
        
        //load line startups to ignore
        Map<String, String> ignoredLineStartups = pm.getLineStartupsToIgnore();
        String[][] data = new String[ignoredLineStartups.size()][2];
        
        int i = 0;
        for (String key : ignoredLineStartups.keySet()) {
            data[i][0] = key;
            data[i][1] = ignoredLineStartups.get(key);
            i++;
        }
        
        tableModel = new DefaultTableModel();
        tableModel.setDataVector(data, new String[] { "Files", "Starting snippet" });
        tableModel.setRowCount(data.length + 1);
        ignoredLineStartupTable.setModel(tableModel);
    }
    
    private void createUI(UIController uic) {
        this.setLayout(new GridBagLayout());
        
        GridBagConstraints c = new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10,
                10, 0, 10), 0, 0);
        this.add(new JLabel("Notepad++ path:"), c);
        
        c.gridy = 2;
        c.gridwidth = 2;
        this.add(new JLabel("Ignored files/folders (comma separated list):"), c);
        
        c.gridy = 4;
        this.add(new JLabel("File filter for 'only string' search (comma separated list):"), c);
        
        c.gridy = 7;
        this.add(new JLabel("Ignore Lines in files with starting strings (comma separated)"), c);
        
        c.gridy = 1;
        c.gridwidth = 1;
        c.insets = new Insets(10, 10, 10, 10);
        this.add(notepad, c);
        
        c.gridy = 3;
        this.add(foldersToIgnore, c);
        
        c.gridy = 6;
        this.add(onlyStringFileFilter, c);
        
        c.gridy = 8;
        c.weighty = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        JScrollPane sc = new JScrollPane(ignoredLineStartupTable);
        this.add(sc, c);
        
        c = new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0);
        JButton but = new JButton("Browse");
        but.setActionCommand("settings_browseNotepad");
        but.addActionListener(uic);
        this.add(but, c);
        
        c = new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0);
        but = new JButton("Cancel");
        but.setActionCommand("settings_cancel");
        but.addActionListener(uic);
        this.add(but, c);
        
        c.gridx = 1;
        but = new JButton("Save");
        but.setActionCommand("settings_save");
        but.addActionListener(uic);
        this.add(but, c);
    }
    
    public void setNotepadFolder(String absolutePath) {
        notepad.setText(absolutePath);
    }
    
    public String getNotepadFolder() {
        return notepad.getText();
    }
    
    public String getFilesToIgnore() {
        return foldersToIgnore.getText();
    }
    
    public String getOnlyStringSearchFilter() {
        return onlyStringFileFilter.getText();
    }
    
    @SuppressWarnings("rawtypes")
    public Vector getLineStartupsToIgnore() {
        if (ignoredLineStartupTable.getCellEditor() != null) {
            ignoredLineStartupTable.getCellEditor().stopCellEditing();
        }
        return tableModel.getDataVector();
    }
    
    private String sort(String list) {
        String[] arr = list.split(",");
        Arrays.sort(arr);
        
        String result = "";
        for (String s : arr) {
            result += (result.isEmpty() ? "" : ",") + s;
        }
        return result;
    }
}

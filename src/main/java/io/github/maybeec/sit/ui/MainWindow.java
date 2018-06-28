package io.github.maybeec.sit.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import io.github.maybeec.sit.logic.UIController;
import io.github.maybeec.sit.model.CompareListModel;
import io.github.maybeec.sit.model.LineCompare;
import io.github.maybeec.sit.tools.PropertiesManager;

public class MainWindow extends JFrame {
    
    private static final long serialVersionUID = -9195139647756971052L;
    
    private final JTextField folder = new JTextField();
    private final JTextField fileFilter = new JTextField("*.*");
    private final JTextField searchText = new JTextField();
    private final JTextField replacement = new JTextField();
    private final JCheckBox caseSensitive = new JCheckBox("case sensitive");
    private final JCheckBox isRegex = new JCheckBox("isRegex (first group will be replaced)");
    private final JList<LineCompare> finds = new JList<LineCompare>();
    private final CompareListModel findsModel = new CompareListModel();
    
    public MainWindow() {
        super("Search it");
        Dimension frameSize = new Dimension(1000, 600);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int top = (screenSize.height - frameSize.height) / 2;
        int left = (screenSize.width - frameSize.width) / 2;
        setSize(frameSize);
        setLocation(left, top);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        UIController uic = new UIController(this);
        createMenu(uic);
        createUI(uic);
        loadProperties();
        this.setVisible(true);
    }
    
    private void loadProperties() {
        PropertiesManager pm = PropertiesManager.getInstance();
        
        String prop;
        if (!(prop = pm.getProperty(PropertiesManager.SEARCH_FOLDER)).equals(PropertiesManager.UNDEFINED)) {
            folder.setText(prop);
        }
        if (!(prop = pm.getProperty(PropertiesManager.FILE_FILTER)).equals(PropertiesManager.UNDEFINED)) {
            fileFilter.setText(prop);
        }
        if (!(prop = pm.getProperty(PropertiesManager.CASE_SENSITIVE)).equals(PropertiesManager.UNDEFINED)) {
            caseSensitive.setSelected(Boolean.parseBoolean(prop));
        }
        if (!(prop = pm.getProperty(PropertiesManager.SEARCH_TEXT)).equals(PropertiesManager.UNDEFINED)) {
            searchText.setText(prop);
        }
        if (!(prop = pm.getProperty(PropertiesManager.REPLACEMENT)).equals(PropertiesManager.UNDEFINED)) {
            replacement.setText(prop);
        }
        if (!(prop = pm.getProperty(PropertiesManager.USE_REGEX)).equals(PropertiesManager.UNDEFINED)) {
            isRegex.setSelected(Boolean.parseBoolean(prop));
        }
    }
    
    private void createMenu(UIController uic) {
        JMenuBar menuBar = new JMenuBar();
        this.setJMenuBar(menuBar);
        
        JMenu menu = new JMenu("Advanced");
        menuBar.add(menu);
        
        JMenuItem menuItem = new JMenuItem("Settings");
        menuItem.setActionCommand("settings");
        menuItem.addActionListener(uic);
        menu.add(menuItem);
    }
    
    private void createUI(UIController uic) {
        this.setLayout(new GridBagLayout());
        
        GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10,
                10, 10, 10), 0, 0);
        this.add(new JLabel("Search in folder:"), c);
        
        c.gridy = 1;
        this.add(new JLabel("File filter (comma separated list):"), c);
        
        c.gridy = 2;
        this.add(new JLabel("Search text:"), c);
        
        c.gridy = 4;
        this.add(new JLabel("Replacement:"), c);
        
        c = new GridBagConstraints(1, 0, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 10, 10), 0, 0);
        this.add(folder, c);
        
        c.gridy = 1;
        this.add(fileFilter, c);
        
        c.gridy = 2;
        this.add(searchText, c);
        
        c.gridy = 4;
        this.add(replacement, c);
        
        c = new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 10, 10), 0, 0);
        c.weightx = 0;
        this.add(caseSensitive, c);
        
        c.gridx = 2;
        this.add(isRegex, c);
        
        c = new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 10, 10), 0, 0);
        JButton but = new JButton("Browse");
        but.setActionCommand("browse");
        but.addActionListener(uic);
        this.add(but, c);
        
        c.gridy = 4;
        but = new JButton("Search");
        but.setActionCommand("search");
        but.addActionListener(uic);
        this.add(but, c);
        
        c.gridy = 6;
        but = new JButton("Replace changes");
        but.setActionCommand("replaceAll");
        but.addActionListener(uic);
        this.add(but, c);
        
        c = new GridBagConstraints(0, 5, 4, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 10, 10, 10), 0, 0);
        finds.setCellRenderer(new ColorListCellRenderer(finds));
        finds.setModel(findsModel);
        finds.addMouseListener(uic);
        finds.addKeyListener(uic);
        
        JScrollPane sc = new JScrollPane(finds);
        this.add(sc, c);
    }
    
    public void refreshListSelection(List<LineCompare> selectedListItems) {
        findsModel.refreshSelection(selectedListItems);
    }
    
    public String getFolder() {
        return folder.getText();
    }
    
    public void setFolder(String txt) {
        folder.setText(txt);
    }
    
    public String getFileFilter() {
        return fileFilter.getText();
    }
    
    public String getSearchText() {
        return searchText.getText().trim();
    }
    
    public String[] getSearchTexts() {
        if (isRegex.isSelected()) {
            return new String[] { searchText.getText() };
        } else {
            Set<String> result = new HashSet<String>(Arrays.asList(searchText.getText().split("\\|"))); //TODO allow escaping |
            //delete empty strings 
            Iterator<String> it = result.iterator();
            while (it.hasNext()) {
                String next = it.next();
                if (next.trim().isEmpty()) {
                    it.remove();
                }
            }
            return result.toArray(new String[0]);
        }
    }
    
    public String getReplacement() {
        return replacement.getText();
    }
    
    public List<LineCompare> getListItems() {
        return findsModel.getElements();
    }
    
    public List<LineCompare> getSelectedListItems() {
        return finds.getSelectedValuesList();
    }
    
    public void clearList() {
        findsModel.clear();
    }
    
    public void addListItem(LineCompare sc) {
        findsModel.addElement(sc);
    }
    
    public boolean isCaseSensitive() {
        return caseSensitive.isSelected();
    }
    
    public void updateList() {
        findsModel.refresh();
    }
    
    public boolean isRegex() {
        return isRegex.isSelected();
    }
}

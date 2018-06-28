package io.github.maybeec.sit.logic;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.swing.JOptionPane;

import io.github.maybeec.sit.exceptions.NoCharsetDeterminedException;
import io.github.maybeec.sit.model.LineCompare;
import io.github.maybeec.sit.ui.MainWindow;

public class ReplaceOperator {
    
    private HashMap<File, HashMap<Integer, String>> fileLineMap = new HashMap<File, HashMap<Integer, String>>();
    private MainWindow ui;
    
    public ReplaceOperator(MainWindow ui) {
        this.ui = ui;
    }
    
    public void replace(List<LineCompare> replacements) throws NoCharsetDeterminedException {
        fillFileLineMap(replacements);
        
        for (final File f : fileLineMap.keySet()) {
            
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    HashMap<Integer, String> lines = fileLineMap.get(f);
                    try {
                        FileOperator fo = new FileOperator(f);
                        String line = null;
                        String newText = "";
                        int lineNr = 1;
                        
                        while ((line = fo.readLine()) != null) {
                            if (lines.containsKey(lineNr)) {
                                newText += lines.get(lineNr) + "\r\n";
                            } else {
                                newText += line + "\r\n";
                            }
                            lineNr++;
                        }
                        fo.finishReading();
                        
                        fo.writeFile(newText);
                        
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(ui, "Error while reading/writing file: \n" + f.getAbsolutePath(), "An error occured",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        }
    }
    
    private void fillFileLineMap(List<LineCompare> replacements) {
        
        for (LineCompare lc : replacements) {
            
            if (fileLineMap.containsKey(lc.getFile())) {
                fileLineMap.get(lc.getFile()).put(lc.getLine(), lc.getNewString());
            } else {
                HashMap<Integer, String> lines = new HashMap<Integer, String>();
                lines.put(lc.getLine(), lc.getNewString());
                fileLineMap.put(lc.getFile(), lines);
            }
        }
    }
}

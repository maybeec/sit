package io.github.maybeec.sit.ui;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JTextPane;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;

import io.github.maybeec.sit.model.LineCompare;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class ColorListCellRenderer implements ListCellRenderer<LineCompare> {
    
    private HashMap<LineCompare, JTextPane> cellMap = new HashMap<LineCompare, JTextPane>();
    private HighlightPainter painter = new UnderlineHighlightPainter(Color.RED);
    private Border selectedBorder;
    private Border whiteBorder;
    private Border nonFocusBorder;
    
    public ColorListCellRenderer(JList<?> list) {
        whiteBorder = BorderFactory.createLineBorder(list.getBackground());
        nonFocusBorder = BorderFactory.createLineBorder(list.getSelectionBackground());
        selectedBorder = BorderFactory.createLineBorder(list.getSelectionBackground().darker());
    }
    
    @Override
    public Component getListCellRendererComponent(JList<? extends LineCompare> list, LineCompare value, int index, boolean isSelected,
            boolean cellHasFocus) {
        
        JTextPane textPane;
        
        if (cellMap.containsKey(value)) {
            textPane = cellMap.get(value);
        } else {
            textPane = new JTextPane();
            
            List<Integer[]> ranges = value.getHighlightingRanges();
            
            if (ranges.size() == 0) {
                textPane.setText(value.toString());
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setBackground(attrs, Color.lightGray);
                StyledDocument sdoc = textPane.getStyledDocument();
                sdoc.setCharacterAttributes(0, textPane.getText().length(), attrs, true);
            } else {
                String str = value.toString();
                textPane.setText(str);
                
                Highlighter highlighter = textPane.getHighlighter();
                highlighter.removeAllHighlights();
                for (Integer[] i : ranges) {
                    try {
                        if (i[0] == 0) {
                            highlighter.addHighlight(i[1] + value.getDrawOffsetOldString(), i[2] + value.getDrawOffsetOldString(), painter);
                        } else {
                            highlighter.addHighlight(i[1] + value.getDrawOffsetNewString(), i[2] + value.getDrawOffsetNewString(), painter);
                        }
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }
            }
            cellMap.put(value, textPane);
        }
        
        //color definitions
        Color dColor = list.getBackground();
        Color accept = new Color(50, 205, 50);
        
        if (isSelected) {
            switch (value.getAction()) {
            case ACCEPT:
                textPane.setBackground(accept.brighter().brighter());
                break;
            case IGNORE:
                textPane.setBackground(list.getSelectionBackground());
                break;
            }
            
            textPane.setForeground(list.getSelectionForeground());
            if (cellHasFocus) {
                textPane.setBorder(selectedBorder);
            } else {
                textPane.setBorder(nonFocusBorder);
            }
        } else {
            textPane.setBorder(whiteBorder);
            
            switch (value.getAction()) {
            case ACCEPT:
                textPane.setBackground(accept);
                break;
            case IGNORE:
                textPane.setBackground(dColor);
                break;
            }
            textPane.setForeground(list.getForeground());
        }
        return textPane;
    }
}

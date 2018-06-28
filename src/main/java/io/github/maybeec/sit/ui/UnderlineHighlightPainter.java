package io.github.maybeec.sit.ui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.Position;
import javax.swing.text.View;

/** Painter for underlined highlights */
public class UnderlineHighlightPainter extends LayeredHighlighter.LayerPainter {
    
    public UnderlineHighlightPainter(Color c) {
        color = c;
    }
    
    public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c) {
        // Do nothing: this method will never be called
    }
    
    @Override
    public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, View view) {
        g.setColor(color == null ? c.getSelectionColor() : color);
        
        Rectangle alloc = null;
        if (offs0 == view.getStartOffset() && offs1 == view.getEndOffset()) {
            if (bounds instanceof Rectangle) {
                alloc = (Rectangle) bounds;
            } else {
                alloc = bounds.getBounds();
            }
        } else {
            try {
                Shape shape = view.modelToView(offs0, Position.Bias.Forward, offs1, Position.Bias.Backward, bounds);
                alloc = (shape instanceof Rectangle) ? (Rectangle) shape : shape.getBounds();
            } catch (BadLocationException e) {
                return null;
            }
        }
        
        FontMetrics fm = c.getFontMetrics(c.getFont());
        int baseline = alloc.y + alloc.height - fm.getDescent() + 1;
        g.drawLine(alloc.x, baseline, alloc.x + alloc.width, baseline);
        g.drawLine(alloc.x, baseline + 1, alloc.x + alloc.width, baseline + 1);
        
        return alloc;
    }
    
    /** The color for the underline */
    protected Color color;
}

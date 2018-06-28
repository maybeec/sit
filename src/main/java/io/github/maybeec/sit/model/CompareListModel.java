package io.github.maybeec.sit.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class CompareListModel implements ListModel<LineCompare>, PropertyChangeListener {
    
    private LinkedList<ListDataListener> listeners = new LinkedList<ListDataListener>();
    private LinkedList<LineCompare> data = new LinkedList<LineCompare>();
    
    public void addElement(LineCompare sc) {
        if (!data.contains(sc)) {
            data.add(sc);
        }
        
        firePropertyChanged(data.size(), data.size() - 1, ListDataEvent.INTERVAL_ADDED);
    }
    
    public List<LineCompare> getElements() {
        return new LinkedList<LineCompare>(data);
    }
    
    public void refreshSelection(List<LineCompare> selectedListItems) {
        for (LineCompare lc : selectedListItems) {
            firePropertyChanged(data.indexOf(lc), data.indexOf(lc), ListDataEvent.CONTENTS_CHANGED);
        }
    }
    
    public void refresh() {
        firePropertyChanged(0, data.size() - 1, ListDataEvent.CONTENTS_CHANGED);
    }
    
    public void clear() {
        int oldSize = data.size();
        data.clear();
        
        firePropertyChanged(0, oldSize, ListDataEvent.INTERVAL_ADDED);
    }
    
    private void firePropertyChanged(int from, int to, int type) {
        for (ListDataListener l : listeners) {
            l.contentsChanged(new ListDataEvent(this, type, 0, data.size() - 1));
        }
    }
    
    @Override
    public void addListDataListener(ListDataListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }
    
    @Override
    public LineCompare getElementAt(int index) {
        return data.get(index);
    }
    
    @Override
    public int getSize() {
        return data.size();
    }
    
    @Override
    public void removeListDataListener(ListDataListener l) {
        listeners.remove(l);
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        firePropertyChanged(0, data.size() - 1, ListDataEvent.CONTENTS_CHANGED);
    }
}

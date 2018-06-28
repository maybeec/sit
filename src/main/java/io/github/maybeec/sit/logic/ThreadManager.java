package io.github.maybeec.sit.logic;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;

public class ThreadManager extends Thread {
    
    protected Thread[] threads;
    protected List<Runnable> queue = Collections.synchronizedList(new LinkedList<Runnable>());
    protected ProgressMonitor progressMonitor;
    private Parser parser;
    
    public ThreadManager(Parser parser, ProgressMonitor progressMonitor) {
        this.progressMonitor = progressMonitor;
        this.parser = parser;
        if (getNoOfAvailableProcessors() < 2) {
            threads = new Thread[1];
        } else {
            threads = new Thread[getNoOfAvailableProcessors() - 1];
        }
    }
    
    public void addTask(Runnable runnable) {
        queue.add(runnable);
    }
    
    private int getNoOfAvailableProcessors() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.availableProcessors();
    }
    
    private void startNewThread(int index, Runnable runnable) {
        threads[index] = new Thread(runnable);
        threads[index].start();
    }
    
    @Override
    public void run() {
        initializeProgressMonitor(queue.size());
        
        while (!queue.isEmpty() && (progressMonitor == null || (progressMonitor != null && !progressMonitor.isCanceled()))) {
            for (int i = 0; i < threads.length; i++) {
                if (!queue.isEmpty() && ((threads[i] != null && !threads[i].isAlive()) || threads[i] == null)) {
                    startNewThread(i, queue.remove(0));
                    update(progressMonitor.getMaximum() - queue.size());
                }
            }
        }
        
        blockUntilAllThreadsFinished();
        parser.searchingFinished();
    }
    
    private void blockUntilAllThreadsFinished() {
        boolean finished = false;
        while (!finished) {
            finished = !threads[0].isAlive();
            for (Thread t : threads) {
                finished = finished && !t.isAlive();
            }
        }
    }
    
    private void update(final int state) {
        if (progressMonitor == null) return;
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                progressMonitor.setProgress(state);
                progressMonitor.setNote(state + "/" + progressMonitor.getMaximum());
            }
        });
    }
    
    private void initializeProgressMonitor(final int size) {
        if (progressMonitor == null) return;
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                progressMonitor.setProgress(0);
                progressMonitor.setMaximum(size);
            }
        });
    }
}

package io.github.maybeec.sit.exceptions;

import java.io.File;

public class NoCharsetDeterminedException extends Exception {
    
    private static final long serialVersionUID = 248497830175676998L;
    private final File file;
    
    public NoCharsetDeterminedException(File f) {
        file = f;
    }
    
    public File getFile() {
        return file;
    }
}

package io.github.maybeec.sit.logic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import org.mozilla.universalchardet.UniversalDetector;

public class FileOperator {
    
    private final File file;
    private final String encoding;
    private boolean readingInitialized = false;
    private FileInputStream fstream;
    private InputStreamReader in;
    private BufferedReader br;
    
    FileOperator(File file) throws UnsupportedEncodingException, IOException {
        this.file = file;
        encoding = getCharSet(file);
    }
    
    String readLine() throws IOException {
        if (!readingInitialized) {
            fstream = new FileInputStream(file);
            in = new InputStreamReader(fstream, encoding);
            br = new BufferedReader(in);
            readingInitialized = true;
        }
        return br.readLine();
    }
    
    void finishReading() throws IOException {
        if (br != null && in != null) {
            fstream.close();
            in.close();
            br.close();
            readingInitialized = false;
        }
    }
    
    void writeFile(String content) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), encoding);
        writer.write(content);
        writer.close();
    }
    
    private String getCharSet(File f) throws IOException {
        
        byte[] buf = new byte[4096];
        FileInputStream fis = new FileInputStream(f);
        
        UniversalDetector detector = new UniversalDetector(null);
        
        int nread;
        while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
            detector.handleData(buf, 0, nread);
        }
        detector.dataEnd();
        fis.close();
        
        String encoding = detector.getDetectedCharset();
        if (encoding != null) {
            return encoding;
        } else {
            return "UTF-8";
        }
    }
}

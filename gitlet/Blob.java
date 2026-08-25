package gitlet;

import java.io.Serializable;

public class Blob implements Serializable {
    private String filename;
    private String fileContents;

    public Blob(String filename, String fileContents) {
        this.filename = filename;
        this.fileContents = fileContents;
    }

    public String getFilename() {
        return filename;
    }

    public String getFileContents() {
        return getFilename();
    }
}

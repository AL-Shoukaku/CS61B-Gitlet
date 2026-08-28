package gitlet;

import java.io.Serializable;

public class Blob implements Serializable {
    private final byte[] fileContents;

    public Blob(byte[] fileContents) {
        this.fileContents = fileContents;
    }

    public byte[] getFileContents() {
        return fileContents;
    }
}

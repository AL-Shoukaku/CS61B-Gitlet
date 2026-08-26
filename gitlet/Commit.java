package gitlet;

// TODO: any imports you need here

import java.io.Serializable;
import java.util.Date; // TODO: You'll likely use this in this class
import java.util.HashMap;
import java.util.HashSet;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author AL-Shoukaku
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private final String message;
    private final String firstParent;
    private final String secondParent;
    private HashMap<String, String> blobs;  // <name, blob>
    private final Date date;

    public Commit(String message, String first, String second, Date date) {
        this.message = message;
        this.date = date;
        this.firstParent = first;
        this.secondParent = second;
        this.blobs = new HashMap<>();
    }

    public String getFirstParent() {
        return firstParent;
    }

    public String getSecondParent() {
        return secondParent;
    }

    public String getMessage() {
        return message;
    }

    public Date getDate() {
        return date;
    }

    public void addBlob(String filename, String sha1) {
        this.blobs.put(filename, sha1);
    }

    /** 根据文件名判断是否在改 commit 中 */
    public boolean hasFile(String filename) {
        return blobs.containsKey(filename);
    }

    /** 根据文件名获取对应的 blob */
    public Blob getBlob(String filename) {
        if (!blobs.containsKey(filename)) {
            return null;
        }
        return Repository.getBlob(blobs.get(filename));
    }

    /** 获取整个 blobs */
    public HashMap<String, String> getBlobs() {
        return blobs;
    }
}

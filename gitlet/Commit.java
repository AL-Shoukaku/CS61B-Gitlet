package gitlet;

import java.io.Serializable;
import java.util.Date;
import java.util.TreeMap;

/** Represents a gitlet commit object.
 *  @author AL-Shoukaku
 */
public class Commit implements Serializable {
    /**
     *
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private final String message;
    private final String firstParent;
    private final String secondParent;
    private TreeMap<String, String> blobs;  // <name, blob>
    private final Date date;

    public Commit(String message, String first, String second, Date date) {
        this.message = message;
        this.date = date;
        this.firstParent = first;
        this.secondParent = second;
        this.blobs = new TreeMap<>();
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

    /** 根据 filename 获取对应的 blob 的 sha1 */
    public String getBlobSha1(String filename) {
        return blobs.get(filename);
    }

    /** 获取整个 blobs */
    public TreeMap<String, String> getBlobs() {
        return blobs;
    }
}

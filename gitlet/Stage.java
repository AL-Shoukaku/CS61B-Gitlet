package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

/** 用于记录缓存区的信息，包括：
 * add 文件的名字-sha1的映射
 * remove 文件的名字
 */
public class Stage implements Serializable {
    private HashMap<String, String> addFile;
    private HashSet<String> removeFile;

    public Stage() {
        this.addFile = new HashMap<>();
        this.removeFile = new HashSet<>();
    }

    public void addFile(String name, String sha1) {
        addFile.put(name, sha1);
    }

    public void removeFile(String name) {
        removeFile.add(name);
    }

    public HashMap<String, String> getAddFile() {
        return addFile;
    }

    public HashSet<String> getRemoveFile() {
        return removeFile;
    }

    public boolean hasStage(String filename) {
        return addFile.containsKey(filename);
    }

    public boolean hasRemove(String filename) {
        return removeFile.contains(filename);
    }

    public void removeStage(String name) {
        Repository.deleteBlob(addFile.get(name));
        addFile.remove(name);
    }

    public void deleteRemove(String name) {
        removeFile.remove(name);
    }
}

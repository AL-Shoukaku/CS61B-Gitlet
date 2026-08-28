package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

/** 用于记录缓存区的信息，包括：
 * add 文件的名字-sha1的映射
 * remove 文件的名字
 */
public class Stage implements Serializable {
    private final HashMap<String, String> addFile;
    private final HashSet<String> removeFile;

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

    /** 缓存区是否有该文件 */
    public boolean hasStage(String filename) {
        return addFile.containsKey(filename);
    }

    /** 暂存区是否有该文件的删除记录 */
    public boolean hasRemove(String filename) {
        return removeFile.contains(filename);
    }

    /** 将文件移除暂存区，不删掉对应blob应为可能有别人在引用！ */
    public void removeStage(String name) {
        addFile.remove(name);
    }

    /** 取消一个已删除的记录 */
    public void deleteRemove(String name) {
        removeFile.remove(name);
    }

    /** 暂存区是否为空 */
    public boolean isEmpty() {
        return (addFile.isEmpty() && removeFile.isEmpty());
    }
}

package gitlet;

import java.io.File;
import java.io.IOException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static gitlet.Utils.*;

/** 代表 .gitlet 仓库
 *  封装对于仓库文件的操作，对外提供操作仓库的静态方法
 *  @author AL-Shoukaku
 */
public class Repository {
    /**
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** 仓库的基本目录与文件 */
    public static final File CWD = new File(System.getProperty("user.dir"));
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File COMMITS_DIR = join(GITLET_DIR, "commits");
    public static final File BLOBS_DIR = join(GITLET_DIR, "blobs");
    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches");
    public static final File REMOTES_DIR = join(GITLET_DIR, "remotes");
    public static final File HEAD = join(GITLET_DIR, "head");
    public static final File STAGE = join(GITLET_DIR, "stage");

    /** 创建并初始化 .gitlet 目录 */
    public static void dirInit() {
        GITLET_DIR.mkdir();
        COMMITS_DIR.mkdir();
        BLOBS_DIR.mkdir();
        BRANCHES_DIR.mkdir();
        REMOTES_DIR.mkdir();
        try {
            HEAD.createNewFile();
            STAGE.createNewFile();
        } catch (IOException e) {
            throw new GitletException("dirInit error!\n");
        }
    }

    /** 获取头指针 */
    public static Head getHead() {
        return readObject(HEAD, Head.class);
    }

    /** 写头指针 */
    public static void writeHead(Head head) {
        writeObject(HEAD, head);
    }

    /** 获取暂存区 */
    public static Stage getStage() {
        return readObject(STAGE, Stage.class);
    }

    /** 写入暂存区 */
    public static void writeStage(Stage stage) {
        writeObject(STAGE, stage);
    }

    /** 获取指定 commit */
    public static Commit getCommit(String sha1) {
        if (sha1.length() < 40) {
            // 处理简写情况
            List<String> commitList = Utils.plainFilenamesIn(COMMITS_DIR);
            for (String name : commitList) {
                if (sha1.equals(name.substring(0, sha1.length()))) {
                    sha1 = name;
                    break;
                }
            }
        }
        File file = join(COMMITS_DIR, sha1);
        if (!file.exists()) {
            return null;
        }
        return readObject(file, Commit.class);
    }

    /** 写入一个 commit */
    public static void writeCommit(Commit commit, String sha1) {
        File file = join(COMMITS_DIR, sha1);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new GitletException("create a new commit error!\n");
            }
        }
        writeObject(file, commit);
    }

    /** 获取指定 branch */
    public static Branch getBranch(String name) {
        File file = join(BRANCHES_DIR, name);
        if (!file.exists()) {
            return null;
        }
        return readObject(file, Branch.class);
    }

    /** 写入 branch */
    public static void writeBranch(Branch branch) {
        File file = join(BRANCHES_DIR, branch.getName());
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new GitletException("create a new branch error!\n");
            }
        }
        writeObject(file, branch);
    }

    /** 获取指定 blob */
    public static Blob getBlob(String sha1) {
        File file = join(BLOBS_DIR, sha1);
        if (!file.exists()) {
            return null;
        }
        return readObject(file, Blob.class);
    }

    /** 写入指定 blob */
    public static void writeBlob(Blob blob, String sha1) {
        File file = join(BLOBS_DIR, sha1);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new GitletException("create a new blob error!\n");
            }
        }
        writeObject(file, blob);
    }

    /** 获取指定的远程仓库 */
    public static Remote getRemote(String remoteName) {
        File file = join(REMOTES_DIR, remoteName);
        if (!file.exists()) {
            return null;
        }
        return readObject(file, Remote.class);
    }

    /** 写入远程仓库 */
    public static void writeRemote(Remote remote) {
        File file = join(REMOTES_DIR, remote.getName());
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        writeObject(file, remote);
    }

    /** 删除远程仓库 */
    public static void deleteRemote(String remoteName) {
        File file = join(REMOTES_DIR, remoteName);
        if (file.exists()) {
            file.delete();
        }
    }

    /** 判断给定blob是否与当前commit中的内容完全一样 */
    public static boolean blobEqualCurrentCommit(String filename, Blob blob) {
        if (!getCurrentCommit().hasFile(filename)) {
            return false;
        }
        byte[] b1 = blob.getFileContents();
        byte[] b2 = getCurrentCommit().getBlob(filename).getFileContents();
        return Arrays.equals(b1, b2);
    }

    /** 拿到当前commit */
    public static Commit getCurrentCommit() {
        return getCommit(getHead().getCommit());
    }

    /** 清空暂存区 */
    public static void clearStage() {
        writeStage(new Stage());
    }

    /** 根据 filename 删除当前工作目录的文件 */
    public static void deleteFile(String filename) {
        File file = join(CWD, filename);
        if (file.exists()) {
            file.delete();
        }
    }

    /** 根据 filename 写入当前目录中的文件 */
    public static void writeFile(String filename, Object... contents) {
        File file = join(CWD, filename);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Utils.writeContents(file, contents);
    }

    /** 删除指定分支 */
    public static void deleteBranch(String branchName) {
        File file = join(BRANCHES_DIR, branchName);
        if (file.exists()) {
            file.delete();
        }
    }
}

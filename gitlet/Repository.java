package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** 代表 .gitlet 仓库
 *  封装对于仓库文件的操作，对外提供操作仓库的静态方法
 *  @author AL-Shoukaku
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
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
    public static final File HEAD = join(GITLET_DIR, "head");

    public Repository() {
        // 待补充
    }

    /** 创建并初始化 .gitlet 目录 */
    public static void dirInit() {
        GITLET_DIR.mkdir();
        COMMITS_DIR.mkdir();
        BLOBS_DIR.mkdir();
        BRANCHES_DIR.mkdir();
        try {
            HEAD.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    /** 获取指定 commit */
    public static Commit getCommit(String name) {
        File file = join(COMMITS_DIR, name);
        if (!file.exists()) {
            return null;
        }
        return readObject(file, Commit.class);
    }

    /** 写入一个 commit */
    public static void writeCommit(Commit commit, String name) {
        File file = join(COMMITS_DIR, name);
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
    public static void writeBranch(Branch branch, String name) {
        File file = join(BRANCHES_DIR, name);
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
    public static Blob getBlob(String name) {
        File file = join(BLOBS_DIR, name);
        if (!file.exists()) {
            return null;
        }
        return readObject(file, Blob.class);
    }

    /** 写入指定 blob */
    public static void writeBlob(Blob blob, String name) {
        File file = join(BLOBS_DIR, name);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new GitletException("create a new blob error!\n");
            }
        }
        writeObject(file, blob);
    }
}

package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gitlet.Utils.join;
import static gitlet.Utils.readObject;
import static gitlet.Utils.writeObject;

public class RemoteRepo {
    private final File CWD;
    private final File GITLET_DIR;
    private final File COMMITS_DIR;
    private final File BLOBS_DIR;
    private final File BRANCHES_DIR;
    private final File REMOTES_DIR;
    private final File HEAD;
    private final File STAGE;

    public RemoteRepo(Remote remote) {
        String path = remote.getPath();
        CWD = join(path.substring(0,path.length() - 8));
        GITLET_DIR = join(path);
        COMMITS_DIR = join(GITLET_DIR, "commits");
        BLOBS_DIR = join(GITLET_DIR, "blobs");
        BRANCHES_DIR = join(GITLET_DIR, "branches");
        REMOTES_DIR = join(GITLET_DIR, "remotes");
        HEAD = join(GITLET_DIR, "head");
        STAGE = join(GITLET_DIR, "stage");
    }

    /** 获取头指针 */
    public Head getHead() {
        return readObject(HEAD, Head.class);
    }

    /** 写头指针 */
    public void writeHead(Head head) {
        writeObject(HEAD, head);
    }

    /** 获取暂存区 */
    public Stage getStage() {
        return readObject(STAGE, Stage.class);
    }

    /** 写入暂存区 */
    public void writeStage(Stage stage) {
        writeObject(STAGE, stage);
    }

    /** 获取指定 commit */
    public Commit getCommit(String sha1) {
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
    public void writeCommit(Commit commit, String sha1) {
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
    public Branch getBranch(String name) {
        File file = join(BRANCHES_DIR, name);
        if (!file.exists()) {
            return null;
        }
        return readObject(file, Branch.class);
    }

    /** 写入 branch */
    public void writeBranch(Branch branch) {
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
    public Blob getBlob(String sha1) {
        File file = join(BLOBS_DIR, sha1);
        if (!file.exists()) {
            return null;
        }
        return readObject(file, Blob.class);
    }

    /** 写入指定 blob */
    public void writeBlob(Blob blob, String sha1) {
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
    public Remote getRemote(String remoteName) {
        File file = join(REMOTES_DIR, remoteName);
        if (!file.exists()) {
            return null;
        }
        return readObject(file, Remote.class);
    }

    /** 写入远程仓库 */
    public void writeRemote(Remote remote) {
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
    public void deleteRemote(String remoteName) {
        File file = join(REMOTES_DIR, remoteName);
        if (file.exists()) {
            file.delete();
        }
    }

    /** 判断给定blob是否与当前commit中的内容完全一样 */
    public boolean blobEqualCurrentCommit(String filename, Blob blob) {
        if (!getCurrentCommit().hasFile(filename)) {
            return false;
        }
        byte[] b1 = blob.getFileContents();
        byte[] b2 = getCurrentCommit().getBlob(filename).getFileContents();
        return Arrays.equals(b1, b2);
    }

    /** 拿到当前commit */
    public Commit getCurrentCommit() {
        return getCommit(getHead().getCommit());
    }

    /** 清空暂存区 */
    public void clearStage() {
        writeStage(new Stage());
    }

    /** 根据 filename 删除当前工作目录的文件 */
    public void deleteFile(String filename) {
        File file = join(CWD, filename);
        if (file.exists()) {
            file.delete();
        }
    }

    /** 根据 filename 写入当前目录中的文件 */
    public void writeFile(String filename, Object... contents) {
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
    public void deleteBranch(String branchName) {
        File file = join(BRANCHES_DIR, branchName);
        if (file.exists()) {
            file.delete();
        }
    }

    /** 判断当前仓库是否有 .gitlet */
    public boolean validRepo() {
        return GITLET_DIR.exists();
    }

    /** 接受一次 push */
    public void acceptPush(HashMap<String, Commit> allCommit, String branchName, String commitId) {
        for (Map.Entry<String, Commit> entry : allCommit.entrySet()) {
            String sha1 = entry.getKey();
            Commit commit = entry.getValue();
            Map<String, String> blobs = commit.getBlobs();
            if (hasCommit(sha1)) {
                continue;
            }
            // 将该 commit 的所有 blob 都拷贝过来
            for (String blobSha1 : blobs.values()) {
                if (hasBlob(blobSha1)) {
                    continue;
                }
                Blob blob = Repository.getBlob(blobSha1);
                writeBlob(blob, blobSha1);
            }
            writeCommit(commit, sha1);
        }
        Head head = getHead();
        // 如果要推送的分支不在，则创建并设置到当前的 head
        if (getBranch(branchName) == null) {
            Branch branch = new Branch(branchName);
            branch.setCommit(head.getCommit());
            writeBranch(branch);
        }
        head.setBranch(branchName);
        writeHead(head);
        reset(commitId);
    }

    /** 判断是否有这个 commit */
    public boolean hasCommit(String sha1) {
        return Utils.plainFilenamesIn(COMMITS_DIR).contains(sha1);
    }

    /** 判断是否有这个 blob */
    public boolean hasBlob(String sha1) {
        return Utils.plainFilenamesIn(BLOBS_DIR).contains(sha1);
    }

    /** 执行 reset 命令 */
    public void reset(String commitId) {
        Commit newCommit = getCommit(commitId);
        Commit curCommit = getCurrentCommit();
        Head head = getHead();
        Branch branch = getBranch(head.getBranch());
        if (newCommit == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        List<String> workList = Utils.plainFilenamesIn(CWD);
        for (String name : newCommit.getBlobs().keySet()) {
            if (!curCommit.hasFile(name) && workList.contains(name)) {
                System.out.println("There is an untracked file in the way; delete it, "
                        + "or add and commit it first.");
                return;
            }
        }
        for (String name : curCommit.getBlobs().keySet()) {
            deleteFile(name);
        }
        for (String name : newCommit.getBlobs().keySet()) {
            checkoutFileCommit(name, commitId);
        }
        clearStage();
        head.setCommit(commitId);
        branch.setCommit(commitId);
        writeHead(head);
        writeBranch(branch);
    }

    private void checkoutFileCommit(String filename, String commitId) {
        Commit commit = getCommit(commitId);
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        if (!commit.hasFile(filename)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        writeFile(filename, (Object) commit.getBlob(filename).getFileContents());
    }
}

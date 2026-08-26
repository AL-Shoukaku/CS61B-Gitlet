package gitlet;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static gitlet.Utils.join;
import static gitlet.Utils.readContentsAsString;

public class Gitlet {
    private Repository rep;

    public Gitlet() {

    }

    public static void init(String[] args) {
        if (args.length > 1) {
            errorOperands();
        }
        if (Repository.GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in thecurrent directory.");
            return;
        }
        Repository.dirInit();   // 初始化目录
        // 建立第一个 commit 和 master 分支
        Commit commit = new Commit("initial commit", null, null, new Date(0));
        String commitSHA1 = Utils.sha1((Object) Utils.serialize(commit));
        Branch branch = new Branch("master");
        branch.setCommit(commitSHA1);
        Repository.writeBranch(branch);
        Repository.writeCommit(commit, commitSHA1);
        // 设置好 head 指针
        Head head = new Head();
        head.setBranch(branch.getName());
        head.setCommit(commitSHA1);
        Repository.writeHead(head);
        // 建立空的暂存区
        Repository.writeStage(new Stage());
    }

    public static void add(String[] args) {
        // 不处理文件的删除情况
        if (args.length != 2) {
            errorOperands();
        }
        String name = args[1];
        File file = join(Repository.CWD, name);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            return;
        }
        Stage stage = Repository.getStage();
        Blob blob = new Blob(Utils.readContents(file));
        // 如果已经暂存，则先删除，如果在已删除中，则取消
        if (stage.hasStage(name)) {
            stage.removeStage(name);    //已经暂存，先删除等待重写
        } else if (stage.hasRemove(name)) {
            stage.deleteRemove(name);
        }
        // 如果文件在当前提交中，并且内容一致，则不写入blob
        if (Repository.getCurrentCommit().hasFile(name) && Repository.BlobEqualToCurCommit(name, blob)) {
            return;
        }
        stage.addFile(name, Utils.sha1(Utils.serialize(blob)));
        Repository.writeBlob(blob, Utils.sha1(Utils.serialize(blob)));
        Repository.writeStage(stage);
    }

    public static void commit(String[] args) {
        if (args.length != 2) {
            errorOperands();
        }
        Stage stage = Repository.getStage();
        if (stage.isEmpty()) {
            System.out.println("Nochanges added to the commit.");
            return;
        }
        String message = args[1];
        if (message.isEmpty()) {
            System.out.println("Please entera commit message.");
            return;
        }
        Commit curCommit = Repository.getCurrentCommit();
        Head head = Repository.getHead();
        Branch branch = branch = Repository.getBranch(head.getBranch());
        Commit commit = new Commit(message, head.getCommit(), null, new Date());
        // 先遍历当前 commit，不在 stage 中的文件一律加入新 commit
        for (Map.Entry<String, String> entry : curCommit.getBlobs().entrySet()) {
            if (!stage.hasStage(entry.getKey()) && !stage.hasRemove(entry.getKey())) {
                commit.addBlob(entry.getKey(), entry.getValue());
            }
        }
        // 然后将 stage 的暂存文件放入新 commit
        commit.getBlobs().putAll(stage.getAddFile());
        // 接下来写入 commit 到 .gitlet ，然后设置好 head 和 branch 并写回，最后清空暂存区
        String commitSha1 = Utils.sha1(Utils.serialize(commit));
        Repository.writeCommit(commit, commitSha1);
        head.setCommit(commitSha1);
        branch.setCommit(commitSha1);
        Repository.writeHead(head);
        Repository.writeBranch(branch);
        Repository.clearStage();
    }

    public static void rm(String[] args) {
        if (args.length != 2) {
            errorOperands();
        }
        String filename = args[1];
        Stage stage = Repository.getStage();
        // 如果被暂存了，则直接取消暂存
        if (stage.hasStage(filename)) {
            stage.removeStage(filename);
            Repository.writeStage(stage);
            return;
        }
        Commit curCommit = Repository.getCurrentCommit();
        //如果在当前 commit 中，则加入到 remove 中，如果该文件在工作目录中存在，则删除该文件
        if (curCommit.hasFile(filename)) {
            stage.removeFile(filename);
            Repository.writeStage(stage);
            Repository.deleteFile(filename);
            return;
        }
        System.out.println("No reason to remove the file.");
    }

    public static void log(String[] args) {
        if (args.length > 1) {
            errorOperands();
        }
        Commit commit = Repository.getCurrentCommit();
        while (true) {
            // 打印 ===
            System.out.println("===");
            // 打印 commit sha1
            System.out.print("commit ");
            System.out.println(Utils.sha1(Utils.serialize(commit)));
            // 打印 merge
            if (commit.getFirstParent() != null && commit.getSecondParent() != null) {
                String first = toHex(commit.getFirstParent()).substring(0, 7);
                String second = toHex(commit.getSecondParent()).substring(0, 7);
                System.out.println("Merge: " + first + " " + second);
            }
            // 打印 date
            System.out.println(String.format(Locale.US,
                    "Date: %1$ta %1$tb %1$td %1$tT %1$tY %1$tz",
                    commit.getDate()));
            // 打印 message 和最后的空行
            System.out.println(commit.getMessage() + "\n");
            if (commit.getFirstParent() == null) {
                break;
            }
            commit = Repository.getCommit(commit.getFirstParent());
        }
    }

    public static void globalLog(String[] args) {
        if (args.length > 1) {
            errorOperands();
        }
        List<String> nameList = Utils.plainFilenamesIn(Repository.COMMITS_DIR);
        for (String filename : nameList) {
            Commit commit = Repository.getCommit(filename);
            System.out.println("===");
            System.out.print("commit ");
            System.out.println(Utils.sha1(Utils.serialize(commit)));
            if (commit.getFirstParent() != null && commit.getSecondParent() != null) {
                String first = toHex(commit.getFirstParent()).substring(0, 7);
                String second = toHex(commit.getSecondParent()).substring(0, 7);
                System.out.println("Merge: " + first + " " + second);
            }
            System.out.println(String.format(Locale.US,
                    "Date: %1$ta %1$tb %1$td %1$tT %1$tY %1$tz",
                    commit.getDate()));
            System.out.println(commit.getMessage() + "\n");
        }
    }

    public static void find(String[] args) {
        if (args.length != 2) {
            errorOperands();
        }
        String message = args[1];
        List<String> nameList = Utils.plainFilenamesIn(Repository.COMMITS_DIR);
        boolean hasCommit = false;
        for (String filename : nameList) {
            Commit commit = Repository.getCommit(filename);
            if (commit.getMessage().contains(message)) {
                System.out.println(Utils.sha1(Utils.serialize(commit)));
                hasCommit = true;
            }
        }
        if (!hasCommit) {
            System.out.println("Found no commit with that message.");
        }
    }

    private static void errorOperands() {
        System.out.println("Incorrect operands.");
        System.exit(0);
    }

    /** sha1 转 16 进制字符串 */
    private static String toHex(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

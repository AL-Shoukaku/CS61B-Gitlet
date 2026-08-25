package gitlet;

import java.io.File;
import java.util.Date;

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
        String branchSHA1 = Utils.sha1((Object) Utils.serialize(branch));
        Repository.writeBranch(branch, branchSHA1);
        Repository.writeCommit(commit, commitSHA1);
        // 设置好 head 指针
        Head head = new Head();
        head.setBranch(branchSHA1);
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
    }

    public static void commit(String[] args) {
        if (args.length != 2) {
            errorOperands();
        }
    }

    private static void errorOperands() {
        System.out.println("Incorrect operands.");
        System.exit(0);
    }


}

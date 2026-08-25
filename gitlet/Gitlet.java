package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static gitlet.Utils.join;
import static gitlet.Utils.serialize;

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
    }

    private static void errorOperands() {
        System.out.println("Incorrect operands.");
        System.exit(0);
    }
}

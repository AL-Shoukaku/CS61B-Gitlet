package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.Utils.join;

public class Gitlet {
    private Repository rep;

    public Gitlet() {

    }

    public static void init(String[] args) {
        if (args.length > 1) {
            errorOperands();
        }
        if (Repository.GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in "
                    + "thecurrent directory.");
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
        }
        if (stage.hasRemove(name)) {
            stage.deleteRemove(name);
        }
        // 如果文件在当前提交中，并且内容一致，则不写入blob,只更新暂存区
        if (Repository.getCurrentCommit().hasFile(name)
                && Repository.blobEqualCurrentCommit(name, blob)) {
            Repository.writeStage(stage);
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
        doCommit(args[1], null);
    }

    public static void doCommit(String message, String secondParent) {
        Stage stage = Repository.getStage();
        if (stage.isEmpty()) {
            System.out.println("Nochanges added to the commit.");
            return;
        }
        if (message.isEmpty()) {
            System.out.println("Please entera commit message.");
            return;
        }
        Commit curCommit = Repository.getCurrentCommit();
        Head head = Repository.getHead();
        Branch branch = Repository.getBranch(head.getBranch());
        Commit commit = new Commit(message, head.getCommit(), secondParent, new Date());
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
                String first = commit.getFirstParent().substring(0, 7);
                String second = commit.getSecondParent().substring(0, 7);
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
                String first = commit.getFirstParent().substring(0, 7);
                String second = commit.getSecondParent().substring(0, 7);
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

    public static void status(String[] args) {
        if (args.length != 1) {
            errorOperands();
        }
        // 准备所需要的对象
        Head head = Repository.getHead();
        Stage stage = Repository.getStage();
        Commit curCommit = Repository.getCurrentCommit();
        List<String> branchList = Utils.plainFilenamesIn(Repository.BRANCHES_DIR);
        branchList.sort(Comparator.naturalOrder());
        TreeMap<String, String> stageMap = new TreeMap<>(stage.getAddFile());   //treemap自动排序
        List<String> removeList = (new ArrayList<>(stage.getRemoveFile()));
        removeList.sort(Comparator.naturalOrder());
        List<String> workList = Utils.plainFilenamesIn(Repository.CWD);
        workList.sort(Comparator.naturalOrder());
        List<String> untrackList = new ArrayList<>();
        System.out.println("=== Branches ===");
        for (String branch : branchList) {
            if (branch.equals(head.getBranch())) {
                System.out.print("*");
            }
            System.out.println(branch);
        }
        System.out.println("\n=== Staged Files ===");
        for (String name : stageMap.keySet()) {
            System.out.println(name);
        }
        System.out.println("\n=== Removed Files ===");
        for (String name : removeList) {
            System.out.println(name);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        TreeMap<String, String> notStage = new TreeMap<>(); // 记录对应的文件与原因
        for (String filename : workList) {
            File file = join(Repository.CWD, filename);
            if (file.isDirectory()) {
                continue;
            }
            // 已经暂存,未暂存但在当前commit中,未跟踪
            if (stage.hasStage(filename)) {
                File stageFile = join(Repository.BLOBS_DIR, stageMap.get(filename));
                if (!Arrays.equals(Utils.readContents(file),
                        Utils.readObject(stageFile, Blob.class).getFileContents())) {
                    notStage.put(filename, " (modified)");
                }
            } else if (curCommit.hasFile(filename)) {
                if (!Arrays.equals(Utils.readContents(file),
                        curCommit.getBlob(filename).getFileContents())) {
                    notStage.put(filename, " (modified)");
                }
            } else {
                untrackList.add(filename);
            }
        }
        for (String filename : stage.getAddFile().keySet()) {
            if (!workList.contains(filename)) {
                notStage.put(filename, " (deleted)");
            }
        }
        for (String filename : curCommit.getBlobs().keySet()) {
            if (!workList.contains(filename) && !stage.hasStage(filename)) {
                notStage.put(filename, " (deleted)");
            }
        }
        for (Map.Entry<String, String> entry : notStage.entrySet()) {
            System.out.println(entry.getKey() + entry.getValue());
        }
        System.out.println("\n=== Untracked Files ===");
        for (String filename : untrackList) {
            System.out.println(filename);
        }
        System.out.println();
    }

    public static void checkout(String[] args) {
        if (args.length == 3 && args[1].equals("--")) {
            checkoutFile(args[2]);
        } else if (args.length == 4 && args[2].equals("--")) {
            checkoutFileCommit(args[3], args[1]);
        } else if (args.length == 2) {
            checkoutBranch(args[1]);
        } else {
            errorOperands();
        }
    }

    private static void checkoutFile(String filename) {
        Commit curCommit = Repository.getCurrentCommit();
        if (!curCommit.hasFile(filename)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        Repository.writeFile(filename, (Object) curCommit.getBlob(filename).getFileContents());
    }

    private static void checkoutFileCommit(String filename, String commitId) {
        Commit commit = Repository.getCommit(commitId);
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        if (!commit.hasFile(filename)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        Repository.writeFile(filename, (Object) commit.getBlob(filename).getFileContents());
    }

    private static void checkoutBranch(String branchName) {
        List<String> branchList = Utils.plainFilenamesIn(Repository.BRANCHES_DIR);
        if (!branchList.contains(branchName)) {
            System.out.println("No such branch exists.");
            return;
        }
        if (branchName.equals(Repository.getHead().getBranch())) {
            System.out.println("No need to checkout the current branch.");
        }
        Commit newCommit = Repository.getCommit(Repository.getBranch(branchName).getCommit());
        Commit curCommit = Repository.getCurrentCommit();
        List<String> workList = Utils.plainFilenamesIn(Repository.CWD);
        for (String name : newCommit.getBlobs().keySet()) {
            if (!curCommit.hasFile(name) && workList.contains(name)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }
        // 清空暂存区并删掉当前 commit 的所有文件, 然后写入新 commit 的所有文件
        Repository.clearStage();
        for (String name : curCommit.getBlobs().keySet()) {
            Repository.deleteFile(name);
        }
        for (Map.Entry<String, String> entry : newCommit.getBlobs().entrySet()) {
            Repository.writeFile(entry.getKey(),
                    Repository.getBlob(entry.getValue()).getFileContents());
        }
        Head head = Repository.getHead();
        head.setCommit(Utils.sha1(Utils.serialize(newCommit)));
        head.setBranch(branchName);
        Repository.writeHead(head);
    }

    public static void branch(String[] args) {
        if (args.length != 2) {
            errorOperands();
        }
        String branchName = args[1];
        List<String> allBranch = Utils.plainFilenamesIn(Repository.BRANCHES_DIR);
        if (allBranch.contains(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        Branch branch = new Branch(branchName);
        branch.setCommit(Repository.getHead().getCommit());
        Repository.writeBranch(branch);
    }

    public static void rmbranch(String[] args) {
        if (args.length != 2) {
            errorOperands();
        }
        String branchName = args[1];
        Branch branch = Repository.getBranch(branchName);
        if (branch == null) {
            System.out.println("A branch with that name does notexist.");
            return;
        }
        if (branchName.equals(Repository.getHead().getBranch())) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        Repository.deleteBranch(branchName);
    }

    public static void reset(String[] args) {
        if (args.length != 2) {
            errorOperands();
        }
        String commitId = args[1];
        Commit newCommit = Repository.getCommit(commitId);
        Commit curCommit = Repository.getCurrentCommit();
        Head head = Repository.getHead();
        Branch branch = Repository.getBranch(head.getBranch());
        if (newCommit == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        List<String> workList = Utils.plainFilenamesIn(Repository.CWD);
        for (String name : newCommit.getBlobs().keySet()) {
            if (!curCommit.hasFile(name) && workList.contains(name)) {
                System.out.println("There is an untracked file in the way; delete it, "
                        + "or add and commit it first.");
                return;
            }
        }
        for (String name : curCommit.getBlobs().keySet()) {
            Repository.deleteFile(name);
        }
        for (String name : newCommit.getBlobs().keySet()) {
            checkoutFileCommit(name, commitId);
        }
        Repository.clearStage();
        head.setCommit(commitId);
        branch.setCommit(commitId);
        Repository.writeHead(head);
        Repository.writeBranch(branch);
    }

    public static void merge(String[] args) {
        if (args.length != 2) {
            errorOperands();
        }
        if (!Repository.getStage().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        Head head = Repository.getHead();
        Branch obranch = Repository.getBranch(args[1]);
        Branch cbranch = Repository.getBranch(head.getBranch());
        if (obranch == null) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (obranch.getName().equals(cbranch.getName())) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        Commit curCommit = Repository.getCurrentCommit();
        Commit othCommit = Repository.getCommit(obranch.getCommit());
        Commit splitCommit = findSplitPoint(curCommit, othCommit);
        // 给定分支是分裂点，什么也不做
        if (commitEquals(othCommit, splitCommit)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }
        // 当前分支是分裂点，则移动到给定分支，复用 checkout 并设置好 head 和 cbranch
        if (commitEquals(curCommit, splitCommit)) {
            checkoutBranch(obranch.getName());
            cbranch.setCommit(obranch.getCommit());
            head.setBranch(cbranch.getName());
            Repository.writeHead(head);
            Repository.writeBranch(cbranch);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        List<String> conflictList = new ArrayList<>();
        Stage stage = Repository.getStage();
        // 遍历 split 的所有文件
        for (String filename : splitCommit.getBlobs().keySet()) {
            dealSplitHas(curCommit, othCommit, splitCommit, filename, stage, conflictList);
        }
        // 遍历 given branch 的所有文件
        for (String filename : othCommit.getBlobs().keySet()) {
            if (!splitCommit.hasFile(filename)) {
                dealGivenBranch(curCommit, othCommit, filename, stage, conflictList);
            }
        }
        // 检验是否与当前工作目录的 untrack 发生冲突
        checkWorkInMerge(curCommit, stage, conflictList);
        // 处理冲突文件并 stage
        dealConflict(curCommit, othCommit, stage, conflictList);
        // 进行新提交，复用 commit 代码，先写回stage
        Repository.writeStage(stage);
        // commit 不会写或删除文件， 需要手动操作
        for (Map.Entry<String, String> entry : stage.getAddFile().entrySet()) {
            Blob blob = Repository.getBlob(entry.getValue());
            Repository.writeFile(entry.getKey(), blob.getFileContents());
        }
        for (String filename : stage.getRemoveFile()) {
            Repository.deleteFile(filename);
        }
        String message = "Merged " + obranch.getName() + " into " + cbranch.getName() + ".";
        if (!conflictList.isEmpty()) {
            message += "Encountered a merge conflict.";
        }
        doCommit(message, obranch.getCommit());
    }

    private static void errorOperands() {
        System.out.println("Incorrect operands.");
        System.exit(0);
    }

    /** 找到两个 commit 的分裂点，确保两个 commit 不是同一个 */
    private static Commit findSplitPoint(Commit current, Commit other) {
        HashMap<String, Integer> curParent = new HashMap<>();
        HashMap<String, Integer> othParent = new HashMap<>();
        dfsCommit(current, curParent, 0);
        dfsCommit(other, othParent, 0);
        Commit min = null;
        int minDis = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> entry : curParent.entrySet()) {
            if (othParent.containsKey(entry.getKey()) && entry.getValue() < minDis) {
                min = Repository.getCommit(entry.getKey());
                minDis = entry.getValue();
            }
        }
        return min;
    }

    /** 采用 DFS 遍历一个 commit 并记录其父节点及其距离,对于起始节点需要自行处理 */
    private static void dfsCommit(Commit commit, HashMap<String, Integer> parent, int dis) {
        String sha1 = Utils.sha1(Utils.serialize(commit));
        if (!parent.containsKey(sha1) || (parent.containsKey(sha1) && parent.get(sha1) > dis)) {
            parent.put(sha1, dis);
        }
        if (commit.getFirstParent() != null && !parent.containsKey(commit.getFirstParent())) {
            dfsCommit(Repository.getCommit(commit.getFirstParent()), parent, dis + 1);
        }
        if (commit.getSecondParent() != null && !parent.containsKey(commit.getSecondParent())) {
            dfsCommit(Repository.getCommit(commit.getSecondParent()), parent, dis + 1);
        }
    }

    /** 给定一个 commit 计算它在树种的深度 */
    private static int countDepth(Commit commit) {
        if (commit == null) {
            return 0;
        }
        int depth = 1;
        while (commit.getFirstParent() != null) {
            commit = Repository.getCommit(commit.getFirstParent());
            depth++;
        }
        return depth;
    }

    /** 利用 sha1 来判断两个 commit 是同一个 commit */
    private static boolean commitEquals(Commit a, Commit b) {
        return Utils.sha1(Utils.serialize(a)).equals(Utils.sha1(Utils.serialize(b)));
    }

    /** 分类处理 split 种含有的文件 */
    private static void dealSplitHas(Commit cur, Commit oth, Commit split,
                                     String filename, Stage stage, List<String> conflict) {
        Blob curBlob = cur.getBlob(filename);
        Blob othBlob = oth.getBlob(filename);
        Blob splitBlob = split.getBlob(filename);

        // 两者都有则进一步判断，一方有且改动则表示冲突，否则不作为
        if (cur.hasFile(filename) && oth.hasFile(filename)) {
            // 若文件内容一样则不用变动，不一样则进入下一步判断
            if (!Arrays.equals(curBlob.getFileContents(), othBlob.getFileContents())) {
                // 若 cur 与 split相同，则 stage other，若 other 与 split 相同，则不作为，否则是冲突
                if (Arrays.equals(curBlob.getFileContents(), splitBlob.getFileContents())) {
                    stage.addFile(filename, oth.getBlobSha1(filename));
                } else if (!Arrays.equals(othBlob.getFileContents(), splitBlob.getFileContents())) {
                    conflict.add(filename);
                }
            }
        } else if (oth.hasFile(filename)
                && !Arrays.equals(othBlob.getFileContents(), splitBlob.getFileContents())) {
            conflict.add(filename);
        } else if (cur.hasFile(filename)) {
            if (!Arrays.equals(curBlob.getFileContents(), splitBlob.getFileContents())) {
                conflict.add(filename);
            } else {
                stage.removeFile(filename);
            }
        }
    }

    /** 处理 split 中没有但给定 branch 里有的文件 */
    private static void dealGivenBranch(Commit cur, Commit oth, String filename,
                                        Stage stage, List<String> conflict) {
        if (!cur.hasFile(filename)) {
            stage.addFile(filename, oth.getBlobSha1(filename));
        } else {
            Blob curBlob = cur.getBlob(filename);
            Blob othBlob = oth.getBlob(filename);
            if (!Arrays.equals(curBlob.getFileContents(), othBlob.getFileContents())) {
                conflict.add(filename);
            }
        }
    }

    /** 检查 merge 操作是否与当前工作目录中的 untrack 文件冲突 */
    private static void checkWorkInMerge(Commit current, Stage stage, List<String> conflict) {
        List<String> workList = Utils.plainFilenamesIn(Repository.CWD);
        for (String name : workList) {
            if (!current.hasFile(name) && (conflict.contains(name)
                    || stage.hasStage(name) || stage.hasRemove(name))) {
                // 有冲突直接退出
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                System.exit(0);
            }
        }
    }

    /** 处理 merge 中的冲突文件 */
    private static void dealConflict(Commit cur, Commit oth, Stage stage, List<String> conflict) {
        for (String filename : conflict) {
            StringBuilder sb = new StringBuilder();
            sb.append("<<<<<<< HEAD\n");
            if (cur.hasFile(filename)) {
                sb.append(new String(cur.getBlob(filename).getFileContents()));
            }
            sb.append("=======\n");
            if (oth.hasFile(filename)) {
                sb.append(new String(oth.getBlob(filename).getFileContents()));
            }
            sb.append(">>>>>>>");
            Blob blob = new Blob(sb.toString().getBytes());
            String blobSha1 = Utils.sha1(Utils.serialize(blob));
            Repository.writeBlob(blob, blobSha1);
            stage.addFile(filename, blobSha1);
        }
    }
}

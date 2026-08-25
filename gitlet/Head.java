package gitlet;

import java.io.Serializable;

public class Head implements Serializable {
    private String commit;
    private String branch;

    public Head() {
        this.commit = null;
        this.branch = null;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getBranch() {
        return branch;
    }

    public String getCommit() {
        return commit;
    }
}

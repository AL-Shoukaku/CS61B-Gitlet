package gitlet;

import java.io.Serializable;

public class Branch implements Serializable {
    private final String name;
    private String commit;

    public Branch(String name) {
        this.name = name;
        this.commit = null;
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public String getName() {
        return name;
    }
}

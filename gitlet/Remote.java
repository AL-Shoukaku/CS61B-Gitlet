package gitlet;

import java.io.Serializable;

public class Remote implements Serializable {
    private String name;
    private String path;

    public Remote(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }
}

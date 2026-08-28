package gitlet;

import java.io.Serializable;

public class Remote implements Serializable {
    private final String name;
    private final String path;

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

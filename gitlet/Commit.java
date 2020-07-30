import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Commit implements Serializable {
    String message;

    Date time;
    String parent;
    Map<String, String> blobs;
    String parent1 = null;
    String parent2 = null;
    int length;

    public Commit(String message, Date time, String parent, Map<String, String> blobs){
        this.message = message;
        this.time = time;
        this.parent = parent;
        this.blobs = blobs;
        if (parent == null) {
            length = 0;
        }
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int length() {
        return this.length;
    }

    public Commit previousKParent(int k) {
        Commit commit = this;
        for (int i = 0; i < k; i++) {
            commit = GitTree.getCommit(commit.parent);
        }
        return commit;
    }

    public String getSHA() {
        return Utils.sha1(Utils.serialize(this));
    }


    public boolean equals(Commit commit) {
        return this.getSHA().equals(commit.getSHA());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public String getParent() {
        return parent;
    }

    public String getParent1() {
        return parent1;
    }

    public String getParent2() {
        return parent2;
    }

    public void setParent1(String parent1) {
        this.parent1 = parent1;
    }

    public void setParent2(String parent2) {
        this.parent2 = parent2;
    }

    public Map<String, String> getBlobs() {
        return this.blobs;
    }

    public Date getTime() {
        return time;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("=====\n");
        buffer.append("Commit: " + Utils.sha1(Utils.serialize(this)) + "\n");
        buffer.append("Date: " + time + "\n");
        buffer.append(message + "\n");
        return buffer.toString();
    }

}

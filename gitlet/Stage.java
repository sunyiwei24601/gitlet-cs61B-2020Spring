import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Stage implements Serializable {

    private HashMap<String, byte[]> stagedFiles;
    private ArrayList<String> markedFiles;
    private Commit head;


    public Stage() {
        stagedFiles = new HashMap<>();
        markedFiles = new ArrayList<>();
    }

    // !! 缺少对commit前后明明没有改变的情况的处理
    public Commit commit(String message) {
        HashMap<String, String> blobs = new HashMap<>();
        Map<String, String> headBlobs = head.getBlobs();
        for (String fileName : headBlobs.keySet()) {
            blobs.put(fileName, headBlobs.get(fileName));
        }

        for (String fileName : stagedFiles.keySet()) {
            String sha1 = Utils.sha1(stagedFiles.get(fileName));
            blobs.put(fileName, sha1);
            File file = new File(GitTree.BLOB_PATH + sha1);
            Utils.writeContents(file, stagedFiles.get(fileName));
        }

        for (String key : markedFiles) {
            blobs.remove(key);
        }
        String parentSHA = Utils.sha1(Utils.serialize(head));
        Commit newCommit = new Commit(message, new Date(), parentSHA, blobs);

        clear();
        newCommit.setLength(head.length + 1);
        head = newCommit;
        return newCommit;
    }

    public void add(String fileName) {
        File file = new File(fileName);
        byte[] bytes = Utils.readContents(file);
        String sha1 = Utils.sha1(bytes);


        String previousSHA1 = head.getBlobs().get(fileName);
        if (previousSHA1 == null) {
            stagedFiles.put(fileName, bytes);
        } else {//如果更改后仍与当前节点相同，则无需更改
            if (sha1.equals(previousSHA1)) {
                stagedFiles.remove(fileName);
            } else {
                stagedFiles.put(fileName, bytes);
            }
        }
        markedFiles.remove(fileName);
    }

    public void setHead(Commit head) {
        this.head = head;
    }

    public ArrayList<String> getMarkedFiles() {
        return markedFiles;
    }

    public HashMap<String, byte[]> getStagedFiles() {
        return stagedFiles;
    }

    public void clear() {
        stagedFiles.clear();
        markedFiles.clear();
    }

    public void removeFile(String fileName) {
        if (!markedFiles.contains(fileName)) {
            markedFiles.add(fileName);
        }
        stagedFiles.remove(fileName);
    }

    public Commit mergeCommit(String message, String parent1, String parent2){
        Commit mergeCommit = commit(message);
        mergeCommit.setParent1(parent1);
        mergeCommit.setParent2(parent2);


        return mergeCommit;
    }
}

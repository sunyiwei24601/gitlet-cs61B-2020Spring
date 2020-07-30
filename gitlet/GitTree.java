
import java.io.File;
import java.io.Serializable;
import java.util.*;

public class GitTree implements Serializable {
    static final String GIT_PATH = "./.gitlet/";
    static final String COMMIT_PATH = "./.gitlet/commits/";
    static final String STAGE_PATH = "./.gitlet/stages/";
    static final String BLOB_PATH = "./.gitlet/blobs/";
    static final String GIT_TREE_PATH = "./.gitlet/gitTree";
    static final String CURRENT_PATH = "./";
    HashMap<String, String> branchMap;
    ArrayList<String> commitMap;
    Stage curStage;
    String _head;
    String _branch;

    public GitTree() {
        branchMap = new HashMap<>();
        commitMap = new ArrayList<>();
    }

    public Commit init() {
        File root = new File(GIT_PATH);
        if (root.exists()) {
            throw new RuntimeException(".gitlet directory has existed.");
        }

        File commit = new File(COMMIT_PATH);
        File stage = new File(STAGE_PATH);
        File blob = new File(BLOB_PATH);
        commit.mkdirs();
        stage.mkdirs();
        blob.mkdirs();


        Commit initCommit = new Commit("init message", new Date(0), null, new HashMap<>());
        String commitId = Utils.sha1(Utils.serialize(initCommit));
        saveCommit(initCommit, commitId);

        _head = commitId;
        curStage = new Stage();
        curStage.setHead(initCommit);
        commitMap.add(commitId);
        branchMap.put("master", commitId);
        _branch = "master";

        return initCommit;
    }

    void saveCommit(Commit commit, String commitId) {
        File f = new File(COMMIT_PATH + commitId);
        Utils.writeObject(f, commit);
    }

    static Commit getCommit(String commitId) {
        File commit = new File(COMMIT_PATH + commitId);
        return Utils.readObject(commit, Commit.class);
    }

    public void commit(String message) {
        Commit commit = curStage.commit(message);
        _head = Utils.sha1(Utils.serialize(commit));
        commitMap.add(_head);
        saveCommit(commit, _head);
        branchMap.put(_branch, _head);
    }

    public Commit getHead() {
        return getCommit(_head);
    }

    public void add(String fileName) {
        curStage.add(fileName);
    }

    public void checkout(String fileName, String commitId) {
        String findCommit = findCommit(commitId);
        if (findCommit == null) {
            throw new RuntimeException("Can not find commit id");
        }
        Commit commit = getCommit(findCommit);
        String blobId = commit.getBlobs().get(fileName);
        if (blobId == null) {
            System.out.println("File does not exist in that commit");
            return;
        }

        File blobFile = new File(BLOB_PATH + blobId);
        byte[] oldContent = Utils.readContents(blobFile);
        File curFile = new File(fileName);
        Utils.writeContents(curFile, oldContent);
    }

    public void checkout(String fileName) {
        checkout(fileName, _head);
    }

    public void checkoutBranch(String branchName) {
        String branchCommitId = branchMap.get(branchName);
        if (branchCommitId == null) {
            System.out.println("Branch Name not exists.");
        } else if (branchName.equals(_branch)) {
            System.out.println("No need to checkout to current Branch.");
        }

        Commit branchCommit = GitTree.getCommit(branchCommitId);
        Map<String, String> givenBlobs = branchCommit.getBlobs();
        Map<String, String> currentBlobs = getHead().getBlobs();
        List<String> workingDirectoryFiles = Utils.filesInDirectory(new File(CURRENT_PATH));

        for (String fileName : givenBlobs.keySet()) {
            String blobId = givenBlobs.get(fileName);
            File blobFile = new File(BLOB_PATH + blobId);
            byte[] contents = Utils.readContents(blobFile);
            File targetFile = new File(CURRENT_PATH + fileName);
            Utils.writeContents(targetFile, contents);
        }

        for (String fileName : workingDirectoryFiles) {
            if (!givenBlobs.containsKey(fileName) && currentBlobs.containsKey(fileName)) {
                Utils.restrictedDelete(new File(CURRENT_PATH + fileName));
            }
        }

        curStage.clear();
        curStage.setHead(branchCommit);
        _head = branchCommitId;
        _branch = branchName;

    }

    //根据指定前缀寻找commit
    public String findCommit(String prefix) {
        for (String commit : commitMap) {
            if (commit.substring(0, prefix.length()).equals(prefix)) {
                return commit;
            }
        }
        return null;
    }

    //创建一个branch
    public void createBranch(String branchName) {
        if (branchMap.containsKey(branchName)) {
            System.out.println("Branch has existed.");
        } else {
            branchMap.put(branchName, _head);
        }
    }

    //删除一个branch
    public void rmBranch(String branchName) {
        if (!branchMap.containsKey(branchName)) {
            System.out.print(String.format("branch %s not found", branchName));
        } else if (_branch.equals(branchName)) {
            System.out.println("Can not remove current Branch");
        } else {
            branchMap.remove(branchName);
        }
    }

    //删除指定文件，如果在当前commit中则删除该文件，并在stage中标记该文件已被删除，否则就不动
    public void rmFile(String fileName) {
        List<String> fileNames = Utils.filesInDirectory(new File(CURRENT_PATH));
        Commit commit = getHead();
        if (commit.getBlobs().containsKey(fileName)) {
            if (fileNames.contains(fileName)) {
                Utils.restrictedDelete(new File(fileName));
            }
            curStage.removeFile(fileName);
        } else {
            System.out.println("No reason to remove the file");
        }

    }

    public void printStatus() {
        //print branches
        statusTitle("Branches");
        for (String branchName : branchMap.keySet()) {
            if (branchName.equals(_branch)) {
                System.out.print("*");
            }
            System.out.println(branchName);
        }

        //print Staged Files
        statusTitle("Staged Files");
        for (String fileName : curStage.getStagedFiles().keySet()) {
            System.out.println(fileName);
        }
        System.out.println();

        //print Removed Files
        statusTitle("Removed Files");
        for (String fileName : curStage.getMarkedFiles()) {
            System.out.println(fileName);
        }
        System.out.println();

        //print modified Files
        statusTitle("Modified Files");
        Map<String, String> modifiedFiles = findModifiedFiles();
        for (String fileName : modifiedFiles.keySet()) {
            System.out.println(fileName + "(" + modifiedFiles.get(fileName) + ")");
        }
        System.out.println();

        // print Untracked Files
        statusTitle("Untracked Files");
        for (String fileName : findUntrackedFiles()) {
            System.out.println(fileName);
        }
        System.out.println();
    }

    public void statusTitle(String str) {
        System.out.println(String.format("=== %s ===", str));
    }

    public void printLog() {
        Commit commit = getHead();
        while (commit != null) {
            System.out.println(commit.toString());
            if (commit.parent != null) {
                commit = getCommit(commit.parent);
            } else {
                commit = null;
            }
        }
    }

    public void merge(String givenBranch) {

        if (!checkMergeError(givenBranch)) {
            return;
        }

        Commit headCommit = getHead();
        if (branchMap.get(givenBranch) == null) {
            System.out.println("given branch not existed");
            return;
        }
        Commit givenCommit = getCommit(branchMap.get(givenBranch));
        Commit splitCommit = getFirstSplitPoint(headCommit, givenCommit);

        Map<String, String> givenBM = givenCommit.getBlobs();
        Map<String, String> splitBM = splitCommit.getBlobs();
        Map<String, String> headBM = headCommit.getBlobs();

        Set<String> unionSet = new HashSet<>();
        unionSet.addAll(givenBM.keySet());
        unionSet.addAll(splitBM.keySet());
        unionSet.addAll(headBM.keySet());

        Map<String, String> mergeBlobMap = new HashMap<>();

        for (String fileName : unionSet) {
            String givenBlob = givenBM.getOrDefault(fileName, "");
            String splitBlob = splitBM.getOrDefault(fileName, "");
            String headBlob = headBM.getOrDefault(fileName, "");

            if (givenBlob.equals(splitBlob) && headBlob.equals(givenBlob))
            //nothing change, so ignore
            {
                mergeBlobMap.put(fileName, headBlob);
            }
            else if (givenBlob.equals(headBlob) && !givenBlob.equals(splitBlob))
            //only head change, so ignore
            {
                mergeBlobMap.put(fileName, headBlob);
            }
            else if (!givenBlob.equals(splitBlob) && splitBlob.equals(headBlob))
            //accept given blobs, change the file to given blob
            {
                mergeBlobMap.put(fileName, givenBlob);
                File givenFile = new File(BLOB_PATH + givenBlob);
                File curFile = new File(CURRENT_PATH + fileName);
                //file exists rewrite it, if not remove it
                if (givenFile.exists() && givenBlob.length() > 0){
                    String contents = Utils.readContentsAsString(givenFile);
                    Utils.writeContents(curFile, contents);
                    curStage.add(fileName);
                } else {
                    Utils.restrictedDelete(curFile);
                    curStage.removeFile(fileName);
                }
            }
            // there is a conflict! add two blobs together and write to file
            else if (!givenBlob.equals(headBlob) && !givenBlob.equals(splitBlob) && !headBlob.equals(splitBlob))
            {

                File givenFile = new File(BLOB_PATH + givenBlob);
                File headFile = new File(BLOB_PATH + headBlob);
                String givenContent = "";
                String headContent = "";
                if (givenFile.exists() && givenBlob.length() > 0) {
                    givenContent = Utils.readContentsAsString(givenFile);
                }
                if (headFile.exists() && headBlob.length() > 0) {
                    headContent = Utils.readContentsAsString(headFile);
                }

                File curFile = new File(CURRENT_PATH + fileName);
                Utils.writeContents(curFile, "<<<<<<<< HEAD\n" + headContent +
                        "===========\n" + givenContent + ">>>>>>>>");
                curStage.add(fileName);
            }
        }

        String message = String.format("Merged %s into %s", givenBranch, _branch);
        Commit mergeCommit = curStage.mergeCommit(message, _head, branchMap.get(givenBranch));

        String sha1 = Utils.sha1(Utils.serialize(mergeCommit));
        commitMap.add(sha1);
        File mergeCommitFile = new File(COMMIT_PATH + sha1);
        Utils.writeObject(mergeCommitFile, mergeCommit);

        _head = sha1;
        branchMap.put(_branch, sha1);

    }

    public Commit getFirstSplitPoint(Commit head, Commit given) {
        int headLength = head.length();
        int givenLength = given.length();
        if (headLength > givenLength) {
            head = head.previousKParent(headLength - givenLength);
        } else {
            given = given.previousKParent(givenLength - headLength);
        }

        while (!given.equals(head)) {
            given = given.previousKParent(1);
            head = head.previousKParent(1);
        }

        return given;
    }

    public boolean checkMergeError(String branchName) {
        //stage 不为空的情况
        if (!curStage.getMarkedFiles().isEmpty() || !curStage.getStagedFiles().isEmpty()) {
            System.out.println("There are still files in staged area.");
            return false;
        }

        if (branchMap.get(branchName) == null) {
           System.out.println("branch " + branchName + " not exist.");
           return false;
        }

        if (_branch.equals(branchName)) {
            System.out.println("Can not merge the same branch.");
            return false;
        }

        List<String> untrackedFiles = findUntrackedFiles();
        if (untrackedFiles.size() != 0) {
            System.out.println("Still have untracked files!");
            for (String fileName : untrackedFiles) {
                System.out.print(fileName + "  ");
            }
            System.out.println();
            return false;
        }

        Map<String, String> modifiedFiles = findModifiedFiles();
        if (modifiedFiles.size() != 0) {
            System.out.println("Still have modified files in current directory!");
            for (String fileName : modifiedFiles.keySet()) {
                System.out.print(fileName + "(" + modifiedFiles.get(fileName) + ")    ");
            }
            System.out.println();
            return false;
        }

        return true;
    }

    List<String> findUntrackedFiles() {
        Commit commit = getHead();
        List<String> fileNames = new ArrayList<>();
        Map<String, String> blobs = commit.getBlobs();
        Set<String> stagedFiles = curStage.getStagedFiles().keySet();
        for (String fileName : Utils.filesInDirectory(new File(CURRENT_PATH))) {
            if (!blobs.containsKey(fileName) && !stagedFiles.contains(fileName)) {
                fileNames.add(fileName);
            }
        }
        return fileNames;
    }

    Map<String, String> findModifiedFiles() {
        Commit commit = getHead();
        Map<String, String> blobs = commit.getBlobs();
        Map<String, String>  fileNames = new HashMap<>();
        List<String> filesInDirectory = Utils.filesInDirectory(new File(CURRENT_PATH));
        for (String fileName : filesInDirectory) {
            String sha = Utils.sha1(Utils.readContents(new File(CURRENT_PATH + fileName)));
            if (blobs.containsKey(fileName) && !blobs.get(fileName).equals(sha)) {
                fileNames.put(fileName, "modified");
            }
        }

        for (String fileName : blobs.keySet()) {
            if (!filesInDirectory.contains(fileName) && !curStage.getMarkedFiles().contains(fileName)){
                fileNames.put(fileName, "deleted");
            }
        }

        return fileNames;
    }

    public void reset(String givenCommitId) {
        String commitId = findCommit(givenCommitId);
        if (!checkResetError(givenCommitId)) {
            return;
        }

        Commit givenCommit = getCommit(commitId);
        Commit headCommit = getCommit(commitId);
        Map<String, String> givenBlobs = givenCommit.getBlobs();
        Map<String, String> headBlobs = headCommit.getBlobs();

        for (String fileName : givenBlobs.keySet()) {
            String givenBlob = givenBlobs.get(fileName);
            //存在了文件上的不同，进行切换
            if (!givenBlob.equals(headBlobs.get(fileName))) {
                byte[] givenContents = Utils.readContents(new File(BLOB_PATH + givenBlob));
                Utils.writeContents(new File(CURRENT_PATH + fileName), givenContents);
            }
        }

        for (String fileName : headBlobs.keySet()) {
            if (!givenBlobs.containsKey(fileName)) {
                File file = new File(CURRENT_PATH + fileName);
                Utils.restrictedDelete(file);
            }
        }

        branchMap.put(_branch, commitId);
        curStage.clear();
        curStage.setHead(givenCommit);
        _head = commitId;

    }

    public boolean checkResetError(String commitId) {
        //stage 不为空的情况
        if (!curStage.getMarkedFiles().isEmpty() || !curStage.getStagedFiles().isEmpty()) {
            System.out.println("There are still files in staged area.");
            return false;
        }

        if (findCommit(commitId) == null) {
            System.out.println("commit " + commitId + " not exist.");
            return false;
        }


        List<String> untrackedFiles = findUntrackedFiles();
        if (untrackedFiles.size() != 0) {
            System.out.println("Still have untracked files! Delete it or commit it first.");
            for (String fileName : untrackedFiles) {
                System.out.print(fileName + "  ");
            }
            System.out.println();
            return false;
        }

        Map<String, String> modifiedFiles = findModifiedFiles();
        if (modifiedFiles.size() != 0) {
            System.out.println("Still have modified files in current directory!");
            for (String fileName : modifiedFiles.keySet()) {
                System.out.print(fileName + "(" + modifiedFiles.get(fileName) + ")    ");
            }
            System.out.println();
            return false;
        }

        return true;
    }
}

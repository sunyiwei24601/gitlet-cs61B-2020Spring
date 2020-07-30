import org.junit.Test;

import java.io.File;

public class Main {
    public static void main(String[] args) {
    }


    public  void testInit() {
        runCommand("init");
    }

    public static void clearRepository() {
        Utils.deleteDirectory(new File(GitTree.GIT_PATH));
    }

    static void init() {
        GitTree gt = new GitTree();
        gt.init();
        saveGitTree(gt);
    }

    static void saveGitTree(GitTree gt) {
        File gtFile = new File(GitTree.GIT_TREE_PATH);
        Utils.writeObject(gtFile, gt);
    }

    //添加error情况
    static void addCommand(GitTree gt, String... fileNames) {
        int index = 0;
        for (String filename : fileNames) {
            if (index == 0) {
                index++;
                continue;
            }
            gt.add(filename);
        }
    }

    //添加error情况
    static void commitCommand(GitTree gt, String... args) {
        if (args.length == 1) {
            System.out.println("Please input commit message");
        } else if (args.length > 2) {
            System.out.println("too may parameters");
        } else {
            gt.commit(args[1]);
        }
    }

    static void checkoutCommand(GitTree gt, String... args) {
        if (args.length == 1) {
            throw new RuntimeException("Parameters not enough.");
        } else if (args.length == 2) {
            gt.checkoutBranch(args[1]);
        } else if (args.length == 3) {
            gt.checkout(args[2]);
        } else if (args.length == 4) {
            gt.checkout(args[3], args[1]);
        } else {
            System.out.println("incorrent checkout input");
        }
    }

    static void branchCommand(GitTree gt, String... args) {
        if (args.length > 2) {
            System.out.println("Too many parameters for branch.");
        }
        gt.createBranch(args[1]);
    }

    static void rmBranchCommand(GitTree gt, String... args) {
        if (args.length > 2) {
            System.out.println("too many parameters in this command");
        } else if (args.length < 2) {
            System.out.println("please input the branch to be removed");
        } else {
            String branchName = args[1];
            gt.rmBranch(branchName);
        }
    }

    static void rmCommand(GitTree gt, String... args) {
        if (args.length != 2) {
            System.out.println("rm parameters incorrent");
        } else {
            gt.rmFile(args[1]);
        }
    }

    static void statusCommand(GitTree gt, String... args) {
        if (args.length >= 2) {
            System.out.println("Too many arguments");
        } else {
            gt.printStatus();
        }
    }

    static void logCommand(GitTree gt, String... args) {
        if (args.length != 1) {
            System.out.println("Incorrent arguments input");
        } else {
            gt.printLog();
        }
    }

    static void mergeCommand(GitTree gt, String... args) {
        if (args.length > 2) {
            System.out.println("incorrect arguments");
        } else if (args.length == 1) {
            System.out.println("Please input the branch name to be merged");
        } else {
            gt.merge(args[1]);
        }
    }

    static void resetCommand(GitTree gt, String... args) {
        if (args.length > 2) {
            System.out.println("too many arguments");
        } else if (args.length == 1) {
            System.out.println("Please input the commit id.");
        } else {
            gt.reset(args[1]);
        }
    }

    static void runCommand(String... args) {
        String command = args[0];
        GitTree gt;

        if (command.equals("init")) {
            init();
        } else {
            File gtFile = new File(GitTree.GIT_TREE_PATH);
            if (!gtFile.exists()) {
                System.out.println("Not in an initialized"
                        + " Gitlet directory.");
                return;
            }
            gt = Utils.readObject(gtFile, GitTree.class);
            switch (command) {
                case "add":
                    addCommand(gt, args); break;
                case "commit":
                    commitCommand(gt, args); break;
                case "checkout":
                    checkoutCommand(gt, args); break;
                case "branch":
                    branchCommand(gt, args); break;
                case "rmbranch":
                    rmBranchCommand(gt, args); break;
                case "rm":
                    rmCommand(gt, args); break;
                case "status":
                    statusCommand(gt, args); break;
                case "log":
                    logCommand(gt, args); break;
                case "merge":
                    mergeCommand(gt, args); break;
                case "reset":
                    resetCommand(gt,args); break;
                default:
                    System.out.println("Unknown command");
            }
            saveGitTree(gt);

        }
    }
}

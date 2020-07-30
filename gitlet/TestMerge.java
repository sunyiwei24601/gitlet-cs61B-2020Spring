import org.junit.Test;

import java.io.File;

public class TestMerge {

    public static Main main = new Main();
    static String fn2 = "test2.txt";
    static String fn1 = "test1.txt";

    static File f1 = new File(fn1);
    static File f2 = new File(fn2);

    @Test
    //case 1 both modified and no conflicts
    public void testCase1() {
        main.clearRepository();
        main.testInit();

        Utils.writeContents(f1, "123");
        Utils.writeContents(f2, "123");

        run("add", fn1);
        run("add", fn2);
        run("commit", "first commit");

        run("branch", "other");
        Utils.writeContents(f1, "123456");
        run("add", fn1);
        run("commit", "test1 123456");

        run("checkout", "other");
        Utils.writeContents(f2, "123456");
        run("add", fn2);
        run("commit", "test2 123456");

        run("merge", "master");
        run("log");
        run("status");


    }

    @Test
    //cas2 2 both modified the same files ==> confliots
    public void testCase2() {
        main.clearRepository();
        main.testInit();

        Utils.writeContents(f1, "123\n");
        Utils.writeContents(f2, "123\n");

        run("add", fn1);
        run("add", fn2);
        run("commit", "first commit");

        run("branch", "other");
        Utils.writeContents(f1, "123456\n");
        run("add", fn1);
        run("commit", "test1 123456");

        run("checkout", "other");
        Utils.writeContents(f1, "123456789\n");
        run("add", fn1);
        run("commit", "test1 123456789");

        run("merge", "master");

    }

    @Test
    //case3 one unchanged, the other delete the file,
    public void testCase3() {
        main.clearRepository();
        main.testInit();

        Utils.writeContents(f1, "123\n");
        Utils.writeContents(f2, "123\n");

        run("add", fn1);
        run("add", fn2);
        run("commit", "first commit");

        run("branch", "other");
        run("checkout", "other");
        run("rm", fn2);
        run("commit", "test2 deleted");


        run("checkout", "master");
        run("merge", "other");

    }

    @Test
    //case4 currentHead deleted, the other branch changed ==> conflicts
    public void testCase4() {
        main.clearRepository();
        main.testInit();

        Utils.writeContents(f1, "123\n");
        Utils.writeContents(f2, "123\n");

        run("add", fn1);
        run("add", fn2);
        run("commit", "first commit");

        run("branch", "other");
        run("rm", fn2);
        run("commit", "test2 deleted");


        run("checkout", "other");
        Utils.writeContents(f2, "123456\n");
        run("add", fn2);
        run("commit", "test2 123456");

        run("checkout", "master");
        run("merge", "other");
    }

    @Test
    //case5 uncommitted Staged remainded
    public void testCase5() {
        main.clearRepository();
        main.testInit();

        Utils.writeContents(f2, "123\n");
        Utils.writeContents(f1, "123\n");

        run("add", fn2, fn1);
        run("commit", "first commit");
        run("branch", "other");


        run("checkout", "other");
        Utils.writeContents(f2, "123456\n");
        run("add", fn2);
        run("commit", "test2 123456");


//        run("checkout", "master");
//        Utils.writeContents(f2, "123456789\n");
//        run("add", fn2);
        f1.delete();
        Utils.writeContents(f2, "123456789\n");
        run("merge", "master");
        run("status");



    }

    @Test
    public void testResetEnviron() {
        main.clearRepository();
        main.testInit();

        Utils.writeContents(f1, "123\n");
        run("add", fn1);
        run("commit", "first commit");

        Utils.writeContents(f1, "123456\n");
        run("add", fn1);
        run("commit", "456 commit");

        Utils.writeContents(f1, "123456789\n");
        run("add", fn1);
        run("commit", "789 commit");

        run("log");

    }

    @Test
    public void testReset() {
        Utils.writeContents(f1, "12345678910\n");
        run("add", fn1);


        run("reset", "72da55");
        run("log");
    }
    static void run(String... args) {
        Main.runCommand(args);
    }
}

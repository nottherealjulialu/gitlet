package gitlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
/* Driver class for Gitlet, the tiny stupid version-control system.
   @author
*/
public class Main {

    public static void main(String... args) {
        Gitletdirectory gitdir = new Gitletdirectory();
        String wkdir = System.getProperty("user/dir");
        File inFile = new File(wkdir, "serialized_repo");
        if (args == null) {
            System.out.println("Please enter a command.");
        } else if (args[0].equals("init")) {
            if (args.length == 1) {
                gitdir.init();
            } else {
                System.out.println("Incorrect operands.");
            }
        } else {
            File dir = new File(wkdir, ".gitlet");
            if (dir.exists()) {
                try {
                    ObjectInputStream inp = new ObjectInputStream(new FileInputStream(inFile));
                    gitdir = (Gitletdirectory) inp.readObject();
                    inp.close();
                } catch (IOException | ClassNotFoundException excp) {
                    gitdir = null;
                }
                if (args.length == 2 && !args[0].equals("checkout")) {
                    if (args[0].equals("add")) {
                        gitdir.stage(args[1]);
                    } else if (args[0].equals("commit")) {
                        gitdir.newcommit(args[1]);
                    } else if (args[0].equals("rm")) {
                        gitdir.removefile(args[1]);
                    } else if (args[0].equals("find")) {
                        gitdir.find(args[1]);
                    } else if (args[0].equals("branch")) {
                        gitdir.branch(args[1]);
                    } else if (args[0].equals("rm-branch")) {
                        gitdir.removebranch(args[1]);
                    } else if (args[0].equals("reset")) {
                        gitdir.reset(args[1]);
                    } else if (args[0].equals("merge")) {
                        gitdir.merge(args[1]);
                    } else {
                        System.out.println("No command with that name exists.");
                    }
                } else if (args.length == 1) {
                    if (args[0].equals("log")) {
                        gitdir.log();
                    } else if (args[0].equals("global-log")) {
                        gitdir.globallog();
                    } else if (args[0].equals("status")) {
                        gitdir.status();
                    } else {
                        System.out.println("No command with that name exists.");
                    }
                } else if (args[0].equals("checkout")) {
                    if (args[1].equals("--") && args.length == 3) {
                        gitdir.checkoutfile(args[2]);
                    } else if (args.length == 4 && args[2].equals("--")) {
                        gitdir.checkoutfileincommit(args[1], args[3]);
                    } else if (args.length == 2) {
                        gitdir.checkoutbranch(args[1]);
                    } else {
                        System.out.println("Incorrect operands.");
                    }
                } else {
                    System.out.println("Incorrect operands.");
                }
            } else {
                System.out.println("Not in an initialized gitlet directory.");
            }
        }
        File outfile = new File(wkdir, "serialized_repo");
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outfile));
            out.writeObject(gitdir);
            out.close();
        } catch (IOException e) {
            System.out.println("exception main");
        }
    }
}

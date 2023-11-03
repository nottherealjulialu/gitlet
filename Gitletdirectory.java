package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import static gitlet.Utils.restrictedDelete;
import static gitlet.Utils.readContents;

import static gitlet.Utils.writeContents;
import java.io.Serializable;
import static gitlet.Utils.sha1;
import static gitlet.Utils.plainFilenamesIn;

public class Gitletdirectory implements Serializable {

    File wkdir;
    File gitlet;
    File stagingarea;
    File removedfolder;
    Commit currcommit;
    String currbranchname;
    ArrayList<String> branchheads = new ArrayList<>();
    ArrayList<Commit> history = new ArrayList<>();


    public void init() {
        wkdir = new File(System.getProperty("user.dir"));
        gitlet = new File(wkdir, ".gitlet");
        if (!gitlet.exists()) {
            gitlet.mkdir();
        } else {
            String e = "A gitlet version-control system already exists in the current directory.";
            System.out.println(e);
        }


        stagingarea = new File(gitlet, "staging_area");
        stagingarea.mkdir();
        removedfolder = new File(gitlet, "removed_files");
        removedfolder.mkdir();

        currbranchname = "master";
        currcommit = new Commit("initial commit", currbranchname);
        File commitfolder = new File(gitlet, currcommit.getSha1());
        commitfolder.mkdir();
        history.add(currcommit);
        String firstbranch = "master " + currcommit.getSha1();
        branchheads.add(firstbranch);

    }

    public void stage(String name) {
        File f = new File(wkdir, name);
        if (!f.exists()) {
            System.out.println("File does not exist.");
        } else {
            Path fpath = f.toPath();
            File stage = new File(stagingarea, name);
            Path stagepath = stage.toPath();
            File folder = new File(gitlet, currcommit.getSha1());
            File currcommitversion = new File(folder, name);
            File remove = new File(removedfolder, name);
            if (currcommitversion.exists()) {
                if (remove.exists()) {
                    remove.delete();
                }
                if (currcommit.untracked.contains(currcommitversion)) {
                    currcommit.untracked.remove(currcommitversion);
                }
                if (!sha1(readContents(f)).equals(sha1(readContents(currcommitversion)))) {
                    try {
                        Files.copy(fpath, stagepath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        System.out.println("exception 63");
                    }
                }
            } else {
                try {
                    Files.copy(fpath, stagepath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    System.out.println("exception 72");
                }
            }
        }

    }

    public void newcommit(String m) {
        if (m == null || m.equals("")) {
            System.out.println("Please enter a commit message.");
        } else {
            ArrayList<File> blobs = new ArrayList<>();
            boolean changes = false;
            File folder = new File(gitlet, currcommit.getSha1());
            for (File f : folder.listFiles()) {
                blobs.add(f);
            }
            for (File f : folder.listFiles()) {
                File staged = new File(stagingarea, f.getName());
                if (staged.exists()) {
                    changes = true;
                    blobs.remove(f);
                    blobs.add(staged);
                }
            }
            for (File f : stagingarea.listFiles()) {
                if (!blobs.contains(f)) {
                    changes = true;
                    blobs.add(f);
                }
            }
            for (File f : currcommit.untracked) {
                if (blobs.contains(f)) {
                    changes = true;
                    blobs.remove(f);
                }
            }
            currcommit.untracked.clear();
            for (File file : removedfolder.listFiles()) {
                file.delete();
            }
            if (!changes) {
                System.out.println("No changes added to the commit.");
            } else {
                Commit child = new Commit(m, blobs, currcommit, currbranchname);
                history.add(child);
                String newbranchhead = currbranchname + " " + child.getSha1();
                for (String b : branchheads) {
                    String bname = b.split(" ")[0];
                    if (bname.equals(currbranchname)) {
                        branchheads.remove(b);
                        break;
                    }
                }
                branchheads.add(newbranchhead);
                currcommit = child;
                File f = new File(gitlet, child.getSha1());
                f.mkdir();
                for (File file : blobs) {
                    File newfile = new File(f, file.getName());
                    Path fpath = newfile.toPath();
                    try {
                        Files.copy(file.toPath(), fpath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException E) {
                        E.printStackTrace();
                    }
                }
                for (File file : stagingarea.listFiles()) {
                    file.delete();
                }
                for (File file : removedfolder.listFiles()) {
                    file.delete();
                }
            }
        }
    }

    public void removefile(String name) {
        File currcommitfolder = new File(gitlet, currcommit.getSha1());
        File remove = new File(currcommitfolder, name);
        File stage = new File(stagingarea, name);
        File r = new File(removedfolder, name);
        File wkdirremove = new File(wkdir, name);
        if (remove.exists()) {
            restrictedDelete(wkdirremove);
            if (stage.exists()) {
                stage.delete();
            }
            try {
                Files.copy(remove.toPath(), r.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            currcommit.untracked.add(remove);
        } else {
            if (stage.exists()) {
                stage.delete();
            } else {
                System.out.println("No reason to remove the file.");
            }
        }

    }

    public void displaycommit(Commit c) {
        System.out.println("=== ");
        System.out.println("Commit " + c.getSha1());
        System.out.println(c.getDatecreated());
        System.out.println(c.getMsg());
        System.out.println();
    }

    public void log() {
        Commit currdisplay = currcommit;
        while (currdisplay != null) {
            displaycommit(currdisplay);
            currdisplay = currdisplay.getParent();
        }
    }

    public void globallog() {
        for (Commit c : history) {
            displaycommit(c);
        }
    }

    public void find(String m) {
        int count = 0;
        for (Commit c : history) {
            if (c.getMsg().equals(m)) {
                System.out.println(c.getSha1());
                count++;
            }
        }
        if (count == 0) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void status() {
        System.out.println("=== Branches ===");
        String[] branchnames = new String[branchheads.size()];
        for (int i = 0; i < branchheads.size(); i++) {
            branchnames[i] = branchheads.get(i).split(" ")[0];
        }
        for (int i = 0; i < branchnames.length - 1; i++) {
            for (int j = i + 1; j < branchnames.length; j++) {
                if (branchnames[i].compareTo(branchnames[j]) > 0) {
                    String t = branchnames[i];
                    branchnames[i] = branchnames[j];
                    branchnames[j] = t;
                }
            }
        }
        for (String n : branchnames) {
            if (n.equals(currcommit.getBranchname())) {
                System.out.print("*");
            }
            System.out.println(n);
        }
        System.out.println("");
        System.out.println("=== Staged Files ===");
        List<String> stagednames = plainFilenamesIn(stagingarea);
        for (String n : stagednames) {
            System.out.println(n);
        }
        System.out.println("");
        System.out.println("=== Removed Files ===");
        List<String> removednames = plainFilenamesIn(removedfolder);
        for (String n : removednames) {
            System.out.println(n);
        }
        System.out.println("");
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println("");
        System.out.println("=== Untracked Files ===");
    }

    public void checkoutfile(String name) {
        File folder = new File(gitlet, currcommit.getSha1());
        for (File f : folder.listFiles()) {
            if (f.getName().equals(name)) {
                File old = new File(wkdir, name);
                try {
                    Files.copy(f.toPath(), old.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    System.out.println("exception 228");
                }
                return;
            }
        }
        System.out.println("File does not exist in that commit.");
    }

    public void checkoutfileincommit(String sha1, String name) {
        if (sha1.length() < 40) {
            for (Commit c : history) {
                if (c.getSha1().substring(0, sha1.length()).equals(sha1)) {
                    sha1 = c.getSha1();
                    break;
                }
            }
        }
        File commitfolder = new File(gitlet, sha1);
        if (!commitfolder.exists()) {
            System.out.println("No commit with that id exists.");
        } else {
            File f = new File(commitfolder, name);
            if (!f.exists()) {
                System.out.println("File does not exist in that commit.");
            } else {
                File old = new File(wkdir, name);
                try {
                    Files.copy(f.toPath(), old.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void checkoutbranch(String name) {
        File currfolder = new File(gitlet, currcommit.getSha1());
        String givensha1 = null;
        Commit given = null;
        File givenfolder = null;
        for (String b : branchheads) {
            String bname = b.split(" ")[0];
            if (bname.equals(name)) {
                givensha1 = b.split(" ")[1];
            }
        }
        if (givensha1 != null) {
            givenfolder = new File(gitlet, givensha1);
        }
        for (Commit c : history) {
            if (c.getSha1().equals(givensha1)) {
                given = c;
                break;
            }
        }
        for (File f : wkdir.listFiles()) {
            boolean tracked = false;
            boolean tobeoverwritten = false;
            if (f.isFile()) {
                File comfile = new File(currfolder, f.getName());
                File givenfile = null;
                if (givenfolder != null) {
                    givenfile = new File(givenfolder, f.getName());
                }
                if (comfile.isFile() && sha1(readContents(comfile)).equals(sha1(readContents(f)))) {
                    tracked = true;
                }
                if (givenfile != null && givenfile.isFile()) {
                    tobeoverwritten = true;
                }
                if (!tracked && tobeoverwritten) {
                    String e = "There is an untracked file in the way; delete it or add it first.";
                    System.out.println(e);
                    return;
                }
            }
        }
        if (name.equals(currcommit.getBranchname())) {
            System.out.println("No need to checkout the current branch.");
        } else {
            if (given == null) {
                System.out.println("No such branch exists.");
            } else {
                for (File f : givenfolder.listFiles()) {
                    File w = new File(wkdir, f.getName());
                    try {
                        Files.copy(f.toPath(), w.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                for (File f : wkdir.listFiles()) {
                    if (f.isFile()) {
                        File comfile = new File(givenfolder, f.getName());
                        File currfile = new File(currfolder, f.getName());
                        if (!comfile.isFile() && currfile.isFile()) {
                            restrictedDelete(f);
                        }
                    }
                }
                currbranchname = name;
                currcommit = given;
                currcommit.changebranchname(currbranchname);
            }
        }
    }

    public void branch(String name) {
        for (String b : branchheads) {
            String bname = b.split(" ")[0];
            if (bname.equals(name)) {
                System.out.println("A branch with that name already exists.");
                return;
            }
        }
        String newbranch = name + " " + currcommit.getSha1();
        branchheads.add(newbranch);
    }

    public void removebranch(String name) {
        if (currbranchname.equals(name)) {
            System.out.println("Cannot remove the current branch.");
        } else {
            for (String b : branchheads) {
                String bname = b.split(" ")[0];
                if (bname.equals(name)) {
                    branchheads.remove(b);
                    return;
                }
            }
            System.out.println("A branch with that name does not exist.");

        }
    }

    public void reset(String sha1) {
        File givenfolder = new File(gitlet, sha1);
        File currfolder = new File(gitlet, currcommit.getSha1());
        Commit given = null;
        for (Commit c : history) {
            if (c.getSha1().equals(sha1)) {
                given = c;
            } else if (c.getSha1().substring(0, sha1.length()).equals(sha1)) {
                given = c;
            }
        }
        if (given != null) {
            for (File f : wkdir.listFiles()) {
                if (f.isFile()) {
                    boolean tracked = false;
                    boolean tobeoverwritten = false;
                    File commitfile = new File(currfolder, f.getName());
                    File givenfile = new File(givenfolder, f.getName());
                    if (commitfile.isFile()) {
                        if (sha1(readContents(f)).equals(sha1(readContents(commitfile)))) {
                            tracked = true;
                        }
                    }
                    if (givenfile.isFile()) {
                        if (!sha1(readContents(givenfile)).equals(sha1(readContents(f)))) {
                            tobeoverwritten = true;
                        }
                    }
                    if (!tracked && tobeoverwritten) {
                        String e = "There is an untracked file in the way;";
                        e += " delete it or add it first.";
                        System.out.println(e);
                        return;
                    }
                }
            }
            for (File f : givenfolder.listFiles()) {
                if (f.isFile()) {
                    File w = new File(wkdir, f.getName());
                    try {
                        Files.copy(f.toPath(), w.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            for (File f : currfolder.listFiles()) {
                if (f.isFile()) {
                    File givenfile = new File(givenfolder, f.getName());
                    File wkfile = new File(wkdir, f.getName());
                    if (!givenfile.isFile() && wkfile.isFile()) {
                        restrictedDelete(wkfile);
                    }
                }
            }
            for (File f : stagingarea.listFiles()) {
                f.delete();
            }
            currcommit = given;
            for (int i = 0; i < branchheads.size(); i++) {
                String bname = branchheads.get(i).split(" ")[0];
                if (bname.equals(currbranchname)) {
                    branchheads.remove(i);
                    String newbhead = bname + " " + given.getSha1();
                    branchheads.add(newbhead);
                    break;
                }
            }
            given.changebranchname(currbranchname);
        } else {
            System.out.println("No commit with that id exists.");
        }

    }

    public void conflict(File curr, File given) {
        String name = "";
        String head = "<<<<<<< HEAD\n";
        String separator = "=======\n";
        String end = ">>>>>>>\n";
        byte[] currcont = new byte[0];
        byte[]  givcont = new byte[0];
        if (curr != null && curr.isFile()) {
            name = curr.getName();
            currcont = readContents(curr);
        }
        if (given != null && given.isFile()) {
            name = given.getName();
            givcont = readContents(given);
        }
        File dest = new File(wkdir, name);
        if (dest.isFile()) {
            dest.delete();
        }
        String currcontent = new String(currcont, StandardCharsets.UTF_8);
        String givcontent = new String(givcont, StandardCharsets.UTF_8);
        String combine = head + currcontent + separator + givcontent + end;
        byte[] content = combine.getBytes();
        writeContents(dest, content);
    }

    public void mergecheckuntracked(Commit split, Commit given) {
        File currfolder = new File(gitlet, currcommit.getSha1());
        File givenfolder = null;
        if (given != null) {
            givenfolder = new File(gitlet, given.getSha1());
        }
        File splitfolder = null;
        if (split != null) {
            splitfolder = new File(gitlet, split.getSha1());
        }
        for (File f : wkdir.listFiles()) {
            boolean tracked = false;
            boolean tobeoverwritten = false;
            boolean tobedeleted = false;
            if (f.isFile()) {
                File comf = new File(currfolder, f.getName());
                File givf = null;
                File splitfile = null;
                if (givenfolder != null) {
                    givf = new File(givenfolder, f.getName());
                }
                if (splitfolder != null) {
                    splitfile = new File(splitfolder, f.getName());
                }
                if (comf.isFile()) {
                    if (sha1(readContents(comf)).equals(sha1(readContents(f)))) {
                        tracked = true;
                    }
                }
                if (givenfolder != null) {
                    if (givf.isFile() && comf.isFile()) {
                        if (sha1(readContents(givf)).equals(sha1(readContents(comf)))) {
                            tobeoverwritten = true;
                        }
                    } else if (givf.isFile() || comf.isFile()) {
                        tobeoverwritten = true;
                    }
                }

                if (splitfile != null && splitfile.isFile()) {
                    if (comf.isFile()) {
                        if (sha1(readContents(splitfile)).equals(sha1(readContents(comf)))) {
                            if (givf != null && !givf.isFile()) {
                                tobedeleted = true;
                            }
                        }
                    }
                }
                if (splitfile != null && splitfile.isFile()) {
                    if (givf != null && givf.isFile()) {
                        if (sha1(readContents(splitfile)).equals(sha1(readContents(givf)))) {
                            if (!comf.isFile()) {
                                tobedeleted = true;
                            }
                        }
                    }
                }
                if (!tracked && (tobeoverwritten || tobedeleted)) {
                    String e = "There is an untracked file in the way; delete it or add it first.";
                    System.out.println(e);
                    System.exit(0);
                }
            }
        }
    }

    public void merge(String name) {
        String givensha1 = null;
        Commit given = null;
        Commit split = null;
        for (String b : branchheads) {
            String bname = b.split(" ")[0];
            if (bname.equals(name)) {
                givensha1 = b.split(" ")[1];
            }
        }
        for (Commit c : history) {
            if (c.getSha1().equals(givensha1)) {
                given = c;
                break;
            }
        }
        Commit t1 = currcommit;
        while (split == null && t1 != null) {
            Commit t2 = given;
            while (split == null && t2 != null) {
                if (t1 == t2) {
                    split = t1;
                }
                t2 = t2.getParent();
            }
            t1 = t1.getParent();
        }

        for (File f : stagingarea.listFiles()) {
            if (f.isFile()) {
                System.out.println("You have uncommitted changes.");
                return;
            }
        }
        if (removedfolder.listFiles().length > 0) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        if (stagingarea.listFiles().length > 0) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        for (File f : removedfolder.listFiles()) {
            if (f.exists()) {
                System.out.println("You have uncommitted changes.");
                return;
            }
        }
        mergecheckuntracked(split, given);
        if (given == null) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (given == currcommit) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        if (split == given) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }
        if (split == currcommit) {
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        mergehelper(given, split);
    }

    public void mergehelper(Commit given, Commit split) {
        int conflict = 0;
        File givenfolder = new File(gitlet, given.getSha1());
        File splitfolder = new File(gitlet, split.getSha1());
        File currfolder = new File(gitlet, currcommit.getSha1());
        for (File f : givenfolder.listFiles()) {
            if (f.isFile()) {
                String fname = f.getName();
                File splitf = new File(splitfolder, fname);
                File currf = new File(currfolder, fname);
                File wkf = new File(wkdir, fname);
                if (!splitf.isFile()) {
                    if (!currf.isFile()) {
                        checkoutfileincommit(given.getSha1(), fname);
                        stage(fname);
                    } else {
                        if (!sha1(readContents(f)).equals(sha1(readContents(currf)))) {
                            conflict++;
                            conflict(currf, f);
                        }
                    }
                } else {
                    if (!currf.isFile()) {
                        if (!sha1(readContents(f)).equals(sha1(readContents(splitf)))) {
                            conflict++;
                            conflict(currf, f);
                        } else {
                            if (wkf.isFile()) {
                                restrictedDelete(wkf);
                            }
                        }
                    } else {
                        if (!sha1(readContents(f)).equals(sha1(readContents(splitf)))) {
                            if (sha1(readContents(splitf)).equals(sha1(readContents(currf)))) {
                                checkoutfileincommit(given.getSha1(), fname);
                                stage(fname);
                            } else {
                                conflict++;
                                conflict(currf, f);
                            }
                        }
                    }
                }
            }
        }
        for (File f : currfolder.listFiles()) {
            if (f.isFile()) {
                String fname = f.getName();
                File splitf = new File(splitfolder, fname);
                File givf = new File(givenfolder, fname);
                if (splitf.isFile()) {

                    if (!givf.isFile()) {
                        if (sha1(readContents(f)).equals(sha1(readContents(splitf)))) {
                            removefile(fname);
                        } else {
                            conflict++;
                            conflict(f, givf);
                        }
                    }
                } else {
                    if (givf.isFile()) {
                        if (!sha1(readContents(givf)).equals(sha1(readContents(f)))) {
                            conflict++;
                            conflict(f, givf);
                        }
                    }
                }
            }
        }
        if (conflict == 0) {
            newcommit("Merged " + currbranchname + " with " + given.getBranchname() + ".");
        } else {
            System.out.println("Encountered a merge conflict.");
        }
    }
}

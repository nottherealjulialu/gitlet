package gitlet;

import java.io.File;
import java.io.IOException;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import static gitlet.Utils.sha1;

import java.io.Serializable;

public class Commit implements Serializable {

    ArrayList<File> blobs;
    ArrayList<File> untracked = new ArrayList<>();
    String msg;
    String datecreated;
    SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Commit parent;
    String sha1;
    String currbranchname;
    ArrayList<String> branchnames = new ArrayList<>();


    public Commit(String msg, String branchname) {
        this.msg = msg;
        datecreated = s.format(new Date());
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(stream);
            objectStream.writeObject(this);
            objectStream.close();
            sha1 = sha1(stream.toByteArray());
        } catch (IOException excp) {
            System.out.println("Internal error serializing commit.");
        }
        currbranchname = branchname;
        branchnames.add(currbranchname);

    }

    public Commit(String msg, ArrayList<File> blobs, Commit parent, String branchname) {
        this.msg = msg;
        this.parent = parent;
        datecreated = s.format(new Date());
        this.blobs = blobs;
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(stream);
            objectStream.writeObject(this);
            objectStream.close();
            sha1 = sha1(stream.toByteArray());
        } catch (IOException excp) {
            System.out.println("Internal error serializing commit.");
        }
        this.currbranchname = branchname;
        branchnames.add(branchname);
    }


    public String getSha1() {
        return sha1;
    }

    public String getDatecreated() {
        return datecreated;
    }

    public Commit getParent() {
        return parent;
    }

    public String getMsg() {
        return msg;
    }

    public String getBranchname() {
        return currbranchname;
    }

    public void changebranchname(String name) {
        currbranchname = name;
        if (name != null) {
            branchnames.add(name);
        }
    }
}

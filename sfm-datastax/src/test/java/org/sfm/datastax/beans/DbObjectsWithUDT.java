package org.sfm.datastax.beans;

import org.sfm.tuples.Tuple3;

public class DbObjectsWithUDT {
    private long id;
    private MyType t;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public MyType getT() {
        return t;
    }

    public void setT(MyType t) {
        this.t = t;
    }


    public static class MyType {
        public String str;
        public long l;
    }
}

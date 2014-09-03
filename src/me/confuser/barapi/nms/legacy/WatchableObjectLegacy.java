package me.confuser.barapi.nms.legacy;

public class WatchableObjectLegacy {

    private final int a;
    private final int b;
    private Object c;
    private boolean d;

    public WatchableObjectLegacy(int i, int j, Object object) {
        this.b = j;
        this.c = object;
        this.a = i;
        this.d = true;
    }

    public int a() {
        return this.b;
    }

    public void a(Object object) {
        this.c = object;
    }

    public Object b() {
        return this.c;
    }

    public int c() {
        return this.a;
    }

    public boolean d() {
        return this.d;
    }

    public void a(boolean flag) {
        this.d = flag;
    }

    static boolean a(WatchableObjectLegacy watchableobject, boolean flag) {
        return watchableobject.d = flag;
    }
}

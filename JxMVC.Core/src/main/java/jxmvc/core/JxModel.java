package jxmvc.core;

public abstract class JxModel {
    protected static JxDB db() {
        return new JxDB();
    }
}

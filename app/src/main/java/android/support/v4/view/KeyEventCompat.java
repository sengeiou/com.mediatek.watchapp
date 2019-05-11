package android.support.v4.view;

import android.os.Build.VERSION;
import android.view.KeyEvent;

public final class KeyEventCompat {
    static final KeyEventVersionImpl IMPL;

    interface KeyEventVersionImpl {
        boolean metaStateHasModifiers(int i, int i2);

        boolean metaStateHasNoModifiers(int i);
    }

    static class BaseKeyEventVersionImpl implements KeyEventVersionImpl {
        BaseKeyEventVersionImpl() {
        }

        private static int metaStateFilterDirectionalModifiers(int metaState, int modifiers, int basic, int left, int right) {
            boolean wantBasic;
            boolean wantLeftOrRight;
            if ((modifiers & basic) == 0) {
                wantBasic = false;
            } else {
                wantBasic = true;
            }
            int directional = left | right;
            if ((modifiers & directional) == 0) {
                wantLeftOrRight = false;
            } else {
                wantLeftOrRight = true;
            }
            if (wantBasic) {
                if (!wantLeftOrRight) {
                    return (directional ^ -1) & metaState;
                }
                throw new IllegalArgumentException("bad arguments");
            } else if (wantLeftOrRight) {
                return (basic ^ -1) & metaState;
            } else {
                return metaState;
            }
        }

        public int normalizeMetaState(int metaState) {
            if ((metaState & 192) != 0) {
                metaState |= 1;
            }
            if ((metaState & 48) != 0) {
                metaState |= 2;
            }
            return metaState & 247;
        }

        public boolean metaStateHasModifiers(int metaState, int modifiers) {
            if (metaStateFilterDirectionalModifiers(metaStateFilterDirectionalModifiers(normalizeMetaState(metaState) & 247, modifiers, 1, 64, 128), modifiers, 2, 16, 32) != modifiers) {
                return false;
            }
            return true;
        }

        public boolean metaStateHasNoModifiers(int metaState) {
            return (normalizeMetaState(metaState) & 247) == 0;
        }
    }

    static class HoneycombKeyEventVersionImpl extends BaseKeyEventVersionImpl {
        HoneycombKeyEventVersionImpl() {
        }

        public int normalizeMetaState(int metaState) {
            return KeyEventCompatHoneycomb.normalizeMetaState(metaState);
        }

        public boolean metaStateHasModifiers(int metaState, int modifiers) {
            return KeyEventCompatHoneycomb.metaStateHasModifiers(metaState, modifiers);
        }

        public boolean metaStateHasNoModifiers(int metaState) {
            return KeyEventCompatHoneycomb.metaStateHasNoModifiers(metaState);
        }
    }

    static {
        if (VERSION.SDK_INT < 11) {
            IMPL = new BaseKeyEventVersionImpl();
        } else {
            IMPL = new HoneycombKeyEventVersionImpl();
        }
    }

    public static boolean hasModifiers(KeyEvent event, int modifiers) {
        return IMPL.metaStateHasModifiers(event.getMetaState(), modifiers);
    }

    public static boolean hasNoModifiers(KeyEvent event) {
        return IMPL.metaStateHasNoModifiers(event.getMetaState());
    }

    private KeyEventCompat() {
    }
}

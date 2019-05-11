package android.support.v7.widget;

import android.support.v4.util.Pools$Pool;
import android.support.v4.util.Pools$SimplePool;
import android.support.v7.widget.RecyclerView.ViewHolder;
import java.util.ArrayList;
import java.util.List;

class AdapterHelper implements Callback {
    final Callback mCallback;
    final boolean mDisableRecycler;
    private int mExistingUpdateTypes;
    Runnable mOnItemProcessedCallback;
    final OpReorderer mOpReorderer;
    final ArrayList<UpdateOp> mPendingUpdates;
    final ArrayList<UpdateOp> mPostponedList;
    private Pools$Pool<UpdateOp> mUpdateOpPool;

    interface Callback {
        ViewHolder findViewHolder(int i);

        void markViewHoldersUpdated(int i, int i2, Object obj);

        void offsetPositionsForAdd(int i, int i2);

        void offsetPositionsForMove(int i, int i2);

        void offsetPositionsForRemovingInvisible(int i, int i2);

        void offsetPositionsForRemovingLaidOutOrNewView(int i, int i2);

        void onDispatchFirstPass(UpdateOp updateOp);

        void onDispatchSecondPass(UpdateOp updateOp);
    }

    static class UpdateOp {
        int cmd;
        int itemCount;
        Object payload;
        int positionStart;

        UpdateOp(int cmd, int positionStart, int itemCount, Object payload) {
            this.cmd = cmd;
            this.positionStart = positionStart;
            this.itemCount = itemCount;
            this.payload = payload;
        }

        String cmdToString() {
            switch (this.cmd) {
                case 1:
                    return "add";
                case 2:
                    return "rm";
                case 4:
                    return "up";
                case 8:
                    return "mv";
                default:
                    return "??";
            }
        }

        public String toString() {
            return Integer.toHexString(System.identityHashCode(this)) + "[" + cmdToString() + ",s:" + this.positionStart + "c:" + this.itemCount + ",p:" + this.payload + "]";
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UpdateOp op = (UpdateOp) o;
            if (this.cmd != op.cmd) {
                return false;
            }
            if (this.cmd == 8 && Math.abs(this.itemCount - this.positionStart) == 1 && this.itemCount == op.positionStart && this.positionStart == op.itemCount) {
                return true;
            }
            if (this.itemCount != op.itemCount || this.positionStart != op.positionStart) {
                return false;
            }
            if (this.payload == null) {
                return op.payload == null;
            } else {
                if (!this.payload.equals(op.payload)) {
                    return false;
                }
            }
        }

        public int hashCode() {
            return (((this.cmd * 31) + this.positionStart) * 31) + this.itemCount;
        }
    }

    AdapterHelper(Callback callback) {
        this(callback, false);
    }

    AdapterHelper(Callback callback, boolean disableRecycler) {
        this.mUpdateOpPool = new Pools$SimplePool(30);
        this.mPendingUpdates = new ArrayList();
        this.mPostponedList = new ArrayList();
        this.mExistingUpdateTypes = 0;
        this.mCallback = callback;
        this.mDisableRecycler = disableRecycler;
        this.mOpReorderer = new OpReorderer(this);
    }

    void reset() {
        recycleUpdateOpsAndClearList(this.mPendingUpdates);
        recycleUpdateOpsAndClearList(this.mPostponedList);
        this.mExistingUpdateTypes = 0;
    }

    void preProcess() {
        this.mOpReorderer.reorderOps(this.mPendingUpdates);
        int count = this.mPendingUpdates.size();
        for (int i = 0; i < count; i++) {
            UpdateOp op = (UpdateOp) this.mPendingUpdates.get(i);
            switch (op.cmd) {
                case 1:
                    applyAdd(op);
                    break;
                case 2:
                    applyRemove(op);
                    break;
                case 4:
                    applyUpdate(op);
                    break;
                case 8:
                    applyMove(op);
                    break;
            }
            if (this.mOnItemProcessedCallback != null) {
                this.mOnItemProcessedCallback.run();
            }
        }
        this.mPendingUpdates.clear();
    }

    void consumePostponedUpdates() {
        int count = this.mPostponedList.size();
        for (int i = 0; i < count; i++) {
            this.mCallback.onDispatchSecondPass((UpdateOp) this.mPostponedList.get(i));
        }
        recycleUpdateOpsAndClearList(this.mPostponedList);
        this.mExistingUpdateTypes = 0;
    }

    private void applyMove(UpdateOp op) {
        postponeAndUpdateViewHolders(op);
    }

    private void applyRemove(UpdateOp op) {
        int tmpStart = op.positionStart;
        int tmpCount = 0;
        int tmpEnd = op.positionStart + op.itemCount;
        int type = -1;
        int position = op.positionStart;
        while (position < tmpEnd) {
            boolean typeChanged = false;
            if (this.mCallback.findViewHolder(position) == null && !canFindInPreLayout(position)) {
                if (type == 1) {
                    postponeAndUpdateViewHolders(obtainUpdateOp(2, tmpStart, tmpCount, null));
                    typeChanged = true;
                }
                type = 0;
            } else {
                if (type == 0) {
                    dispatchAndUpdateViewHolders(obtainUpdateOp(2, tmpStart, tmpCount, null));
                    typeChanged = true;
                }
                type = 1;
            }
            if (typeChanged) {
                position -= tmpCount;
                tmpEnd -= tmpCount;
                tmpCount = 1;
            } else {
                tmpCount++;
            }
            position++;
        }
        if (tmpCount != op.itemCount) {
            recycleUpdateOp(op);
            op = obtainUpdateOp(2, tmpStart, tmpCount, null);
        }
        if (type != 0) {
            postponeAndUpdateViewHolders(op);
        } else {
            dispatchAndUpdateViewHolders(op);
        }
    }

    private void applyUpdate(UpdateOp op) {
        int tmpStart = op.positionStart;
        int tmpCount = 0;
        int tmpEnd = op.positionStart + op.itemCount;
        int type = -1;
        int position = op.positionStart;
        while (position < tmpEnd) {
            if (this.mCallback.findViewHolder(position) == null && !canFindInPreLayout(position)) {
                if (type == 1) {
                    postponeAndUpdateViewHolders(obtainUpdateOp(4, tmpStart, tmpCount, op.payload));
                    tmpCount = 0;
                    tmpStart = position;
                }
                type = 0;
            } else {
                if (type == 0) {
                    dispatchAndUpdateViewHolders(obtainUpdateOp(4, tmpStart, tmpCount, op.payload));
                    tmpCount = 0;
                    tmpStart = position;
                }
                type = 1;
            }
            tmpCount++;
            position++;
        }
        if (tmpCount != op.itemCount) {
            Object payload = op.payload;
            recycleUpdateOp(op);
            op = obtainUpdateOp(4, tmpStart, tmpCount, payload);
        }
        if (type != 0) {
            postponeAndUpdateViewHolders(op);
        } else {
            dispatchAndUpdateViewHolders(op);
        }
    }

    private void dispatchAndUpdateViewHolders(UpdateOp op) {
        if (op.cmd == 1 || op.cmd == 8) {
            throw new IllegalArgumentException("should not dispatch add or move for pre layout");
        }
        int positionMultiplier;
        int tmpStart = updatePositionWithPostponed(op.positionStart, op.cmd);
        int tmpCnt = 1;
        int offsetPositionForPartial = op.positionStart;
        switch (op.cmd) {
            case 2:
                positionMultiplier = 0;
                break;
            case 4:
                positionMultiplier = 1;
                break;
            default:
                throw new IllegalArgumentException("op should be remove or update." + op);
        }
        for (int p = 1; p < op.itemCount; p++) {
            int updatedPos = updatePositionWithPostponed(op.positionStart + (positionMultiplier * p), op.cmd);
            boolean continuous = false;
            switch (op.cmd) {
                case 2:
                    continuous = updatedPos == tmpStart;
                    break;
                case 4:
                    continuous = updatedPos == tmpStart + 1;
                    break;
            }
            if (continuous) {
                tmpCnt++;
            } else {
                UpdateOp tmp = obtainUpdateOp(op.cmd, tmpStart, tmpCnt, op.payload);
                dispatchFirstPassAndUpdateViewHolders(tmp, offsetPositionForPartial);
                recycleUpdateOp(tmp);
                if (op.cmd == 4) {
                    offsetPositionForPartial += tmpCnt;
                }
                tmpStart = updatedPos;
                tmpCnt = 1;
            }
        }
        Object payload = op.payload;
        recycleUpdateOp(op);
        if (tmpCnt > 0) {
            tmp = obtainUpdateOp(op.cmd, tmpStart, tmpCnt, payload);
            dispatchFirstPassAndUpdateViewHolders(tmp, offsetPositionForPartial);
            recycleUpdateOp(tmp);
        }
    }

    void dispatchFirstPassAndUpdateViewHolders(UpdateOp op, int offsetStart) {
        this.mCallback.onDispatchFirstPass(op);
        switch (op.cmd) {
            case 2:
                this.mCallback.offsetPositionsForRemovingInvisible(offsetStart, op.itemCount);
                return;
            case 4:
                this.mCallback.markViewHoldersUpdated(offsetStart, op.itemCount, op.payload);
                return;
            default:
                throw new IllegalArgumentException("only remove and update ops can be dispatched in first pass");
        }
    }

    private int updatePositionWithPostponed(int pos, int cmd) {
        int i;
        for (i = this.mPostponedList.size() - 1; i >= 0; i--) {
            UpdateOp postponed = (UpdateOp) this.mPostponedList.get(i);
            if (postponed.cmd == 8) {
                int start;
                int end;
                if (postponed.positionStart >= postponed.itemCount) {
                    start = postponed.itemCount;
                    end = postponed.positionStart;
                } else {
                    start = postponed.positionStart;
                    end = postponed.itemCount;
                }
                if (pos >= start && pos <= end) {
                    if (start != postponed.positionStart) {
                        if (cmd == 1) {
                            postponed.positionStart++;
                        } else if (cmd == 2) {
                            postponed.positionStart--;
                        }
                        pos--;
                    } else {
                        if (cmd == 1) {
                            postponed.itemCount++;
                        } else if (cmd == 2) {
                            postponed.itemCount--;
                        }
                        pos++;
                    }
                } else if (pos < postponed.positionStart) {
                    if (cmd == 1) {
                        postponed.positionStart++;
                        postponed.itemCount++;
                    } else if (cmd == 2) {
                        postponed.positionStart--;
                        postponed.itemCount--;
                    }
                }
            } else if (postponed.positionStart > pos) {
                if (cmd == 1) {
                    postponed.positionStart++;
                } else if (cmd == 2) {
                    postponed.positionStart--;
                }
            } else if (postponed.cmd == 1) {
                pos -= postponed.itemCount;
            } else if (postponed.cmd == 2) {
                pos += postponed.itemCount;
            }
        }
        for (i = this.mPostponedList.size() - 1; i >= 0; i--) {
            UpdateOp op = (UpdateOp) this.mPostponedList.get(i);
            if (op.cmd != 8) {
                if (op.itemCount <= 0) {
                    this.mPostponedList.remove(i);
                    recycleUpdateOp(op);
                }
            } else if (op.itemCount == op.positionStart || op.itemCount < 0) {
                this.mPostponedList.remove(i);
                recycleUpdateOp(op);
            }
        }
        return pos;
    }

    private boolean canFindInPreLayout(int position) {
        int count = this.mPostponedList.size();
        for (int i = 0; i < count; i++) {
            UpdateOp op = (UpdateOp) this.mPostponedList.get(i);
            if (op.cmd != 8) {
                if (op.cmd != 1) {
                    continue;
                } else {
                    int end = op.positionStart + op.itemCount;
                    for (int pos = op.positionStart; pos < end; pos++) {
                        if (findPositionOffset(pos, i + 1) == position) {
                            return true;
                        }
                    }
                    continue;
                }
            } else if (findPositionOffset(op.itemCount, i + 1) == position) {
                return true;
            }
        }
        return false;
    }

    private void applyAdd(UpdateOp op) {
        postponeAndUpdateViewHolders(op);
    }

    private void postponeAndUpdateViewHolders(UpdateOp op) {
        this.mPostponedList.add(op);
        switch (op.cmd) {
            case 1:
                this.mCallback.offsetPositionsForAdd(op.positionStart, op.itemCount);
                return;
            case 2:
                this.mCallback.offsetPositionsForRemovingLaidOutOrNewView(op.positionStart, op.itemCount);
                return;
            case 4:
                this.mCallback.markViewHoldersUpdated(op.positionStart, op.itemCount, op.payload);
                return;
            case 8:
                this.mCallback.offsetPositionsForMove(op.positionStart, op.itemCount);
                return;
            default:
                throw new IllegalArgumentException("Unknown update op type for " + op);
        }
    }

    boolean hasPendingUpdates() {
        return this.mPendingUpdates.size() > 0;
    }

    boolean hasAnyUpdateTypes(int updateTypes) {
        return (this.mExistingUpdateTypes & updateTypes) != 0;
    }

    int findPositionOffset(int position) {
        return findPositionOffset(position, 0);
    }

    int findPositionOffset(int position, int firstPostponedItem) {
        int count = this.mPostponedList.size();
        for (int i = firstPostponedItem; i < count; i++) {
            UpdateOp op = (UpdateOp) this.mPostponedList.get(i);
            if (op.cmd != 8) {
                if (op.positionStart > position) {
                    continue;
                } else if (op.cmd != 2) {
                    if (op.cmd == 1) {
                        position += op.itemCount;
                    }
                } else if (position < op.positionStart + op.itemCount) {
                    return -1;
                } else {
                    position -= op.itemCount;
                }
            } else if (op.positionStart != position) {
                if (op.positionStart < position) {
                    position--;
                }
                if (op.itemCount <= position) {
                    position++;
                }
            } else {
                position = op.itemCount;
            }
        }
        return position;
    }

    void consumeUpdatesInOnePass() {
        consumePostponedUpdates();
        int count = this.mPendingUpdates.size();
        for (int i = 0; i < count; i++) {
            UpdateOp op = (UpdateOp) this.mPendingUpdates.get(i);
            switch (op.cmd) {
                case 1:
                    this.mCallback.onDispatchSecondPass(op);
                    this.mCallback.offsetPositionsForAdd(op.positionStart, op.itemCount);
                    break;
                case 2:
                    this.mCallback.onDispatchSecondPass(op);
                    this.mCallback.offsetPositionsForRemovingInvisible(op.positionStart, op.itemCount);
                    break;
                case 4:
                    this.mCallback.onDispatchSecondPass(op);
                    this.mCallback.markViewHoldersUpdated(op.positionStart, op.itemCount, op.payload);
                    break;
                case 8:
                    this.mCallback.onDispatchSecondPass(op);
                    this.mCallback.offsetPositionsForMove(op.positionStart, op.itemCount);
                    break;
            }
            if (this.mOnItemProcessedCallback != null) {
                this.mOnItemProcessedCallback.run();
            }
        }
        recycleUpdateOpsAndClearList(this.mPendingUpdates);
        this.mExistingUpdateTypes = 0;
    }

    public int applyPendingUpdatesToPosition(int position) {
        int size = this.mPendingUpdates.size();
        for (int i = 0; i < size; i++) {
            UpdateOp op = (UpdateOp) this.mPendingUpdates.get(i);
            switch (op.cmd) {
                case 1:
                    if (op.positionStart > position) {
                        break;
                    }
                    position += op.itemCount;
                    break;
                case 2:
                    if (op.positionStart > position) {
                        continue;
                    } else if (op.positionStart + op.itemCount <= position) {
                        position -= op.itemCount;
                        break;
                    } else {
                        return -1;
                    }
                case 8:
                    if (op.positionStart == position) {
                        position = op.itemCount;
                        break;
                    }
                    if (op.positionStart < position) {
                        position--;
                    }
                    if (op.itemCount > position) {
                        break;
                    }
                    position++;
                    break;
                default:
                    break;
            }
        }
        return position;
    }

    boolean hasUpdates() {
        return (this.mPostponedList.isEmpty() || this.mPendingUpdates.isEmpty()) ? false : true;
    }

    public UpdateOp obtainUpdateOp(int cmd, int positionStart, int itemCount, Object payload) {
        UpdateOp op = (UpdateOp) this.mUpdateOpPool.acquire();
        if (op == null) {
            return new UpdateOp(cmd, positionStart, itemCount, payload);
        }
        op.cmd = cmd;
        op.positionStart = positionStart;
        op.itemCount = itemCount;
        op.payload = payload;
        return op;
    }

    public void recycleUpdateOp(UpdateOp op) {
        if (!this.mDisableRecycler) {
            op.payload = null;
            this.mUpdateOpPool.release(op);
        }
    }

    void recycleUpdateOpsAndClearList(List<UpdateOp> ops) {
        int count = ops.size();
        for (int i = 0; i < count; i++) {
            recycleUpdateOp((UpdateOp) ops.get(i));
        }
        ops.clear();
    }
}

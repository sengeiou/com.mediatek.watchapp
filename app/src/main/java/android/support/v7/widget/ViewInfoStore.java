package android.support.v7.widget;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.LongSparseArray;
import android.support.v4.util.Pools$Pool;
import android.support.v4.util.Pools$SimplePool;
import android.support.v7.widget.RecyclerView.ItemAnimator.ItemHolderInfo;
import android.support.v7.widget.RecyclerView.ViewHolder;

class ViewInfoStore {
    @VisibleForTesting
    final ArrayMap<ViewHolder, InfoRecord> mLayoutHolderMap = new ArrayMap();
    @VisibleForTesting
    final LongSparseArray<ViewHolder> mOldChangedHolders = new LongSparseArray();

    interface ProcessCallback {
        void processAppeared(ViewHolder viewHolder, @Nullable ItemHolderInfo itemHolderInfo, ItemHolderInfo itemHolderInfo2);

        void processDisappeared(ViewHolder viewHolder, @NonNull ItemHolderInfo itemHolderInfo, @Nullable ItemHolderInfo itemHolderInfo2);

        void processPersistent(ViewHolder viewHolder, @NonNull ItemHolderInfo itemHolderInfo, @NonNull ItemHolderInfo itemHolderInfo2);

        void unused(ViewHolder viewHolder);
    }

    static class InfoRecord {
        static Pools$Pool<InfoRecord> sPool = new Pools$SimplePool(20);
        int flags;
        @Nullable
        ItemHolderInfo postInfo;
        @Nullable
        ItemHolderInfo preInfo;

        private InfoRecord() {
        }

        static InfoRecord obtain() {
            InfoRecord record = (InfoRecord) sPool.acquire();
            return record != null ? record : new InfoRecord();
        }

        static void recycle(InfoRecord record) {
            record.flags = 0;
            record.preInfo = null;
            record.postInfo = null;
            sPool.release(record);
        }

        static void drainCache() {
            do {
            } while (sPool.acquire() != null);
        }
    }

    ViewInfoStore() {
    }

    void clear() {
        this.mLayoutHolderMap.clear();
        this.mOldChangedHolders.clear();
    }

    void addToPreLayout(ViewHolder holder, ItemHolderInfo info) {
        InfoRecord record = (InfoRecord) this.mLayoutHolderMap.get(holder);
        if (record == null) {
            record = InfoRecord.obtain();
            this.mLayoutHolderMap.put(holder, record);
        }
        record.preInfo = info;
        record.flags |= 4;
    }

    boolean isDisappearing(ViewHolder holder) {
        InfoRecord record = (InfoRecord) this.mLayoutHolderMap.get(holder);
        if (record == null || (record.flags & 1) == 0) {
            return false;
        }
        return true;
    }

    @Nullable
    ItemHolderInfo popFromPreLayout(ViewHolder vh) {
        return popFromLayoutStep(vh, 4);
    }

    @Nullable
    ItemHolderInfo popFromPostLayout(ViewHolder vh) {
        return popFromLayoutStep(vh, 8);
    }

    private ItemHolderInfo popFromLayoutStep(ViewHolder vh, int flag) {
        int index = this.mLayoutHolderMap.indexOfKey(vh);
        if (index < 0) {
            return null;
        }
        InfoRecord record = (InfoRecord) this.mLayoutHolderMap.valueAt(index);
        if (record == null || (record.flags & flag) == 0) {
            return null;
        }
        ItemHolderInfo info;
        record.flags &= flag ^ -1;
        if (flag == 4) {
            info = record.preInfo;
        } else if (flag != 8) {
            throw new IllegalArgumentException("Must provide flag PRE or POST");
        } else {
            info = record.postInfo;
        }
        if ((record.flags & 12) == 0) {
            this.mLayoutHolderMap.removeAt(index);
            InfoRecord.recycle(record);
        }
        return info;
    }

    void addToOldChangeHolders(long key, ViewHolder holder) {
        this.mOldChangedHolders.put(key, holder);
    }

    void addToAppearedInPreLayoutHolders(ViewHolder holder, ItemHolderInfo info) {
        InfoRecord record = (InfoRecord) this.mLayoutHolderMap.get(holder);
        if (record == null) {
            record = InfoRecord.obtain();
            this.mLayoutHolderMap.put(holder, record);
        }
        record.flags |= 2;
        record.preInfo = info;
    }

    boolean isInPreLayout(ViewHolder viewHolder) {
        InfoRecord record = (InfoRecord) this.mLayoutHolderMap.get(viewHolder);
        if (record == null || (record.flags & 4) == 0) {
            return false;
        }
        return true;
    }

    ViewHolder getFromOldChangeHolders(long key) {
        return (ViewHolder) this.mOldChangedHolders.get(key);
    }

    void addToPostLayout(ViewHolder holder, ItemHolderInfo info) {
        InfoRecord record = (InfoRecord) this.mLayoutHolderMap.get(holder);
        if (record == null) {
            record = InfoRecord.obtain();
            this.mLayoutHolderMap.put(holder, record);
        }
        record.postInfo = info;
        record.flags |= 8;
    }

    void addToDisappearedInLayout(ViewHolder holder) {
        InfoRecord record = (InfoRecord) this.mLayoutHolderMap.get(holder);
        if (record == null) {
            record = InfoRecord.obtain();
            this.mLayoutHolderMap.put(holder, record);
        }
        record.flags |= 1;
    }

    void removeFromDisappearedInLayout(ViewHolder holder) {
        InfoRecord record = (InfoRecord) this.mLayoutHolderMap.get(holder);
        if (record != null) {
            record.flags &= -2;
        }
    }

    void process(ProcessCallback callback) {
        for (int index = this.mLayoutHolderMap.size() - 1; index >= 0; index--) {
            ViewHolder viewHolder = (ViewHolder) this.mLayoutHolderMap.keyAt(index);
            InfoRecord record = (InfoRecord) this.mLayoutHolderMap.removeAt(index);
            if ((record.flags & 3) == 3) {
                callback.unused(viewHolder);
            } else if ((record.flags & 1) == 0) {
                if ((record.flags & 14) == 14) {
                    callback.processAppeared(viewHolder, record.preInfo, record.postInfo);
                } else if ((record.flags & 12) == 12) {
                    callback.processPersistent(viewHolder, record.preInfo, record.postInfo);
                } else if ((record.flags & 4) != 0) {
                    callback.processDisappeared(viewHolder, record.preInfo, null);
                } else if ((record.flags & 8) != 0) {
                    callback.processAppeared(viewHolder, record.preInfo, record.postInfo);
                } else if ((record.flags & 2) == 0) {
                }
            } else if (record.preInfo != null) {
                callback.processDisappeared(viewHolder, record.preInfo, record.postInfo);
            } else {
                callback.unused(viewHolder);
            }
            InfoRecord.recycle(record);
        }
    }

    void removeViewHolder(ViewHolder holder) {
        for (int i = this.mOldChangedHolders.size() - 1; i >= 0; i--) {
            if (holder == this.mOldChangedHolders.valueAt(i)) {
                this.mOldChangedHolders.removeAt(i);
                break;
            }
        }
        InfoRecord info = (InfoRecord) this.mLayoutHolderMap.remove(holder);
        if (info != null) {
            InfoRecord.recycle(info);
        }
    }

    void onDetach() {
        InfoRecord.drainCache();
    }

    public void onViewDetached(ViewHolder viewHolder) {
        removeFromDisappearedInLayout(viewHolder);
    }
}

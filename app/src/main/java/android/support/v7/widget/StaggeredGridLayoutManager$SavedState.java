package android.support.v7.widget;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import java.util.List;

@RestrictTo({Scope.LIBRARY_GROUP})
public class StaggeredGridLayoutManager$SavedState implements Parcelable {
    public static final Creator<StaggeredGridLayoutManager$SavedState> CREATOR = new C00741();
    boolean mAnchorLayoutFromEnd;
    int mAnchorPosition;
    List<StaggeredGridLayoutManager$LazySpanLookup$FullSpanItem> mFullSpanItems;
    boolean mLastLayoutRTL;
    boolean mReverseLayout;
    int[] mSpanLookup;
    int mSpanLookupSize;
    int[] mSpanOffsets;
    int mSpanOffsetsSize;
    int mVisibleAnchorPosition;

    /* renamed from: android.support.v7.widget.StaggeredGridLayoutManager$SavedState$1 */
    static class C00741 implements Creator<StaggeredGridLayoutManager$SavedState> {
        C00741() {
        }

        public StaggeredGridLayoutManager$SavedState createFromParcel(Parcel in) {
            return new StaggeredGridLayoutManager$SavedState(in);
        }

        public StaggeredGridLayoutManager$SavedState[] newArray(int size) {
            return new StaggeredGridLayoutManager$SavedState[size];
        }
    }

    StaggeredGridLayoutManager$SavedState(Parcel in) {
        boolean z;
        boolean z2 = false;
        this.mAnchorPosition = in.readInt();
        this.mVisibleAnchorPosition = in.readInt();
        this.mSpanOffsetsSize = in.readInt();
        if (this.mSpanOffsetsSize > 0) {
            this.mSpanOffsets = new int[this.mSpanOffsetsSize];
            in.readIntArray(this.mSpanOffsets);
        }
        this.mSpanLookupSize = in.readInt();
        if (this.mSpanLookupSize > 0) {
            this.mSpanLookup = new int[this.mSpanLookupSize];
            in.readIntArray(this.mSpanLookup);
        }
        if (in.readInt() != 1) {
            z = false;
        } else {
            z = true;
        }
        this.mReverseLayout = z;
        if (in.readInt() != 1) {
            z = false;
        } else {
            z = true;
        }
        this.mAnchorLayoutFromEnd = z;
        if (in.readInt() == 1) {
            z2 = true;
        }
        this.mLastLayoutRTL = z2;
        this.mFullSpanItems = in.readArrayList(StaggeredGridLayoutManager$LazySpanLookup$FullSpanItem.class.getClassLoader());
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        int i;
        int i2 = 0;
        dest.writeInt(this.mAnchorPosition);
        dest.writeInt(this.mVisibleAnchorPosition);
        dest.writeInt(this.mSpanOffsetsSize);
        if (this.mSpanOffsetsSize > 0) {
            dest.writeIntArray(this.mSpanOffsets);
        }
        dest.writeInt(this.mSpanLookupSize);
        if (this.mSpanLookupSize > 0) {
            dest.writeIntArray(this.mSpanLookup);
        }
        if (this.mReverseLayout) {
            i = 1;
        } else {
            i = 0;
        }
        dest.writeInt(i);
        if (this.mAnchorLayoutFromEnd) {
            i = 1;
        } else {
            i = 0;
        }
        dest.writeInt(i);
        if (this.mLastLayoutRTL) {
            i2 = 1;
        }
        dest.writeInt(i2);
        dest.writeList(this.mFullSpanItems);
    }
}
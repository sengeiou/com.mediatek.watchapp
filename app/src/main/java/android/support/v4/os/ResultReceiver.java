package android.support.v4.os;

import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.os.IResultReceiver.Stub;

@RestrictTo({Scope.LIBRARY_GROUP})
public class ResultReceiver implements Parcelable {
    public static final Creator<ResultReceiver> CREATOR = new C00241();
    final Handler mHandler = null;
    final boolean mLocal = false;
    IResultReceiver mReceiver;

    /* renamed from: android.support.v4.os.ResultReceiver$1 */
    static class C00241 implements Creator<ResultReceiver> {
        C00241() {
        }

        public ResultReceiver createFromParcel(Parcel in) {
            return new ResultReceiver(in);
        }

        public ResultReceiver[] newArray(int size) {
            return new ResultReceiver[size];
        }
    }

    class MyResultReceiver extends Stub {
        MyResultReceiver() {
        }

        public void send(int resultCode, Bundle resultData) {
            if (ResultReceiver.this.mHandler == null) {
                ResultReceiver.this.onReceiveResult(resultCode, resultData);
            } else {
                ResultReceiver.this.mHandler.post(new MyRunnable(resultCode, resultData));
            }
        }
    }

    class MyRunnable implements Runnable {
        final int mResultCode;
        final Bundle mResultData;

        MyRunnable(int resultCode, Bundle resultData) {
            this.mResultCode = resultCode;
            this.mResultData = resultData;
        }

        public void run() {
            ResultReceiver.this.onReceiveResult(this.mResultCode, this.mResultData);
        }
    }

    protected void onReceiveResult(int resultCode, Bundle resultData) {
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        synchronized (this) {
            if (this.mReceiver == null) {
                this.mReceiver = new MyResultReceiver();
            }
            out.writeStrongBinder(this.mReceiver.asBinder());
        }
    }

    ResultReceiver(Parcel in) {
        this.mReceiver = Stub.asInterface(in.readStrongBinder());
    }
}

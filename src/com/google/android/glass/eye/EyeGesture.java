package com.google.android.glass.eye;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Almost empty stub, only for the code to compile. The implementation in on Glass itself.
 *
 */
public enum EyeGesture implements Parcelable {
    DOFF, DON, DOUBLE_BLINK, DOUBLE_WINK, WINK;

    public int getId() {
        return -1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel arg0, int arg1) {
    }
}

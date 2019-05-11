package android.support.v4.view;

import android.annotation.TargetApi;
import android.support.annotation.RequiresApi;
import android.view.WindowInsets;

@TargetApi(20)
@RequiresApi(20)
class WindowInsetsCompatApi20 {
    WindowInsetsCompatApi20() {
    }

    public static int getSystemWindowInsetBottom(Object insets) {
        return ((WindowInsets) insets).getSystemWindowInsetBottom();
    }

    public static int getSystemWindowInsetLeft(Object insets) {
        return ((WindowInsets) insets).getSystemWindowInsetLeft();
    }

    public static int getSystemWindowInsetRight(Object insets) {
        return ((WindowInsets) insets).getSystemWindowInsetRight();
    }

    public static int getSystemWindowInsetTop(Object insets) {
        return ((WindowInsets) insets).getSystemWindowInsetTop();
    }

    public static Object replaceSystemWindowInsets(Object insets, int left, int top, int right, int bottom) {
        return ((WindowInsets) insets).replaceSystemWindowInsets(left, top, right, bottom);
    }
}

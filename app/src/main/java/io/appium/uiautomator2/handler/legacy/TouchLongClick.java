/*
 * Matrix: legacy handler restored from the sonic 5.7.4 fork.
 * Serves the /touch/longclick endpoint used by sonic-driver-core's UiaClient.
 */
package io.appium.uiautomator2.handler.legacy;

import android.os.SystemClock;

import androidx.test.uiautomator.UiObjectNotFoundException;

import io.appium.uiautomator2.common.exceptions.InvalidElementStateException;

public class TouchLongClick extends BaseTouchAction {
    private static final int DEFAULT_DURATION_MS = 2000;

    public TouchLongClick(String mappedUri) {
        super(mappedUri);
    }

    private boolean performLongClick(final int x, final int y, final int duration) {
        boolean isSuccessful = getIc().touchDown(x, y);
        if (isSuccessful) {
            SystemClock.sleep(duration);
            isSuccessful = getIc().touchUp(x, y);
        }
        return isSuccessful;
    }

    @Override
    protected void executeEvent() throws UiObjectNotFoundException {
        int duration = params.duration != null
                ? (int) Math.round(params.duration)
                : DEFAULT_DURATION_MS;
        printEventDebugLine(duration);
        if (performLongClick(clickX, clickY, duration)) {
            return;
        }
        if (element == null) {
            throw new InvalidElementStateException(
                    String.format("Cannot perform %s action at (%s, %s)", getName(), clickX, clickY));
        }
        element.longClick();
    }
}

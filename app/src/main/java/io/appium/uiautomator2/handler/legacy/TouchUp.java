/*
 * Matrix: legacy handler restored from the sonic 5.7.4 fork.
 * Serves the /touch/up endpoint used by sonic-driver-core's UiaClient.
 */
package io.appium.uiautomator2.handler.legacy;

import io.appium.uiautomator2.common.exceptions.InvalidElementStateException;

public class TouchUp extends BaseTouchAction {
    public TouchUp(String mappedUri) {
        super(mappedUri);
    }

    @Override
    protected void executeEvent() {
        printEventDebugLine();
        if (!getIc().touchUp(clickX, clickY)) {
            throw new InvalidElementStateException(
                    String.format("Cannot perform %s action at (%s, %s)", getName(), clickX, clickY));
        }
    }
}

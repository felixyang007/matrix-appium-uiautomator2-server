/*
 * Matrix: legacy handler restored from the sonic 5.7.4 fork.
 * Serves the /touch/move endpoint used by sonic-driver-core's UiaClient.
 */
package io.appium.uiautomator2.handler.legacy;

import io.appium.uiautomator2.common.exceptions.InvalidElementStateException;
import io.appium.uiautomator2.common.exceptions.UiAutomator2Exception;

public class TouchMove extends BaseTouchAction {
    public TouchMove(String mappedUri) {
        super(mappedUri);
    }

    @Override
    protected void executeEvent() throws UiAutomator2Exception {
        printEventDebugLine();
        if (!getIc().touchMove(clickX, clickY)) {
            throw new InvalidElementStateException(
                    String.format("Cannot perform %s action at (%s, %s)", getName(), clickX, clickY));
        }
    }
}

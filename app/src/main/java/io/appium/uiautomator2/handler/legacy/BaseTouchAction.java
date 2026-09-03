/*
 * Matrix: legacy touch-action base restored from the sonic 5.7.4 fork.
 * Serves /touch/longclick, /touch/down, /touch/move and /touch/up endpoints
 * used by sonic-driver-core's UiaClient.
 */
package io.appium.uiautomator2.handler.legacy;

import android.graphics.Rect;

import androidx.test.uiautomator.UiObjectNotFoundException;

import io.appium.uiautomator2.common.exceptions.InvalidElementStateException;
import io.appium.uiautomator2.core.InteractionController;
import io.appium.uiautomator2.core.UiAutomatorBridge;
import io.appium.uiautomator2.handler.request.SafeRequestHandler;
import io.appium.uiautomator2.http.AppiumResponse;
import io.appium.uiautomator2.http.IHttpRequest;
import io.appium.uiautomator2.model.AndroidElement;
import io.appium.uiautomator2.model.AppiumUIA2Driver;
import io.appium.uiautomator2.model.Session;
import io.appium.uiautomator2.model.api.legacy.TouchEventModel;
import io.appium.uiautomator2.model.api.legacy.TouchEventParams;
import io.appium.uiautomator2.utils.Logger;

import static io.appium.uiautomator2.utils.ModelUtils.toModel;

public abstract class BaseTouchAction extends SafeRequestHandler {
    protected int clickX;
    protected int clickY;
    protected AndroidElement element;
    protected TouchEventParams params;

    public BaseTouchAction(String mappedUri) {
        super(mappedUri);
    }

    @Override
    protected AppiumResponse safeHandle(IHttpRequest request) throws UiObjectNotFoundException {
        params = toModel(request, TouchEventModel.class).params;
        final String elementId = params.getUnifiedId();
        if (elementId != null && params.x == null && params.y == null) {
            Session session = AppiumUIA2Driver.getInstance().getSessionOrThrow();
            element = session.getElementsCache().get(elementId);
            final Rect bounds = element.getBounds();
            clickX = bounds.centerX();
            clickY = bounds.centerY();
        } else { // no element so extract x and y from params
            if (params.x == null || params.y == null) {
                throw new IllegalArgumentException(
                        "Both x and y coordinates must be provided without element id set");
            }
            clickX = (int) Math.round(params.x);
            clickY = (int) Math.round(params.y);
        }
        executeEvent();
        return new AppiumResponse(getSessionId(request));
    }

    protected abstract void executeEvent() throws UiObjectNotFoundException;

    protected InteractionController getIc() {
        return UiAutomatorBridge.getInstance().getInteractionController();
    }

    protected String getName() {
        return getClass().getSimpleName();
    }

    protected void printEventDebugLine() {
        printEventDebugLine(null);
    }

    protected void printEventDebugLine(Integer duration) {
        Logger.debug(String.format(
                "Performing %s at x: (%s, %s)%s", getName(), clickX, clickY,
                duration == null ? "" : String.format(", duration: %s", duration)));
    }
}

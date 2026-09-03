/*
 * Matrix: legacy handler restored from the sonic 5.7.4 fork.
 * Serves the /touch/drag endpoint used by sonic-driver-core's UiaClient.
 */
package io.appium.uiautomator2.handler.legacy;

import androidx.test.uiautomator.UiObjectNotFoundException;

import io.appium.uiautomator2.common.exceptions.InvalidElementStateException;
import io.appium.uiautomator2.handler.request.SafeRequestHandler;
import io.appium.uiautomator2.http.AppiumResponse;
import io.appium.uiautomator2.http.IHttpRequest;
import io.appium.uiautomator2.model.AndroidElement;
import io.appium.uiautomator2.model.AppiumUIA2Driver;
import io.appium.uiautomator2.model.ElementsCache;
import io.appium.uiautomator2.model.Point;
import io.appium.uiautomator2.model.api.legacy.LegacyDragModel;
import io.appium.uiautomator2.utils.Logger;
import io.appium.uiautomator2.utils.PositionHelper;

import static io.appium.uiautomator2.utils.Device.getUiDevice;
import static io.appium.uiautomator2.utils.ModelUtils.toModel;

public class Drag extends SafeRequestHandler {
    public Drag(String mappedUri) {
        super(mappedUri);
    }

    @Override
    protected AppiumResponse safeHandle(IHttpRequest request) throws UiObjectNotFoundException {
        LegacyDragModel model = toModel(request, LegacyDragModel.class);
        ElementsCache ke = AppiumUIA2Driver.getInstance().getSessionOrThrow().getElementsCache();
        AndroidElement startElement = model.elementId == null ? null : ke.get(model.elementId);
        AndroidElement endElement = model.destElId == null ? null : ke.get(model.destElId);
        Point start = (model.startX != null && model.startY != null) ? new Point(model.startX, model.startY) : null;
        Point end = (model.endX != null && model.endY != null) ? new Point(model.endX, model.endY) : null;
        if (startElement == null) {
            if (start == null || end == null) {
                throw new IllegalArgumentException(
                        "Both startX/startY and endX/endY must be set if no element ids are provided");
            }
            Point absStartPos = PositionHelper.getDeviceAbsPos(start);
            Point absEndPos = PositionHelper.getDeviceAbsPos(end);
            if (!performDrag(absStartPos, absEndPos, model.steps)) {
                throw new InvalidElementStateException(String.format(
                        "Drag from %s to %s did not complete successfully",
                        absStartPos, absEndPos));
            }
        } else if (endElement == null) {
            if (end == null) {
                throw new IllegalArgumentException(
                        "Both endX and endY must be set if no destination element id is provided");
            }
            Point absEndPos = PositionHelper.getDeviceAbsPos(end);
            if (!performDrag(startElement, absEndPos, model.steps)) {
                throw new InvalidElementStateException(String.format(
                        "Drag element %s to %s did not complete successfully",
                        startElement.getId(), absEndPos));
            }
        } else {
            if (!performDrag(startElement, endElement, model.steps)) {
                throw new InvalidElementStateException(String.format(
                        "Drag element %s to element %s did not complete successfully",
                        startElement.getId(), endElement.getId()));
            }
        }
        return new AppiumResponse(getSessionId(request));
    }

    private boolean performDrag(Point start, Point end, int steps) {
        Logger.debug(String.format("Dragging from %s to %s in %s steps",
                start, end, steps));
        return getUiDevice().drag(start.x.intValue(), start.y.intValue(),
                end.x.intValue(), end.y.intValue(), steps);
    }

    private boolean performDrag(AndroidElement start, Point end, int steps) throws UiObjectNotFoundException {
        Logger.debug(String.format("Dragging the element %s to %s in %s steps",
                start.getId(), end, steps));
        return start.dragTo(end.x.intValue(), end.y.intValue(), steps);
    }

    private boolean performDrag(AndroidElement start, AndroidElement end, int steps) throws UiObjectNotFoundException {
        Logger.debug(String.format("Dragging the element %s to the element %s in %s steps",
                start.getId(), end.getId(), steps));
        return start.dragTo(end.getUiObject(), steps);
    }
}

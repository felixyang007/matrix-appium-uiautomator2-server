/*
 * Matrix: legacy models restored from the sonic 5.7.4 fork for the /touch/down,
 * /touch/move, /touch/up and /touch/longclick endpoints used by
 * sonic-driver-core's UiaClient.
 */
package io.appium.uiautomator2.model.api.legacy;

import io.appium.uiautomator2.model.RequiredField;
import io.appium.uiautomator2.model.api.BaseModel;
import io.appium.uiautomator2.model.api.ElementModel;

public class TouchEventParams extends ElementModel {
    public Double x;
    public Double y;
    public Double duration;

    public TouchEventParams() {}
}


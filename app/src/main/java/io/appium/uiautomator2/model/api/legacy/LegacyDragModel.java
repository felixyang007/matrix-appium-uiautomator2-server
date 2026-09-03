/*
 * Matrix: legacy model restored from the sonic 5.7.4 fork for the /touch/drag
 * endpoint used by sonic-driver-core's UiaClient.
 */
package io.appium.uiautomator2.model.api.legacy;

import io.appium.uiautomator2.model.RequiredField;
import io.appium.uiautomator2.model.api.BaseModel;

public class LegacyDragModel extends BaseModel {
    public String elementId;
    public String destElId;
    public Double startX;
    public Double startY;
    public Double endX;
    public Double endY;
    @RequiredField
    public Integer steps;

    public LegacyDragModel() {}
}

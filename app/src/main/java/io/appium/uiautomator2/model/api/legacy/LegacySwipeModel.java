/*
 * Matrix: legacy model restored from the sonic 5.7.4 fork for the /touch/perform
 * endpoint used by sonic-driver-core's UiaClient.
 */
package io.appium.uiautomator2.model.api.legacy;

import io.appium.uiautomator2.model.RequiredField;
import io.appium.uiautomator2.model.api.BaseModel;

public class LegacySwipeModel extends BaseModel {
    public String elementId;
    @RequiredField
    public Double startX;
    @RequiredField
    public Double startY;
    @RequiredField
    public Double endX;
    @RequiredField
    public Double endY;
    @RequiredField
    public Integer steps;

    public LegacySwipeModel() {}
}

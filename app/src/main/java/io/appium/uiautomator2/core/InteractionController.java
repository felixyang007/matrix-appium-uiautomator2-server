/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.appium.uiautomator2.core;

import android.os.SystemClock;
import android.view.InputEvent;
import android.view.MotionEvent;

import io.appium.uiautomator2.common.exceptions.UiAutomator2Exception;

import static io.appium.uiautomator2.utils.ReflectionUtils.getMethod;
import static io.appium.uiautomator2.utils.ReflectionUtils.invoke;

public class InteractionController {

    private static final String CLASS_INTERACTION_CONTROLLER = "androidx.test.uiautomator.InteractionController";
    private static final String METHOD_INJECT_EVENT_SYNC = "injectEventSync";
    private final Object interactionController;

    public InteractionController(Object interactionController) {
        this.interactionController = interactionController;
    }

    public boolean injectEventSync(final InputEvent event) throws UiAutomator2Exception {
        return (Boolean) invoke(getMethod(CLASS_INTERACTION_CONTROLLER,
                METHOD_INJECT_EVENT_SYNC, InputEvent.class), interactionController, event);
    }

    /**
     * Matrix: legacy low-level touch primitives (restored for /touch/down, /touch/move and /touch/up
     * endpoints used by sonic-driver-core's UiaClient for remote-control drag sessions).
     *
     * The androidx InteractionController used to expose touchDown/touchMove/touchUp directly
     * (as the sonic 5.7.4 fork relied on via reflection); newer uiautomator (2.3.0) only guarantees
     * injectEventSync(InputEvent), so we synthesize single-pointer MotionEvents ourselves.
     *
     * A valid touch sequence requires MOVE/UP events to reference the downTime of the last
     * ACTION_DOWN — each event getting its own downTime makes the injected sequence invalid
     * (the input pipeline may drop it or worse), so the last downTime is tracked here.
     */
    private static volatile long lastTouchDownTime = 0L;

    private boolean injectSinglePointerEvent(final int action, final long downTime,
            final int x, final int y) throws UiAutomator2Exception {
        final MotionEvent event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(),
                action, x, y, 0 /* metaState */);
        try {
            return injectEventSync(event);
        } finally {
            event.recycle();
        }
    }

    public boolean touchDown(final int x, final int y) throws UiAutomator2Exception {
        lastTouchDownTime = SystemClock.uptimeMillis();
        return injectSinglePointerEvent(MotionEvent.ACTION_DOWN, lastTouchDownTime, x, y);
    }

    public boolean touchMove(final int x, final int y) throws UiAutomator2Exception {
        return injectSinglePointerEvent(MotionEvent.ACTION_MOVE, lastTouchDownTime, x, y);
    }

    public boolean touchUp(final int x, final int y) throws UiAutomator2Exception {
        return injectSinglePointerEvent(MotionEvent.ACTION_UP, lastTouchDownTime, x, y);
    }
}

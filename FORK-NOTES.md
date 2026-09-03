# matrix-appium-uiautomator2-server

> **Matrix**（Plaud 团队）自维护 fork：基于 **appium/appium-uiautomator2-server v10.6.2**（AGPL-2.0 风格 Apache-2.0 许可，见上游 LICENSE），**加回 sonic 5.7.4 fork 的 7 个老端点**，使 `matrix-driver-core`（sonic-driver-core 1.1.x）与 `matrix-agent` **零改动**即可升级到新引擎。

## 分支 / Tag 约定

- `matrix/10.6.2-legacy-touch`（当前）：v10.6.2 + legacy 端点移植
- 上游：`upstream` → https://github.com/appium/appium-uiautomator2-server （活跃维护，可 fetch 跟进新版本）
- 发布产物：`app/src/main/gradle.properties` 的 `versionName` 保持与上游 tag 一致（10.6.2）——agent 用 `dumpsys package io.appium.uiautomator2.server` 的 versionName 与配置 `sonic.saus` 精确比对，**改版本必须三处同步**（gradle.properties / agent `application.yml` / 部署的 apk）。

## 为什么要 fork：兼容性差异（10.6.2 vs sonic 5.7.4）

上游 **v6 起**移除了 JSON Wire Protocol 时代的 touch 系列端点，而 sonic-driver-core 1.1.33 的 `UiaClientImpl` 仍在使用：

| 老端点（sonic 5.7.4 有 / 上游 10.6.2 已删） | driver-core 用途 | 本 fork 处置 |
|---|---|---|
| `POST /session/:id/appium/tap` | 坐标点击 | 从 sonic fork 移植 `legacy.Tap` |
| `POST /session/:id/touch/longclick` | 长按 | `legacy.TouchLongClick` |
| `POST /session/:id/touch/perform` | 坐标滑动 | `legacy.Swipe` |
| `POST /session/:id/touch/drag` | 拖拽 | `legacy.Drag` |
| `POST /session/:id/touch/down` / `move` / `up` | **远控实时拖拽**（逐事件跨 HTTP 调用，无法用 W3C actions 替代） | `legacy.TouchDown/Move/Up` |

其余老依赖 API（`UiAutomatorBridge`、`InteractionController.injectEventSync`、`PositionHelper`、`EventRegister`、`AndroidElement.dragTo` 等）在 10.6.2 均健在；唯一需要重建的是低级触摸注入——androidx uiautomator 2.3.0 的 InteractionController 不再暴露 `touchDown/touchMove/touchUp`（sonic 5.7.4 靠反射调用），本 fork 在 `core/InteractionController` 用 `MotionEvent.obtain(...) + injectEventSync(event)` 重新实现（与上游 W3C `ActionsExecutor` 同通道）。

## 代码地图（相对上游 v10.6.2 的全部改动）

```
app/src/main/java/io/appium/uiautomator2/
├── core/InteractionController.java        # + touchDown/touchMove/touchUp（MotionEvent 通道）
├── server/AppiumServlet.java              # + 7 条 legacy 路由注册（POST）
├── handler/legacy/                        # 新增：BaseTouchAction + Tap/TouchDown/TouchMove/
│                                           #   TouchUp/TouchLongClick/Swipe/Drag（源码自 sonic fork）
└── model/api/legacy/                      # 新增：TapModel/TouchEventModel/TouchEventParams/
                                            #   LegacySwipeModel/LegacyDragModel（字段与 sonic 请求体一致）
```

## 构建

```bash
# 需 JDK 17+（本机 Java 21 验证通过）、Android SDK（compileSdk 36）
./gradlew :vendor-xpath2:jar assembleServerDebug assembleServerDebugAndroidTest -x lint
# 产物：
#   app/build/outputs/apk/server/debug/appium-uiautomator2-server-v10.6.2.apk        （主 apk）
#   app/build/outputs/apk/androidTest/server/debug/appium-uiautomator2-server-v10.6.2-debug-androidTest.apk （test apk）
```

上游 `package.json` 的 `build` 还有 `move-apks`/`sign-apk`（TESTKEY），非必需——debug 签名即可被 agent `adb install`。

## 部署到 agent

1. 两个 apk 覆盖 `matrix-agent/plugins/`（**保持原文件名** `sonic-appium-uiautomator2-server.apk` / `sonic-appium-uiautomator2-server-test.apk`，agent 源码按此路径引用）。
2. `matrix-agent/src/main/resources/application.yml`：`sonic.saus: 5.7.4 → 10.6.2`。
3. 存量设备上旧版会被 agent 自动检测（versionName 不匹配 → 卸载重装），无需手动清理。

## 跟进上游

```bash
git fetch upstream && git checkout matrix/10.6.2-legacy-touch
git rebase v10.7.0   # 例：升级到新版后，重新落上面「代码地图」的改动（面小，冲突可控）
./gradlew ...        # 重新构建 + 真机回归 touch 系列
```

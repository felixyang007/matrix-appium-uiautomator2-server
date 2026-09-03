#!/usr/bin/env bash
#
# Matrix: uiautomator2-server fork 冒烟测试（Android 设备/模拟器）
# 验证 v10.6.2 fork 的全部 driver-core 依赖端点，重点压测移植的
# /touch/down|move|up 低级序列（downTime 修复验证）与测试后设备存活。
#
# 用法：./scripts/matrix-smoke-test.sh <device-serial>
#   例：./scripts/matrix-smoke-test.sh 10.0.155.203:51931
#
set -uo pipefail

DEV="${1:?用法: $0 <device-serial>}"
LOCAL_PORT=16790
B="http://localhost:$LOCAL_PORT"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MAIN_APK="$ROOT/app/build/outputs/apk/server/debug/appium-uiautomator2-server-v10.6.2.apk"
TEST_APK="$ROOT/app/build/outputs/apk/androidTest/server/debug/appium-uiautomator2-server-debug-androidTest.apk"
PASS=0; FAIL=0

ok()   { PASS=$((PASS+1)); echo "  ✅ $1"; }
bad()  { FAIL=$((FAIL+1)); echo "  ❌ $1"; }

req() { # req <name> <method> <path> [body]
  local name="$1" method="$2" path="$3" body="${4:-}" resp
  if [ -n "$body" ]; then
    resp="$(curl -s -m 20 -X "$method" "$B$path" -H 'Content-Type: application/json' -d "$body")"
  else
    resp="$(curl -s -m 20 -X "$method" "$B$path")"
  fi
  if echo "$resp" | grep -q '"error"'; then bad "$name → $(echo "$resp" | head -c 160)"; else ok "$name"; fi
}

echo "=== 0. 设备与产物检查 ==="
adb -s "$DEV" get-state >/dev/null 2>&1 || { echo "❌ 设备不在线: $DEV"; exit 1; }
[ -f "$MAIN_APK" ] || { echo "❌ 缺主 apk，先 ./gradlew assembleServerDebug assembleServerDebugAndroidTest -x lint"; exit 1; }
[ -f "$TEST_APK" ] || { echo "❌ 缺 test apk"; exit 1; }

echo "=== 1. 安装（versionName 应为 10.6.2）==="
adb -s "$DEV" uninstall io.appium.uiautomator2.server >/dev/null 2>&1
adb -s "$DEV" uninstall io.appium.uiautomator2.server.test >/dev/null 2>&1
adb -s "$DEV" install -r "$MAIN_APK" >/dev/null 2>&1 && ok "install main" || bad "install main"
adb -s "$DEV" install -r "$TEST_APK" >/dev/null 2>&1 && ok "install test" || bad "install test"
VN="$(adb -s "$DEV" shell dumpsys package io.appium.uiautomator2.server 2>/dev/null | grep -m1 versionName | tr -d ' \r')"
echo "$VN" | grep -q "10.6.2" && ok "versionName=10.6.2" || bad "$VN"

echo "=== 2. 启动 instrumentation（与 agent 相同的命令与等待标志）==="
adb -s "$DEV" shell "appops set io.appium.uiautomator2.server RUN_IN_BACKGROUND allow; appops set io.appium.uiautomator2.server.test RUN_IN_BACKGROUND allow; dumpsys deviceidle whitelist +io.appium.uiautomator2.server; dumpsys deviceidle whitelist +io.appium.uiautomator2.server.test" >/dev/null 2>&1
adb -s "$DEV" forward tcp:$LOCAL_PORT tcp:6790 >/dev/null
# 杀掉残留 instrumentation（幂等重跑）
adb -s "$DEV" shell am instrument -w io.appium.uiautomator2.server.test/androidx.test.runner.AndroidJUnitRunner \
  -e DISABLE_SUPPRESS_ACCESSIBILITY_SERVICES true -e disableAnalytics true >/dev/null 2>&1 &
INSTR_PID=$!
READY=0
for i in $(seq 1 20); do
  R="$(curl -s -m 3 $B/status 2>/dev/null)"
  echo "$R" | grep -q '"ready":true' && { READY=1; break; }
  sleep 2
done
[ "$READY" = "1" ] && ok "server ready: $(echo "$R" | head -c 120)" || { bad "server not ready"; kill $INSTR_PID 2>/dev/null; exit 1; }

echo "=== 3. 端点冒烟（driver-core UiaClient 全部依赖）==="
SID=$(curl -s -m 15 -X POST $B/session -H 'Content-Type: application/json' \
  -d '{"capabilities":{"alwaysMatch":{"platformName":"android","appium:automationName":"UiAutomator2"}}}' \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['value']['sessionId'])") || SID=""
[ -n "$SID" ] && ok "POST /session ($SID)" || bad "POST /session"
P="/session/$SID"

req "GET  /window/:windowHandle/size" GET  "$P/window/:windowHandle/size"
req "POST /appium/tap"                POST "$P/appium/tap" '{"x":540,"y":1200}'
req "POST /touch/longclick"           POST "$P/touch/longclick" '{"params":{"x":540,"y":1300,"duration":800}}'
req "POST /touch/perform (swipe)"     POST "$P/touch/perform" '{"startX":540,"startY":1600,"endX":540,"endY":800,"steps":100}'
req "POST /touch/drag"                POST "$P/touch/drag" '{"startX":540,"startY":1600,"endX":300,"endY":600,"steps":100}'
req "POST /keys"                      POST "$P/keys" '{"value":["S"]}'
req "POST /appium/settings"           POST "$P/appium/settings" '{"settings":{"waitForIdleTimeout":500}}'
req "POST /appium/device/set_clipboard" POST "$P/appium/device/set_clipboard" '{"content":"matrix-test"}'
req "POST /appium/device/get_clipboard" POST "$P/appium/device/get_clipboard" '{"contentType":"PLAINTEXT"}'
req "GET  /source"                    GET  "$P/source"
req "GET  /screenshot"                GET  "$P/screenshot"

echo "=== 4. 低级 touch 序列压测（downTime 修复验证，5 轮）==="
for i in 1 2 3 4 5; do
  req "round$i /touch/down"  POST "$P/touch/down"  '{"params":{"x":540,"y":1600}}'
  req "round$i /touch/move"  POST "$P/touch/move"  '{"params":{"x":540,"y":1300}}'
  req "round$i /touch/move"  POST "$P/touch/move"  '{"params":{"x":540,"y":1000}}'
  req "round$i /touch/up"    POST "$P/touch/up"    '{"params":{"x":540,"y":1000}}'
  sleep 1
done

echo "=== 5. 压测后存活检查（上次疑似在此崩溃）==="
sleep 5
R="$(curl -s -m 5 $B/status 2>/dev/null)"
echo "$R" | grep -q '"ready":true' && ok "server 仍 ready" || bad "server 无响应: $R"
adb -s "$DEV" get-state >/dev/null 2>&1 && ok "设备仍在线" || bad "设备掉线！"

req "DELETE /session" DELETE "$P"

kill $INSTR_PID 2>/dev/null
adb -s "$DEV" forward --remove tcp:$LOCAL_PORT >/dev/null 2>&1

echo ""
echo "=== 结果: PASS=$PASS FAIL=$FAIL ==="
[ "$FAIL" = "0" ] && echo "🎉 全部通过" || echo "⚠️ 有失败项，见上"
exit $([ "$FAIL" = "0" ] && echo 0 || echo 1)

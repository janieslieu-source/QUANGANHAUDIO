#!/usr/bin/env bash
# Đóng gói APK "Quang Anh MT" (khung app mở phần mềm bảo hành bản web).
# Cần: JDK 17+, aapt2 (npm aaptjs3), android.jar API 28 (Maven Central: org.robolectric:android-all:9-robolectric-4913185-2),
#      dx (Maven Central: com.jakewharton.android.repackaged:dalvik-dx:14.0.0_r21), keystore ký app.
# Dùng: TOOLS=/đường/dẫn/thư/mục/công/cụ KEYSTORE=quanganh.jks KSPASS=... ./build.sh
set -euo pipefail
cd "$(dirname "$0")"
T="${TOOLS:?đặt TOOLS = thư mục chứa aapt2, android-all.jar, dalvik-dx.jar}"
OUT=build; rm -rf "$OUT"; mkdir -p "$OUT/classes"
"$T/aapt2" compile --dir res -o "$OUT/res.zip"
"$T/aapt2" link -o "$OUT/base.apk" -I "$T/android-all.jar" --manifest AndroidManifest.xml "$OUT/res.zip" --auto-add-overlay
javac -nowarn --release 8 -encoding UTF-8 -cp "$T/android-all.jar" -d "$OUT/classes" $(find src -name '*.java')
java -cp "$T/dalvik-dx.jar" com.android.dx.command.Main --dex --min-sdk-version=23 --output="$OUT/classes.dex" "$OUT/classes"
cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT" && zip -q -0 unsigned.apk classes.dex)
jarsigner -sigalg SHA256withRSA -digestalg SHA-256 -keystore "${KEYSTORE:?}" -storepass "${KSPASS:?}" -signedjar "$OUT/QuangAnhMT.apk" "$OUT/unsigned.apk" "${KEYALIAS:-quanganh}" >/dev/null
echo "APK: $OUT/QuangAnhMT.apk"

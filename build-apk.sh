#!/bin/bash
# 一键编译 APK
TYPE=${1:-debug}  # 默认 debug，也可传入 release

echo "=============================="
echo " Local AI Painter v3.7 编译"
echo " 模式: $TYPE"
echo "=============================="

if [ ! -f "gradlew" ]; then
    echo "[ERROR] 找不到 gradlew，请确保你在项目根目录。"
    exit 1
fi

if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "[ERROR] 缺少 gradle-wrapper.jar，请先运行 fetch-wrapper-jar.sh"
    exit 1
fi

chmod +x gradlew
echo "[INFO] 开始编译 ${TYPE} 版 APK..."
./gradlew "assemble${TYPE^}"

if [ $? -eq 0 ]; then
    echo ""
    echo "[SUCCESS] 编译成功！APK 位于:"
    echo "  app/build/outputs/apk/${TYPE}/app-${TYPE}.apk"
else
    echo "[FAIL] 编译失败，请检查上方错误信息。"
    exit 1
fi

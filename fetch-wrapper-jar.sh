#!/bin/bash
# 自动下载 Gradle 9.3.1 发行包并提取 gradle-wrapper.jar
WRAPPER_DIR="gradle/wrapper"
ZIP_FILE="gradle-9.3.1-bin.zip"
URL="https://services.gradle.org/distributions/${ZIP_FILE}"

echo "[INFO] 正在下载 Gradle 9.3.1 发行包..."
wget -q --show-progress "$URL" -O "$ZIP_FILE"

if [ $? -ne 0 ]; then
    echo "[ERROR] 下载失败，请检查网络连接。"
    exit 1
fi

echo "[INFO] 正在提取 gradle-wrapper.jar..."
unzip -j "$ZIP_FILE" "gradle-9.3.1/lib/gradle-wrapper.jar" -d "$WRAPPER_DIR"

if [ $? -eq 0 ]; then
    echo "[SUCCESS] gradle-wrapper.jar 已放入 $WRAPPER_DIR/"
    rm -f "$ZIP_FILE"
    echo "[INFO] 临时文件已清理。"
else
    echo "[ERROR] 提取失败，请手动解压。"
    exit 1
fi

#!/bin/bash
# =============================================================================
# install-wrapper-jar.sh
# 终极方案：自动获取 gradle-wrapper.jar
# 适用于 Termux / Linux / macOS / Windows(Git Bash)
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WRAPPER_DIR="${SCRIPT_DIR}/gradle/wrapper"
JAR_PATH="${WRAPPER_DIR}/gradle-wrapper.jar"

echo "╔════════════════════════════════════════════════════════════╗"
echo "║   Gradle Wrapper Jar 安装器 (Gradle 9.3.1)                ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# 确保目录存在
mkdir -p "${WRAPPER_DIR}"

# ========== 方法 1: 从用户指定的路径复制 ==========
echo "📋 方法 1: 从本地已有文件复制"
echo "────────────────────────────────────────"
echo "如果你已经从 Local Dream 或其他项目提取了 gradle-wrapper.jar，"
echo "请把它放到以下任一位置，然后重新运行此脚本:"
echo ""
echo "  ${SCRIPT_DIR}/gradle-wrapper.jar"
echo "  ${SCRIPT_DIR}/so-input/gradle-wrapper.jar"
echo "  ${SCRIPT_DIR}/input/gradle-wrapper.jar"
echo "  /sdcard/Download/gradle-wrapper.jar"
echo ""
if [ -f "${SCRIPT_DIR}/gradle-wrapper.jar" ]; then
    cp "${SCRIPT_DIR}/gradle-wrapper.jar" "${JAR_PATH}"
    echo "✅ 从项目根目录复制成功"
elif [ -f "${SCRIPT_DIR}/so-input/gradle-wrapper.jar" ]; then
    cp "${SCRIPT_DIR}/so-input/gradle-wrapper.jar" "${JAR_PATH}"
    echo "✅ 从 so-input/ 复制成功"
elif [ -f "${SCRIPT_DIR}/input/gradle-wrapper.jar" ]; then
    cp "${SCRIPT_DIR}/input/gradle-wrapper.jar" "${JAR_PATH}"
    echo "✅ 从 input/ 复制成功"
elif [ -f "/sdcard/Download/gradle-wrapper.jar" ]; then
    cp "/sdcard/Download/gradle-wrapper.jar" "${JAR_PATH}"
    echo "✅ 从 /sdcard/Download/ 复制成功"
fi

# 检查是否成功
if [ -f "${JAR_PATH}" ]; then
    SIZE=$(stat -c%s "${JAR_PATH}" 2>/dev/null || stat -f%z "${JAR_PATH}" 2>/dev/null)
    if [ "$SIZE" -gt 10000 ]; then
        echo ""
        echo "✅✅✅ gradle-wrapper.jar 就绪! (${SIZE} bytes)"
        echo "   路径: ${JAR_PATH}"
        echo ""
        echo "🚀 下一步:"
        echo "   ./gradlew --version       # 验证"
        echo "   ./gradlew assembleDebug   # 编译"
        echo ""
        exit 0
    fi
fi

# ========== 方法 2: 从 Gradle 官方下载 ==========
echo ""
echo "📋 方法 2: 从 Gradle 官方下载"
echo "────────────────────────────────────────"

# 先尝试下载完整的 Gradle 发行包（wrapper jar 在里面）
TMP_DIR=$(mktemp -d)
GRADLE_ZIP="${TMP_DIR}/gradle-9.3.1-bin.zip"

echo "📥 下载 Gradle 9.3.1 完整包 (约 200MB，耐心等待)..."
echo "   URL: https://services.gradle.org/distributions/gradle-9.3.1-bin.zip"
echo ""

DL_SUCCESS=0
if command -v wget >/dev/null 2>&1; then
    wget -q --show-progress -O "${GRADLE_ZIP}" \
        "https://services.gradle.org/distributions/gradle-9.3.1-bin.zip" && DL_SUCCESS=1 || DL_SUCCESS=0
elif command -v curl >/dev/null 2>&1; then
    curl -L --progress-bar -o "${GRADLE_ZIP}" \
        "https://services.gradle.org/distributions/gradle-9.3.1-bin.zip" && DL_SUCCESS=1 || DL_SUCCESS=0
fi

if [ "$DL_SUCCESS" -eq 1 ] && [ -s "${GRADLE_ZIP}" ]; then
    ZIP_SIZE=$(stat -c%s "${GRADLE_ZIP}" 2>/dev/null || stat -f%z "${GRADLE_ZIP}" 2>/dev/null)
    echo ""
    echo "✅ 下载完成 (${ZIP_SIZE} bytes)"

    # 解压
    echo "📂 解压中..."
    unzip -q "${GRADLE_ZIP}" -d "${TMP_DIR}"

    # 查找 wrapper jar
    FOUND_JAR=$(find "${TMP_DIR}" -name "gradle-wrapper.jar" -type f | head -1)
    if [ -n "${FOUND_JAR}" ]; then
        cp "${FOUND_JAR}" "${JAR_PATH}"
        FINAL_SIZE=$(stat -c%s "${JAR_PATH}" 2>/dev/null || stat -f%z "${JAR_PATH}" 2>/dev/null)
        echo "✅ gradle-wrapper.jar 已提取 (${FINAL_SIZE} bytes)"
        rm -rf "${TMP_DIR}"
        echo ""
        echo "🚀 下一步:"
        echo "   ./gradlew --version       # 验证"
        echo "   ./gradlew assembleDebug   # 编译"
        exit 0
    fi
    rm -rf "${TMP_DIR}"
fi

# ========== 方法 3: 从 GitHub 下载 ==========
echo ""
echo "📋 方法 3: 从 GitHub 下载 (ghproxy 加速)"
echo "────────────────────────────────────────"

# 尝试多个 GitHub 加速镜像
MIRRORS=(
    "https://raw.githubusercontent.com/gradle/gradle/v9.3.1/gradle/wrapper/gradle-wrapper.jar"
    "https://ghproxy.cc/https://raw.githubusercontent.com/gradle/gradle/v9.3.1/gradle/wrapper/gradle-wrapper.jar"
    "https://gh-proxy.com/https://raw.githubusercontent.com/gradle/gradle/v9.3.1/gradle/wrapper/gradle-wrapper.jar"
    "https://ghproxy.cn/https://raw.githubusercontent.com/gradle/gradle/v9.3.1/gradle/wrapper/gradle-wrapper.jar"
)

for url in "${MIRRORS[@]}"; do
    echo "  尝试: ${url:0:60}..."
    if command -v wget >/dev/null 2>&1; then
        wget -q --timeout=20 -O "${JAR_PATH}" "${url}" 2>/dev/null && break
    elif command -v curl >/dev/null 2>&1; then
        curl -L --max-time 20 -o "${JAR_PATH}" "${url}" 2>/dev/null && break
    fi
done

if [ -f "${JAR_PATH}" ]; then
    SIZE=$(stat -c%s "${JAR_PATH}" 2>/dev/null || stat -f%z "${JAR_PATH}" 2>/dev/null)
    if [ "$SIZE" -gt 10000 ]; then
        echo "✅ 下载成功 (${SIZE} bytes)"
        echo ""
        echo "🚀 下一步:"
        echo "   ./gradlew --version       # 验证"
        echo "   ./gradlew assembleDebug   # 编译"
        exit 0
    fi
fi

# ========== 全部失败，给出手动方案 ==========
echo ""
echo "════════════════════════════════════════════════════════════"
echo "❌ 自动下载全部失败（网络限制）"
echo ""
echo "🔧 手动方案（你在手机上操作）:"
echo ""
echo "方案 A: 从 Local Dream 项目提取（你已经有那个 zip 了）"
echo "  1. 用 MT 管理器打开 Local Dream 的 zip 包"
echo "  2. 导航到: gradle/wrapper/gradle-wrapper.jar"
echo "  3. 长按 → 提取 → 保存到: ${SCRIPT_DIR}/"
echo "  4. 重命名为 'gradle-wrapper.jar'（如果名字不对）"
echo "  5. 再运行: bash install-wrapper-jar.sh"
echo ""
echo "方案 B: 从任何 Android 项目复制"
echo "  任何用 Gradle 构建的 Android 项目都有这个文件"
echo "  路径都是: 项目根目录/gradle/wrapper/gradle-wrapper.jar"
echo "  复制过来就行，Gradle 9.x 的 wrapper jar 都通用"
echo ""
echo "方案 C: 电脑上下载再传手机"
echo "  1. 电脑浏览器打开:"
echo "     https://services.gradle.org/distributions/gradle-9.3.1-bin.zip"
echo "  2. 下载后解压，找到 lib/plugins/ 目录下的 wrapper"
echo "  3. 或直接搜索 gradle-wrapper.jar"
echo "  4. 传到手机，放到: ${SCRIPT_DIR}/gradle/wrapper/"
echo ""
echo "════════════════════════════════════════════════════════════"
exit 1

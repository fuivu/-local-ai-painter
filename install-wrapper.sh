#!/bin/bash
# ============================================================
#  install-wrapper.sh
#  自动安装 gradle-wrapper.jar + 验证 Gradle 9.3.1 环境
#  适用于 Termux / Linux / macOS
# ============================================================

set -e

echo "============================================"
echo "  Local AI Painter v3.7 — Wrapper Installer"
echo "============================================"
echo ""

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_DIR="$PROJECT_DIR/gradle/wrapper"
JAR_PATH="$WRAPPER_DIR/gradle-wrapper.jar"
PROPS_PATH="$WRAPPER_DIR/gradle-wrapper.properties"

# 1. 检查 gradle-wrapper.properties
echo "📋 [1/4] 检查 gradle-wrapper.properties..."
if [ -f "$PROPS_PATH" ]; then
    echo "   ✅ 已存在: $PROPS_PATH"
    cat "$PROPS_PATH"
    echo ""
else
    echo "   ❌ 不存在，创建默认配置..."
    mkdir -p "$WRAPPER_DIR"
    cat > "$PROPS_PATH" << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.3.1-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF
    echo "   ✅ 已创建"
fi

# 2. 检查 gradle-wrapper.jar
echo ""
echo "📦 [2/4] 检查 gradle-wrapper.jar..."
if [ -f "$JAR_PATH" ] && [ -s "$JAR_PATH" ]; then
    SIZE=$(stat -c%s "$JAR_PATH" 2>/dev/null || stat -f%z "$JAR_PATH" 2>/dev/null)
    echo "   ✅ 已存在: $JAR_PATH (${SIZE} bytes)"
else
    echo "   ⚠️  不存在或为空，尝试自动下载..."
    
    # 方法 A：从 Gradle 9.3.1 发行包中提取
    TMP_DIR=$(mktemp -d)
    ZIP_PATH="$TMP_DIR/gradle-9.3.1-bin.zip"
    
    echo "   📥 下载 Gradle 9.3.1 (约 140MB)..."
    if command -v curl >/dev/null 2>&1; then
        curl -L -o "$ZIP_PATH" "https://services.gradle.org/distributions/gradle-9.3.1-bin.zip" || true
    elif command -v wget >/dev/null 2>&1; then
        wget -O "$ZIP_PATH" "https://services.gradle.org/distributions/gradle-9.3.1-bin.zip" || true
    fi
    
    if [ -f "$ZIP_PATH" ] && [ -s "$ZIP_PATH" ]; then
        echo "   📂 解压提取 gradle-wrapper.jar..."
        unzip -o "$ZIP_PATH" "gradle-9.3.1/lib/plugins/gradle-wrapper-*.jar" -d "$TMP_DIR/" 2>/dev/null || true
        
        # 从 wrapper jar 中提取 gradle-wrapper.jar
        WRAPPER_PLUGIN=$(find "$TMP_DIR" -name "gradle-wrapper-*.jar" | head -1)
        if [ -n "$WRAPPER_PLUGIN" ]; then
            mkdir -p "$TMP_DIR/extract"
            unzip -o "$WRAPPER_PLUGIN" "gradle-wrapper.jar" -d "$TMP_DIR/extract/" 2>/dev/null || true
            if [ -f "$TMP_DIR/extract/gradle-wrapper.jar" ]; then
                cp "$TMP_DIR/extract/gradle-wrapper.jar" "$JAR_PATH"
                echo "   ✅ 提取成功: $JAR_PATH"
            fi
        fi
    fi
    
    # 方法 B：如果从发行包提取失败，尝试直接下载（备用）
    if [ ! -f "$JAR_PATH" ] || [ ! -s "$JAR_PATH" ]; then
        echo "   📥 备用方案：直接下载 gradle-wrapper.jar..."
        # 从 GitHub 上的 Gradle 源码仓库获取
        GITHUB_URL="https://raw.githubusercontent.com/gradle/gradle/v9.3.1/gradle/wrapper/gradle-wrapper.jar"
        if command -v curl >/dev/null 2>&1; then
            curl -L -o "$JAR_PATH" "$GITHUB_URL" 2>/dev/null || true
        elif command -v wget >/dev/null 2>&1; then
            wget -O "$JAR_PATH" "$GITHUB_URL" 2>/dev/null || true
        fi
    fi
    
    # 清理临时文件
    rm -rf "$TMP_DIR"
    
    if [ -f "$JAR_PATH" ] && [ -s "$JAR_PATH" ]; then
        SIZE=$(stat -c%s "$JAR_PATH" 2>/dev/null || stat -f%z "$JAR_PATH" 2>/dev/null)
        echo "   ✅ gradle-wrapper.jar 就绪 (${SIZE} bytes)"
    else
        echo "   ❌ 自动下载失败，请手动放置文件："
        echo "      将 gradle-wrapper.jar 复制到:"
        echo "      $JAR_PATH"
        echo ""
        echo "   手动获取方式："
        echo "   1. 从任何 Gradle 9.x 项目的 gradle/wrapper/ 目录复制"
        echo "   2. 或从 Local Dream 项目的同一位置复制"
        echo "   3. 或从电脑上运行: gradle wrapper --gradle-version 9.3.1"
    fi
fi

# 3. 检查 gradlew
echo ""
echo "🔧 [3/4] 检查 gradlew..."
if [ -x "$PROJECT_DIR/gradlew" ]; then
    echo "   ✅ 已存在且可执行"
else
    echo "   ⚠️  不存在或不可执行"
    if [ -f "$PROJECT_DIR/gradlew" ]; then
        chmod +x "$PROJECT_DIR/gradlew"
        echo "   ✅ 已赋予执行权限"
    else
        echo "   ❌ gradlew 脚本不存在，请确认项目完整"
    fi
fi

# 4. 验证环境
echo ""
echo "🔍 [4/4] 验证环境..."
echo "   Java:"
if command -v java >/dev/null 2>&1; then
    java -version 2>&1 | head -1
else
    echo "   ❌ Java 未安装（需要 JDK 17+）"
    echo "   Termux 安装: pkg install openjdk-17"
fi

echo ""
echo "   Gradle (如有):"
if command -v gradle >/dev/null 2>&1; then
    gradle --version 2>&1 | head -3
else
    echo "   ℹ️  系统未安装 Gradle（wrapper 会自动下载）"
fi

echo ""
echo "============================================"
echo "  安装检查完成！"
echo ""
echo "  下一步："
echo "    ./gradlew --version        # 验证 wrapper"
echo "    ./gradlew assembleDebug    # 编译 Debug APK"
echo "    ./gradlew assembleRelease  # 编译 Release APK"
echo "============================================"

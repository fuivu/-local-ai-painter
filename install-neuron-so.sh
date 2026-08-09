#!/bin/bash
# ══════════════════════════════════════════════════════════
#  install-neuron-so.sh
#  将 MediaTek NeuroPilot SDK 的 .so 文件复制到正确位置
#
#  用法：
#    1. 从 MediaTek NeuroPilot SDK 包中解压出以下 .so：
#         libneuronusdk_adapter.mtk.so
#         libneuron_runtime.so
#         libneuron_vpu.so
#         libmtkneuron_runtime.so
#    2. 把这些 .so 放到本脚本同目录的  so-input/  文件夹
#    3. 运行：bash install-neuron-so.sh
#
#  脚本会自动把 .so 复制到：
#     app/src/main/jniLibs/arm64-v8a/
#  Gradle 构建时会自动打包进 APK 的 lib/arm64-v8a/
# ══════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SO_INPUT="$SCRIPT_DIR/so-input"
SO_OUTPUT="$SCRIPT_DIR/app/src/main/jniLibs/arm64-v8a"

echo "═══ Local AI Painter v3.5 — NeuroPilot .so 安装 ═══"

# 检查输入目录
if [ ! -d "$SO_INPUT" ]; then
    echo "❌ 请将 .so 文件放入: $SO_INPUT/"
    echo ""
    echo "需要的文件（从 MediaTek NeuroPilot SDK 获取）："
    echo "  • libneuronusdk_adapter.mtk.so"
    echo "  • libneuron_runtime.so"
    echo "  • libneuron_vpu.so"
    echo "  • libmtkneuron_runtime.so"
    echo ""
    echo "放入后重新运行：bash install-neuron-so.sh"
    exit 1
fi

# 创建输出目录
mkdir -p "$SO_OUTPUT"

# 复制 .so 文件
count=0
for so in "$SO_INPUT"/*.so; do
    [ -f "$so" ] || continue
    cp -v "$so" "$SO_OUTPUT/"
    count=$((count + 1))
done

if [ "$count" -eq 0 ]; then
    echo "❌ so-input/ 目录中没有找到 .so 文件"
    exit 1
fi

echo ""
echo "✅ 已复制 $count 个 .so 文件到:"
echo "   $SO_OUTPUT/"
echo ""
echo "现在可以构建 APK 了："
echo "  ./build-apk.sh debug      # 调试版"
echo "  ./build-apk.sh release    # 正式版（混淆+压缩）"
echo ""
echo "════════════════════════════════════════════"

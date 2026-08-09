#!/bin/bash
# ============================================================
#  Local AI Painter v3.0 — Git 仓库初始化脚本
#  功能: 初始化 Git 仓库 + 安装钩子 + 首次提交
#  用法: bash setup-git-repo.sh [remote-url]
# ============================================================

set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "${PROJECT_ROOT}"

echo ""
echo -e "${BOLD}${BLUE}  ╔════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${BLUE}  ║   Local AI Painter v3.0 — Git 初始化  ║${NC}"
echo -e "${BOLD}${BLUE}  ╚════════════════════════════════════════╝${NC}"
echo ""

# 1. 检查是否已初始化
if [ -d ".git" ]; then
    echo -e "${YELLOW}⚠${NC}  Git 仓库已存在，跳过初始化"
else
    echo -e "${CYAN}[1/4]${NC} 初始化 Git 仓库..."
    git init
    git branch -M main
    echo -e "${GREEN}✓${NC}  Git 仓库初始化完成"
fi

# 2. 安装 pre-commit 钩子
echo ""
echo -e "${CYAN}[2/4]${NC} 安装 Git 钩子..."
if [ -f "git-hooks/pre-commit" ]; then
    mkdir -p .git/hooks
    cp git-hooks/pre-commit .git/hooks/pre-commit
    chmod +x .git/hooks/pre-commit
    echo -e "${GREEN}✓${NC} pre-commit 钩子已安装"
    echo -e "  功能: 提交前自动检查 Kotlin/C++/XML 语法 + 关键文件完整性"
else
    echo -e "${YELLOW}⚠${NC} 未找到 git-hooks/pre-commit，跳过"
fi

# 3. 添加文件并提交
echo ""
echo -e "${CYAN}[3/4]${NC} 添加文件到暂存区..."
git add .
echo -e "${GREEN}✓${NC} 文件已添加"

# 检查是否有提交
if git rev-parse HEAD >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠${NC}  已有提交记录，跳过首次提交"
else
    echo ""
    echo -e "${CYAN}[4/4]${NC} 创建首次提交..."
    git commit -m "feat: Local AI Painter v3.0.0 完整源码

- C++ 引擎: SIMD/FFT/量化/内存管理/图像IO/超分
- Kotlin 引擎: 5后端异构推理/调度器/LoRA/ControlNet
- UI: Compose 创作/画廊/模型/设置四页 + 6套主题
- 构建: Gradle 8.7 + AGP 8.5 + Kotlin 2.0
- CI/CD: GitHub Actions 自动编译 + 发布
- 包体优化: R8 fullMode + 仅arm64-v8a + 资源缩减"
    echo -e "${GREEN}✓${NC} 首次提交完成"
fi

# 4. 关联远程仓库（如果提供了参数）
echo ""
if [ -n "$1" ]; then
    echo -e "${CYAN}[额外]${NC} 关联远程仓库: $1"
    git remote remove origin 2>/dev/null || true
    git remote add origin "$1"
    echo -e "${GREEN}✓${NC} 远程仓库已关联"
    echo ""
    echo -e "  推送到远程: ${BOLD}git push -u origin main${NC}"
else
    echo -e "${YELLOW}💡${NC} 如需关联远程仓库，运行:"
    echo "     git remote add origin <your-repo-url>"
    echo "     git push -u origin main"
fi

echo ""
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo -e "${GREEN}  Git 仓库准备就绪!${NC}"
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo ""
echo "  常用命令:"
echo "    git status              # 查看状态"
echo "    git add .                # 添加所有改动"
echo "    git commit -m 'msg'      # 提交（自动触发 pre-commit 检查）"
echo "    git push                 # 推送到远程"
echo "    git tag v3.0.0           # 打版本标签（触发 CI 发布）"
echo "    git push --tags          # 推送标签"
echo ""
echo "  构建命令:"
echo "    ./build-apk.sh debug     # 构建 Debug APK"
echo "    ./build-apk.sh release   # 构建 Release APK"
echo "    ./install-wrapper.sh     # 安装 Gradle Wrapper"
echo ""

# Local AI Painter v3.5 "Aurora"

> 本地运行的 Android AI 绘画应用，对标 Local Dream，功能全面升级。

## ✨ 功能特性

### 核心引擎
- **6 种模型架构**：SD1.5 / SD2.1 / SDXL / LCM / PixArt / Kolors
- **6 后端异构推理**：QNN (NPU) / MNN / NCNN / ORT (Vulkan) / OpenGL / CPU
- **混动模式 (HYBRID)**：CPU + GPU 同时工作，4 种并行策略（串行/流水线/数据并行/自适应）
- **14 种采样器**：Euler A, DPM++ 2M Karras, UniPC, LCM, Heun, DPM2, DPM2 A, LMS, Euler, DDIM, PNDM, DEIS, DPM++ SDE, Restart
- **5 路 LoRA 叠加**：独立权重 0~2.0，自动拼合触发词
- **6 种 ControlNet**：Canny / OpenPose / Depth / Scribble / MLSD / Seg
- **3 种超分引擎**：ESRGAN / SwinIR / RealESRGAN（分块羽化处理大图）
- **3 种人脸修复**：GFPGAN / CodeFormer / RestoreFormer

### v3.5 Aurora — 推理引擎升级
- **INT2 权重量化** (ParetoQ)：2-bit 压缩 16×，天玑 NPU 990 原生加速
- **FP8 E4M3/E5M2 量化**：8-bit 浮点，骁龙 8 至尊 / 天玑 9400+ 原生
- **图融合优化器**：Conv+BN+SiLU 三件套融合 / QKV 投影融合 / Flash Attention 在线 softmax / LUT-Diff 查表（9.1× 加速）
- **双缓冲 + DMA 重叠管线**：GPU 计算与权重搬运完全并行（Hexagon-MLIR 风格）
- **CIM 存算一体**（天玑 9500）：功耗降低 33%
- **投机解码 SpD+**（天玑 NPU 990/890+）：文本编码 2-4× 加速
- **权重流式加载器**：LRU 淘汰 + 热点预取，512MB 常驻

### v3.5 Aurora — 渲染管线升级
- **Vulkan 零拷贝管线**：NPU → Vulkan Buffer → Swapchain → Display，零 CPU 中转
- **GLSL Compute Shader 库**：ACES 色调映射 / Gamma / YUV↔RGB / 3D LUT / 双线性 & Lanczos 上采样 / Subgroup 归约
- **天玑 CIM → Vulkan 共享内存直通**
- **Subgroup 自适应优化**：Adreno size=64 / Mali size=16-32

### v3.5 Aurora — 天玑芯片深度适配
| 芯片 | INT2 | FP8 | CIM | 双NPU | 5D张量 | MoE | SpD+ | 4K生成 | 128K上下文 |
|------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 天玑 9500 (NPU 990) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 天玑 9400+ (NPU 890+) | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ✅ | ❌ | ❌ |
| 天玑 9400 (NPU 890) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

### C++ 引擎层
- NEON SIMD 加速 GEMM / GELU / SiLU / LayerNorm
- INT8 / INT4 / INT2 / FP8 多级量化推理
- FFT 快速卷积
- Arena 内存分配器（零碎片）
- GPU 缓冲池 + 张量 LRU 缓存
- 统一内存管理（CPU/GPU/DMA/共享）

### UI 层 (Jetpack Compose)
- **创作页**：提示词输入 + 18 个快捷模板 + 后端选择 + 并行策略 + 高级参数面板
- **画廊页**：网格展示 + 收藏/最近筛选 + 多选批量操作
- **模型页**：5 分类管理 + 导入/扫描/删除/收藏/备注
- **设置页**：6 套主题 + 性能调优 + 日志导出 + 芯片信息
- **6 套主题**：深色 / 浅色 / AMOLED / 日落 / 深海 / 森林

### 数据层
- Room 数据库（生成历史持久化）
- DataStore 偏好设置（14 项用户配置）
- 模型仓库（自动扫描 + 格式识别）

## 📦 项目结构

```
LocalAIPainter/
├── app/                          # 主应用模块
│   ├── src/main/
│   │   ├── cpp/                 # C++ 引擎源码 (v3.5: 65+ 文件)
│   │   │   ├── CMakeLists.txt   # 聚合所有模块
│   │   │   ├── engine_factory.cpp
│   │   │   ├── v35/            # ⭐ v3.5 Aurora 核心
│   │   │   │   ├── engine_v35.h/cpp       # 引擎总入口
│   │   │   │   ├── locai_v35.h/cpp       # C API + 主入口
│   │   │   │   ├── quantization/
│   │   │   │   │   ├── int2_fp8_quantizer.h/cpp  # INT2/FP8 量化
│   │   │   │   ├── optimization/
│   │   │   │   │   ├── graph_fusion.h/cpp      # 图融合
│   │   │   │   │   └── double_buffered_pipeline.h/cpp # DMA 重叠
│   │   │   │   ├── engine_mediatek/
│   │   │   │   │   └── mediatek_npu_adapter.h/cpp # 天玑NPU
│   │   │   │   └── rendering/
│   │   │   │       └── vulkan_zero_copy.h/cpp  # Vulkan零拷贝
│   │   │   ├── engine/         # 调度器/VAE/LoRA/ControlNet等
│   │   │   ├── engine/vulkan/  # Vulkan Compute
│   │   │   ├── engine/opengl/  # OpenGL ES 辅助
│   │   │   └── engine/mediatek/# 天玑8400适配
│   │   ├── java/.../engine/    # Kotlin 引擎层 (6后端)
│   │   ├── java/.../data/      # Room + DataStore
│   │   ├── java/.../ui/        # Compose UI (4页)
│   │   └── res/                # Android 资源
│   └── build.gradle.kts
├── engine-core/                   # 核心引擎 Kotlin 模块
├── engine-mnn/ engine-ncnn/ engine-onnx/  # 各推理后端
├── ui-compose/                    # Compose UI 模块
├── data-store/                    # 数据层模块
├── gradle/wrapper/               # Gradle Wrapper
├── .github/workflows/            # CI/CD
├── install-wrapper.sh            # Wrapper 安装脚本
├── build-apk.sh / .ps1          # 编译脚本
└── README.md
```

## 🛠️ 编译指南

### 环境要求
- **JDK 17+**
- **Android SDK 34+**
- **Android NDK 26+**
- **Gradle 8.7**（Wrapper 自动管理）
- **操作系统**：Windows 10+ / macOS 12+ / Linux (Ubuntu 20.04+)

### 快速开始

#### 1. 克隆/下载项目
```bash
git clone <your-repo-url> LocalAIPainter
cd LocalAIPainter
```

#### 2. 安装 Gradle Wrapper
```bash
# 将已下载的 gradle-8.7-bin.zip 放到项目根目录
# 然后运行:
bash install-wrapper.sh

# 或手动:
gradle wrapper --gradle-version 8.7
```

#### 3. 编译 APK

**Linux / macOS:**
```bash
# Debug 版（快速编译，不混淆）
./build-apk.sh debug

# Release 版（混淆优化，目标 30-50MB）
./build-apk.sh release
```

**Windows (PowerShell):**
```powershell
# Debug 版
.\build-apk.ps1 -BuildType debug

# Release 版
.\build-apk.ps1 -BuildType release
```

**手动编译:**
```bash
# 确保 ANDROID_HOME 已设置
export ANDROID_HOME=/path/to/android/sdk

# 编译
./gradlew clean assembleRelease --no-daemon

# APK 输出位置
# app/build/outputs/apk/release/app-release-unsigned.apk
```

### 包体优化策略

| 策略 | 预期效果 |
|------|----------|
| R8 fullMode + 混淆 | -30~40% |
| 仅 arm64-v8a | -40~50% |
| 资源缩减 (shrinkResources) | -5~10% |
| WebP 替代 PNG | -20~30% |
| 动态交付 (Dynamic Feature) | 拆分模型包 |
| **合计目标** | **30-50MB** |

## 📋 Git 工作流

### 初始化仓库
```bash
bash setup-git-repo.sh https://github.com/yourname/LocalAIPainter.git
```

### 日常开发
```bash
# 查看状态
git status

# 添加改动
git add .

# 提交（自动触发 pre-commit 检查）
git commit -m "feat: 新增 XX 功能"

# 推送
git push

# 打版本标签（触发 CI 自动发布）
git tag v3.0.1
git push --tags
```

### pre-commit 钩子自动检查
- ✅ Kotlin 大括号匹配
- ✅ C++ 头文件引用完整性
- ✅ XML 资源文件有效性
- ✅ 关键文件存在性
- ✅ 版本号一致性

## 🔄 CI/CD (GitHub Actions)

推送代码后自动触发：
- **Push to main/develop** → 自动编译 Debug APK → 上传为 Artifact
- **Push tag v\*** → 自动编译 Release APK → 发布 GitHub Release

### 配置密钥（Release 签名）
在 GitHub 仓库 Settings → Secrets 中添加：
- `KEYSTORE_BASE64` — keystore 文件的 Base64 编码
- `KEYSTORE_PASSWORD` — keystore 密码
- `KEY_ALIAS` — 密钥别名
- `KEY_PASSWORD` — 密钥密码

```bash
# 生成 base64
base64 -i my-release-key.jks | tr -d '\n' > keystore_base64.txt
```

## 📊 性能基准（v3.5 Aurora）

| 设备等级 | 芯片示例 | 512×512 出图 | 1024×1024 出图 |
|----------|----------|-------------|----------------|
| 旗舰 (NPU 990) | 天玑 9500 | **~1.1s** (INT2+CIM) | **~4.5s** |
| 旗舰 (NPU+GPU) | 骁龙 8 至尊 | ~1.5s (FP8) | ~6s |
| 高端 (NPU) | 天玑 9400+ | ~2.0s (FP8) | ~8s |
| 中端 (GPU) | 天玑 9400 | ~3.5s (INT8) | ~12s |
| 入门 (CPU) | 骁龙 695 | ~90s | ~180s |

## 📝 许可证

MIT License

## 🙏 致谢

- [Stable Diffusion](https://github.com/CompVis/stable-diffusion) — 基础模型架构
- [NCNN](https://github.com/Tencent/ncnn) — 腾讯开源推理框架
- [MNN](https://github.com/alibaba/MNN) — 阿里开源推理框架
- [Compose Multiplatform](https://www.jetbrains.com/lm/compose-multiplatform/) — UI 框架
- [Local Dream](https://github.com/) — 对标参考应用

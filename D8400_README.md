# Dimensity 8400 Adapter — 使用说明

## 快速开始

### 1. 把 NeuroPilot SDK 的 .so 放进 `so-input/`

从 MediaTek NeuroPilot SDK 中提取以下文件，放到 `so-input/` 目录：

| 文件名 | 作用 |
|--------|------|
| `libneuronusdk_adapter.mtk.so` | NeuroPilot 主适配库 |
| `libneuron_runtime.so` | NPU 运行时 |
| `libneuron_vpu.so` | VPU 驱动 |
| `libmtkneuron_runtime.so` | 备选运行时 |

### 2. 运行安装脚本

```bash
bash install-neuron-so.sh
```

脚本会自动把 .so 复制到 `app/src/main/jniLibs/arm64-v8a/`，Gradle 构建时会自动打包进 APK。

### 3. 构建 APK

```bash
./build-apk.sh debug      # 调试版
./build-apk.sh release    # 正式版（混淆+压缩，目标 30-50MB）
```

---

## 架构设计

```
┌─────────────────────────────────────────────────────┐
│  Kotlin 层                                        │
│  ┌───────────────────────────────────────────────┐  │
│  │ Dimensity8400Bridge (object, JNI 桥接)       │  │
│  │ Dimensity8400Engine  (BaseEngine 子类)       │  │
│  │ DataClasses (ChipCapabilities, SessionConfig…) │  │
│  └────────────────┬──────────────────────────────┘  │
│                   │ JNI                             │
├───────────────────┼─────────────────────────────────┤
│  C++ 层          ▼                                 │
│  ┌───────────────────────────────────────────────┐  │
│  │ JniDimensity8400.cpp (JNI 入口)              │  │
│  │ Dimensity8400Adapter.cpp (软逻辑 + dlopen)   │  │
│  │ Dimensity8400Adapter.h (接口 + 数据结构)     │  │
│  └────────────────┬──────────────────────────────┘  │
│                   │ dlopen/dlsym                   │
├───────────────────┼─────────────────────────────────┤
│  NeuroPilot SDK   ▼                                 │
│  ┌───────────────────────────────────────────────┐  │
│  │ libneuronusdk_adapter.mtk.so  (你提供)       │  │
│  │ libneuron_runtime.so         (你提供)         │  │
│  │ libneuron_vpu.so             (你提供)         │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

---

## STUB 模式（没有 .so 时）

如果你暂时没有 NeuroPilot SDK，项目照样能编译运行：

- `dlopen` 失败 → 进入 STUB 模式
- `Dimensity8400Bridge.isAvailable()` 返回 `false`
- 引擎自动降级到 **Vulkan → OpenCL → CPU**
- 所有 JNI 调用包了 `try-catch UnsatisfiedLinkError`，**绝不崩溃**
- 日志显示 `⚠️ STUB MODE — running in degraded mode`

---

## 天玑 8400 关键规格（自动探测）

| 参数 | 值 |
|------|-----|
| SoC | MT6899 / MT6899Z |
| 制程 | TSMC N4P (4nm) |
| CPU | 8× Cortex-A725 全大核 (1×3.25G + 3×3.0G + 4×2.1G) |
| GPU | Mali-G720 MP7 @ 1.3GHz, 896 ALU, ~2330 GFLOPS |
| NPU | NPU 880 (第8代), INT4 混合精度 |
| 内存 | LPDDR5X @ 8533Mbps, 68.2 GB/s |
| 缓存 | 6MB L3 + 5MB SLC（比上代翻倍）|
| 特性 | DiT ✓ | DAE ✓ | INT4 ✓ | UFS 4.0 ✓ |

---

## 推荐推理配置（自动计算）

| 模型角色 | 精度 | 线程 | 内存池 | 说明 |
|----------|------|------|--------|------|
| TEXT_ENCODER | INT8 | 2 | 256MB | 文本编码 INT8 足够 |
| UNET | **INT4** | 4 | 1536MB | 权重最大，INT4 省 4× 内存 |
| VAE_DECODER | FP16 | 2 | 512MB | VAE 对精度敏感 |
| ESRGAN | FP16 | 4 | 1024MB | 超分需要精度 |

---

## 性能预估（NPU 880 + INT8）

| 模型 | 分辨率 | 步数 | 预估耗时 |
|------|--------|------|----------|
| SD 1.5 | 512² | 20 | **~2.8s** |
| SD 1.5 | 512² | 8 (LCM) | **~1.1s** |
| SD 1.5 | 768² | 20 | ~6.3s |
| SDXL | 1024² | 20 | ~12s (需分块) |

> 官方数据：NPU 880 比上代 +54% NPU 性能，SD1.5 提速 21%

---

## 文件清单

```
app/src/main/cpp/engine/mediatek/
├── Dimensity8400Adapter.h       ← 接口 + 数据结构 + 量化工具声明
├── Dimensity8400Adapter.cpp     ← 实现 + dlopen 管理 + 量化工具
└── JniDimensity8400.cpp        ← JNI 桥接（Kotlin ↔ C++）

app/src/main/java/.../engine/mediatek/
├── Dimensity8400Bridge.kt      ← Kotlin JNI 桥接器
└── DataClasses.kt              ← ChipCapabilities / SessionConfig / 枚举 / Engine

install-neuron-so.sh             ← .so 安装脚本
D8400_README.md                 ← 本文件
```

---

## 日志标签

| TAG | 来源 | 内容 |
|-----|------|------|
| `D8400` | C++ 适配层 | 初始化、能力探测、缓存配置 |
| `D8400-JNI` | JNI 层 | JNI 调用、dlopen 结果 |
| `D8400Bridge` | Kotlin 桥接 | Java 侧状态、降级通知 |
| `Engine/D8400` | Dimensity8400Engine | 引擎生命周期、预热 |

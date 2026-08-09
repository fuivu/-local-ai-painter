# 📱 手机端编译指南 (Termux)

## 前置条件
- 已安装 Termux（F-Droid 下载，不要用 Play Store 版）
- 已下载本项目的 zip 包
- 已从 Local Dream 项目提取出 `gradle-wrapper.jar`

---

## 第 1 步：安装 Termux 依赖

```bash
pkg update -y
pkg install -y openjdk-17 wget unzip git
```

等待安装完成（约 200MB）

---

## 第 2 步：解压项目

```bash
cd ~
mkdir -p projects
cd projects

# 把 LocalAIPainter-v3.7.zip 放到 Download 目录后
cp /sdcard/Download/LocalAIPainter-v3.7.zip .
unzip LocalAIPainter-v3.7.zip
cd LocalAIPainter
```

---

## 第 3 步：放入 gradle-wrapper.jar

> 这是最关键的一步！

### 方法 A：从 Local Dream 项目提取（推荐）

1. 打开 MT 管理器
2. 找到你下载的 Local Dream 项目 zip
3. 导航到 `gradle/wrapper/gradle-wrapper.jar`
4. 长按 → 提取 → 保存到 `/sdcard/Download/`
5. 回到 Termux：

```bash
cp /sdcard/Download/gradle-wrapper.jar ~/projects/LocalAIPainter/gradle/wrapper/
```

### 方法 B：用脚本自动下载

```bash
cd ~/projects/LocalAIPainter
bash install-wrapper-jar.sh
```

> 如果网络不通，脚本会提示你用方法 A

### 验证 jar 是否到位

```bash
ls -la gradle/wrapper/gradle-wrapper.jar
# 应该显示约 60000+ bytes（不是 0 或 22）
```

---

## 第 4 步：安装 Android SDK + NDK

```bash
# 安装 Android SDK 命令行工具
pkg install -y android-sdk android-ndk

# 或使用 sdkmanager 安装（更灵活）
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
mkdir -p ~/android-sdk/cmdline-tools
unzip commandlinetools-linux-11076708_latest.zip -d ~/android-sdk/cmdline-tools/
mv ~/android-sdk/cmdline-tools/cmdline-tools ~/android-sdk/cmdline-tools/latest

# 设置环境变量
echo 'export ANDROID_HOME=$HOME/android-sdk' >> ~/.bashrc
echo 'export ANDROID_SDK_ROOT=$HOME/android-sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.bashrc
source ~/.bashrc

# 安装必要的 SDK 包
sdkmanager "platforms;android-34" "build-tools;34.0.0" "ndk;26.1.10909125"
```

---

## 第 5 步：编译

```bash
cd ~/projects/LocalAIPainter

# 验证 Gradle 能启动
./gradlew --version

# 编译 Debug APK（快，约 5-15 分钟）
./gradlew assembleDebug

# 或编译 Release APK（慢，需要签名配置）
./gradlew assembleRelease
```

编译成功后：
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

---

## 第 6 步：安装到手机

```bash
# 安装 Debug 版
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/
```

然后在手机文件管理器里点击 APK 安装即可。

---

## 常见问题

### Q: `./gradlew: permission denied`
```bash
chmod +x gradlew
```

### Q: `gradle-wrapper.jar` 找不到
确认文件存在且大小正确：
```bash
ls -la gradle/wrapper/gradle-wrapper.jar
# 应该 > 10000 bytes
```

### Q: Java 版本不对
```bash
java -version
# 应该是 17.x.x
# 如果是 21+ 可能不兼容，需要降级
pkg install openjdk-17
```

### Q: NDK 找不到
```bash
echo 'export NDK_HOME=$ANDROID_HOME/ndk/26.1.10909125' >> ~/.bashrc
source ~/.bashrc
```

### Q: 内存不足（编译 OOM）
Termux 默认内存有限，创建 `gradle.properties`：
```bash
echo "org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8" >> gradle.properties
echo "org.gradle.parallel=true" >> gradle.properties
echo "org.gradle.caching=true" >> gradle.properties
```

---

## 天玑 8400 NPU 支持（可选）

如果想启用 NPU 加速：

1. 从 MediaTek NeuroPilot SDK 获取 `.so` 文件
2. 放入 `so-input/` 目录
3. 运行 `bash install-neuron-so.sh`
4. 重新编译

不放入 `.so` 也能正常编译和运行，只是走 CPU/GPU 路径。

---

## 快速检查清单

- [ ] Termux 已安装 openjdk-17
- [ ] Android SDK + NDK 已安装
- [ ] `gradle/wrapper/gradle-wrapper.jar` 存在且 > 10KB
- [ ] `gradlew` 有执行权限
- [ ] `gradle-wrapper.properties` 内容正确
- [ ] `./gradlew --version` 能正常输出

全部打勾后，运行 `./gradlew assembleDebug` 即可编译！

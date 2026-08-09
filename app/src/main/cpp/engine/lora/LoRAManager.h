#pragma once

#include <string>
#include <vector>
#include <unordered_map>
#include <memory>

namespace sd_engine {
namespace lora {

// 单个 LoRA 权重
struct LoRATensor {
    std::string name;
    std::vector<float> up;   // 上投影
    std::vector<float> down; // 下投影
    std::vector<int> shape_up;
    std::vector<int> shape_down;
};

// 一个 LoRA 模型（一组张量 + 权重）
struct LoRAModel {
    std::string name;
    float weight = 1.0f;
    std::unordered_map<std::string, LoRATensor> tensors;
};

// LoRA 管理器接口
class LoRAManager {
public:
    virtual ~LoRAManager() = default;

    // 加载一个 LoRA 文件（支持 .safetensors / .ckpt）
    virtual bool load(const std::string& path, float weight = 1.0f) = 0;

    // 卸载指定 LoRA
    virtual void unload(const std::string& name) = 0;

    // 设置某个 LoRA 的权重
    virtual void set_weight(const std::string& name, float weight) = 0;

    // 获取当前激活的 LoRA 列表
    virtual std::vector<std::string> list_active() const = 0;

    // 将所有激活 LoRA 合并到目标权重（供 UNet 调用）
    virtual void apply_to(std::unordered_map<std::string, std::vector<float>>& weights) = 0;

    // 工厂
    static std::unique_ptr<LoRAManager> create();
};

} // namespace lora
} // namespace sd_engine

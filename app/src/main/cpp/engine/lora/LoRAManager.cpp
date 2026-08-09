#include "engine/lora/LoRAManager.h"
#include <stdexcept>
#include <algorithm>

namespace sd_engine {
namespace lora {

class LoRAManagerImpl : public LoRAManager {
public:
    bool load(const std::string& path, float weight) override {
        LoRAModel model;
        model.name = path; // 简化：用路径做名字
        model.weight = weight;
        // TODO: 解析 safetensors 中的 lora_unet / lora_te 张量
        models_[path] = model;
        active_.push_back(path);
        return true;
    }

    void unload(const std::string& name) override {
        models_.erase(name);
        auto it = std::find(active_.begin(), active_.end(), name);
        if (it != active_.end()) active_.erase(it);
    }

    void set_weight(const std::string& name, float weight) override {
        auto it = models_.find(name);
        if (it != models_.end()) it->second.weight = weight;
    }

    std::vector<std::string> list_active() const override { return active_; }

    void apply_to(std::unordered_map<std::string, std::vector<float>>& weights) override {
        for (const auto& name : active_) {
            const auto& model = models_[name];
            for (const auto& kv : model.tensors) {
                const auto& t = kv.second;
                // W_new = W + weight * down * up
                // TODO: 真实矩阵乘累加
            }
        }
    }

private:
    std::unordered_map<std::string, LoRAModel> models_;
    std::vector<std::string> active_;
};

std::unique_ptr<LoRAManager> LoRAManager::create() {
    return std::unique_ptr<LoRAManager>(new LoRAManagerImpl());
}

} // namespace lora
} // namespace sd_engine

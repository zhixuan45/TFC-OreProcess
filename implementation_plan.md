# 允许整合包作者配置启用模板模组经典矿物处理流水线实施计划

## 背景与需求分析

当前模组默认采用现代精简流水线（原矿 -> 锤碎 -> 单级 Sluice 洗矿 -> 5 mB 矿粉 -> 25/100 mB 矿团 -> 熔炼），适合轻量化整合包。而在根目录的模板模组（`tfcorewashing`）中，包含了一套更为硬核、多级细化的经典矿物处理流水线，包含 4 档品位细分产出、Create 冲压与石磨、沉浸工业多级破碎、手推磨粉碎以及 TFC 淘金盘碎石淘矿等深度玩法。

本计划旨在设计一套配置化机制，允许整合包作者自由切换或启用模板模组的这套深度矿物处理流水线，同时不影响默认精简模式的正常体验。

## 需要用户评审的核心设计项

> [!IMPORTANT]
> **关于流水线模式的启用方式**
> 方案采用**配置文件开关 + 内置可选数据包**双轨制：
> 1. 在 `Config.java` 中提供 `processingPipelineMode`（选项：`STANDARD` 精简标准模式 / `TEMPLATE_CLASSIC` 模板经典扩展模式）。
> 2. 将模板模组的扩展配方集成进模组数据层，当模式设为 `TEMPLATE_CLASSIC` 时激活全套多级配方。
> 3. 整合包作者亦可直接在模组内置数据包列表中单独启用/禁用 `tfc_oreprocess:template_classic_pipeline`。

> [!NOTE]
> **品位细分产出规格**
> 当启用模板模组经典线路时，原矿锤碎与机器粉碎将遵循 TFC 的 4 档矿石品位生成碎矿：
> - `small`（小矿粒）：产出 1 份碎矿
> - `poor`（贫矿）：产出 3 份碎矿
> - `normal`（正常矿）：产出 5 份碎矿
> - `rich`（富矿）：产出 7 份碎矿

## 详细功能规划

### 1. 经典多级机械加工配方接入

当启用经典模式时，自动激活以下与主流科技模组联动的加工方式：
- **TFC 手推磨（Quern）**：将各品位原矿研磨为碎矿。
- **Create 机械动力**：
  - 石磨（Milling）与粉碎轮（Crushing）：原矿粉碎为碎矿与副产矿粉。
  - 动力压板序列组装（Sequenced Assembly Pressing）：将原矿多重冲压破碎为碎矿。
- **Immersive Engineering 沉浸工业**：
  - Crusher 粉碎机：按品位粉碎原矿，并有固定概率产生额外矿粉副产物。

### 2. TFC 淘金盘（Panning）碎石淘矿扩展

引入模板模组中对各岩石类型碎石（`gravel_<rock>`）的淘洗支持：
- 玩家手持淘金盘在水流中淘洗花岗岩、玄武岩、片岩等各类碎石，低概率产出对应的伴生碎矿、矿粒与沙子。
- 数据生成到 `tfc/panning/deposits/` 下，为整合包早期生存提供可选的沙金与淘矿玩法。

### 3. 配置系统扩展

在 [Config.java](file:///c:/Users/JuziD/IdeaProjects/TFC%20OreProcess/src/main/java/org/shengxi/TFCOreProcess/tfc_oreprocess/Config.java) 中新增流水线模式配置：
```java
public enum PipelineMode {
    STANDARD,          // 默认精简流线
    TEMPLATE_CLASSIC   // 模板模组经典多级扩展流水线
}
```
并在配方加载与数据包条件中与配置进行绑定。

## 文件与模块变动规划

1. **构建脚本与数据生成**
   - [MODIFY] [build.gradle](file:///c:/Users/JuziD/IdeaProjects/TFC%20OreProcess/build.gradle)：
     - 在构建期新增对 4 档品位（small/poor/normal/rich）原矿的锤碎与 Quern 研磨配方生成。
     - 增加 Create 石磨/粉碎轮及 IE Crusher 的品位分级配方生成。
     - 生成各类碎石的淘金盘 Panning Deposit 与战利品表。
2. **配置模块**
   - [MODIFY] [Config.java](file:///c:/Users/JuziD/IdeaProjects/TFC%20OreProcess/src/main/java/org/shengxi/TFCOreProcess/tfc_oreprocess/Config.java)：新增 `pipelineMode` 与品位倍率选项。
3. **配方条件与控制模块**
   - [NEW] `src/main/java/org/shengxi/TFCOreProcess/tfc_oreprocess/recipe/PipelineCondition.java`：实现基于模组配置的配方加载条件（NeoForge `ICondition`）。
   - [NEW] `src/main/java/org/shengxi/TFCOreProcess/tfc_oreprocess/registry/ModConditions.java`：注册自定义加载条件。
4. **文档更新**
   - [NEW] `docs/pipeline_modes.md`：详细说明两种流水线模式的差异、产出比例与整合包配置指南。

## 验证计划

1. **构建与生成验证**：
   - 运行 `./gradlew.bat processResources compileJava --no-configuration-cache` 确保所有 4 档品位配方与 Panning 数据正确生成且编译无误。
2. **模式切换测试**：
   - 验证 `STANDARD` 模式下仅加载标准精简配方。
   - 验证 `TEMPLATE_CLASSIC` 模式下自动激活品位细分锤碎、Quern 研磨、Create 冲压/粉碎以及淘金盘淘洗。

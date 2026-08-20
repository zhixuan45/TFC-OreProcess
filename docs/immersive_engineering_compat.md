# Immersive Engineering 兼容方案

本项目核心不依赖 Immersive Engineering（IE）的代码层 API，兼容资源通过带有 NeoForge 模组加载条件的数据包配方在构建时自动生成到 `data/tfc_oreprocess/recipe/compat/immersiveengineering/` 目录。

生成的 Crusher 配方结构如下：

```json
{
  "neoforge:conditions": [
    {
      "type": "neoforge:mod_loaded",
      "mod_id": "immersiveengineering"
    }
  ],
  "type": "immersiveengineering:crusher",
  "energy": 1600,
  "input": {
    "item": "tfc:ore/normal_native_copper"
  },
  "result": {
    "item": "tfc_oreprocess:crushed_copper_ore"
  },
  "secondaries": [
    {
      "chance": 0.20,
      "output": {
        "item": "tfc_oreprocess:copper_ore_powder"
      }
    }
  ]
}
```

未安装 IE 时，NeoForge 会在数据包加载阶段安全跳过这些条件配方，不影响原版 TFC 与本模组的手工锤碎、洗矿与熔炼；当环境中安装了沉浸工业时，这些配方自动激活，使 IE 粉碎机可以直接输出本模组的碎矿与矿粉副产物，无缝接入后续的 TFC 原生洗矿与燃烧熔化。

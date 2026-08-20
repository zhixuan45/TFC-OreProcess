# 额外矿石数据包配置指南

对于非 TFC 原生的额外矿石（例如沉浸工业或其它模组添加的铝、铅、铀、钛等矿石），本项目支持完全通过纯数据包与数据组件（Data Component）进行无代码扩展。

## 1. 锤碎配方配置

在数据包的 `data/<namespace>/recipe/` 目录下新增配方 JSON 文件：

```json
{
  "type": "tfc_oreprocess:ore_crushing",
  "ingredient": {
    "item": "immersiveengineering:ore_lead"
  },
  "result": {
    "id": "tfc_oreprocess:crushed_ore",
    "count": 1,
    "components": {
      "tfc_oreprocess:ore_process": {
        "source_item": "immersiveengineering:ore_lead",
        "molten_fluid": "tfc:metal/lead",
        "metal_amount_mb": 25,
        "melting_temperature": 327.0
      }
    }
  }
}
```

- 该配方严格要求 1 个原矿 + 1 个 TFC 锤子工具，合成时锤子会自动损耗 1 点耐久。
- 产物为通用碎矿 `tfc_oreprocess:crushed_ore`，并附带 `OreProcessData` 数据组件。

## 2. TFC 原生 Sluice 洗矿与战利品表配置

为碎矿配置 Sluice Deposit（`data/<namespace>/tfc/deposit/lead.json`）：

```json
{
  "ingredient": {
    "item": "tfc_oreprocess:crushed_ore"
  },
  "loot_table": "<namespace>:deposit/lead"
}
```

在对应战利品表（`data/<namespace>/loot_table/deposit/lead.json`）中声明矿粉与副产物：

```json
{
  "type": "minecraft:empty",
  "pools": [
    {
      "name": "base_powder",
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "tfc_oreprocess:ore_powder",
          "functions": [
            {
              "function": "minecraft:set_count",
              "count": 5
            },
            {
              "function": "minecraft:set_components",
              "components": {
                "tfc_oreprocess:ore_process": {
                  "source_item": "immersiveengineering:ore_lead",
                  "molten_fluid": "tfc:metal/lead",
                  "metal_amount_mb": 5,
                  "melting_temperature": 327.0
                }
              }
            }
          ]
        }
      ]
    }
  ]
}
```

## 3. TFC 燃烧熔化说明

由于模组内置了 `tfc_oreprocess:dynamic_heating` 动态加热适配配方，所有带有 `tfc_oreprocess:ore_process` 数据组件的通用矿粉、矿团与碎矿在放入 TFC 的坩埚、木炭锻炉或高炉加热达到指定熔点后，都会自动解析组件并熔化为对应的熔融金属流体，无需额外编写静态加热 JSON。

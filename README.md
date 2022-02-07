[中文](READNE_CN.md)

# Not Enough Energistics

[![Downloads](https://cf.way2muchnoise.eu/full_515565_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/not-enough-energistics) ![MCVsrsion](https://cf.way2muchnoise.eu/versions/515565.svg)

----

NotEnoughEnergistics is a port of [Just Enough Energistics](https://www.curseforge.com/minecraft/mc-mods/just-enough-energistics-jee) to Minecraft 1.7.10

This mod replaces AE2's default NEI integration for the Pattern Terminal.

By default, AE2 forces the player to either have the necessary items for a recipe in their personal inventory or in their ME network.

This mod removes that functionality and allows the player to use NEI's transfer system (shift + click [?] button) to create patterns whether the player possesses the items required by the recipe.


## Features

- Automatically switch the mode of PatternTerminal.
- If an item is a probability output, then nee will not transfer it.
- Combine like stacks in processing patterns.
- Support Processing Pattern Terminal(16 -> 4 mode).
- Allow you to add item blackList and item priority list, if item in them, it will not be transferred / transfer it first.(use /nee RecipeProcessor to get RecipeProcessor and identifier in log)
- Allow you to add mod priority list,if the mod's has this item,it will be use first.
- You can use the ctrl + mouse wheel to change the Pattern item input/output amount.
- You can use shift + mouse wheel to choose ingredient.
- you can use ctrl+ click [?] in crafting terminal to request the items while missing in AE and having existing pattern in AE(alt + click for no preview)

## Compatible Modslist as followed：

- [x]  AppliedEnergistics2
- [x]  Vanilla
- [x]  GregTech5
- [x]  GregTech6
- [x]  IndustrialCraft2
- [x]  Avaritia
- [x]  EnderIO 
- [x]  Forestry (Doesn't support Fermenter and Still,because they don't have an item output)
- [x]  Thaumcraft NEI Plugin
- [x]  Thaumic Energistics (Allows you to transfer ArcaneRecipe from NEI to Knowledge Inscriber, it requires Thaumcraft NEI Plugin)
- [x]  ThermalExpansion
- [x]  ImmersiveEngineering
- [x]  Mekanism
- [x]  BloodMagic
- [x]  BuildCraft
- [x]  Avaritiaddons (auto extreme crafting table support)
- [x]  GT++

If you want other Mod support, see [RecipeProcessor](./src/main/java/com/github/vfyjxf/nee/processor/RecipeProcessor.java) and [Mod support Issue](https://github.com/vfyjxf/NotEnoughEnergistics/issues/1)

## TODO

- [x]  Add item blackList, if item in the blacklist, it will not be transferred.
- [x]  Add item priority list, if item in tne priority list, it will be transferred first.
- [x]  Combine like stacks in processing patterns.

## Credits
Thanks TheRealp455w0rd and his [Just Enough Energistics](https://www.curseforge.com/minecraft/mc-mods/just-enough-energistics-jee)

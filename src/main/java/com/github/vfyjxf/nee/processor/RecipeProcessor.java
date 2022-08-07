package com.github.vfyjxf.nee.processor;

import com.github.vfyjxf.nee.NotEnoughEnergistics;
import cpw.mods.fml.common.Loader;
import java.util.ArrayList;
import java.util.List;

public class RecipeProcessor {

    public static final String NULL_IDENTIFIER = "null";
    public static List<IRecipeProcessor> recipeProcessors = new ArrayList<>();

    public static void init() {
        NotEnoughEnergistics.logger.info("-----Not Enough Energistics Init Start-----");
        NotEnoughEnergistics.logger.info("Install Vanilla Support");
        recipeProcessors.add(new VanillaRecipeProcessor());

        if (Loader.isModLoaded("appliedenergistics2")) {
            NotEnoughEnergistics.logger.info("Found Applied Energistics 2,install Applied Energistics 2 support");
            recipeProcessors.add(new AppengRecipeProcessor());
        }

        if (Loader.isModLoaded("Botania")) {
            NotEnoughEnergistics.logger.info("Found Botania,install Botania support");
            recipeProcessors.add(new BotaniaRecipeProcessor());
        }

        if (Loader.isModLoaded("gregtech") && !Loader.isModLoaded("gregapi")) {
            NotEnoughEnergistics.logger.info("Found GregTech5,install GregTech5 support");
            recipeProcessors.add(new GregTech5RecipeProcessor());
        }
        if (Loader.isModLoaded("gregapi") && Loader.isModLoaded("gregapi_post")) {
            NotEnoughEnergistics.logger.info("Found GregTech6,install GregTech6 support");
            recipeProcessors.add(new GregTech6RecipeProcessor());
        }
        if (Loader.isModLoaded("IC2")) {
            NotEnoughEnergistics.logger.info("Found IC2,install IC2 Support");
            recipeProcessors.add(new ICRecipeProcessor());
        }
        if (Loader.isModLoaded("Avaritia")) {
            NotEnoughEnergistics.logger.info("Found Avaritia, install Avaritia support");
            recipeProcessors.add(new AvaritiaRecipeProcessor());
        }
        if (Loader.isModLoaded("EnderIO")) {
            NotEnoughEnergistics.logger.info("Found EnderIO, install EnderIO support");
            recipeProcessors.add(new EnderIORecipeProcessor());
        }
        if (Loader.isModLoaded("Forestry")) {
            NotEnoughEnergistics.logger.info("Found Forestry, install Forestry support");
            recipeProcessors.add(new ForestryRecipeProcessor());
        }
        if (Loader.isModLoaded("thaumcraftneiplugin")) {
            NotEnoughEnergistics.logger.info("Found TCNEIPlugin, install TCNEIPlugin support");
            recipeProcessors.add(new TCNEIPluginRecipeProcessor());
        }
        if (Loader.isModLoaded("ThermalExpansion")) {
            NotEnoughEnergistics.logger.info("Found ThermalExpansion, install ThermalExpansion support");
            recipeProcessors.add(new ThermalExpansionRecipeProcessor());
        }
        if (Loader.isModLoaded("ImmersiveEngineering")) {
            NotEnoughEnergistics.logger.info("Found ImmersiveEngineering, install ImmersiveEngineering support");
            recipeProcessors.add(new ImmersiveEngineeringRecipeProcessor());
        }
        if (Loader.isModLoaded("Mekanism")) {
            NotEnoughEnergistics.logger.info("Found Mekanism, install Mekanism support");
            recipeProcessors.add(new MekanismRecipeProcessor());
        }
        if (Loader.isModLoaded("AWWayofTime")) {
            NotEnoughEnergistics.logger.info("Found BloodMagic, install BloodMagic support");
            recipeProcessors.add(new BloodMagicRecipeProcessor());
        }
        if (Loader.isModLoaded("BuildCraft|Compat")) {
            NotEnoughEnergistics.logger.info("Found BuildCraft-Compat, install BuildCraft-Compat support");
            recipeProcessors.add(new BuildCraftRecipeProcessor());
        }
        if (Loader.isModLoaded("miscutils")) {
            NotEnoughEnergistics.logger.info("Found GT++, install GT++ support");
            recipeProcessors.add(new GTPPRecipeProcessor());
        }
        if (Loader.isModLoaded("GoodGenerator")) {
            NotEnoughEnergistics.logger.info("Found Good Generator, install Good Generator support");
            recipeProcessors.add(new GoodGeneratorRecipeProcessor());
        }

        NotEnoughEnergistics.logger.info("-----Not Enough Energistics Init  Finished-----");
    }
}

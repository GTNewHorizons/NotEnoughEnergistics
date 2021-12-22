package com.github.vfyjxf.nee.config;

import com.github.vfyjxf.nee.NotEnoughEnergistics;
import com.github.vfyjxf.nee.utils.ItemUtils;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * @author vfyjxf
 */
public class NEEConfig {

    public static Configuration config;

    public static String[] transformBlacklist = new String[0];
    public static String[] transformPriorityList = new String[0];
    public static String[] transformPriorityModList = new String[0];

    public static boolean noShift = true;
    public static boolean matchOtherItems = true;
    public static boolean drawHighlight = true;
    public static boolean allowSynchronousSwitchIngredient = true;
    public static boolean useStackSizeFromNEI = true;

    public static int draggedStackDefaultSize = 1;

    public static void loadConfig(File configFile) {
        config = new Configuration(configFile);
        config.load();

        transformBlacklist = config.get("client", "transformItemBlacklist", new String[0],
                "If item in the blacklist, it will not be transferred.\n" +
                        "the format is \" {modid:modid,name:name,meta:meta,recipeProcessor:recipeProcessorID,identifier:identifier}\"\n" +
                        "example: \"{modid:minecraft,name:iron_ingot,recipeProcessor:EnderIO,identifier:EnderIOAlloySmelter}\"").getStringList();
        transformPriorityList = config.get("client", "transformItemPriorityList", new String[0],
                "If item in the priority list, it will be transferred first.").getStringList();

        transformPriorityModList = config.get("client", "transformPriorityModList", new String[0],
                "if oredict has this mod's item, use it first").getStringList();

        noShift = config.get("client", "noShift", true,
                "if true, you don't need to press shift to use NEI's transfer system in CratingTerminal and PatternTerminal").getBoolean();

        matchOtherItems = config.get("client", "matchOnCraftableItems", true,
                "If true, Crafting Helper will match other items even they can't auto-crafting").getBoolean();

        drawHighlight = config.get("client", "drawHighlight", true,
                "if true,it will draw highlight for missing items and item which can autocraft in nei").getBoolean();

        allowSynchronousSwitchIngredient = config.get("client", "allowSynchronousSwitchIngredient", true,
                "If true, it will make all similar ingredient switch at the same time").getBoolean(true);

        useStackSizeFromNEI = config.get("client","useStackSizeFromNEI",true,
                "Use the StackSize set by NEI").getBoolean();

        draggedStackDefaultSize = config.get("client", "draggedStackDefaultSize", 1,
                "The default size of the dragged ItemStack when it is put in slot(Used when useStackSizeFromNEI is false)", 1, 64).getInt();

        if (config.hasChanged()) config.save();
    }

    public static void reload() {
        config.load();
        transformBlacklist = config.get("client", "transformItemBlacklist", new String[0],
                "If item in the blacklist, it will not be transferred.\n" +
                        "the format is \" {modid:modid,name:name,meta:meta,recipeProcessor:recipeProcessorID,identifier:identifier}\"\n" +
                        "example: \"{modid:minecraft,name:iron_ingot,recipeProcessor:EnderIO,identifier:EnderIOAlloySmelter}\"").getStringList();
        transformPriorityList = config.get("client", "transformItemPriorityList", new String[0],
                "If item in the priority list, it will be transferred first.").getStringList();

        transformPriorityModList = config.get("client", "transformPriorityModList", new String[0],
                "if oredict has this mod's item, use it first").getStringList();

        matchOtherItems = config.get("client", "matchOnCraftableItems", false,
                "If false, Crafting Helper will not match other items").getBoolean();

        allowSynchronousSwitchIngredient = config.get("client", "allowSynchronousSwitchIngredient", true,
                "If true, it will make all similar ingredient switch at the same time").getBoolean(true);

        useStackSizeFromNEI = config.get("client","useStackSizeFromNEI",true,
                "Use the StackSize set by NEI").getBoolean();

        draggedStackDefaultSize = config.get("client", "draggedStackDefaultSize", 1,
                "The default size of the dragged ItemStack when it is put in slot(Used when useStackSizeFromNEI is false)", 1, 64).getInt();

        ItemUtils.reloadConfig();
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (NotEnoughEnergistics.MODID.equals(event.modID)) {
            config.save();
            NEEConfig.reload();
        }
    }

}

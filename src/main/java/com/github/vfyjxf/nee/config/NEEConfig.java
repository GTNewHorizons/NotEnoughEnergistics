package com.github.vfyjxf.nee.config;

import com.github.vfyjxf.nee.NotEnoughEnergistics;
import com.github.vfyjxf.nee.utils.ItemUtils;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.io.File;
import net.minecraftforge.common.config.Configuration;

/**
 * @author vfyjxf
 */
public class NEEConfig {

    public static Configuration config;
    public static final String CATEGORY_PATTERN_TRANSFER = "transfer";
    public static final String CATEGORY_CRAFTING_HELPER = "helper";
    public static final String CATEGORY_OTHER_SETTINGS = "other";

    public static String[] transformBlacklist = new String[0];
    public static String[] transformPriorityList = new String[0];
    public static String[] transformPriorityModList = new String[0];
    public static String itemCombinationMode = "ENABLED";
    public static String[] itemCombinationWhitelist = new String[0];

    public static boolean noShift = true;
    public static boolean matchOtherItems = true;
    public static boolean drawHighlight = true;
    public static boolean allowSynchronousSwitchIngredient = true;
    public static boolean useNEIDragFromNEIAddons = false;
    public static boolean enableNEIDragDrop = true;
    public static boolean useStackSizeFromNEI = true;
    public static boolean keepGhostitems = true;

    public static int draggedStackDefaultSize = 1;

    public static void loadConfig(File configFile) {
        config = new Configuration(configFile);
        config.load();

        {
            transformBlacklist = config.get(
                            CATEGORY_PATTERN_TRANSFER,
                            "transformItemBlacklist",
                            new String[0],
                            "If item in the blacklist, it will not be transferred.\n"
                                    + "the format is \" {modid:modid,name:name,meta:meta,recipeProcessor:recipeProcessorID,identifier:identifier}\"\n"
                                    + "example: \"{modid:minecraft,name:iron_ingot,recipeProcessor:EnderIO,identifier:EnderIOAlloySmelter}\"")
                    .getStringList();
            transformPriorityList = config.get(
                            CATEGORY_PATTERN_TRANSFER,
                            "transformItemPriorityList",
                            new String[0],
                            "If item in the priority list, it will be transferred first.")
                    .getStringList();

            transformPriorityModList = config.get(
                            CATEGORY_PATTERN_TRANSFER,
                            "transformPriorityModList",
                            new String[0],
                            "if oredict has this mod's item, use it first")
                    .getStringList();

            noShift = config.get(
                            CATEGORY_PATTERN_TRANSFER,
                            "noShift",
                            true,
                            "if true, you don't need to press shift to use NEI's transfer system in CratingTerminal and PatternTerminal")
                    .getBoolean();

            itemCombinationMode = config.get(
                            CATEGORY_PATTERN_TRANSFER,
                            "itemCombinationMode",
                            itemCombinationMode,
                            "Item Combination Mode Setting, here are all the modes. \n" + "\"ENABLED\"" + " "
                                    + "\"DISABLED\"" + " " + "WHITELIST")
                    .getString();

            itemCombinationWhitelist = config.get(
                            CATEGORY_PATTERN_TRANSFER,
                            "itemCombinationWhitelist",
                            itemCombinationWhitelist,
                            "Whitelist for item combination")
                    .getStringList();
        }

        {
            matchOtherItems = config.get(
                            CATEGORY_CRAFTING_HELPER,
                            "matchOnCraftableItems",
                            true,
                            "If true, Crafting Helper will match other items even they can't auto-crafting")
                    .getBoolean();

            drawHighlight = config.get(
                            CATEGORY_CRAFTING_HELPER,
                            "drawHighlight",
                            true,
                            "if true,it will draw highlight for missing items and item which can autocraft in nei")
                    .getBoolean();
        }

        {
            allowSynchronousSwitchIngredient = config.get(
                            CATEGORY_OTHER_SETTINGS,
                            "allowSynchronousSwitchIngredient",
                            true,
                            "If true, it will make all similar ingredient switch at the same time")
                    .getBoolean(true);

            useNEIDragFromNEIAddons = config.get(
                            CATEGORY_OTHER_SETTINGS,
                            "useNEIDragFromNEIAddons",
                            false,
                            "Use the NEI Drag from NEI Addons")
                    .getBoolean();

            enableNEIDragDrop = config.get(CATEGORY_OTHER_SETTINGS, "enableNEIDragDrop", true, "")
                    .getBoolean();

            useStackSizeFromNEI = config.get(
                            CATEGORY_OTHER_SETTINGS, "useStackSizeFromNEI", true, "Use the StackSize set by NEI")
                    .getBoolean();

            keepGhostitems = config.get(CATEGORY_OTHER_SETTINGS, "keepGhostitems", true, "keep ghost items after click")
                    .getBoolean();

            draggedStackDefaultSize = config.get(
                            CATEGORY_OTHER_SETTINGS,
                            "draggedStackDefaultSize",
                            1,
                            "The default size of the dragged ItemStack when it is put in slot(Used when useStackSizeFromNEI is false)",
                            1,
                            64)
                    .getInt();
        }

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static void reload() {
        config.load();

        {
            transformBlacklist = config.get(
                            CATEGORY_PATTERN_TRANSFER,
                            "transformItemBlacklist",
                            new String[0],
                            "If item in the blacklist, it will not be transferred.\n"
                                    + "the format is \" {modid:modid,name:name,meta:meta,recipeProcessor:recipeProcessorID,identifier:identifier}\"\n"
                                    + "example: \"{modid:minecraft,name:iron_ingot,recipeProcessor:EnderIO,identifier:EnderIOAlloySmelter}\"")
                    .getStringList();
            transformPriorityList = config.get(
                            CATEGORY_PATTERN_TRANSFER,
                            "transformItemPriorityList",
                            new String[0],
                            "If item in the priority list, it will be transferred first.")
                    .getStringList();

            transformPriorityModList = config.get(
                            CATEGORY_PATTERN_TRANSFER,
                            "transformPriorityModList",
                            new String[0],
                            "if oredict has this mod's item, use it first")
                    .getStringList();

            noShift = config.get(
                            CATEGORY_PATTERN_TRANSFER,
                            "noShift",
                            true,
                            "if true, you don't need to press shift to use NEI's transfer system in CratingTerminal and PatternTerminal")
                    .getBoolean();

            itemCombinationMode = config.get(
                            CATEGORY_PATTERN_TRANSFER,
                            "itemCombinationMode",
                            itemCombinationMode,
                            "Item Combination Mode Setting, here are all the modes. \n" + "\"ENABLED\"" + " "
                                    + "\"DISABLED\"" + " " + "WHITELIST")
                    .getString();

            itemCombinationWhitelist = config.get(
                            CATEGORY_PATTERN_TRANSFER,
                            "itemCombinationWhitelist",
                            itemCombinationWhitelist,
                            "Whitelist for item combination")
                    .getStringList();
        }

        {
            matchOtherItems = config.get(
                            CATEGORY_CRAFTING_HELPER,
                            "matchOnCraftableItems",
                            true,
                            "If true, Crafting Helper will match other items even they can't auto-crafting")
                    .getBoolean();

            drawHighlight = config.get(
                            CATEGORY_CRAFTING_HELPER,
                            "drawHighlight",
                            true,
                            "if true,it will draw highlight for missing items and item which can autocraft in nei")
                    .getBoolean();
        }

        {
            allowSynchronousSwitchIngredient = config.get(
                            CATEGORY_OTHER_SETTINGS,
                            "allowSynchronousSwitchIngredient",
                            true,
                            "If true, it will make all similar ingredient switch at the same time")
                    .getBoolean(true);

            enableNEIDragDrop = config.get(CATEGORY_OTHER_SETTINGS, "enableNEIDragNDrop", true, "")
                    .getBoolean();

            useStackSizeFromNEI = config.get(
                            CATEGORY_OTHER_SETTINGS, "useStackSizeFromNEI", true, "Use the StackSize set by NEI")
                    .getBoolean();

            keepGhostitems = config.get(CATEGORY_OTHER_SETTINGS, "keepGhostitems", true, "keep ghost items after click")
                    .getBoolean();

            draggedStackDefaultSize = config.get(
                            CATEGORY_OTHER_SETTINGS,
                            "draggedStackDefaultSize",
                            1,
                            "The default size of the dragged ItemStack when it is put in slot(Used when useStackSizeFromNEI is false)",
                            1,
                            64)
                    .getInt();
        }

        ItemUtils.reloadConfig();
    }

    public static void setItemCombinationMode(String itemCombinationMode) {
        NEEConfig.itemCombinationMode = itemCombinationMode;
        config.get(
                        CATEGORY_PATTERN_TRANSFER,
                        "itemCombinationMode",
                        itemCombinationMode,
                        "Item Combination Mode Setting, here are all the modes. \n" + "\"ENABLED\"" + " "
                                + "\"DISABLED\"" + " " + "WHITELIST")
                .set(itemCombinationMode);
        config.save();
    }

    public static void setItemCombinationWhitelist(String[] itemCombinationWhitelist) {
        NEEConfig.itemCombinationWhitelist = itemCombinationWhitelist;
        config.get(
                        CATEGORY_PATTERN_TRANSFER,
                        "itemCombinationWhitelist",
                        itemCombinationWhitelist,
                        "Whitelist for item combination")
                .set(itemCombinationWhitelist);
        config.save();
    }

    public static void setTransformBlacklist(String[] transformBlacklist) {
        NEEConfig.transformBlacklist = transformBlacklist;
        config.get(
                        "client",
                        "transformItemBlacklist",
                        new String[0],
                        "If item in the blacklist, it will not be transferred.\n"
                                + "the format is \" {modid:modid,name:name,meta:meta,recipeProcessor:recipeProcessorID,identifier:identifier}\"\n"
                                + "example: \"{modid:minecraft,name:iron_ingot,recipeProcessor:EnderIO,identifier:EnderIOAlloySmelter}\"")
                .set(transformBlacklist);
        config.save();
    }

    public static void setTransformPriorityList(String[] transformPriorityList) {
        NEEConfig.transformPriorityList = transformPriorityList;
        config.get("client", "transformItemPriorityList", new String[0], "if oredict has this mod's item, use it first")
                .set(transformPriorityList);
        config.save();
    }

    public static void setTransformPriorityModList(String[] transformPriorityModList) {
        NEEConfig.transformPriorityModList = transformPriorityModList;
        config.get("client", "transformPriorityModList", new String[0], "if oredict has this mod's item, use it first")
                .set(transformPriorityModList);
        config.save();
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (NotEnoughEnergistics.MODID.equals(event.modID)) {
            config.save();
            NEEConfig.reload();
        }
    }
}

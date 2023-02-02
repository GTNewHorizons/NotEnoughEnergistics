package com.github.vfyjxf.nee.utils;

import static com.github.vfyjxf.nee.config.NEEConfig.*;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;

import codechicken.nei.PositionedStack;

import com.github.vfyjxf.nee.NotEnoughEnergistics;
import com.github.vfyjxf.nee.processor.IRecipeProcessor;
import com.github.vfyjxf.nee.processor.RecipeProcessor;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;

public final class ItemUtils {

    public static Gson gson = new Gson();
    public static List<StackProcessor> transformItemBlacklist = getTransformItemBlacklist();
    public static List<StackProcessor> transformItemPriorityList = getTransformItemPriorityList();

    private static Class<?> GT_MetaGenerated_ToolClass;

    static {
        try {
            GT_MetaGenerated_ToolClass = Class.forName("gregtech.api.items.GT_MetaGenerated_Tool");
        } catch (ClassNotFoundException ignored) {}
    }

    public static void reloadConfig() {
        transformItemBlacklist = getTransformItemBlacklist();
        transformItemPriorityList = getTransformItemPriorityList();
    }

    public static List<StackProcessor> getTransformItemBlacklist() {
        List<StackProcessor> transformItemBlacklist = new ArrayList<>();
        for (String itemJsonString : transformBlacklist) {
            StackProcessor processor;
            try {
                processor = gson.fromJson(itemJsonString, StackProcessor.class);
            } catch (JsonSyntaxException e) {
                NotEnoughEnergistics.logger.error("Found a error item json in item blacklist: " + itemJsonString);
                continue;
            }
            if (processor != null) {
                Item currentItem = GameRegistry.findItem(processor.modid, processor.name);
                if (currentItem != null) {
                    ItemStack currentStack = processor.meta != null
                            ? new ItemStack(currentItem, 1, Integer.parseInt(processor.meta))
                            : new ItemStack(currentItem);
                    if (processor.nbt != null) {
                        NBTTagCompound nbt = null;
                        try {
                            nbt = (NBTTagCompound) JsonToNBT.func_150315_a(processor.nbt);
                        } catch (NBTException e) {
                            e.printStackTrace();
                        }
                        if (nbt != null) {
                            currentStack.setTagCompound(nbt);
                        }
                    }
                    transformItemBlacklist.add(
                            new StackProcessor(
                                    currentStack,
                                    currentItem,
                                    processor.recipeProcessor,
                                    processor.identifier));
                }
            }
        }

        return transformItemBlacklist;
    }

    public static List<StackProcessor> getTransformItemPriorityList() {
        List<StackProcessor> transformItemPriorityList = new ArrayList<>();
        for (String itemJsonString : transformPriorityList) {
            StackProcessor processor;
            try {
                processor = gson.fromJson(itemJsonString, StackProcessor.class);
            } catch (JsonSyntaxException e) {
                NotEnoughEnergistics.logger.error("Found a error item json in item priority list: " + itemJsonString);
                continue;
            }
            if (processor != null) {
                Item currentItem = GameRegistry.findItem(processor.modid, processor.name);
                if (currentItem != null) {
                    ItemStack currentStack = processor.meta != null
                            ? new ItemStack(currentItem, 1, Integer.parseInt(processor.meta))
                            : new ItemStack(currentItem);
                    if (processor.nbt != null) {
                        NBTTagCompound nbt = null;
                        try {
                            nbt = (NBTTagCompound) JsonToNBT.func_150315_a(processor.nbt);
                        } catch (NBTException e) {
                            e.printStackTrace();
                        }
                        if (nbt != null) {
                            currentStack.setTagCompound(nbt);
                        }
                    }
                    transformItemPriorityList.add(
                            new StackProcessor(
                                    currentStack,
                                    currentItem,
                                    processor.recipeProcessor,
                                    processor.identifier));
                }
            }
        }

        return transformItemPriorityList;
    }

    public static boolean isPreferItems(ItemStack itemStack, String recipeProcessor, String identifier) {
        for (StackProcessor processor : ItemUtils.transformItemPriorityList) {
            ItemStack copyStack = itemStack.copy();
            copyStack.stackSize = 1;
            if (ItemStack.areItemStacksEqual(copyStack, processor.itemStack)) {
                if (processor.recipeProcessor == null && processor.identifier == null) {
                    return true;
                } else if (processor.recipeProcessor == null) {
                    return identifier.equals(processor.identifier);
                } else if (processor.identifier == null) {
                    return recipeProcessor.equals(processor.recipeProcessor);
                } else {
                    return recipeProcessor.equals(processor.recipeProcessor) && identifier.equals(processor.identifier);
                }
            }
        }
        return false;
    }

    public static boolean isPreferItems(ItemStack itemStack) {
        for (StackProcessor processor : ItemUtils.transformItemPriorityList) {
            ItemStack copyStack = itemStack.copy();
            copyStack.stackSize = 1;
            if (ItemStack.areItemStacksEqual(copyStack, processor.itemStack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInBlackList(ItemStack itemStack, String recipeProcessor, String identifier) {
        for (StackProcessor processor : ItemUtils.transformItemBlacklist) {
            ItemStack copyStack = itemStack.copy();
            copyStack.stackSize = 1;
            if (ItemStack.areItemStacksEqual(copyStack, processor.itemStack)) {
                if (processor.recipeProcessor == null && processor.identifier == null) {
                    return true;
                } else if (processor.recipeProcessor == null) {
                    return identifier.equals(processor.identifier);
                } else if (processor.identifier == null) {
                    return recipeProcessor.equals(processor.recipeProcessor);
                } else {
                    return recipeProcessor.equals(processor.recipeProcessor) && identifier.equals(processor.identifier);
                }
            }
        }
        return false;
    }

    public static ItemStack getPreferModItem(ItemStack[] items) {
        for (String currentId : transformPriorityModList) {
            for (ItemStack stack : items) {
                GameRegistry.UniqueIdentifier itemId = GameRegistry.findUniqueIdentifierFor(stack.getItem());
                if (itemId.modId.equals(currentId)) {
                    return stack;
                }
            }
        }
        return null;
    }

    public static int getIngredientIndex(ItemStack stack, PositionedStack positionedStack) {
        ItemStack stackInput = stack.copy();
        stackInput.stackSize = 1;
        for (int i = 0; i < positionedStack.items.length; i++) {
            ItemStack currentStack = positionedStack.items[i].copy();
            currentStack.stackSize = 1;
            if (ItemStack.areItemStacksEqual(currentStack, stackInput)) {
                return i;
            }
        }
        return -1;
    }

    public static String toItemJsonString(ItemStack stack) {
        GameRegistry.UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(stack.getItem());
        String nbtString = stack.hasTagCompound() ? ",\"nbt\":" + stack.getTagCompound().toString() : "";
        int meta = stack.getItemDamage();
        return "{" + "\"modid\":"
                + identifier.modId
                + ","
                + "\"name\":"
                + identifier.name
                + ","
                + "\"meta\":"
                + meta
                + nbtString
                + "}";
    }

    public static boolean hasRecipeProcessor(String processorId) {
        for (IRecipeProcessor processor : RecipeProcessor.recipeProcessors) {
            if (processor.getRecipeProcessorId().equals(processorId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasOverlayIdentifier(String identifier) {
        for (IRecipeProcessor processor : RecipeProcessor.recipeProcessors) {
            for (String ident : processor.getAllOverlayIdentifier()) {
                if (ident.equals(identifier)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasModId(String modid) {
        for (String currentId : transformPriorityModList) {
            if (currentId.equals(modid)) {
                return true;
            }
        }
        return false;
    }

    public static void transformGTTool(ItemStack stack) {
        if (!Loader.isModLoaded("gregtech")) return;
        if (GT_MetaGenerated_ToolClass != null
                && GT_MetaGenerated_ToolClass.isAssignableFrom(stack.getItem().getClass())) {
            NBTTagCompound NBT = stack.getTagCompound();
            if (NBT == null) {
                NBT = new NBTTagCompound();
                NBTTagCompound NBTToolStats = new NBTTagCompound();
                NBTToolStats.setString("PrimaryMaterial", "Neutronium");
                NBTToolStats.setString("SecondaryMaterial", "Neutronium");
                NBTToolStats.setInteger("MaxDamage", 65536000);
                NBT.setTag("GT.ToolStats", NBTToolStats);
                stack.setTagCompound(NBT);
            }
        }
    }
}

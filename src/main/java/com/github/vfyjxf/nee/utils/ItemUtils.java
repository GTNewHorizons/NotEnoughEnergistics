package com.github.vfyjxf.nee.utils;

import static com.github.vfyjxf.nee.NEEConfig.transformBlacklist;
import static com.github.vfyjxf.nee.NEEConfig.transformPriorityList;

import com.github.vfyjxf.nee.NotEnoughEnergistics;
import com.google.gson.Gson;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;


import java.util.ArrayList;
import java.util.List;

public final class ItemUtils {

    public static Gson gson = new Gson();

    public static List<StackProcessor> getTransformItemBlacklist() {
        List<StackProcessor> transformItemBlacklist = new ArrayList<>();
        for (String itemJsonString : transformBlacklist) {
            StackProcessor processor = gson.fromJson(itemJsonString, StackProcessor.class);
            if (processor != null) {
                Item currentItem = GameRegistry.findItem(processor.modid, processor.name);
                if (currentItem != null) {
                    ItemStack currentStack = processor.meta != null ? new ItemStack(currentItem, 1, Integer.parseInt(processor.meta)) : new ItemStack(currentItem);
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
                    transformItemBlacklist.add(new StackProcessor(currentStack, currentItem, processor.recipeProcessor, processor.identifier));
                }
            }
        }

        return transformItemBlacklist;
    }

    public static List<StackProcessor> getTransformItemPriorityList() {
        List<StackProcessor> transformItemPriorityList = new ArrayList<>();
        for (String itemJsonString : transformPriorityList) {
            StackProcessor processor = gson.fromJson(itemJsonString, StackProcessor.class);
            if (processor != null) {
                Item currentItem = GameRegistry.findItem(processor.modid, processor.name);
                if (currentItem != null) {
                    ItemStack currentStack = processor.meta != null ? new ItemStack(currentItem, 1, Integer.parseInt(processor.meta)) : new ItemStack(currentItem);
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
                    transformItemPriorityList.add(new StackProcessor(currentStack, currentItem, processor.recipeProcessor, processor.identifier));
                }
            }
        }

        return transformItemPriorityList;
    }


    public static boolean isPreferItems(ItemStack itemStack, String recipeProcessor, String identifier) {
        for (StackProcessor processor : getTransformItemPriorityList()) {
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
        for (StackProcessor processor : getTransformItemPriorityList()) {
            ItemStack copyStack = itemStack.copy();
            copyStack.stackSize = 1;
            if (ItemStack.areItemStacksEqual(copyStack, processor.itemStack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInBlackList(ItemStack itemStack, String recipeProcessor, String identifier) {
        for (StackProcessor processor : getTransformItemBlacklist()) {
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

}
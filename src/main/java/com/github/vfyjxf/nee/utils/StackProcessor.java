package com.github.vfyjxf.nee.utils;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class StackProcessor {

    public String modid;
    public String name;
    public String meta;
    public String nbt;
    public ItemStack itemStack;
    public Item item;
    public String recipeProcessor;
    public String identifier;

    public StackProcessor(String modid, String name, String meta, String nbt) {
        this.modid = modid;
        this.name = name;
        this.meta = meta;
        this.nbt = nbt;
    }

    public StackProcessor(String modid, String name, String meta) {
        this.modid = modid;
        this.name = name;
        this.meta = meta;
    }

    public StackProcessor(String modid, String name) {
        this.modid = modid;
        this.name = name;
    }

    public StackProcessor(ItemStack itemStack, Item item, String recipeProcessor, String identifier) {
        this.itemStack = itemStack;
        this.item = item;
        this.recipeProcessor = recipeProcessor;
        this.identifier = identifier;
    }
}

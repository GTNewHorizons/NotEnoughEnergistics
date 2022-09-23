package com.github.vfyjxf.nee.nei;

import static com.github.vfyjxf.nee.processor.RecipeProcessor.NULL_IDENTIFIER;
import static com.github.vfyjxf.nee.utils.GuiUtils.isPatternTerm;

import appeng.util.Platform;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import com.github.vfyjxf.nee.config.ItemCombination;
import com.github.vfyjxf.nee.config.NEEConfig;
import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketArcaneRecipe;
import com.github.vfyjxf.nee.network.packet.PacketExtremeRecipe;
import com.github.vfyjxf.nee.network.packet.PacketNEIPatternRecipe;
import com.github.vfyjxf.nee.processor.IRecipeProcessor;
import com.github.vfyjxf.nee.processor.RecipeProcessor;
import com.github.vfyjxf.nee.utils.ItemUtils;
import com.github.vfyjxf.nee.utils.ModIDs;
import com.glodblock.github.nei.FluidPatternTerminalRecipeTransferHandler;
import cpw.mods.fml.common.Optional;
import java.util.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

/**
 * @author vfyjxf
 */
public class NEECraftingHandler implements IOverlayHandler {

    public static final String INPUT_KEY = "#";
    public static final String OUTPUT_KEY = "Outputs";
    public static Map<String, PositionedStack> ingredients = new HashMap<>();

    public static final NEECraftingHandler INSTANCE = new NEECraftingHandler();
    private static Class<?> knowledgeInscriberClz;
    private static Class<?> itemAspectClz;
    private static Class<?> guiExtremeAutoCrafterClz;

    static {
        try {
            knowledgeInscriberClz = Class.forName("thaumicenergistics.client.gui.GuiKnowledgeInscriber");
        } catch (ClassNotFoundException ignored) {
        }
        try {
            itemAspectClz = Class.forName("com.djgiannuzz.thaumcraftneiplugin.items.ItemAspect");
        } catch (ClassNotFoundException ignored) {
        }
        try {
            guiExtremeAutoCrafterClz =
                    Class.forName("wanion.avaritiaddons.block.extremeautocrafter.GuiExtremeAutoCrafter");
        } catch (ClassNotFoundException ignored) {
        }
    }

    public static boolean isCraftingTableRecipe(IRecipeHandler recipe) {
        if (recipe instanceof TemplateRecipeHandler) {
            TemplateRecipeHandler templateRecipeHandler = (TemplateRecipeHandler) recipe;
            String overlayIdentifier = templateRecipeHandler.getOverlayIdentifier();
            return "crafting".equals(overlayIdentifier) || "crafting2x2".equals(overlayIdentifier);
        } else {
            return false;
        }
    }

    @Optional.Method(modid = ModIDs.FC)
    private void fluidCraftOverlayRecipe(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex, boolean shift) {
        FluidPatternTerminalRecipeTransferHandler.INSTANCE.overlayRecipe(firstGui, recipe, recipeIndex, shift);
    }

    @Override
    public void overlayRecipe(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex, boolean shift) {
        if (isPatternTerm(firstGui)) {
            NEENetworkHandler.getInstance().sendToServer(packRecipe(recipe, recipeIndex));
            fluidCraftOverlayRecipe(firstGui, recipe, recipeIndex, shift);
        } else {
            knowledgeInscriberHandler(firstGui, recipe, recipeIndex);
            extremeAutoCrafterHandler(firstGui, recipe, recipeIndex);
        }
    }

    private PacketNEIPatternRecipe packRecipe(IRecipeHandler recipe, int recipeIndex) {
        if (isCraftingTableRecipe(recipe)) {
            return packCraftingTableRecipe(recipe, recipeIndex);
        } else {
            return packProcessRecipe(recipe, recipeIndex);
        }
    }

    private PacketNEIPatternRecipe packProcessRecipe(IRecipeHandler recipe, int recipeIndex) {
        final NBTTagCompound recipeInputs = new NBTTagCompound();
        NBTTagCompound recipeOutputs = new NBTTagCompound();
        String identifier = recipe instanceof TemplateRecipeHandler ? recipe.getOverlayIdentifier() : NULL_IDENTIFIER;
        if (identifier == null) {
            identifier = NULL_IDENTIFIER;
        }
        int inputIndex = 0;
        int outputIndex = 0;
        // get all recipe inputs and other stacks,use first item
        for (IRecipeProcessor processor : RecipeProcessor.recipeProcessors) {
            if (processor.getAllOverlayIdentifier().contains(identifier)) {
                List<PositionedStack> inputs = processor.getRecipeInput(recipe, recipeIndex, identifier);
                List<PositionedStack> outputs = processor.getRecipeOutput(recipe, recipeIndex, identifier);
                String recipeProcessorId = processor.getRecipeProcessorId();
                List<PositionedStack> mergedInputs = new ArrayList<>();

                if (!inputs.isEmpty() && !outputs.isEmpty()) {
                    NEECraftingHandler.ingredients.clear();
                    for (PositionedStack positionedStack : inputs) {
                        ItemStack currentStack = positionedStack.items[0];
                        boolean find = false;
                        ItemCombination currentValue = ItemCombination.valueOf(NEEConfig.itemCombinationMode);
                        if (currentValue != ItemCombination.DISABLED
                                && processor.mergeStacks(recipe, recipeIndex, identifier)) {
                            boolean isWhitelist = currentValue == ItemCombination.WHITELIST
                                    && Arrays.asList(NEEConfig.itemCombinationWhitelist)
                                            .contains(identifier);
                            if (currentValue == ItemCombination.ENABLED || isWhitelist) {
                                for (PositionedStack storedStack : mergedInputs) {
                                    ItemStack firstStack = storedStack.items[0];
                                    boolean areItemStackEqual = firstStack.isItemEqual(currentStack)
                                            && ItemStack.areItemStackTagsEqual(firstStack, currentStack);
                                    if (areItemStackEqual
                                            && (firstStack.stackSize + currentStack.stackSize)
                                                    <= firstStack.getMaxStackSize()) {
                                        find = true;
                                        storedStack.items[0].stackSize = firstStack.stackSize + currentStack.stackSize;
                                    }
                                }
                            }
                        }
                        if (!find) {
                            mergedInputs.add(positionedStack.copy());
                        }
                    }
                    for (PositionedStack positionedStack : mergedInputs) {
                        ItemStack currentStack = positionedStack.items[0];
                        ItemStack preferModItem = ItemUtils.getPreferModItem(positionedStack.items);

                        if (preferModItem != null) {
                            currentStack = preferModItem;
                            currentStack.stackSize = positionedStack.items[0].stackSize;
                        }

                        for (ItemStack stack : positionedStack.items) {
                            if (Platform.isRecipePrioritized(stack)
                                    || ItemUtils.isPreferItems(stack, recipeProcessorId, identifier)) {
                                currentStack = stack.copy();
                                currentStack.stackSize = positionedStack.items[0].stackSize;
                                break;
                            }
                        }
                        if (ItemUtils.isInBlackList(currentStack, recipeProcessorId, identifier)) {
                            continue;
                        }

                        if (currentStack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                            currentStack.setItemDamage(0);
                        }

                        recipeInputs.setTag("#" + inputIndex, currentStack.writeToNBT(new NBTTagCompound()));
                        NEECraftingHandler.ingredients.put(INPUT_KEY + inputIndex, positionedStack);
                        inputIndex++;
                    }

                    for (PositionedStack positionedStack : outputs) {
                        if (outputIndex >= 4 || positionedStack == null || positionedStack.item == null) {
                            continue;
                        }

                        ItemStack outputStack = positionedStack.item.copy();
                        if (outputStack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                            outputStack.setItemDamage(0);
                        }

                        recipeOutputs.setTag(OUTPUT_KEY + outputIndex, outputStack.writeToNBT(new NBTTagCompound()));
                        outputIndex++;
                    }
                    break;
                }
            }
        }
        return new PacketNEIPatternRecipe(recipeInputs, recipeOutputs);
    }

    private PacketNEIPatternRecipe packCraftingTableRecipe(IRecipeHandler recipe, int recipeIndex) {
        final NBTTagCompound recipeInputs = new NBTTagCompound();
        final List<PositionedStack> ingredients = recipe.getIngredientStacks(recipeIndex);
        NEECraftingHandler.ingredients.clear();
        for (final PositionedStack positionedStack : ingredients) {
            final int col = (positionedStack.relx - 25) / 18;
            final int row = (positionedStack.rely - 6) / 18;
            int slotIndex = col + row * 3;
            if (positionedStack.items != null && positionedStack.items.length > 0) {
                final ItemStack[] currentStackList = positionedStack.items;
                ItemStack stack = positionedStack.items[0];

                ItemStack preferModItem = ItemUtils.getPreferModItem(positionedStack.items);
                if (preferModItem != null) {
                    stack = preferModItem;
                }

                for (ItemStack currentStack : currentStackList) {
                    if (Platform.isRecipePrioritized(currentStack) || ItemUtils.isPreferItems(currentStack)) {
                        stack = currentStack.copy();
                    }
                }

                ItemUtils.transformGTTool(stack);

                if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                    stack.setItemDamage(0);
                }

                recipeInputs.setTag("#" + slotIndex, stack.writeToNBT(new NBTTagCompound()));
                NEECraftingHandler.ingredients.put(INPUT_KEY + slotIndex, positionedStack);
            }
        }
        return new PacketNEIPatternRecipe(recipeInputs, null);
    }

    private void knowledgeInscriberHandler(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex) {
        if (knowledgeInscriberClz != null && knowledgeInscriberClz.isInstance(firstGui)) {
            NEENetworkHandler.getInstance().sendToServer(packetArcaneRecipe(recipe, recipeIndex));
        }
    }

    private PacketArcaneRecipe packetArcaneRecipe(IRecipeHandler recipe, int recipeIndex) {
        final NBTTagCompound recipeInputs = new NBTTagCompound();
        List<PositionedStack> ingredients = recipe.getIngredientStacks(recipeIndex);
        if (itemAspectClz != null) {
            ingredients.removeIf(positionedStack -> itemAspectClz.isInstance(positionedStack.item.getItem()));
        }
        for (PositionedStack positionedStack : ingredients) {
            if (positionedStack.items != null && positionedStack.items.length > 0) {
                int slotIndex = getSlotIndex(positionedStack.relx * 100 + positionedStack.rely);
                final ItemStack[] currentStackList = positionedStack.items;
                ItemStack stack = positionedStack.item;
                for (ItemStack currentStack : currentStackList) {
                    if (Platform.isRecipePrioritized(currentStack)) {
                        stack = currentStack.copy();
                    }
                }
                recipeInputs.setTag("#" + slotIndex, stack.writeToNBT(new NBTTagCompound()));
            }
        }
        return new PacketArcaneRecipe(recipeInputs);
    }

    private void extremeAutoCrafterHandler(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex) {
        if (guiExtremeAutoCrafterClz != null && guiExtremeAutoCrafterClz.isInstance(firstGui)) {
            NEENetworkHandler.getInstance().sendToServer(packetExtremeRecipe(recipe, recipeIndex));
        }
    }

    private PacketExtremeRecipe packetExtremeRecipe(IRecipeHandler recipe, int recipeIndex) {
        NBTTagCompound recipeInputs = new NBTTagCompound();
        List<PositionedStack> ingredients = recipe.getIngredientStacks(recipeIndex);
        for (PositionedStack positionedStack : ingredients) {
            int col = (positionedStack.relx - 3) / 18;
            int row = (positionedStack.rely - 3) / 18;
            if (positionedStack.rely == 129) {
                col = (positionedStack.relx - 2) / 18;
            }
            int slotIndex = col + row * 9;
            ItemStack currentStack = positionedStack.items[0];
            ItemStack preferModItem = ItemUtils.getPreferModItem(positionedStack.items);
            if (preferModItem != null) {
                currentStack = preferModItem;
            }

            for (ItemStack stack : positionedStack.items) {
                if (ItemUtils.isPreferItems(stack)) {
                    currentStack = stack.copy();
                }
            }
            recipeInputs.setTag("#" + slotIndex, currentStack.writeToNBT(new NBTTagCompound()));
        }
        return new PacketExtremeRecipe(recipeInputs);
    }

    private int getSlotIndex(int xy) {
        switch (xy) {
            case 7533:
                return 1;
            case 10333:
                return 2;
            case 4960:
                return 3;
            case 7660:
                return 4;
            case 10360:
                return 5;
            case 4987:
                return 6;
            case 7687:
                return 7;
            case 10387:
                return 8;
            case 4832:
            default:
                return 0;
        }
    }
}

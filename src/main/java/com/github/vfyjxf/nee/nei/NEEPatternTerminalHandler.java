package com.github.vfyjxf.nee.nei;

import static com.github.vfyjxf.nee.processor.RecipeProcessor.NULL_IDENTIFIER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import com.github.vfyjxf.nee.config.ItemCombination;
import com.github.vfyjxf.nee.config.NEEConfig;
import com.github.vfyjxf.nee.nei.NEEGuiOverlayButton.NEEItemOverlayState;
import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketNEIPatternRecipe;
import com.github.vfyjxf.nee.processor.IRecipeProcessor;
import com.github.vfyjxf.nee.processor.RecipeProcessor;
import com.github.vfyjxf.nee.utils.Ingredient;
import com.github.vfyjxf.nee.utils.IngredientTracker;
import com.github.vfyjxf.nee.utils.ItemUtils;
import com.github.vfyjxf.nee.utils.ModIDs;
import com.glodblock.github.nei.FluidPatternTerminalRecipeTransferHandler;

import appeng.util.Platform;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiOverlayButton.ItemOverlayState;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * @author vfyjxf
 */
public class NEEPatternTerminalHandler implements IOverlayHandler {

    public static final String INPUT_KEY = "#";
    public static final String OUTPUT_KEY = "Outputs";
    public static Map<String, PositionedStack> ingredients = new HashMap<>();// Add the function of mouse wheel to
                                                                             // switch materials

    public static final NEEPatternTerminalHandler instance = new NEEPatternTerminalHandler();

    private NEEPatternTerminalHandler() {}

    @Override
    public void overlayRecipe(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex, boolean shift) {
        NEENetworkHandler.getInstance().sendToServer(packRecipe(recipe, recipeIndex));

        if (Loader.isModLoaded(ModIDs.FC)) {
            fluidCraftOverlayRecipe(firstGui, recipe, recipeIndex, shift);
        }
    }

    @Override
    public List<ItemOverlayState> presenceOverlay(GuiContainer firstGui, IRecipeHandler handler, int recipeIndex) {
        final IngredientTracker tracker = new IngredientTracker(firstGui, handler, recipeIndex);
        final List<ItemOverlayState> itemPresenceSlots = new ArrayList<>();

        for (Ingredient ingredient : tracker.getIngredients()) {
            itemPresenceSlots.add(new NEEItemOverlayState(ingredient, false));
        }

        return itemPresenceSlots;
    }

    @Optional.Method(modid = ModIDs.FC)
    private void fluidCraftOverlayRecipe(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex, boolean shift) {
        FluidPatternTerminalRecipeTransferHandler.INSTANCE.overlayRecipe(firstGui, recipe, recipeIndex, shift);
    }

    private PacketNEIPatternRecipe packRecipe(IRecipeHandler recipe, int recipeIndex) {
        if (isCraftingTableRecipe(recipe)) {
            return packCraftingTableRecipe(recipe, recipeIndex);
        } else {
            return packProcessRecipe(recipe, recipeIndex);
        }
    }

    private boolean isCraftingTableRecipe(IRecipeHandler recipe) {
        if (recipe instanceof TemplateRecipeHandler) {
            TemplateRecipeHandler templateRecipeHandler = (TemplateRecipeHandler) recipe;
            String overlayIdentifier = templateRecipeHandler.getOverlayIdentifier();
            return "crafting".equals(overlayIdentifier) || "crafting2x2".equals(overlayIdentifier);
        } else {
            return false;
        }
    }

    private PacketNEIPatternRecipe packProcessRecipe(IRecipeHandler recipe, int recipeIndex) {
        final NBTTagCompound recipeInputs = new NBTTagCompound();
        final NBTTagCompound recipeOutputs = new NBTTagCompound();
        String identifier = recipe instanceof TemplateRecipeHandler ? recipe.getOverlayIdentifier() : NULL_IDENTIFIER;
        if (identifier == null) {
            identifier = NULL_IDENTIFIER;
        }
        int inputIndex = 0;
        int outputIndex = 0;

        // get all recipe inputs and other stacks,use first item
        for (IRecipeProcessor processor : RecipeProcessor.recipeProcessors) {
            if (processor.getAllOverlayIdentifier().contains(identifier)) {
                final List<PositionedStack> inputs = processor.getRecipeInput(recipe, recipeIndex, identifier);
                final List<PositionedStack> outputs = processor.getRecipeOutput(recipe, recipeIndex, identifier);
                final String recipeProcessorId = processor.getRecipeProcessorId();

                if (!inputs.isEmpty() && !outputs.isEmpty()) {
                    final List<PositionedStack> mergedInputs = getMergedInputs(
                            inputs,
                            processor,
                            recipe,
                            recipeIndex,
                            identifier);
                    NEEPatternTerminalHandler.ingredients.clear();

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

                        if (currentStack.stackSize < 1) {
                            currentStack.stackSize = 1;
                        }

                        recipeInputs.setTag(
                                INPUT_KEY + inputIndex,
                                ItemUtils.writeItemStackToNBT(currentStack, new NBTTagCompound()));
                        NEEPatternTerminalHandler.ingredients.put(INPUT_KEY + inputIndex, positionedStack);
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

                        if (outputStack.stackSize < 1) {
                            outputStack.stackSize = 1;
                        }

                        recipeOutputs.setTag(
                                OUTPUT_KEY + outputIndex,
                                ItemUtils.writeItemStackToNBT(outputStack, new NBTTagCompound()));
                        outputIndex++;
                    }

                    break;
                }
            }
        }

        return new PacketNEIPatternRecipe(recipeInputs, recipeOutputs);
    }

    private List<PositionedStack> getMergedInputs(List<PositionedStack> inputs, IRecipeProcessor processor,
            IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> mergedInputs = new ArrayList<>();

        for (PositionedStack positionedStack : inputs) {
            ItemStack currentStack = positionedStack.item;
            ItemCombination currentValue = ItemCombination.valueOf(NEEConfig.itemCombinationMode);
            boolean find = false;

            if (currentValue != ItemCombination.DISABLED && processor.mergeStacks(recipe, recipeIndex, identifier)) {
                boolean isWhitelist = currentValue == ItemCombination.WHITELIST
                        && Arrays.asList(NEEConfig.itemCombinationWhitelist).contains(identifier);

                if (currentValue == ItemCombination.ENABLED || isWhitelist) {
                    for (PositionedStack storedStack : mergedInputs) {
                        ItemStack firstStack = storedStack.items[0];
                        boolean areItemStackEqual = firstStack.isItemEqual(currentStack)
                                && ItemStack.areItemStackTagsEqual(firstStack, currentStack);

                        if (areItemStackEqual
                                && (firstStack.stackSize + currentStack.stackSize) <= firstStack.getMaxStackSize()) {
                            storedStack.items[0].stackSize = firstStack.stackSize + currentStack.stackSize;
                            find = true;
                        }
                    }
                }
            }

            if (!find) {
                mergedInputs.add(positionedStack.copy());
            }
        }

        return mergedInputs;
    }

    private PacketNEIPatternRecipe packCraftingTableRecipe(IRecipeHandler recipe, int recipeIndex) {
        final NBTTagCompound recipeInputs = new NBTTagCompound();
        final List<PositionedStack> ingredients = recipe.getIngredientStacks(recipeIndex);
        NEEPatternTerminalHandler.ingredients.clear();

        for (final PositionedStack positionedStack : ingredients) {
            final int col = (positionedStack.relx - 25) / 18;
            final int row = (positionedStack.rely - 6) / 18;
            int slotIndex = col + row * 3;

            if (positionedStack.items != null && positionedStack.items.length > 0) {
                final ItemStack[] currentStackList = positionedStack.items;
                ItemStack stack = positionedStack.item;

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

                recipeInputs.setTag(INPUT_KEY + slotIndex, ItemUtils.writeItemStackToNBT(stack, new NBTTagCompound()));
                NEEPatternTerminalHandler.ingredients.put(INPUT_KEY + slotIndex, positionedStack);
            }

        }

        return new PacketNEIPatternRecipe(recipeInputs, null);
    }

    @SubscribeEvent
    public void onActionPerformedEventPre(GuiOverlayButton.UpdateOverlayButtonsEvent.Post event) {
        if (event.gui instanceof GuiRecipe && isGuiPatternTerm((GuiRecipe<?>) event.gui)) {
            for (int i = 0; i < event.buttonList.size(); i++) {
                event.buttonList.set(i, new NEEGuiOverlayButton(event.buttonList.get(i)));
            }
        }
    }

    private boolean isGuiPatternTerm(GuiRecipe<?> gui) {
        return this.getClass().isInstance(gui.getHandler().getOverlayHandler(gui.firstGui, 0));
    }

}

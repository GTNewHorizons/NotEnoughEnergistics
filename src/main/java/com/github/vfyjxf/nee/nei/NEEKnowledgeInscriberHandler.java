package com.github.vfyjxf.nee.nei;

import static com.github.vfyjxf.nee.processor.RecipeProcessor.AspectRecipeIndex_isLoaded;
import static com.github.vfyjxf.nee.processor.RecipeProcessor.TCNEIPlugin_isLoaded;

import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.github.vfyjxf.nee.config.NEEConfig;
import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketArcaneRecipe;
import com.github.vfyjxf.nee.utils.Ingredient;
import com.github.vfyjxf.nee.utils.IngredientTracker;
import com.github.vfyjxf.nee.utils.ItemUtils;
import com.github.vfyjxf.nee.utils.ModIDs;
import com.gtnewhorizons.aspectrecipeindex.nei.arcaneworkbench.ArcaneSlotPositioner;
import com.gtnewhorizons.aspectrecipeindex.nei.arcaneworkbench.ShapedArcaneRecipeHandler;

import appeng.util.Platform;
import codechicken.nei.ItemsTooltipLineHandler;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiRecipeButton;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * @author vfyjxf
 */
public class NEEKnowledgeInscriberHandler implements IOverlayHandler {

    public static class NEEArcaneOverlayButton extends NEETerminalOverlayButton {

        public NEEArcaneOverlayButton(GuiOverlayButton button) {
            super(button.firstGui, button.handlerRef, button.xPosition, button.yPosition);
        }

        @Override
        protected List<ItemOverlayState> ingredientsOverlay() {
            List<PositionedStack> ingredients = this.handlerRef.handler
                    .getIngredientStacks(this.handlerRef.recipeIndex);

            if (this.itemPresenceCache.size() != ingredients.size()) {
                this.itemPresenceCache.clear();

                final IngredientTracker tracker = new IngredientTracker(
                        firstGui,
                        this.handlerRef.handler,
                        this.handlerRef.recipeIndex);

                for (Ingredient ingredient : tracker.getIngredients()) {
                    this.itemPresenceCache.add(new NEEItemOverlayState(ingredient, true));
                }

                List<ItemStack> items = this.itemPresenceCache.stream().filter(state -> !state.isPresent())
                        .map(state -> state.getSlot().item).collect(Collectors.toList());

                if (!items.isEmpty()) {
                    this.missedMaterialsTooltipLineHandler = new ItemsTooltipLineHandler(
                            NEIClientUtils.translate("recipe.overlay.missing"),
                            items,
                            true,
                            Integer.MAX_VALUE);
                } else {
                    this.missedMaterialsTooltipLineHandler = null;
                }
            }

            return this.itemPresenceCache;
        }

    }

    public static final NEEKnowledgeInscriberHandler instance = new NEEKnowledgeInscriberHandler();

    private Class<?> knowledgeInscriberClz;
    private Class<?> itemAspectClz;

    private NEEKnowledgeInscriberHandler() {

        try {
            knowledgeInscriberClz = Class.forName("thaumicenergistics.client.gui.GuiKnowledgeInscriber");
        } catch (ClassNotFoundException ignored) {}

        try {
            itemAspectClz = Class.forName("com.djgiannuzz.thaumcraftneiplugin.items.ItemAspect");
        } catch (ClassNotFoundException ignored) {}

    }

    @Override
    public void overlayRecipe(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex, boolean shift) {
        if (knowledgeInscriberClz != null && knowledgeInscriberClz.isInstance(firstGui)) {
            NEENetworkHandler.getInstance().sendToServer(packetArcaneRecipe(recipe, recipeIndex));
        }
    }

    private PacketArcaneRecipe packetArcaneRecipe(IRecipeHandler recipe, int recipeIndex) {
        final NBTTagCompound recipeInputs = new NBTTagCompound();
        final List<PositionedStack> ingredients = recipe.getIngredientStacks(recipeIndex);

        if (itemAspectClz != null) {
            // Aspect Recipe Index already does not include vis in the ingredients for arcane recipes so they do not
            // need to be removed.
            ingredients.removeIf(positionedStack -> itemAspectClz.isInstance(positionedStack.item.getItem()));
        }
        // Also applies to shapeless and wand handlers through inheritance
        if (AspectRecipeIndex_isLoaded && recipe instanceof ShapedArcaneRecipeHandler) {
            for (PositionedStack ps : ingredients) {
                addIndexedIngredient(ps, ArcaneSlotPositioner.getSlotIndex(ps), recipeInputs);
            }
        } else if (TCNEIPlugin_isLoaded) {
            for (PositionedStack ps : ingredients) {
                addIndexedIngredient(ps, getSlotIndex(ps.relx * 100 + ps.rely), recipeInputs);
            }
        }
        return new PacketArcaneRecipe(recipeInputs);
    }

    private static void addIndexedIngredient(PositionedStack ps, int i, NBTTagCompound recipeInputs) {
        if (ps.items != null && ps.items.length > 0) {
            ItemStack stack = getPrioritizedItem(ps.items);
            if (stack == null) return;
            recipeInputs.setTag("#" + i, ItemUtils.writeItemStackToNBT(stack, stack.stackSize));
        }
    }

    private static ItemStack getPrioritizedItem(ItemStack[] ps) {
        for (ItemStack currentStack : ps) {
            if (Platform.isRecipePrioritized(currentStack)) {
                return currentStack.copy();
            }
        }
        return ps[0];
    }

    private int getSlotIndex(int xy) {
        return switch (xy) {
            case 7533 -> 1;
            case 10333 -> 2;
            case 4960 -> 3;
            case 7660 -> 4;
            case 10360 -> 5;
            case 4987 -> 6;
            case 7687 -> 7;
            case 10387 -> 8;
            default -> 0;
        };
    }

    @SubscribeEvent
    public void onActionPerformedEventPost(GuiRecipeButton.UpdateRecipeButtonsEvent.Post event) {
        if (NEEConfig.noShift && event.gui instanceof GuiRecipe<?>guiRecipe) {
            if (isGuiArcaneCraftingTerm(guiRecipe)) {
                for (int i = 0; i < event.buttonList.size(); i++) {
                    if (event.buttonList.get(i) instanceof GuiOverlayButton btn) {
                        event.buttonList.set(i, new NEEArcaneOverlayButton(btn));
                    }
                }
            } else if (isGuiKnowledgeInscriber(guiRecipe)) {
                for (int i = 0; i < event.buttonList.size(); i++) {
                    if (event.buttonList.get(i) instanceof GuiOverlayButton btn) {
                        btn.setRequireShiftForOverlayRecipe(false);
                    }
                }
            }
        }

    }

    private boolean isGuiKnowledgeInscriber(GuiRecipe<?> gui) {
        return gui.firstGui != null && this.getClass().isInstance(gui.getHandler().getOverlayHandler(gui.firstGui, 0));
    }

    private boolean isGuiArcaneCraftingTerm(GuiRecipe<?> gui) {
        return Loader.isModLoaded(ModIDs.ThE)
                && gui.firstGui instanceof thaumicenergistics.client.gui.GuiArcaneCraftingTerminal;
    }

}

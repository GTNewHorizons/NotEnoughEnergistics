package com.github.vfyjxf.nee.nei;

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

    public class NEEArcaneOverlayButton extends NEETerminalOverlayButton {

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
            // "Knowledge Inscriber"
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
            ingredients.removeIf(positionedStack -> itemAspectClz.isInstance(positionedStack.item.getItem()));
        }

        for (PositionedStack positionedStack : ingredients) {
            if (positionedStack.items != null && positionedStack.items.length > 0) {
                final int slotIndex = getSlotIndex(positionedStack.relx * 100 + positionedStack.rely);
                final ItemStack[] currentStackList = positionedStack.items;
                ItemStack stack = positionedStack.item;

                for (ItemStack currentStack : currentStackList) {
                    if (Platform.isRecipePrioritized(currentStack)) {
                        stack = currentStack.copy();
                    }
                }

                recipeInputs.setTag("#" + slotIndex, ItemUtils.writeItemStackToNBT(stack, stack.stackSize));
            }
        }

        return new PacketArcaneRecipe(recipeInputs);
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

    @SubscribeEvent
    public void onActionPerformedEventPost(GuiRecipeButton.UpdateRecipeButtonsEvent.Post event) {

        if (NEEConfig.noShift && event.gui instanceof GuiRecipe guiRecipe) {

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

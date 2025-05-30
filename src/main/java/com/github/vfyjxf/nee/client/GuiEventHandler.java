package com.github.vfyjxf.nee.client;

import static com.github.vfyjxf.nee.config.NEEConfig.draggedStackDefaultSize;
import static com.github.vfyjxf.nee.config.NEEConfig.useStackSizeFromNEI;
import static com.github.vfyjxf.nee.nei.NEEPatternTerminalHandler.INPUT_KEY;
import static com.github.vfyjxf.nee.utils.GuiUtils.isPatternTerm;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;

import org.lwjgl.input.Mouse;

import com.github.vfyjxf.nee.client.gui.widgets.GuiImgButtonEnableCombination;
import com.github.vfyjxf.nee.config.ItemCombination;
import com.github.vfyjxf.nee.config.NEEConfig;
import com.github.vfyjxf.nee.nei.NEEPatternTerminalHandler;
import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketSlotStackChange;
import com.github.vfyjxf.nee.network.packet.PacketStackCountChange;
import com.github.vfyjxf.nee.network.packet.PacketValueConfigServer;
import com.github.vfyjxf.nee.utils.GuiUtils;
import com.github.vfyjxf.nee.utils.ItemUtils;
import com.github.vfyjxf.nee.utils.ModIDs;
import com.glodblock.github.client.gui.GuiLevelMaintainer;
import com.glodblock.github.client.gui.base.FCGuiEncodeTerminal;
import com.glodblock.github.client.gui.container.base.FCContainerEncodeTerminal;

import appeng.api.events.GuiScrollEvent;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiInterface;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.slot.SlotFake;
import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.INEIGuiAdapter;
import codechicken.nei.guihook.IContainerTooltipHandler;
import codechicken.nei.recipe.AcceptsFollowingTooltipLineHandler;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.RecipeInfo;
import codechicken.nei.util.NEIMouseUtils;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class GuiEventHandler extends INEIGuiAdapter implements IContainerTooltipHandler {

    public static final GuiEventHandler instance = new GuiEventHandler();
    private final GuiImgButtonEnableCombination buttonCombination = new GuiImgButtonEnableCombination(
            0,
            0,
            ItemCombination.ENABLED);

    private AcceptsFollowingTooltipLineHandler acceptsFollowingTooltipLineHandler;
    private List<GuiButton> buttonList;

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {

        if (isGuiPatternTerm(event.gui)) {
            event.buttonList.add(buttonCombination);
            this.buttonList = getButtonList((GuiContainer) event.gui, event.buttonList);
            updateCombinationButtonPosition((GuiContainer) event.gui);
        } else {
            this.buttonList = null;
        }

        if (event.gui instanceof GuiRecipe<?>) {
            NEENetworkHandler.getInstance().sendToServer(new PacketValueConfigServer("PatternInterface.check"));
        }
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {

        if (this.buttonList != null && isGuiPatternTerm(event.gui)) {
            updateCombinationButtonPosition((GuiContainer) event.gui);
        }
    }

    private boolean isGuiPatternTerm(GuiScreen guiScreen) {
        if (guiScreen instanceof GuiContainer guiContainer) {

            if (Loader.isModLoaded(ModIDs.FC) && guiContainer instanceof FCGuiEncodeTerminal) {
                return false;
            }

            return RecipeInfo.getOverlayHandler(guiContainer, "crafting") instanceof NEEPatternTerminalHandler
                    || RecipeInfo.getOverlayHandler(guiContainer, "smelting") instanceof NEEPatternTerminalHandler;
        }

        return false;
    }

    private List<GuiButton> getButtonList(GuiContainer guiContainer, List<Object> items) {
        final Rectangle buttonsArea = new Rectangle(
                guiContainer.guiLeft + 70,
                guiContainer.guiTop + guiContainer.ySize - 165,
                38,
                68);
        return items.stream().filter(btn -> btn instanceof GuiButton && btn != buttonCombination)
                .map(GuiButton.class::cast).filter(btn -> buttonsArea.contains(btn.xPosition, btn.yPosition))
                .collect(Collectors.toList());
    }

    private void updateCombinationButtonPosition(GuiContainer guiContainer) {
        final Point leftPoint = new Point(Integer.MAX_VALUE, 0);
        final Point rightPoint = new Point(0, 0);

        for (GuiButton button : buttonList) {
            if (button.visible) {
                leftPoint.x = Math.min(leftPoint.x, button.xPosition);
                rightPoint.x = Math.max(rightPoint.x, button.xPosition);
            }
        }

        for (GuiButton button : buttonList) {
            if (button.visible) {
                if (leftPoint.x == button.xPosition) {
                    leftPoint.y = Math.max(leftPoint.y, button.yPosition + button.height);
                }

                if (rightPoint.x == button.xPosition) {
                    rightPoint.y = Math.max(rightPoint.y, button.yPosition + button.height);
                }
            }
        }

        if (rightPoint.y < leftPoint.y) {
            buttonCombination.xPosition = rightPoint.x;
            buttonCombination.yPosition = leftPoint.y - buttonCombination.height;
        } else if (rightPoint.y == leftPoint.y) {
            buttonCombination.xPosition = leftPoint.x;
            buttonCombination.yPosition = leftPoint.y + 2;
        } else {
            buttonCombination.xPosition = leftPoint.x;
            buttonCombination.yPosition = rightPoint.y - buttonCombination.height;
        }

        buttonCombination.setValue(ItemCombination.valueOf(NEEConfig.itemCombinationMode));
        buttonCombination.enabled = buttonCombination.visible = guiContainer.inventorySlots instanceof AEBaseContainer
                && !isCraftingMode((AEBaseContainer) guiContainer.inventorySlots);
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.button == this.buttonCombination) {
            int ordinal = this.buttonCombination.getCurrentValue().ordinal() + (Mouse.getEventButton() == 0 ? 1 : -1);

            if (ordinal >= ItemCombination.values().length) {
                ordinal = 0;
            }
            if (ordinal < 0) {
                ordinal = ItemCombination.values().length - 1;
            }

            this.buttonCombination.setValue(ItemCombination.values()[ordinal]);
            NEEConfig.setItemCombinationMode(ItemCombination.values()[ordinal].name());
        }
    }

    private boolean isCraftingMode(AEBaseContainer container) {

        if (container instanceof ContainerPatternTerm) {
            return ((ContainerPatternTerm) container).isCraftingMode();
        } else if (Loader.isModLoaded(ModIDs.FC) && container instanceof FCContainerEncodeTerminal) {
            return ((FCContainerEncodeTerminal) container).isCraftingMode();
        }

        return false;
    }

    @Override
    public boolean handleDragNDrop(GuiContainer gui, int mouseX, int mouseY, ItemStack draggedStack, int button) {
        // When NEIAddons exist, give them to NEIAddons to handle
        if (Loader.isModLoaded("NEIAddons") && NEEConfig.useNEIDragFromNEIAddons) {
            return false;
        }

        if (NEEConfig.enableNEIDragDrop && Loader.isModLoaded("ae2fc") && gui instanceof GuiLevelMaintainer) {
            return false;
        }

        if (NEEConfig.enableNEIDragDrop && draggedStack != null && gui instanceof AEBaseGui) {
            Slot currentSlot = gui.getSlotAtPosition(mouseX, mouseY);

            if (currentSlot instanceof SlotFake) {
                ItemStack slotStack = currentSlot.getStack();
                ItemStack copyStack = draggedStack.copy();
                boolean sendPacket = false;
                int copySize = useStackSizeFromNEI ? copyStack.stackSize : draggedStackDefaultSize;
                if (button == 0) {
                    copyStack.stackSize = NEIServerUtils.areStacksSameTypeCraftingWithNBT(slotStack, copyStack)
                            ? slotStack.stackSize + copySize
                            : copySize;
                    sendPacket = true;
                } else if (button == 1) {
                    if (NEIServerUtils.areStacksSameTypeCraftingWithNBT(slotStack, copyStack)) {
                        copyStack.stackSize = slotStack.stackSize;
                    } else {
                        copyStack.stackSize = slotStack == null ? 1 : copySize;
                    }
                    sendPacket = true;
                }

                if (sendPacket) {
                    NEENetworkHandler.getInstance().sendToServer(
                            new PacketSlotStackChange(copyStack, Collections.singletonList(currentSlot.slotNumber)));
                    if (!NEEConfig.keepGhostitems) {
                        draggedStack.stackSize = 0;
                    }
                    return true;
                }
            }

            if (button == 2) {
                draggedStack.stackSize = 0;
            }
        }

        return false;
    }

    /**
     * Prevent the scroll bar from being triggered when modifying the number of items This method is not intended to be
     * called by NEE. Do not use this method for any reason.
     */
    @SubscribeEvent
    public boolean handleMouseWheelInput(GuiScrollEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        Slot currentSlot = getFakeSlotAtPosition((GuiContainer) mc.currentScreen, event.mouseX, event.mouseY);

        if (currentSlot != null) {
            // try to change current itemstack to next ingredient;
            if (NEIClientConfig.isKeyHashDown("nee.ingredient") && GuiUtils.isCraftingSlot(currentSlot)) {
                handleRecipeIngredientChange(event.guiScreen, currentSlot, event.scrollAmount);
                return true;
            } else if (NEIClientConfig.isKeyHashDown("nee.count")) {
                NEENetworkHandler.getInstance()
                        .sendToServer(new PacketStackCountChange(currentSlot.slotNumber, event.scrollAmount));
                return true;
            }
        }

        return false;
    }

    @Override
    public List<String> handleItemTooltip(GuiContainer gui, ItemStack itemstack, int mousex, int mousey,
            List<String> currenttip) {
        final Slot currentSlot = itemstack != null ? getFakeSlotAtPosition(gui, mousex, mousey) : null;

        if (currentSlot != null) {
            PositionedStack currentIngredients = NEEPatternTerminalHandler.ingredients
                    .get(INPUT_KEY + currentSlot.getSlotIndex());
            if (currentIngredients != null && currentIngredients.items.length > 1
                    && currentIngredients.containsWithNBT(currentSlot.getStack())) {

                if (this.acceptsFollowingTooltipLineHandler == null
                        || this.acceptsFollowingTooltipLineHandler.tooltipGUID != currentIngredients) {
                    this.acceptsFollowingTooltipLineHandler = AcceptsFollowingTooltipLineHandler.of(
                            currentIngredients,
                            currentIngredients.getFilteredPermutations(),
                            currentIngredients.item);
                }

                if (this.acceptsFollowingTooltipLineHandler != null) {
                    this.acceptsFollowingTooltipLineHandler.setActiveStack(currentIngredients.item);
                    currenttip.add(
                            GuiDraw.TOOLTIP_HANDLER + GuiDraw.getTipLineId(this.acceptsFollowingTooltipLineHandler));
                }
            }
        }

        return currenttip;
    }

    @Override
    public Map<String, String> handleHotkeys(GuiContainer gui, int mousex, int mousey, Map<String, String> hotkeys) {
        final Slot currentSlot = getFakeSlotAtPosition(gui, mousex, mousey);

        if (currentSlot != null) {

            if (this.acceptsFollowingTooltipLineHandler != null) {
                hotkeys.put(
                        NEIClientConfig.getKeyName(
                                "nee.ingredient",
                                0,
                                NEIMouseUtils.MOUSE_BTN_NONE + NEIMouseUtils.MOUSE_SCROLL),
                        I18n.format("neenergistics.gui.tooltip.ingredient.permutation"));
            }

            hotkeys.put(
                    NEIClientConfig
                            .getKeyName("nee.count", 0, NEIMouseUtils.MOUSE_BTN_NONE + NEIMouseUtils.MOUSE_SCROLL),
                    I18n.format("neenergistics.gui.tooltip.ingredient.count"));
        }

        return hotkeys;
    }

    private Slot getFakeSlotAtPosition(GuiContainer gui, int mousex, int mousey) {
        if (gui instanceof GuiInterface || isPatternTerm(gui)) {
            final Slot currentSlot = gui.getSlotAtPosition(mousex, mousey);
            if (currentSlot instanceof SlotFake && currentSlot.getHasStack()) {
                return currentSlot;
            }
        }

        return null;
    }

    private void handleRecipeIngredientChange(GuiContainer gui, Slot currentSlot, int dWheel) {
        final int currentSlotIndex = currentSlot.getSlotIndex();
        final PositionedStack currentIngredients = NEEPatternTerminalHandler.ingredients
                .get(INPUT_KEY + currentSlotIndex);

        if (currentIngredients != null && currentIngredients.items.length > 1) {
            final List<Integer> craftingSlots = new ArrayList<>();
            final List<ItemStack> items = currentIngredients.getFilteredPermutations();
            final int currentStackIndex = ItemUtils.getPermutationIndex(currentSlot.getStack(), items);
            final ItemStack nextStack = items.get((items.size() - dWheel + currentStackIndex) % items.size());
            final ItemStack currentStack = currentIngredients.item;

            if (NEEConfig.allowSynchronousSwitchIngredient) {
                for (Slot slot : getCraftingSlots(gui)) {
                    final PositionedStack slotIngredients = NEEPatternTerminalHandler.ingredients
                            .get(INPUT_KEY + slot.getSlotIndex());

                    if (slotIngredients != null && slotIngredients.containsWithNBT(nextStack)
                            && NEIServerUtils.areStacksSameTypeCraftingWithNBT(slotIngredients.item, currentStack)) {
                        slotIngredients.setPermutationToRender(nextStack);
                        craftingSlots.add(slot.slotNumber);
                    }
                }
            } else {
                currentIngredients.setPermutationToRender(nextStack);
                craftingSlots.add(currentSlot.slotNumber);
            }

            NEENetworkHandler.getInstance().sendToServer(new PacketSlotStackChange(nextStack, craftingSlots));
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Slot> getCraftingSlots(GuiContainer gui) {
        List<Slot> craftingSlots = new ArrayList<>();
        for (Slot slot : (List<Slot>) gui.inventorySlots.inventorySlots) {
            if (GuiUtils.isCraftingSlot(slot)) {
                craftingSlots.add(slot);
            }
        }
        return craftingSlots;
    }
}

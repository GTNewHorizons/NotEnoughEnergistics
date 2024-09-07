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
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.github.vfyjxf.nee.client.gui.widgets.GuiImgButtonEnableCombination;
import com.github.vfyjxf.nee.config.ItemCombination;
import com.github.vfyjxf.nee.config.NEEConfig;
import com.github.vfyjxf.nee.nei.NEEPatternTerminalHandler;
import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketSlotStackChange;
import com.github.vfyjxf.nee.network.packet.PacketStackCountChange;
import com.github.vfyjxf.nee.utils.GuiUtils;
import com.github.vfyjxf.nee.utils.ItemUtils;
import com.github.vfyjxf.nee.utils.ModIDs;
import com.glodblock.github.client.gui.container.base.FCContainerEncodeTerminal;
import com.glodblock.github.client.gui.base.FCGuiEncodeTerminal;

import appeng.api.events.GuiScrollEvent;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiInterface;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.slot.SlotFake;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.PositionedStack;
import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import codechicken.nei.recipe.RecipeInfo;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class GuiEventHandler implements INEIGuiHandler {

    public static final GuiEventHandler instance = new GuiEventHandler();
    private final GuiImgButtonEnableCombination buttonCombination = new GuiImgButtonEnableCombination(
            0,
            0,
            ItemCombination.ENABLED);

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
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {

        if (this.buttonList != null && isGuiPatternTerm(event.gui)) {
            updateCombinationButtonPosition((GuiContainer) event.gui);
        }
    }

    private boolean isGuiPatternTerm(GuiScreen guiScreen) {
        if (guiScreen instanceof GuiContainer) {
            final GuiContainer guiContainer = (GuiContainer) guiScreen;

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
            int ordinal = Mouse.getEventButton() != 2 ? this.buttonCombination.getCurrentValue().ordinal() + 1
                    : this.buttonCombination.getCurrentValue().ordinal() - 1;

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
    public VisiblityData modifyVisiblity(GuiContainer gui, VisiblityData currentVisibility) {
        return null;
    }

    @Override
    public Iterable<Integer> getItemSpawnSlots(GuiContainer gui, ItemStack item) {
        return null;
    }

    @Override
    public List<TaggedInventoryArea> getInventoryAreas(GuiContainer gui) {
        return null;
    }

    @Override
    public boolean handleDragNDrop(GuiContainer gui, int mouseX, int mouseY, ItemStack draggedStack, int button) {
        // When NEIAddons exist, give them to NEIAddons to handle
        if (Loader.isModLoaded("NEIAddons") && NEEConfig.useNEIDragFromNEIAddons) {
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
                    copyStack.stackSize = areStackEqual(slotStack, copyStack) ? slotStack.stackSize + copySize
                            : copySize;
                    sendPacket = true;
                } else if (button == 1) {
                    if (areStackEqual(slotStack, copyStack)) {
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

    private boolean areStackEqual(ItemStack stackA, ItemStack stackB) {
        return stackA != null && stackA.isItemEqual(stackB) && ItemStack.areItemStackTagsEqual(stackA, stackB);
    }

    @Override
    public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int w, int h) {
        return false;
    }

    /**
     * Prevent the scroll bar from being triggered when modifying the number of items This method is not intended to be
     * called by NEE. Do not use this method for any reason.
     */
    @SubscribeEvent
    public boolean handleMouseWheelInput(GuiScrollEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        boolean isPatternTerm = isPatternTerm(mc.currentScreen);
        boolean isInterface = event.guiScreen instanceof GuiInterface;
        if (isPatternTerm || isInterface) {
            Slot currentSlot = event.guiScreen.getSlotAtPosition(event.mouseX, event.mouseY);
            if (currentSlot instanceof SlotFake && currentSlot.getHasStack()) {
                // try to change current itemstack to next ingredient;
                if (Keyboard.isKeyDown(NEIClientConfig.getKeyBinding("nee.ingredient"))
                        && GuiUtils.isCraftingSlot(currentSlot)) {
                    handleRecipeIngredientChange(event.guiScreen, currentSlot, event.scrollAmount);
                    return true;
                } else if (Keyboard.isKeyDown(NEIClientConfig.getKeyBinding("nee.count"))) {
                    NEENetworkHandler.getInstance()
                            .sendToServer(new PacketStackCountChange(currentSlot.slotNumber, event.scrollAmount));
                    return true;
                }
            }
        }
        return false;
    }

    private static void handleRecipeIngredientChange(GuiContainer gui, Slot currentSlot, int dWheel) {
        List<Integer> craftingSlots = new ArrayList<>();
        int currentSlotIndex = currentSlot.getSlotIndex();
        PositionedStack currentIngredients = NEEPatternTerminalHandler.ingredients.get(INPUT_KEY + currentSlotIndex);
        if (currentIngredients != null && currentIngredients.items.length > 1) {
            int currentStackIndex = ItemUtils.getIngredientIndex(currentSlot.getStack(), currentIngredients);
            if (currentStackIndex >= 0) {
                for (int j = 0; j < Math.abs(dWheel); j++) {
                    currentStackIndex += (dWheel < 0) ? -1 : 1;

                    if (currentStackIndex >= currentIngredients.items.length) {
                        currentStackIndex = 0;
                    } else if (currentStackIndex < 0) {
                        currentStackIndex = currentIngredients.items.length - 1;
                    }

                    ItemStack currentStack = currentIngredients.items[currentStackIndex].copy();
                    currentStack.stackSize = currentSlot.getStack().stackSize;

                    if (NEEConfig.allowSynchronousSwitchIngredient) {
                        for (Slot slot : getCraftingSlots(gui)) {

                            PositionedStack slotIngredients = NEEPatternTerminalHandler.ingredients
                                    .get(INPUT_KEY + slot.getSlotIndex());

                            boolean areItemStackEqual = currentSlot.getHasStack() && slot.getHasStack()
                                    && currentSlot.getStack().isItemEqual(slot.getStack())
                                    && ItemStack.areItemStackTagsEqual(currentSlot.getStack(), slot.getStack());

                            boolean areIngredientEqual = slotIngredients != null
                                    && currentIngredients.contains(slotIngredients.items[0]);

                            if (areItemStackEqual && areIngredientEqual) {
                                craftingSlots.add(slot.slotNumber);
                            }
                        }
                    } else {
                        craftingSlots.add(currentSlot.slotNumber);
                    }
                    NEENetworkHandler.getInstance()
                            .sendToServer(new PacketSlotStackChange(currentStack, craftingSlots));
                }
            }
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

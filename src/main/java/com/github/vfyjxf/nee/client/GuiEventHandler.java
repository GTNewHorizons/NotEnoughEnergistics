package com.github.vfyjxf.nee.client;

import static com.github.vfyjxf.nee.config.NEEConfig.draggedStackDefaultSize;
import static com.github.vfyjxf.nee.config.NEEConfig.useStackSizeFromNEI;
import static com.github.vfyjxf.nee.nei.NEECraftingHelper.tracker;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.slot.SlotFake;
import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import com.github.vfyjxf.nee.client.gui.widgets.GuiImgButtonEnableCombination;
import com.github.vfyjxf.nee.config.ItemCombination;
import com.github.vfyjxf.nee.config.NEEConfig;
import com.github.vfyjxf.nee.container.ContainerCraftingConfirm;
import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketSlotStackChange;
import com.github.vfyjxf.nee.utils.GuiUtils;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.lwjgl.input.Mouse;

public class GuiEventHandler implements INEIGuiHandler {

    public static GuiEventHandler instance = new GuiEventHandler();

    private GuiImgButtonEnableCombination buttonCombination;
    private boolean hasDoubleBtn = true;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        GuiScreen old = Minecraft.getMinecraft().currentScreen;
        GuiScreen next = event.gui;
        if (old != null) {
            if (GuiUtils.isGuiCraftConfirm(old) && isContainerCraftConfirm(((GuiContainer) old).inventorySlots)) {
                if (tracker != null) {
                    if (GuiUtils.isGuiCraftingTerm(next)) {
                        if (tracker.hasNext()) {
                            tracker.requestNextIngredient();
                        } else {
                            tracker = null;
                        }
                    } else {
                        if (tracker != null) {
                            tracker = null;
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onCraftConfirmActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (tracker != null) {
            if (event.gui instanceof GuiCraftConfirm) {
                if (getCancelButton((GuiCraftConfirm) event.gui) == event.button) {
                    tracker = null;
                }
            }
        }
    }

    private GuiButton getCancelButton(GuiCraftConfirm gui) {
        return ObfuscationReflectionHelper.getPrivateValue(GuiCraftConfirm.class, gui, "cancel");
    }

    private boolean isContainerCraftConfirm(Container container) {
        return (container instanceof ContainerCraftConfirm || GuiUtils.isContainerWirelessCraftingConfirm(container))
                && !((container instanceof ContainerCraftingConfirm)
                        || (GuiUtils.isWCTContainerCraftingConfirm(container)));
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiPatternTerm) {
            GuiPatternTerm gui = (GuiPatternTerm) event.gui;
            try {
                GuiPatternTerm.class.getDeclaredField("doubleBtn");
            } catch (NoSuchFieldException e) {
                hasDoubleBtn = false;
            }
            if (hasDoubleBtn) {
                buttonCombination = new GuiImgButtonEnableCombination(
                        gui.guiLeft + 84,
                        gui.guiTop + gui.ySize - 153,
                        ItemCombination.valueOf(NEEConfig.itemCombinationMode));
            } else {
                buttonCombination = new GuiImgButtonEnableCombination(
                        gui.guiLeft + 74,
                        gui.guiTop + gui.ySize - 153,
                        ItemCombination.valueOf(NEEConfig.itemCombinationMode));
            }
            event.buttonList.add(buttonCombination);
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.button == this.buttonCombination) {
            GuiImgButtonEnableCombination button = (GuiImgButtonEnableCombination) event.button;
            int ordinal = Mouse.getEventButton() != 2
                    ? button.getCurrentValue().ordinal() + 1
                    : button.getCurrentValue().ordinal() - 1;

            if (ordinal >= ItemCombination.values().length) {
                ordinal = 0;
            }
            if (ordinal < 0) {
                ordinal = ItemCombination.values().length - 1;
            }
            button.setValue(ItemCombination.values()[ordinal]);
            NEEConfig.setItemCombinationMode(ItemCombination.values()[ordinal].name());
        }
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event.gui instanceof GuiPatternTerm) {
            ContainerPatternTerm container = (ContainerPatternTerm) ((GuiPatternTerm) event.gui).inventorySlots;
            if (container.isCraftingMode()) {
                buttonCombination.enabled = false;
                buttonCombination.visible = false;
            } else {
                buttonCombination.enabled = true;
                buttonCombination.visible = true;
            }
        }
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

        if (NEEConfig.enableNEIDragDrop) {
            if (gui instanceof AEBaseGui) {
                if (draggedStack != null) {
                    Slot currentSlot = gui.getSlotAtPosition(mouseX, mouseY);
                    if (currentSlot instanceof SlotFake) {
                        ItemStack slotStack = currentSlot.getStack();
                        ItemStack copyStack = draggedStack.copy();
                        boolean sendPacket = false;
                        int copySize = useStackSizeFromNEI ? copyStack.stackSize : draggedStackDefaultSize;
                        if (button == 0) {
                            boolean areStackEqual = slotStack != null
                                    && slotStack.isItemEqual(copyStack)
                                    && ItemStack.areItemStackTagsEqual(slotStack, copyStack);
                            copyStack.stackSize = areStackEqual
                                    ? Math.min(slotStack.stackSize + copySize, 127)
                                    : Math.min(copySize, 127);
                            sendPacket = true;
                        } else if (button == 1) {
                            boolean areStackEqual = slotStack != null
                                    && slotStack.isItemEqual(copyStack)
                                    && ItemStack.areItemStackTagsEqual(slotStack, copyStack);
                            if (areStackEqual) {
                                copyStack.stackSize = Math.min(slotStack.stackSize + 1, 127);
                            } else {
                                copyStack.stackSize = slotStack == null ? 1 : copySize;
                            }
                            sendPacket = true;
                        }

                        if (sendPacket) {
                            NEENetworkHandler.getInstance()
                                    .sendToServer(new PacketSlotStackChange(
                                            copyStack, Collections.singletonList(currentSlot.slotNumber)));
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
            }
        }
        return false;
    }

    @Override
    public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int w, int h) {
        return false;
    }
}

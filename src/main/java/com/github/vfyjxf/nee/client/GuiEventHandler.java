package com.github.vfyjxf.nee.client;

import static com.github.vfyjxf.nee.config.NEEConfig.draggedStackDefaultSize;
import static com.github.vfyjxf.nee.config.NEEConfig.useStackSizeFromNEI;
import static com.github.vfyjxf.nee.nei.NEEPatternTerminalHandler.INPUT_KEY;
import static com.github.vfyjxf.nee.utils.GuiUtils.isPatternTerm;

import java.awt.Point;
import java.awt.Rectangle;
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
import com.github.vfyjxf.nee.utils.ItemUtils;
import com.glodblock.github.client.gui.GuiLevelMaintainer;

import appeng.api.events.GuiScrollEvent;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiInterface;
import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.client.gui.slots.VirtualMEPatternSlot;
import appeng.client.gui.slots.VirtualMEPhantomSlot;
import appeng.client.gui.slots.VirtualMESlot;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.slot.SlotFake;
import appeng.util.item.AEItemStack;
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
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

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
                    Int2ObjectMap<IAEItemStack> map = new Int2ObjectOpenHashMap<IAEItemStack>();
                    map.put(currentSlot.slotNumber, AEItemStack.create(copyStack));
                    NEENetworkHandler.getInstance().sendToServer(new PacketSlotStackChange(map));
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

        if (mc.currentScreen instanceof GuiPatternTerm gpt
                && gpt.getVirtualMESlotUnderMouse() instanceof VirtualMEPatternSlot slot) {
            if (NEIClientConfig.isKeyHashDown("nee.ingredient")) {
                handleRecipeIngredientChange(gpt, slot, event.scrollAmount);
                return true;
            } else if (NEIClientConfig.isKeyHashDown("nee.count")) {
                NEENetworkHandler.getInstance().sendToServer(
                        new PacketStackCountChange(slot.getSlotIndex(), slot.getStorageName(), event.scrollAmount));
                return true;
            }
        }

        return false;
    }

    @Override
    public List<String> handleItemTooltip(GuiContainer gui, ItemStack itemstack, int mousex, int mousey,
            List<String> currenttip) {
        final VirtualMEPhantomSlot currentSlot = itemstack != null ? getPhantomSlotUnderMouse(gui) : null;

        if (currentSlot != null) {
            PositionedStack currentIngredients = NEEPatternTerminalHandler.ingredients
                    .get(INPUT_KEY + currentSlot.getSlotIndex());
            if (currentIngredients != null && currentIngredients.items.length > 1
                    && currentIngredients.containsWithNBT(itemstack)) {

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
        final VirtualMEPhantomSlot currentSlot = getPhantomSlotUnderMouse(gui);

        final boolean isPatternTerminal = isGuiPatternTerm(gui);

        if (currentSlot != null) {
            if (isPatternTerminal && this.acceptsFollowingTooltipLineHandler != null) {
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

    private VirtualMEPhantomSlot getPhantomSlotUnderMouse(GuiContainer gui) {
        if (gui instanceof GuiInterface || isPatternTerm(gui)) {
            final VirtualMESlot currentSlot = ((AEBaseGui) gui).getVirtualMESlotUnderMouse();
            if (currentSlot instanceof VirtualMEPhantomSlot phantomSlot && phantomSlot.getAEStack() != null) {
                return phantomSlot;
            }
        }

        return null;
    }

    private void handleRecipeIngredientChange(GuiPatternTerm gui, VirtualMEPatternSlot currentSlot, int dWheel) {
        final int currentSlotIndex = currentSlot.getSlotIndex();
        final PositionedStack baseIngredients = NEEPatternTerminalHandler.ingredients.get(INPUT_KEY + currentSlotIndex);

        if (baseIngredients != null && baseIngredients.items.length > 1) {
            final IAEStack<?> aes = currentSlot.getAEStack();
            if (aes == null) return;
            final ItemStack baseSlotStack = aes.getItemStackForNEI();
            if (baseSlotStack == null) return;
            final List<ItemStack> items = baseIngredients.getFilteredPermutations();
            final int currentStackIndex = ItemUtils.getPermutationIndex(baseSlotStack, items);
            final ItemStack nextStack = items.get((items.size() - dWheel + currentStackIndex) % items.size()).copy();

            final Int2ObjectMap<IAEItemStack> craftingSlotss = new Int2ObjectOpenHashMap<>();
            if (NEEConfig.allowSynchronousSwitchIngredient) {
                final ItemStack baseStack = baseIngredients.item;
                for (VirtualMEPatternSlot slot : gui.getCraftingSlots()) {
                    IAEStack<?> slotAEStack = slot.getAEStack();
                    if (!(slotAEStack instanceof IAEItemStack ais)) continue;
                    final ItemStack slotStack = ais.getItemStackForNEI();
                    if (slotStack == null) continue;

                    final PositionedStack slotIngredients = NEEPatternTerminalHandler.ingredients
                            .get(INPUT_KEY + slot.getSlotIndex());

                    if (slotIngredients != null && slotIngredients.containsWithNBT(nextStack)
                            && NEIServerUtils.areStacksSameTypeCraftingWithNBT(slotStack, baseStack)
                            && NEIServerUtils.areStacksSameTypeCraftingWithNBT(slotIngredients.item, baseStack)) {

                        // If the current slot's stack size is a multiple of the recipe's default, apply that multiplier
                        // to nextStack.
                        long nextStackAmount;
                        if (slotStack.stackSize % baseStack.stackSize == 0) {
                            nextStackAmount = (long) nextStack.stackSize * (slotStack.stackSize / baseStack.stackSize);
                        } else {
                            nextStackAmount = slotAEStack.getStackSize();
                        }

                        craftingSlotss
                                .put(slot.getSlotIndex(), AEItemStack.create(nextStack).setStackSize(nextStackAmount));
                        slotIngredients.setPermutationToRender(nextStack);
                    }
                }
            } else {
                // If the current slot's stack size is a multiple of the recipe's default, apply that multiplier to
                // nextStack.
                long nextStackAmount;
                if (baseSlotStack.stackSize % baseIngredients.item.stackSize == 0) {
                    nextStackAmount = (long) nextStack.stackSize
                            * (baseSlotStack.stackSize / baseIngredients.item.stackSize);
                } else {
                    nextStackAmount = aes.getStackSize();
                }

                craftingSlotss
                        .put(currentSlot.getSlotIndex(), AEItemStack.create(nextStack).setStackSize(nextStackAmount));
                baseIngredients.setPermutationToRender(nextStack);
            }

            NEENetworkHandler.getInstance().sendToServer(new PacketSlotStackChange(craftingSlotss));
        }
    }
}

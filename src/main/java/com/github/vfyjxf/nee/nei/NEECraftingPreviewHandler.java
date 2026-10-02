package com.github.vfyjxf.nee.nei;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.oredict.OreDictionary;

import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketCraftingRequest;
import com.github.vfyjxf.nee.utils.GuiUtils;
import com.github.vfyjxf.nee.utils.Ingredient;
import com.github.vfyjxf.nee.utils.IngredientTracker;
import com.github.vfyjxf.nee.utils.ItemUtils;
import com.github.vfyjxf.nee.utils.ModIDs;

import appeng.api.storage.data.IAEStack;
import appeng.client.gui.implementations.GuiAmount;
import appeng.client.gui.implementations.GuiCraftAmount;
import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerNull;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.helpers.InventoryAction;
import appeng.util.Platform;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

public class NEECraftingPreviewHandler {

    public static final NEECraftingPreviewHandler instance = new NEECraftingPreviewHandler();

    private boolean isPatternInterfaceExists = false;

    private NBTTagCompound patternCompound = null;
    private IngredientTracker tracker = null;
    private boolean isAutoStart = false;
    private boolean isRequesting = false;
    private long resultStackSize = 0;
    private String modID = "";

    private NEECraftingPreviewHandler() {}

    public boolean handle(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex) {
        final PositionedStack pStack = recipe.getResultStack(recipeIndex);
        final IAEStack<?> aeStack = pStack != null ? ItemUtils.toAEStack(pStack.item) : null;
        this.isAutoStart = NEIClientConfig.isKeyHashDown("nee.nopreview");
        this.modID = getModID(firstGui.inventorySlots);
        this.resultStackSize = 0;
        this.patternCompound = null;
        this.isRequesting = false;
        this.tracker = null;

        if ((this.isAutoStart || NEIClientConfig.isKeyHashDown("nee.preview")) && !this.modID.isEmpty()) {
            firstGui.mc.displayGuiScreen(firstGui);

            if (aeStack != null) {
                this.resultStackSize = aeStack.getStackSize();
            }

            if (aeStack != null && existsRecipeResult(firstGui, aeStack)) {

                if (this.isAutoStart) {
                    final PacketCraftingRequest craftingRequest = new PacketCraftingRequest(
                            this.modID,
                            PacketCraftingRequest.COMMAND_OPEN_CRAFT_CONFIRM,
                            aeStack.toNBTGeneric(),
                            aeStack.getStackSize(),
                            true);
                    NEENetworkHandler.getInstance().sendToServer(craftingRequest);
                } else {
                    openCraftAmount(firstGui, aeStack);
                }

                return true;
            } else if (aeStack != null && this.isPatternInterfaceExists && isCraftingTableRecipe(recipe)) {
                this.patternCompound = getPatternStack(recipe, recipeIndex, Minecraft.getMinecraft().theWorld);

                if (this.isAutoStart) {
                    final PacketCraftingRequest craftingRequest = new PacketCraftingRequest(
                            this.modID,
                            PacketCraftingRequest.COMMAND_CREATE_PATTERN,
                            this.patternCompound,
                            aeStack.getStackSize(),
                            true);
                    NEENetworkHandler.getInstance().sendToServer(craftingRequest);
                } else {
                    openCraftAmount(firstGui, aeStack);
                }

                return true;
            } else {

                this.tracker = new IngredientTracker(firstGui, recipe, recipeIndex);

                if (this.tracker.hasNext()) {

                    if (this.isAutoStart) {
                        requestNextIngredient();
                    } else if (aeStack != null) {
                        openCraftAmount(firstGui, aeStack);
                    } else {
                        PositionedStack otherStack = null;

                        for (PositionedStack positionedStack : recipe.getOtherStacks(recipeIndex)) {
                            otherStack = positionedStack;
                            break;
                        }

                        if (otherStack != null) {
                            IAEStack<?> aeOtherStack = ItemUtils.toAEStack(otherStack.item);
                            this.resultStackSize = aeOtherStack.getStackSize();
                            openCraftAmount(firstGui, aeOtherStack);
                        } else {
                            return false;
                        }
                    }

                    return true;
                } else {
                    this.tracker = null;
                    return false;
                }

            }

        }

        return false;
    }

    private void openCraftAmount(GuiContainer firstGui, IAEStack<?> stack) {
        if (stack != null && firstGui.inventorySlots instanceof AEBaseContainer baseContainer) {
            baseContainer.setTargetStack(stack);
            NetworkHandler.instance.sendToServer(new PacketInventoryAction(InventoryAction.AUTO_CRAFT, 0, 0));
        }
    }

    @SubscribeEvent
    public void onGuiCraftConfirmOpen(GuiOpenEvent event) {
        if (this.patternCompound == null && this.tracker == null) {
            return;
        }

        if (!this.isRequesting && event.gui instanceof GuiCraftAmount) {
            this.isRequesting = true;
            return;
        }

        final GuiScreen old = Minecraft.getMinecraft().currentScreen;

        if (this.isRequesting && !(event.gui instanceof GuiCraftConfirm) && !(old instanceof GuiCraftConfirm)) {
            removePatternIfNeeded();
            this.isRequesting = false;
            this.tracker = null;
            return;
        }

        if (this.patternCompound != null && old instanceof GuiCraftConfirm) {
            removePatternIfNeeded();
            this.isRequesting = false;
        }

        if (this.tracker != null && old instanceof GuiCraftConfirm) {
            if (this.tracker.hasNext()) {
                requestNextIngredient();
            } else {
                this.tracker = null;
                this.isRequesting = false;
            }
        }
    }

    @SubscribeEvent
    public void onCraftConfirmActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {

        if ((this.tracker != null || this.patternCompound != null) && event.gui instanceof GuiContainer gui) {
            final long craftAmount = getCraftAmount(gui, event.button);

            if (craftAmount == -1 || this.resultStackSize <= 0) {
                return;
            }

            if (this.tracker != null) {
                final long craftMultiplier = (craftAmount + this.resultStackSize - 1) / this.resultStackSize;
                this.isAutoStart = this.isAutoStart || GuiScreen.isShiftKeyDown();

                try {
                    for (Ingredient ingr : this.tracker.getIngredients()) {
                        ingr.setRequireCount(Math.multiplyExact(ingr.getDefaultRequireCount(), craftMultiplier));
                    }
                } catch (ArithmeticException e) {
                    this.tracker = null;
                    this.isRequesting = false;
                    event.setCanceled(true);
                    return;
                }

                this.tracker.calculateIngredients();

                if (this.tracker.hasNext()) {
                    requestNextIngredient();
                    event.setCanceled(true);
                } else {
                    this.tracker = null;
                    this.isRequesting = false;
                }

            } else if (this.patternCompound != null) {
                PacketCraftingRequest craftingRequest = new PacketCraftingRequest(
                        this.modID,
                        PacketCraftingRequest.COMMAND_CREATE_PATTERN,
                        this.patternCompound,
                        craftAmount,
                        GuiScreen.isShiftKeyDown());
                NEENetworkHandler.getInstance().sendToServer(craftingRequest);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onCraftConfirmActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {

        if ((this.tracker != null || this.patternCompound != null) && event.gui instanceof GuiCraftConfirm guiConfirm
                && getCancelButton(guiConfirm) == event.button) {
            removePatternIfNeeded();
            this.isRequesting = false;
            this.tracker = null;
        }
    }

    private static GuiButton getCancelButton(GuiCraftConfirm gui) {
        return ReflectionHelper.getPrivateValue(GuiCraftConfirm.class, gui, "cancel");
    }

    private void removePatternIfNeeded() {
        if (this.patternCompound != null) {
            PacketCraftingRequest craftingRequest = new PacketCraftingRequest(
                    this.modID,
                    PacketCraftingRequest.COMMAND_REMOVE_PATTERN,
                    this.patternCompound,
                    0,
                    false);
            NEENetworkHandler.getInstance().sendToServer(craftingRequest);
            this.patternCompound = null;
        }
    }

    private static long getCraftAmount(GuiContainer screen, GuiButton button) {

        try {
            if (screen instanceof GuiCraftAmount gui) {

                if (ReflectionHelper.getPrivateValue(GuiAmount.class, gui, "nextBtn") == button) {
                    return (long) ReflectionHelper.findMethod(GuiAmount.class, gui, new String[] { "getAmountLong" })
                            .invoke(gui);
                }

            }
        } catch (Exception ex) {
            return 1;
        }

        return -1;
    }

    private static String getModID(Container container) {

        if (Loader.isModLoaded(ModIDs.ThE)
                && container.getClass().getName().startsWith("thaumicenergistics.common.container")) {
            return ModIDs.ThE;
        } else if (Loader.isModLoaded(ModIDs.FC)
                && container.getClass().getName().startsWith("com.glodblock.github.client.gui.container")) {
                    return ModIDs.FC;
                } else
            if (Loader.isModLoaded(ModIDs.WCT) && container.getClass().getName()
                    .startsWith("net.p455w0rd.wirelesscraftingterminal.common.container")) {
                        return ModIDs.WCT;
                    } else
                if (container instanceof AEBaseContainer) {
                    return "AE";
                }

        return "";
    }

    private void requestNextIngredient() {
        final IAEStack<?> stack = this.tracker.getNextIngredient();

        if (stack != null) {
            PacketCraftingRequest craftingRequest = new PacketCraftingRequest(
                    this.modID,
                    PacketCraftingRequest.COMMAND_OPEN_CRAFT_CONFIRM,
                    stack.toNBTGeneric(),
                    stack.getStackSize(),
                    this.isAutoStart);
            NEENetworkHandler.getInstance().sendToServer(craftingRequest);
        }
    }

    private boolean isCraftingTableRecipe(IRecipeHandler recipe) {
        if (recipe instanceof TemplateRecipeHandler templateRecipeHandler) {
            String overlayIdentifier = templateRecipeHandler.getOverlayIdentifier();
            return "crafting".equals(overlayIdentifier) || "crafting2x2".equals(overlayIdentifier);
        } else {
            return false;
        }
    }

    private NBTTagCompound getPatternStack(IRecipeHandler recipeHandler, int recipeIndex, World world) {
        final InventoryCrafting ic = new InventoryCrafting(new ContainerNull(), 3, 3);
        final List<PositionedStack> ingredients = recipeHandler.getIngredientStacks(recipeIndex);

        for (final PositionedStack positionedStack : ingredients) {
            final int col = (positionedStack.relx - 25) / 18;
            final int row = (positionedStack.rely - 6) / 18;
            final int slotIndex = col + row * 3;

            if (positionedStack.items != null && positionedStack.items.length > 0) {
                final ItemStack[] currentStackList = positionedStack.items;
                ItemStack stack = positionedStack.items[0];

                final ItemStack preferModItem = ItemUtils.getPreferModItem(positionedStack.items);

                if (preferModItem != null) {
                    stack = preferModItem;
                }

                for (ItemStack currentStack : currentStackList) {
                    if (Platform.isRecipePrioritized(currentStack) || ItemUtils.isPreferItems(currentStack)) {
                        stack = currentStack.copy();
                    }
                }

                if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                    stack.setItemDamage(0);
                }

                ic.setInventorySlotContents(slotIndex, stack);
            } else {
                ic.setInventorySlotContents(slotIndex, null);
            }
        }

        final ItemStack result = CraftingManager.getInstance().findMatchingRecipe(ic, world);

        if (result != null) {
            final NBTTagCompound patternValue = new NBTTagCompound();
            final NBTTagList tagIn = new NBTTagList();

            for (int slotIndex = 0; slotIndex < ic.getSizeInventory(); slotIndex++) {
                final ItemStack itemStack = ic.getStackInSlot(slotIndex);

                if (itemStack != null) {
                    tagIn.appendTag(itemStack.writeToNBT(new NBTTagCompound()));
                } else {
                    tagIn.appendTag(new NBTTagCompound());
                }
            }

            patternValue.setTag("in", tagIn);
            patternValue.setTag("out", result.writeToNBT(new NBTTagCompound()));
            patternValue.setBoolean("crafting", true);
            patternValue.setBoolean("substitute", false);

            return patternValue;
        }

        return null;
    }

    public void setIsPatternInterfaceExists(boolean isPatternInterfaceExists) {
        this.isPatternInterfaceExists = isPatternInterfaceExists;
    }

    public boolean canCraftRecipeResult(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex) {
        final PositionedStack pStack = recipe.getResultStack(recipeIndex);

        if (pStack == null) {
            return false;
        }

        if (this.isPatternInterfaceExists && isCraftingTableRecipe(recipe)) {
            return true;
        }

        return existsRecipeResult(firstGui, ItemUtils.toAEStack(pStack.item));
    }

    private boolean existsRecipeResult(GuiContainer firstGui, IAEStack<?> stack) {
        return stack != null
                && !GuiUtils.getStorageStacks(firstGui, s -> s.isCraftable() && s.isSameType(stack)).isEmpty();
    }

}

package com.github.vfyjxf.nee.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;

import appeng.api.storage.ITerminalHost;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.util.Platform;

import com.github.vfyjxf.nee.block.tile.TilePatternInterface;

public class WCTContainerCraftingConfirm extends ContainerCraftConfirm {

    private TilePatternInterface tile;
    private int patternIndex;
    private boolean hasWorkCommitted = false;

    public WCTContainerCraftingConfirm(InventoryPlayer ip, ITerminalHost te) {
        super(ip, te);
    }

    @Override
    public void startJob() {
        if (Platform.isServer()) {
            hasWorkCommitted = true;
        }
        super.startJob();
    }

    @Override
    public void onContainerClosed(EntityPlayer par1EntityPlayer) {
        super.onContainerClosed(par1EntityPlayer);
        if (Platform.isServer() && tile != null && !hasWorkCommitted) {
            this.tile.getPatternInventory().setInventorySlotContents(patternIndex, null);
            this.tile.updateCraftingList();
        }
    }

    public void setTile(TilePatternInterface tile) {
        this.tile = tile;
    }

    public void setPatternIndex(int patternIndex) {
        this.patternIndex = patternIndex;
    }
}

package com.github.vfyjxf.nee.proxy;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import com.github.vfyjxf.nee.NEECommands;
import com.github.vfyjxf.nee.client.GuiEventHandler;
import com.github.vfyjxf.nee.nei.NEECraftingPreviewHandler;
import com.github.vfyjxf.nee.nei.NEECraftingTerminalHandler;
import com.github.vfyjxf.nee.nei.NEEExtremeAutoCrafterHandler;
import com.github.vfyjxf.nee.nei.NEEKnowledgeInscriberHandler;
import com.github.vfyjxf.nee.nei.NEEPatternTerminalHandler;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ClientCommandHandler.instance.registerCommand(new NEECommands());
        FMLCommonHandler.instance().bus().register(GuiEventHandler.instance);
        MinecraftForge.EVENT_BUS.register(GuiEventHandler.instance);
        MinecraftForge.EVENT_BUS.register(NEECraftingTerminalHandler.instance);
        MinecraftForge.EVENT_BUS.register(NEEPatternTerminalHandler.instance);
        MinecraftForge.EVENT_BUS.register(NEEExtremeAutoCrafterHandler.instance);
        MinecraftForge.EVENT_BUS.register(NEEKnowledgeInscriberHandler.instance);
        MinecraftForge.EVENT_BUS.register(NEECraftingPreviewHandler.instance);
    }
}

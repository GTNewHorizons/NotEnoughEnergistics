package com.github.vfyjxf.nee.proxy;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import com.github.vfyjxf.nee.NEECommands;
import com.github.vfyjxf.nee.client.GuiEventHandler;
import com.github.vfyjxf.nee.client.NEEContainerDrawHandler;
import com.github.vfyjxf.nee.config.NEEConfig;
import com.github.vfyjxf.nee.nei.NEECraftingHelper;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ClientCommandHandler.instance.registerCommand(new NEECommands());
        FMLCommonHandler.instance().bus().register(GuiEventHandler.instance);
        MinecraftForge.EVENT_BUS.register(GuiEventHandler.instance);
        if (NEEConfig.noShift) {
            MinecraftForge.EVENT_BUS.register(NEECraftingHelper.INSTANCE);
        }
        if (NEEConfig.drawHighlight) {
            MinecraftForge.EVENT_BUS.register(NEEContainerDrawHandler.instance);
        }
    }
}

package com.github.vfyjxf.nee.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;

import com.github.vfyjxf.nee.NotEnoughEnergistics;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;

public class NEEConfigGui extends GuiConfig {

    public NEEConfigGui(GuiScreen parentScreen) {
        super(parentScreen, getConfigElements(), NotEnoughEnergistics.MODID, false, false, NotEnoughEnergistics.NAME);
    }

    private static List<IConfigElement> getConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        for (String name : NEEConfig.config.getCategoryNames()) {
            list.add(new ConfigElement(NEEConfig.config.getCategory(name)));
        }
        return list;
    }
}

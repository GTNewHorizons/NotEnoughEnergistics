package com.github.vfyjxf.nee;

import static com.github.vfyjxf.nee.NotEnoughEnergistics.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import com.github.vfyjxf.nee.config.NEEConfig;
import com.github.vfyjxf.nee.processor.IRecipeProcessor;
import com.github.vfyjxf.nee.processor.RecipeProcessor;
import com.github.vfyjxf.nee.utils.ItemUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class NEECommands extends CommandBase {

    @Override
    public String getCommandName() {
        return "nee";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "See /nee help";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 0) {
            if ("help".equalsIgnoreCase(args[0])) {
                sender.addChatMessage(new ChatComponentText("Usage:"));
                sender.addChatMessage(new ChatComponentText("/nee add blacklist/priorityItem processor_id identifier"));
                sender.addChatMessage(
                        new ChatComponentText("/nee add blacklist/priorityItem/itemCombination identifier"));
            } else if ("processor".equalsIgnoreCase(args[0])) {
                for (IRecipeProcessor processor : RecipeProcessor.recipeProcessors) {
                    logger.info("RecipeProcessor:" + processor.getRecipeProcessorId() + "  identifier:");
                    for (String ident : processor.getAllOverlayIdentifier()) {
                        logger.info(ident);
                    }
                }
            } else if ("add".equalsIgnoreCase(args[0]) && args.length > 1) {
                if ("blacklist".equalsIgnoreCase(args[1]) || "priorityItem".equalsIgnoreCase(args[1])) {
                    ItemStack currentStack = Minecraft.getMinecraft().thePlayer.getCurrentEquippedItem();
                    if (currentStack != null) {
                        String currentItemJsonString = ItemUtils.toItemJsonString(currentStack);
                        JsonObject itemJsonObject = new JsonParser().parse(currentItemJsonString).getAsJsonObject();
                        if (args.length >= 3) {
                            boolean hasRecipeProcessor = ItemUtils.hasRecipeProcessor(args[2]);
                            boolean hasOverlayIdentifier = ItemUtils.hasOverlayIdentifier(args[2]);

                            if (args.length == 4) {
                                hasOverlayIdentifier = ItemUtils.hasOverlayIdentifier(args[3]);
                            }

                            if (hasRecipeProcessor) {
                                itemJsonObject.addProperty("recipeProcessor", args[2]);
                            } else if (!hasOverlayIdentifier) {
                                sender.addChatMessage(new ChatComponentText("Can't find processor: " + args[2]));
                            }

                            if (hasOverlayIdentifier) {
                                itemJsonObject.addProperty("identifier", args.length == 4 ? args[3] : args[2]);
                            } else if (!hasRecipeProcessor) {
                                sender.addChatMessage(new ChatComponentText("Can't find identifier: " + args[2]));
                            }
                        }
                        String newJsonString = new Gson().toJson(itemJsonObject);
                        String[] oldList = "blacklist".equalsIgnoreCase(args[1]) ? NEEConfig.transformBlacklist
                                : NEEConfig.transformPriorityList;
                        List<String> newList = new ArrayList<>(Arrays.asList(oldList));
                        for (String currentJsonString : oldList) {
                            if (currentJsonString.equals(newJsonString)) {
                                return;
                            }
                        }
                        newList.add(newJsonString);
                        if ("blacklist".equalsIgnoreCase(args[1])) {
                            NEEConfig.setTransformBlacklist(newList.toArray(new String[0]));
                        } else {
                            NEEConfig.setTransformPriorityList(newList.toArray(new String[0]));
                        }
                    }
                } else if ("priorityMod".equalsIgnoreCase(args[1]) && args.length == 3) {
                    String modid = args[2];
                    if (!ItemUtils.hasModId(modid)) {
                        List<String> newList = new ArrayList<>(Arrays.asList(NEEConfig.transformPriorityModList));
                        newList.add(modid);
                        NEEConfig.setTransformPriorityModList(newList.toArray(new String[0]));
                    }
                } else if ("itemCombination".equalsIgnoreCase(args[1]) && args.length == 3) {
                    String identifier = args[2];
                    if (!Arrays.asList(NEEConfig.itemCombinationWhitelist).contains(identifier)) {
                        if (ItemUtils.hasOverlayIdentifier(args[2])) {
                            List<String> newLists = new ArrayList<>(Arrays.asList(NEEConfig.itemCombinationWhitelist));
                            newLists.add(identifier);
                            NEEConfig.setItemCombinationWhitelist(newLists.toArray(new String[0]));
                        } else {
                            sender.addChatMessage(new ChatComponentText("Can't find identifier: " + args[2]));
                        }
                    }
                }
            } else if ("reload".equalsIgnoreCase(args[0])) {
                NEEConfig.reload();
            }
        } else {
            throw new WrongUsageException("");
        }
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        return args.length <= 1
                ? CommandBase.getListOfStringsMatchingLastWord(args, "help", "reload", "add", "processor")
                : args.length == 2 ? CommandBase.getListOfStringsMatchingLastWord(
                        args,
                        "blacklist",
                        "priorityItem",
                        "priorityMod",
                        "itemCombination") : null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 1;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}

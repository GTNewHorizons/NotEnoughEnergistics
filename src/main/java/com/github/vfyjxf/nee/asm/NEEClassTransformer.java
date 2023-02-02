package com.github.vfyjxf.nee.asm;

import static org.objectweb.asm.Opcodes.*;

import java.util.Arrays;
import java.util.List;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import codechicken.lib.asm.ASMInit;
import codechicken.lib.asm.ObfMapping;

import com.github.vfyjxf.nee.NotEnoughEnergistics;

public class NEEClassTransformer implements IClassTransformer {

    static {
        ASMInit.init();
    }

    private static final List<String> transformedClassNames = Arrays.asList(
            "appeng/integration/modules/NEIHelpers/NEIInscriberRecipeHandler",
            "appeng/integration/modules/NEIHelpers/NEIGrinderRecipeHandler");

    private static final String TARGET_CLASS_NAME = "appeng/client/gui/AEBaseGui";

    private static final ObfMapping TARGET_METHOD_MAPPING = new ObfMapping(TARGET_CLASS_NAME, "func_146274_d", "()V")
            .toClassloading();

    private static final String METHOD_OWNER = "codechicken/nei/recipe/TemplateRecipeHandler";

    private static final String METHOD_NAME_1 = "getOverlayHandler";
    private static final String METHOD_NAME_2 = "hasOverlay";

    private static final String METHOD_TARGET_1 = "(Lnet/minecraft/client/gui/inventory/GuiContainer;I)Lcodechicken/nei/api/IOverlayHandler;";
    private static final String METHOD_TARGET_2 = "(Lnet/minecraft/client/gui/inventory/GuiContainer;Lnet/minecraft/inventory/Container;I)Z";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {

        String internalName = transformedName.replace('.', '/');

        // rewrite ae2's methods to support recipe transfer.
        if (transformedClassNames.contains(internalName)) {
            ClassNode classNode = createClassNode(basicClass);
            for (MethodNode methodNode : classNode.methods) {
                if (METHOD_NAME_1.equals(methodNode.name) && METHOD_TARGET_1.equals(methodNode.desc)) {
                    NotEnoughEnergistics.logger
                            .info("Transforming : " + internalName + methodNode.name + methodNode.desc);
                    for (AbstractInsnNode instruction : methodNode.instructions.toArray()) {
                        if (instruction.getOpcode() == ACONST_NULL) {
                            InsnList insnList = new InsnList();
                            insnList.add(new VarInsnNode(ALOAD, 0));
                            insnList.add(new VarInsnNode(ALOAD, 1));
                            insnList.add(new VarInsnNode(ILOAD, 2));
                            insnList.add(
                                    new MethodInsnNode(
                                            INVOKESPECIAL,
                                            METHOD_OWNER,
                                            METHOD_NAME_1,
                                            METHOD_TARGET_1,
                                            false));
                            methodNode.instructions.insert(instruction, insnList);
                            methodNode.instructions.remove(instruction);
                        }
                    }
                }

                if (METHOD_NAME_2.equals(methodNode.name) && METHOD_TARGET_2.equals(methodNode.desc)) {
                    NotEnoughEnergistics.logger
                            .info("Transforming : " + internalName + methodNode.name + methodNode.desc);
                    for (AbstractInsnNode instruction : methodNode.instructions.toArray()) {
                        if (instruction.getOpcode() == ICONST_0) {
                            InsnList insnList = new InsnList();
                            insnList.add(new VarInsnNode(ALOAD, 0));
                            insnList.add(new VarInsnNode(ALOAD, 1));
                            insnList.add(new VarInsnNode(ALOAD, 2));
                            insnList.add(new VarInsnNode(ILOAD, 3));
                            insnList.add(
                                    new MethodInsnNode(
                                            INVOKESPECIAL,
                                            METHOD_OWNER,
                                            METHOD_NAME_2,
                                            METHOD_TARGET_2,
                                            false));
                            methodNode.instructions.insert(instruction, insnList);
                            methodNode.instructions.remove(instruction);
                        }
                    }
                }
            }
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            return classWriter.toByteArray();
        }

        if (TARGET_CLASS_NAME.equals(internalName)) {
            ClassNode classNode = createClassNode(basicClass);
            for (MethodNode methodNode : classNode.methods) {
                if (TARGET_METHOD_MAPPING.matches(methodNode)) {
                    NotEnoughEnergistics.logger
                            .info("Transforming : " + internalName + methodNode.name + methodNode.desc);
                    AbstractInsnNode aLoad = methodNode.instructions.getFirst();
                    while (aLoad.getOpcode() != ILOAD) {
                        aLoad = aLoad.getNext();
                    }
                    InsnList insnList = new InsnList();
                    insnList.add(new VarInsnNode(ILOAD, 1));
                    insnList.add(
                            new MethodInsnNode(
                                    INVOKESTATIC,
                                    "com/github/vfyjxf/nee/asm/AppengHelper",
                                    "handleMouseWheelInput",
                                    "(I)Z",
                                    false));
                    LabelNode label = new LabelNode();
                    insnList.add(new JumpInsnNode(IFEQ, label));
                    insnList.add(new InsnNode(RETURN));
                    insnList.add(label);
                    methodNode.instructions.insertBefore(aLoad, insnList);
                }
            }
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            return classWriter.toByteArray();
        }

        return basicClass;
    }

    private ClassNode createClassNode(byte[] basicClass) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(basicClass);
        classReader.accept(classNode, 0);
        return classNode;
    }
}

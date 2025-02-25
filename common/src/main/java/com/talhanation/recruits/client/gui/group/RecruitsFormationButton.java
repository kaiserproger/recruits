package com.talhanation.recruits.client.gui.group;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.component.ActivateableButton;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecruitsFormationButton extends ActivateableButton {
    private final CommandScreen.Formation formation;
    public RecruitsFormationButton(CommandScreen.Formation formation, int xPos, int yPos, OnPress handler, OnTooltip tooltip) {
        super(xPos - 10, yPos - 10, 21, 21, Component.empty(), handler, tooltip);
        this.formation = formation;
    }

    @Override
    public void renderButton(PoseStack guiGraphics, int mouseX, int mouseY, float f) {
        super.renderButton(guiGraphics, mouseX, mouseY, f);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, getTextureLocation());
        blit(guiGraphics, x, y, 0, 0, 21, 21, 21, 21);
    }

    private ResourceLocation getTextureLocation() {
        ResourceLocation location;
        switch (this.formation.getIndex()){
            default -> location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/none.png");
            case 1 ->  location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/line.png");
            case 2 ->  location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/square.png");
            case 3 ->  location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/triangle.png");
            case 4 ->  location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/hcircle.png");
            case 5 ->  location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/hsquare.png");
            case 6 ->  location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/vform.png");
            case 7 ->  location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/circle.png");
            case 8 ->  location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/movement.png");
        }
        return location;
    }
}

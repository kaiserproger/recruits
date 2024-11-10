package com.talhanation.recruits.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ImageToast implements Toast {

    private final ResourceLocation image;
    private final Component title;
    private final Component description;
    private long lastChanged;
    private boolean hasStarted;
    public final long SHOW_TIME = 20000L;

    public ImageToast(ResourceLocation image, Component title, Component description) {
        this.image = image;
        this.title = title;
        this.description = description;
    }

    @Override
    public Visibility render(PoseStack poseStack, ToastComponent toastComponent, long deltaTime) {
        toastComponent.getMinecraft().getTextureManager().bindForSetup(TEXTURE);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        toastComponent.blit(poseStack, 0, 0, 0, 0, this.width(), this.height());


        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, this.image);
        GuiComponent.blit(poseStack, 5, 5, 0, 0, 21, 21, 21, 21);

        toastComponent.getMinecraft().font.draw(poseStack, this.title, 30, 7, 0xFFFFFF);

        poseStack.pushPose();
        poseStack.translate(30, 18, 0);  // Position für die Beschreibung
        poseStack.scale(0.6f, 0.6f, 1.0f);  // Skalierung auf 80% der Standardgröße
        toastComponent.getMinecraft().font.draw(poseStack, this.description, 0, 0, 0xCCCCCC);
        poseStack.popPose();

        if (!this.hasStarted) {
            this.lastChanged = deltaTime;
            this.hasStarted = true;
        }
        return deltaTime - this.lastChanged >= SHOW_TIME ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public int width() {
        return 160;
    }

    @Override
    public int height() {
        return 32;
    }
}
package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.client.gui.diplomacy.DiplomacyTeamListScreen;
import com.talhanation.recruits.inventory.TeamMainContainer;
import com.talhanation.recruits.network.MessageServerUpdateTeamInspectMenu;
import com.talhanation.recruits.network.MessageToServerRequestUpdateDiplomacyList;
import com.talhanation.recruits.network.MessageToServerRequestUpdateTeamList;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.awt.*;
@OnlyIn(Dist.CLIENT)
public class TeamMainScreen extends ScreenBase<TeamMainContainer> {


    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/team/team_main_gui.png");
    Player player;
    private int leftPos;
    private int topPos;
    private static final MutableComponent CREATE_TEAM = new TranslatableComponent("gui.recruits.team_creation.create_team");
    private static final MutableComponent INSPECT_TEAM = new TranslatableComponent("gui.recruits.team_creation.inspect_team");
    private static final MutableComponent TEAMS_LIST = new TranslatableComponent("gui.recruits.team_creation.teams_list");

    public TeamMainScreen(TeamMainContainer commandContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, commandContainer, playerInventory, new TextComponent(""));
        imageWidth = 250;
        imageHeight = 83;
        player = playerInventory.player;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        boolean isInTeam = TeamEvents.isPlayerInATeam(player);


        MutableComponent mutableComponent = isInTeam ? INSPECT_TEAM : CREATE_TEAM;
        addRenderableWidget(new ExtendedButton(leftPos + 20, topPos + 29, 100, 20, mutableComponent, btn -> {
            if (isInTeam && player.getTeam() != null) {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageServerUpdateTeamInspectMenu(player.getTeam()));
                TeamEvents.openTeamInspectionScreen(player, player.getTeam());
                onClose();
            }
            else {
                TeamEvents.openTeamCreationScreen(player);
            }
        }));

        addRenderableWidget(new ExtendedButton(leftPos + 130, topPos + 29, 100, 20, TEAMS_LIST, btn -> {
            minecraft.setScreen(new DiplomacyTeamListScreen(this));
            //TeamEvents.openTeamListScreen(player);
        }));
    }

    protected void render(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, RESOURCE_LOCATION);
        this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

}

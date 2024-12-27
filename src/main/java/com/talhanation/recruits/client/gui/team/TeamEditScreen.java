package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.recruits.client.gui.widgets.ColorChatFormattingSelectionDropdownMatrix;
import com.talhanation.recruits.client.gui.widgets.ColorSelectionDropdownMatrix;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class TeamEditScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_big.png");
    private static final Component TITLE = new TranslatableComponent("gui.recruits.team.edit");
    private static final Component BACK = new TranslatableComponent("gui.recruits.button.back");
    private ColorChatFormattingSelectionDropdownMatrix teamColorDropdownMatrix;
    private ColorSelectionDropdownMatrix unitColorDropdownMatrix;
    private final Player player;
    private final RecruitsTeam recruitsTeam;
    private final Screen parent;
    private ChatFormatting teamColor;
    private Color unitColor;
    private final ArrayList<ChatFormatting> teamColors = new ArrayList<>(
            Arrays.asList(
                    ChatFormatting.BLACK,
                    ChatFormatting.DARK_BLUE,
                    ChatFormatting.DARK_GREEN,
                    ChatFormatting.DARK_AQUA,
                    ChatFormatting.DARK_RED,
                    ChatFormatting.DARK_PURPLE,
                    ChatFormatting.GOLD,
                    ChatFormatting.GRAY,
                    ChatFormatting.DARK_GRAY,
                    ChatFormatting.BLUE,
                    ChatFormatting.GREEN,
                    ChatFormatting.AQUA,
                    ChatFormatting.RED,
                    ChatFormatting.LIGHT_PURPLE,
                    ChatFormatting.YELLOW,
                    ChatFormatting.WHITE
            )
    );

    private final ArrayList<Color> unitColors = new ArrayList<>(
            Arrays.asList(

            )
    );
    public TeamEditScreen(Screen parent, Player player, RecruitsTeam recruitsTeam) {
        super(TITLE, 195,160);
        this.parent = parent;
        this.player = player;
        this.recruitsTeam = recruitsTeam;
        this.teamColor = teamColors.get(recruitsTeam.getTeamColor());
        this.unitColor = unitColors.get(recruitsTeam.getUnitColor());
    }

    @Override
    protected void init() {
        super.init();

        setButtons();
    }

    private void setButtons(){
        clearWidgets();
        // TeamColor-Dropdown

        addRenderableWidget(new Button(guiLeft + 32, guiTop + ySize - 32 - 7, 130, 20, BACK,
                btn -> {
                    minecraft.setScreen(parent);
                }
        ));

        teamColorDropdownMatrix = new ColorChatFormattingSelectionDropdownMatrix(this, guiLeft + 32 , guiTop + ySize - 62 - 7, 130, 20,
                teamColors,
                this::setTeamColor
        );
        addRenderableWidget(teamColorDropdownMatrix);

        unitColorDropdownMatrix = new ColorSelectionDropdownMatrix(this, guiLeft + 32 , guiTop + ySize - 92 - 7, 130, 20,
                unitColors,
                this::setUnitColor
        );
        addRenderableWidget(teamColorDropdownMatrix);
    }

    private void setUnitColor(Color color) {
        this.unitColor = color;
        setButtons();
    }

    private void setTeamColor(ChatFormatting selected) {
        this.teamColor = selected;
        setButtons();
    }

    @Override
    public void mouseMoved(double x, double y) {
        if(teamColorDropdownMatrix != null) teamColorDropdownMatrix.onMouseMove(x,y);
        super.mouseMoved(x, y);
    }


    @Override
    public boolean mouseClicked(double x, double y, int p_94697_) {
        if(teamColorDropdownMatrix != null) teamColorDropdownMatrix.onMouseClicked(x,y);
        return super.mouseClicked(x, y, p_94697_);
    }

    @Override
    public void renderBackground(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(poseStack, guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    public void renderForeground(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        font.draw(poseStack, TITLE, guiLeft + xSize / 2 - font.width(TITLE) / 2, guiTop + 7, FONT_COLOR);
    }
    public ChatFormatting getSelectedTeamColor() {
        return this.teamColor;
    }

    public Color getSelectedUnitColor() {
        return this.unitColor;
    }
}

package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.inventory.TeamListContainer;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import java.util.List;
@OnlyIn(Dist.CLIENT)
public class TeamListScreen extends ScreenBase<TeamListContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/team/team_list_gui.png");
    private Player player;
    public List<PlayerTeam> teams;
    private int leftPos;
    private int topPos;
    private int page = 1;
    private static final MutableComponent TEAMS_LIST = new TranslatableComponent("gui.recruits.team_creation.teams_list");
    private static final MutableComponent NO_TEAMS = new TranslatableComponent("gui.recruits.team_creation.no_teams");

    public TeamListScreen(TeamListContainer commandContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, commandContainer, playerInventory, new TextComponent(""));
        imageWidth = 197;
        imageHeight = 250;
        player = playerInventory.player;
    }

    @Override
    protected void init() {
        super.init();

        this.teams = player.getScoreboard().getPlayerTeams().stream().toList();

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        this.setPageButtons();
    }

    public void setPageButtons() {
        clearWidgets();

        if (teams.size() > 9) createPageForwardButton();

        if (page > 1) createPageBackButton();

        int teamsPerPage = 9;
        int startIndex = Math.max((page - 1) * teamsPerPage, 0); // Ensure startIndex is not negative
        int endIndex = Math.min(startIndex + teamsPerPage, teams.size());

        for (int i = startIndex; i < endIndex; i++) {
            PlayerTeam team = teams.get(i);
            String teamName = team.getName();
            ExtendedButton joinButton = createJoinButton(teamName, i - startIndex);

            joinButton.active = player != null && player.getTeam() == null;
        }
    }

    protected void render(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, RESOURCE_LOCATION);
        this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        // Info
        int fontColor = 4210752;
        int teamFontColor = 4210752;

        if(teams.size() > 9)
            font.draw(matrixStack, "Page: " + page, 140, 11, fontColor);

        font.draw(matrixStack, TEAMS_LIST.getString(), 18, 11, fontColor);
        int teamsPerPage = 9;
        int startIndex = Math.max((page - 1) * teamsPerPage, 0); // Ensure startIndex is not negative
        int endIndex = Math.min(startIndex + teamsPerPage, teams.size());

        if (!teams.isEmpty()) {
            for (int i = startIndex; i < endIndex; i++) {
                PlayerTeam playerTeam = teams.get(i);
                String name = playerTeam.getName();
                List<String> allMembers = playerTeam.getPlayers().stream().toList();

                if (playerTeam.getColor().isColor()) {
                    teamFontColor = playerTeam.getColor().getColor();
                }

                int players = allMembers.stream().filter((str) -> str.chars().count() <= 16).toList().size();
                int x = 18;
                int y = 32 + (23 * (i - startIndex));

                font.draw(matrixStack, name, x, y, teamFontColor);
                font.draw(matrixStack, "" + players, x + 80, y, teamFontColor);
            }
        } else {
            font.draw(matrixStack, NO_TEAMS, 20, 26, fontColor);
        }
    }

    public ExtendedButton createJoinButton(String teamName, int index) {
        return addRenderableWidget(new ExtendedButton(leftPos + 150, topPos + 25 + (23 * index), 30, 15, new TranslatableComponent("chat.recruits.team_creation.join"),
                button -> {

                    //Main.SIMPLE_CHANNEL.sendToServer(new MessageSendJoinRequestTeam(player.getUUID(), teamName));
                    this.onClose();
                }
        ));
    }

    public ExtendedButton createPageBackButton() {
        return addRenderableWidget(new ExtendedButton(leftPos, topPos + 40 + (23 * 9), 20, 15, new TextComponent("<"),
                button -> {
                    if(this.page > 0) page--;
                    this.setPageButtons();
                }
        ));
    }

    public ExtendedButton createPageForwardButton() {
        return addRenderableWidget(new ExtendedButton(leftPos + 175, topPos + 40 + (23 * 9), 20, 15, new TextComponent(">"),
                button -> {
                    page++;
                    this.setPageButtons();
                }
        ));
    }
}

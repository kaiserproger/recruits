package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.PromoteContainer;
import com.talhanation.recruits.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

public class PromoteScreen extends ScreenBase<PromoteContainer> {


    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID, "textures/gui/professions/professions_main_gui.png");
    private final Player player;
    private final AbstractRecruitEntity recruit;
    private EditBox textField;
    private int leftPos;
    private int topPos;

    private static final MutableComponent BUTTON_MESSENGER = new TranslatableComponent("gui.recruits.inv.text.messenger");
    private static final MutableComponent TOOLTIP_MESSENGER = new TranslatableComponent("gui.recruits.inv.tooltip.messenger");
    private static final MutableComponent BUTTON_PATROL_LEADER = new TranslatableComponent("gui.recruits.inv.text.patrol_leader");
    private static final MutableComponent TOOLTIP_PATROL_LEADER = new TranslatableComponent("gui.recruits.inv.tooltip.patrol_leader");
    private static final MutableComponent BUTTON_CAPTAIN = new TranslatableComponent("gui.recruits.inv.text.captain");
    private static final MutableComponent TOOLTIP_CAPTAIN = new TranslatableComponent("gui.recruits.inv.tooltip.captain");
    private static final MutableComponent TOOLTIP_CAPTAIN_DISABLED = new TranslatableComponent("gui.recruits.inv.tooltip.captain_disabled");
    private static final MutableComponent BUTTON_SCOUT = new TranslatableComponent("gui.recruits.inv.text.scout");
    private static final MutableComponent TOOLTIP_SCOUT = new TranslatableComponent("gui.recruits.inv.tooltip.scout");
    private static final MutableComponent BUTTON_GOVERNOR = new TranslatableComponent("gui.recruits.inv.text.governor");
    private static final MutableComponent TOOLTIP_GOVERNOR = new TranslatableComponent("gui.recruits.inv.tooltip.governor");
    private static final MutableComponent BUTTON_ASSASSIN = new TranslatableComponent("gui.recruits.inv.text.assassin");
    private static final MutableComponent TOOLTIP_ASSASSIN = new TranslatableComponent("gui.recruits.inv.tooltip.assassin");
    private static final MutableComponent BUTTON_SPY = new TranslatableComponent("gui.recruits.inv.text.spy");
    private static final MutableComponent TOOLTIP_SPY = new TranslatableComponent("gui.recruits.inv.tooltip.spy");
    private static final MutableComponent BUTTON_SIEGE_ENGINEER = new TranslatableComponent("gui.recruits.inv.text.siege_engineer");
    private static final MutableComponent TOOLTIP_SIEGE_ENGINEER = new TranslatableComponent("gui.recruits.inv.tooltip.siege_engineer");
    private static final MutableComponent BUTTON_ROGUE = new TranslatableComponent("gui.recruits.inv.text.rogue");
    private static final MutableComponent TOOLTIP_ROGUE = new TranslatableComponent("gui.recruits.inv.tooltip.rogue");

    //private boolean keepTeam;


    public PromoteScreen(PromoteContainer container, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, container, playerInventory, new TextComponent(""));
        this.imageWidth = 197;
        this.imageHeight = 250;
        this.player = container.getPlayerEntity();
        this.recruit = container.getRecruit();

    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        //keepTeam = false;

        setWidgets();
    }

    private void setEditBox() {
        minecraft.keyboardHandler.setSendRepeatsToGui(true);
        Component name = new TextComponent("Name");
        if(recruit.getCustomName() != null) name = recruit.getCustomName();

        textField = new EditBox(font, leftPos + 16, topPos + 8, 170, 20, name);
        textField.setValue(name.getString());
        textField.setTextColor(-1);
        textField.setTextColorUneditable(-1);
        textField.setBordered(true);
        textField.setFocus(true);
        textField.setMaxLength(13);

        addRenderableWidget(textField);
        setInitialFocus(textField);
    }

    @Override
    public boolean mouseClicked(double p_97748_, double p_97749_, int p_97750_) {
        textField.setFocus(true);

        return super.mouseClicked(p_97748_, p_97749_, p_97750_);

    }

    private void setWidgets() {
        clearWidgets();
        setEditBox();
        createProfessionButtons(BUTTON_MESSENGER, TOOLTIP_MESSENGER, 0, recruit.getXpLevel() >= 3);
        createProfessionButtons(BUTTON_SCOUT, TOOLTIP_SCOUT, 1, recruit.getXpLevel() >= 3);

        createProfessionButtons(BUTTON_PATROL_LEADER, TOOLTIP_PATROL_LEADER, 2,recruit.getXpLevel() >= 5);
        createProfessionButtons(BUTTON_CAPTAIN, Main.isSmallShipsCompatible ? TOOLTIP_CAPTAIN : TOOLTIP_CAPTAIN_DISABLED, 3, recruit.getXpLevel() >= 5 && Main.isSmallShipsLoaded && Main.isSmallShipsCompatible);
        createProfessionButtons(BUTTON_ASSASSIN, TOOLTIP_ASSASSIN, 4, false && recruit.getXpLevel() >= 5);
        createProfessionButtons(BUTTON_SIEGE_ENGINEER, TOOLTIP_SIEGE_ENGINEER, 5, false && recruit.getXpLevel() >= 5 && Main.isSiegeWeaponsLoaded);

        createProfessionButtons(BUTTON_GOVERNOR, TOOLTIP_GOVERNOR, 6, false && recruit.getXpLevel() >= 7);
        createProfessionButtons(BUTTON_SPY, TOOLTIP_SPY, 7, false && recruit.getXpLevel() >= 7);
        createProfessionButtons(BUTTON_ROGUE, TOOLTIP_ROGUE, 8, false && recruit.getXpLevel() >= 7);
    }

    private Button createProfessionButtons(Component buttonText, Component buttonTooltip, int professionID, boolean active){
        Button professionButton = addRenderableWidget(new Button(leftPos + 59, 31 + topPos + 23 * professionID, 80, 20, buttonText,
                btn -> {
                    if (recruit != null) {
                        String name = this.textField.getValue();
                        if(name.isEmpty() || name.isBlank()){
                            name = recruit.getName().getString();
                        }

                        Main.SIMPLE_CHANNEL.sendToServer(new MessagePromoteRecruit(this.recruit.getUUID(), professionID, name));
                        onClose();
                    }
                },
                (button, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, buttonTooltip, i, i1);
                }
        ));
        professionButton.active = active;
        return professionButton;
    }

    @Override
    public boolean keyPressed(int key, int a, int b) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        setFocused(textField);

        return textField.keyPressed(key, a, b) || textField.canConsumeInput() || super.keyPressed(key, a, b);
    }

    protected void render(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, RESOURCE_LOCATION);
        this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }
}
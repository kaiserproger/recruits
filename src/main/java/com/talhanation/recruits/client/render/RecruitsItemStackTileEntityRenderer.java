package com.talhanation.recruits.client.render;

/*

public class RecruitsItemStackTileEntityRenderer extends BlockEntityWithoutLevelRenderer {
    private final RenderInfo renderInfo;
    private final HeldBannerModel heldBannerModel;

    public RecruitsItemStackTileEntityRenderer(RenderInfo renderInfo, HeldBannerModel heldBannerModel) {
        this.renderInfo = renderInfo;
        this.heldBannerModel = new HeldBannerModel();
    }


    public void renderByItem(ItemStack stack, ItemTransforms.TransformType transformType, PoseStack matrixStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        Item item = stack.getItem();
        if (item instanceof HeldBannerItem) {
            boolean flag = (stack.getTagElement("BlockEntityTag") != null);
            matrixStack.pushPose();
            matrixStack.scale(1.0F, -1.0F, -1.0F);
            Material material = flag ? ModelBakery.SHIELD_BASE : ModelBakery.NO_PATTERN_SHIELD;
            VertexConsumer vertexBuilder = material.sprite().wrap(ItemRenderer.getFoilBufferDirect(buffer, this.heldBannerModel.renderType(material.atlasLocation()), true, stack.hasFoil()));
            this.heldBannerModel.getHandle().render(matrixStack, vertexBuilder, combinedLight, combinedOverlay);
            this.heldBannerModel.getPlate().render(matrixStack, vertexBuilder, combinedLight, combinedOverlay);
            if (flag) {
                List<Pair<BannerPattern, DyeColor>> list = BannerBlockEntity.createPatterns(ShieldItem.getColor(stack), BannerBlockEntity.getItemPatterns(stack));
                renderBanner(matrixStack, vertexBuilder, buffer, combinedLight, combinedOverlay, this.heldBannerModel.getPlate(), material, false, list);
            }
            matrixStack.popPose();
        }
    }

    private void renderBanner(PoseStack matrixStack, VertexConsumer vertexBuilder, MultiBufferSource renderTypeBuffer, int combinedLight, int combinedOverlay, ModelPart modelRenderer, Material modelMaterial, boolean isBanner, List<Pair<BannerPattern, DyeColor>> bannerList) {
        for (int i = 0; i < 17 && i < bannerList.size(); i++) {
            Pair<BannerPattern, DyeColor> pair = bannerList.get(i);
            float[] afloat = (pair.getSecond()).getTextureDiffuseColors();
            Material material = new Material(isBanner ? Sheets.BANNER_SHEET : Sheets.SHIELD_SHEET, (pair.getFirst()).location(isBanner));
            modelRenderer.render(matrixStack, material.buffer(renderTypeBuffer, RenderType::entitySolid), combinedLight, combinedOverlay, afloat[0], afloat[1], afloat[2], 1.0F);
        }
    }


}

 */

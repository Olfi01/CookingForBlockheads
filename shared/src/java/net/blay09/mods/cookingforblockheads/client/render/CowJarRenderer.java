package net.blay09.mods.cookingforblockheads.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blay09.mods.cookingforblockheads.block.MilkJarBlock;
import net.blay09.mods.cookingforblockheads.tile.CowJarBlockEntity;
import net.blay09.mods.cookingforblockheads.tile.MilkJarBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.Level;

public class CowJarRenderer extends MilkJarRenderer<CowJarBlockEntity> {

    private static Cow entity;

    public CowJarRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MilkJarBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        super.render(blockEntity, partialTicks, poseStack, buffer, combinedLight, combinedOverlay);

        if (entity == null) {
            entity = new Cow(EntityType.COW, level);
        }

        float shrinkage = 0.2f;
        poseStack.pushPose();
        RenderUtils.applyBlockAngle(poseStack, blockEntity.getBlockState(), 0f);
        poseStack.translate(0, 0 + (MilkJarBlock.shouldBlockRenderLowered(level, blockEntity.getBlockPos()) ? -0.05 : 0), 0);
        poseStack.scale(shrinkage, shrinkage, shrinkage);

        Minecraft.getInstance().getEntityRenderDispatcher().render(entity, 0, 0, 0, 0f, 0f, poseStack, buffer, combinedLight);
        poseStack.popPose();
    }

}

package com.fluidphysics.minecraft;

import com.fluidphysics.core.FluidLevelData;
import com.fluidphysics.core.FluidRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * Custom renderer for fluid blocks.
 * Visualizes fluid levels and flow direction.
 */
public class FluidRenderHandler implements BlockEntityRenderer<FluidBlockEntity> {
    // The fluid registry
    private final FluidRegistry fluidRegistry;
    
    /**
     * Creates a new FluidRenderHandler.
     * 
     * @param context The renderer provider context
     * @param fluidRegistry The fluid registry
     */
    public FluidRenderHandler(BlockEntityRendererProvider.Context context, FluidRegistry fluidRegistry) {
        this.fluidRegistry = fluidRegistry;
    }
    
    @Override
    public void render(FluidBlockEntity blockEntity, float partialTicks, PoseStack poseStack, 
                      MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        // Get the block position
        BlockPos pos = blockEntity.getBlockPos();
        Level world = blockEntity.getLevel();
        
        // Get the fluid data
        FluidLevelData fluidData = fluidRegistry.getFluidData(pos);
        
        // Skip if there's no fluid data or the fluid is empty
        if (fluidData == null || fluidData.isEmpty()) {
            return;
        }
        
        // Get the fluid
        Fluid fluid = Fluids.WATER; // Default to water
        
        // Calculate the fluid height based on level
        float height = fluidData.getLevel() / (float) FluidLevelData.MAX_LEVEL;
        
        // Render the fluid
        renderFluid(poseStack, bufferSource, fluid, height, combinedLight, combinedOverlay);
    }
    
    /**
     * Renders a fluid with the specified height.
     * 
     * @param poseStack The pose stack
     * @param bufferSource The buffer source
     * @param fluid The fluid to render
     * @param height The fluid height (0-1)
     * @param combinedLight The combined light
     * @param combinedOverlay The combined overlay
     */
    private void renderFluid(PoseStack poseStack, MultiBufferSource bufferSource, 
                            Fluid fluid, float height, int combinedLight, int combinedOverlay) {
        // Get the fluid sprite
        IClientFluidTypeExtensions fluidExtensions = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation stillTexture = fluidExtensions.getStillTexture();
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(
            net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(stillTexture);
        
        // Get the fluid color
        int color = fluidExtensions.getTintColor();
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        
        // Get the vertex builder
        VertexConsumer builder = bufferSource.getBuffer(RenderType.translucent());
        
        // Save the current matrix state
        poseStack.pushPose();
        
        // Render the top face of the fluid
        float y = height;
        
        // Add vertices for the top face
        addVertex(builder, poseStack, 0, y, 0, sprite.getU0(), sprite.getV0(), red, green, blue, alpha, combinedLight);
        addVertex(builder, poseStack, 0, y, 1, sprite.getU0(), sprite.getV1(), red, green, blue, alpha, combinedLight);
        addVertex(builder, poseStack, 1, y, 1, sprite.getU1(), sprite.getV1(), red, green, blue, alpha, combinedLight);
        addVertex(builder, poseStack, 1, y, 0, sprite.getU1(), sprite.getV0(), red, green, blue, alpha, combinedLight);
        
        // Restore the matrix state
        poseStack.popPose();
    }
    
    /**
     * Adds a vertex to the vertex builder.
     * 
     * @param builder The vertex builder
     * @param poseStack The pose stack
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @param u The u texture coordinate
     * @param v The v texture coordinate
     * @param red The red color component
     * @param green The green color component
     * @param blue The blue color component
     * @param alpha The alpha color component
     * @param light The light value
     */
    private void addVertex(VertexConsumer builder, PoseStack poseStack, 
                          float x, float y, float z, float u, float v, 
                          float red, float green, float blue, float alpha, int light) {
        builder.vertex(poseStack.last().pose(), x, y, z)
               .color(red, green, blue, alpha)
               .uv(u, v)
               .uv2(light)
               .normal(poseStack.last().normal(), 0, 1, 0)
               .endVertex();
    }
}

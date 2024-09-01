package main.java.com.noknownerrors.ainpcmod;

import main.java.com.noknownerrors.ainpcmod.TreeChopperNPC;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;

public class TreeChopperNPCRenderer extends HumanoidMobRenderer<TreeChopperNPC, HumanoidModel<TreeChopperNPC>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("ainpcmod", "textures/entity/tom.png");

    public TreeChopperNPCRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(TreeChopperNPC entity) {
        return TEXTURE;
    }
}
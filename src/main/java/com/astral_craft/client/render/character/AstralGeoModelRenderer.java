package com.astral_craft.client.render.character;

import com.astral_craft.client.model.character.AstralGeoAnimationClip;
import com.astral_craft.client.model.character.AstralGeoAnimationManager;
import com.astral_craft.client.model.character.AstralGeoAnimationSet;
import com.astral_craft.client.model.character.AstralGeoModelDefinition;
import com.astral_craft.client.model.character.AstralGeoPose;
import com.astral_craft.client.model.character.AstralGeoTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

public class AstralGeoModelRenderer {

    public static void submit(AstralGeoModelDefinition model, Identifier animationSetKey, String action, float time,
                              Identifier texture, int light, PoseStack poseStack, SubmitNodeCollector collector) {
        if (model == null || !model.hasRenderableGeometry()) return;
        AstralGeoAnimationSet set = AstralGeoAnimationManager.INSTANCE.get(animationSetKey);
        AstralGeoAnimationClip clip = set == null ? null : set.clip(action);
        Set<String> rendered = new HashSet<>();
        for (AstralGeoModelDefinition.Bone bone : model.rootBones()) {
            renderBone(model, bone, clip, time, texture, light, poseStack, collector, rendered);
        }
    }

    private static void renderBone(AstralGeoModelDefinition model, AstralGeoModelDefinition.Bone bone,
                                   AstralGeoAnimationClip clip, float time, Identifier texture, int light,
                                   PoseStack poseStack, SubmitNodeCollector collector, Set<String> rendered) {
        if (!rendered.add(bone.name())) return;
        poseStack.pushPose();
        applyBoneTransform(bone, clip == null ? AstralGeoPose.IDENTITY : clip.sample(bone.name(), time), poseStack);
        if (!bone.neverRender()) {
            for (AstralGeoModelDefinition.Cube cube : bone.cubes()) renderCube(model, cube, texture, light, poseStack, collector);
        }
        for (AstralGeoModelDefinition.Bone child : model.children(bone.name())) renderBone(model, child, clip, time, texture, light, poseStack, collector, rendered);
        poseStack.popPose();
    }

    private static void applyBoneTransform(AstralGeoModelDefinition.Bone bone, AstralGeoPose animation, PoseStack poseStack) {
        AstralGeoTransform pivot = bone.pivot();
        AstralGeoTransform position = animation.position();
        poseStack.translate(position.x() / 16.0F, position.y() / 16.0F, position.z() / 16.0F);
        poseStack.translate(pivot.x() / 16.0F, pivot.y() / 16.0F, pivot.z() / 16.0F);
        rotate(poseStack, bone.rotation().add(animation.rotation()));
        poseStack.scale(Math.max(0.0001F, animation.scale().x()), Math.max(0.0001F, animation.scale().y()), Math.max(0.0001F, animation.scale().z()));
        poseStack.translate(-pivot.x() / 16.0F, -pivot.y() / 16.0F, -pivot.z() / 16.0F);
    }

    private static void renderCube(AstralGeoModelDefinition model, AstralGeoModelDefinition.Cube cube, Identifier texture,
                                   int light, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        AstralGeoTransform pivot = cube.pivot();
        poseStack.translate(pivot.x() / 16.0F, pivot.y() / 16.0F, pivot.z() / 16.0F);
        rotate(poseStack, cube.rotation());
        poseStack.translate(-pivot.x() / 16.0F, -pivot.y() / 16.0F, -pivot.z() / 16.0F);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture),
                (pose, consumer) -> emitCube(model, cube, pose, consumer, light));
        poseStack.popPose();
    }

    private static void emitCube(AstralGeoModelDefinition model, AstralGeoModelDefinition.Cube cube,
                                 PoseStack.Pose pose, VertexConsumer consumer, int light) {
        float inflate = cube.inflate();
        float x0 = (cube.origin().x() - inflate) / 16.0F;
        float y0 = (cube.origin().y() - inflate) / 16.0F;
        float z0 = (cube.origin().z() - inflate) / 16.0F;
        float x1 = (cube.origin().x() + cube.size().x() + inflate) / 16.0F;
        float y1 = (cube.origin().y() + cube.size().y() + inflate) / 16.0F;
        float z1 = (cube.origin().z() + cube.size().z() + inflate) / 16.0F;
        face(model, cube, "north", consumer, pose, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, 0,0,-1, light);
        face(model, cube, "south", consumer, pose, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, 0,0,1, light);
        face(model, cube, "west", consumer, pose, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, -1,0,0, light);
        face(model, cube, "east", consumer, pose, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, 1,0,0, light);
        face(model, cube, "up", consumer, pose, x0,y1,z1, x1,y1,z1, x1,y1,z0, x0,y1,z0, 0,1,0, light);
        face(model, cube, "down", consumer, pose, x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1, 0,-1,0, light);
    }

    private static void face(AstralGeoModelDefinition model, AstralGeoModelDefinition.Cube cube, String face,
                             VertexConsumer consumer, PoseStack.Pose pose,
                             float x0,float y0,float z0,float x1,float y1,float z1,float x2,float y2,float z2,float x3,float y3,float z3,
                             float nx,float ny,float nz,int light) {
        AstralGeoModelDefinition.FaceUv uv = cube.uv().face(face, cube.size());
        float u0 = uv.u() / model.textureWidth();
        float v0 = uv.v() / model.textureHeight();
        float u1 = (uv.u() + uv.width()) / model.textureWidth();
        float v1 = (uv.v() + uv.height()) / model.textureHeight();
        if (cube.mirror()) { float t = u0; u0 = u1; u1 = t; }
        vertex(consumer, pose, x0,y0,z0,u0,v1,nx,ny,nz,light);
        vertex(consumer, pose, x1,y1,z1,u1,v1,nx,ny,nz,light);
        vertex(consumer, pose, x2,y2,z2,u1,v0,nx,ny,nz,light);
        vertex(consumer, pose, x3,y3,z3,u0,v0,nx,ny,nz,light);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x,float y,float z,float u,float v,
                               float nx,float ny,float nz,int light) {
        consumer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, nx, ny, nz);
    }

    private static void rotate(PoseStack poseStack, AstralGeoTransform rotation) {
        if (rotation.x() != 0.0F) poseStack.mulPose(Axis.XP.rotationDegrees(rotation.x()));
        if (rotation.y() != 0.0F) poseStack.mulPose(Axis.YP.rotationDegrees(rotation.y()));
        if (rotation.z() != 0.0F) poseStack.mulPose(Axis.ZP.rotationDegrees(rotation.z()));
    }
}

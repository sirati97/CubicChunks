package io.github.opencubicchunks.cubicchunks.client;

import static java.lang.Math.abs;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.opencubicchunks.cubicchunks.chunk.ITrackingCubeStatusListener;
import io.github.opencubicchunks.cubicchunks.utils.MathUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.TransformationMatrix;
import net.minecraft.client.renderer.Vector4f;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.Direction;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.listener.TrackingChunkStatusListener;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CubicWorldLoadScreen {

    public static void doRender(TrackingChunkStatusListener trackerParam, int xBase, int yBase, int scale, int spacing,
            Object2IntMap<ChunkStatus> colors) {
        render3d(trackerParam, xBase, yBase, scale, spacing, colors);
        // render2d(trackerParam, xBase, yBase, scale, spacing, colors);
    }

    private static void render3d(TrackingChunkStatusListener trackerParam, int xBase, int yBase, int scale, int spacing,
            Object2IntMap<ChunkStatus> colors) {
        float aspectRatio = Minecraft.getInstance().currentScreen.width / (float) Minecraft.getInstance().currentScreen.height;

        RenderSystem.matrixMode(GL11.GL_PROJECTION);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        RenderSystem.multMatrix(Matrix4f.perspective(60, aspectRatio, 0.01f, -10));
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);

        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        RenderSystem.translatef(0, 0, -10);
        RenderSystem.rotatef(30, 1, 0, 0);

        RenderSystem.rotatef((float) ((System.currentTimeMillis() * 0.04) % 360), 0, 1, 0);
        render3dDrawCubes(trackerParam, xBase, yBase, scale, spacing, colors);

        RenderSystem.popMatrix();

        RenderSystem.matrixMode(GL11.GL_PROJECTION);
        RenderSystem.popMatrix();
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
    }

    private static void render3dDrawCubes(TrackingChunkStatusListener trackerParam, int xBase, int yBase, int scale, int spacing,
            Object2IntMap<ChunkStatus> colors) {
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        final int renderRadius = MathUtil.ceilDiv(trackerParam.func_219523_d(), 2);

        EnumSet<Direction> renderFaces = EnumSet.noneOf(Direction.class);

        ITrackingCubeStatusListener tracker = (ITrackingCubeStatusListener) trackerParam;
        for (int dx = -renderRadius; dx <= renderRadius; dx++) {
            for (int dy = -renderRadius; dy <= renderRadius; dy++) {
                for (int dz = -renderRadius; dz <= renderRadius; dz++) {
                    int x = dx + renderRadius;
                    int y = dy + renderRadius;
                    int z = dz + renderRadius;
                    ChunkStatus status = tracker.getCubeStatus(x, y, z);
                    if (status == null) {
                        continue;
                    }
                    renderFaces.clear();
                    float ratio = status.ordinal() / (float) ChunkStatus.FULL.ordinal();
                    int alpha = (int) (0x20 + ratio * (0xFF - 0x20));
                    int c = colors.getOrDefault(status, 0xFFFF00FF) | (alpha << 24);
                    for (Direction value : Direction.values()) {
                        ChunkStatus cubeStatus = tracker.getCubeStatus(x + value.getXOffset(), y + value.getYOffset(), z + value.getZOffset());
                        if (cubeStatus == null || !cubeStatus.isAtLeast(status)) {
                            renderFaces.add(value);
                        }
                    }
                    drawCube(buffer, dx, dy, dz, 0.12f * scale, c, renderFaces);
                }
            }
        }

        float[] modelviewMatrix = new float[16];
        GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelviewMatrix);
        Matrix4f m = new Matrix4f(modelviewMatrix);
        m.transpose();
        m.invert();
        Vector4f vec = new Vector4f(0, 0, 0, 1);
        vec.transform(m);

        buffer.sortVertexData(vec.getX(), vec.getY(), vec.getZ());
        buffer.finishDrawing();
        WorldVertexBufferUploader.draw(buffer);

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private static void drawCube(BufferBuilder buffer, int x, int y, int z, float scale, int color, EnumSet<Direction> renderFaces) {

        float x0 = x * scale;
        float x1 = x0 + scale;
        float y0 = y * scale;
        float y1 = y0 + scale;
        float z0 = z * scale;
        float z1 = z0 + scale;
        if (renderFaces.contains(Direction.UP)) {
            // up face
            vertex(buffer, x0, y1, z0, 0, 1, 0, color);
            vertex(buffer, x0, y1, z1, 0, 1, 0, color);
            vertex(buffer, x1, y1, z1, 0, 1, 0, color);
            vertex(buffer, x1, y1, z0, 0, 1, 0, color);
        }
        if (renderFaces.contains(Direction.DOWN)) {
            // down face
            vertex(buffer, x1, y0, z0, 0, -1, 0, color);
            vertex(buffer, x1, y0, z1, 0, -1, 0, color);
            vertex(buffer, x0, y0, z1, 0, -1, 0, color);
            vertex(buffer, x0, y0, z0, 0, -1, 0, color);
        }
        if (renderFaces.contains(Direction.EAST)) {
            // right face
            vertex(buffer, x1, y1, z0, 1, 0, 0, color);
            vertex(buffer, x1, y1, z1, 1, 0, 0, color);
            vertex(buffer, x1, y0, z1, 1, 0, 0, color);
            vertex(buffer, x1, y0, z0, 1, 0, 0, color);
        }
        if (renderFaces.contains(Direction.WEST)) {
            // left face
            vertex(buffer, x0, y0, z0, -1, 0, 0, color);
            vertex(buffer, x0, y0, z1, -1, 0, 0, color);
            vertex(buffer, x0, y1, z1, -1, 0, 0, color);
            vertex(buffer, x0, y1, z0, -1, 0, 0, color);
        }
        if (renderFaces.contains(Direction.NORTH)) {
            // front face (facing camera)
            vertex(buffer, x0, y1, z0, 0, 0, -1, color);
            vertex(buffer, x1, y1, z0, 0, 0, -1, color);
            vertex(buffer, x1, y0, z0, 0, 0, -1, color);
            vertex(buffer, x0, y0, z0, 0, 0, -1, color);
        }
        if (renderFaces.contains(Direction.SOUTH)) {
            // back face
            vertex(buffer, x0, y0, z1, 0, 0, 1, color);
            vertex(buffer, x1, y0, z1, 0, 0, 1, color);
            vertex(buffer, x1, y1, z1, 0, 0, 1, color);
            vertex(buffer, x0, y1, z1, 0, 0, 1, color);
        }
    }

    private static void vertex(BufferBuilder buffer, float x, float y, float z, int nx, int ny, int nz, int color) {
        float scale = 1f/255;
        float r = (color >>> 16 & 0xFF) * scale;
        float g = (color >>> 8 & 0xFF) * scale;
        float b = (color & 0xFF) * scale;
        float a = (color >>> 24) * scale;

        buffer.pos(x, y, z).color(r, g, b, a).endVertex();
    }


    private static void render2d(TrackingChunkStatusListener trackerParam, int xBase, int yBase, int scale, int spacing,
            Object2IntMap<ChunkStatus> colors) {
        int squareScale = scale + spacing;
        int loadDiameter = trackerParam.getDiameter();
        int diameterPixels = loadDiameter * squareScale - spacing;
        int totalDiameter = trackerParam.func_219523_d();
        int totalDiameterPixels = totalDiameter * squareScale - spacing;

        int minX = xBase - totalDiameterPixels / 2;
        int minZ = yBase - totalDiameterPixels / 2;

        int radiusPixels = diameterPixels / 2 + 1;

        int color = 0xff001ff;
        if (spacing != 0) {
            AbstractGui.fill(xBase - radiusPixels, yBase - radiusPixels, xBase - radiusPixels + 1, yBase + radiusPixels, color);
            AbstractGui.fill(xBase + radiusPixels - 1, yBase - radiusPixels, xBase + radiusPixels, yBase + radiusPixels, color);
            AbstractGui.fill(xBase - radiusPixels, yBase - radiusPixels, xBase + radiusPixels, yBase - radiusPixels + 1, color);
            AbstractGui.fill(xBase - radiusPixels, yBase + radiusPixels - 1, xBase + radiusPixels, yBase + radiusPixels, color);
        }

        final List<ChunkStatus> statuses = ChunkStatus.getAll();
        final List<ChunkStatus> statusesReverse = new ArrayList<>(statuses);
        Collections.reverse(statusesReverse);

        for (int dx = 0; dx < totalDiameter; ++dx) {
            for (int dz = 0; dz < totalDiameter; ++dz) {
                Map<ChunkStatus, Integer> statusCounts = new HashMap<>();
                for (int dy = 0; dy < totalDiameter; dy++) {
                    ChunkStatus chunkstatus = ((ITrackingCubeStatusListener) trackerParam).getCubeStatus(dx, dy, dz);
                    statusCounts.putIfAbsent(chunkstatus, 0);
                    //noinspection ConstantConditions
                    statusCounts.compute(chunkstatus, (status, count) -> count + 1);
                }
                Map<ChunkStatus, Float> squareSizes = new HashMap<>();

                int count = 0;
                final float centerX = minX + dx * squareScale + squareScale * 0.5f;
                final float centerZ = minZ + dz * squareScale + squareScale * 0.5f;
                for (ChunkStatus status : statuses) {
                    if (!statusCounts.containsKey(status)) {
                        continue;
                    }
                    count += statusCounts.get(status);
                    float fraction = count / (float) loadDiameter;
                    float radius = fraction * squareScale * 0.5f;
                    squareSizes.put(status, radius);
                }

                fillFloat(TransformationMatrix.identity().getMatrix(),
                        centerX, centerZ, centerX + squareScale, centerZ +  squareScale, colors.getInt(null) | 0xff000000);

                for (ChunkStatus status : statusesReverse) {
                    if (!squareSizes.containsKey(status)) {
                        continue;
                    }
                    float radius = squareSizes.get(status);

                    float screenX = centerX - radius;
                    float screenY = centerZ - radius;

                    fillFloat(TransformationMatrix.identity().getMatrix(),
                            screenX, screenY, screenX + radius * 2, screenY + radius * 2, colors.getInt(status) | 0xff000000);
                }
            }
        }
    }

    private static void fillFloat(Matrix4f transform, float x1, float y1, float x2, float y2, int color) {
        if (x1 < x2) {
            float i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            float j = y1;
            y1 = y2;
            y2 = j;
        }

        float alpha = (float) (color >> 24 & 255) / 255.0F;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(transform, x1, y2, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.pos(transform, x2, y2, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.pos(transform, x2, y1, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.pos(transform, x1, y1, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.finishDrawing();

        WorldVertexBufferUploader.draw(buffer);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }
}

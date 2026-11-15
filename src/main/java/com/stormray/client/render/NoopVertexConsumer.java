package com.stormray.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Swallows vertices; used to skip name tags, glints, etc. in the stone overlay pass. */
public final class NoopVertexConsumer implements VertexConsumer {
    public static final NoopVertexConsumer INSTANCE = new NoopVertexConsumer();

    private NoopVertexConsumer() {
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        return this;
    }

    @Override
    public VertexConsumer color(int r, int g, int b, int a) {
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        return this;
    }

    @Override
    public void endVertex() {
    }

    @Override
    public void defaultColor(int r, int g, int b, int a) {
    }

    @Override
    public void unsetDefaultColor() {
    }

    @Override
    public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v,
                       int overlay, int light, float normalX, float normalY, float normalZ) {
    }
}

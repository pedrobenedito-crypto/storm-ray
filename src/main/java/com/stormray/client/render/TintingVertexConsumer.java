package com.stormray.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Pass-through vertex consumer that multiplies vertex colours and optionally scales UVs.
 * Used to darken petrified entities and to draw the tiled stone overlay on top of them.
 */
public final class TintingVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;
    private final float uvScale;

    public TintingVertexConsumer(VertexConsumer delegate, float red, float green, float blue, float alpha, float uvScale) {
        this.delegate = delegate;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        this.uvScale = uvScale;
    }

    private static int scale(int channel, float factor) {
        return Math.max(0, Math.min(255, (int) (channel * factor)));
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        this.delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int r, int g, int b, int a) {
        this.delegate.color(scale(r, this.red), scale(g, this.green), scale(b, this.blue), scale(a, this.alpha));
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        this.delegate.uv(u * this.uvScale, v * this.uvScale);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        this.delegate.overlayCoords(u, v);
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        this.delegate.uv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        this.delegate.normal(x, y, z);
        return this;
    }

    @Override
    public void endVertex() {
        this.delegate.endVertex();
    }

    @Override
    public void defaultColor(int r, int g, int b, int a) {
        this.delegate.defaultColor(scale(r, this.red), scale(g, this.green), scale(b, this.blue), scale(a, this.alpha));
    }

    @Override
    public void unsetDefaultColor() {
        this.delegate.unsetDefaultColor();
    }

    @Override
    public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v,
                       int overlay, int light, float normalX, float normalY, float normalZ) {
        this.delegate.vertex(x, y, z, r * this.red, g * this.green, b * this.blue, a * this.alpha,
                u * this.uvScale, v * this.uvScale, overlay, light, normalX, normalY, normalZ);
    }
}

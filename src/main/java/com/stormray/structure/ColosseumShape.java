package com.stormray.structure;

import com.stormray.util.RotNoise;

/**
 * Pure geometry of the Ruined Colosseum: a round amphitheatre (arena, podium wall, stepped
 * seating over a ring corridor, three-storey arcaded facade) with deterministic decay: one
 * large collapsed sector, a smaller breach, a ragged skyline and rubble.
 *
 * <p>Everything is a function of the offset from the centre and a seed, never of generation
 * order, so each chunk can build its slice independently and both worlds get the same ruin.
 * No Minecraft classes here, so it can be unit tested and previewed outside the game.</p>
 */
public final class ColosseumShape {
    /** Half-size of the footprint; the structure spans -RADIUS..RADIUS around the centre. */
    public static final int RADIUS = 46;
    /** Height above the arena floor (y = 0 is the first block above the ground layer). */
    public static final int HEIGHT = 26;

    public static final double ARENA_RADIUS = 21.5D;
    private static final double PODIUM_RADIUS = 23.5D;
    private static final double SEAT_RADIUS = 37.5D;
    private static final double CORRIDOR_RADIUS = 35.5D;
    private static final double FACADE_RADIUS = 41.5D;
    private static final double CORNICE_RADIUS = 42.5D;
    private static final double FOUNDATION_RADIUS = 43.5D;
    private static final int FACADE_HEIGHT = 24;
    private static final int STOREY = 7;
    private static final int BAYS = 36;

    public static final byte AIR = 0;
    public static final byte WALL = 1;
    public static final byte SEAT = 2;
    public static final byte CORNICE = 3;
    public static final byte PILLAR = 4;
    public static final byte RUBBLE = 5;
    public static final byte FLOOR = 6;
    public static final byte FOUNDATION = 7;
    public static final byte SIGIL_DARK = 8;
    public static final byte SIGIL_LINE = 9;
    public static final byte SIGIL_CORE = 10;

    private ColosseumShape() {
    }

    /** Everything that is constant along one vertical column. */
    public static final class Column {
        private final int dx;
        private final int dz;
        private final int seed;
        private final double r;
        private int facadeLimit;
        private int seatTop;
        private int podiumTop;
        private int rubbleHeight;
        private int gateHeight;
        private int pillarHeight;
        private boolean pier;
        private double archHeight;

        private Column(int dx, int dz, int seed) {
            this.dx = dx;
            this.dz = dz;
            this.seed = seed;
            this.r = Math.sqrt((double) dx * dx + (double) dz * dz);
        }

        public double radius() {
            return this.r;
        }

        /** Block kind at the given height; -1 is the ground layer, 0 the first block above it. */
        public byte at(int y) {
            if (y < -1 || y >= HEIGHT) {
                return AIR;
            }
            if (y == -1) {
                return ground();
            }
            byte block = structure(y);
            if (block == AIR && y < this.rubbleHeight) {
                return RUBBLE;
            }
            return block;
        }

        public boolean solid(int y) {
            return at(y) != AIR;
        }

        /** True where terrain must be cleared and a foundation laid. */
        public boolean inFootprint() {
            return this.r <= FOUNDATION_RADIUS;
        }

        private byte ground() {
            if (this.r <= 0.8D) {
                return SIGIL_CORE;
            }
            if (this.r <= 2.6D) {
                return this.dx == 0 || this.dz == 0 ? SIGIL_LINE : SIGIL_DARK;
            }
            if (this.r <= 3.6D) {
                return SIGIL_LINE;
            }
            if (this.r <= ARENA_RADIUS) {
                return FLOOR;
            }
            return this.r <= FOUNDATION_RADIUS ? FOUNDATION : AIR;
        }

        private byte structure(int y) {
            if (this.gateHeight > 0 && y < this.gateHeight) {
                return AIR;
            }
            if (this.r <= ARENA_RADIUS) {
                return y < this.pillarHeight ? PILLAR : AIR;
            }
            if (this.r <= PODIUM_RADIUS) {
                return y < this.podiumTop ? WALL : AIR;
            }
            if (this.r <= SEAT_RADIUS) {
                if (this.r > CORRIDOR_RADIUS && y < 5) {
                    return AIR; // the ring corridor under the top rows
                }
                return y < this.seatTop ? SEAT : AIR;
            }
            if (this.r <= FACADE_RADIUS) {
                if (y >= this.facadeLimit) {
                    return AIR;
                }
                if (y >= this.facadeLimit - 3 && RotNoise.unit(this.dx, y, this.dz, this.seed + 5) < 0.35F) {
                    return AIR; // ragged, crumbling skyline
                }
                if (y >= 3 * STOREY) {
                    return WALL; // attic
                }
                int level = y % STOREY;
                if (level == STOREY - 1) {
                    return CORNICE;
                }
                boolean opening = !this.pier && level < this.archHeight;
                if (!opening) {
                    return WALL;
                }
                // ground and top arcades are open; the middle one is a blind niche
                return y / STOREY == 1 && this.r <= 39.5D ? WALL : AIR;
            }
            if (this.r <= CORNICE_RADIUS) {
                boolean band = y % STOREY == STOREY - 1 || y == FACADE_HEIGHT - 1;
                return band && y < this.facadeLimit ? CORNICE : AIR;
            }
            return AIR;
        }
    }

    public static Column column(int dx, int dz, int seed) {
        Column c = new Column(dx, dz, seed);
        double r = c.r;
        if (r > FOUNDATION_RADIUS) {
            return c; // outside the ruin: nothing at all, the terrain is left alone
        }
        double theta = Math.atan2(dz, dx);
        double turn = theta / (Math.PI * 2.0D);
        turn -= Math.floor(turn);

        // decay around the ring
        double mainCentre = RotNoise.unit(seed, 1, 0, 71) * Math.PI * 2.0D;
        double sideCentre = mainCentre + 2.2D + RotNoise.unit(seed, 2, 0, 71) * 1.6D;
        double collapse = Math.max(sector(theta, mainCentre, 0.72D), 0.75D * sector(theta, sideCentre, 0.42D));
        double seatCollapse = Math.max(sector(theta, mainCentre, 0.62D), 0.75D * sector(theta, sideCentre, 0.3D));
        double coarse = ringNoise(turn, 14, seed + 1);
        double fine = ringNoise(turn, 44, seed + 2);
        float jitter = RotNoise.unit(dx, 0, dz, seed + 3);

        double limit = FACADE_HEIGHT * (1.0D - 0.93D * collapse) * (0.8D + 0.2D * coarse) + (fine - 0.5D) * 4.0D + (jitter - 0.5D) * 2.0D;
        if (coarse > 0.5D && collapse < 0.05D) {
            limit = FACADE_HEIGHT; // a few stretches still stand to full height
        }
        c.facadeLimit = clamp((int) Math.round(limit), 0, FACADE_HEIGHT);

        int tier = (int) Math.floor((r - PODIUM_RADIUS) / 2.0D);
        int seatBase = 4 + (tier * 3 + 1) / 2;
        double seat = seatBase * (1.0D - 0.92D * seatCollapse);
        if (jitter < 0.07F) {
            seat -= 1.0D;
        }
        c.seatTop = clamp((int) Math.round(seat), 0, 14);
        c.podiumTop = clamp((int) Math.round(4.0D * (1.0D - 0.95D * seatCollapse)), 0, 4);

        // rubble spilling out of the breaches, plus loose stones in the arena
        double spread = clamp01((r - 9.0D) / 8.0D) * clamp01((FOUNDATION_RADIUS - r) / 2.0D);
        double pile = Math.pow(collapse, 0.7D) * 5.5D * RotNoise.smooth(dx, dz, 5, seed + 4) * spread;
        c.rubbleHeight = (int) Math.round(pile);
        if (c.rubbleHeight == 0 && r > 4.5D && r < ARENA_RADIUS && RotNoise.unit(dx, 1, dz, seed + 6) < 0.018F) {
            c.rubbleHeight = 1;
        }

        // four vaulted gates on the axes
        if (r > ARENA_RADIUS - 1.0D) {
            int across = Math.min(Math.abs(dx), Math.abs(dz));
            if (across <= 2) {
                c.gateHeight = across <= 1 ? 6 : 5;
            }
        }

        // eight broken pillars inside the arena
        for (int k = 0; k < 8; k++) {
            double angle = Math.toRadians(22.5D + 45.0D * k);
            int px = (int) Math.round(15.0D * Math.cos(angle));
            int pz = (int) Math.round(15.0D * Math.sin(angle));
            if ((dx == px || dx == px + 1) && (dz == pz || dz == pz + 1)) {
                c.pillarHeight = 3 + (int) (RotNoise.unit(k, 0, 0, seed + 7) * 7.0F);
                if (RotNoise.unit(dx, 2, dz, seed + 7) < 0.3F) {
                    c.pillarHeight--; // chipped top
                }
            }
        }

        // arcade bays: pier, then a round-headed opening; gates sit on bay centres
        double bay = turn * BAYS + 0.64D;
        bay -= Math.floor(bay);
        c.pier = bay < 0.28D;
        if (!c.pier) {
            double u = (bay - 0.28D) / 0.72D;
            c.archHeight = 3.4D + 2.4D * Math.sin(Math.PI * u);
        }
        return c;
    }

    /** 1 at the centre of a collapsed sector, easing to 0 at its edge. */
    private static double sector(double theta, double centre, double halfWidth) {
        double d = Math.abs(theta - centre) % (Math.PI * 2.0D);
        if (d > Math.PI) {
            d = Math.PI * 2.0D - d;
        }
        double t = clamp01(1.0D - d / halfWidth);
        return t * t * (3.0D - 2.0D * t);
    }

    /** Periodic value noise around the ring: {@code points} control values, smoothly joined. */
    private static double ringNoise(double turn, int points, int salt) {
        double p = turn * points;
        int i0 = (int) Math.floor(p);
        double t = p - i0;
        t = t * t * (3.0D - 2.0D * t);
        float a = RotNoise.unit(Math.floorMod(i0, points), 0, 0, salt);
        float b = RotNoise.unit(Math.floorMod(i0 + 1, points), 0, 0, salt);
        return a + (b - a) * t;
    }

    private static double clamp01(double v) {
        return v < 0.0D ? 0.0D : Math.min(v, 1.0D);
    }

    private static int clamp(int v, int min, int max) {
        return v < min ? min : Math.min(v, max);
    }
}

package com.stormray.portal;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalShapeTest {
    private static Set<String> keys(int[][] cells) {
        Set<String> set = new HashSet<>();
        for (int[] c : cells) {
            set.add(c[0] + "," + c[1]);
        }
        return set;
    }

    @Test
    void interiorIsFourByFour() {
        Set<String> interior = keys(PortalShape.interior());
        assertEquals(16, interior.size());
        for (String k : interior) {
            String[] p = k.split(",");
            int x = Integer.parseInt(p[0]);
            int z = Integer.parseInt(p[1]);
            assertTrue(x >= 0 && x < 4 && z >= 0 && z < 4);
        }
    }

    @Test
    void ringSurroundsInteriorWithoutOverlap() {
        Set<String> edges = keys(PortalShape.edges());
        Set<String> corners = keys(PortalShape.corners());
        Set<String> interior = keys(PortalShape.interior());
        assertEquals(16, edges.size());
        assertEquals(4, corners.size());
        for (String e : edges) {
            assertTrue(!interior.contains(e) && !corners.contains(e));
        }
        // every interior cell on the border touches an edge block
        for (int[] c : PortalShape.interior()) {
            boolean onBorder = c[0] == 0 || c[0] == 3 || c[1] == 0 || c[1] == 3;
            if (onBorder) {
                boolean touches = edges.contains((c[0] - 1) + "," + c[1]) || edges.contains((c[0] + 1) + "," + c[1])
                        || edges.contains(c[0] + "," + (c[1] - 1)) || edges.contains(c[0] + "," + (c[1] + 1));
                assertTrue(touches);
            }
        }
    }

    @Test
    void landingIsOutsideTheFrame() {
        Set<String> occupied = keys(PortalShape.interior());
        occupied.addAll(keys(PortalShape.edges()));
        occupied.addAll(keys(PortalShape.corners()));
        for (int[] c : PortalShape.landing()) {
            assertTrue(!occupied.contains(c[0] + "," + c[1]));
        }
    }

    @Test
    void steveNameRules() {
        assertTrue(com.stormray.util.SteveNames.isSteve(null, false));
        assertTrue(com.stormray.util.SteveNames.isSteve("Steve", true));
        assertTrue(com.stormray.util.SteveNames.isSteve("mhf_steve", false));
        assertTrue(!com.stormray.util.SteveNames.isSteve("Notch", true));
        assertTrue(!com.stormray.util.SteveNames.isSteve("", true));
    }
}

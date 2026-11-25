package com.mohammedmustafa.tirbal;

import org.junit.Test;
import static org.junit.Assert.*;

public class CropGuideCatalogTest {
    @Test public void everyRegionHasAtLeastOneOfflineCropEntry() {
        for (String[] region : CropGuideCatalog.REGIONS) {
            assertTrue(region[1], CropGuideCatalog.cropsForRegion(region[0]).length > 0);
        }
    }

    @Test public void regionFilteringDoesNotLeakOtherRegions() {
        for (String[] region : CropGuideCatalog.REGIONS) {
            for (String[] crop : CropGuideCatalog.cropsForRegion(region[0])) {
                assertEquals(region[0], crop[0]);
                assertEquals(8, crop.length);
            }
        }
    }

    @Test public void includesSevenDayIndependentAgronomyFields() {
        String[][] north = CropGuideCatalog.cropsForRegion("north");
        assertTrue(north.length >= 2);
        assertTrue(north[0][2].contains("نوفمبر"));
    }

    @Test public void unknownRegionFallsBackToFirstRegionIndex() {
        assertEquals(0, CropGuideCatalog.regionIndex("not-a-region"));
    }
}

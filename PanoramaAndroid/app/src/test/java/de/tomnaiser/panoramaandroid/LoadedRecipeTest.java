package de.tomnaiser.panoramaandroid;

import org.junit.Test;
import static org.junit.Assert.*;

public class LoadedRecipeTest {
    private PanoramaPlan plan(long id,double end,int settle,boolean spherical) {
        return PanoramaPlan.create(id,spherical,0,0,end,5,3,4,20,settle,30,500,500,false);
    }
    @Test public void previewIdentityDoesNotInvalidateLoadedRecipe() { assertTrue(plan(1,10,700,false).sameRecipe(plan(2,10,700,false))); }
    @Test public void geometryAndTimingChangesRequireNewUpload() {
        PanoramaPlan loaded=plan(1,10,700,false);
        assertFalse(loaded.sameRecipe(plan(2,20,700,false))); assertFalse(loaded.sameRecipe(plan(2,10,800,false)));
        assertFalse(loaded.sameRecipe(plan(2,10,700,true))); assertFalse(loaded.sameRecipe(null));
    }
}

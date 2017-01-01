package de.belu.firestopper.tests;

import android.test.InstrumentationTestCase;

import de.belu.firestopper.tools.KodiUpdater;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by attila.szasz on 23-Oct-15.
 */

@Slf4j
public class KodiTests extends InstrumentationTestCase {
    public void testCheckForUpdate() throws Exception {
        KodiUpdater mKodiUpdater = new KodiUpdater(this.getInstrumentation().getContext());
        mKodiUpdater.checkForUpdate(true);
        assertNotNull("Latest version should not be null", mKodiUpdater.getLatestVersion());
    }
}

package de.belu.firestopper.gui;

import android.app.Fragment;
import android.view.KeyEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Fragement that has additional features
 */

@Slf4j
public class CustomFragment extends Fragment {
    /**
     * Custom on backpressed method
     */
    public boolean onBackPressed() {
        return false;
    }

    public boolean onKeyDown(int keycode, KeyEvent e) {
        return false;
    }
}

package de.belu.firestopper.observer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import lombok.extern.slf4j.Slf4j;

/**
 * Receives broadcasts to help to detect home-button clicks
 */

@Slf4j
public class BroadcastHelperReceiver extends BroadcastReceiver {
    public final static String ACTION_CLOSE_SYSTEM_DIALOGS = "android.intent.action.CLOSE_SYSTEM_DIALOGS";

    /** Event for close system dialog broadcasts */
    private OnReceivedCloseSystemDialog mOnReceivedCloseSystemDialog;

    /** Set the close system dialog event listener */
    public void setOnReceivedCloseSystemDialog(OnReceivedCloseSystemDialog onReceivedCloseSystemDialog) {
        mOnReceivedCloseSystemDialog = onReceivedCloseSystemDialog;
    }

    @Override
    public void onReceive(Context paramContext, Intent paramIntent) {
        String msg = "Detected SystemOverlayClose-Event.";
        if (paramIntent.hasExtra("reason")) {
            msg += paramIntent.getStringExtra("reason");
        }
        log.debug(msg);

        String reason = "";
        if (paramIntent.hasExtra("reason")) {
            reason = paramIntent.getStringExtra("reason");
        }
        if (mOnReceivedCloseSystemDialog != null) {
            mOnReceivedCloseSystemDialog.onReceivedCloseSystemDialog(reason);
        }
    }

    /**
     * Interface for received broadcasts
     */
    public interface OnReceivedCloseSystemDialog {
        public void onReceivedCloseSystemDialog(String reason);
    }
}

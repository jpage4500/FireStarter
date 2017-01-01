package de.belu.firestopper.observer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.belu.firestopper.tools.AppStarter;
import de.belu.firestopper.tools.SettingsProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * Receiver for Boot-Complete Broadcast
 */

@Slf4j
public class StartUpBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            log.debug("Received BOOT_COMPLETED intent.");

            // Get settings provider
            SettingsProvider settingsProvider = SettingsProvider.getInstance(context);

            // Check if background observer is active
            if (settingsProvider.getBackgroundObserverEnabled()) {
                // Start foreground service
                Intent startIntent = new Intent(context, ForeGroundService.class);
                startIntent.setAction(ForeGroundService.FOREGROUNDSERVICE_START);
                context.startService(startIntent);

                // Start startup-activity
                String startPackage = settingsProvider.getStartupPackage();
                log.debug("Startup start package is: " + startPackage);
                if (startPackage != null && !startPackage.equals("")) {
                    AppStarter.startAppByPackageName(context, startPackage, true, true, true);
                }
            }
        }
    }
}

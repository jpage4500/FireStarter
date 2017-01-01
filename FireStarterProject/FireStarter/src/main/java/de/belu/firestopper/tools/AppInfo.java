package de.belu.firestopper.tools;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

import lombok.extern.slf4j.Slf4j;

/**
 * Container to hold app informations
 */

@Slf4j
public class AppInfo extends ApplicationInfo {
    /** The current context */
    Context mContext;

    /**
     * @param app App to be hold
     */
    public AppInfo(Context context, ApplicationInfo app) {
        super(app);
        mContext = context;
    }

    /**
     * @return Name to be displayed
     */
    public String getDisplayName() {
        String retVal = this.loadLabel(mContext.getPackageManager()).toString();
        if (retVal == null || retVal.equals("")) {
            retVal = packageName;
        }
        return retVal;
    }

    /**
     * @return Icon to be displayed
     */
    public Drawable getDisplayIcon() {
        return this.loadIcon(mContext.getPackageManager());
    }
}

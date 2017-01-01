package de.belu.firestopper.gui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ListView;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.belu.firestopper.R;
import de.belu.firestopper.tools.FireStarterUpdater;
import de.belu.firestopper.tools.SettingsProvider;
import de.belu.firestopper.tools.Updater;
import lombok.extern.slf4j.Slf4j;

/**
 * Shows the updates for all available installable apps including FireStarter itself
 */

@Slf4j
public class UpdaterActivity extends Fragment {
    /** Instance of settings provider */
    private SettingsProvider mSettings = SettingsProvider.getInstance(getActivity());

    /** ListView to show all updateable apps */
    private ListView mListView;

    /** Indicates if update shall be triggered directly */
    private Boolean mTriggerUpdate = false;

    /** Updater apps adapter */
    UpdaterAppsAdapter mUpdaterAppsAdapter;

    /** Mandatory for fragment initation */
    public UpdaterActivity() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.updateractivity, container, false);

        // Get ListView
        mListView = (ListView) rootView.findViewById(R.id.listView);

        // Set updateable apps adapter
        mUpdaterAppsAdapter = new UpdaterAppsAdapter(this.getActivity());
        mListView.setAdapter(mUpdaterAppsAdapter);

        // Make buttons useable
        mListView.setItemsCanFocus(true);
        mListView.setFocusable(false);
        mListView.setFocusableInTouchMode(false);
        mListView.setClickable(false);

        // Trigger update mechanism when first view is ready..
        mListView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                try {
                    // Remove listener
                    mListView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    if (mTriggerUpdate) {
                        mTriggerUpdate = false;

                        Updater actFireStarterUpdater = (Updater) mUpdaterAppsAdapter.getItem(0);
                        if (actFireStarterUpdater != null && actFireStarterUpdater instanceof FireStarterUpdater) {
                            actFireStarterUpdater.DialogHandler.performUpdate();
                        }
                    }
                } catch (Exception e) {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    String errorReason = errors.toString();
                    log.debug("Failed to trigger update directly: \n" + errorReason);
                }
            }
        });

        return rootView;
    }

    /** Trigger update programmtically */
    public void triggerUpdateOnStartup() {
        mTriggerUpdate = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mUpdaterAppsAdapter.notifyDataSetChanged();
    }
}

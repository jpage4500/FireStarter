package de.belu.firestopper.observer;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.SequenceInputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import de.belu.firestopper.tools.AppStarter;
import de.belu.firestopper.tools.SettingsProvider;
import lombok.extern.slf4j.Slf4j;


/**
 * Runs in the Background and observes the home button clicks
 */

@Slf4j
public class BackgroundHomeButtonObserverThreadADB extends Thread {
    public static final String HANDLE_TOO_MUCH_FAILS = "HANDLE_TO_MUCH_FAILS";

    /** Name / IP of the device to be connected via TCP */
    private final String CONNECTDEVICETCP = "localhost";

    /** Name / IP of the device to be connected via EMULATOR */
    private final String CONNECTDEVICEEMU = "skipemulator";

    /** Home-button-clicked-listener */
    private OnHomeButtonClickedListener mHomeButtonClickedListener = null;

    /** ServiceError listener */
    private OnServiceErrorListener mOnServiceErrorListener = null;

    /** Thread to wait for second click */
    private Thread mWaitForSecondClickThread = null;

    /** Indicator for second click */
    private Boolean mSecondClickInTime = false;

    /** Holds an error message in case something is wrong */
    private String mErrorMessage = "Service is not yet up and running..";

    /** Boolean indicates if background-service shall run */
    private Boolean mRun = true;

    /** Boolean indicating if the adb client is successfully connected */
    private Boolean mIsConnected = false;

    /** Indicates if logcat has been cleared successfull */
    private Boolean mIsLogcatCleared = false;

    /** Indicates that the logcat-clear thread have found an error message */
    private Boolean mIsLogcatErrorMessageFound = false;

    /** String indicating the name of the current adb-device */
    private String mAdbDevice = null;

    /** Counts failed ADB connection tries */
    private Integer mFailCounter = 0;

    /** Process object */
    private Process mProcess = null;

    /** Instance of settings */
    private SettingsProvider mSettings;

    /** Indicates if the observation is currently running */
    private Boolean mIsObservationRunning = false;

    /** Store the read bytes count */
    private int mReadBytes;

    /** Store the read data */
    private char[] mReadBuffer = new char[4096];

    /** Used reader */
    private BufferedReader mReader = null;

    /** Current active sub-thread */
    private Thread mCurrentSubThread = null;

    /** Current path to adb */
    public String mAdbPath = "";

    /**
     * Create new BackgroundObserverThread
     */
    public BackgroundHomeButtonObserverThreadADB(Context context) {
        // Get settings instance
        mSettings = SettingsProvider.getInstance(context);

        // Set our priority to minimal
        this.setPriority(Thread.MIN_PRIORITY);

        // copy the adb from assets directory
        log.debug("Trying to copy adb");
        File f = new File(context.getFilesDir() + "/adb");
        if (!f.exists()) try {

            InputStream is = context.getAssets().open("adb");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            FileOutputStream fos = new FileOutputStream(f);
            fos.write(buffer);
            fos.close();
            f.setExecutable(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        mAdbPath = f.getPath();
        log.debug("adb copied to path: " + mAdbPath);

        Map<String, String> ipAddresses = getIpAddresses();
    }

    /**
     * Get a list of ip addresses
     *
     * @return
     */
    public Map<String, String> getIpAddresses() {
        Map<String, String> ipAddresses = new HashMap<>();
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                if (intf.getDisplayName().equals("eth0") || intf.getDisplayName().equals("wlan0")) {
                    log.debug("getIpAddresses: {}", intf.getDisplayName());
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress() && !inetAddress.isMulticastAddress()) {
                            String ip = inetAddress.getHostAddress().toString();
                            if (ip.contains(".")) {
                                ipAddresses.put(intf.getDisplayName(), inetAddress.getHostAddress().toString());
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("IP Address", ex.toString());
        }
        return ipAddresses;
    }

    /** Try to stop thread */
    public void stopThread() {
        mRun = false;

        // Stop sub-thread if one is running
        stopCurrentSubThread();

        // Stop main-thread if one is running
        try {
            if (mProcess != null) {
                mProcess.destroy();
            }
            this.join(1000);
            this.interrupt();
            this.join();
        } catch (InterruptedException e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String errorReason = errors.toString();
            log.debug("stopThread(): Exception: \n" + errorReason);
        }

//        if(!mIsObservationRunning)
//        {
//            log.debug("stopThread(): wait till current process finished.");
//            try
//            {
//                this.join(3000);
//            }
//            catch (InterruptedException e)
//            {
//                StringWriter errors = new StringWriter();
//                e.printStackTrace(new PrintWriter(errors));
//                String errorReason = errors.toString();
//                log.debug("stopThread(): Exception: \n" + errorReason);
//            }
//        }
//        else
//        {
//            log.debug("stopThread(): killing process.");
//            killCurrentProcess("stopThread()");
//        }
//
//        log.debug("stopThread(): stop other threads.");
//        if(mWaitForSecondClickThread != null && mWaitForSecondClickThread.isAlive())
//        {
//            mWaitForSecondClickThread.interrupt();
//            try
//            {
//                mWaitForSecondClickThread.join();
//            }
//            catch (InterruptedException e)
//            {
//                StringWriter errors = new StringWriter();
//                e.printStackTrace(new PrintWriter(errors));
//                String errorReason = errors.toString();
//                log.debug("stopThread(): Exception: \n" + errorReason);
//            }
//            mWaitForSecondClickThread = null;
//        }
        log.debug("stopThread(): finished.");
    }

    /**
     * @param listener OnHomeButtonClickedLister to be added
     */
    public void setOnHomeButtonClickedListener(OnHomeButtonClickedListener listener) {
        mHomeButtonClickedListener = listener;
    }

    /**
     * @param listener OnServiceErrorListener to be added
     */
    public void setOnServiceErrorListener(OnServiceErrorListener listener) {
        mOnServiceErrorListener = listener;
    }

    /** Override run-method which is initiated on Thread-Start */
    @Override
    public void run() {
        // Start endless-loop to observer the running TopActivity
        while (true) {
            try {
                mErrorMessage = null;
                mIsObservationRunning = false;
                log.debug("Starting HomeButtonObserver.");

                // Init some variables
                mIsConnected = false;

                // Reset adb device name
                mAdbDevice = null;

                // Check if emulator is running
                mCurrentSubThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            log.debug("GETADBEMUDEVICETHREAD: try get adb device");

                            // Run process
                            mProcess = Runtime.getRuntime().exec(new String[]{mAdbPath, "devices"});

                            // Get output reader
                            mReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
                            try {
                                // Reads stdout of process
                                while ((mReadBytes = mReader.read(mReadBuffer)) > 0) {
                                    String message = String.valueOf(mReadBuffer, 0, mReadBytes);
                                    log.debug("GETADBEMUDEVICETHREAD: adb received: " + message);
                                    if (message.contains(CONNECTDEVICEEMU)) {
                                        // Split message by any whitespace:
                                        String[] lines = message.split("\\s+");
                                        for (String line : lines) {
                                            if (line.contains(CONNECTDEVICEEMU)) {
                                                mAdbDevice = line;
                                                break;
                                            }
                                        }

                                        if (mAdbDevice != null) {
                                            log.debug("GETADBEMUDEVICETHREAD: adb device found: " + mAdbDevice);
                                            mIsConnected = true;
                                        }
                                    }
                                }
                            } finally {
                                mReader.close();
                            }
                            mProcess.waitFor();
                        } catch (Exception e) {
                            StringWriter errors = new StringWriter();
                            e.printStackTrace(new PrintWriter(errors));
                            String errorReason = errors.toString();
                            log.debug("GETADBEMUDEVICETHREAD: Exception: \n" + errorReason);
                        }
                    }
                });
                mCurrentSubThread.start();
                mCurrentSubThread.join(5000);

                if (!mRun) {
                    break;
                }

                // Check if still alive after timeout
                if (mCurrentSubThread.isAlive()) {
                    // Try to kill process
                    stopCurrentSubThread();
                }

                if (!mRun) {
                    break;
                }

                // Only if no emulator have been found, try to connect over network:
                if (!mIsConnected) {
                    // Reset adb device name
                    mAdbDevice = null;

                    // Connect an instance on localhost
                    mCurrentSubThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String ipAddress = null;
                                Map<String, String> ipAddresses = getIpAddresses();
                                if (ipAddresses.containsKey("eth0")) {
                                    ipAddress = ipAddresses.get("eth0");
                                } else if (ipAddresses.containsKey("wlan0")) {
                                    ipAddress = ipAddresses.get("wlan0");
                                }
                                if (ipAddress != null) {
//                                    log.debug("trying to kill adb");
//                                    try {
//                                        mProcess = Runtime.getRuntime().exec(new String[]{mAdbPath, "kill-server"});
//                                        mReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
//                                        try
//                                        {
//                                            // Reads stdout of process
//                                            while ((mReadBytes = mReader.read(mReadBuffer)) > 0)
//                                            {
//                                                String message = String.valueOf(mReadBuffer, 0, mReadBytes);
//                                                log.debug("CONNECTHREAD: adb received: " + message);
//                                                fireServiceErrorEvent(message);
//                                            }
//                                        }
//                                        finally
//                                        {
//                                            mReader.close();
//                                        }
//                                        mProcess.waitFor();
//                                    } catch (Exception e) {
//                                        Log.e("adb-kill", e.getMessage());
//                                    }
//                                    log.debug("trying to start in tcpip 5555");
//                                    try {
//                                        mProcess = Runtime.getRuntime().exec(new String[]{mAdbPath, "tcpip", "5555"});
//                                        mReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
//                                        try
//                                        {
//                                            // Reads stdout of process
//                                            while ((mReadBytes = mReader.read(mReadBuffer)) > 0)
//                                            {
//                                                String message = String.valueOf(mReadBuffer, 0, mReadBytes);
//                                                log.debug("CONNECTHREAD: adb received: " + message);
//                                                fireServiceErrorEvent(message);
//                                            }
//                                        }
//                                        finally
//                                        {
//                                            mReader.close();
//                                        }
//                                        mProcess.waitFor();
//                                    } catch (Exception e) {
//                                        Log.e("adb-start", e.getMessage());
//                                    }

                                    log.debug("CONNECTHREAD: try connect to " + ipAddress);

                                    // Run process
                                    mProcess = Runtime.getRuntime().exec(new String[]{mAdbPath, "connect", ipAddress});

                                    // Get output reader
                                    mReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
                                    try {
                                        // Reads stdout of process
                                        while ((mReadBytes = mReader.read(mReadBuffer)) > 0) {
                                            String message = String.valueOf(mReadBuffer, 0, mReadBytes);
                                            log.debug("CONNECTHREAD: adb received: " + message);
                                            fireServiceErrorEvent(message);
                                            if (message.contains("connected") && !message.contains("unable")) {
                                                mIsConnected = true;
                                            }
                                        }
                                    } finally {
                                        mReader.close();
                                    }
                                    mProcess.waitFor();
                                } else {

                                }
                            } catch (Exception e) {
                                StringWriter errors = new StringWriter();
                                e.printStackTrace(new PrintWriter(errors));
                                String errorReason = errors.toString();
                                log.debug("CONNECTHREAD: Exception: \n" + errorReason);
                            }
                        }
                    });
                    mCurrentSubThread.start();
                    mCurrentSubThread.join(5000);

                    if (!mRun) {
                        break;
                    }

                    // Check if still alive after timeout
                    if (mCurrentSubThread.isAlive()) {
                        // Try to kill process
                        stopCurrentSubThread();
                    }

                    if (!mIsConnected) {
                        throw new Exception("Error while connecting to adb.");
                    }
                    log.debug("Adb is connected, check devices..");

                    // Check device name
                    mCurrentSubThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                log.debug("GETADBDEVICETHREAD: try get adb device");

                                // Run process
                                mProcess = Runtime.getRuntime().exec(new String[]{mAdbPath, "devices"});

                                // Get output reader
                                mReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
                                try {
                                    // Reads stdout of process
                                    while ((mReadBytes = mReader.read(mReadBuffer)) > 0) {
                                        String message = String.valueOf(mReadBuffer, 0, mReadBytes);
                                        log.debug("GETADBDEVICETHREAD: adb received: " + message);
                                        if (message.contains(CONNECTDEVICETCP)) {
                                            // Split message by any whitespace:
                                            String[] lines = message.split("\\s+");
                                            for (String line : lines) {
                                                if (line.contains(CONNECTDEVICETCP)) {
                                                    mAdbDevice = line;
                                                    break;
                                                }
                                            }

                                            if (mAdbDevice != null) {
                                                log.debug("GETADBDEVICETHREAD: adb device found: " + mAdbDevice);
                                            }
                                        }
                                    }
                                } finally {
                                    mReader.close();
                                }
                                mProcess.waitFor();
                            } catch (Exception e) {
                                StringWriter errors = new StringWriter();
                                e.printStackTrace(new PrintWriter(errors));
                                String errorReason = errors.toString();
                                log.debug("GETADBDEVICETHREAD: Exception: \n" + errorReason);
                            }
                        }
                    });
                    mCurrentSubThread.start();
                    mCurrentSubThread.join(3000);

                    if (!mRun) {
                        break;
                    }

                    // Check if still alive after timeout
                    if (mCurrentSubThread.isAlive()) {
                        // Try to kill process
                        stopCurrentSubThread();
                    }
                }

                if (mAdbDevice == null) {
                    throw new Exception("Error finding the correct adb device.");
                }
                log.debug("Adb device found, empty logcat..");

                // Reset empty logcat flag
                mIsLogcatCleared = false;
                mIsLogcatErrorMessageFound = false;

                // Empty logcat
                mCurrentSubThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            log.debug("EMPTYLOGCATTHREAD: try clear logcat");

                            // Run process
                            mProcess = Runtime.getRuntime().exec(new String[]{mAdbPath, "-s", mAdbDevice, "logcat", "-c"});

                            // Get output reader
                            mReader = new BufferedReader(new InputStreamReader(new SequenceInputStream(mProcess.getInputStream(), mProcess.getErrorStream())));
                            try {
                                // Reads stdout of process
                                while ((mReadBytes = mReader.read(mReadBuffer)) > 0) {
                                    String message = String.valueOf(mReadBuffer, 0, mReadBytes);
                                    log.debug("EMPTYLOGCATTHREAD: adb received: " + message);
                                    if (message.contains("- waiting for device -")) {
                                        log.debug("EMPTYLOGCATTHREAD: device name must be wrong.. ");
                                        mIsLogcatErrorMessageFound = true;
                                    }
                                }
                            } finally {
                                mReader.close();
                            }
                            mProcess.waitFor();
                        } catch (Exception e) {
                            StringWriter errors = new StringWriter();
                            e.printStackTrace(new PrintWriter(errors));
                            String errorReason = errors.toString();
                            log.debug("EMPTYLOGCATTHREAD: Exception: \n" + errorReason);
                        }
                    }
                });
                mCurrentSubThread.start();
                mCurrentSubThread.join(5000);

                if (!mRun) {
                    break;
                }

                // Check if still alive after timeout
                if (mCurrentSubThread.isAlive()) {
                    // Try to kill process
                    stopCurrentSubThread();
                } else {
                    // If thread is no more alive and no error message have been found, clearing logcat was successfull
                    if (!mIsLogcatErrorMessageFound) {
                        mIsLogcatCleared = true;
                    }
                }

                if (!mIsLogcatCleared) {
                    throw new Exception("Clearing logcat failed.");
                }
                log.debug("Adb logcat cleared, now start observation..");

                // Start logcat with proper filters
                mProcess = Runtime.getRuntime().exec(new String[]{mAdbPath, "-s", mAdbDevice, "logcat", "ActivityManager:I", "*:S"});

                // Seems as everything is fine, so reset fail-counter
                mIsObservationRunning = true;
                mFailCounter = 0;

                // Get output reader// Get output reader
                mReader = new BufferedReader(new InputStreamReader(new SequenceInputStream(mProcess.getInputStream(), mProcess.getErrorStream())));
                try {
                    // Reads stdout of process
                    while ((mReadBytes = mReader.read(mReadBuffer)) > 0) {
                        String message = String.valueOf(mReadBuffer, 0, mReadBytes);
                        //log.debug("OBSERVATION: adb received: " + message);
                        if (message.startsWith("I/ActivityManager") && message.contains("act=android.intent.action.MAIN cat=[android.intent.category.HOME]")) {
                            if (mWaitForSecondClickThread != null && mWaitForSecondClickThread.isAlive()) {
                                // Signal second click
                                mSecondClickInTime = true;
                            } else {
                                // For each first home-button click disable immediately the jumpback mechanism
                                AppStarter.stopWatchThread();

                                // Create new thread to check for double click
                                mWaitForSecondClickThread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Thread.sleep(mSettings.getDoubleClickInterval());
                                            if (mSecondClickInTime) {
                                                // Fire double click event
                                                fireHomeButtonDoubleClickedEvent();
                                            } else {
                                                // Fire single click event
                                                fireHomeButtonClickedEvent();
                                            }
                                            mSecondClickInTime = false;
                                        } catch (InterruptedException ignore) {
                                        }
                                    }
                                });
                                mWaitForSecondClickThread.start();
                                mSecondClickInTime = false;
                            }
                        }
                    }
                } finally {
                    mReader.close();
                }
                mProcess.waitFor();

                log.debug("Lost connection to adb..");
                mErrorMessage = "Lost connection to adb..";
                fireServiceErrorEvent(mErrorMessage);
            } catch (Exception e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                String errorReason = errors.toString();
                log.debug("Error in BackgroundObserver: \n" + errorReason);

                mErrorMessage = "Exception: " + e.getMessage();
                fireServiceErrorEvent(mErrorMessage);
            }

            mIsObservationRunning = false;

            // Check if we shall run again:
            if (mRun) {
                // FailCounter Handling
                mFailCounter++;
                if (mFailCounter >= 3) {
                    log.debug("Too much fails, call fail handling..");
                    fireServiceErrorEvent(HANDLE_TOO_MUCH_FAILS);
                    mFailCounter = 0;
                } else {
                    log.debug("Restart observer in a few seconds..");
                }

                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ignore) {
                }

                if (mRun) {
                    // Wait some time to try again
                    mErrorMessage = "Something went wrong, restarting HomeButtonObserver now...";
                    fireServiceErrorEvent(mErrorMessage);
                }
            } else {
                break;
            }
        }
        log.debug("Observer have been stopped..");
    }

    /**
     * Stops the current running sub-thread incl. process
     */
    private void stopCurrentSubThread() {
        if (mCurrentSubThread != null && mCurrentSubThread.isAlive()) {
            if (mProcess != null) {
                mProcess.destroy();
            }
            mCurrentSubThread.interrupt();
            try {
                mCurrentSubThread.join();
            } catch (InterruptedException e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                String errorReason = errors.toString();
                log.debug("StopCurrentSubThread: Exception: \n" + errorReason);
            }
        }
        mCurrentSubThread = null;
    }


    /**
     * Fire home button clicked event to all registered listeners
     */
    private void fireHomeButtonClickedEvent() {
        Thread fireThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (mHomeButtonClickedListener != null) {
                    mHomeButtonClickedListener.onHomeButtonClicked();
                }
            }
        });
        fireThread.start();
    }

    /**
     * Fire home button double clicked event to all registered listeners
     */
    private void fireHomeButtonDoubleClickedEvent() {
        Thread fireThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (mHomeButtonClickedListener != null) {
                    mHomeButtonClickedListener.onHomeButtonDoubleClicked();
                }
            }
        });
        fireThread.start();
    }

    /**
     * @param message Fire service error message
     */
    private void fireServiceErrorEvent(final String message) {
        Thread fireThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (mOnServiceErrorListener != null) {
                    mOnServiceErrorListener.onServiceError(message);
                }
            }
        });
        fireThread.start();
    }
}

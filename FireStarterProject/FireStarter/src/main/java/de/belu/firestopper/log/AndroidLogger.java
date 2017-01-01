package de.belu.firestopper.log;

import android.util.Log;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

import de.belu.firestopper.BuildConfig;

public class AndroidLogger extends MarkerIgnoringBase {

    private static final long serialVersionUID = -1227274521521287937L;

    protected AndroidLogger(final String name) {
        this.name = name;
    }

    private boolean isDebug() {
        return BuildConfig.DEBUG;
    }

    /**
     * Only log trace and debug output for debug builds
     */
    @Override
    public boolean isTraceEnabled() {
        return isDebug();
    }

    @Override
    public void trace(final String msg) {
        if (!isTraceEnabled()) return;
        Log.v(this.name, msg);
    }

    @Override
    public void trace(final String format, final Object arg) {
        if (!isTraceEnabled()) return;
        logVerbose(MessageFormatter.format(format, arg));
    }

    @Override
    public void trace(final String format, final Object arg1, final Object arg2) {
        if (!isTraceEnabled()) return;
        logVerbose(MessageFormatter.format(format, arg1, arg2));
    }

    @Override
    public void trace(final String format, final Object... arguments) {
        if (!isTraceEnabled()) return;
        logVerbose(MessageFormatter.arrayFormat(format, arguments));
    }

    @Override
    public void trace(final String msg, final Throwable t) {
        if (!isTraceEnabled()) return;
        Log.v(this.name, msg, t);
    }

    /**
     * Only log trace and debug lines if user has enabled debug mode
     */
    @Override
    public boolean isDebugEnabled() {
        return isDebug();
    }

    @Override
    public void debug(final String msg) {
        if (!isDebugEnabled()) return;
        Log.d(this.name, msg);
    }

    @Override
    public void debug(final String format, final Object arg) {
        if (!isDebugEnabled()) return;
        logDebug(MessageFormatter.format(format, arg));
    }

    @Override
    public void debug(final String format, final Object arg1, final Object arg2) {
        if (!isDebugEnabled()) return;
        logDebug(MessageFormatter.format(format, arg1, arg2));
    }

    @Override
    public void debug(final String format, final Object... arguments) {
        if (!isDebugEnabled()) return;
        logDebug(MessageFormatter.arrayFormat(format, arguments));
    }

    @Override
    public void debug(final String msg, final Throwable t) {
        if (!isDebugEnabled()) return;
        Log.d(this.name, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(final String msg) {
        Log.i(this.name, msg);
    }

    @Override
    public void info(final String format, final Object arg) {
        logInfo(MessageFormatter.format(format, arg));
    }

    @Override
    public void info(final String format, final Object arg1, final Object arg2) {
        logInfo(MessageFormatter.format(format, arg1, arg2));
    }

    @Override
    public void info(final String format, final Object... arguments) {
        logInfo(MessageFormatter.arrayFormat(format, arguments));
    }

    @Override
    public void info(final String msg, final Throwable t) {
        Log.i(this.name, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(final String msg) {
        Log.w(this.name, msg);
    }

    @Override
    public void warn(final String format, final Object arg) {
        logWarning(MessageFormatter.format(format, arg));
    }

    @Override
    public void warn(final String format, final Object arg1, final Object arg2) {
        logWarning(MessageFormatter.format(format, arg1, arg2));
    }

    @Override
    public void warn(final String format, final Object... arguments) {
        logWarning(MessageFormatter.arrayFormat(format, arguments));
    }

    @Override
    public void warn(final String msg, final Throwable t) {
        Log.w(this.name, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(final String msg) {
        Log.e(this.name, msg);
    }

    @Override
    public void error(final String format, final Object arg) {
        logError(MessageFormatter.format(format, arg));
    }

    @Override
    public void error(final String format, final Object arg1, final Object arg2) {
        logError(MessageFormatter.format(format, arg1, arg2));
    }

    @Override
    public void error(final String format, final Object... arguments) {
        logError(MessageFormatter.arrayFormat(format, arguments));
    }

    @Override
    public void error(final String msg, final Throwable t) {
        Log.e(this.name, msg, t);
    }

    private void logVerbose(FormattingTuple ft) {
        if (ft.getThrowable() == null) {
            Log.v(this.name, ft.getMessage());
        } else {
            Log.v(this.name, ft.getMessage(), ft.getThrowable());
        }
    }

    private void logDebug(FormattingTuple ft) {
        if (ft.getThrowable() == null) {
            Log.d(this.name, ft.getMessage());
        } else {
            Log.d(this.name, ft.getMessage(), ft.getThrowable());
        }
    }

    private void logInfo(FormattingTuple ft) {
        if (ft.getThrowable() == null) {
            Log.i(this.name, ft.getMessage());
        } else {
            Log.i(this.name, ft.getMessage(), ft.getThrowable());
        }
    }

    private void logWarning(FormattingTuple ft) {
        if (ft.getThrowable() == null) {
            Log.w(this.name, ft.getMessage());
        } else {
            Log.w(this.name, ft.getMessage(), ft.getThrowable());
        }
    }

    private void logError(FormattingTuple ft) {
        if (ft.getThrowable() == null) {
            Log.e(this.name, ft.getMessage());
        } else {
            Log.e(this.name, ft.getMessage(), ft.getThrowable());
        }
    }
}

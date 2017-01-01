package de.belu.firestopper.log;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * An implementation of {@link ILoggerFactory} which always returns {@link AndroidLogger} instances.
 */
public class AndroidLoggerFactory implements ILoggerFactory {

    private static final int TAG_MAX_LENGTH = 23;
    private static final String TAG_PREFIX = "fst_";

    private final ConcurrentHashMap<String, AndroidLogger> nameToLogMap = new ConcurrentHashMap<>();

    @Override
    public Logger getLogger(final String name) {
        AndroidLogger logger = this.nameToLogMap.get(name);
        if (logger == null) {
            String tag = getTag(name);
            logger = new AndroidLogger(tag); // : new common.android.log.log.ReleaseAndroidLogger(tag);

            AndroidLogger existingLogger = this.nameToLogMap.putIfAbsent(name, logger);
            if (existingLogger != null) {
                logger = existingLogger;
            }
        }

        return logger;
    }

    private String getTag(String name) {
        if (name == null) {
            return null;
        }

        int indexOfLastDot = name.lastIndexOf('.');
        //dot must not be the first or last character
        if (indexOfLastDot > 0 && indexOfLastDot < name.length() - 2) {
            //assume it is a class
            name = TAG_PREFIX + name.substring(indexOfLastDot + 1);

            if (name.length() > TAG_MAX_LENGTH) {
                name = name.substring(0, TAG_MAX_LENGTH);
            }
        }

        return name;
    }
}

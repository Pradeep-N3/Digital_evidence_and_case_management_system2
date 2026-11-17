package dems.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {

    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter LONG = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimeUtil() {
    }

    public static String pretty(LocalDateTime time) {
        return time.format(SHORT);
    }

    public static String nowLong() {
        return LocalDateTime.now().format(LONG);
    }
}

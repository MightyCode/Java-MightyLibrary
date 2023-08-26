package MightyLibrary.mightylib.util.math;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class MathTime {
    public static long datetimeStrToTick(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime givenDateTime = LocalDateTime.parse(dateString, formatter);
        LocalDateTime ticksStart = LocalDateTime.of(1, 1, 1, 0, 0, 0);
        return ChronoUnit.MICROS.between(ticksStart, givenDateTime);
    }


    public static String tickToStr(long tickValue) {
        LocalDateTime ticksStart = LocalDateTime.of(1, 1, 1, 0, 0, 0);
        LocalDateTime givenDateTime = ticksStart.plus(tickValue, ChronoUnit.MICROS);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return givenDateTime.format(formatter);
    }

        public static String tickToCustomizedStr(long tickValue) {
            return tickToCustomizedStr(tickValue, "dd MMMM yyyy, H 'h' m 'm' s 's'");
        }

        public static String tickToCustomizedStr(long tickValue, String pattern) {
            LocalDateTime ticksStart = LocalDateTime.of(1, 1, 1, 0, 0, 0);
            LocalDateTime givenDateTime = ticksStart.plus(tickValue, ChronoUnit.MICROS);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return givenDateTime.format(formatter);
        }
}

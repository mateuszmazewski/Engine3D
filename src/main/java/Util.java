import java.math.BigDecimal;
import java.math.RoundingMode;

public class Util {
    public static double EPS = 10e-8;

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static double scaleToRange(double inRangeA, double inRangeB, double in, double outRangeA, double outRangeB) {
        return (in - inRangeA) / (inRangeB - inRangeA) * (outRangeB - outRangeA) + outRangeA;
    }

}

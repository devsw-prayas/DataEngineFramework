import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public class GrowthRuns {
    public static void main(String[] args) {
        lowPrecision();
    }

    private static void highPrecision(){
        System.out.println("Running growth calculations to " +900000000000000000L);
        BigDecimal length = new BigDecimal("16");
        final BigDecimal GOLDEN_RATIO = new BigDecimal("1.61803398875");
        System.out.println("Initial capacity: "+length);
        BigDecimal growReductionFactor = new BigDecimal("0.998");
        BigDecimal strength = new BigDecimal("0");
        while(length.compareTo(new BigDecimal("900000000000000000.0")) < 0){
            length = length.multiply(GOLDEN_RATIO.multiply(growReductionFactor)).round(MathContext.UNLIMITED);
            System.out.println("New capacity: "+length.toBigInteger());
        }
    }
    private static void lowPrecision(){
        System.out.println("Running growth calculations to " +Integer.MAX_VALUE);
        int length = 16;
        final double GOLDEN_RATIO = 1.61803398875;
        System.out.println("Initial capacity: "+length);
        double reductionFactor = 1, strength = 0.998;
        while (length < Integer.MAX_VALUE){
            length = (int)(Math.floor(GOLDEN_RATIO * reductionFactor * length));
            System.out.println("New capacity: "+length);
            reductionFactor *= strength;
        }
    }
}

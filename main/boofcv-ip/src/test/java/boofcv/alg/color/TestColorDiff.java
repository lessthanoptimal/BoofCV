package boofcv.alg.color;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ColorDiff methods tests.
 *
 * The best source for color difference  is [3]. I believe it is the most correct implementation I've found. Usually different sources
 * shows different results, especially for CIEDE2000. [1] is used to implement CIEDE2000. [3] used to implement remaining
 * functions. [2] is a source of comparative data for RGB delta difference, although some results are still little
 * different and I could not found the source of error.
 *
 * Equation from:
 * [1] Sharma G, Wu W, Dalal EN.The
 * CIEDE2000 Color-Difference Formula: Implementation Notes, Supplementary Test Data and Mathematical Observations. 2004.
 * [2] Mokrzycki WS, Tatol M. Colour difference delta E - A survey
 * [3] http://brucelindbloom.com/index.html?ColorDifferenceCalc.html
 *
 */
public class TestColorDiff {
    final double precision = 0.0001;
    // Some reference values
    final double[] colorA1 = new double[]{50.0, 2.6772, -79.7751};
    final double[] colorA2 = new double[]{50.0, 0.0, -82.7485};
    final double CIE76A = 4.0011, CIE94A = 1.3950, CIE2000A = 2.0425, CMCA = 1.7387;

    final double[] colorB1 = new double[]{2.0776, 0.0795, -1.1350};
    final double[] colorB2 = new double[]{0.9033, -0.0636, -0.5514};
    final double CIE76B = 1.3191, CIE94B = 1.3066, CIE2000B = 0.9082, CMCB = 2.4493;

    final double[] colorC1 = new double[]{50.0, 2.5, 0.0};
    final double[] colorC2 = new double[]{56.0, -27.0, -3.0};
    final double CIE76C = 30.2530, CIE94C = 27.9141, CIE2000C = 31.9030, CMCC = 38.3601;

    final double[] colorD1 = new double[]{63.0109, -31.0961, -5.8663};
    final double[] colorD2 = new double[]{62.8187, -29.7946, -4.0864};
    final double CIE76D = 2.2133, CIE94D = 1.2480, CIE2000D = 1.2630, CMCD = 1.2548;

    final double[] colorE1 = new double[]{50.0, 2.4900, -0.0010};
    final double[] colorE2 = new double[]{50.000, -2.4900, 0.0011};
    final double CIE76E = 4.9800, CIE94E = 4.8007, CIE2000E = 7.2195, CMCE = 6.5784;

    @Test
    public void testDeltaECIE76() {
        assertEquals(CIE76A,  ColorDiff.deltaECIE76(colorA1,colorA2),precision);
        assertEquals(CIE76B,  ColorDiff.deltaECIE76(colorB1, colorB2), precision);
        assertEquals(CIE76C,  ColorDiff.deltaECIE76(colorC1, colorC2), precision);
        assertEquals(CIE76D,  ColorDiff.deltaECIE76( colorD1, colorD2), precision);
        assertEquals(CIE76E,  ColorDiff.deltaECIE76( colorE1, colorE2), precision);
    }

    @Test
    public void testDeltaECIE94() {
        assertEquals(CIE94A,  ColorDiff.deltaECIE94(colorA1,colorA2),precision);
        assertEquals(CIE94B,  ColorDiff.deltaECIE94(colorB1, colorB2), precision);
        assertEquals(CIE94C,  ColorDiff.deltaECIE94(colorC1, colorC2), precision);
        assertEquals(CIE94D,  ColorDiff.deltaECIE94( colorD1, colorD2), precision);
        assertEquals(CIE94E,  ColorDiff.deltaECIE94( colorE1, colorE2), precision);
        assertEquals(CIE94E,  ColorDiff.deltaECIE94( colorE1, colorE2, 1,1,1,0.045,0.015),
                precision);
    }

    @Test
    public void testDeltaECIEDE2000() {
        assertEquals(CIE2000A,  ColorDiff.deltaECIEDE2000(colorA1,colorA2),precision);
        assertEquals(CIE2000B,  ColorDiff.deltaECIEDE2000(colorB1, colorB2), precision);
        assertEquals(CIE2000C,  ColorDiff.deltaECIEDE2000(colorC1, colorC2), precision);
        assertEquals(CIE2000D,  ColorDiff.deltaECIEDE2000( colorD1, colorD2), precision);
        assertEquals(CIE2000E,  ColorDiff.deltaECIEDE2000( colorE1, colorE2), precision);
    }

    @Test
    public void testDeltaECMC() {
        assertEquals(CMCA,  ColorDiff.deltaECMC(colorA1,colorA2),precision);
        assertEquals(CMCB,  ColorDiff.deltaECMC(colorB1, colorB2), precision);
        assertEquals(CMCC,  ColorDiff.deltaECMC(colorC1, colorC2), precision);
        assertEquals(CMCD,  ColorDiff.deltaECMC(colorD1, colorD2), precision);
        assertEquals(CMCE,  ColorDiff.deltaECMC(colorE1, colorE2), precision);
        assertEquals(CMCE,  ColorDiff.deltaECMC(colorE1, colorE2, 1, 1), precision);
    }
}

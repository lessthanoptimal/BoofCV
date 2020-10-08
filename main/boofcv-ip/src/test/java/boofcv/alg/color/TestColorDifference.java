/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.color;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
class TestColorDifference extends BoofStandardJUnit {
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
    void deltaECIE76() {
        assertEquals(CIE76A,  ColorDifference.deltaECIE76(colorA1,colorA2),precision);
        assertEquals(CIE76B,  ColorDifference.deltaECIE76(colorB1, colorB2), precision);
        assertEquals(CIE76C,  ColorDifference.deltaECIE76(colorC1, colorC2), precision);
        assertEquals(CIE76D,  ColorDifference.deltaECIE76( colorD1, colorD2), precision);
        assertEquals(CIE76E,  ColorDifference.deltaECIE76( colorE1, colorE2), precision);
    }

    @Test
    void deltaECIE94() {
        assertEquals(CIE94A,  ColorDifference.deltaECIE94(colorA1,colorA2),precision);
        assertEquals(CIE94B,  ColorDifference.deltaECIE94(colorB1, colorB2), precision);
        assertEquals(CIE94C,  ColorDifference.deltaECIE94(colorC1, colorC2), precision);
        assertEquals(CIE94D,  ColorDifference.deltaECIE94( colorD1, colorD2), precision);
        assertEquals(CIE94E,  ColorDifference.deltaECIE94( colorE1, colorE2), precision);
        assertEquals(CIE94E,  ColorDifference.deltaECIE94( colorE1, colorE2, 1,1,1,0.045,0.015),
                precision);
    }

    @Test
    void deltaECIEDE2000() {
        assertEquals(CIE2000A,  ColorDifference.deltaECIEDE2000(colorA1,colorA2),precision);
        assertEquals(CIE2000B,  ColorDifference.deltaECIEDE2000(colorB1, colorB2), precision);
        assertEquals(CIE2000C,  ColorDifference.deltaECIEDE2000(colorC1, colorC2), precision);
        assertEquals(CIE2000D,  ColorDifference.deltaECIEDE2000( colorD1, colorD2), precision);
        assertEquals(CIE2000E,  ColorDifference.deltaECIEDE2000( colorE1, colorE2), precision);
    }

    @Test
    void dltaECMC() {
        assertEquals(CMCA,  ColorDifference.deltaECMC(colorA1,colorA2),precision);
        assertEquals(CMCB,  ColorDifference.deltaECMC(colorB1, colorB2), precision);
        assertEquals(CMCC,  ColorDifference.deltaECMC(colorC1, colorC2), precision);
        assertEquals(CMCD,  ColorDifference.deltaECMC(colorD1, colorD2), precision);
        assertEquals(CMCE,  ColorDifference.deltaECMC(colorE1, colorE2), precision);
        assertEquals(CMCE,  ColorDifference.deltaECMC(colorE1, colorE2, 1, 1), precision);
    }

    @Test
    void range() {
        // Feed a lot of different numbers to see if we break things
        for (int i = 0; i<100; i++) {
            double l1 = rand.nextDouble() * 100.0;
            double l2 = rand.nextDouble() * 100.0;
            double a1 = rand.nextDouble() * (128 * 2) - 128;
            double a2 = rand.nextDouble() * (128 * 2) - 128;
            double b1 = rand.nextDouble() * (128 * 2) - 128;
            double b2 = rand.nextDouble() * (128 * 2) - 128;

            assertTrue(ColorDifference.deltaECIE76(l1, a1, b1, l2, a2, b2) >= 0);
            assertTrue(ColorDifference.deltaECIE94(l1, a1, b1, l2, a2, b2) >= 0);
            assertTrue(ColorDifference.deltaECIEDE2000(l1, a1, b1, l2, a2, b2) >= 0);
            assertTrue(ColorDifference.deltaECMC(l1, a1, b1, l2, a2, b2) >= 0);
        }
    }
}

/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

/**
 * <p>Methods for computing the difference (or error) between two colors in CIELAB color space</p>
 *
 * Colour difference deltaE: difference between two colors is a metric that allow quantified examination of a notion
 * of color similarity. These methods uses Lab color space. Color distance is a positive value, and lower values
 * means better match. Scale changes with used method
 *
 * "The observed difference in the color (perceptual difference) is a psychophysical difference noticeable by the
 * observer, determined by the actual observation of two samples.
 * The calculated difference in the color depends on the color model. Because the color stimulus can
 * be represented as a point in space, the difference in color ∆E between two stimuli is calculated as the
 * distance between the points representing these stimuli." [2]
 *
 *
 * The best source for color difference  is [3]. I believe it is the most correct implementation I've found. Usually
 * different sources shows different results, especially for CIEDE2000. [1] is used to implement CIEDE2000.
 * [3] used to implement other methods. [2] is a source of comparative data for RGB delta difference,
 * although some results are still little different (second decimal place)  and I could not found the source of error.
 * [4] is also a good source of information.
 *
 * Equation from:
 * <ol>
 * <li> Sharma G, Wu W, Dalal EN.The CIEDE2000 Color-Difference Formula: Implementation Notes, Supplementary Test
 *      Data and Mathematical Observations. 2004. </li>
 * <li> Mokrzycki WS, Tatol M. Colour difference delta E - A survey </li>
 * <li> <a href="http://brucelindbloom.com/index.html?ColorDifferenceCalc.html">brucelindbloom.com</a> </li>
 * <li> <a href="https://en.wikipedia.org/wiki/Color_difference">Wikipedia</a> </li>
 * </ol>
 *
 * @author Jairo Rotava
 */
public class ColorDifference {
	// indexes to values L, a and b in the lab array used as input
	final static int l = 0;
	final static int a = 1;
	final static int b = 2;

	/**
	 * <p>CIE76 simple color difference equation in CIELAB color space.
	 * Calculate distance between color1 and color2 close to human perception.</p>
	 *
	 * <p>
	 * l range 0:100        <br>
	 * a range -128:+128    <br>
	 * b range -128:+128    <br>
	 * </p>
	 *
	 * @param lab1 color1
	 * @param lab2 color2
	 * @return colors distance (>=0)
	 */
	public static double deltaECIE76( double[] lab1, double[] lab2 ) {
		return deltaECIE76(lab1[l], lab1[a], lab1[b], lab2[l], lab2[a], lab2[b]);
	}

	public static double deltaECIE76( double l1, double a1, double b1, double l2, double a2, double b2 ) {
		//return (Math.sqrt(Math.pow(l1-l2,2) + Math.pow(a1-a2,2) + Math.pow(b1-b2,2)));
		double p1 = l1 - l2;
		double p2 = a1 - a2;
		double p3 = b1 - b2;
		return Math.sqrt(p1*p1 + p2*p2 + p3*p3);
	}

	/**
	 * <p>CIE94 color difference equation.
	 * This is the an extended CIE76 to address perceptual non-uniformities in CIELAB color space.</p>
	 *
	 * <p>
	 * l range 0:100        <br>
	 * a range -128:+128    <br>
	 * b range -128:+128    <br>
	 * </p>
	 *
	 * @param lab1 color1
	 * @param lab2 color2
	 * @param KL depend on application: graphics arts = 1, textiles = 2
	 * @param KC usually = 1
	 * @param KH usually = 1
	 * @param K1 depend on application: graphics arts = 0.045, textiles = 0.048
	 * @param K2 depend on application: graphics arts = 0.015, textiles = 0.014
	 * @return colors distance (>=0)
	 */
	public static double deltaECIE94( double[] lab1, double[] lab2, double KL, double KC, double KH,
									  double K1, double K2 ) {
		return deltaECIE94(lab1[l], lab1[a], lab1[b], lab2[l], lab2[a], lab2[b], KL, KC, KH, K1, K2);
	}

	public static double deltaECIE94( double l1, double a1, double b1, double l2, double a2, double b2,
									  double KL, double KC, double KH, double K1, double K2 ) {
		double deltaL = l1 - l2;
		double deltaA = a1 - a2;
		double deltaB = b1 - b2;
		//double C1 = Math.sqrt(Math.pow(a1,2) + Math.pow(b1,2));
		double C1 = Math.sqrt(a1*a1 + b1*b1);
		//double C2 = Math.sqrt(Math.pow(a2,2) + Math.pow(b2,2));
		double C2 = Math.sqrt(a2*a2 + b2*b2);
		double deltaC = C1 - C2;
		// Changed to avoid NaN due limited arithmetic precision
		//double deltaH = Math.sqrt(Math.pow(deltaA,2) + Math.pow(deltaB,2) - Math.pow(deltaC,2));
		//double deltaH = Math.pow(deltaA,2) + Math.pow(deltaB,2) - Math.pow(deltaC,2);
		double deltaH = deltaA*deltaA + deltaB*deltaB - deltaC*deltaC;
		double SL = 1;
		double SC = 1 + K1*C1;
		double SH = 1 + K2*C1;
		// Changed to avoid NaN due limited arithmetic precision
		//return (Math.sqrt(Math.pow(deltaL / (KL * SL),2) + Math.pow(deltaC / (KC * SC),2) + Math.pow(deltaH / (KH * SH),2)));
		//return (Math.sqrt(Math.pow(deltaL / (KL * SL),2) + Math.pow(deltaC / (KC * SC),2) + deltaH * Math.pow( 1 / (KH * SH),2)));
		double p1 = deltaL/(KL*SL);
		double p2 = deltaC/(KC*SC);
		double p3 = 1/(KH*SH);
		return Math.sqrt(p1*p1 + p2*p2 + deltaH*p3*p3);
	}

	/**
	 * <p>CIE94 color difference equation.
	 * CIE94 with reasonably parameters (graphics arts). In doubt, use this one!!</p>
	 *
	 * <p>
	 * l range 0:100        <br>
	 * a range -128:+128    <br>
	 * b range -128:+128    <br>
	 * </p>
	 *
	 * @param lab1 color1
	 * @param lab2 color2
	 * @return colors distance (>=0)
	 */
	public static double deltaECIE94( double[] lab1, double[] lab2 ) {
		return deltaECIE94(lab1[l], lab1[a], lab1[b], lab2[l], lab2[a], lab2[b]);
	}

	public static double deltaECIE94( double l1, double a1, double b1, double l2, double a2, double b2 ) {
		return deltaECIE94(l1, a1, b1, l2, a2, b2, 1, 1, 1, 0.045, 0.015);
	}

	/**
	 * <p>CIEDE2000 color difference equation.</p>
	 * Super crazy color difference equation. Extends the CIE94 to a very nerdy level. Should be better than CIE94,
	 * but more expensive and should make no difference in practical applications according
	 * to [Color Apperance Models, Mark D Fairchild]
	 *
	 * <p>
	 * l range 0:100        <br>
	 * a range -128:+128    <br>
	 * b range -128:+128    <br>
	 * </p>
	 *
	 * @param lab1 color1
	 * @param lab2 color2
	 * @return color distance (>=0)
	 */
	public static double deltaECIEDE2000( double[] lab1, double[] lab2 ) {
		return deltaECIEDE2000(lab1[l], lab1[a], lab1[b], lab2[l], lab2[a], lab2[b]);
	}

	public static double deltaECIEDE2000( double l1, double a1, double b1, double l2, double a2, double b2 ) {
		//double C1 = Math.sqrt(Math.pow(a1,2) + Math.pow(b1,2));
		double C1 = Math.sqrt(a1*a1 + b1*b1);
		//double C2 = Math.sqrt(Math.pow(a2,2) + Math.pow(b2,2));
		double C2 = Math.sqrt(a2*a2 + b2*b2);
		double avgC = (C1 + C2)/2.0;
		//double G = 0.5 * (1.0 - Math.sqrt(Math.pow(avgC, 7) / (Math.pow(avgC, 7) + Math.pow(25.0, 7))));
		double p = avgC*avgC*avgC*avgC*avgC*avgC*avgC;
		double G = 0.5*(1.0 - Math.sqrt(p/(p + 6103515625.0)));
		double a1p = a1*(1.0 + G);
		double a2p = a2*(1.0 + G);
		//double C1p = Math.sqrt(Math.pow(a1p,2) + Math.pow(b1,2));
		double C1p = Math.sqrt(a1p*a1p + b1*b1);
		//double C2p = Math.sqrt(Math.pow(a2p,2) + Math.pow(b2,2));
		double C2p = Math.sqrt(a2p*a2p + b2*b2);
		double h1p = 0;
		if (!(b1 == 0 && a1p == 0)) {
			h1p = Math.toDegrees(Math.atan2(b1, a1p));
			if (h1p < 0) {
				h1p += 360.0;
			}
		}
		double h2p = 0;
		if (!(b2 == 0 && a2p == 0)) {
			h2p = Math.toDegrees(Math.atan2(b2, a2p));
			if (h2p < 0) {
				h2p += 360.0;
			}
		}
		double deltaLp = l2 - l1;
		double deltaCp = C2p - C1p;
		double deltahp = 0;
		if (C1p*C2p != 0) {
			if (Math.abs(h2p - h1p) <= 180) {
				deltahp = h2p - h1p;
			} else if (h2p - h1p > 180) {
				deltahp = h2p - h1p - 360;
			} else {
				deltahp = h2p - h1p + 360;
			}
		}
		double deltaHp = 2*Math.sqrt(C1p*C2p)*Math.sin(Math.toRadians(deltahp/2.0));
		double avgLp = (l1 + l2)/2;
		double avgCp = (C1p + C2p)/2;
		double avghp = 0;
		if (C1p*C2p != 0) {
			if (Math.abs(h1p - h2p) <= 180) {
				avghp = (h1p + h2p)/2;
			} else {
				if (h1p + h2p < 360.0) {
					avghp = (h1p + h2p + 360.0)/2;
				} else if (h1p + h2p >= 360.0) {
					avghp = (h1p + h2p - 360.0)/2;
				}
			}
		}
		double T = 1 - 0.17*Math.cos(Math.toRadians(avghp - 30.0)) + 0.24*Math.cos(Math.toRadians(2.0*avghp)) +
				0.32*Math.cos(Math.toRadians(3.0*avghp + 6.0)) - 0.20*Math.cos(Math.toRadians(4.0*avghp - 63.0));
		//double deltaTheta = 30.0*Math.exp(-Math.pow((avghp - 275.0)/25.0,2));
		p = (avghp - 275.0)/25.0;
		double deltaTheta = 30.0*Math.exp(-p*p);
		//double RC = 2.0*Math.sqrt(Math.pow(avgCp, 7) / (Math.pow(avgCp, 7) + Math.pow(25.0, 7)));
		p = avgCp*avgCp*avgCp*avgCp*avgCp*avgCp*avgCp;
		double RC = 2.0*Math.sqrt(p/(p + 6103515625.0));
		//double SL = 1.0 + 0.015*Math.pow(avgLp - 50.0,2)/(Math.sqrt(20.0 + Math.pow(avgLp - 50.0,2)));
		p = avgLp - 50.0;
		double SL = 1.0 + 0.015*p*p/Math.sqrt(20.0 + p*p);
		double SC = 1.0 + 0.045*avgCp;
		double SH = 1.0 + 0.015*avgCp*T;
		double RT = -RC*Math.sin(Math.toRadians(2.0*deltaTheta));
		double KL = 1.0;
		double KC = 1.0;
		double KH = 1.0;
		//return (Math.sqrt(Math.pow(deltaLp/(KL*SL),2) + Math.pow(deltaCp/(KC*SC),2)
		//       + Math.pow(deltaHp/(KH*SH),2) + RT*(deltaCp/(KC*SC))*(deltaHp/(KH*SH))));
		double p1 = deltaLp/(KL*SL);
		double p2 = deltaCp/(KC*SC);
		double p3 = deltaHp/(KH*SH);
		return Math.sqrt(p1*p1 + p2*p2 + p3*p3 + RT*(deltaCp/(KC*SC))*(deltaHp/(KH*SH)));
	}

	/**
	 * CMC l:c  color difference equation defined by Colour Measurement Committee of the Society of
	 * Dyers and Colourist.</p>
	 *
	 * Commonly used values are 2:1 (l=2,c=1) for acceptability and 1:1 (l=1,c=1) for the threshold of imperceptibility.
	 *
	 * <p>
	 * l range 0:100        <br>
	 * a range -128:+128    <br>
	 * b range -128:+128    <br>
	 * </p>
	 *
	 * @param lab1 color1
	 * @param lab2 color2
	 * @param lightness lightness.
	 * @param chroma chroma.
	 * @return color distance (>=0)
	 */
	public static double deltaECMC( double[] lab1, double[] lab2, double lightness, double chroma ) {
		return deltaECMC(lab1[l], lab1[a], lab1[b], lab2[l], lab2[a], lab2[b], lightness, chroma);
	}

	public static double deltaECMC( double l1, double a1, double b1, double l2, double a2, double b2,
									double lightness, double chroma ) {
		//double C1 = Math.sqrt(Math.pow(a1,2) + Math.pow(b1,2));
		double C1 = Math.sqrt(a1*a1 + b1*b1);
		//double C2 = Math.sqrt(Math.pow(a2,2) + Math.pow(b2,2));
		double C2 = Math.sqrt(a2*a2 + b2*b2);
		double deltaC = C1 - C2;
		double deltaL = l1 - l2;
		double deltaA = a1 - a2;
		double deltaB = b1 - b2;
		// Changed to avoid NaN due limited arithmetic precision
		//double deltaH = Math.sqrt(Math.pow(deltaA,2) + Math.pow(deltaB,2) - Math.pow(deltaC,2));
		//double deltaH = Math.pow(deltaA,2) + Math.pow(deltaB,2) - Math.pow(deltaC,2);
		double deltaH = deltaA*deltaA + deltaB*deltaB - deltaC*deltaC;
		double SL = 0.511;
		if (l1 >= 16.0) {
			SL = 0.040975*l1/(1.0 + 0.01765*l1);
		}
		double SC = 0.0638*C1/(1 + 0.0131*C1) + 0.638;
		double H = Math.toDegrees(Math.atan2(b1, a1));
		double H1 = H;
		if (H < 0) {
			H1 += 360;
		}
		//double F = Math.sqrt(Math.pow(C1,4)/(Math.pow(C1,4) + 1900.0));
		double p = C1*C1*C1*C1;
		double F = Math.sqrt(p/(p + 1900.0));
		double T;
		if (H1 <= 345.0 && H1 >= 164.0) {
			T = 0.56 + Math.abs(0.2*Math.cos(Math.toRadians(H1 + 168.0)));
		} else {
			T = 0.36 + Math.abs(0.4*Math.cos(Math.toRadians(H1 + 35.0)));
		}
		double SH = SC*(F*T + 1.0 - F);
		// Changed to avoid NaN due limited arithmetic precision
		//return(Math.sqrt(Math.pow(deltaL/(l*SL),2) + Math.pow(deltaC/(c*SC),2) + Math.pow(deltaH/SH,2)));
		double p1 = deltaL/(lightness*SL);
		double p2 = deltaC/(chroma*SC);
		double p3 = 1/SH;

		return Math.sqrt(p1*p1 + p2*p2 + deltaH*p3*p3);
	}

	/**
	 * <p>CMC l:c  color difference equation defined by Colour Measurement Committee of the
	 * Society of Dyers and Colourist.</p>
	 *
	 * Default to l:c = 1:1
	 *
	 * <p>
	 * l range 0:100        <br>
	 * a range -128:+128    <br>
	 * b range -128:+128    <br>
	 * </p>
	 *
	 * @param lab1 color1
	 * @param lab2 color2
	 * @return color distance (>=0)
	 */
	public static double deltaECMC( double[] lab1, double[] lab2 ) {
		return deltaECMC(lab1[l], lab1[a], lab1[b], lab2[l], lab2[a], lab2[b]);
	}

	public static double deltaECMC( double l1, double a1, double b1, double l2, double a2, double b2 ) {
		return deltaECMC(l1, a1, b1, l2, a2, b2, 1, 1);
	}
}

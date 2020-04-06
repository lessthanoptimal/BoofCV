package boofcv.alg.color;

/**
 * ColorDiff methods.
 *
 * The best source for color difference  is [3]. I believe it is the most correct implementation I've found. Usually
 * different sources shows different results, especially for CIEDE2000. [1] is used to implement CIEDE2000.
 * [3] used to implement remaining functions. [2] is a source of comparative data for RGB delta difference,
 * although some results are still little different and I could not found the source of error.
 * [4] is also a good source of information.
 *
 * Equation from:
 * [1] Sharma G, Wu W, Dalal EN.The CIEDE2000 Color-Difference Formula: Implementation Notes, Supplementary Test
 *      Data and Mathematical Observations. 2004.
 * [2] Mokrzycki WS, Tatol M. Colour difference delta E - A survey
 * [3] http://brucelindbloom.com/index.html?ColorDifferenceCalc.html
 * [4] https://en.wikipedia.org/wiki/Color_difference
 *
 * @author  Jairo Rotava
 *
 */
public class ColorDiff {
    final static int l = 0;
    final static int a = 1;
    final static int b = 2;

    /**
     * CIE76 simple color difference equation in CIELAB color space.
     * Calculate distance between color1 and color2 close to human perception.
     *
     * @param lab1 color1
     * @param lab2 color2
     * @return colors distance
     */
    public static double deltaECIE76(double[] lab1, double [] lab2) {
        return (Math.sqrt(Math.pow(lab1[l]-lab2[l],2) + Math.pow(lab1[a]-lab2[a],2) + Math.pow(lab1[b]-lab2[b],2)));
    }

    /**
     * CIE94 color difference equation.
     * This is the an extended CIE76 to address perceptual non-uniformities in CIELAB color space.
     *
     * @param lab1 color1
     * @param lab2 color2
     * @param KL depend on application: graphics arts = 1, textiles = 2
     * @param KC usually = 1
     * @param KH usually = 1
     * @param K1 depend on application: graphics arts = 0.045, textiles = 0.048
     * @param K2 depend on application: graphics arts = 0.015, textiles = 0.014
     * @return colors distance
     */
    public static double deltaECIE94(double[] lab1, double [] lab2, double KL, double KC, double KH,
                                        double K1, double K2) {
        double deltaL = lab1[l] - lab2[l];
        double deltaA = lab1[a] - lab2[a];
        double deltaB = lab1[b] - lab2[b];
        double C1 = Math.sqrt(Math.pow(lab1[a],2) + Math.pow(lab1[b],2));
        double C2 = Math.sqrt(Math.pow(lab2[a],2) + Math.pow(lab2[b],2));
        double deltaC = C1 - C2;
        // Changed to avoid NaN due limited arithmetic precision
        //double deltaH = Math.sqrt(Math.pow(deltaA,2) + Math.pow(deltaB,2) - Math.pow(deltaC,2));
        double deltaH = Math.pow(deltaA,2) + Math.pow(deltaB,2) - Math.pow(deltaC,2);
        double SL = 1;
        double SC = 1 + K1*C1;
        double SH = 1 + K2*C1;
        // Changed to avoid NaN due limited arithmetic precision
        //return (Math.sqrt(Math.pow(deltaL / (KL * SL),2) + Math.pow(deltaC / (KC * SC),2) + Math.pow(deltaH / (KH * SH),2)));
        return (Math.sqrt(Math.pow(deltaL / (KL * SL),2) + Math.pow(deltaC / (KC * SC),2) + deltaH * Math.pow( 1 / (KH * SH),2)));
    }

    /**
     * CIE94 color difference equation.
     * CIE94 with reasonably parameters (graphics arts). In doubt, use this one!!
     *
     * @param lab1 color1
     * @param lab2 color2
     * @return colors distance
     */
    public static double deltaECIE94(double[] lab1, double [] lab2) {
        return(deltaECIE94(lab1,lab2,1,1,1,0.045,0.015));
    }

    /**
     * CIEDE2000 color difference equation.
     * Super crazy color difference equation. Extends the CIE94 to a very nerdy level. Should be better than CIE94,
     * but more expensive and should make no difference in practical applications according
     * to [Color Apperance Models, Mark D Fairchild]
     *
     * @param lab1 color1
     * @param lab2 color2
     * @return color distance
     */
    public static double deltaECIEDE2000(double[] lab1, double [] lab2) {
        double C1 = Math.sqrt(Math.pow(lab1[a],2) + Math.pow(lab1[b],2));
        double C2 = Math.sqrt(Math.pow(lab2[a],2) + Math.pow(lab2[b],2));
        double avgC = (C1 + C2)/2.0;
        double G = 0.5 * (1.0 - Math.sqrt(Math.pow(avgC, 7) / (Math.pow(avgC, 7) + Math.pow(25.0, 7))));
        double a1p = lab1[ColorDiff.a] * (1.0 + G);
        double a2p = lab2[ColorDiff.a] * (1.0 + G);
        double C1p = Math.sqrt(Math.pow(a1p,2) + Math.pow(lab1[b],2));
        double C2p = Math.sqrt(Math.pow(a2p,2) + Math.pow(lab2[b],2));
        double h1p = 0;
        if (!(lab1[b] == 0 && a1p == 0)) {
            h1p = Math.toDegrees( Math.atan2(lab1[b],a1p));
            if (h1p<0) { h1p += 360.0; }
        }
        double h2p = 0;
        if (!(lab2[b] == 0 && a2p == 0)) {
            h2p = Math.toDegrees( Math.atan2(lab2[b],a2p));
            if (h2p<0) {h2p += 360.0; }
        }
        double deltaLp = lab2[l] - lab1[l];
        double deltaCp = C2p - C1p;
        double deltahp = 0;
        if (C1p*C2p != 0) {
            if (Math.abs(h2p - h1p) <= 180) {
                deltahp = h2p - h1p;
            } else if ((h2p - h1p) > 180) {
                deltahp = h2p - h1p - 360;
            } else {
                deltahp = h2p - h1p + 360;
            }
        }
        double deltaHp = 2*Math.sqrt(C1p*C2p)*Math.sin(Math.toRadians(deltahp/2.0));
        double avgLp = (lab1[l] + lab2[l])/2;
        double avgCp = (C1p + C2p)/2;
        double avghp = 0;
        if (C1p*C2p != 0) {
            if (Math.abs(h1p-h2p) <= 180) { avghp = (h1p + h2p)/2; }
            else {
                if ((h1p + h2p) < 360.0 ) {
                    avghp = (h1p + h2p + 360.0)/2; }
                else  if ((h1p + h2p) >= 360.0 ){
                    avghp = (h1p + h2p - 360.0)/2;
                }
            }
        }
        double T = 1 - 0.17*Math.cos(Math.toRadians(avghp - 30.0)) + 0.24*Math.cos(Math.toRadians(2.0*avghp)) +
                0.32*Math.cos(Math.toRadians(3.0*avghp + 6.0)) - 0.20*Math.cos(Math.toRadians(4.0*avghp - 63.0));
        double deltaTheta = 30.0*Math.exp(-Math.pow((avghp - 275.0)/25.0,2));
        double RC = 2.0*Math.sqrt(Math.pow(avgCp, 7) / (Math.pow(avgCp, 7) + Math.pow(25.0, 7)));
        double SL = 1.0 + 0.015*Math.pow(avgLp - 50.0,2)/(Math.sqrt(20.0 + Math.pow(avgLp - 50.0,2)));
        double SC = 1.0 + 0.045*avgCp;
        double SH = 1.0 + 0.015*avgCp*T;
        double RT = -RC*Math.sin(Math.toRadians(2.0*deltaTheta));
        double KL = 1.0;
        double KC = 1.0;
        double KH = 1.0;
        return (Math.sqrt(Math.pow(deltaLp/(KL*SL),2) + Math.pow(deltaCp/(KC*SC),2)
                + Math.pow(deltaHp/(KH*SH),2) + RT*(deltaCp/(KC*SC))*(deltaHp/(KH*SH))));
    }

    /**
     * CMC l:c  color difference equation defined by Colour Measurement Committee of the Society of Dyers and Colourist.
     *
     * Commonly used values are 2:1 (l=2,c=1) for acceptability and 1:1 (l=1,c=1) for the threshold of imperceptibility.
     *
     * @param lab1 color1
     * @param lab2 color2
     * @param l lightness.
     * @param c chroma.
     * @return
     */
    public static double deltaECMC(double[] lab1, double [] lab2, double l, double c) {
        double C1 = Math.sqrt(Math.pow(lab1[a],2) + Math.pow(lab1[b],2));
        double C2 = Math.sqrt(Math.pow(lab2[a],2) + Math.pow(lab2[b],2));
        double deltaC = C1 - C2;
        double deltaL = lab1[ColorDiff.l] - lab2[ColorDiff.l];
        double deltaA = lab1[a] - lab2[a];
        double deltaB = lab1[b] - lab2[b];
        // Changed to avoid NaN due limited arithmetic precision
        //double deltaH = Math.sqrt(Math.pow(deltaA,2) + Math.pow(deltaB,2) - Math.pow(deltaC,2));
        double deltaH = Math.pow(deltaA,2) + Math.pow(deltaB,2) - Math.pow(deltaC,2);
        double SL = 0.511;
        if (lab1[ColorDiff.l] >= 16.0) {
            SL = 0.040975*lab1[ColorDiff.l]/(1.0 + 0.01765*lab1[ColorDiff.l]);
        }
        double SC = 0.0638*C1/(1 + 0.0131*C1) + 0.638;
        double H = Math.toDegrees(Math.atan2(lab1[b],lab1[a]));
        double H1 = H;
        if (H < 0) {
            H1 += 360;
        }
        double F = Math.sqrt(Math.pow(C1,4)/(Math.pow(C1,4) + 1900.0));
        double T;
        if (H1<=345.0 && H1>=164.0) {
            T = 0.56 + Math.abs(0.2*Math.cos(Math.toRadians(H1 + 168.0)));
        } else {
            T = 0.36 + Math.abs(0.4*Math.cos(Math.toRadians(H1 + 35.0)));
        }
        double SH = SC*(F*T + 1.0 - F);
        // Changed to avoid NaN due limited arithmetic precision
        //return(Math.sqrt(Math.pow(deltaL/(l*SL),2) + Math.pow(deltaC/(c*SC),2) + Math.pow(deltaH/SH,2)));
        return(Math.sqrt(Math.pow(deltaL/(l*SL),2) + Math.pow(deltaC/(c*SC),2) + deltaH * Math.pow(1/SH,2)));
    }

    /**
     * CMC l:c  color difference equation defined by Colour Measurement Committee of the Society of Dyers and Colourist.
     *
     * Default to l:c = 1:1
     *
     * @param lab1 color1
     * @param lab2 color2
     * @return color distance
     */
    public static double deltaECMC(double[] lab1, double [] lab2) {
        return (deltaECMC(lab1,lab2,1,1));
    }


}

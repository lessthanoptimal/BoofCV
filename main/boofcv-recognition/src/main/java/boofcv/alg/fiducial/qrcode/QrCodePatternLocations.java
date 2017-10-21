/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

/**
 * Lookup table by QR Code version for location and number of alignment patterns.
 *
 * @author Peter Abeles
 */
public class QrCodePatternLocations {
	/**
	 * Maximum possible version of a QR Code
	 */
	public static final int MAX_VERSION = 40;
	/**
	 * The QR code version after which and including version information is encoded into the QR code
	 */
	public static final int VERSION_VERSION = 7;
	
	public int []size = new int[MAX_VERSION+1];
	
	// alignment pattern center location as specified in QR documentation
	public int [][] alignment = new int[MAX_VERSION+1][];

	public QrCodePatternLocations() {
		for (int v = 1; v <= MAX_VERSION; v++) {
			size[v] = totalModules(v);
		}

		// There appears to be no simple equation for alignment pattern locations
		// Found several places where people claimed to have done it only to have others point out flaws
		// My personal attempt only approximated the actual solution too
		alignment[1] = new int[0];
		alignment[2] = new int[]{6,18};
		alignment[3] = new int[]{6,22};
		alignment[4] = new int[]{6,26};
		alignment[5] = new int[]{6,30};
		alignment[6] = new int[]{6,34};
		alignment[7] = new int[]{6,22,38};
		alignment[8] = new int[]{6,24,42};
		alignment[9] = new int[]{6,26,46};
		alignment[10] = new int[]{6,28,50};
		alignment[11] = new int[]{6,30,54};
		alignment[12] = new int[]{6,32,58};
		alignment[13] = new int[]{6,34,60};
		alignment[14] = new int[]{6,26,46,66};
		alignment[15] = new int[]{6,26,48,70};
		alignment[16] = new int[]{6,26,50,74};
		alignment[17] = new int[]{6,30,54,78};
		alignment[18] = new int[]{6,30,56,82};
		alignment[19] = new int[]{6,30,58,86};
		alignment[20] = new int[]{6,34,62,90};
		alignment[21] = new int[]{6,28,50,72,94};
		alignment[22] = new int[]{6,26,50,74,98};
		alignment[23] = new int[]{6,30,54,78,102};
		alignment[24] = new int[]{6,28,54,80,106};
		alignment[25] = new int[]{6,32,58,84,110};
		alignment[26] = new int[]{6,30,58,86,114};
		alignment[27] = new int[]{6,34,62,90,118};
		alignment[28] = new int[]{6,26,50,74, 98,122};
		alignment[29] = new int[]{6,30,54,78,102,126};
		alignment[30] = new int[]{6,26,52,78,104,130};
		alignment[31] = new int[]{6,30,56,82,108,134};
		alignment[32] = new int[]{6,34,60,86,112,138};
		alignment[33] = new int[]{6,30,58,86,114,142};
		alignment[34] = new int[]{6,34,62,90,118,146};
		alignment[35] = new int[]{6,30,54,78,102,126,150};
		alignment[36] = new int[]{6,24,50,76,102,128,154};
		alignment[37] = new int[]{6,28,54,80,106,132,158};
		alignment[39] = new int[]{6,32,58,84,110,136,162};
		alignment[38] = new int[]{6,26,54,82,110,138,166};
		alignment[40] = new int[]{6,30,58,86,114,142,170};
	}

	public static int totalModules( int version ) {
		return version*4+17;
	}

	public static void main(String[] args) {
		QrCodePatternLocations alg = new QrCodePatternLocations();
	}
}

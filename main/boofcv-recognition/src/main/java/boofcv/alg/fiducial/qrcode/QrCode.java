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

import georegression.struct.shapes.Polygon2D_F64;

/**
 * Information for a detected QR Code.
 *
 * <p>Position Patterns (PP) have their vertices CCW order. The polygons are oriented such that the following sides are
 * paired: ppCorner[1,2] paired ppRight[3,0] and ppCorner[2,3] paired ppDown[0,1].</p>
 *
 * @author Peter Abeles
 */
public class QrCode {
	/**
	 * The finder pattern that is composed of the 3 position patterns.
	 */
	public Polygon2D_F64 ppRight = new Polygon2D_F64(4);
	public Polygon2D_F64 ppCorner = new Polygon2D_F64(4);
	public Polygon2D_F64 ppDown = new Polygon2D_F64(4);

	// locally computed binary threshold at each position pattern
	public double threshRight,threshCorner,threshDown;

	/** which version of QR code was found. 1 to 40*/
	public int version;

	/**
	 * Approximate bounding box for QR-Code. The bottom right corner is estimated by intersecting lines
	 * and should not be used in SFM applications.
	 *
	 * Order: top-left = 0. Top-right = 1, Bottom-Right = 2, Bottom-Left = 3.
	 */
	public Polygon2D_F64 bounds = new Polygon2D_F64(4);

	public enum ErrorCorrectionLevel {
		L,M,Q,H
	}
}

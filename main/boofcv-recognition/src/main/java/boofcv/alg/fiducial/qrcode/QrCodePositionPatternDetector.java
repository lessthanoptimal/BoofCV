/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.fiducial.calib.squares.SquareGraph;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.alg.shapes.polygon.DetectPolygonFromContour;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageGray;
import georegression.geometry.UtilLine2D_F64;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Set;

/**
 * Detects position patterns for a QR code inside an image. This is done by detecting squares and seeing if they
 * have the expected shape.
 *
 * <p>If a lens distortion model is provided the returned pixel coordinates will be in an undistorted image</p>
 *
 * @author Peter Abeles
 */
public class QrCodePositionPatternDetector<T extends ImageGray<T>>  extends SquareLocatorPatternDetectorBase<T> {
	/**
	 * Configures the detector
	 *
	 * @param squareDetector Square detector
	 */
	public QrCodePositionPatternDetector( DetectPolygonBinaryGrayRefine<T> squareDetector ) {
		super(squareDetector);
	}

	public void resetRuntimeProfiling() {
		squareDetector.resetRuntimeProfiling();
	}

	@Override protected void findLocatorPatternsFromSquares() {
		squaresToPositionList();
	}

	/**
	 * Takes the detected squares and turns it into a list of {@link PositionPatternNode}.
	 */
	private void squaresToPositionList() {
		this.positionPatterns.reset();
		List<DetectPolygonFromContour.Info> infoList = squareDetector.getPolygonInfo();
		for (int i = 0; i < infoList.size(); i++) {
			DetectPolygonFromContour.Info info = infoList.get(i);

			// The test below has been commented out because the new external only contour
			// detector discards all information related to internal contours
			// squares with no internal contour cannot possibly be a finder pattern
//			if( !info.hasInternal() )
//				continue;

			// See if the appearance matches a finder pattern
			double grayThreshold = (info.edgeInside + info.edgeOutside)/2;
			if (!checkPositionPatternAppearance(info.polygon, (float)grayThreshold))
				continue;

			// refine the edge estimate
			squareDetector.refine(info);

			PositionPatternNode pp = this.positionPatterns.grow();
			pp.square = info.polygon;
			pp.grayThreshold = grayThreshold;

			SquareGraph.computeNodeInfo(pp);
		}
	}


	/**
	 * Determines if the found polygon looks like a position pattern. A horizontal and vertical line are sampled.
	 * At each sample point it is marked if it is above or below the binary threshold for this square. Location
	 * of sample points is found by "removing" perspective distortion.
	 *
	 * @param square Position pattern square.
	 */
	boolean checkPositionPatternAppearance( Polygon2D_F64 square, float grayThreshold ) {
		return (checkLine(square, grayThreshold, 0) || checkLine(square, grayThreshold, 1));
	}

	LineSegment2D_F64 segment = new LineSegment2D_F64();
	LineParametric2D_F64 parametric = new LineParametric2D_F64();
	float[] samples = new float[9*5 + 1];
	int[] length = new int[12]; // 9 is the max, but I'll let it go farther for no reason
	int[] type = new int[12];

	private boolean checkLine( Polygon2D_F64 square, float grayThreshold, int side ) {
		// find the mid point between two parallel sides
		int c0 = side;
		int c1 = (side + 1)%4;
		int c2 = (side + 2)%4;
		int c3 = (side + 3)%4;

		UtilPoint2D_F64.mean(square.get(c0), square.get(c1), segment.a);
		UtilPoint2D_F64.mean(square.get(c2), square.get(c3), segment.b);

		UtilLine2D_F64.convert(segment, parametric);

		// Scan along the line plus some extra
		int period = samples.length/9;
		double N = samples.length - 2*period - 1;

		for (int i = 0; i < samples.length; i++) {
			double location = (i - period)/N;

			float x = (float)(parametric.p.x + location*parametric.slope.x);
			float y = (float)(parametric.p.y + location*parametric.slope.y);

			samples[i] = interpolate.get(x, y);
		}

		// threshold and compute run length encoding

		int size = 0;
		boolean black = samples[0] < grayThreshold;
		type[0] = black ? 0 : 1;
		for (int i = 0; i < samples.length; i++) {
			boolean b = samples[i] < grayThreshold;

			if (black == b) {
				length[size]++;
			} else {
				black = b;
				if (size < type.length - 1) {
					size += 1;
					type[size] = black ? 0 : 1;
					length[size] = 1;
				} else {
					break;
				}
			}
		}
		size++;

		// if too simple or too complex reject
		if (size < 5 || size > 9)
			return false;
		// detect finder pattern inside RLE
		for (int i = 0; i + 5 <= size; i++) {
			if (type[i] != 0)
				continue;

			int black0 = length[i];
			int black1 = length[i + 2];
			int black2 = length[i + 4];

			int white0 = length[i + 1];
			int white1 = length[i + 3];

			// the center black area can get exagerated easily
			if (black0 < 0.4*white0 || black0 > 3*white0)
				continue;
			if (black2 < 0.4*white1 || black2 > 3*white1)
				continue;

			int black02 = black0 + black2;

			if (black1 >= black02 && black1 <= 2*black02)
				return true;
		}
		return false;
	}

	/**
	 * Checks to see if the array of sampled intensity values follows the expected pattern for a position pattern.
	 * X.XXX.X where x = black and . = white.
	 */
	static boolean positionSquareIntensityCheck( float[] values, float threshold ) {
		if (values[0] > threshold || values[1] < threshold)
			return false;
		if (values[2] > threshold || values[3] > threshold || values[4] > threshold)
			return false;
		if (values[5] < threshold || values[6] > threshold)
			return false;
		return true;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}

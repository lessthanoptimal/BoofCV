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

import boofcv.abst.filter.binary.BinaryContourFinder;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.fiducial.calib.squares.SquareGraph;
import boofcv.alg.interpolate.InterpolatePixelDistortS;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.alg.shapes.polygon.DetectPolygonFromContour;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.BoofMiscOps;
import boofcv.misc.MovingAverage;
import boofcv.struct.border.BorderType;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.PointToPixelTransform_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.geometry.UtilLine2D_F64;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.shapes.Polygon2D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.VerbosePrint;
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
public class QrCodePositionPatternDetector<T extends ImageGray<T>> implements VerbosePrint {

	// used to subsample the input image
	InterpolatePixelS<T> interpolate;

	/** Used to prune very large contours. This is tuned for QR codes which have two position patterns side by side */
	@Getter @Setter double maxContourFraction = 4.0/3.0;

	/** Used to detect black squares */
	@Getter DetectPolygonBinaryGrayRefine<T> squareDetector;

	/**
	 * Returns a list of all the detected position pattern squares and the other PP that they are connected to.
	 * If a lens distortion model is provided then coordinates will be in an undistorted image.
	 */
	@Getter DogArray<PositionPatternNode> positionPatterns = new DogArray<>(
			PositionPatternNode::new, PositionPatternNode::reset);

	/** runtime profiling */
	@Getter protected MovingAverage profilingMS = new MovingAverage(0.8);

	@Nullable PrintStream verbose = null;

	/**
	 * Configures the detector
	 *
	 * @param squareDetector Square detector
	 */
	public QrCodePositionPatternDetector( DetectPolygonBinaryGrayRefine<T> squareDetector ) {

		this.squareDetector = squareDetector;

		squareDetector.getDetector().setConvex(true);
		squareDetector.getDetector().setOutputClockwiseUpY(false);
		squareDetector.getDetector().setNumberOfSides(4, 4);

		interpolate = FactoryInterpolation.bilinearPixelS(squareDetector.getInputType(), BorderType.EXTENDED);
	}

	public void resetRuntimeProfiling() {
		squareDetector.resetRuntimeProfiling();
	}

	/**
	 * Detects position patterns inside the image and forms a graph.
	 *
	 * @param gray Gray scale input image
	 * @param binary Thresholed version of gray image.
	 */
	public void process( T gray, GrayU8 binary ) {
		configureContourDetector(gray);
		interpolate.setImage(gray);

		// detect squares
		squareDetector.process(gray, binary);

		long time0 = System.nanoTime();
		squaresToPositionList();
		long time1 = System.nanoTime();

		if (verbose != null)
			verbose.printf("squares=%d position_pattern=%d\n", squareDetector.getPolygonInfo().size(), positionPatterns.size);

		profilingMS.update((time1 - time0)*1e-6);
	}

	/**
	 * <p>Specifies transforms which can be used to change coordinates from distorted to undistorted and the opposite
	 * coordinates. The undistorted image is never explicitly created.</p>
	 *
	 * @param width Input image width. Used in sanity check only.
	 * @param height Input image height. Used in sanity check only.
	 * @param model distortion model. Null to remove a distortion model.
	 */
	public void setLensDistortion( int width, int height, @Nullable LensDistortionNarrowFOV model ) {
		interpolate = FactoryInterpolation.bilinearPixelS(
				squareDetector.getInputType(), BorderType.EXTENDED);

		if (model != null) {
			PixelTransform<Point2D_F32> distToUndist = new PointToPixelTransform_F32(model.undistort_F32(true, true));
			PixelTransform<Point2D_F32> undistToDist = new PointToPixelTransform_F32(model.distort_F32(true, true));

			squareDetector.setLensDistortion(width, height, distToUndist, undistToDist);

			// needs to sample the original image when the
			Point2Transform2_F32 u2d = model.distort_F32(true, true);
			this.interpolate = new InterpolatePixelDistortS<>(this.interpolate, u2d);
		} else {
			squareDetector.setLensDistortion(width, height, null, null);
		}
	}

	/**
	 * Configures the contour detector based on the image size. Setting a maximum contour and turning off recording
	 * of inner contours and improve speed and reduce the memory foot print significantly.
	 */
	private void configureContourDetector( T gray ) {
		// determine the maximum possible size of a position pattern
		// contour size is maximum when viewed head one. Assume the smallest qrcode is 3x this width
		// 4 side in a square
		int maxContourSize = (int)(Math.min(gray.width, gray.height)*maxContourFraction);
		BinaryContourFinder contourFinder = squareDetector.getDetector().getContourFinder();
		contourFinder.setMaxContour(maxContourSize);
		contourFinder.setSaveInnerContour(false);
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

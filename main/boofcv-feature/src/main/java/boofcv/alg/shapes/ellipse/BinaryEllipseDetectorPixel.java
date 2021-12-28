/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.shapes.ellipse;

import boofcv.abst.filter.binary.BinaryContourFinderLinearExternal;
import boofcv.abst.filter.binary.BinaryContourInterface;
import boofcv.abst.filter.binary.BinaryLabelContourFinder;
import boofcv.alg.filter.binary.ContourOps;
import boofcv.alg.filter.binary.ContourPacked;
import boofcv.factory.filter.binary.FactoryBinaryContourFinder;
import boofcv.struct.ConnectRule;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import georegression.fitting.curves.ClosestPointEllipseAngle_F64;
import georegression.fitting.curves.FitEllipseAlgebraic_F64;
import georegression.geometry.UtilEllipse_F64;
import georegression.struct.curve.EllipseQuadratic_F64;
import georegression.struct.curve.EllipseRotated_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

/**
 * <p>Detects ellipses inside a binary image by finding their contour and fitting an ellipse to the contour. Fitting
 * is done using pixel precise contour points. See {@link SnapToEllipseEdge} for a way to use sub-pixel points
 * and improve the fit's accuracy. Optionally, lens distortion can be removed from the contour points prior
 * to processing.</p>
 *
 * After the contour has been found an ellipse is fit to them using the {@link FitEllipseAlgebraic_F64 algebraic}
 * formulation. Points which are not approximately ellipsoidal are removed.
 * Approximately ellipsoidal is defined by the distance of the farthest contour point away from the ellipse. For
 * computational efficiency reasons a maximum of 20 points are sampled. If there are more than 20 points in
 * the contour then they are evenly sampled across the contour. Only external contours are considered.
 *
 * Parameters:
 * <dl>
 *    <dt>maxDistanceFromEllipse</dt>
 *    <dd>maximum distance from the ellipse in pixels</dd>
 *    <dt>minimumContour</dt>
 *    <dd>minimum number of pixels in the contour</dd>
 *    <dt>maximumContour</dt>
 *    <dd>maximum number of pixels in the contour</dd>
 *    <dt>internalContour</dt>
 *    <dd>If true it will process internal contours</dd>
 * </dl>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class BinaryEllipseDetectorPixel {
	// maximum distance from the ellipse in pixels
	private @Getter @Setter double maxDistanceFromEllipse = 3.0;

	// minimum number of pixels in the contour
	private @Getter @Setter int minimumContour = 20;
	// maximum number of pixels in the contour. 0 == no limit
	private @Getter @Setter int maximumContour = 0;

	// minimum number of pixels along the minor axis
	private @Getter @Setter double minimumMinorAxis = 1.5;

	// Can be used to filter out shapes which are very skinny
	private @Getter @Setter double maxMajorToMinorRatio = Double.MAX_VALUE;

	// connect rule used with contour finding
	private final @Getter ConnectRule connectRule;
	// allow it to switch between two algorithms
	private @Nullable BinaryLabelContourFinder contourFinder;
	private final GrayS32 labeled = new GrayS32(1, 1);
	private @Nullable BinaryContourFinderLinearExternal contourExternal;

	private final FitEllipseAlgebraic_F64 algebraic = new FitEllipseAlgebraic_F64();

	private final ClosestPointEllipseAngle_F64 closestPoint = new ClosestPointEllipseAngle_F64(1e-4f, 15);

	// transforms which can be used to handle lens distortion
	protected @Nullable PixelTransform<Point2D_F32> distToUndist;
	protected Point2D_F32 distortedPoint = new Point2D_F32();

	private @Nullable PrintStream verbose = null;

	private final DogArray<Point2D_F64> pointsF = new DogArray<>(Point2D_F64::new);

	private final DogArray<Found> found = new DogArray<>(Found::new);

	// temporary storage for a contour
	private final DogArray<Point2D_I32> contourTmp = new DogArray<>(Point2D_I32::new);

	public BinaryEllipseDetectorPixel( ConnectRule connectRule ) {
		this.connectRule = connectRule;
		declareContour(false);
	}

	public BinaryEllipseDetectorPixel() {
		this(ConnectRule.FOUR);
	}

	/**
	 * <p>Specifies transforms which can be used to change coordinates from distorted to undistorted.
	 * The undistorted image is never explicitly created.</p>
	 *
	 * <p>
	 * WARNING: The undistorted image must have the same bounds as the distorted input image. This is because
	 * several of the bounds checks use the image shape. This are simplified greatly by this assumption.
	 * </p>
	 *
	 * @param distToUndist Transform from distorted to undistorted image.
	 */
	public void setLensDistortion( @Nullable PixelTransform<Point2D_F32> distToUndist ) {
		this.distToUndist = distToUndist;
	}

	/**
	 * Finds all valid ellipses in the binary image
	 *
	 * @param binary binary image
	 */
	public void process( GrayU8 binary ) {
		found.reset();

		final BinaryContourInterface selectedFinder = getContourFinder();

		selectedFinder.setMaxContour(maximumContour == 0 ? Integer.MAX_VALUE : maximumContour);
		selectedFinder.setMinContour(minimumContour);

		if (isInternalContour()) {
			Objects.requireNonNull(contourFinder).process(binary, labeled);
		} else {
			Objects.requireNonNull(contourExternal).process(binary);
		}

		List<ContourPacked> blobs = selectedFinder.getContours();

		for (int i = 0; i < blobs.size(); i++) {
			ContourPacked c = blobs.get(i);

			selectedFinder.loadContour(c.externalIndex, contourTmp);
			proccessContour(contourTmp.toList(), binary.width, binary.height);

			if (isInternalContour()) {
				for (int j = 0; j < c.internalIndexes.size(); j++) {
					selectedFinder.loadContour(c.internalIndexes.get(j), contourTmp);
					proccessContour(contourTmp.toList(), binary.width, binary.height);
				}
			}
		}
	}

	private void proccessContour( List<Point2D_I32> contour, final int width, final int height ) {
		// No longer needed since contourFinder can have handle the limit internally now. Keeping this commented out
		// for quick sanity checks in the future
//		if (contour.size() < minimumContour || (maximumContour > 0 && contour.size() > maximumContour) ) {
//			if( verbose != null )
//				verbose.println("Rejecting: too small (or large) "+contour.size());
//			return;
//		}

		// discard shapes which touch the image border
		if (ContourOps.isTouchBorder(contour, width, height))
			return;

		pointsF.reset();
		undistortContour(contour, pointsF);

		// fit it to an ellipse. This will just be approximate. The more precise technique is much slower
		if (!algebraic.process(pointsF.toList())) {
			if (verbose != null)
				verbose.println("Rejecting: algebraic fit failed. size = " + pointsF.size());
			return;
		}

		EllipseQuadratic_F64 quad = algebraic.getEllipse();
		Found f = found.grow();
		UtilEllipse_F64.convert(quad, f.ellipse);

		boolean accepted = true;

		if (f.ellipse.b <= minimumMinorAxis) {
			if (verbose != null)
				verbose.println("Rejecting: Minor axis too small. size = " + f.ellipse.b);
			accepted = false;
		} else if (!isApproximatelyElliptical(f.ellipse, pointsF.toList(), 20)) {
			if (verbose != null)
				verbose.println("Rejecting: Not approximately elliptical. size = " + pointsF.size());
			accepted = false;
		} else if (f.ellipse.a > maxMajorToMinorRatio*f.ellipse.b) {
			if (verbose != null)
				verbose.println("Rejecting: Major to minor axis length ratio too extreme = " + pointsF.size());
			accepted = false;
		}

		if (accepted) {
			if (verbose != null)
				verbose.println("Success!  size = " + pointsF.size());

			adjustElipseForBinaryBias(f.ellipse);
			f.contour = contour;
		} else {
			found.removeTail();
		}
	}

	/**
	 * In a binary image the contour on the right and bottom is off by one pixel. This is because the block region
	 * extends the entire pixel not just the lower extent which is where it is indexed from.
	 */
	protected void adjustElipseForBinaryBias( EllipseRotated_F64 ellipse ) {
		ellipse.center.x += 0.5;
		ellipse.center.y += 0.5;

		ellipse.a += 0.5;
		ellipse.b += 0.5;
	}

	/**
	 * Undistort the contour points and convert into a floating point format for the fitting operation
	 *
	 * @param external The external contour
	 * @param pointsF Output of converted points
	 */
	void undistortContour( List<Point2D_I32> external, DogArray<Point2D_F64> pointsF ) {
		for (int j = 0; j < external.size(); j++) {
			Point2D_I32 p = external.get(j);

			if (distToUndist != null) {
				distToUndist.compute(p.x, p.y, distortedPoint);
				pointsF.grow().setTo(distortedPoint.x, distortedPoint.y);
			} else {
				pointsF.grow().setTo(p.x, p.y);
			}
		}
	}

	/**
	 * Look at the maximum distance contour points are from the ellipse and see if they exceed a maximum threshold
	 */
	boolean isApproximatelyElliptical( EllipseRotated_F64 ellipse, List<Point2D_F64> points, int maxSamples ) {

		closestPoint.setEllipse(ellipse);

		double maxDistance2 = maxDistanceFromEllipse*maxDistanceFromEllipse;

		if (points.size() <= maxSamples) {
			for (int i = 0; i < points.size(); i++) {
				Point2D_F64 p = points.get(i);
				closestPoint.process(p);
				double d = closestPoint.getClosest().distance2(p);

				if (d > maxDistance2) {
					return false;
				}
			}
		} else {
			for (int i = 0; i < maxSamples; i++) {
				Point2D_F64 p = points.get(i*points.size()/maxSamples);
				closestPoint.process(p);
				double d = closestPoint.getClosest().distance2(p);

				if (d > maxDistance2) {
					return false;
				}
			}
		}
		return true;
	}

	public List<ContourPacked> getContours() {
		return getContourFinder().getContours();
	}

	public void loadContour( int id, DogArray<Point2D_I32> storage ) {
		getContourFinder().loadContour(id, storage);
	}

	public BinaryContourInterface getContourFinder() {
		if (contourFinder != null)
			return contourFinder;
		else
			return Objects.requireNonNull(contourExternal);
	}

	public boolean isVerbose() {
		return verbose != null;
	}

	public boolean isInternalContour() {
		return contourFinder != null;
	}

	public void setInternalContour( boolean internalContour ) {
		if (internalContour == isInternalContour())
			return;
		declareContour(internalContour);
	}

	private void declareContour( boolean internalContour ) {
		if (internalContour) {
			contourFinder = FactoryBinaryContourFinder.linearChang2004();
			contourFinder.setConnectRule(connectRule);
			contourExternal = null;
		} else {
			contourExternal = FactoryBinaryContourFinder.linearExternal();
			contourExternal.setConnectRule(connectRule);
			// If these are not set then a black border is added to the input image
			contourExternal.setCreatePaddedCopy(true);
			contourExternal.setCoordinateAdjustment(1, 1);
			contourFinder = null;
		}
	}

	public void setVerbose( PrintStream verbose ) {
		this.verbose = verbose;
	}

	public List<Found> getFound() {
		return found.toList();
	}

	@SuppressWarnings({"NullAway.Init"})
	public static class Found {
		/**
		 * Computed ellipse in undistorted pixel coordinates
		 */
		public EllipseRotated_F64 ellipse = new EllipseRotated_F64();
		/**
		 * Contour in distorted pixel coordinates
		 */
		public List<Point2D_I32> contour;
	}
}

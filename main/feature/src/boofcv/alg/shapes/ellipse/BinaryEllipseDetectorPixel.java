/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.LinearContourLabelChang2004;
import boofcv.struct.ConnectRule;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import georegression.fitting.ellipse.ClosestPointEllipseAngle_F64;
import georegression.fitting.ellipse.FitEllipseAlgebraic;
import georegression.geometry.UtilEllipse_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.EllipseQuadratic_F64;
import georegression.struct.shapes.EllipseRotated_F64;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * <p>Detects ellipses inside a binary image by finding their contour and fitting an ellipse to the contour.  Fitting
 * is done using pixel precise contour points.  See {@link SnapToEllipseEdge} for a way to use sub-pixel points
 * and improve the fit's accuracy.  Optionally, lens distortion can be removed from the contour points prior
 * to processing.</p>
 *
 * After the contour has been found an ellipse is fit to them using the {@link FitEllipseAlgebraic algebraic}
 * formulation.  Points which are not approximately ellipsoidal are removed.
 * Approximately ellipsoidal is defined by the distance of the farthest contour point away from the ellipse. For
 * computational efficiency reasons a maximum of 20 points are sampled.  If there are more than 20 points in
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
public class BinaryEllipseDetectorPixel {

	// maximum distance from the ellipse in pixels
	private double maxDistanceFromEllipse = 3.0;

	// minimum number of pixels in the contour
	private int minimumContour = 20;
	// maximum number of pixels in the contour. 0 == no limit
	private int maximumContour = 0;

	// Can be used to filter out shapes which are very skinny
	private double maxMajorToMinorRatio = Double.MAX_VALUE;

	private boolean internalContour = false;

	private LinearContourLabelChang2004 contourFinder = new LinearContourLabelChang2004(ConnectRule.FOUR);
	private GrayS32 labeled = new GrayS32(1,1);

	private FitEllipseAlgebraic algebraic = new FitEllipseAlgebraic();

	private ClosestPointEllipseAngle_F64 closestPoint = new ClosestPointEllipseAngle_F64(1e-8,100);

	// transforms which can be used to handle lens distortion
	protected PixelTransform2_F32 distToUndist;

	private boolean verbose = false;

	private FastQueue<Point2D_F64> pointsF = new FastQueue<>(Point2D_F64.class, true);

	private FastQueue<Found> found = new FastQueue<>(Found.class, true);

	/**
	 * <p>Specifies transforms which can be used to change coordinates from distorted to undistorted.
	 * The undistorted image is never explicitly created.</p>
	 *
	 * <p>
	 * WARNING: The undistorted image must have the same bounds as the distorted input image.  This is because
	 * several of the bounds checks use the image shape.  This are simplified greatly by this assumption.
	 * </p>
	 *
	 * @param distToUndist Transform from distorted to undistorted image.
	 */
	public void setLensDistortion( PixelTransform2_F32 distToUndist ) {
		this.distToUndist = distToUndist;
	}

	/**
	 * Finds all valid ellipses in the binary image
	 * @param binary binary image
	 */
	public void process( GrayU8 binary ) {
		found.reset();
		labeled.reshape(binary.width, binary.height);

		contourFinder.process(binary, labeled);

		FastQueue<Contour> blobs = contourFinder.getContours();
		for (int i = 0; i < blobs.size; i++) {
			Contour c = blobs.get(i);

			proccessContour(c.external);

			if(internalContour) {
				for( int j = 0; j < c.internal.size(); j++ ) {
					proccessContour(c.internal.get(j));
				}
			}
		}
	}

	private void proccessContour(List<Point2D_I32> contour) {
		if (contour.size() < minimumContour || (maximumContour > 0 && contour.size() > maximumContour) ) {
			if( verbose )
				System.out.println("Rejecting: too small (or large) "+contour.size());
			return;
		}

		// discard shapes which touch the image border
		if( touchesBorder(contour) )
			return;

		pointsF.reset();
		undistortContour(contour,pointsF);

		// fit it to an ellipse.  This will just be approximate.  The more precise technique is much slower
		if( !algebraic.process(pointsF.toList())) {
			if( verbose )
				System.out.println("Rejecting: algebraic fit failed. size = "+pointsF.size());
			return;
		}

		EllipseQuadratic_F64 quad = algebraic.getEllipse();
		Found f = found.grow();
		UtilEllipse_F64.convert(quad,f.ellipse);

		if( !isApproximatelyElliptical(f.ellipse,pointsF.toList(),20)) {
			if( verbose )
				System.out.println("Rejecting: Not approximately elliptical. size = "+pointsF.size());
			found.removeTail();
		} else if( f.ellipse.a > maxMajorToMinorRatio*f.ellipse.b ) {
			if( verbose )
				System.out.println("Rejecting: Major to minor axis length ratio too extreme = "+pointsF.size());
			found.removeTail();
		}

		if( verbose )
			System.out.println("Success!  size = "+pointsF.size());

		f.contour = contour;
	}

	protected final boolean touchesBorder( List<Point2D_I32> contour ) {
		int endX = labeled.width-1;
		int endY = labeled.height-1;

		for (int j = 0; j < contour.size(); j++) {
			Point2D_I32 p = contour.get(j);
			if( p.x == 0 || p.y == 0 || p.x == endX || p.y == endY )
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Undistort the contour points and convert into a floating point format for the fitting operation
	 *
	 * @param external The external contour
	 * @param pointsF Output of converted points
	 */
	void undistortContour(List<Point2D_I32> external, FastQueue<Point2D_F64> pointsF ) {
		for (int j = 0; j < external.size(); j++) {
			Point2D_I32 p = external.get(j);

			if( distToUndist != null ) {
				distToUndist.compute(p.x,p.y);
				pointsF.grow().set( distToUndist.distX , distToUndist.distY );
			} else {
				pointsF.grow().set(p.x, p.y);
			}
		}
	}

	/**
	 * Look at the maximum distance contour points are from the ellipse and see if they exceed a maximum threshold
	 */
	boolean isApproximatelyElliptical(EllipseRotated_F64 ellipse , List<Point2D_F64> points , int maxSamples ) {

		closestPoint.setEllipse(ellipse);

		double maxDistance2 = maxDistanceFromEllipse*maxDistanceFromEllipse;

		if( points.size() <= maxSamples ) {
			for( int i = 0; i < points.size(); i++ ) {
				Point2D_F64 p = points.get(i);
				closestPoint.process(p);
				double d = closestPoint.getClosest().distance2(p);

				if( d > maxDistance2 ) {
					return false;
				}
			}
		} else {
			for (int i = 0; i < maxSamples; i++) {
				Point2D_F64 p = points.get( i*points.size()/maxSamples );
				closestPoint.process(p);
				double d = closestPoint.getClosest().distance2(p);

				if( d > maxDistance2 ) {
					return false;
				}
			}
		}
		return true;
	}

	public LinearContourLabelChang2004 getContourFinder() {
		return contourFinder;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public boolean isInternalContour() {
		return internalContour;
	}

	public void setInternalContour(boolean internalContour) {
		this.internalContour = internalContour;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public double getMaxDistanceFromEllipse() {
		return maxDistanceFromEllipse;
	}

	public void setMaxDistanceFromEllipse(double maxDistanceFromEllipse) {
		this.maxDistanceFromEllipse = maxDistanceFromEllipse;
	}

	public int getMinimumContour() {
		return minimumContour;
	}

	public void setMinimumContour(int minimumContour) {
		this.minimumContour = minimumContour;
	}

	public int getMaximumContour() {
		return maximumContour;
	}

	public void setMaximumContour(int maximumContour) {
		this.maximumContour = maximumContour;
	}

	public double getMaxMajorToMinorRatio() {
		return maxMajorToMinorRatio;
	}

	public void setMaxMajorToMinorRatio(double maxMajorToMinorRatio) {
		this.maxMajorToMinorRatio = maxMajorToMinorRatio;
	}

	public List<Found> getFound() {
		return found.toList();
	}

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

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
import boofcv.struct.distort.PixelTransform_F32;
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
 * Detects ellipses inside a binary image by finding their contour.  Optionally, lens distortion can be removed
 * from the contour points prior to processing.  After the contour has been found an ellipse is fit to them using
 * the {@link FitEllipseAlgebraic algebraic} formulation.  Points which are not approximately ellipsoidal are removed.
 * Approximately ellipsoidal is defined by the distance of the farthest contour point away from the ellipse. For
 * computational efficiency reasons a maximum of 20 points are sampled.  If there are more than 20 points in
 * the contour then they are evenly sampled across the contour.
 *
 * Parameters:
 * <dl>
 *    <dt>maxDistanceFromEllipse</dt>
 *    <dd>maximum distance from the ellipse in pixels</dd>
 *    <dt>minimumContour</dt>
 *    <dd>minimum number of pixels in the contour</dd>
 *    <dt>maximumContour</dt>
 *    <dd>maximum number of pixels in the contour</dd>
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

	private LinearContourLabelChang2004 contourFinder = new LinearContourLabelChang2004(ConnectRule.FOUR);
	private GrayS32 labeled = new GrayS32(1,1);

	private FitEllipseAlgebraic algebraic = new FitEllipseAlgebraic();

	private ClosestPointEllipseAngle_F64 closestPoint = new ClosestPointEllipseAngle_F64(1e-8,100);

	// transforms which can be used to handle lens distortion
	protected PixelTransform_F32 distToUndist, undistToDist;

	private boolean verbose = false;

	private FastQueue<Point2D_F64> pointsF = new FastQueue<Point2D_F64>(Point2D_F64.class,true);

	private FastQueue<Found> found = new FastQueue<Found>(Found.class,true);


	/**
	 * <p>Specifies transforms which can be used to change coordinates from distorted to undistorted and the opposite
	 * coordinates.  The undistorted image is never explicitly created.</p>
	 *
	 * <p>
	 * WARNING: The undistorted image must have the same bounds as the distorted input image.  This is because
	 * several of the bounds checks use the image shape.  This are simplified greatly by this assumption.
	 * </p>
	 *
	 * @param width Input image width.  Used in sanity check only.
	 * @param height Input image height.  Used in sanity check only.
	 * @param distToUndist Transform from distorted to undistorted image.
	 * @param undistToDist Transform from undistorted to distorted image.
	 */
	public void setLensDistortion(int width , int height ,
								  PixelTransform_F32 distToUndist , PixelTransform_F32 undistToDist ) {
		this.distToUndist = distToUndist;
		this.undistToDist = undistToDist;
	}


	/**
	 * Finds all valid ellipses in the binary image
	 * @param binary binary image
	 */
	public void process( GrayU8 binary ) {
		labeled.reshape(binary.width, binary.height);

		contourFinder.process(binary, labeled);

		FastQueue<Contour> blobs = contourFinder.getContours();
		for (int i = 0; i < blobs.size; i++) {
			Contour c = blobs.get(i);

			if (c.external.size() < minimumContour || (maximumContour != 0 && c.external.size() > maximumContour) ) {
				continue;
			}

			pointsF.reset();
			undistortContour(c.external,pointsF);

			// fit it to an ellipse.  This will just be approximate.  The more precise technique is much slower
			if( !algebraic.process(pointsF.toList())) {
				continue;
			}

			EllipseQuadratic_F64 quad = algebraic.getEllipse();
			Found f = found.grow();
			UtilEllipse_F64.convert(quad,f.ellipse);

			if( !isApproximatelyElliptical(f.ellipse,pointsF.toList(),20)) {
				found.removeTail();
			}

			f.contour = c.external;
		}
	}

	/**
	 * Undistort the contour points and convert into a floating point format for the fitting operation
	 *
	 * @param external The external contour
	 * @param pointsF Output of converted points
	 */
	private void undistortContour(List<Point2D_I32> external, FastQueue<Point2D_F64> pointsF ) {
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
	private boolean isApproximatelyElliptical(EllipseRotated_F64 ellipse , List<Point2D_F64> points , int maxSamples ) {

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

	public boolean isVerbose() {
		return verbose;
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

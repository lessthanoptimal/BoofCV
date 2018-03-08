/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.square;

import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.abst.geo.EstimateNofPnP;
import boofcv.abst.geo.RefinePnP;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.line.LineParametric3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Estimates the pose using P3P and iterative refinement from 4 points on a plane with known locations.  While
 * this seems like it would be a trivial problem it actually takes several techniques to ensure accurate results.
 * At a high level it uses P3P to provide an estimate.  If the error is large it then uses EPNP.  Which ever
 * is better it then refines.  If the target is small and directly facing the camera it will enlarge the target
 * to estimate it's orientation.  Otherwise it will over fit location since it takes a large change in orientation
 * to influence the result.
 *
 * @author Peter Abeles
 */
public class QuadPoseEstimator {

	// if the target is less than or equals to this number of pixels along a side then it is considered small
	// and a special case will be handled.
	// I think this threshold should be valid across different resolution images.  Corner accuracy should be
	// less than a pixel and it becomes unstable because changes in hangle result in an error of less than a pixel
	// when the target is small
	public static final double SMALL_PIXELS = 60.0;

	// the adjusted solution is accepted if it doesn't increase the pixel reprojection error more than this amount
	public static final double FUDGE_FACTOR = 0.5;

	// provides set of hypotheses from 3 points
	private EstimateNofPnP p3p;
	// iterative refinement
	private RefinePnP refine;

	private Estimate1ofPnP epnp = FactoryMultiView.computePnP_1(EnumPNP.EPNP,50,0);

	// transforms from distorted pixel observation normalized image coordinates
	protected Point2Transform2_F64 pixelToNorm;
	protected Point2Transform2_F64 normToPixel;

	// storage for inputs to estimation algorithms
	// observations in normalized image coordinates
	protected List<Point2D3D> points = new ArrayList<>();

	// observation in undistorted pixels
	protected List<Point2D_F64> listObs = new ArrayList<>();

	private List<Point2D3D> inputP3P = new ArrayList<>();
	private FastQueue<Se3_F64> solutions = new FastQueue(Se3_F64.class,true);
	private Se3_F64 outputFiducialToCamera = new Se3_F64();
	private Se3_F64 foundEPNP = new Se3_F64();

	// error for outputFiducialToCamera
	private double outputError;

	private Point3D_F64 cameraP3 = new Point3D_F64();
	private Point2D_F64 predicted = new Point2D_F64();

	// storage for when it searches for the best solution
	protected double bestError;
	protected Se3_F64 bestPose = new Se3_F64();

	// predeclared internal work space.  Minimizing new memory
	Quadrilateral_F64 pixelCorners = new Quadrilateral_F64();
	Quadrilateral_F64 normCorners = new Quadrilateral_F64();

	Point2D_F64 center = new Point2D_F64();

	// used to store the ray pointing from the camera to the marker in marker reference frame
	LineParametric3D_F64 ray = new LineParametric3D_F64();

	/**
	 * Constructor which picks reasonable and generally good algorithms for pose estimation.
	 *
	 * @param refineTol  Convergence tolerance.  Try 1e-8
	 * @param refineIterations Number of refinement iterations.  Try 200
	 */
	public QuadPoseEstimator( double refineTol , int refineIterations ) {
		this(FactoryMultiView.computePnP_N(EnumPNP.P3P_GRUNERT, -1),
				FactoryMultiView.refinePnP(refineTol,refineIterations));
	}

	/**
	 * Constructor in which internal estimation algorithms are provided
	 */
	public QuadPoseEstimator(EstimateNofPnP p3p, RefinePnP refine) {
		this.p3p = p3p;
		this.refine = refine;

		for (int i = 0; i < 4; i++) {
			points.add( new Point2D3D() );
		}
	}

	/**
	 * Specifies the intrinsic parameters.
	 * @param distortion Intrinsic camera parameters
	 */
	public void setLensDistoriton(LensDistortionNarrowFOV distortion ) {
		pixelToNorm = distortion.undistort_F64(true,false);
		normToPixel = distortion.distort_F64(false, true);
	}

	/**
	 * Specify the location of points on the 2D fiducial.  These should be in "world coordinates"
	 */
	public void setFiducial( double x0 , double y0 , double x1 , double y1 ,
							 double x2 , double y2 , double x3 , double y3 ) {
		points.get(0).location.set(x0,y0,0);
		points.get(1).location.set(x1,y1,0);
		points.get(2).location.set(x2,y2,0);
		points.get(3).location.set(x3,y3,0);
	}

	/**
	 * Given the found solution, compute the the observed pixel would appear on the marker's surface.
	 * pixel -> normalized pixel -> rotated -> projected on to plane
	 * @param pixelX (Input) pixel coordinate
	 * @param pixelY (Input) pixel coordinate
	 * @param marker (Output) location on the marker
	 */
	public void pixelToMarker( double pixelX , double pixelY , Point2D_F64 marker ) {

		// find pointing vector in camera reference frame
		pixelToNorm.compute(pixelX,pixelY,marker);
		cameraP3.set(marker.x,marker.y,1);

		// rotate into marker reference frame
		GeometryMath_F64.multTran(outputFiducialToCamera.R,cameraP3,ray.slope);
		GeometryMath_F64.multTran(outputFiducialToCamera.R,outputFiducialToCamera.T,ray.p);
		ray.p.scale(-1);

		double t = -ray.p.z/ray.slope.z;

		marker.x = ray.p.x + ray.slope.x*t;
		marker.y = ray.p.y + ray.slope.y*t;
	}

	/**
	 * <p>Estimate the 3D pose of the camera from the observed location of the fiducial.</p>
	 *
	 * MUST call {@link #setFiducial} and {@link #setLensDistoriton} before calling this function.
	 *
	 * @param corners Observed corners of the fiducial.
	 * @param unitsPixels If true the specified corners are in  original image pixels or false for normalized image coordinates
	 * @return true if successful or false if not
	 */
	public boolean process(Quadrilateral_F64 corners, boolean unitsPixels) {

		if( unitsPixels ) {
			pixelCorners.set(corners);
			pixelToNorm.compute(corners.a.x, corners.a.y, normCorners.a);
			pixelToNorm.compute(corners.b.x, corners.b.y, normCorners.b);
			pixelToNorm.compute(corners.c.x, corners.c.y, normCorners.c);
			pixelToNorm.compute(corners.d.x, corners.d.y, normCorners.d);
		} else {
			normCorners.set(corners);
			normToPixel.compute(corners.a.x, corners.a.y, pixelCorners.a);
			normToPixel.compute(corners.b.x, corners.b.y, pixelCorners.b);
			normToPixel.compute(corners.c.x, corners.c.y, pixelCorners.c);
			normToPixel.compute(corners.d.x, corners.d.y, pixelCorners.d);
		}

		if( estimate(pixelCorners, normCorners, outputFiducialToCamera) ) {
			outputError = computeErrors(outputFiducialToCamera);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Given the observed corners of the quad in the image in pixels estimate and store the results
	 * of its pose
	 */
	protected boolean estimate( Quadrilateral_F64 cornersPixels ,
								Quadrilateral_F64 cornersNorm ,
								Se3_F64 foundFiducialToCamera ) {
		// put it into a list to simplify algorithms
		listObs.clear();
		listObs.add( cornersPixels.a );
		listObs.add( cornersPixels.b );
		listObs.add( cornersPixels.c );
		listObs.add( cornersPixels.d );

		// convert observations into normalized image coordinates which P3P requires
		points.get(0).observation.set(cornersNorm.a);
		points.get(1).observation.set(cornersNorm.b);
		points.get(2).observation.set(cornersNorm.c);
		points.get(3).observation.set(cornersNorm.d);

		// estimate pose using all permutations
		bestError = Double.MAX_VALUE;
		estimateP3P(0);
		estimateP3P(1);
		estimateP3P(2);
		estimateP3P(3);

		if( bestError == Double.MAX_VALUE )
			return false;

		// refine the best estimate
		inputP3P.clear();
		for( int i = 0; i < 4; i++ ) {
			inputP3P.add( points.get(i) );
		}

		// got poor or horrible solution the first way, let's try it with EPNP
		// and see if it does better
		if( bestError > 2 ) {
			if (epnp.process(inputP3P, foundEPNP)) {
				if( foundEPNP.T.z > 0 ) {
					double error = computeErrors(foundEPNP);
//					System.out.println("   error EPNP = "+error);
					if (error < bestError) {
						bestPose.set(foundEPNP);
					}
				}
			}
		}

		if( !refine.fitModel(inputP3P,bestPose,foundFiducialToCamera) ) {
			// us the previous estimate instead
			foundFiducialToCamera.set(bestPose);
			return true;
		}

		return true;
	}

	/**
	 * Estimates the pose using P3P from 3 out of 4 points.  Then use all 4 to pick the best solution
	 *
	 * @param excluded which corner to exclude and use to check the answers from the others
	 */
	protected void estimateP3P(int excluded) {

		// the point used to check the solutions is the last one
		inputP3P.clear();
		for( int i = 0; i < 4; i++ ) {
			if( i != excluded ) {
				inputP3P.add( points.get(i) );
			}
		}

		// initial estimate for the pose
		solutions.reset();
		if( !p3p.process(inputP3P,solutions) ) {
//			System.err.println("PIP Failed!?! That's weird");
			return;
		}


		for (int i = 0; i < solutions.size; i++) {
			double error = computeErrors(solutions.get(i));

			// see if it's better and it should save the results
			if( error < bestError ) {
				bestError = error;
				bestPose.set(solutions.get(i));
			}
		}

	}

	/**
	 * Enlarges the quadrilateral to make it more sensitive to changes in orientation
	 */
	protected void enlarge( Quadrilateral_F64 corners, double scale ) {

		UtilPolygons2D_F64.center(corners, center);

		extend(center,corners.a,scale);
		extend(center,corners.b,scale);
		extend(center,corners.c,scale);
		extend(center,corners.d,scale);
	}

	protected void extend( Point2D_F64 pivot , Point2D_F64 corner , double scale ) {
		corner.x = pivot.x + (corner.x-pivot.x)*scale;
		corner.y = pivot.y + (corner.y-pivot.y)*scale;
	}

	/**
	 * Compute the sum of reprojection errors for all four points
	 * @param fiducialToCamera Transform being evaluated
	 * @return sum of Euclidean-squared errors
	 */
	protected double computeErrors(Se3_F64 fiducialToCamera ) {
		if( fiducialToCamera.T.z < 0 ) {
			// the low level algorithm should already filter this code, but just incase
			return Double.MAX_VALUE;
		}

		double maxError = 0;

		for( int i = 0; i < 4; i++ ) {
			maxError = Math.max(maxError,computePixelError(fiducialToCamera, points.get(i).location, listObs.get(i)));
		}

		return maxError;
	}

	private double computePixelError( Se3_F64 fiducialToCamera , Point3D_F64 X , Point2D_F64 pixel ) {
		SePointOps_F64.transform(fiducialToCamera,X,cameraP3);

		normToPixel.compute( cameraP3.x/cameraP3.z , cameraP3.y/cameraP3.z , predicted );

		return predicted.distance(pixel);
	}

	public Se3_F64 getWorldToCamera() {
		return outputFiducialToCamera;
	}

	/**
	 * Reprojection error of fiducial
	 */
	public double getError() {
		return outputError;
	}

	public List<Point2D3D> createCopyPoints2D3D() {
		List<Point2D3D> out = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			out.add( points.get(i).copy() );
		}
		return out;
	}
}

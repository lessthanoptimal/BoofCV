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

package boofcv.alg.fiducial.square;

import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.abst.geo.EstimateNofPnP;
import boofcv.abst.geo.RefinePnP;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import georegression.geometry.UtilPolygons2D_F64;
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
	private Point2Transform2_F64 normToUndistorted;
	private Point2Transform2_F64 pixelToNorm;
	private Point2Transform2_F64 normToPixel;

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
	CameraPinholeRadial intrinsicUndist = new CameraPinholeRadial();
	Quadrilateral_F64 pixelCorners = new Quadrilateral_F64();
	Quadrilateral_F64 enlargedCorners = new Quadrilateral_F64();
	Se3_F64 foundEnlarged = new Se3_F64();
	Se3_F64 foundRegular = new Se3_F64();
	Point2D_F64 center = new Point2D_F64();

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
	 * Specify the location of points on the 2D fiducial.
	 */
	public void setFiducial( double x0 , double y0 , double x1 , double y1 ,
							 double x2 , double y2 , double x3 , double y3 ) {
		points.get(0).location.set(x0,y0,0);
		points.get(1).location.set(x1,y1,0);
		points.get(2).location.set(x2,y2,0);
		points.get(3).location.set(x3,y3,0);
	}

	/**
	 * <p>Estimate the 3D pose of the camera from the observed location of the fiducial.</p>
	 *
	 * MUST call {@link #setFiducial} and {@link #setLensDistoriton} before calling this function.
	 *
	 * @param corners Observed corners of the fiducial in normalized image coordinates.
	 * @return true if successful or false if not
	 */
	public boolean process( Quadrilateral_F64 corners ) {

		// put quad into undistorted pixels so that weird stuff doesn't happen when it expands
		normToPixel.compute(corners.a.x, corners.a.y, pixelCorners.a);
		normToPixel.compute(corners.b.x, corners.b.y, pixelCorners.b);
		normToPixel.compute(corners.c.x, corners.c.y, pixelCorners.c);
		normToPixel.compute(corners.d.x, corners.d.y, pixelCorners.d);

		double length0 =  pixelCorners.getSideLength(0);
		double length1 =  pixelCorners.getSideLength(1);

		double ratio = Math.max(length0,length1)/Math.min(length0,length1);

		// this is mainly an optimization thing.  The handling of the pathological cause is only
		// used if it doesn't add a bunch of error.  But this technique is only needed when certain conditions
		// are meet.
		boolean success;
//		if( ratio < 1.3 && length0 < SMALL_PIXELS && length1 < SMALL_PIXELS ) {
//			success = estimatePathological(outputFiducialToCamera);
//		} else {
			success = estimate(pixelCorners, outputFiducialToCamera);
//		}

		if( success ) {
			outputError = computeErrors(outputFiducialToCamera);
		}
		return success;
	}

	/**
	 * Estimating the orientation is difficult when looking directly at a fiducial head on.  Basically
	 * when the target appears close to a perfect square.  What I believe is happening is that when
	 * the target is small significant changes in orientation only cause a small change in reprojection
	 * error.  So it over fits location and comes up with some crazy orientation.
	 *
	 * To add more emphasis on orientation  target is enlarged, which shouldn't change orientation, but now a small
	 * change in orientation results in a large error.  The translational component is still estimated the
	 * usual way.  To ensure that this technique isn't hurting the state estimate too much it checks to
	 * see if the error has increased too much.
	 *
	 * TODO Potential improvement.  Non-linear refinement on translation only after estimating orientation.
	 *      The location estimate uses the over fit result still in the code below.
	 *
	 * @return true if successful false if not
	 */
	private boolean estimatePathological( Se3_F64 outputFiducialToCamera ) {
		enlargedCorners.set(pixelCorners);
		enlarge(enlargedCorners, 4);

		if( !estimate(enlargedCorners, foundEnlarged) )
			return false;

		if( !estimate(pixelCorners, foundRegular) )
			return false;

		double errorRegular = computeErrors(foundRegular);

		outputFiducialToCamera.getT().set(foundRegular.getT());
		outputFiducialToCamera.getR().set(foundEnlarged.getR());

		double errorModified = computeErrors(outputFiducialToCamera);

		// todo do a weighted average of the two estimates so the transition is smooth
		// if the solutions are very similar go with the enlarged version
		if (errorModified > errorRegular + FUDGE_FACTOR ) {
			outputFiducialToCamera.set(foundRegular);
		}
		return true;
	}

	/**
	 * Given the observed corners of the quad in the image in pixels estimate and store the results
	 * of its pose
	 */
	protected boolean estimate( Quadrilateral_F64 corners , Se3_F64 foundFiducialToCamera ) {
		// put it into a list to simplify algorithms
		listObs.clear();
		listObs.add( corners.a );
		listObs.add( corners.b );
		listObs.add( corners.c );
		listObs.add( corners.d );

		// convert observations into normalized image coordinates which P3P requires
		pixelToNorm.compute(corners.a.x,corners.a.y,points.get(0).observation);
		pixelToNorm.compute(corners.b.x,corners.b.y,points.get(1).observation);
		pixelToNorm.compute(corners.c.x,corners.c.y,points.get(2).observation);
		pixelToNorm.compute(corners.d.x,corners.d.y,points.get(3).observation);

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
			System.out.println("Failed!?!");
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

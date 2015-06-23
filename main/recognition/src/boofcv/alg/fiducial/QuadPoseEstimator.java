/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial;

import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.abst.geo.EstimateNofPnP;
import boofcv.abst.geo.RefinePnP;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.geo.Point2D3D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Estimates the pose using P3P and iterative refinement from 4 points on a plane with known locations.
 *
 * @author Peter Abeles
 */
public class QuadPoseEstimator {

	// provides set of hypotheses from 3 points
	EstimateNofPnP p3p;
	// iterative refinement
	RefinePnP refine;

	Estimate1ofPnP epnp = FactoryMultiView.computePnP_1(EnumPNP.EPNP,50,0);

	// transforms from distorted pixel observation normalized image coordinates
	PointTransform_F64 pixelToNorm;
	PointTransform_F64 normToPixel;

	// storage for inputs to estimation algorithms
	Point2D3D[] points = new Point2D3D[4];

	List<Point2D_F64> listObs = new ArrayList<Point2D_F64>();

	List<Point2D3D> inputP3P = new ArrayList<Point2D3D>();
	FastQueue<Se3_F64> solutions = new FastQueue(Se3_F64.class,true);
	Se3_F64 refinedFiducialToCamera = new Se3_F64();
	Se3_F64 foundEPNP = new Se3_F64();

	Point3D_F64 cameraP3 = new Point3D_F64();
	Point2D_F64 predicted = new Point2D_F64();

	// storage for when it searches for the best solution
	double bestError;
	Se3_F64 bestPose = new Se3_F64();
	int bestIndex;

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
			points[i] = new Point2D3D();
		}
	}

	/**
	 * Specifies the intrinsic parameters.
	 * @param intrinsic Intrinsic camera parameters
	 */
	public void setIntrinsic( IntrinsicParameters intrinsic ) {
		pixelToNorm = LensDistortionOps.distortTransform(intrinsic).undistort_F64(true,false);
		normToPixel = LensDistortionOps.distortTransform(intrinsic).distort_F64(false, true);
	}

	/**
	 * Specify the location of points on the 2D fiducial.
	 */
	public void setFiducial( double x0 , double y0 , double x1 , double y1 ,
							 double x2 , double y2 , double x3 , double y3 ) {
		points[0].location.set(x0,y0,0);
		points[1].location.set(x1,y1,0);
		points[2].location.set(x2,y2,0);
		points[3].location.set(x3,y3,0);
	}

	/**
	 * <p>Estimate the 3D pose of the camera from the observed location of the fiducial.</p>
	 *
	 * MUST call {@link #setFiducial} and {@link #setIntrinsic} before calling this function.
	 *
	 * @param corners Observed corners of fiducials which are in the same order as their 2D counter parts
	 * @return true if successful or false if not
	 */
	public boolean process( Quadrilateral_F64 corners ) {

		// put it into a list to simplify algorithms
		listObs.clear();
		listObs.add( corners.a );
		listObs.add( corners.b );
		listObs.add( corners.c );
		listObs.add( corners.d );

		// convert obervations into normalized image coordinates which P3P requires
		pixelToNorm.compute(corners.a.x,corners.a.y,points[0].observation);
		pixelToNorm.compute(corners.b.x,corners.b.y,points[1].observation);
		pixelToNorm.compute(corners.c.x,corners.c.y,points[2].observation);
		pixelToNorm.compute(corners.d.x,corners.d.y,points[3].observation);

		// estimate pose using all permutations
		bestError = Double.MAX_VALUE;
		estimate(0);
		estimate(1);
		estimate(2);
		estimate(3);

		if( bestError == Double.MAX_VALUE )
			return false;

//		System.out.println("bestError = "+bestError);

		// refine the best estimate
		inputP3P.clear();
		for( int i = 0; i < 4; i++ ) {
			inputP3P.add( points[i] );
		}

		// got poor or horrible solution the first way, let's try it with EPNP
		// and see if it does better
		if( bestError > 2 ) {
			if (epnp.process(inputP3P, foundEPNP)) {
				if( foundEPNP.T.z > 0 ) {
					double error = computeErrors(foundEPNP);
					if (error < bestError) {
						bestPose.set(foundEPNP);
//						System.out.println("    better epnp error = " + error);
					}
				}
			}
		}

//		refinedFiducialToCamera.set(bestPose);
		if( !refine.fitModel(inputP3P,bestPose,refinedFiducialToCamera) ) {
			// us the previous estimate instead
			refinedFiducialToCamera.set(bestPose);
			return true;
		}

//		double refineError = computeErrors(refinedFiducialToCamera);
//		System.out.println("    refined error = "+refineError);

		return true;
	}

	/**
	 * Estimates the pose from all put the excluded point
	 * @param excluded which corner to exclude and use to check the answers from the others
	 */
	private void estimate( int excluded ) {

		// the point used to check the solutions is the last one
		inputP3P.clear();
		for( int i = 0; i < 4; i++ ) {
			if( i != excluded ) {
				inputP3P.add( points[i] );
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
				bestIndex = excluded;
			}
		}

	}

	/**
	 * Compute the sum of preprojection errors for all four points
	 * @param fiducialToCamera Transform being evaluated
	 * @return sum of error
	 */
	private double computeErrors(Se3_F64 fiducialToCamera ) {
		if( fiducialToCamera.T.z < 0 ) {
			// the low level algorithm should already filter this code, but just incase
			return Double.MAX_VALUE;
		}

		double error = 0;

		for( int i = 0; i < 4; i++ ) {
			error += computePixelError(fiducialToCamera,points[i].location,listObs.get(i));
		}

		return error;
	}

	private double computePixelError( Se3_F64 fiducialToCamera , Point3D_F64 X , Point2D_F64 pixel ) {
		SePointOps_F64.transform(fiducialToCamera,X,cameraP3);

		normToPixel.compute( cameraP3.x/cameraP3.z , cameraP3.y/cameraP3.z , predicted );

		return predicted.distance2(pixel);
	}

	public Se3_F64 getWorldToCamera() {
		return refinedFiducialToCamera;
	}
}

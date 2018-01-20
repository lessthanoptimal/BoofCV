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

package boofcv.examples.sfm;

import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.abst.geo.RefinePnP;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.factory.geo.*;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.fitting.modelset.ransac.Ransac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The Perspective-n-Point problem or PnP for short, is a problem where there are N observations of points with
 * known 3D coordinates.  The output is the pose of the camera observing the points.  The minimal solution requires
 * 3-points but produces multiple solutions.  BoofCV provide several solutions to the PnP problem and the example
 * below demonstrates how to use them.
 *
 * @author Peter Abeles
 */
public class ExamplePnP {

	// describes the intrinsic camera parameters.
	CameraPinholeRadial intrinsic = new CameraPinholeRadial(500,490,0,320,240,640,480).fsetRadial(0.1,-0.05);

	// Used to generate random observations
	Random rand = new Random(234);

	public static void main(String[] args) {
		// create an arbitrary transform from world to camera reference frames
		Se3_F64 worldToCamera = new Se3_F64();
		worldToCamera.getT().set(5, 10, -7);
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.1,-0.3,0,worldToCamera.getR());

		ExamplePnP app = new ExamplePnP();

		// Let's generate observations with no outliers
		// NOTE: Image observations are in normalized image coordinates NOT pixels
		List<Point2D3D> observations = app.createObservations(worldToCamera,100);

		System.out.println("Truth:");
		worldToCamera.print();
		System.out.println();
		System.out.println("Estimated, assumed no outliers:");
		app.estimateNoOutliers(observations).print();
		System.out.println("Estimated, assumed that there are outliers:");
		app.estimateOutliers(observations).print();

		System.out.println();
		System.out.println("Adding outliers");
		System.out.println();

		// add a bunch of outliers
		app.addOutliers(observations, 50);
		System.out.println("Estimated, assumed no outliers:");
		app.estimateNoOutliers(observations).print();
		System.out.println("Estimated, assumed that there are outliers:");
		app.estimateOutliers(observations).print();
	}

	/**
	 * Assumes all observations actually match the correct/real 3D point
	 */
	public Se3_F64 estimateNoOutliers( List<Point2D3D> observations ) {

		// Compute a single solution using EPNP
		// 10 iterations is what JavaDoc recommends, but might need to be tuned.
		// 0 test points.  This parameters is actually ignored because EPNP only returns a single solution
		Estimate1ofPnP pnp = FactoryMultiView.computePnP_1(EnumPNP.EPNP, 10, 0);

		Se3_F64 worldToCamera = new Se3_F64();
		pnp.process(observations,worldToCamera);

		// For some applications the EPNP solution might be good enough, but let's refine it
		RefinePnP refine = FactoryMultiView.refinePnP(1e-8,200);

		Se3_F64 refinedWorldToCamera = new Se3_F64();

		if( !refine.fitModel(observations,worldToCamera,refinedWorldToCamera) )
			throw new RuntimeException("Refined failed! Input probably bad...");

		return refinedWorldToCamera;
	}

	/**
	 * Uses robust techniques to remove outliers
	 */
	public Se3_F64 estimateOutliers( List<Point2D3D> observations ) {
		// We can no longer trust that each point is a real observation.  Let's use RANSAC to separate the points
		// You will need to tune the number of iterations and inlier threshold!!!
		Ransac<Se3_F64,Point2D3D> ransac =
				FactoryMultiViewRobust.pnpRansac(new ConfigPnP(intrinsic),new ConfigRansac(300,1.0));

		// Observations must be in normalized image coordinates!  See javadoc of pnpRansac
		if( !ransac.process(observations) )
			throw new RuntimeException("Probably got bad input data with NaN inside of it");

		System.out.println("Inlier size "+ransac.getMatchSet().size());
		Se3_F64 worldToCamera = ransac.getModelParameters();

		// You will most likely want to refine this solution too.  Can make a difference with real world data
		RefinePnP refine = FactoryMultiView.refinePnP(1e-8,200);

		Se3_F64 refinedWorldToCamera = new Se3_F64();

		// notice that only the match set was passed in
		if( !refine.fitModel(ransac.getMatchSet(),worldToCamera,refinedWorldToCamera) )
			throw new RuntimeException("Refined failed! Input probably bad...");

		return refinedWorldToCamera;
	}

	/**
	 * Generates synthetic observations randomly in front of the camera.  Observations are in normalized image
	 * coordinates and not pixels!  See {@link PerspectiveOps#convertPixelToNorm} for how to go from pixels
	 * to normalized image coordinates.
	 */
	public List<Point2D3D> createObservations( Se3_F64 worldToCamera , int total ) {

		Se3_F64 cameraToWorld = worldToCamera.invert(null);

		// transform from pixel coordinates to normalized pixel coordinates, which removes lens distortion
		Point2Transform2_F64 pixelToNorm = LensDistortionOps.narrow(intrinsic).undistort_F64(true,false);

		List<Point2D3D> observations = new ArrayList<>();

		Point2D_F64 norm = new Point2D_F64();
		for (int i = 0; i < total; i++) {
			// randomly pixel a point inside the image
			double x = rand.nextDouble()*intrinsic.width;
			double y = rand.nextDouble()*intrinsic.height;

			pixelToNorm.compute(x,y,norm);

			// Randomly pick a depth and compute 3D coordinate
			double Z = rand.nextDouble()+4;
			double X = norm.x*Z;
			double Y = norm.y*Z;

			// Change the point's reference frame from camera to world
			Point3D_F64 cameraPt = new Point3D_F64(X,Y,Z);
			Point3D_F64 worldPt = new Point3D_F64();

			SePointOps_F64.transform(cameraToWorld,cameraPt,worldPt);

			// Save the perfect noise free observation
			Point2D3D o = new Point2D3D();
			o.getLocation().set(worldPt);
			o.getObservation().set(norm.x,norm.y);

			observations.add(o);
		}

		return observations;
	}

	/**
	 * Adds some really bad observations to the mix
	 */
	public void addOutliers( List<Point2D3D> observations , int total ) {

		int size = observations.size();

		for (int i = 0; i < total; i++) {
			// outliers will be created by adding lots of noise to real observations
			Point2D3D p = observations.get(rand.nextInt(size));

			Point2D3D o = new Point2D3D();
			o.observation.set(p.observation);
			o.location.x = p.location.x + rand.nextGaussian()*5;
			o.location.y = p.location.y + rand.nextGaussian()*5;
			o.location.z = p.location.z + rand.nextGaussian()*5;

			observations.add(o);
		}

		// randomize the order
		Collections.shuffle(observations,rand);
	}
}

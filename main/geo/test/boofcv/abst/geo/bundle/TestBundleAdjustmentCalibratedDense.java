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

package boofcv.abst.geo.bundle;

import boofcv.alg.geo.GeoTestingOps;
import boofcv.alg.geo.bundle.CalibPoseAndPointResiduals;
import boofcv.alg.geo.bundle.CalibratedPoseAndPoint;
import boofcv.alg.geo.bundle.PointIndexObservation;
import boofcv.alg.geo.bundle.ViewPointObservations;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestBundleAdjustmentCalibratedDense {

	Random rand = new Random(234);
	int numViews = 2;
	int numPoints = 4;

	/**
	 * Perfect observations and model
	 */
	@Test
	public void perfect() {

		CalibratedPoseAndPoint model = createModel(numViews,numPoints,rand);
		List<ViewPointObservations> observations = createObservations(model,numViews,numPoints);
		
		BundleAdjustmentCalibratedDense alg = new BundleAdjustmentCalibratedDense(1e-8,500);
	
		assertTrue(alg.process(model, observations));
		
		// compute error, which should be zero
		assertEquals(0,computeError(model,observations),1e-8);
	}

	/**
	 * Perfect observations with a corrupted model.  See if the error decreases
	 */
	@Test
	public void noisy() {
		CalibratedPoseAndPoint model = createModel(numViews,numPoints,rand);
		List<ViewPointObservations> observations = createObservations(model,numViews,numPoints);

		BundleAdjustmentCalibratedDense alg = new BundleAdjustmentCalibratedDense(1e-12,500);

		// add noise to the model
		model.getWorldToCamera(0).getT().x += 0.02;
		model.getWorldToCamera(1).getT().y -= 0.01;

		double errorBefore = computeError(model,observations);

		assertTrue(alg.process(model, observations));

		double errorAfter = computeError(model,observations);

		// the error should be less now
		assertTrue(errorAfter<errorBefore*0.1);
	}

	private double computeError( CalibratedPoseAndPoint model ,
								 List<ViewPointObservations> observations )
	{
		CalibPoseAndPointResiduals func = new CalibPoseAndPointResiduals();
		func.configure(null,model,observations);
		double residuals[] = new double[ func.getNumOfOutputsM() ];

		func.process(model,residuals);

		return GeoTestingOps.residualError(residuals);
	}

	public static CalibratedPoseAndPoint createModel( int numViews ,
													  int numPoints ,
													  Random rand )
	{
		CalibratedPoseAndPoint model = new CalibratedPoseAndPoint();
	
		model.configure(numViews,numPoints);
		
		for( int i = 0; i < numViews; i++ ) {
			// create a small rotation
			double euler[] = new double[]{rand.nextGaussian()*1e-3,
					rand.nextGaussian()*1e-1,rand.nextGaussian()*1e-3};
			
			// move mostly along x-axis
			double x = i*0.1;
			double y = rand.nextGaussian()*0.0001;
			double z = rand.nextGaussian()*0.0001;

			Se3_F64 view = model.getWorldToCamera(i);
			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,euler[0],euler[1],euler[2],view.getR());
			view.getT().set(x,y,z);
		}
		
		for( int i = 0; i < numPoints; i++ ) {
			// put points a bit away in z direction
			Point3D_F64 p = model.getPoint(i);
			p.x = rand.nextGaussian()*0.1;
			p.y = rand.nextGaussian()*0.1;
			p.z = rand.nextGaussian()*0.1 + 3;
		}
		
		return model;
	}

	public static List<ViewPointObservations> createObservations(
			CalibratedPoseAndPoint model , int numViews , int numPoints  )
	{
		List<ViewPointObservations> ret = new ArrayList<>();
		
		for( int view = 0; view < numViews; view++ ) {

			ViewPointObservations l = new ViewPointObservations();
			
			Se3_F64 v = model.getWorldToCamera(view);
			Point3D_F64 cameraPt = new Point3D_F64();
			
			for( int j = 0; j < numPoints; j++ ) {
				SePointOps_F64.transform(v,model.getPoint(j),cameraPt);

				Point2D_F64 obs = new Point2D_F64();
				obs.x = cameraPt.x / cameraPt.z;
				obs.y = cameraPt.y / cameraPt.z;

				PointIndexObservation p = l.getPoints().grow();
				p.set(j,obs);
			}
			
			ret.add(l);
		}
		
		return ret;
	}
}

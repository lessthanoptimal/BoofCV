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

package boofcv.alg.geo.bundle;

import boofcv.alg.geo.GeoTestingOps;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static boofcv.alg.geo.bundle.TestCalibPoseAndPointRodiguesCodec.configure;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCalibPoseAndPointResiduals {

	Random rand = new Random(234);
	
	@Test
	public void changeInValue() {

		int numViews = 2;
		int numPoints = 3;

		// randomly configure the model
		CalibPoseAndPointRodriguesCodec codec = new CalibPoseAndPointRodriguesCodec();
		codec.configure(numViews,numPoints,numViews, new boolean[]{false,false});

		// create the true model
		CalibratedPoseAndPoint model = new CalibratedPoseAndPoint();
		configure(model,numViews,numPoints,rand);
		
		// generate observations from the model
		List<ViewPointObservations> obs = new ArrayList<>();
		
		for( int view = 0; view < numViews; view++ ) {
			Se3_F64 m = model.getWorldToCamera(view);

			ViewPointObservations v = new ViewPointObservations();
			
			Point3D_F64 cameraPt = new Point3D_F64();
			for( int i = 0; i < numPoints; i++ ) {
				SePointOps_F64.transform(m,model.getPoint(i),cameraPt);
				
				PointIndexObservation o = v.getPoints().grow();
				o.pointIndex = i;
				o.obs = new Point2D_F64();
				o.obs.x = cameraPt.x/cameraPt.z;
				o.obs.y = cameraPt.y/cameraPt.z;
			}

			obs.add(v);
		}
		
		CalibPoseAndPointResiduals alg = new CalibPoseAndPointResiduals();
		alg.configure(codec,model,obs);

		// give it perfect data
		double param[] = new double[ codec.getParamLength() ];
		codec.encode(model,param);
		
		double residuals[] = new double[alg.getNumOfOutputsM()];
		alg.process(param,residuals);
		
		assertEquals(0, GeoTestingOps.residualError(residuals),1e-8);

		// mess up the model a bit
		param[0] += 0.1;
		param[5] -= 2;

		alg.process(param,residuals);

		assertTrue(GeoTestingOps.residualError(residuals) > 1e-8);
	}
}

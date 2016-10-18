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

package boofcv.alg.geo.triangulate;

import boofcv.alg.geo.MultiViewOps;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class CommonTriangulationChecks {

	Random rand = new Random(234);
	
	int N = 30;

	protected Point3D_F64 worldPoint;
	protected List<Point2D_F64> obsPts;
	protected List<Se3_F64> motionWorldToCamera;
	protected List<DenseMatrix64F> essential;
	
	public void createScene() {
		worldPoint = new Point3D_F64(0.1,-0.2,4);
		motionWorldToCamera = new ArrayList<>();
		obsPts = new ArrayList<>();
		essential = new ArrayList<>();
		
		Point3D_F64 cameraPoint = new Point3D_F64();
		
		for( int i = 0; i < N; i++ ) {
			// random motion from world to frame 'i'
			Se3_F64 tranWtoI = new Se3_F64();
			if( i > 0 ) {
				tranWtoI.getR().set(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,
						rand.nextGaussian()*0.01, rand.nextGaussian()*0.05, rand.nextGaussian()*0.1,null ));
				tranWtoI.getT().set(0.2+rand.nextGaussian()*0.1, rand.nextGaussian()*0.1, rand.nextGaussian()*0.01);
			}

			DenseMatrix64F E = MultiViewOps.createEssential(tranWtoI.getR(), tranWtoI.getT());

			SePointOps_F64.transform(tranWtoI, worldPoint,cameraPoint);
			
			Point2D_F64 o = new Point2D_F64(cameraPoint.x/cameraPoint.z,cameraPoint.y/cameraPoint.z);

			obsPts.add(o);
			motionWorldToCamera.add(tranWtoI);
			essential.add(E);
		}
	}
}

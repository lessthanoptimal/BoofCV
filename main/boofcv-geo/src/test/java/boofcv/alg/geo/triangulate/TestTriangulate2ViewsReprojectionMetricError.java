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

package boofcv.alg.geo.triangulate;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestTriangulate2ViewsReprojectionMetricError extends BoofStandardJUnit {
	// pick two very different cameras to make sure the error is being indpendently computed in image each image correctly
	CameraPinhole cameraA = new CameraPinhole(400,400,0,500,500,1000,1000);
	CameraPinhole cameraB = new CameraPinhole(600,400,0,200,250,600,700);

	Se3_F64 a_to_b = SpecialEuclideanOps_F64.eulerXyz(-0.2,0,0,0,0,0,null);

	Point3D_F64 Xa = new Point3D_F64(0,0,1);
	Point3D_F64 Xb = new Point3D_F64();

	Point2D_F64 na = new Point2D_F64();
	Point2D_F64 nb = new Point2D_F64();

	@BeforeEach
	public void setup() {
		a_to_b.transform(Xa,Xb);

		na.setTo( Xa.x/Xa.z, Xa.y/Xa.z );
		nb.setTo( Xb.x/Xb.z, Xb.y/Xb.z );
	}

	@Test void perfect() {
		var alg = new Triangulate2ViewsReprojectionMetricError();
		alg.configure(cameraA,cameraB);

		assertEquals(0,alg.process(na,nb,a_to_b,Xa), UtilEjml.TEST_F64);
	}

	@Test void errorInA() {
		var alg = new Triangulate2ViewsReprojectionMetricError();
		alg.configure(cameraA,cameraB);

		Point2D_F64 pa = new Point2D_F64();
		PerspectiveOps.convertNormToPixel(cameraA,na.x,na.y,pa);
		pa.x += 0.5;
		PerspectiveOps.convertPixelToNorm(cameraA,pa.x,pa.y,na);

		assertEquals(0.5*0.5/2 , alg.process(na,nb,a_to_b,Xa), UtilEjml.TEST_F64);
	}

	@Test void errorInB() {
		var alg = new Triangulate2ViewsReprojectionMetricError();
		alg.configure(cameraA,cameraB);

		Point2D_F64 pb = new Point2D_F64();
		PerspectiveOps.convertNormToPixel(cameraB,nb.x,nb.y,pb);
		pb.x += 0.5;
		PerspectiveOps.convertPixelToNorm(cameraB,pb.x,pb.y,nb);

		assertEquals(0.5*0.5/2 , alg.process(na,nb,a_to_b,Xa), UtilEjml.TEST_F64);
	}
}

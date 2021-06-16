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

package boofcv.alg.geo.robust;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.Triangulate2ViewsMetricH;
import boofcv.factory.geo.ConfigTriangulation;
import boofcv.factory.geo.EnumFundamental;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedPair;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSe3FromEssentialGenerator extends BoofStandardJUnit {

	@Test void simpleTest() {
		// define motion
		Se3_F64 motion = new Se3_F64();
		motion.getR().setTo(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.1, -0.05, -0.01, null));
		motion.getT().setTo(2, -0.1, 0.1);

		// define observations
		List<AssociatedPair> obs = new ArrayList<>();

		for (int i = 0; i < 8; i++) {
			Point3D_F64 p = new Point3D_F64(rand.nextGaussian()*0.1, rand.nextGaussian()*0.1, 3 + rand.nextGaussian()*0.1);

			AssociatedPair o = new AssociatedPair();

			o.p1.x = p.x/p.z;
			o.p1.y = p.y/p.z;

			Point3D_F64 pp = new Point3D_F64();
			SePointOps_F64.transform(motion, p, pp);

			o.p2.x = pp.x/pp.z;
			o.p2.y = pp.y/pp.z;

			obs.add(o);
		}

		// create alg
		Estimate1ofEpipolar essentialAlg = FactoryMultiView.fundamental_1(EnumFundamental.LINEAR_8, 0);
		Triangulate2ViewsMetricH triangulate = FactoryMultiView.triangulate2ViewMetricH(
				new ConfigTriangulation(ConfigTriangulation.Type.GEOMETRIC));

		Se3FromEssentialGenerator alg = new Se3FromEssentialGenerator(essentialAlg, triangulate);

		Se3_F64 found = new Se3_F64();

		// recompute the motion
		assertTrue(alg.generate(obs, found));

		// account for scale difference
		double scale = found.getT().norm()/motion.getT().norm();

		assertTrue(MatrixFeatures_DDRM.isIdentical(motion.getR(), found.getR(), 1e-6));

		assertEquals(motion.getT().x*scale, found.getT().x, 1e-8);
		assertEquals(motion.getT().y*scale, found.getT().y, 1e-8);
		assertEquals(motion.getT().z*scale, found.getT().z, 1e-8);
	}
}

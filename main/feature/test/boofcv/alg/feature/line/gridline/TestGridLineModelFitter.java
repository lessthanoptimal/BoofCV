/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.line.gridline;

import boofcv.alg.feature.detect.line.gridline.Edgel;
import boofcv.alg.feature.detect.line.gridline.GridLineModelFitter;
import boofcv.numerics.fitting.modelset.HypothesisList;
import georegression.metric.UtilAngle;
import georegression.struct.line.LinePolar2D_F32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestGridLineModelFitter {


	/**
	 * If only two points are passed in, they should fail if their orientations
	 * are more than the specified tolerance apart
	 */
	@Test
	public void checkFailInCompatible() {
		GridLineModelFitter alg = new GridLineModelFitter(0.1f);
		HypothesisList<LinePolar2D_F32> models = new HypothesisList<LinePolar2D_F32>(alg);

		// angle test should use half-circle and this should pass
		List<Edgel> l = new ArrayList<Edgel>();
		l.add( new Edgel(0,0,(float)Math.PI/2f));
		l.add( new Edgel(1,0,(float)-Math.PI/2f));

		alg.generate(l,models);
		assertEquals(1,models.size());

		// this one should fail
		l.clear();
		l.add( new Edgel(0,0,(float)Math.PI/2f));
		l.add( new Edgel(1,0,(float)Math.PI/2f-0.5f));

		models.reset();
		alg.generate(l,models);
		assertEquals(0, models.size());
	}

	@Test
	public void checkFit() {
		GridLineModelFitter alg = new GridLineModelFitter(0.1f);
		HypothesisList<LinePolar2D_F32> models = new HypothesisList<LinePolar2D_F32>(alg);

		// angle test should use half-circle and this should pass
		List<Edgel> l = new ArrayList<Edgel>();
		l.add( new Edgel(1,0,0f));
		l.add( new Edgel(1,2,(float)Math.PI));

		alg.generate(l, models);
		assertEquals(1,models.get(0).distance,1e-4f);
		assertTrue(UtilAngle.distHalf(0, models.get(0).angle) < 1e-4f);

		// three points
		l.add( new Edgel(1,3,(float)-Math.PI/2f));
		models.reset();
		alg.generate(l, models);
		assertEquals(1,models.get(0).distance,1e-4f);
		assertTrue(UtilAngle.distHalf(0, models.get(0).angle) < 1e-4f);
	}
}

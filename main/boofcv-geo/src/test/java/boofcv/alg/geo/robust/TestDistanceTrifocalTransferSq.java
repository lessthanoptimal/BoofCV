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

package boofcv.alg.geo.robust;

import boofcv.alg.geo.trifocal.CommonTrifocalChecks;
import boofcv.factory.geo.EnumTrifocal;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedTriple;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDistanceTrifocalTransferSq extends CommonTrifocalChecks {
	@Test
	public void perfect() {
		// estimate the tensors to ensure it's in pixels
		FactoryMultiView.trifocal_1(EnumTrifocal.LINEAR_7,-1).process(observationsPixels,found);

		DistanceTrifocalTransferSq alg = new DistanceTrifocalTransferSq();
		alg.setModel(found);

		for(AssociatedTriple a : observationsPixels) {
			assertEquals(0,alg.computeDistance(a), UtilEjml.TEST_F64);
		}
	}

	@Test
	public void noise() {
		// estimate the tensors to ensure it's in pixels
		FactoryMultiView.trifocal_1(EnumTrifocal.LINEAR_7,-1).process(observationsPixels,found);

		DistanceTrifocalTransferSq alg = new DistanceTrifocalTransferSq();
		alg.setModel(found);

		double error = 0.5;
		AssociatedTriple tmp = new AssociatedTriple();
		for(AssociatedTriple a : observationsPixels) {
			tmp.set(a);
			tmp.p3.x += error;

			// the error will be larger than this value but not too much larger
			double min = error*error;
			double max = 1.5*min;

			double found = alg.computeDistance(tmp);
			assertTrue( found >= min && found <= max );
		}
	}
}

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

package boofcv.alg.geo.trifocal;

import boofcv.struct.geo.AssociatedTriple;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRefineThreeViewProjectiveGeometric extends CommonTrifocalChecks{
	@Test void perfect() {
		RefineThreeViewProjectiveGeometric alg = new RefineThreeViewProjectiveGeometric();

		List<AssociatedTriple> originalPixels = new ArrayList<>();
		for (int i = 0; i < observationsPixels.size(); i++) {
			originalPixels.add(observationsPixels.get(i).copy());
		}
		DMatrixRMaj originalP2 = P2.copy();
		DMatrixRMaj originalP3 = P3.copy();

		alg.refine(observations,P2,P3);

		// make sure there were no changes
		for (int i = 0; i < originalPixels.size(); i++) {
			assertTrue(originalPixels.get(i).isIdentical(observationsPixels.get(i), UtilEjml.TEST_F64));
		}

		assertTrue(MatrixFeatures_DDRM.isIdentical(originalP2,P2,1e-3));
		assertTrue(MatrixFeatures_DDRM.isIdentical(originalP3,P3,1e-3));
	}

	/**
	 * Small error in initial parameters
	 */
	@Test void incorrectInitial() {
		RefineThreeViewProjectiveGeometric alg = new RefineThreeViewProjectiveGeometric();

		List<AssociatedTriple> originalPixels = new ArrayList<>();
		for (int i = 0; i < observationsPixels.size(); i++) {
			originalPixels.add(observationsPixels.get(i).copy());
		}

		P2.data[4] += 0.6;
		P3.data[11] += 0.6;

		alg.refine(observations,P2,P3);

		// make sure there were no changes
		for (int i = 0; i < originalPixels.size(); i++) {
			assertTrue(originalPixels.get(i).isIdentical(observationsPixels.get(i), UtilEjml.TEST_F64));
		}

		// Use the fit score since there are an infinite number of solutions
		assertEquals(0, alg.sba.getFitScore(), 1e-8);
	}
}

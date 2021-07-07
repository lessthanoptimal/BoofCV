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

package boofcv.abst.geo.fitting;

import boofcv.testing.BoofStandardJUnit;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Peter Abeles
 */
public class TestModelManagerEpipolarMatrix extends BoofStandardJUnit {

	@Test void createModelInstance() {
		ModelManagerEpipolarMatrix alg = new ModelManagerEpipolarMatrix();
		DMatrixRMaj found = alg.createModelInstance();

		assertNotNull(found);
		assertEquals(3, found.getNumRows());
		assertEquals(3,found.getNumCols());
	}

	@Test void copyModel() {
		ModelManagerEpipolarMatrix alg = new ModelManagerEpipolarMatrix();

		DMatrixRMaj m = new DMatrixRMaj(3,3);
		for( int i = 0; i < 9; i++ )
			m.data[i] = i+1;
		DMatrixRMaj copy = new DMatrixRMaj(3,3);

		alg.copyModel(m,copy);
		for( int i = 0; i < 9; i++ )
			assertEquals(i+1,copy.data[i],1e-8);
	}
}

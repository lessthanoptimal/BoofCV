/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestModelManagerEpipolarMatrix {

	@Test
	public void createModelInstance() {
		ModelManagerEpipolarMatrix alg = new ModelManagerEpipolarMatrix();
		DenseMatrix64F found = alg.createModelInstance();

		assertTrue( found != null );
		assertEquals(3, found.getNumRows());
		assertEquals(3,found.getNumCols());
	}

	@Test
	public void copyModel() {
		ModelManagerEpipolarMatrix alg = new ModelManagerEpipolarMatrix();

		DenseMatrix64F m = new DenseMatrix64F(3,3);
		for( int i = 0; i < 9; i++ )
			m.data[i] = i+1;
		DenseMatrix64F copy = new DenseMatrix64F(3,3);

		alg.copyModel(m,copy);
		for( int i = 0; i < 9; i++ )
			assertEquals(i+1,copy.data[i],1e-8);
	}

}

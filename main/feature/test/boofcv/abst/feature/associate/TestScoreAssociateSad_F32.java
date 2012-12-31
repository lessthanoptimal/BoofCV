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

package boofcv.abst.feature.associate;

import boofcv.struct.feature.TupleDesc_F32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestScoreAssociateSad_F32 {

	@Test
	public void compareToExpected() {
		ScoreAssociateSad_F32 scorer = new ScoreAssociateSad_F32();

		TupleDesc_F32 a = new TupleDesc_F32(5);
		TupleDesc_F32 b = new TupleDesc_F32(5);

		a.value=new float[]{1,2,3,4,5};
		b.value=new float[]{-1,2,6,3,6};

		assertEquals(7,scorer.score(a,b),1e-2);
	}

	@Test
	public void checkZeroMinimum() {
		ScoreAssociateSad_F32 scorer = new ScoreAssociateSad_F32();
		assertTrue(scorer.isZeroMinimum());
	}
}

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

package boofcv.alg.feature.associate;

import boofcv.struct.feature.TupleDesc_U8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestScoreAssociateSad_U8 {

	@Test
	public void compareToExpected() {
		ScoreAssociateSad_U8 scorer = new ScoreAssociateSad_U8();

		TupleDesc_U8 a = new TupleDesc_U8(5);
		TupleDesc_U8 b = new TupleDesc_U8(5);

		a.value=new byte[]{1,2,3,4,5};
		b.value=new byte[]{6,2,6,3,6};

		assertEquals(10,scorer.score(a,b),1e-2);
	}

	@Test
	public void checkZeroMinimum() {
		ScoreAssociateSad_U8 scorer = new ScoreAssociateSad_U8();
		assertTrue(scorer.isZeroMinimum());
	}
}

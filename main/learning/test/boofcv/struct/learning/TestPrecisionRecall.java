/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.learning;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestPrecisionRecall {
	double TP=1.0,TN=2.0,FP=3.0,FN=4.0;
	@Test
	public void getTruePositive() {
		PrecisionRecall p = new PrecisionRecall(1,2,3,4);

		assertEquals(1, p.getTruePositive(), 1e-8);
	}

	@Test
	public void getTrueNegative() {
		PrecisionRecall p = new PrecisionRecall(1,2,3,4);

		assertEquals(2, p.getTrueNegative(), 1e-8);
	}

	@Test
	public void getFalsePositive() {
		PrecisionRecall p = new PrecisionRecall(1,2,3,4);

		assertEquals(3, p.getFalsePositive(), 1e-8);
	}

	@Test
	public void getFalseNegative() {
		PrecisionRecall p = new PrecisionRecall(1,2,3,4);

		assertEquals(4, p.getFalseNegative(), 1e-8);
	}

	@Test
	public void getFMeasure() {
		PrecisionRecall p = new PrecisionRecall(1,2,3,4);

		double expected = 2.0*(p.getPrecision()*p.getRecall())/(p.getPrecision()+p.getRecall());

		assertEquals(expected,p.getFMeasure(),1e-8);
	}

	@Test
	public void getPrecision() {
		PrecisionRecall p = new PrecisionRecall(TP,TN,FP,FN);

		double expected =  TP/(TP+FP);
		assertEquals(expected,p.getPrecision(),1e-8);
	}

	@Test
	public void getRecall() {
		PrecisionRecall p = new PrecisionRecall(TP,TN,FP,FN);

		double expected =  TP/(TP+FN);
		assertEquals(expected, p.getRecall(), 1e-8);
	}
}

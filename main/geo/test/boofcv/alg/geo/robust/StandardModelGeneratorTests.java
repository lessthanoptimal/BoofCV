/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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


import org.ddogleg.fitting.modelset.ModelGenerator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Basic tests for model fitters
 *
 * @author Peter Abeles
 */
public abstract class StandardModelGeneratorTests<Model, Point> {

	int dof;

	protected Random rand = new Random(234);
	ModelTestingInterface<Model,Point> helper;

	protected StandardModelGeneratorTests(ModelTestingInterface<Model,Point> helper,int dof) {
		this.dof = dof;
		this.helper = helper;
	}

	/**
	 * Creates a new model fitter\
	 */
	public abstract ModelGenerator<Model, Point> createAlg();

	public abstract Model createModelInstance();

	public void allTest() {
		checkMinPoints();
		simpleTest();
	}

	@Test
	public void checkMinPoints() {
		ModelGenerator<Model, Point> fitter = createAlg();
		assertEquals(dof,fitter.getMinimumPoints());
	}

	/**
	 * Give it points which have been transform by the true affine model.  See
	 * if the transform is correctly estimated
	 */
	@Test
	public void simpleTest() {

		Model model = helper.createRandomModel();

		List<Point> dataSet = new ArrayList<>();

		// give it perfect observations
		for( int i = 0; i < 10; i++ ) {
			Point p = helper.createRandomPointFromModel(model);
			dataSet.add(p);
		}

		ModelGenerator<Model, Point> fitter = createAlg();

		Model found = createModelInstance();
		assertTrue(fitter.generate(dataSet,found));

		// test the found transform by seeing if it recomputes the current points
		assertTrue(helper.doPointsFitModel(found, dataSet));
	}

}

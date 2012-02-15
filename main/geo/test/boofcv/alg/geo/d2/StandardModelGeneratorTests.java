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

package boofcv.alg.geo.d2;


import boofcv.numerics.fitting.modelset.HypothesisList;
import boofcv.numerics.fitting.modelset.ModelGenerator;
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

	protected StandardModelGeneratorTests(int dof) {
		this.dof = dof;
	}

	/**
	 * Creates a new model fitter\
	 */
	public abstract ModelGenerator<Model, Point> createAlg();

	/**
	 * Creates a random model
	 */
	public abstract Model createRandomModel();

	/**
	 * Creates a random point that fits the provided model
	 */
	public abstract Point createRandomPointFromModel( Model model );

	/**
	 * Checks to see of the dat set are described by the model correctly
	 */
	public abstract boolean doPointsFitModel( Model model , List<Point> dataSet );

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

		Model model = createRandomModel();

		List<Point> dataSet = new ArrayList<Point>();

		// give it perfect observations
		for( int i = 0; i < 10; i++ ) {
			Point p = createRandomPointFromModel(model);
			dataSet.add(p);
		}

		ModelGenerator<Model, Point> fitter = createAlg();

		HypothesisList<Model> list = new HypothesisList<Model>(fitter);
		fitter.generate(dataSet,list);
		assertEquals(1,list.size());

		// test the found transform by seeing if it recomputes the current points
		assertTrue(doPointsFitModel(list.get(0),dataSet));
	}

}

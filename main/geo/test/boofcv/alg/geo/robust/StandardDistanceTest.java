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

import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * Standard tests to apply to implementors of {@link DistanceFromModel}.
 *
 * @author Peter Abeles
 */
public abstract class StandardDistanceTest<Model,Data> {
	
	public abstract DistanceFromModel<Model,Data> create();

	public abstract Model createRandomModel();

	public abstract Data createRandomData();

	public abstract double distance( Model model , Data data );

	@Test
	public void testSingle() {
		DistanceFromModel<Model,Data> alg = create();

		Model m = createRandomModel();
		alg.setModel(m);

		for( int i = 0; i < 10; i++ ) {
			Data d = createRandomData();

			assertEquals(distance(m,d),alg.computeDistance(d),1e-4);
		}
	}

	@Test
	public void testMultiple() {
		DistanceFromModel<Model,Data> alg = create();

		Model m = createRandomModel();
		alg.setModel(m);

		List<Data> obs = new ArrayList<>();
		double expected[] = new double[10];
		double found[] = new double[10];

		for( int i = 0; i < 10; i++ ) {
			Data d = createRandomData();
			obs.add(d);
			expected[i] = distance(m,d);
		}

		alg.computeDistance(obs,found);

		for( int i = 0; i < 10; i++ ) {
			assertEquals(expected[i],found[i],1e-4);
		}
	}
}

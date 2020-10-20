/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class TestDistanceFromModelIntoViews extends BoofStandardJUnit {
	@Test
	void mock() {
		var orig = new DistanceFromModel() {
			@Override
			public void distances( List list, double[] distance ) {
				for (int i = 0; i < list.size(); i++) {
					distance[i] = 6;
				}
			}

			@Override public void setModel( Object o ) {}
			@Override public double distance( Object pt ) { return 2; }
			@Override public Class getPointType() { return Double.class; }
			@Override public Class getModelType() { return Integer.class; }
		};

		var alg = new DistanceFromModelIntoViews(orig, 3);

		assertEquals(3, alg.getNumberOfViews());
		assertEquals(2.0, alg.distance(null), 1e-8);
		List list = new ArrayList();
		double[] distances = new double[5];
		for (int i = 0; i < distances.length; i++) {
			list.add(new Object());
		}
		alg.distances(list, distances);
		for (int i = 0; i < distances.length; i++) {
			assertEquals(6, distances[i], 1e-8);
		}
		assertEquals(Double.class, alg.getPointType());
		assertEquals(Integer.class, alg.getModelType());
	}
}

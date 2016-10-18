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

package boofcv.abst.geo.pose;

import boofcv.abst.geo.EstimateNofPnP;
import boofcv.struct.geo.Point2D3D;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * General tests for implementations of EstimateNofPnP.  Ensures that the returned motion estimate is in
 * the correct direction and works under nominal conditions
 *
 *
 * @author Peter Abeles
 */
public abstract class CheckEstimateNofPnP extends BaseChecksPnP {

	// algorithm being tested
	EstimateNofPnP alg;
	// true if it can only process the minimum number of observations
	boolean onlyMinimum;

	FastQueue<Se3_F64> solutions = new FastQueue<>(1, Se3_F64.class, true);

	Se3_F64 worldToCamera0 = new Se3_F64();
	Se3_F64 worldToCamera1 = new Se3_F64();

	protected CheckEstimateNofPnP(boolean onlyMinimum) {
		this.onlyMinimum = onlyMinimum;
	}

	public void setAlgorithm( EstimateNofPnP alg ) {
		this.alg = alg;
	}

	/**
	 * Feed it perfect observations and see if it returns nearly perfect results
	 */
	@Test
	public void perfectObservations() {
		if( !onlyMinimum ) {
			// test it with extra observations
			perfectObservations(alg.getMinimumPoints()+20);
		}
		// test it with the minimum number
		perfectObservations(alg.getMinimumPoints());
	}

	private void perfectObservations( int numSample ) {
		perfectObservations(worldToCamera0,numSample);
		perfectObservations(worldToCamera1, numSample);
	}

	private void perfectObservations( Se3_F64 worldToCamera , int numSample ) {
		List<Point2D3D> inputs = createObservations(worldToCamera,numSample);

		assertTrue(alg.process(inputs,solutions));

		int numMatched = 0;
		for( Se3_F64 found : solutions.toList() ) {
			if(!MatrixFeatures.isIdentical(worldToCamera.getR(), found.getR(), 1e-8))
				continue;

			if( !found.getT().isIdentical(worldToCamera.getT(), 1e-8))
				continue;

			numMatched++;
		}

		assertTrue(numMatched > 0);
	}

	/**
	 * All observations are behind the camera.  It should reject these
	 */
	@Test
	public void checkAllObservationsBehindCamera() {
		// This test is intentionally left blank.  If the point is behind the camera it shouldn't be observable so
		// it is reasonable for the PnP solution to come up with something crazy
	}

	/**
	 * Call it multiple times and make sure the same solutions are returned.
	 */
	@Test
	public void checkMultipleCalls() {
		List<Point2D3D> inputs = createObservations(worldToCamera0,alg.getMinimumPoints());

		assertTrue(alg.process(inputs,solutions));
		assertTrue(solutions.size() > 0 );

		List<Se3_F64> orig = new ArrayList<>();
		for( Se3_F64 m : solutions.toList() ) {
			orig.add(m.copy());
		}

		// run it a few times and see if the output is identical
		for( int i = 0; i < 2; i++ ) {
			assertTrue(alg.process(inputs,solutions));
			assertEquals(orig.size(),solutions.size());
			for( int j = 0; j < orig.size(); j++ ) {
				Se3_F64 o = orig.get(j);
				Se3_F64 f = solutions.get(j);

				assertTrue(MatrixFeatures.isIdentical(o.getR(), f.getR(), 1e-8));
				assertTrue(f.getT().isIdentical(o.getT(), 1e-8));
			}
		}
	}

	/**
	 * Sanity check to see if the minimum number of observations has been set.
	 */
	@Test
	public void checkMinimumPoints() {
		assertTrue(alg.getMinimumPoints() != 0);
	}

	@Test
	public void checkZerosInput() {
		List<Point2D3D> inputs = new ArrayList<>();

		for( int i = 0; i < 3; i++ ) {
			inputs.add( new Point2D3D());
		}

		assertFalse(alg.process(inputs, solutions));
		assertTrue(solutions.size() == 0 );
	}
}

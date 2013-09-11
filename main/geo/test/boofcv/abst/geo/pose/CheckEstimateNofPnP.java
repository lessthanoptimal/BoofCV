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

package boofcv.abst.geo.pose;

import boofcv.abst.geo.EstimateNofPnP;
import boofcv.alg.geo.f.EpipolarTestSimulation;
import boofcv.struct.geo.Point2D3D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
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
public abstract class CheckEstimateNofPnP extends EpipolarTestSimulation {

	// algorithm being tested
	EstimateNofPnP alg;
	// true if it can only process the minimum number of observations
	boolean onlyMinimum;

	FastQueue<Se3_F64> solutions = new FastQueue<Se3_F64>(1,Se3_F64.class,true);

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
		init(numSample,false);

		List<Point2D3D> inputs = new ArrayList<Point2D3D>();

		for( int i = 0; i < currentObs.size(); i++ ) {
			Point2D_F64 o = currentObs.get(i);
			Point3D_F64 X = worldPts.get(i);

			inputs.add( new Point2D3D(o,X));
		}

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
	 * Call it multiple times and make sure the same solutions are returned.
	 */
	@Test
	public void checkMultipleCalls() {
		init(alg.getMinimumPoints(),false);

		List<Point2D3D> inputs = new ArrayList<Point2D3D>();

		for( int i = 0; i < currentObs.size(); i++ ) {
			Point2D_F64 o = currentObs.get(i);
			Point3D_F64 X = worldPts.get(i);

			inputs.add( new Point2D3D(o,X));
		}

		assertTrue(alg.process(inputs,solutions));
		assertTrue(solutions.size() > 0 );

		List<Se3_F64> orig = new ArrayList<Se3_F64>();
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
		List<Point2D3D> inputs = new ArrayList<Point2D3D>();

		for( int i = 0; i < 3; i++ ) {
			inputs.add( new Point2D3D());
		}

		assertFalse(alg.process(inputs, solutions));
		assertTrue(solutions.size() == 0 );
	}
}

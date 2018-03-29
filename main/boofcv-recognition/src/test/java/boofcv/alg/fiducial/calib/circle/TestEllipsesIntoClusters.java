/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib.circle;

import boofcv.alg.shapes.ellipse.BinaryEllipseDetector.EllipseInfo;
import georegression.misc.GrlConstants;
import georegression.struct.curve.EllipseRotated_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestEllipsesIntoClusters {
	/**
	 * Just make sure it doesn't blow up with an empty input
	 */
	@Test
	public void emptyInput() {
		EllipsesIntoClusters alg = new EllipsesIntoClusters(2.0,0.5,0.5);

		List<EllipseInfo> input = new ArrayList<>();
		List<List<EllipsesIntoClusters.Node>> output = new ArrayList<>();

		alg.process(input,output);

		assertEquals( 0 , output.size());
	}

	/**
	 * Makes sure the output is cleared
	 */
	@Test
	public void outputCleared() {
		EllipsesIntoClusters alg = new EllipsesIntoClusters(2.0,0.5,0.5);

		List<EllipseInfo> input = new ArrayList<>();
		List<List<EllipsesIntoClusters.Node>> output = new ArrayList<>();

		output.add( new ArrayList<EllipsesIntoClusters.Node>());

		alg.process(input,output);

		assertEquals( 0 , output.size());
	}

	/**
	 * Provide it a simple case to cluster and make sure everything is connected properly
	 */
	@Test
	public void checkConnections() {
		EllipsesIntoClusters alg = new EllipsesIntoClusters(2.1,0.5,0.5);

		List<EllipseInfo> input = new ArrayList<>();
		input.add(create(0  ,0,1,1,0));
		input.add(create(2.0,0,1,1,0));
		input.add(create(4.0,0,1,1,0));
		input.add(create(  0,2,1,1,0));
		input.add(create(2.0,2,1,1,0));
		input.add(create(4.0,2,1,1,0));

		List<List<EllipsesIntoClusters.Node>> output = new ArrayList<>();

		alg.process(input,output);

		assertEquals( 1 , output.size());
		List<EllipsesIntoClusters.Node> found = output.get(0);
		assertEquals( 6 , found.size());

		int histogram[] = new int[5];
		for( EllipsesIntoClusters.Node n : found ) {
			histogram[n.connections.size]++;
		}
		assertEquals(0, histogram[0]);
		assertEquals(0, histogram[1]);
		assertEquals(4, histogram[2]);
		assertEquals(2, histogram[3]);
		assertEquals(0, histogram[4]);
	}

	private EllipseInfo create( double x0 , double y0, double a, double b, double phi ) {
		EllipseInfo out = new EllipseInfo();
		out.ellipse = new EllipseRotated_F64(x0, y0, a, b, phi);
		out.averageInside = 10;
		out.averageOutside = 200;
		out.contour = new ArrayList<>();
		return out;
	}

	/**
	 * Points should not be clustered together due to distance apart
	 */
	@Test
	public void noCluster_distance() {
		EllipsesIntoClusters alg = new EllipsesIntoClusters(2.0,0.5,0.5);

		List<EllipseInfo> input = new ArrayList<>();
		input.add(create(0,0,2,1,0));
		input.add(create(4.1,0,2,1,0));

		alg.init(input);
		alg.connect(input);
		assertEquals( 2 , alg.clusters.size()); // each ellipse is its cluster

		// a positive case for sanity right at the border
		input.get(1).ellipse.center.x = 4;
		alg.init(input);
		alg.connect(input);
		assertEquals( 1 , alg.clusters.size());
	}

	/**
	 * Points should not be clustered together due difference in side lengths
	 */
	@Test
	public void noCluster_size() {
		EllipsesIntoClusters alg = new EllipsesIntoClusters(2.0,0.5,0.5);

		List<EllipseInfo> input = new ArrayList<>();
		input.add(create(0,0,2,1,0));
		input.add(create(2,0,0.999,1,0));

		alg.init(input);
		alg.connect(input);
		assertEquals( 2 , alg.clusters.size());

		// a positive case for sanity right at the border
		input.get(1).ellipse.a = 1.0;
		alg.init(input);
		alg.connect(input);
		assertEquals( 1 , alg.clusters.size());
	}

	@Test
	public void multipleClusters() {
		EllipsesIntoClusters alg = new EllipsesIntoClusters(2.0,0.5,0.5);

		// two clusters differentiated by size
		List<EllipseInfo> input = new ArrayList<>();
		input.add(create(0,0,2,1,0));
		input.add(create(1,0,2,1,0));
		input.add(create(1,0,8,1,0));

		input.add(create(0,0,8,1,0));
		input.add(create(2.2,0,2,1,0));
		input.add(create(2.2,0,8,1,0));

		List<List<EllipsesIntoClusters.Node>> output = new ArrayList<>();
		alg.process(input,output);
		assertEquals( 2 , output.size());

		for (int i = 0; i < 2; i++) {
			assertEquals( 3 , output.get(i).size());
			double expected = input.get(output.get(i).get(0).which).ellipse.a;
			for (int j = 0; j < 3; j++) {
				assertEquals(expected, input.get( output.get(i).get(j).which).ellipse.a, GrlConstants.TEST_F64);
			}
		}
	}

	@Test
	public void multipleCalls() {
		EllipsesIntoClusters alg = new EllipsesIntoClusters(2.0,0.5,0.5);

		// two clusters differentiated by size
		List<EllipseInfo> input = new ArrayList<>();
		input.add(create(0,0,2,1,0));
		input.add(create(1,0,2,1,0));
		input.add(create(1,0,8,1,0));

		input.add(create(0,0,8,1,0));
		input.add(create(2.2,0,2,1,0));
		input.add(create(2.2,0,8,1,0));

		List<List<EllipsesIntoClusters.Node>> output = new ArrayList<>();

		// call it twice to see if it resets
		alg.process(input,output);
		alg.process(input,output);

		assertEquals( 2 , output.size());
		for (int i = 0; i < 2; i++) {
			assertEquals(3, output.get(i).size());
		}
	}

	@Test
	public void joinClusters() {
		List<EllipsesIntoClusters.Node> mouth = new ArrayList<>();
		List<EllipsesIntoClusters.Node> food = new ArrayList<>();

		mouth.add( new EllipsesIntoClusters.Node());
		mouth.add( new EllipsesIntoClusters.Node());

		for (int i = 0; i < 4; i++) {
			food.add( new EllipsesIntoClusters.Node());
		}

		EllipsesIntoClusters alg = new EllipsesIntoClusters(0.5,0.5,0.5);
		alg.clusters.add(mouth);
		alg.clusters.add(food);

		alg.joinClusters(0,1);

		assertEquals(6,mouth.size());
		assertEquals(0,food.size());

	}

	@Test
	public void axisAdjustedDistance() {

		EllipseRotated_F64 a = new EllipseRotated_F64(2,3,3,3,0);
		EllipseRotated_F64 b = new EllipseRotated_F64(6,3,3,3,0);

		// it's circular so it should be the usual euclidean distance squared
		assertEquals(4*4, EllipsesIntoClusters.axisAdjustedDistanceSq(a,b), 1e-6);
		a.phi = Math.PI/2.0;
		assertEquals(4*4, EllipsesIntoClusters.axisAdjustedDistanceSq(a,b), 1e-6);

		// not a circle any more.  First test it lies along the major axis, should still be euclidean
		a.a=6;a.phi = 0;
		assertEquals(4*4, EllipsesIntoClusters.axisAdjustedDistanceSq(a,b), 1e-6);
		// now rotate it.  Distance should double
		a.phi = Math.PI/2.0;
		assertEquals(8*8, EllipsesIntoClusters.axisAdjustedDistanceSq(a,b), 1e-6);

		// Now do a rigorous test across all angles
		for (int i = 0; i < 60; i++) {
			a.phi = 2.0*Math.PI*i/60;
			double dd = Math.pow(4*Math.cos(a.phi),2) + Math.pow(2*4*Math.sin(a.phi),2);
			assertEquals(dd, EllipsesIntoClusters.axisAdjustedDistanceSq(a,b), 1e-6);
		}
	}

	@Test
	public void removeSingleConnections() {
		List<EllipsesIntoClusters.Node> cluster = new ArrayList<>();

		for (int i = 0; i < 5; i++) {
			EllipsesIntoClusters.Node n = new EllipsesIntoClusters.Node();
			n.which = i;
			cluster.add(n);
		}

		for (int i = 0,j=3; i < 4; j=i,i++) {
			cluster.get(i).connections.add(j);
			cluster.get(j).connections.add(i);
		}

		cluster.get(2).connections.add(4);
		cluster.get(4).connections.add(2);

		EllipsesIntoClusters.removeSingleConnections(cluster);
		assertEquals(4,cluster.size());
		assertEquals(-1,EllipsesIntoClusters.findNode(4,cluster));
	}

	@Test
	public void findNode() {
		List<EllipsesIntoClusters.Node> cluster = new ArrayList<>();

		for (int i = 0; i < 6; i++) {
			EllipsesIntoClusters.Node n = new EllipsesIntoClusters.Node();
			n.which = i;
			cluster.add(n);
		}

		assertEquals(4,EllipsesIntoClusters.findNode(4,cluster));
	}
}
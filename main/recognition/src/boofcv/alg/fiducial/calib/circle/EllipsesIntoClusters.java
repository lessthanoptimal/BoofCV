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

package boofcv.alg.fiducial.calib.circle;

import georegression.struct.shapes.EllipseRotated_F64;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * Given an unordered list of ellipses found in the image connect them into clusters.  A cluster of
 * ellipses will be composed of ellipses which are spatially close to each other and have major
 * axises which are of similar size.
 *
 * @author Peter Abeles
 */
public class EllipsesIntoClusters {

	// ratio between center distance and major axis
	private double maxDistanceToMajorAxisRatio;

	// minimum allowed ratio difference between major and minor axis
	private double sizeSimilarityTolerance = 0.5;

	// minimum number of elements in a cluster
	private int minimumClusterSize = 2;

	private NearestNeighbor<Node> search = FactoryNearestNeighbor.kdtree();
	private FastQueue<double[]> searchPoints;
	private FastQueue<NnData<Node>> searchResults = new FastQueue(NnData.class,true);

	FastQueue<Node> nodes = new FastQueue<>(Node.class,true);
	FastQueue<List<Node>> clusters;

	/**
	 * Configures clustering
	 *
	 * @param maxDistanceToMajorAxisRatio The maxium distance away the center of another ellipse can be relative
	 *                                    to the major axis of the ellipse being examined
	 * @param sizeSimilarityTolerance How similar two ellipses must be to be connected.  0 to 1.0.  1.0 = perfect
	 *                                match and 0.0 = infinite difference in size
	 */
	public EllipsesIntoClusters( double maxDistanceToMajorAxisRatio,
								 double sizeSimilarityTolerance ) {

		this.maxDistanceToMajorAxisRatio = maxDistanceToMajorAxisRatio;
		this.sizeSimilarityTolerance = sizeSimilarityTolerance;

		search.init(2);

		searchPoints = new FastQueue<double[]>(double[].class,true) {
			@Override
			protected double[] createInstance() {
				return new double[2];
			}
		};

		clusters = new FastQueue(List.class,true) {
			@Override
			protected List<Node> createInstance() {
				return new ArrayList<>();
			}
		};
	}

	/**
	 * Processes the ellipses and creates clusters.
	 *
	 * @param ellipses Set of unordered ellipses
	 * @param output Resulting found clusters.  Cleared automatically.  Returned lists are recycled on next call.
	 */
	public void process(List<EllipseRotated_F64> ellipses , List<List<Node>> output ) {

		init(ellipses);

		connect(ellipses);

		output.clear();
		for (int i = 0; i < clusters.size(); i++) {
			List<Node> c = clusters.get(i);
			if( c.size() >= minimumClusterSize) {
				output.add(c);
			}
		}
	}

	/**
	 * Internal function which connects ellipses together
	 */
	void connect(List<EllipseRotated_F64> ellipses) {
		for (int i = 0; i < ellipses.size(); i++) {
			EllipseRotated_F64 e1 = ellipses.get(i);
			Node node1 = nodes.get(i);

			// Only search the maximum of the major axis times two
			// add a fudge factor.  won't ever be perfect
			double maxDistance = e1.a * maxDistanceToMajorAxisRatio;
			maxDistance *= maxDistance;

			searchResults.reset();
			search.findNearest( searchPoints.get(i), maxDistance, Integer.MAX_VALUE, searchResults );

			// if this node already has a cluster look it up, otherwise create a new one
			List<Node> cluster1;

			if( node1.cluster == -1 ) {
				node1.cluster = clusters.size;
				cluster1 = clusters.grow();
				cluster1.clear();
				cluster1.add( node1 );
			} else {
				cluster1 = clusters.get( node1.cluster );
			}

			// only accept ellipses which have a similar size
			for (int j = 0; j < searchResults.size(); j++) {
				NnData<Node> d = searchResults.get(j);
				EllipseRotated_F64 e2 = ellipses.get(d.data.which);
				if( e2 == e1 )
					continue;

				// the initial search was based on size of major axis.  Now prune and take in account the distance
				// from the minor axis
				if( axisAdjustedDistance(e1,e2) > maxDistance )
					continue;

				// smallest shape divided by largest shape
				double ratioA = e1.a > e2.a ? e2.a / e1.a : e1.a / e2.a;
				double ratioB = e1.b > e2.b ? e2.b / e1.b : e1.b / e2.b;

				int indexNode2 = d.data.which;
				Node node2 = nodes.get(indexNode2);

				// connect if they have a similar size to each other
				if( ratioA >= sizeSimilarityTolerance && ratioB >= sizeSimilarityTolerance ) {

					// node2 isn't in a cluster already.  Add it to this one
					if( node2.cluster == -1 ) {
						node2.cluster = node1.cluster;
						cluster1.add( node2 );
						node1.connections.add( indexNode2 );
						node2.connections.add( i );
					} else if( node2.cluster != node1.cluster ) {
						// Node2 is in a different cluster.  Merge the clusters
						joinClusters( node1.cluster , node2.cluster );
						node1.connections.add( indexNode2 );
						node2.connections.add( i );
					} else {
						// see if they are already connected, if not connect them
						if( node1.connections.indexOf(indexNode2) == -1 ) {
							node1.connections.add( indexNode2 );
							node2.connections.add( i );
						}
					}
				}
			}
		}
	}

	/**
	 * Compute a new distance that two ellipses are apart using major/minor axis size.  If the axises are the
	 * same size then there is no change.  If the minor axis is much smaller and ellipse b lies along that
	 * axis then the returned distance will be greater.
	 */
	static double axisAdjustedDistance( EllipseRotated_F64 a , EllipseRotated_F64 b ) {
		double dx = b.center.x - a.center.x;
		double dy = b.center.y - a.center.y;

		double c = Math.cos(a.phi);
		double s = Math.sin(a.phi);

		// rotate into ellipse's coordinate frame
		// scale by ratio of major/minor axis
		double x = (dx*c + dy*s);
		double y = (-dx*s + dy*c)*a.a/a.b;

		return x*x + y*y;
	}

	/**
	 * Recycles and initializes all internal data structures
	 */
	private void init(List<EllipseRotated_F64> ellipses) {
		searchPoints.resize(ellipses.size());
		nodes.resize(ellipses.size());
		clusters.reset();

		for (int i = 0; i < ellipses.size(); i++) {
			EllipseRotated_F64 e = ellipses.get(i);
			double[] p = searchPoints.get(i);
			p[0] = e.center.x;
			p[1] = e.center.y;

			Node n = nodes.get(i);
			n.connections.reset();
			n.which = i;
			n.cluster = -1;
		}

		search.setPoints(searchPoints.toList(),nodes.toList());
	}

	/**
	 * Moves all the members of 'food' into 'mouth'
	 * @param mouth The group which will not be changed.
	 * @param food All members of this group are put into mouth
	 */
	void joinClusters( int mouth , int food ) {

		List<Node> listMouth = clusters.get(mouth);
		List<Node> listFood = clusters.get(food);

		// put all members of food into mouth
		for (int i = 0; i < listFood.size(); i++) {
			listMouth.add( listFood.get(i) );
			listFood.get(i).cluster = mouth;
		}

		// zero food members
		listFood.clear();
	}

	public double getMaxDistanceToMajorAxisRatio() {
		return maxDistanceToMajorAxisRatio;
	}

	public void setMaxDistanceToMajorAxisRatio(double maxDistanceToMajorAxisRatio) {
		this.maxDistanceToMajorAxisRatio = maxDistanceToMajorAxisRatio;
	}

	public double getSizeSimilarityTolerance() {
		return sizeSimilarityTolerance;
	}

	public void setSizeSimilarityTolerance(double sizeSimilarityTolerance) {
		this.sizeSimilarityTolerance = sizeSimilarityTolerance;
	}

	public int getMinimumClusterSize() {
		return minimumClusterSize;
	}

	public void setMinimumClusterSize(int minimumClusterSize) {
		this.minimumClusterSize = minimumClusterSize;
	}

	public static class Node
	{
		/**
		 * index of the ellipse in the input list
		 */
		public int which;
		/**
		 * ID number of the cluster
		 */
		public int cluster;
		/**
		 * Index of all the ellipses which it is connected to.  Both node should be
		 * connected to each other
		 */
		public GrowQueue_I32 connections = new GrowQueue_I32();
	}
}

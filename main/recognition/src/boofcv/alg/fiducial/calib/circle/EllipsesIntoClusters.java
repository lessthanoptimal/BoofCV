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

	// maximum number of neighbors it will consider
	private int neighborsConsidered = 10;
	// ratio between center distance and major axis
	private double distanceToMajorAxisRatio;

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
	 * @param distanceToMajorAxisRatio Maximum expected space between ellipse centers in 2D plane divided by
	 *                             the ellipses' major axis.   E.g. 3.0 = space between ellipse centers is
	 *                             3x their major axis
	 * @param sizeSimilarityTolerance How similar two ellipses must be to be connected.  0 to 1.0.  1.0 = perfect
	 *                                match and 0.0 = infinite difference in size
	 */
	public EllipsesIntoClusters( double distanceToMajorAxisRatio,
								 double sizeSimilarityTolerance ) {

		this.distanceToMajorAxisRatio = distanceToMajorAxisRatio;
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
			double maxDistance = e1.a * distanceToMajorAxisRatio;
			maxDistance *= maxDistance;

			searchResults.reset();
			// + 1 on max neighbors because this ellipse is returned
			search.findNearest( searchPoints.get(i), maxDistance, neighborsConsidered+1, searchResults );

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

				// smallest shape divided by largest shape
				double ratio = e1.a > e2.a ? e2.a / e1.a : e1.a / e2.a;

				int indexNode2 = d.data.which;
				Node node2 = nodes.get(indexNode2);

				// connect if they have a similar size to each other
				if( ratio >= sizeSimilarityTolerance ) {

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

	public int getNeighborsConsidered() {
		return neighborsConsidered;
	}

	public void setNeighborsConsidered(int neighborsConsidered) {
		this.neighborsConsidered = neighborsConsidered;
	}

	public double getDistanceToMajorAxisRatio() {
		return distanceToMajorAxisRatio;
	}

	public void setDistanceToMajorAxisRatio(double distanceToMajorAxisRatio) {
		this.distanceToMajorAxisRatio = distanceToMajorAxisRatio;
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

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
import georegression.struct.curve.EllipseRotated_F64;
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
	private double sizeSimilarityTolerance;
	// how similar the ratio of major/minor axises need to be to each other to connect
	private double ratioSimilarityTolerance;
	// how similar edge intensity needs to be
	private double edgeIntensitySimilarityTolerance;

	// minimum number of elements in a cluster
	private int minimumClusterSize = 3;

	private NearestNeighbor<Node> search = FactoryNearestNeighbor.kdtree();
	private FastQueue<double[]> searchPoints;
	private FastQueue<NnData<Node>> searchResults = new FastQueue(NnData.class,true);

	FastQueue<Node> nodes = new FastQueue<>(Node.class,true);
	FastQueue<List<Node>> clusters;

	/**
	 * Configures clustering
	 *
	 * @param maxDistanceToMajorAxisRatio The maximum distance away the center of another ellipse that will be
	 *                                   considered specifies as a multiple of the ellipse's major axis
	 * @param sizeSimilarityTolerance How similar two ellipses must be to be connected.  0 to 1.0.  1.0 = perfect
	 *                                match and 0.0 = infinite difference in size
	 * @param edgeIntensitySimilarityTolerance How similar the intensity of the ellipses edges need to be.
	 *                                         0 to 1.0.  1.0 = perfect
	 */
	public EllipsesIntoClusters( double maxDistanceToMajorAxisRatio,
								 double sizeSimilarityTolerance ,
								 double edgeIntensitySimilarityTolerance ) {

		this.maxDistanceToMajorAxisRatio = maxDistanceToMajorAxisRatio;
		this.sizeSimilarityTolerance = sizeSimilarityTolerance;
		this.ratioSimilarityTolerance = sizeSimilarityTolerance;
		this.edgeIntensitySimilarityTolerance = edgeIntensitySimilarityTolerance;

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
	public void process(List<EllipseInfo> ellipses , List<List<Node>> output ) {

		init(ellipses);

		connect(ellipses);

		output.clear();
		for (int i = 0; i < clusters.size(); i++) {
			List<Node> c = clusters.get(i);

			// remove noise
			removeSingleConnections(c);

			if( c.size() >= minimumClusterSize) {
				output.add(c);
			}
		}
	}

	/**
	 * Internal function which connects ellipses together
	 */
	void connect(List<EllipseInfo> ellipses) {
		for (int i = 0; i < ellipses.size(); i++) {
			EllipseInfo info1 = ellipses.get(i);
			EllipseRotated_F64 e1 = info1.ellipse;
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

			double edge1 = info1.averageOutside-info1.averageInside;

			// only accept ellipses which have a similar size
			for (int j = 0; j < searchResults.size(); j++) {
				NnData<Node> d = searchResults.get(j);
				EllipseInfo info2 = ellipses.get(d.data.which);
				EllipseRotated_F64 e2 = info2.ellipse;
				if( e2 == e1 )
					continue;

				// see of they are already connected
				if( node1.connections.indexOf(d.data.which) != -1 ) {
					continue;
				}

				// test the appearance of the ellipses edge
				double edge2 = info2.averageOutside-info2.averageInside;
				double intensityRatio = Math.abs(edge1-edge2)/Math.max(edge1,edge2);

				if( intensityRatio > edgeIntensitySimilarityTolerance)
					continue;

				// the initial search was based on size of major axis.  Now prune and take in account the distance
				// from the minor axis
				if( axisAdjustedDistanceSq(e1,e2) > maxDistance ) {
					continue;
				}

				// TODO take in in account how similar their orientation is, but less important when they are circular
				//      somehow work into aspect ratio test?

				// smallest shape divided by largest shape
				double ratioA = e1.a > e2.a ? e2.a / e1.a : e1.a / e2.a;
				double ratioB = e1.b > e2.b ? e2.b / e1.b : e1.b / e2.b;

				if( ratioA < sizeSimilarityTolerance && ratioB < sizeSimilarityTolerance ) {
					continue;
				}

				// axis ratio similarity check
				double ratioC = (e1.a*e2.b)/(e1.b*e2.a);
				if( ratioC > 1 ) ratioC = 1.0/ratioC;

				if( ratioC < ratioSimilarityTolerance ) {
					continue;
				}

				// Apply rule which combines two features
				if( intensityRatio + (1-ratioC) >
						(edgeIntensitySimilarityTolerance/1.5+(1-ratioSimilarityTolerance)) )
					continue;

				int indexNode2 = d.data.which;
				Node node2 = nodes.get(indexNode2);

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
					node1.connections.add( indexNode2 );
					node2.connections.add( i );
				}
			}
		}
	}

	/**
	 * Removes stray connections that are highly likely to be noise. If a node has one connection it then removed.
	 * Then it's only connection is considered for removal.
	 */
	static void removeSingleConnections( List<Node> cluster ) {
		List<Node> open = new ArrayList<>();
		List<Node> future = new ArrayList<>();

		open.addAll(cluster);

		while( !open.isEmpty() ) {
			for (int i = open.size()-1; i >= 0; i--) {
				Node n = open.get(i);

				if( n.connections.size == 1 ) {
					// clear it's connections and remove it from the cluster
					int index = findNode(n.which,cluster);
					cluster.remove(index);

					// Remove the reference to this node from its one connection
					int parent = findNode(n.connections.get(0),cluster);
					n.connections.reset();
					if( parent == -1 ) throw new RuntimeException("BUG!");
					Node p = cluster.get(parent);
					int edge = p.connections.indexOf(n.which);
					if( edge == -1 ) throw new RuntimeException("BUG!");
					p.connections.remove(edge);

					// if the parent now only has one connection
					if( p.connections.size == 1) {
						future.add(p);
					}
				}
			}
			open.clear();
			List<Node> tmp = open;
			open = future;
			future = tmp;
		}
	}

	static int findNode( int target , List<Node> cluster ) {
		for (int i = 0; i < cluster.size(); i++) {
			if( cluster.get(i).which == target ) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Compute a new distance that two ellipses are apart using major/minor axis size.  If the axises are the
	 * same size then there is no change.  If the minor axis is much smaller and ellipse b lies along that
	 * axis then the returned distance will be greater.
	 */
	static double axisAdjustedDistanceSq(EllipseRotated_F64 a , EllipseRotated_F64 b ) {
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
	void init(List<EllipseInfo> ellipses) {
		searchPoints.resize(ellipses.size());
		nodes.resize(ellipses.size());
		clusters.reset();

		for (int i = 0; i < ellipses.size(); i++) {
			EllipseRotated_F64 e = ellipses.get(i).ellipse;
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

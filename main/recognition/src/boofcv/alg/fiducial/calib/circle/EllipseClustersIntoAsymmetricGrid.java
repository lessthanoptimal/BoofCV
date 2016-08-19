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

import boofcv.alg.fiducial.calib.circle.EllipsesIntoClusters.Node;
import georegression.metric.UtilAngle;
import georegression.struct.shapes.EllipseRotated_F64;
import org.ddogleg.sorting.QuickSortComparator;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class EllipseClustersIntoAsymmetricGrid {

	private FastQueue<Grid> grids = new FastQueue<Grid>(Grid.class,true);

	public static double CONTOUR_ANGLE_MIN = Math.PI*0.8;


	// Information on each ellipse/node in a cluster
	private FastQueue<NodeInfo> listInfo = new FastQueue<NodeInfo>(NodeInfo.class,true);
	// Used to sort edges in a node.  used instead of built in sorting algorithm to maximize memory being recycled
	private QuickSortComparator<Edge> sorter;

	// All ellipses in the contour around the grid
	private FastQueue<NodeInfo> contour = new FastQueue<NodeInfo>(NodeInfo.class,false);

	public EllipseClustersIntoAsymmetricGrid() {

		sorter = new QuickSortComparator<Edge>(new Comparator<Edge>(){

			@Override
			public int compare(Edge o1, Edge o2) {
				if( o1.angle < o2.angle )
					return -1;
				else if( o1.angle > o2.angle )
					return 1;
				else
					return 0;
			}
		});
	}

	public void process(List<EllipseRotated_F64> ellipses , List<List<Node>> clusters ) {

		for (int i = 0; i < clusters.size(); i++) {
			computeClusterInfo(ellipses, clusters.get(i));

			if( !findContour() )
				continue;

			// Find corner to start alignment
			NodeInfo corner = selectSeedCorner();

			// TODO find row, then column
		}
	}

	void computeClusterInfo(List<EllipseRotated_F64> ellipses , List<Node> cluster ) {

		// create an info object for each member inside of the cluster
		listInfo.reset();
		for (int i = 0; i < cluster.size(); i++) {
			Node n = cluster.get(i);
			EllipseRotated_F64 t = ellipses.get( n.which );

			NodeInfo info = listInfo.grow();
			info.reset();
			info.ellipse = t;
		}


		addEdgesToInfo(cluster);
		findLargestAngle();
	}

	/**
	 * Adds edges to node info and computes their orientation
	 */
	private void addEdgesToInfo(List<Node> cluster) {
		for (int i = 0; i < cluster.size(); i++) {
			Node n = cluster.get(i);
			NodeInfo infoA = listInfo.get(i);
			EllipseRotated_F64 a = infoA.ellipse;

			// create the edges and order them based on their direction
			for (int j = 0; j < n.connections.size(); j++) {
				NodeInfo infoB = listInfo.get( indexOf(cluster, n.connections.get(j)));
				EllipseRotated_F64 b = infoB.ellipse;

				Edge edge = infoA.edges.grow();

				edge.target = infoB;
				edge.angle = Math.atan2( b.center.y - a.center.y , b.center.x - a.center.x );
			}

			sorter.sort(infoA.edges.data, infoA.edges.size);
		}
	}

	/**
	 * Finds the two edges with the greatest angular distance between them.
	 */
	private void findLargestAngle() {
		for (int i = 0; i < listInfo.size(); i++) {
			NodeInfo info = listInfo.get(i);

			if( info.edges.size < 2 )
				continue;

			for (int j = 0, k = info.edges.size-1; j < info.edges.size; k=j,j++) {
				double angleA = info.edges.get(j).angle;
				double angleB = info.edges.get(k).angle;

				double distance = UtilAngle.distanceCCW(angleA,angleB);

				if( distance > info.angleBetween ) {
					info.angleBetween = distance;
					info.left = info.edges.get(j).target;
					info.right = info.edges.get(k).target;
				}
			}
		}
	}

	private boolean findContour() {
		contour.reset();

		// mark nodes as being part of the contour
		for (int i = 0; i < listInfo.size(); i++) {
			NodeInfo info = listInfo.get(i);

			if( info.angleBetween >= CONTOUR_ANGLE_MIN ) {
				info.contour = true;
				contour.add( info );
			}
		}

		if( contour.size < 4 )
			return false;

		// Quick sanity check that makes sure all nodes on the contour link to contours
		for (int i = 0; i < contour.size(); i++) {
			NodeInfo info = contour.get(i);
			if( !info.left.contour || !info.right.contour )
				return false;
		}

		int total = contour.size;
		NodeInfo current = contour.get(0);
		NodeInfo start = current;
		contour.reset();

		do {
			// there must be a cycle some place
			if( contour.size >= total )
				return false;
			contour.add( current );
			current = current.right;
		} while( start != current );

		return contour.size == total;
	}

	/**
	 * Finds the node with the index of 'value'
	 */
	public static int indexOf(List<Node> list , int value ) {

		for (int i = 0; i < list.size(); i++) {
			if( list.get(i).which == value )
				return i;
		}

		return -1;
	}

	/**
	 * Selects the node on the contour which is closest to 90 degrees and is thus likely to be
	 * a node on the corner
	 */
	private NodeInfo selectSeedCorner() {
		NodeInfo best = null;
		double bestError = Double.MAX_VALUE;

		for (int i = 0; i < contour.size; i++) {
			NodeInfo info = contour.get(i);

			double error = UtilAngle.dist(Math.PI/2.0,info.angleBetween);

			if( error < bestError ) {
				bestError = error;
				best = info;
			}
		}

		return best;
	}


	public static class NodeInfo {
		EllipseRotated_F64 ellipse;

		FastQueue<Edge> edges = new FastQueue<Edge>(Edge.class,true);

		boolean contour;
		NodeInfo left,right;
		double angleBetween;

		public void reset() {
			contour = false;
			ellipse = null;
			left = right = null;
			angleBetween = 0;
			edges.reset();
		}
	}

	public static class Edge {
		NodeInfo target;
		double angle;
	}

	public static class Grid
	{
		public List<EllipseRotated_F64> ellipses = new ArrayList<EllipseRotated_F64>();
		public int rows;
		public int columns;
	}

}

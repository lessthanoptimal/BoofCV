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

import boofcv.alg.fiducial.calib.circle.EllipsesIntoClusters.Node;
import georegression.metric.UtilAngle;
import georegression.struct.shapes.EllipseRotated_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Given a cluster of ellipses (created with {@link EllipsesIntoClusters}) order the ellipses into
 * a hexagonal grid pattern. In a hexagonal grid the center of each circle is the same distance from
 * its neighbors. Smallest grid size it will detect is 3 x 3.</p>
 *
 *
 * <p>Note that the returned grid is 'sparse'.  every other node is skipped implicitly.
 * This is caused by the asymmetry.  Each row is offset by one circle/grid element.</p>
 *
 * <pre>Examples:
 * 3x6 grid will have 9 elements total.
 * grid(0,0) = [0]
 * grid(0,2) = [1]
 * grid(0,4) = [2]
 * grid(1,1) = [3]
 * grid(1,3) = [4]
 * grid(1,5) = [5]
 * </pre>
 *
 * <p>IMPORTANT: To properly construct the contour the clusters need to connect and jump over the "zig-zags". Thus
 * at a minimum search radius should be distance between centers*2 if not more.
 * </p>
 *
 * <p>See {@link Grid} for a description of how the output grids are described.  It uses a sparse format.</p>
 * <p>See {@link DetectCircleAsymmetricGrid} for an example of an asymmetric grid</p>
 *
 * @author Peter Abeles
 */
public class EllipseClustersIntoHexagonalGrid extends EllipseClustersIntoGrid {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(List<EllipseRotated_F64> ellipses , List<List<Node>> clusters ) {

		foundGrids.reset();
		if( clusters.size() == 0 )
			return;

		verbose = true;

		for (int i = 0; i < clusters.size(); i++) {
			List<Node> cluster = clusters.get(i);
			int clusterSize = cluster.size();
			if( clusterSize < 5 ) // 3 x 3 grid has 5 elements
				continue;

			computeNodeInfo(ellipses, cluster);

			// finds all the nodes in the outside of the cluster
			if( !findContour(true) ) {
				if( verbose ) System.out.println("Contour find failed");
				continue;
			}

			// Find corner to start alignment
			NodeInfo corner = selectSeedCorner();
			if( corner == null ) {
				if( verbose ) System.out.println("No corner found!");
				continue;
			}

			System.out.println("corner "+corner.ellipse.center);
			System.out.println("left   "+corner.left.ellipse.center);
			System.out.println("right  "+corner.right.ellipse.center);

			List<List<NodeInfo>> grid = new ArrayList<>();

			// traverse along the axis with closely spaced circles
			double distLeft = corner.distance(corner.left);
			double distRight = corner.distance(corner.right);
			NodeInfo next = distLeft < distRight ? corner.left : corner.right;
			next.marked = true;
			NodeInfo other = distLeft < distRight ? corner.right : corner.left;

			boolean ccw = UtilAngle.distanceCCW(direction(corner,other),direction(corner,next)) > Math.PI;

			boolean even = true;
			boolean error = false;
			while( true ) {
				List<NodeInfo> column = findLine(corner,next,clusterSize,null,ccw);
				System.out.println("column "+column.size());
				grid.add(column);

				if( grid.size() == 2 ) {
					if( Math.abs(column.size() - grid.get(0).size()) > 1 ) {
						if( verbose ) System.out.println("Unexpected line length, first offset line.");
						error = true;break;
					}
				} else if( grid.size() > 1 ){
					if( column.size() != grid.get( grid.size()-3).size() ) {
						if( verbose )
							System.out.println("Unexpected line length compared to previous.");
						error = true;break;
					}
				}

				if( even ) {
					corner = selectClosestN(corner,next);
					if( corner == null ) break;
					next = selectClosestN(corner,next);
					if( next == null ) break;
				} else {
					next = selectClosestN(corner,next);
					if( next == null ) break;
					corner = selectClosestN(corner,next);
					if( corner == null ) break;
				}
				corner.marked = true;
				next.marked = true;
				even = !even;
			}

			if( !error ) {
				if( grid.size() < 3)
					continue;

				if (checkDuplicates(grid)) {
					if (verbose) System.out.println("contains duplicates");
					continue;
				}

				saveResults(grid);
			}
		}
	}

	private NodeInfo smallestSweep( NodeInfo a , NodeInfo b , boolean ccw ) {

		double bestSweep = Math.PI;
		NodeInfo best = null;

		double reference = b.findEdge(a).angle;

		for (int i = 0; i < b.edges.size; i++) {
			Edge e = b.edges.get(i);
			if( e.target.marked )
				continue;
			double sweep = ccw ? UtilAngle.distanceCCW(reference,e.angle) : UtilAngle.distanceCW(reference,e.angle);

			if( sweep < bestSweep ) {
				bestSweep = sweep;
				best = e.target;
			}
		}
		return best;
	}

	/**
	 * Pick a corner but avoid the pointy edges at the other end
	 */
	NodeInfo selectSeedCorner() {
		NodeInfo best = null;
		double bestScore = 0;
		double minAngle = Math.PI+0.1;

		for (int i = 0; i < contour.size; i++) {
			NodeInfo info = contour.get(i);

			if( info.angleBetween < minAngle )
				continue;

			Edge middleR = selectClosest(info.right,info);
			if( middleR == null )
				continue;
			Edge middleL = selectClosest(info.left,info);
			if( middleL == null )
				continue;

			if( middleL.target != middleR.target )
				continue;

			// With no perspective distortion, at the correct corners difference should be zero
			// while the bad ones will be around 60 degrees
			double r = UtilAngle.bound( middleR.angle + Math.PI);
			double difference = UtilAngle.dist(r,middleL.angle);

			double score = info.angleBetween - difference;
			if( score > bestScore ) {
				best = info;
				bestScore = score;
			}
		}

		if( best != null ) {
			best.marked = true;
		}
		return best;
	}

	private boolean addRemainingColumns(List<NodeInfo> column1, List<NodeInfo> column0, List<List<NodeInfo>> grid) {

		boolean failed = false;
		// add rest of the columns now
		while( true ) {
			NodeInfo a;
			if( grid.size()%2 == 0 )
				a = selectClosestSide(column1.get(0),column0.get(0));
			else
				a = selectClosest(column1.get(1),column1.get(0)).target;
			if( a == null )
				break;
			a.marked = true;
			column0 = column1;
			column1 = new ArrayList<>();
			column1.add(a);
			for (int j = grid.size()%2; j < column0.size(); j++) {
				NodeInfo b = column0.get(j);
				a = selectClosest(b,a).target;
				if( a != null ) {
					a.marked = true;
					column1.add(a);
				} else {
					break;
				}
			}
			if( grid.get( (grid.size())%2).size() != column1.size() ) {
				if( verbose ) System.out.println("Failed: unexpected column size");
				failed = true;
				break;
			} else {
				grid.add(column1);
			}
		}
		return failed;
	}

	/**
	 * Traverses along the first two columns and sets them up
	 */
	static void bottomTwoColumns(NodeInfo corner, List<NodeInfo> column0, List<NodeInfo> column1) {
		column0.add(corner);
		column0.add(corner.right);

		NodeInfo a = selectClosest(corner.right,corner).target;
		if( a == null ) {
			return;
		}
		a.marked = true;
		column1.add(a);
		NodeInfo b = corner.right;

		corner.marked = true;
		corner.right.marked = true;

		while( true ) {
			NodeInfo t = selectClosest(b,a).target;
			if( t == null ) break;
			t.marked = true;
			column1.add(t);

			a = t;
			t = selectClosest(b,a).target;
			if( t == null ) break;
			t.marked = true;
			column0.add(t);
			b = t;
		}
	}

	/**
	 * Finds the closest that is the same distance from the two nodes and part of an approximate equilateral triangle
	 */
	static Edge selectClosest( NodeInfo a , NodeInfo b ) {

		NodeInfo best = null;
		double bestDistance = Double.MAX_VALUE;
		Edge bestEdgeA = null;
		Edge bestEdgeB = null;

		for (int i = 0; i < a.edges.size; i++) {
			NodeInfo aa = a.edges.get(i).target;
			if( aa.marked ) continue;

			for (int j = 0; j < b.edges.size; j++) {
				NodeInfo bb = b.edges.get(j).target;
				if( bb.marked ) continue;

				if( aa == bb ) {
					double da = EllipsesIntoClusters.axisAdjustedDistanceSq(a.ellipse,aa.ellipse);
					double db = EllipsesIntoClusters.axisAdjustedDistanceSq(b.ellipse,aa.ellipse);

					da = Math.sqrt(da);
					db = Math.sqrt(db);

					// see if they are approximately the same distance
					double diffRatio = Math.abs(da-db)/Math.max(da,db);
					if( diffRatio > 0.25 )
						continue;

					// TODO reject if too far
					double d = (da+db);

					if( d < bestDistance ) {
						bestDistance = d;
						best = aa;
						bestEdgeA = a.edges.get(i);
						bestEdgeB = b.edges.get(j);
					}
					break;
				}
			}
		}

		// check the angles
//		if( best != null ) {
//			double angleA = UtilAngle.(bestEdgeA.angle,bestEdgeB.angle);
//
//			if( angleA < Math.PI*0.5 ) // expected with zero distortion is 60 degrees
//				return bestEdgeA;
//			else
//				return null;
//		}

		return bestEdgeA;
	}

	static NodeInfo selectClosestN( NodeInfo a , NodeInfo b ) {
		Edge e = selectClosest(a,b);
		if( e == null )
			return null;
		else
			return e.target;
	}

	/**
	 * Selects the closest node with the assumption that it's along the side of the grid.
	 */
	static NodeInfo selectClosestSide( NodeInfo a , NodeInfo b ) {

		double ratio = 1.7321;

		NodeInfo best = null;
		double bestDistance = Double.MAX_VALUE;
		Edge bestEdgeA = null;
		Edge bestEdgeB = null;

		for (int i = 0; i < a.edges.size; i++) {
			NodeInfo aa = a.edges.get(i).target;
			if( aa.marked ) continue;

			for (int j = 0; j < b.edges.size; j++) {
				NodeInfo bb = b.edges.get(j).target;
				if( bb.marked ) continue;

				if( aa == bb ) {
					double da = EllipsesIntoClusters.axisAdjustedDistanceSq(a.ellipse,aa.ellipse);
					double db = EllipsesIntoClusters.axisAdjustedDistanceSq(b.ellipse,aa.ellipse);

					da = Math.sqrt(da);
					db = Math.sqrt(db);

					double max,min;
					if( da>db) {
						max = da;min = db;
					} else {
						max = db;min = da;
					}

					// see how much it deviates from the ideal length with no distortion
					double diffRatio = Math.abs(max-min*ratio)/max;
					if( diffRatio > 0.25 )
						continue;

					// TODO reject if too far
					double d = da+db;

					if( d < bestDistance ) {
						bestDistance = d;
						best = aa;
						bestEdgeA = a.edges.get(i);
						bestEdgeB = b.edges.get(j);
					}
					break;
				}
			}
		}

		// check the angles
		if( best != null ) {
			double angleA = UtilAngle.distanceCW(bestEdgeA.angle,bestEdgeB.angle);

			if( angleA < Math.PI*0.25 ) // expected with zero distortion is 30 degrees
				return best;
			else
				return null;
		}

		return null;
	}

	/**
	 * Combines the inner and outer grid into one grid for output.  See {@link Grid} for a discussion
	 * on how elements are ordered internally.
	 */
	void saveResults( List<List<NodeInfo>> graph ) {
		Grid g = foundGrids.grow();
		g.reset();

		g.columns = graph.get(0).size() + graph.get(1).size();
		g.rows = graph.size();

		for (int row = 0; row < g.rows; row++) {
			List<NodeInfo> list = graph.get(row);
			for (int i = 0; i < g.columns; i++) {
				if( (i%2) == (row%2))
					g.ellipses.add(list.get(i/2).ellipse );
				else
					g.ellipses.add(null);
			}
		}
	}
}

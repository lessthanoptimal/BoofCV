/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.shapes.polyline;

import boofcv.misc.CircularIndex;
import georegression.metric.Distance2D_F64;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * <p>
 * Prunes corners from a pixel level accuracy contour by minizing a penalized energy function.  The energy of a line
 * segment is defined as Euclidean distance squared of each point from the line summed plus a penalty divided by the
 * distance between two end points of the line.  When a corner is removed the location of its neighbors are optimized
 * again.
 * </p>
 * <p>
 * Energy of line segment = [Sum( distance points from line squared) + penalty ] / (segment length)
 * </p>
 *
 * @author Peter Abeles
 */
public class MinimizeEnergyPrune {

	// how much a corner adds to the energy calculation
	double splitPenalty;

	LineParametric2D_F64 line = new LineParametric2D_F64();
	Point2D_F64 point = new Point2D_F64();


	List<Point2D_I32> contour;
	double energySegment[] = new double[1];

	GrowQueue_I32 bestCorners = new GrowQueue_I32();
	GrowQueue_I32 workCorners1 = new GrowQueue_I32();
	GrowQueue_I32 workCorners2 = new GrowQueue_I32();

	public MinimizeEnergyPrune(double splitPenalty) {
		this.splitPenalty = splitPenalty;
	}

	/**
	 * Given a contour and initial set of corners compute a new set of corner indexes
	 * @param contour List of points in the shape's contour
	 * @param input Initial set of corners
	 * @param output Pruned set of corners
	 * @return true if one or more corners were pruned, false if nothing changed
	 */
	public boolean prune(List<Point2D_I32> contour, GrowQueue_I32 input, GrowQueue_I32 output) {

		this.contour = contour;
		output.setTo(input);
		removeDuplicates(output);

		// can't prune a corner and it will still be a polygon
		if( output.size() <= 3 )
			return false;

		computeSegmentEnergy(output);

		double total = 0;
		for (int i = 0; i < output.size(); i++) {
			total += energySegment[i];
		}

		FitLinesToContour fit = new FitLinesToContour();
		fit.setContour(contour);

		boolean modified = false;
		while( output.size() > 3 ) {
			double bestEnergy = total;
			boolean betterFound = false;
			bestCorners.reset();

			for (int i = 0; i < output.size(); i++) {
				// add all but the one which was removed
				workCorners1.reset();
				for (int j = 0; j < output.size(); j++) {
					if( i != j ) {
						workCorners1.add(output.get(j));
					}
				}

				// just in case it created a duplicate
				removeDuplicates(workCorners1);
				if( workCorners1.size() > 3 ) {

					// when looking at these anchors remember that they are relative to the new list without
					// the removed corner and that the two adjacent corners need to be optimized
					int anchor0 = CircularIndex.addOffset(i, -2, workCorners1.size());
					int anchor1 = CircularIndex.addOffset(i, 1, workCorners1.size());

					// optimize the two adjacent corners to the removed one
					if (fit.fitAnchored(anchor0, anchor1, workCorners1, workCorners2)) {

						// TODO this isn't taking advantage of previously computed line segment energy is it?
						//      maybe a small speed up can be had by doing that
						double score = 0;
						for (int j = 0, k = workCorners2.size() - 1; j < workCorners2.size(); k = j, j++) {
							score += computeSegmentEnergy(workCorners2, k, j);
						}

						if (score < bestEnergy) {
							betterFound = true;
							bestEnergy = score;
							bestCorners.reset();
							bestCorners.addAll(workCorners2);
						}
					}
				}
			}

			if ( betterFound ) {
				modified = true;
				total = bestEnergy;
				output.setTo(bestCorners);
			} else {
				break;
			}
		}

		return modified;
	}

	/**
	 * Look for two corners which point to the same point and removes one of them from the corner list
	 */
	void removeDuplicates( GrowQueue_I32 corners ) {
		// remove duplicates
		for (int i = 0; i < corners.size(); i++) {
			Point2D_I32 a = contour.get(corners.get(i));

			// start from the top so that removing a corner doesn't mess with the for loop
			for (int j = corners.size()-1; j > i; j--) {
				Point2D_I32 b = contour.get(corners.get(j));

				if( a.x == b.x && a.y == b.y ) {
					// this is still ok if j == 0 because it wrapped around.  'i' will now be > size
					corners.remove(j);
				}
			}
		}
	}

	/**
	 * Computes the energy of each segment individually
	 */
	void computeSegmentEnergy( GrowQueue_I32 corners ) {
		if( energySegment.length < corners.size() ) {
			energySegment = new double[ corners.size() ];
		}

		for (int i = 0,j=corners.size()-1; i < corners.size(); j=i,i++) {
			energySegment[j] = computeSegmentEnergy(corners, j, i);
		}
	}

	/**
	 * Returns the total energy after removing a corner
	 * @param removed index of the corner that is being removed
	 * @param corners list of corner indexes
	 */
	protected double energyRemoveCorner( int removed , GrowQueue_I32 corners ) {
		double total = 0;

		int cornerA = CircularIndex.addOffset(removed, -1 , corners.size());
		int cornerB = CircularIndex.addOffset(removed,  1 , corners.size());

		total += computeSegmentEnergy(corners, cornerA, cornerB);

		if( cornerA > cornerB ) {
			for (int i = cornerB; i < cornerA; i++)
				total += energySegment[i];
		} else {
			for (int i = 0; i < cornerA; i++) {
				total += energySegment[i];
			}
			for (int i = cornerB; i < corners.size(); i++) {
				total += energySegment[i];
			}
		}

		return total;
	}

	/**
	 * Computes the energy for a segment defined by the two corner indexes
	 */
	protected double computeSegmentEnergy(GrowQueue_I32 corners, int cornerA, int cornerB) {
		int indexA = corners.get(cornerA);
		int indexB = corners.get(cornerB);

		if( indexA == indexB ) {
			return 100000.0;
		}

		Point2D_I32 a = contour.get(indexA);
		Point2D_I32 b = contour.get(indexB);

		line.p.x = a.x;
		line.p.y = a.y;
		line.slope.set(b.x-a.x,b.y-a.y);

		double total = 0;
		int length = circularDistance(indexA,indexB);

		for (int k = 1; k < length; k++) {
			Point2D_I32 c = getContour(indexA + 1 + k);
			point.set(c.x, c.y);

			total += Distance2D_F64.distanceSq(line, point);
		}

		return (total+ splitPenalty)/a.distance2(b);
	}

	protected Point2D_I32 getContour(int index) {
		return contour.get(index % contour.size());
	}

	/**
	 * Distance the two points are apart in clockwise direction
	 */
	protected int circularDistance( int start , int end ) {
		return CircularIndex.distanceP(start,end,contour.size());
	}
}

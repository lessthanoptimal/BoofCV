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
 * @author Peter Abeles
 */
// TODO make looping optional later
public class MinimizeEnergyPrune {

	double splitPenalty = 4;

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
	 *
	 * @param contour
	 * @param input
	 * @param output
	 * @return true if one or more corners were pruned, false if nothing changed
	 */
	public boolean fit( List<Point2D_I32> contour , GrowQueue_I32 input , GrowQueue_I32 output ) {
		this.contour = contour;

		output.reset();
		output.addAll(input);

		computeSegmentEnergy(output);

		double total = 0;
		for (int i = 0; i < output.size(); i++) {
			total += energySegment[i];
		}

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

				optimizeCorners(workCorners1,workCorners2);

				double score = 0;
				for (int j = 0, k = workCorners2.size()-1; j < workCorners2.size(); k=j,j++) {
					score += computeSegmentEnergy(workCorners2,k,j);
				}


				if (score < bestEnergy) {
					betterFound = true;
					bestEnergy = score;
					bestCorners.reset();
					bestCorners.addAll(workCorners2);
				}
			}

			if ( betterFound ) {
				modified = true;
				total = bestEnergy;
				output.reset();
				output.addAll(bestCorners);
			} else {
				break;
			}
		}

		return modified;
	}

	void optimizeCorners( GrowQueue_I32 input , GrowQueue_I32 output ) {

		output.reset();
		for( int target = 0; target < input.size(); target++ )
		{
			int indexA = input.get(CircularIndex.minusPOffset(target, 1, input.size()));
			int indexB = input.get(CircularIndex.plusPOffset(target, 1, input.size()));

			Point2D_I32 a = contour.get(indexA);
			Point2D_I32 b = contour.get(indexB);

			line.p.x = a.x;
			line.p.y = a.y;
			line.slope.set(b.x - a.x, b.y - a.y);

			int length = CircularIndex.distanceP(indexA, indexB, contour.size());

			double best = 0;
			int bestIndex = -1;
			for (int i = 1; i < length - 1; i++) {
				Point2D_I32 c = getContour(indexA + i);
				point.set(c.x, c.y);

				double d = Distance2D_F64.distanceSq(line, point);
				if (d > best) {
					best = d;
					bestIndex = i;
				}
			}
			int selectedCorner = CircularIndex.addOffset(indexA, bestIndex, contour.size());

			boolean duplicate = false;
			for (int i = 0; i < output.size(); i++) {
				if( output.get(i) == selectedCorner ) {
					duplicate = true;
					break;
				}
			}
			if( !duplicate )
				output.add(selectedCorner);
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

	protected double computeSegmentEnergy(GrowQueue_I32 corners, int cornerA, int cornerB) {
		int indexA = corners.get(cornerA);
		int indexB = corners.get(cornerB);

		Point2D_I32 a = contour.get(indexA);
		Point2D_I32 b = contour.get(indexB);

		line.p.x = a.x;
		line.p.y = a.y;
		line.slope.set(b.x-a.x,b.y-a.y);

		double total = 0;
		int length = circularDistance(indexA,indexB);
		for (int k = 1; k < length; k++) {
			Point2D_I32 c = getContour(indexA + k);
			point.set(c.x,c.y);

			total += Distance2D_F64.distanceSq(line, point);
		}

		if( indexA == indexB ) {
			System.out.println();
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

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

	double splitPentially = 1;

	LineParametric2D_F64 line = new LineParametric2D_F64();
	Point2D_F64 point = new Point2D_F64();


	List<Point2D_I32> contour;
	double energySegment[] = new double[1];

	public void fit( List<Point2D_I32> contour , GrowQueue_I32 input , GrowQueue_I32 output ) {
		if( energySegment.length < input.size() ) {
			energySegment = new double[ input.size() ];
		}
		this.contour = contour;

		output.reset();
		output.addAll(input);

		computeSegmentEnergy(output);

		double total = 0;
		for (int i = 0; i < output.size(); i++) {
			total += energySegment[i];
		}

		while( output.size() > 3 ) {
			double bestEnergy = total;
			int bestIndex = -1;

			for (int i = 0; i < output.size(); i++) {
				double found = energyRemoveCorner(i, output);
				if (found < bestEnergy) {
					bestEnergy = found;
					bestIndex = i;
				}
			}

			if (bestIndex != -1) {
				output.remove(bestIndex);
				total = bestEnergy;
			} else {
				break;
			}
		}
	}

	/**
	 * Computes the energy of each segment individually
	 */
	private void computeSegmentEnergy( GrowQueue_I32 corners ) {
		for (int i = 0,j=corners.size()-1; i < corners.size(); j=i,i++) {
			energySegment[j] = computeSegmentEnergy(corners, j, i);
		}
	}

	private double energyRemoveCorner( int removed , GrowQueue_I32 corners ) {
		double total = 0;

		int cornerA = CircularIndex.addOffset(removed, -1, corners.size());
		int cornerB = CircularIndex.addOffset(removed, 1, corners.size());

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

	private double computeSegmentEnergy(GrowQueue_I32 corners, int cornerA, int cornerB) {
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
			Point2D_I32 c = getCircular( indexA+k);
			point.set(c.x,c.y);

			total += Distance2D_F64.distanceSq(line, point);
		}

		return (total+splitPentially)/a.distance2(b);
	}

	protected Point2D_I32 getCircular( int index ) {
		return contour.get(index % contour.size());
	}

	/**
	 * Distance the two points are apart in clockwise direction
	 */
	protected int circularDistance( int start , int end ) {
		return CircularIndex.distanceP(start,end,contour.size());
	}
}

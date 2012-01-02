/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.calibgrid;

import georegression.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class SquareBlob {
	public List<Point2D_I32> contour;
	// corners in CCW direction from the corner closest to -PI radians
	public List<Point2D_I32> corners;

	public Point2D_I32 center;


	// length of the largest and smallest side
	public double largestSide;
	public double smallestSide;
	
	// length of each side
	public double sideLengths[] = new double[4];

	// what each corner is connected to
	public List<SquareBlob> conn = new ArrayList<SquareBlob>();

	public SquareBlob(List<Point2D_I32> contour, List<Point2D_I32> corners) {
		this.contour = contour;
		this.corners = corners;

		center = FindQuadCorners.findAverage(contour);
		compute();
	}

	public SquareBlob() {
	}

	public void compute() {
		Point2D_I32 a = corners.get(0);
		Point2D_I32 b = corners.get(1);
		Point2D_I32 c = corners.get(2);
		Point2D_I32 d = corners.get(3);

		sideLengths[0] = Math.sqrt(a.distance2(b));
		sideLengths[1] = Math.sqrt(b.distance2(c));
		sideLengths[2] = Math.sqrt(c.distance2(d));
		sideLengths[3] = Math.sqrt(d.distance2(a));
		
		largestSide = sideLengths[0];
		smallestSide = largestSide;
		for( int i = 1; i < 4; i++ ) {
			if( largestSide < sideLengths[i])
				largestSide = sideLengths[i];
			else if( smallestSide > sideLengths[i] )
				smallestSide = sideLengths[i];
		}
	}
}

/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.describe.impl;

import gecv.alg.InputSanityCheck;
import gecv.alg.describe.RegionOrientation;
import gecv.misc.GecvMiscOps;
import gecv.struct.ImageRectangle;
import gecv.struct.image.ImageFloat32;


/**
 * @author Peter Abeles
 */
// todo add weighted option
public class ImplOrientationHistogram_F32 implements RegionOrientation<ImageFloat32> {

	// the region's radius
	private int radius;

	// image x and y derivatives
	private ImageFloat32 derivX;
	private ImageFloat32 derivY;

	// local variable used to define the region being examined.
	// this makes it easy to avoid going outside the image
	private ImageRectangle rect = new ImageRectangle();

	// number of different angles it will consider
	private int numAngles;
	// used to compute the score for each angle
	private double sumDerivX[];
	private double sumDerivY[];

	// resolution of each angle
	private double angleDiv;
	// used to round towards the nearest angle
	private double angleRound;

	public ImplOrientationHistogram_F32( int numAngles ) {
		this.numAngles = numAngles;
		sumDerivX = new double[ numAngles ];
		sumDerivY = new double[ numAngles ];

		angleDiv = 2.0*Math.PI/numAngles;
		angleRound = angleDiv/2.0;
	}

	@Override
	public void setImage(ImageFloat32 derivX, ImageFloat32 derivY) {
		InputSanityCheck.checkSameShape(derivX,derivY);

		this.derivX = derivX;
		this.derivY = derivY;
	}

	@Override
	public double compute(int c_x, int c_y) {

		// compute the visible region
		rect.x0 = c_x-radius;
		rect.y0 = c_y-radius;
		rect.x1 = c_x+radius+1;
		rect.y1 = c_y+radius+1;

		GecvMiscOps.boundRectangleInside(derivX,rect);

		for( int i = 0; i < numAngles; i++ ) {
			sumDerivX[i] = 0;
			sumDerivY[i] = 0;
		}

		// compute the score for each angle in the histogram
		for( int y = rect.y0; y < rect.y1; y++ ) {
			int indexX = derivX.startIndex + derivX.stride*y;
			int indexY = derivY.startIndex + derivY.stride*y;

			for( int x = rect.x0; x < rect.x1; x++ , indexX++ , indexY++ ) {
				float dx = derivX.data[indexX];
				float dy = derivY.data[indexY];

				double angle = Math.atan2(dy,dx);
				// compute which discretized angle it is
				int discreteAngle = (int)((angle + angleRound)/angleDiv) % numAngles;
				// sum up the "score" for this angle
				sumDerivX[discreteAngle] += dx;
				sumDerivY[discreteAngle] += dy;
			}
		}

		// find the angle with the best score
		double bestScore = -1;
		int bestIndex = -1;
		for( int i = 0; i < numAngles; i++ ) {
			double x = sumDerivX[i];
			double y = sumDerivY[i];
			double score = x*x + y*y;
			if( score > bestScore ) {
				bestScore = score;
				bestIndex = i;
			}
		}

		return angleDiv*bestIndex;
	}
}

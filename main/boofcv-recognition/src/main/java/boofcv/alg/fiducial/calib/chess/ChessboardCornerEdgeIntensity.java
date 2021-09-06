/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib.chess;

import boofcv.alg.feature.detect.chess.ChessboardCorner;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.ImageGray;

import java.util.Arrays;

/**
 * Computes edge intensity for the line between two corners. Assumes the edge is approximately straight. This means
 * lens distortion between two points can't be too massive. Orientation of corners is used to estimate which
 * side of the line should be white/black
 *
 * @author Peter Abeles
 */
public class ChessboardCornerEdgeIntensity<T extends ImageGray<T>> {

	// used to sample image at non integer points and handles boundary conditions
	InterpolatePixelS<T> interpolate;

	/**
	 * Number of points along the line from corner a to b that will be sampled
	 */
	private int lengthSamples = 15;
	private float[] sampleValues = new float[lengthSamples];

	// find the normal pointing towards white. Magnitude is relative to distance between two corners
	float nx, ny;
	// tangent step
	float tx, ty;
	float normalDiv = 15.0f;

	// length of the line segment between the two points
	float lineLength;

	int width, height;

	public ChessboardCornerEdgeIntensity( Class<T> imageType ) {
		interpolate = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);
	}

	public void setImage( T image ) {
		interpolate.setImage(image);
		this.width = image.width;
		this.height = image.height;
	}

	/**
	 * Computes a value for the line intensity between two corners.
	 *
	 * @param ca corner a
	 * @param cb corner b
	 * @param direction_a_to_b Direction from a to b in radians.
	 * @return the line intensity. more positive more intense. Units = pixels intensity
	 */
	public float process( ChessboardCorner ca, ChessboardCorner cb, double direction_a_to_b ) {
		float cx = (float)ca.x;
		float cy = (float)ca.y;
		float dx = (float)(cb.x - ca.x);
		float dy = (float)(cb.y - ca.y);

		// find the direction that it should be and the magnitude of the step in tangential direction
		computeUnitNormal(dx, dy);

		// corners will not perfectly touch. Depending on the highest resolution that a corner was detected at
		// set the offset. 2 pixels because that's the radius of the circle that the corner detector uses
		float offsetA = (float)Math.pow(2, ca.levelMax);
		float offsetB = (float)Math.pow(2, cb.levelMax);

		offsetA = Math.max(1, Math.min(offsetA, lineLength*0.1f));
		offsetB = Math.max(1, Math.min(offsetB, lineLength*0.1f));

		// step away from the corner points. This is only really important with small chessboard where the samples
		// will put it next to the corner
		float l = offsetA + offsetB;
		cx += nx*offsetA;
		cy += ny*offsetA;
		dx -= l*nx;
		dy -= l*ny;
		lineLength -= l;

		// the line is too small, abort
		if (lineLength < 2)
			return -1;

		// don't sample less than 1 pixel. This will make longitudinal checks fail
		int lengthSamples = this.lengthSamples;
		if (lengthSamples > lineLength) {
			lengthSamples = (int)lineLength;
		}

		// previous samples values along the tangent
		float prevLeft = 0;
		float prevRight = 0;
		float prevMiddle = 0;
		// The maximum longitudinal gradient magnitude
		float longitudinalMaxValue = 0;

		// The maximum distance it will sample in tangent direction. The offsets are added later which
		// is why they are substracted here
		float tangentMaxDistance = Math.max(0.0f, Math.max(1f, lineLength/normalDiv) - (offsetA + offsetB)/2f);

		// move from one side to the other
		// divide it into lengthSamples+1 regions and don't sample the tail ends
		for (int i = 0; i < lengthSamples; i++) {
			float f = ((float)i)/(lengthSamples - 1);
			float x0 = cx + dx*f;
			float y0 = cy + dy*f;

			// Linearly increase sampling distance along tangent the closer it is to sampling the center.
			// fp = [0,1]
			float fp = (0.5f - Math.abs(f - 0.5f))/0.5f;

			// Sample along the tangent now and look to see how strong the edge is
			float perpDist = offsetA*(1.0f - f) + offsetB*f + tangentMaxDistance*fp;
			float leftVal = interpolate.get(x0 - tx*perpDist, y0 - ty*perpDist);
			float rightVal = interpolate.get(x0 + tx*perpDist, y0 + ty*perpDist);
			float middle = interpolate.get(x0, y0);

			// Compute the gradient along the line
			if (i > 0) {
				//-0.5 is needed to make it independent of the order of ca and cb
				float ff = (i - 0.5f)/(lengthSamples - 1);
				// rises from 0.1 to 1.1 is flat for a bit, then falls to 0.1
				float fpp = 0.1f + Math.min(1.0f, Math.abs(0.5f - Math.abs(ff - 0.5f))/0.35f);

				// Compute longitudinal max error
				longitudinalMaxValue = Math.max(longitudinalMaxValue, Math.abs(leftVal - prevLeft)*fpp);
				longitudinalMaxValue = Math.max(longitudinalMaxValue, Math.abs(rightVal - prevRight)*fpp);
				longitudinalMaxValue = Math.max(longitudinalMaxValue, Math.abs(middle - prevMiddle)*fpp);
			}
			prevLeft = leftVal;
			prevRight = rightVal;
			prevMiddle = middle;

			sampleValues[i] = leftVal - rightVal;
		}

		// Compute the average of perpendicular gradients after removing outliers
		Arrays.sort(sampleValues, 0, lengthSamples);
		float perpendicularAverage = 0;
		int n = lengthSamples > 6 ? 2 : lengthSamples >= 3 ? 1 : 0;
		for (int i = n; i < lengthSamples - n; i++) {
			perpendicularAverage += sampleValues[i];
		}
		perpendicularAverage /= lengthSamples - n*2;

		// The tangent direction was arbitrarily selected. See if the sign needs to be flipped
		int numNegative = 0;
		for (int i = 0; i < lengthSamples; i++) {
			if (sampleValues[i] < 0)
				numNegative++;
		}
		if (numNegative > lengthSamples*3/4) {
			perpendicularAverage *= -1;
		}

		// Maximum perpendicular gradient and minimize longitudinal gradient
		return perpendicularAverage - longitudinalMaxValue;
	}

	/**
	 * Finds the line's unit normal and make sure it points towards what should be white pixels
	 *
	 * @param dx b.x - a.x
	 * @param dy b.y - a.y
	 */
	void computeUnitNormal( float dx, float dy ) {
		lineLength = (float)Math.sqrt(dx*dx + dy*dy);

		// it will now have a normal of 1
		nx = dx/lineLength;
		ny = dy/lineLength;

		// the sign is currently one known, just pick one
		tx = -ny;
		ty = nx;
	}

	public int getLengthSamples() {
		return lengthSamples;
	}

	public void setLengthSamples( int lengthSamples ) {
		this.lengthSamples = lengthSamples;
		if (sampleValues.length < lengthSamples)
			sampleValues = new float[lengthSamples];
	}

	public Class<T> getImageType() {
		return interpolate.getImageType().getImageClass();
	}
}

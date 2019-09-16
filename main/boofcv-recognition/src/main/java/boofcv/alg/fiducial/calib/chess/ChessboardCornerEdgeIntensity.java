/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import georegression.metric.UtilAngle;
import org.ddogleg.sorting.QuickSelect;

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
	private int lengthSamples=10;
	private float[] sampleValues = new float[lengthSamples];

	/**
	 * Number of points radially outwards along the line that are sampled
	 */
	int tangentSamples=2;

	// find the normal pointing towards white. Magnitude is relative to distance between two corners
	float nx,ny;
	// tangent step
	float tx,ty;
	float normalDiv = 30.0f;
	// dynamically computed based on length of side. This is how far away it samples
	private float tangentSampleStep;

	// length of the line segment between the two points
	float lineLength;

	int width,height;

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
	public float process(ChessboardCorner ca, ChessboardCorner cb, double direction_a_to_b ) {
//		boolean meow = false;
//		if( ca.distance(200,369) < 1.5 && cb.distance(198,358) < 2)
//			meow = true;
//		if( cb.distance(200,369) < 1.5 && ca.distance(198,358) < 2)
//			meow = true;

		float cx = (float)ca.x;
		float cy = (float)ca.y;
		float dx = (float)(cb.x-ca.x);
		float dy = (float)(cb.y-ca.y);

		// find the direction that it should be and the magnitude of the step in tangential direction
		computeUnitNormal(ca,cb, direction_a_to_b, dx, dy);

		// corners will not perfectly touch. Depending on the highest resolution that a corner was detected at
		// set the offset. 2 pixels because that's the radius of the circle that the corner detector uses
		float offsetA = (float)Math.pow(2,ca.levelMax);
		float offsetB = (float)Math.pow(2,cb.levelMax);

		// step away from the corner points. This is only really important with small chessboard where the samples
		// will put it next to the corner
		if( lineLength > offsetA+offsetB ) {
			double l = offsetA+offsetB;
			cx += nx*offsetA;
			cy += ny*offsetA;
			dx -= l * nx;
			dy -= l * ny;
			lineLength -= l;
		}

		// move from one side to the other
		// divide it into lengthSamples+1 regions and don't sample the tail ends
		float maxLongitudinal = 0;
		float prevLong=0;

		// don't sample less than 1 pixel. This will make longitudinal checks fail
		int lengthSamples = this.lengthSamples;
		if( lengthSamples > lineLength ) {
			lengthSamples = (int)lineLength;
		}

		for (int i = 0; i < lengthSamples; i++) {
			float f = ((float)i)/(lengthSamples-1);
			float x0 = cx+dx*f;
			float y0 = cy+dy*f;

			// Linearly increase sampling distance along tangent the closer it is to sampling the center.
			// fp = [0,1]
			float fp = (0.5f-Math.abs(f-0.5f))/0.5f;

			float v = interpolate.get(x0,y0);
			float d = Math.abs(v-prevLong)*(0.1f+fp); // multiply by FP since it's more likely to be white at edges
			if( i > 0 ) {
				if( d > maxLongitudinal )
					maxLongitudinal = d;
			}
			prevLong = v;

			float maxValue = -Float.MAX_VALUE;

			for (int l = 0; l < tangentSamples; l++) {
				float perpDist = (offsetA+offsetB)/2f + l*tangentSampleStep*(0.1f+fp);
				float white = interpolate.get(x0-ty*perpDist,y0+tx*perpDist);
				float black = interpolate.get(x0+ty*perpDist,y0-tx*perpDist);

//				if( meow ) {
//					System.out.printf("i=%2d white=%5.2f black=%5.2f\n",i,white,black);
//				}

				maxValue = Math.max(maxValue,white-black);
			}

			sampleValues[i] = maxValue;
		}
//		if( meow )
//			System.out.println("done");

		// Select one of the most intense values.
		// Originally min was used but that proved too sensitive to outliers
		return (QuickSelect.select(sampleValues,2,lengthSamples)-maxLongitudinal);
	}

	/**
	 * Finds the line's unit normal and make sure it points towards what should be white pixels
	 * @param ca corner a
	 * @param direction_a_to_b direction from corner a to b. radians. -pi to pi
	 * @param dx b.x - a.x
	 * @param dy b.y - a.y
	 */
	void computeUnitNormal(ChessboardCorner ca, ChessboardCorner cb, double direction_a_to_b, float dx, float dy) {
		lineLength = (float)Math.sqrt(dx*dx + dy*dy);

		// it will now have a normal of 1
		nx = tx = dx/lineLength;
		ny = ty = dy/lineLength;

		// set the magnitude relative to the square size. Blurred images won't have sharp edges
		// at the same time the magnitude of |n| shouldn't be less than 1
		tangentSampleStep = Math.max(1f,lineLength/normalDiv);

		double dir0 = UtilAngle.boundHalf(direction_a_to_b);
		double dir1 = UtilAngle.boundHalf(ca.orientation-Math.PI/4);

		double distA = UtilAngle.distHalf(dir0,dir1);

		dir0 = UtilAngle.boundHalf(UtilAngle.bound(direction_a_to_b+Math.PI));
		dir1 = UtilAngle.boundHalf(cb.orientation+Math.PI/4);

		double distB = UtilAngle.distHalf(dir0,dir1);

		// Under fisheye distortion it's possible to have a corner's orientation point along the line connecting
		// two corners. In that situation you should go with the corner that has an orientation with the
		// most discrimination
		if( UtilAngle.distHalf(distA,Math.PI/4.0) > UtilAngle.distHalf(distB,Math.PI/4.0) ) {
			if(distA < Math.PI/4.0 ) {
				tx = -tx;
				ty = -ty;
			}
		} else {
			if(distB < Math.PI/4.0 ) {
				tx = -tx;
				ty = -ty;
			}
		}
	}

	public int getLengthSamples() {
		return lengthSamples;
	}

	public void setLengthSamples(int lengthSamples) {
		this.lengthSamples = lengthSamples;
		if( sampleValues.length < lengthSamples)
			sampleValues = new float[lengthSamples];
	}

	public int getTangentSamples() {
		return tangentSamples;
	}

	public void setTangentSamples(int tangentSamples) {
		this.tangentSamples = tangentSamples;
	}

	public Class<T> getImageType() {
		return interpolate.getImageType().getImageClass();
	}
}

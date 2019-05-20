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
	int lengthSamples=5;
	/**
	 * Number of points radially outwards along the line that are sampled
	 */
	int tangentSamples=2;

	// find the normal pointing towards white. Magnitude is relative to distance between two corners
	float nx,ny;
	float normalDiv = 15.0f;

	public ChessboardCornerEdgeIntensity( Class<T> imageType ) {
		interpolate = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);
	}

	public void setImage( T image ) {
		interpolate.setImage(image);
	}

	/**
	 * Computes a value for the line intensity between two corners.
	 *
	 * @param ca corner a
	 * @param cb corner b
	 * @param direction_a_to_b Direction from a to b in radians.
	 * @return the line intensity. more positive more intense. Units = pixels intensity
	 */
	public float process( ChessboardCorner ca , ChessboardCorner cb , double direction_a_to_b ) {
		float cx = (float)ca.x;
		float cy = (float)ca.y;
		float dx = (float)(cb.x-ca.x);
		float dy = (float)(cb.y-ca.y);

		// find the direction that it should be
		computeUnitNormal(ca, direction_a_to_b, dx, dy);

		// move from one side to the other
		// divide it into lengthSamples+1 regions and don't sample the tail ends
		float intensity = Float.MAX_VALUE;
		for (int i = 1; i <= lengthSamples; i++) {
			float f = ((float)i)/(lengthSamples+1);
			float x0 = cx+dx*f;
			float y0 = cy+dy*f;

			float maxValue = -Float.MAX_VALUE;

			for (int l = 1; l <= tangentSamples; l++) {
				float white = interpolate.get(x0+nx*l,y0+ny*l);
				float black = interpolate.get(x0-nx*l,y0-ny*l);

				maxValue = Math.max(maxValue,white-black);
			}

			intensity = Math.min(intensity,maxValue);
		}

		return intensity;
	}

	/**
	 * Finds the line's unit normal and make sure it points towards what should be white pixels
	 * @param ca corner a
	 * @param direction_a_to_b direction from corner a to b. radians. -pi to pi
	 * @param dx b.x - a.x
	 * @param dy b.y - a.y
	 */
	void computeUnitNormal(ChessboardCorner ca, double direction_a_to_b, float dx, float dy) {
		float r = (float)Math.sqrt(dx*dx + dy*dy);

		// it will now have a normal of 1
		nx = -dy/r;
		ny = dx/r;

		// set the magnitude relative to the square size. Blurred images won't have sharp edges
		// at the same time the magnitude of |n| shouldn't be less than 1
		if( r > 1.0 ) {
			r /= normalDiv;
			nx *= r;
			ny *= r;
		}

		double dir0 = UtilAngle.boundHalf(direction_a_to_b);
		double dir1 = UtilAngle.boundHalf(ca.orientation-Math.PI/4);
		if(UtilAngle.distHalf(dir0,dir1) < Math.PI/4.0 ) {
			nx = -nx;
			ny = -ny;
		}
	}

	public int getLengthSamples() {
		return lengthSamples;
	}

	public void setLengthSamples(int lengthSamples) {
		this.lengthSamples = lengthSamples;
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

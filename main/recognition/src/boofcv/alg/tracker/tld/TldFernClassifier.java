/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.tld;

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F32;

import java.util.Random;

/**
 * Manages ferns, creates their descriptions, compute their values, and handles their probabilities.
 *
 * @author Peter Abeles
 */
public class TldFernClassifier<T extends ImageSingleBand> {

	// List of randomly generated ferns
	protected TldFernDescription[] ferns;
	// used to look up fern values
	protected TldFernManager[] managers;

	// provides sub-pixel interpolation to improve quality at different scales
	private InterpolatePixel<T> interpolate;

	// total probability
	private double probability;

	/**
	 * Configures fern algorithm
	 *
	 * @param rand Random number generated used for creating ferns
	 * @param numFerns Number of ferns to created.  Typically 10
	 * @param descriptorSize Size of each fern's descriptor.  Typically 10
	 * @param interpolate Interpolation function for the image
	 */
	public TldFernClassifier( Random rand , int numFerns , int descriptorSize ,
							  InterpolatePixel<T> interpolate ) {

		this.interpolate = interpolate;

		ferns = new TldFernDescription[numFerns];
		managers = new TldFernManager[numFerns];

		// create random ferns
		for( int i = 0; i < numFerns; i++ ) {
			ferns[i] = new TldFernDescription(rand,descriptorSize);
			managers[i] = new TldFernManager(descriptorSize);
		}
	}
	/**
	 * Discard all information on fern values and their probabilities
	 */
	public void reset() {
		for( int i = 0; i < managers.length; i++ )
			managers[i].reset();
	}

	/**
	 * Call before any other functions.  Provides the image that is being sampled.
	 *
	 * @param gray Input image.
	 */
	public void setImage(T gray) {
		interpolate.setImage(gray);
	}

	public void updateFerns( boolean positive , ImageRectangle r ) {

		float rectWidth = r.getWidth();
		float rectHeight = r.getHeight();

		float fx0 = r.x0;
		float fy0 = r.y0;

		for( int i = 0; i < ferns.length; i++ ) {
			int value = computeFernValue(fx0, fy0, rectWidth, rectHeight,ferns[i]);

			TldFernFeature f = managers[i].lookupFern(value);
			if( positive )
				f.incrementP();
			else
				f.incrementN();
		}
	}

	/**
	 * Checks the probability of all ferns at the specified location.  If they pass the test 1 is returned, otherwise
	 * 0 is returned.  To pass the test the forms must have an average posterior probability of 0.5
	 *
	 * The test aborts early if it finds that the probability is too low to pass
	 *
	 * @return true if it could be the target and false if not
	 */
	public boolean performTest( ImageRectangle r ) {

		float rectWidth = r.getWidth();
		float rectHeight = r.getHeight();

		float fx0 = r.x0;
		float fy0 = r.y0;

		probability = 0;

		int h = ferns.length/2;

		for( int i = 0; i < ferns.length; i++ ) {
			TldFernDescription fern = ferns[i];

			int value = computeFernValue(fx0, fy0, rectWidth, rectHeight, fern);
			probability += managers[i].lookupPosterior(value);

			if( i == h && probability <= 0 )
				return false;
		}

		return probability/ferns.length > 0.5;
	}

	/**
	 * Computes the value of the specified fern at the specified location in the image.
	 */
	protected int computeFernValue(float x0, float y0, float rectWidth , float rectHeight , TldFernDescription fern ) {

		int desc = 0;
		for( int i = 0; i < fern.pairs.length; i++ ) {
			Point2D_F32 p_a = fern.pairs[i].a;
			Point2D_F32 p_b = fern.pairs[i].b;

			// TODO make sure this is safe
			float valA = interpolate.get_unsafe(x0 + p_a.x * rectWidth, y0 + p_a.y * rectHeight);
			float valB = interpolate.get_unsafe(x0 + p_b.x * rectWidth, y0 + p_b.y * rectHeight);

			desc *= 2;

			if( valA < valB ) {
				desc += 1;
			}
		}

		return desc;
	}

	public double getProbability() {
		return probability;
	}
}

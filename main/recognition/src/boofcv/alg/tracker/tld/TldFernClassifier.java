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
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.shapes.RectangleCorner2D_F64;
import georegression.transform.affine.AffinePointOps;

import java.util.Random;

/**
 * Manages ferns, creates their descriptions, compute their values, and handles their probabilities.
 *
 * @author Peter Abeles
 */
public class TldFernClassifier<T extends ImageSingleBand> {

	// random number generator used while learning
	private Random rand;
	// number of random ferns it learns
	private int numLearnRandom;
	// standard deviation of noise used while learning
	private float fernLearnNoise;

	// List of randomly generated ferns
	protected TldFernDescription[] ferns;
	// used to look up fern values
	protected TldFernManager[] managers;

	// provides sub-pixel interpolation to improve quality at different scales
	private InterpolatePixel<T> interpolate;

	// total probability
	private double probability;

	// storage for transformed points
	private Point2D_F32 tranA = new Point2D_F32();
	private Point2D_F32 tranB = new Point2D_F32();

	/**
	 * Configures fern algorithm
	 *
	 * @param rand Random number generated used for creating ferns
	 * @param numFerns Number of ferns to created.  Typically 10
	 * @param descriptorSize Size of each fern's descriptor.  Typically 10
	 * @param numLearnRandom TODO comment
	 * @param fernLearnNoise
	 * @param interpolate Interpolation function for the image
	 */
	public TldFernClassifier( Random rand , int numFerns , int descriptorSize ,
							  int numLearnRandom , float fernLearnNoise ,
							  InterpolatePixel<T> interpolate ) {

		this.rand = rand;
		this.interpolate = interpolate;
		this.numLearnRandom = numLearnRandom;
		this.fernLearnNoise = fernLearnNoise;

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

	/**
	 * Learns ferns inside the specified rectangle.  An affine transform is applied to each sample point in the fern.
	 * Noise is added to the sample image points to create a more robust representation of the fern.  Otherwise
	 * the fern test will fail for the exact same region with no motion due to image noise.
	 *
	 * WARNING: Affine is applied to the sample point before being adjusted to the rectangle's width/height.  So
	 * specify it as a relative fraction.
	 *
	 * WARNING: Make sure the transformed rectangle is in bounds first!
	 */
	public void learnFernNoise(boolean positive, RectangleCorner2D_F64 r, Affine2D_F32 transform) {

		float rectWidth = (float)r.getWidth();
		float rectHeight = (float)r.getHeight();

		float c_x = (float)r.x0+rectWidth/2.0f;
		float c_y = (float)r.y0+rectHeight/2.0f;

		for( int i = 0; i < ferns.length; i++ ) {

			// first learn it with no noise
			int value = computeFernValue(c_x, c_y, rectWidth, rectHeight,transform,ferns[i]);
			TldFernFeature f = managers[i].lookupFern(value);
			if( positive )
				f.incrementP();
			else
				f.incrementN();

			for( int j = 0; j < numLearnRandom; j++ ) {
				value = computeFernValueRand(c_x, c_y, rectWidth, rectHeight,transform,ferns[i]);

				f = managers[i].lookupFern(value);
				if( positive )
					f.incrementP();
				else
					f.incrementN();
			}
		}
	}

	public void learnFern(boolean positive, ImageRectangle r) {

		float rectWidth = r.getWidth();
		float rectHeight = r.getHeight();

		float c_x = r.x0+rectWidth/2.0f;
		float c_y = r.y0+rectHeight/2.0f;

		for( int i = 0; i < ferns.length; i++ ) {

			// first learn it with no noise
			int value = computeFernValue(c_x, c_y, rectWidth, rectHeight,ferns[i]);
			TldFernFeature f = managers[i].lookupFern(value);
			if( positive )
				f.incrementP();
			else
				f.incrementN();
		}
	}

	public void learnFernNoise(boolean positive, ImageRectangle r) {

		float rectWidth = r.getWidth();
		float rectHeight = r.getHeight();

		float c_x = r.x0+rectWidth/2.0f;
		float c_y = r.y0+rectHeight/2.0f;

		for( int i = 0; i < ferns.length; i++ ) {

			// first learn it with no noise
			int value = computeFernValue(c_x, c_y, rectWidth, rectHeight,ferns[i]);
			TldFernFeature f = managers[i].lookupFern(value);
			if( positive )
				f.incrementP();
			else
				f.incrementN();

			for( int j = 0; j < numLearnRandom; j++ ) {
				value = computeFernValueRand(c_x, c_y, rectWidth, rectHeight,ferns[i]);
				f = managers[i].lookupFern(value);
				if( positive )
					f.incrementP();
				else
					f.incrementN();
			}
		}
	}

	public boolean lookupFernPN( TldRegionFernInfo info ) {

		ImageRectangle r = info.r;

		float rectWidth = r.getWidth();
		float rectHeight = r.getHeight();

		float c_x = r.x0+rectWidth/2.0f;
		float c_y = r.y0+rectHeight/2.0f;

		probability = 0;

		int sumP = 0;
		int sumN = 0;

		for( int i = 0; i < ferns.length; i++ ) {
			TldFernDescription fern = ferns[i];

			int value = computeFernValue(c_x, c_y, rectWidth, rectHeight, fern);

			TldFernFeature f = managers[i].lookupFern(value);
			if( f != null ) {
				sumP += f.numP;
				sumN += f.numN;
			}
		}

		info.sumP = sumP;
		info.sumN = sumN;

		return sumN != 0 || sumP != 0;
	}

	/**
	 * Computes the value of the specified fern at the specified location in the image. Noise is added
	 * to the measurements and an affine transform applied to each sample point
	 */
	protected int computeFernValueRand(float c_x, float c_y, float rectWidth, float rectHeight,
									   Affine2D_F32 transform,
									   TldFernDescription fern) {

		int desc = 0;
		for( int i = 0; i < fern.pairs.length; i++ ) {
			Point2D_F32 p_a = fern.pairs[i].a;
			Point2D_F32 p_b = fern.pairs[i].b;

			AffinePointOps.transform(transform,p_a,tranA);
			AffinePointOps.transform(transform,p_b,tranB);

			float valA = interpolate.get_unsafe(c_x + tranA.x * rectWidth, c_y + tranA.y * rectHeight);
			float valB = interpolate.get_unsafe(c_x + tranB.x * rectWidth, c_y + tranB.y * rectHeight);

			valA += rand.nextGaussian()*fernLearnNoise;
			valB += rand.nextGaussian()*fernLearnNoise;

			desc *= 2;

			if( valA < valB ) {
				desc += 1;
			}
		}

		return desc;
	}

	/**
	 * Computes the fern's value with no random noise applied to it inside a distorted region
	 */
	protected int computeFernValue(float c_x, float c_y, float rectWidth, float rectHeight,
								   Affine2D_F32 transform,
								   TldFernDescription fern) {

		int desc = 0;
		for( int i = 0; i < fern.pairs.length; i++ ) {
			Point2D_F32 p_a = fern.pairs[i].a;
			Point2D_F32 p_b = fern.pairs[i].b;

			AffinePointOps.transform(transform,p_a,tranA);
			AffinePointOps.transform(transform,p_b,tranB);

			// todo change back to unsafe
			float valA = interpolate.get(c_x + tranA.x * rectWidth, c_y + tranA.y * rectHeight);
			float valB = interpolate.get(c_x + tranB.x * rectWidth, c_y + tranB.y * rectHeight);

			desc *= 2;

			if( valA < valB ) {
				desc += 1;
			}
		}

		return desc;
	}

	/**
	 * Computes the value of the specified fern at the specified location in the image.
	 */
	protected int computeFernValue(float c_x, float c_y, float rectWidth , float rectHeight , TldFernDescription fern ) {

		int desc = 0;
		for( int i = 0; i < fern.pairs.length; i++ ) {
			Point2D_F32 p_a = fern.pairs[i].a;
			Point2D_F32 p_b = fern.pairs[i].b;

			// TODO make sure this is safe
			float valA = interpolate.get_unsafe(c_x + p_a.x * rectWidth, c_y + p_a.y * rectHeight);
			float valB = interpolate.get_unsafe(c_x + p_b.x * rectWidth, c_y + p_b.y * rectHeight);

			desc *= 2;

			if( valA < valB ) {
				desc += 1;
			}
		}

		return desc;
	}

	protected int computeFernValueRand(float c_x, float c_y, float rectWidth , float rectHeight , TldFernDescription fern ) {

		int desc = 0;
		for( int i = 0; i < fern.pairs.length; i++ ) {
			Point2D_F32 p_a = fern.pairs[i].a;
			Point2D_F32 p_b = fern.pairs[i].b;

			// TODO make sure this is safe
			float valA = interpolate.get_unsafe(c_x + p_a.x * rectWidth, c_y + p_a.y * rectHeight);
			float valB = interpolate.get_unsafe(c_x + p_b.x * rectWidth, c_y + p_b.y * rectHeight);

			valA += rand.nextGaussian()*fernLearnNoise;
			valB += rand.nextGaussian()*fernLearnNoise;

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

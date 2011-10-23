/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.describe;

import boofcv.abst.filter.blur.BlurFilter;
import boofcv.alg.feature.describe.brief.BriefDefinition_I32;
import boofcv.alg.feature.describe.brief.BriefFeature;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_I32;

/**
 * <p>
 * BRIEF: Binary Robust Independent Elementary Features. [1] Invariance: light.  Fast to compute
 * and to compare feature descriptions.  Shown to be more robust than SURF in situations it was designed for.
 * </p>
 *
 * <p>
 * Describes an image region by comparing a large number of mage point pairs.  The location of each point in the
 * pair is determined by the feature's {@link boofcv.alg.feature.describe.brief.BriefDefinition_I32 definition} and the comparison itself is done using
 * a simple less than operator: pixel(1) < pixel(2).  Distance between two descriptors is computed using the Hamming distance.
 * </p>
 *
 * <p>
 * [1] Michael Calonder, Vincent Lepetit, Christoph Strecha, and Pascal Fua. "BRIEF: Binary Robust Independent Elementary
 * Features" in European Conference on Computer Vision, September 2010.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class DescribePointBrief<T extends ImageBase> {
	// describes the BRIEF feature
	protected BriefDefinition_I32 definition;
	// blurs the image prior to sampling
	protected BlurFilter<T> filterBlur;
	// blurred image
	protected T blur;

	// precomputed offsets of sample points inside the image.
	// splitting it into two arrays avoids an extract array lookup, boosting performance by about 30%
	private  int offsets[]; // just a temporary place holder
	protected int offsetsA[];
	protected int offsetsB[];

	public DescribePointBrief(BriefDefinition_I32 definition, BlurFilter<T> filterBlur) {
		this.definition = definition;
		this.filterBlur = filterBlur;

		blur = GeneralizedImageOps.createImage(filterBlur.getInputType(),1,1);

		offsets = new int[ definition.samplePoints.length ];
		offsetsA = new int[ definition.compare.length ];
		offsetsB = new int[ definition.compare.length ];
	}

	/**
	 * Function which creates a description of the appropriate size.
	 *
	 * @return Creates a bew description.
	 */
	public BriefFeature createFeature() {
		return new BriefFeature(definition.getLength());
	}

	/**
	 * Specifies the image from which feature descriptions are to be created.
	 *
	 * @param image Image being examined.
	 */
	public void setImage(T image) {
		blur.reshape(image.width,image.height);
		filterBlur.process(image,blur);

		// precompute offsets for faster computing later on
		for( int i = 0; i < definition.samplePoints.length ; i++ ) {
			Point2D_I32 a = definition.samplePoints[i];

			offsets[i] = blur.startIndex + blur.stride*a.y + a.x;
		}

		for( int i = 0; i < definition.compare.length ; i++ ) {
			Point2D_I32 p = definition.compare[i];
			offsetsA[i] = offsets[p.x];
			offsetsB[i] = offsets[p.y];
		}
	}

	/**
	 * Computes the descriptor at the specified point.  If the region go outside of the image then a description
	 * will not be made.
	 *
	 * @param c_x Center of region being described.
	 * @param c_y Center of region being described.
	 * @param feature Where the descriptor is written to.
	 * @return true indicates a descriptor could be created and false one was not.
	 */
	public abstract boolean process( int c_x , int c_y , BriefFeature feature );

	public BriefDefinition_I32 getDefinition() {
		return definition;
	}
}

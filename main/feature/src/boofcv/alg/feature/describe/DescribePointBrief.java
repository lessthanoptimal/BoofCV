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

package boofcv.alg.feature.describe;

import boofcv.abst.filter.blur.BlurFilter;
import boofcv.alg.feature.describe.brief.BriefDefinition_I32;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.feature.TupleDesc_B;
import boofcv.struct.image.ImageSingleBand;
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
public abstract class DescribePointBrief<T extends ImageSingleBand> {
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

		blur = GeneralizedImageOps.createSingleBand(filterBlur.getInputType(), 1, 1);

		offsets = new int[ definition.samplePoints.length ];
		offsetsA = new int[ definition.compare.length ];
		offsetsB = new int[ definition.compare.length ];
	}

	/**
	 * Function which creates a description of the appropriate size.
	 *
	 * @return Creates a bew description.
	 */
	public TupleDesc_B createFeature() {
		return new TupleDesc_B(definition.getLength());
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

			offsets[i] = blur.stride*a.y + a.x;
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
	 */
	public void process( double c_x , double c_y , TupleDesc_B feature ) {
		if( BoofMiscOps.checkInside(blur,(int)c_x, (int)c_y, definition.radius) ) {
			processInside(c_x,c_y,feature);
		} else {
			processBorder(c_x,c_y,feature);
		}
	}

	/**
	 * Called if the descriptor region is contained intirely inside the image
	 */
	public abstract void processInside( double c_x , double c_y , TupleDesc_B feature );

	/**
	 * Called if the descriptor region goes outside the image border
	 */
	public abstract void processBorder( double c_x , double c_y , TupleDesc_B feature );

	public BriefDefinition_I32 getDefinition() {
		return definition;
	}
}

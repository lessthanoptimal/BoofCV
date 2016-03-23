/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;

import java.lang.reflect.Array;

/**
 * Computes a feature description from {@link Planar} images by computing a descriptor separately in each band.
 * The output descriptor is computed by concatenating the descriptors for each bands together. So [1,2,3] and [3,4,5]
 * from a two band image will become [1,2,3,3,4,5].
 *
 * @author Peter Abeles
 */
public abstract class DescribePlanar<T extends ImageGray, Desc extends TupleDesc>
		implements DescribeRegionPoint<Planar<T>,Desc>
{

	// descriptor for each band in the image
	DescribeRegionPoint<T,Desc> describers[];

	// number of elements in the output descriptor
	int length;
	Class<Desc> descType;

	// storage for the descriptor in each band
	Desc descBand[];

	/**
	 * Configuration
	 *
	 * @param describers A descriptor for each band in the image.
	 */
	public DescribePlanar(DescribeRegionPoint<T, Desc> describers[]) {
		this.describers = describers;

		descType = describers[0].getDescriptionType();
		descBand = (Desc[])Array.newInstance(descType,describers.length);

		length = 0;
		for( int i = 0; i < describers.length; i++ ) {
			descBand[i] = describers[i].createDescription();
			length += descBand[i].size();
		}

	}

	@Override
	public void setImage(Planar<T> image) {
		if( image.getNumBands() != describers.length ) {
			throw new IllegalArgumentException("Unexpected number of bands in input image.  Found "+
					image.getNumBands()+" expected "+describers.length);
		}

		for( int i = 0; i < describers.length; i++ ) {
			describers[i].setImage(image.getBand(i));
		}
	}

	@Override
	public boolean process(double x, double y, double orientation, double radius, Desc description) {
		// compute descriptions individually
		for( int i = 0; i < describers.length; i++ ) {
			if( !describers[i].process(x,y,orientation, radius,descBand[i]) )
				return false;
		}

		// combine descriptors together
		combine(description);

		return true;
	}

	/**
	 * Given all the descriptors computed independently in each band, combine them together into a single descriptor.
	 *
	 * @param description
	 */
	protected abstract void combine( Desc description );

	@Override
	public boolean requiresRadius() {
		return describers[0].requiresRadius();
	}

	@Override
	public boolean requiresOrientation() {
		return describers[0].requiresOrientation();
	}

	@Override
	public Class<Desc> getDescriptionType() {
		return descType;
	}

	@Override
	public double getCanonicalWidth() {
		return describers[0].getCanonicalWidth();
	}
}

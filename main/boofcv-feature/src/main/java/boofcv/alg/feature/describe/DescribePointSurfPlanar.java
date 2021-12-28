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

package boofcv.alg.feature.describe;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;

/**
 * Computes a color SURF descriptor from a {@link Planar} image. Each band in the
 * input image is used to compute its own descriptor, which are then combined together into a single one. The
 * laplacian sign is computed from a gray-scale image. The descriptor from each band are not individually
 * normalized. The whole combined descriptor is normalized.
 *
 * @param <II> Type of integral image
 * @author Peter Abeles
 * @see DescribePointSurf
 * @see DescribePointSurfMod
 */
@SuppressWarnings({"NullAway.Init"})
public class DescribePointSurfPlanar<II extends ImageGray<II>> {
	// SURF algorithms
	private final DescribePointSurf<II> describe;

	// number of elements in the feature
	private final int descriptorLength;

	// integral of gray image
	private II grayII;
	// integral of multi-band image
	private Planar<II> colorII;

	// storage for feature compute in each band
	private final TupleDesc_F64 bandDesc;

	// number of bands in the input image
	private final int numBands;

	public DescribePointSurfPlanar( DescribePointSurf<II> describe,
									int numBands ) {
		this.describe = describe;
		this.numBands = numBands;

		bandDesc = new TupleDesc_F64(describe.getDescriptionLength());
		descriptorLength = describe.getDescriptionLength()*numBands;
	}

	public TupleDesc_F64 createDescription() {
		return new TupleDesc_F64(descriptorLength);
	}

	public int getDescriptorLength() {
		return descriptorLength;
	}

	public Class<TupleDesc_F64> getDescriptionType() {
		return TupleDesc_F64.class;
	}

	/**
	 * Specifies input image shapes.
	 *
	 * @param grayII integral image of gray scale image
	 * @param colorII integral image of color image
	 */
	public void setImage( II grayII, Planar<II> colorII ) {
		InputSanityCheck.checkSameShape(grayII, colorII);
		if (colorII.getNumBands() != numBands)
			throw new IllegalArgumentException("Expected planar images to have "
					+ numBands + " not " + colorII.getNumBands());

		this.grayII = grayII;
		this.colorII = colorII;
	}

	public void describe( double x, double y, double angle, double scale, TupleDesc_F64 desc ) {
		int featureIndex = 0;
		for (int band = 0; band < colorII.getNumBands(); band++) {
			describe.setImage(colorII.getBand(band));
			describe.describe(x, y, angle, scale, false, bandDesc);
			System.arraycopy(bandDesc.data, 0, desc.data, featureIndex, bandDesc.size());
			featureIndex += bandDesc.size();
		}

		UtilFeature.normalizeL2(desc);

		describe.setImage(grayII);
	}

	public DescribePointSurf<II> getDescribe() {
		return describe;
	}

	public int getNumBands() {
		return numBands;
	}

	public DescribePointSurfPlanar<II> copy() {
		return new DescribePointSurfPlanar<>(describe.copy(), numBands);
	}
}

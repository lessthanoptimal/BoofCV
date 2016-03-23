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

import boofcv.alg.descriptor.UtilFeature;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;

/**
 * Computes a color SURF descriptor from a {@link Planar} image.  Each band in the
 * input image is used to compute its own descriptor, which are then combined together into a single one. The
 * laplacian sign is computed from a gray-scale image.  The descriptor from each band are not individually
 * normalized.  The whole combined descriptor is normalized.
 *
 * @see DescribePointSurf
 * @see DescribePointSurfMod
 *
 * @param <II> Type of integral image
 *
 * @author Peter Abeles
 */
public class DescribePointSurfPlanar<II extends ImageGray>
{
	// SURF algorithms
	private DescribePointSurf<II> describe;

	// number of elements in the feature
	private int descriptorLength;

	// integral of gray image
	private II grayII;
	// integral of multi-band image
	private Planar<II> ii;

	// storage for feature compute in each band
	private TupleDesc_F64 bandDesc;

	// number of bands in the input image
	private int numBands;

	public DescribePointSurfPlanar(DescribePointSurf<II> describe,
								   int numBands )
	{
		this.describe = describe;
		this.numBands = numBands;

		bandDesc = new TupleDesc_F64(describe.getDescriptionLength());
		descriptorLength = describe.getDescriptionLength()*numBands;
	}

	public BrightFeature createDescription() {
		return new BrightFeature(descriptorLength);
	}

	public int getDescriptorLength() {
		return descriptorLength;
	}

	public Class<BrightFeature> getDescriptionType() {
		return BrightFeature.class;
	}

	public void setImage( II grayII , Planar<II> integralImage ) {
		this.grayII = grayII;
		ii = integralImage;
	}

	public void describe(double x, double y, double angle, double scale, BrightFeature desc)
	{
		int featureIndex = 0;
		for( int band = 0; band < ii.getNumBands(); band++ ) {
			describe.setImage(ii.getBand(band));
			describe.describe(x,y, angle, scale, bandDesc);
			System.arraycopy(bandDesc.value,0,desc.value,featureIndex,bandDesc.size());
			featureIndex += bandDesc.size();
		}

		UtilFeature.normalizeL2(desc);

		describe.setImage(grayII);
		desc.white = describe.computeLaplaceSign((int)(x+0.5),(int)(y+0.5),scale);
	}

	public DescribePointSurf<II> getDescribe() {
		return describe;
	}

	public int getNumBands() {
		return numBands;
	}
}

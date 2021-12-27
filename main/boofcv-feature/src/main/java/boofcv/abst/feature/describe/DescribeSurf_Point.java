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

package boofcv.abst.feature.describe;

import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Wrapper around SURF for {@link DescribePoint}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DescribeSurf_Point<T extends ImageGray<T>, II extends ImageGray<II>>
		implements DescribePoint<T, TupleDesc_F64> {

	/** Size of the region's radius. If set to a value &le; 0, then it will be changed to canonical radius */
	public double regionRadius;

	// computes SURF feature descriptor
	protected DescribePointSurf<II> surf;
	// Integral image based orientation estimate
	protected OrientationIntegral<II> orientation;

	// integral image
	II ii;

	ImageType<T> imageType;
	final double canonicalRadius;

	public DescribeSurf_Point( DescribePointSurf<II> surf,
							   OrientationIntegral<II> orientation,
							   double regionRadius,
							   Class<T> imageType ) {
		this.surf = surf;
		this.orientation = orientation;
		this.imageType = ImageType.single(imageType);
		this.regionRadius = regionRadius;

		this.canonicalRadius = surf.getCanonicalWidth()/2.0;
	}

	@Override public void setImage( T image ) {
		if (ii != null) {
			ii.reshape(image.width, image.height);
		}

		// compute integral image
		ii = GIntegralImageOps.transform(image, ii);
		surf.setImage(ii);
		orientation.setImage(ii);

		// See if it was requested that the default radius be used
		if (regionRadius <= 0.0)
			regionRadius = canonicalRadius;
		orientation.setObjectRadius(regionRadius);
	}

	@Override public boolean process( double x, double y, TupleDesc_F64 storage ) {
		double angle = orientation.compute(x, y);
		double scale = regionRadius/canonicalRadius;
		surf.describe(x, y, angle, scale, true, storage);

		return true;
	}

	@Override public TupleDesc_F64 createDescription() {return new TupleDesc_F64(surf.getDescriptionLength());}

	@Override public ImageType<T> getImageType() {return imageType;}

	@Override public Class<TupleDesc_F64> getDescriptionType() {return TupleDesc_F64.class;}
}

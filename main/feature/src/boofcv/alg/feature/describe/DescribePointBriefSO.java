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

import boofcv.abst.filter.blur.BlurFilter;
import boofcv.alg.feature.describe.brief.BinaryCompareDefinition_I32;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.BoofDefaults;
import boofcv.struct.feature.TupleDesc_B;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I32;

import java.util.Arrays;

/**
 * <p>
 * Extension of {@link DescribePointBrief} which adds invariance to orientation and scale.  Invariance is added by simply
 * applying an orientation/scale transform to the sample points and then applying interpolation to the point at which
 * it has been sampled.
 * </p>
 *
 * <p>
 * Border pixels are handled by setting their value to zero when comparing.
 * </p>
 *
 * @author Peter Abeles
 */
public class DescribePointBriefSO<T extends ImageGray> {
	// describes the BRIEF feature
	protected BinaryCompareDefinition_I32 definition;

	// blurs the image prior to sampling
	protected BlurFilter<T> filterBlur;
	// blurred image
	protected T blur;

	// used to interpolate pixel value at rotated coordinate
	protected InterpolatePixelS<T> interp;

	// values at each sample point
	float values[];

	public DescribePointBriefSO(BinaryCompareDefinition_I32 definition,
								BlurFilter<T> filterBlur,
								InterpolatePixelS<T> interp) {
		this.definition = definition;
		this.filterBlur = filterBlur;
		this.interp = interp;

		Class<T> imageType = filterBlur.getInputType().getImageClass();
		blur = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		values = new float[ definition.samplePoints.length ];
	}

	public TupleDesc_B createFeature() {
		return new TupleDesc_B(definition.getLength());
	}

	public void setImage(T image) {
		blur.reshape(image.width,image.height);
		filterBlur.process(image,blur);
		interp.setImage(blur);
	}

	public void process( float c_x , float c_y , float orientation , float radius , TupleDesc_B feature )
	{
		float scale = (float)(radius/BoofDefaults.BRIEF_SCALE_TO_RADIUS);
		// NOTE: This doesn't seem to take in account the interpolation border.  Might not work algs
		// other than bilinear interpolation
		boolean isInside = BoofMiscOps.checkInside(blur, c_x, c_y, definition.radius*scale);

		float c = (float)Math.cos(orientation);
		float s = (float)Math.sin(orientation);

		Arrays.fill(feature.data, 0);

		if( isInside ) {
			for( int i = 0; i < definition.samplePoints.length; i++ ) {
				Point2D_I32 a = definition.samplePoints[i];
				// rotate the points
				float x0 = c_x + (c*a.x - s*a.y)*scale;
				float y0 = c_y + (s*a.x + c*a.y)*scale;

				values[i] = interp.get_fast(x0, y0);
			}
		} else {
			// handle the image border case
			for( int i = 0; i < definition.samplePoints.length; i++ ) {
				Point2D_I32 a = definition.samplePoints[i];
				// rotate the points
				float x0 = c_x + (c*a.x - s*a.y)*scale;
				float y0 = c_y + (s*a.x + c*a.y)*scale;

				if( BoofMiscOps.checkInside(blur, x0, y0) ) {
					// it might be inside the image but too close to the border for unsafe
					values[i] = interp.get(x0,y0);
				}
			}
		}

		for( int i = 0; i < definition.compare.length; i++ ) {
			Point2D_I32 comp = definition.compare[i];

			if( values[comp.x] < values[comp.y] ) {
				feature.data[ i/32 ] |= 1 << (i % 32);
			}
		}
	}

	public BinaryCompareDefinition_I32 getDefinition() {
		return definition;
	}
}

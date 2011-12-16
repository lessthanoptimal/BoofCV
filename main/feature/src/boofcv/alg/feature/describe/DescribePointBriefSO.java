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
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_I32;

/**
 * <p>
 * Extension of {@link DescribePointBrief} which adds invariance to orientation and scale.  Invariance is added by simply
 * applying an orientation/scale transform to the sample points and then applying interpolation to the point at which
 * it has been sampled.
 * </p>
 *
 * @author Peter Abeles
 */
public class DescribePointBriefSO<T extends ImageSingleBand> {
	// describes the BRIEF feature
	protected BriefDefinition_I32 definition;

	// blurs the image prior to sampling
	protected BlurFilter<T> filterBlur;
	// blurred image
	protected T blur;

	// used to interpolate pixel value at rotated coordinate
	protected InterpolatePixel<T> interp;

	// values at each sample point
	float values[];

	public DescribePointBriefSO(BriefDefinition_I32 definition,
								BlurFilter<T> filterBlur,
								InterpolatePixel<T> interp) {
		this.definition = definition;
		this.filterBlur = filterBlur;
		this.interp = interp;

		blur = GeneralizedImageOps.createSingleBand(filterBlur.getInputType(), 1, 1);
		values = new float[ definition.samplePoints.length ];
	}

	public BriefFeature createFeature() {
		return new BriefFeature(definition.getLength());
	}

	public void setImage(T image) {
		blur.reshape(image.width,image.height);
		filterBlur.process(image,blur);
		interp.setImage(blur);
	}

	public boolean process( float c_x , float c_y , float orientation , float scale , BriefFeature feature )
	{
		int r = definition.radius;
		float c = (float)Math.cos(orientation);
		float s = (float)Math.sin(orientation);

		int pixelX = (int)c_x;
		int pixelY = (int)c_y;

		// todo modify to handle regions partially inside the image
		// make sure the region is inside the image
		if( !checkInBounds(pixelX,pixelY,-r,-r,c,s,scale))
			return false;
		else if( !checkInBounds(pixelX,pixelY,-r,r,c,s,scale))
			return false;
		else if( !checkInBounds(pixelX,pixelY,r,r,c,s,scale))
			return false;
		else if( !checkInBounds(pixelX,pixelY,r,-r,c,s,scale))
			return false;

		BoofMiscOps.zero(feature.data, feature.data.length);

		for( int i = 0; i < definition.samplePoints.length; i++ ) {
			Point2D_I32 a = definition.samplePoints[i];
			// rotate the points
			float x0 = c_x + (c*a.x - s*a.y)*scale;
			float y0 = c_y + (s*a.x + c*a.y)*scale;

			values[i] = interp.get_unsafe(x0,y0);
		}

		for( int i = 0; i < definition.compare.length; i++ ) {
			Point2D_I32 comp = definition.compare[i];

			if( values[comp.x] < values[comp.y] ) {
				feature.data[ i/32 ] |= 1 << (i % 32);
			}
		}

		return true;
	}

	private boolean checkInBounds( int c_x , int c_y , int dx , int dy , float c , float s , float scale )
	{
		float x = c_x + (c*dx - s*dy)*scale;
		float y = c_y + (s*dx + c*dy)*scale;

		return interp.isInSafeBounds((int) x, (int) y);
	}

	public BriefDefinition_I32 getDefinition() {
		return definition;
	}
}

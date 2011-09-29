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
import boofcv.alg.feature.describe.brief.BriefDefinition;
import boofcv.alg.feature.describe.brief.BriefFeature;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I32;

/**
 * @author Peter Abeles
 */
// todo add for other image types.
// todo create a new brief with scale + orientation? new class?  generic transform?
public class DescribePointBrief {
	// scribes the BRIEF feature
	BriefDefinition definition;
	// blurs the image prior to sampling
	BlurFilter filterBlur;
	// blurred image
	ImageFloat32 blur = new ImageFloat32(1,1);

	// precomputed offsets of sample points inside the image.
	int offsetsA[];
	int offsetsB[];

	public DescribePointBrief(BriefDefinition definition, BlurFilter filterBlur) {
		this.definition = definition;
		this.filterBlur = filterBlur;

		offsetsA = new int[ definition.getLength() ];
		offsetsB = new int[ definition.getLength() ];
	}

	public BriefFeature createFeature() {
		return new BriefFeature(definition.getLength());
	}

	public void setImage(ImageFloat32 image) {
		blur.reshape(image.width,image.height);
		filterBlur.process(image,blur);

		for( int i = 0; i < definition.setA.length ; i++ ) {
			Point2D_I32 a = definition.setA[i];
			Point2D_I32 b = definition.setB[i];

			offsetsA[i] = blur.startIndex + blur.stride*a.y + a.x;
			offsetsB[i] = blur.startIndex + blur.stride*b.y + b.x;
		}
	}

	public boolean process( int c_x , int c_y , BriefFeature feature )
	{
		if( !BoofMiscOps.checkInside(blur,c_x,c_y,definition.radius) )
			return false;

		BoofMiscOps.zero(feature.data,feature.data.length);

		int index = blur.startIndex + blur.stride*c_y + c_x;

		for( int i = 0; i < definition.setA.length; i++ ) {
			float valA = blur.data[index+offsetsA[i]];
			float valB = blur.data[index+offsetsB[i]];

			if( valA < valB ) {
				feature.data[ i/32 ] |= 1 << (i % 32);
			}
		}

		return true;
	}
}

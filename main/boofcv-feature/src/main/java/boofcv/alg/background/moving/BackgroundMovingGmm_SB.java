/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.background.moving;

import boofcv.struct.distort.Point2Transform2Model_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.InvertibleTransform;

/**
 * Implementation of {@link BackgroundMovingGmm} for {@link ImageGray}.
 *
 * @author Peter Abeles
 */
public class BackgroundMovingGmm_SB <T extends ImageGray<T>, Motion extends InvertibleTransform<Motion>>
	extends BackgroundMovingGmm<T,Motion>
{
	public BackgroundMovingGmm_SB(float learningPeriod, float decayCoef, int maxGaussians,
								  Point2Transform2Model_F32<Motion> transformImageType, ImageType<T> imageType)
	{
		super(learningPeriod, decayCoef, maxGaussians, transformImageType, imageType);
	}

	@Override
	public void initialize(int backgroundWidth, int backgroundHeight, Motion homeToWorld) {

	}

	@Override
	protected void updateBackground(int x0, int y0, int x1, int y1, T frame) {

	}

	@Override
	protected void _segment(Motion currentToWorld, T frame, GrayU8 segmented) {

	}
}

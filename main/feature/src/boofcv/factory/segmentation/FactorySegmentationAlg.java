/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.segmentation;

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.segmentation.SegmentMeanShiftColor;
import boofcv.alg.segmentation.SegmentMeanShiftGray;
import boofcv.alg.weights.WeightDistance_F32;
import boofcv.alg.weights.WeightPixel_F32;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageMultiBand;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;

/**
 * @author Peter Abeles
 */
public class FactorySegmentationAlg {

	public static<T extends ImageSingleBand> SegmentMeanShiftGray<T>
	meanShiftGray( int maxIterations, float convergenceTol,
				   WeightPixel_F32 weightSpacial,
				   WeightDistance_F32 weightGray, Class<T> imageType ) {

		InterpolatePixelS<T> interp = FactoryInterpolation.bilinearPixelS(imageType);

		return new SegmentMeanShiftGray<T>(maxIterations,convergenceTol,interp,weightSpacial,weightGray);

	}

	public static<T extends ImageMultiBand> SegmentMeanShiftColor<T>
	meanShiftColor( int maxIterations, float convergenceTol,
					WeightPixel_F32 weightSpacial,
					WeightDistance_F32 weightColor, ImageType<T> imageType ) {

		InterpolatePixelMB<T> interp = FactoryInterpolation.createPixelMB(0,255, TypeInterpolate.BILINEAR,imageType);

		return new SegmentMeanShiftColor<T>(maxIterations,convergenceTol,interp,weightSpacial,weightColor,imageType);

	}
}

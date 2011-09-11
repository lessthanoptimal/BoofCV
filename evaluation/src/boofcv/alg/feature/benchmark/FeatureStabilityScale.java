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

package boofcv.alg.feature.benchmark;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PixelTransformAffine;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.struct.image.ImageBase;
import georegression.struct.affine.Affine2D_F32;

import java.awt.image.BufferedImage;
import java.util.List;


/**
 * Checks for stability against changes in scale.  This is done by scaling images up and
 * down and testing to see if corresponding information is extracted at these different scales.
 *
 * @author Peter Abeles
 */
public class FeatureStabilityScale<T extends ImageBase>
		extends FeatureStabilityBase<T>
{
	// scale of the input image
	double scale[];
	// input image type
	Class<T> imageType;

	public FeatureStabilityScale( Class<T> imageType , double ... scale) {
		this.imageType = imageType;
		this.scale = scale.clone();
	}

	@Override
	public List<MetricResult> evaluate( BufferedImage original ,
									 StabilityAlgorithm alg ,
									 StabilityEvaluator<T> evaluator) {
		T image = ConvertBufferedImage.convertFrom(original,null,imageType);
		T adjusted = (T)image._createNew(image.width,image.height);

		evaluator.extractInitial(alg,image);

		List<MetricResult> results = createResultsStorage(evaluator, scale);

		for( int i = 0; i < scale.length; i++ ) {
			Affine2D_F32 initToImage = StabilityEvaluatorPoint.createScale((float)scale[i],image.width,image.height);
			Affine2D_F32 imageToInit = initToImage.invert(null);
			PixelTransformAffine affine = new PixelTransformAffine(imageToInit);
			ImageDistort<T> distorter = DistortSupport.createDistort(imageType,affine,TypeInterpolate.BILINEAR);

			distorter.apply(image,adjusted,125);

			double[]metrics = evaluator.evaluateImage(alg,adjusted, scale[i], 0);

			for( int j = 0; j < results.size(); j++ ) {
				results.get(j).observed[i] = metrics[j];
			}
		}

		return results;
	}


	@Override
	public int getNumberOfObservations() {
		return scale.length;
	}

}

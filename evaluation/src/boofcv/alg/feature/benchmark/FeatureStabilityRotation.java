/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package boofcv.alg.feature.benchmark;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PixelTransformAffine;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.struct.image.ImageBase;
import jgrl.struct.affine.Affine2D_F32;

import java.awt.image.BufferedImage;
import java.util.List;


/**
 * Checks for stability against rotation.  Test images are rotated about their centers
 * by the specified angles.
 *
 * @author Peter Abeles
 */
public class FeatureStabilityRotation<T extends ImageBase>
		extends FeatureStabilityBase<T>
{
	// rotation of the input image
	double angle[];
	// input image type
	Class<T> imageType;

	public FeatureStabilityRotation( Class<T> imageType , double ... angle) {
		this.imageType = imageType;
		this.angle = angle.clone();
	}

	@Override
	public List<MetricResult> evaluate( BufferedImage original ,
									 StabilityAlgorithm alg ,
									 StabilityEvaluator<T> evaluator) {
		T image = ConvertBufferedImage.convertFrom(original,null,imageType);
		T adjusted = (T)image._createNew(image.width,image.height);

		evaluator.extractInitial(alg,image);

		List<MetricResult> results = createResultsStorage(evaluator,angle);

		float centerX = image.width/2;
		float centerY = image.height/2;

		for( int i = 0; i < angle.length; i++ ) {
			float theta = (float)angle[i];
			PixelTransformAffine imageToInit = DistortSupport.transformRotate(centerX,centerY,theta);
			Affine2D_F32 initToImage = imageToInit.getModel().invert(null);
			ImageDistort<T> distorter = DistortSupport.createDistort(imageType,imageToInit,TypeInterpolate.BILINEAR);

			distorter.apply(image,adjusted,125);

			double[]metrics = evaluator.evaluateImage(alg,adjusted, initToImage);

			for( int j = 0; j < results.size(); j++ ) {
				results.get(j).observed[i] = metrics[j];
			}
		}

		return results;
	}

	@Override
	public int getNumberOfObservations() {
		return angle.length;
	}

}

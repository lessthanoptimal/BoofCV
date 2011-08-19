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

package gecv.alg.feature;

import gecv.core.image.ConvertBufferedImage;
import gecv.core.image.GeneralizedImageOps;
import gecv.struct.image.ImageBase;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;


/**
 * Checks for stability against adding independent noise to each pixel.  Gaussian noise is added.
 *
 * @author Peter Abeles
 */
public abstract class FeatureStabilityNoise<T extends ImageBase>
	extends FeatureStabilityBase<T> 
{
	// rand number generator used to add noise
	Random rand;
	// different levels of noise which are to be evaluated
	double noiseSigma[];
	// type of input image
	Class<T> imageType;

	public FeatureStabilityNoise( Class<T> imageType , long randSeed , double ...noiseSigma) {
		this.imageType = imageType;
		this.noiseSigma = noiseSigma.clone();
		this.rand = new Random(randSeed);
	}

	@Override
	public List<MetricResult> evaluate( BufferedImage original ,
									 StabilityAlgorithm alg ,
									 StabilityEvaluator<T> evaluator ) {
		T image = ConvertBufferedImage.convertFrom(original,null,imageType);
		T noisy = (T)image._createNew(image.width,image.height);

		evaluator.extractInitial(alg,image);

		List<MetricResult> results = createResultsStorage(evaluator,noiseSigma);

		for( int i = 0; i < noiseSigma.length; i++ ) {
			noisy.setTo(image);
			GeneralizedImageOps.addGaussian(noisy,rand,noiseSigma[i]);

			double[]metrics = evaluator.evaluateImage(alg,noisy, null);

			for( int j = 0; j < results.size(); j++ ) {
				results.get(j).observed[i] = metrics[j];
			}
		}

		return results;
	}


	@Override
	public int getNumberOfObservations() {
		return noiseSigma.length;
	}
}

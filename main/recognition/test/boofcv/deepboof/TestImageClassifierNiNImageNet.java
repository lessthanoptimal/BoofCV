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

package boofcv.deepboof;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;

/**
 * @author Peter Abeles
 */
public class TestImageClassifierNiNImageNet extends CheckBaseImageClassifier {

	int width = ImageClassifierNiNImageNet.imageCrop;
	int height = ImageClassifierNiNImageNet.imageCrop;

	@Override
	public Planar<GrayF32> createImage() {
		return new Planar<>(GrayF32.class,width,height,3);
	}

	@Override
	public BaseImageClassifier createClassifier() {
		ImageClassifierNiNImageNet nin = new ImageClassifierNiNImageNet();

		// dummy normalization
		nin.mean = new float[width*height];
		nin.stdev = new float[width*height];

		for (int i = 0; i < nin.mean.length; i++) {
			nin.mean[i] = rand.nextFloat()*30+110;
			nin.stdev[i] = 120;
		}

		return nin;
	}
}

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

import boofcv.alg.filter.stat.ImageLocalNormalization;
import boofcv.core.image.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import deepboof.models.YuvStatistics;

/**
 * @author Peter Abeles
 */
public class TestImageClassifierVggCifar10 extends CheckBaseImageClassifier {

	int width = ImageClassifierVggCifar10.inputSize;
	int height = width;


	@Override
	public Planar<GrayF32> createImage() {
		return new Planar<>(GrayF32.class,width,height,3);
	}

	@Override
	public BaseImageClassifier createClassifier() {
		ImageClassifierVggCifar10 alg = new ImageClassifierVggCifar10();

		alg.stats = new YuvStatistics();
		alg.stats.meanU = 120;
		alg.stats.stdevU = 25;
		alg.stats.meanV = 40;
		alg.stats.stdevV = 10;
		alg.stats.kernel = new double[]{0.1,0.5,0.1};
		alg.stats.kernelOffset = 1;

		alg.localNorm = new ImageLocalNormalization<>(GrayF32.class, BorderType.EXTENDED);
		alg.kernel = DataManipulationOps.create1D_F32(alg.stats.kernel);

		return alg;
	}
}

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

package boofcv.alg.feature.detect.template;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.template.FactoryTemplateMatching;
import boofcv.factory.feature.detect.template.TemplateScoreType;
import boofcv.misc.Performer;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkTemplateIntensity<T extends ImageGray> {

	Random rand = new Random(234);
	long TEST_TIME = 2000;

	int width = 320;
	int height = 240;

	Class<T> imageType;
	T image;
	T template;

	public BenchmarkTemplateIntensity(Class<T> imageType) {
		this.imageType = imageType;
		image = GeneralizedImageOps.createSingleBand(imageType,width,height);
		template = GeneralizedImageOps.createSingleBand(imageType,20,30);

		GImageMiscOps.fillUniform(image, rand, 0, 200);
		GImageMiscOps.fillUniform(template, rand, 0, 200);
	}

	public class TemplatePerformer implements Performer {

		TemplateMatchingIntensity<T> alg;
		String name;

		public TemplatePerformer(TemplateScoreType type) {
			this.alg = FactoryTemplateMatching.createIntensity(type,imageType);
			this.name = type.toString();
		}

		@Override
		public void process() {
			alg.process(image,template);
		}

		@Override
		public String getName() {
			return name;
		}
	}

	public void evaluateAll() {
		System.out.println("=========  Profile Image Size " + width + " x " + height + " ========== "+imageType.getSimpleName());
		System.out.println();

		ProfileOperation.printOpsPerSec(new TemplatePerformer(TemplateScoreType.SUM_DIFF_SQ), TEST_TIME);
		ProfileOperation.printOpsPerSec(new TemplatePerformer(TemplateScoreType.NCC), TEST_TIME);
	}

	public static void main( String args[] ) {
		BenchmarkTemplateIntensity<GrayU8>
				benchmark_U8 = new BenchmarkTemplateIntensity<>(GrayU8.class);

		benchmark_U8.evaluateAll();

		BenchmarkTemplateIntensity<GrayF32>
				benchmark_F32 = new BenchmarkTemplateIntensity<>(GrayF32.class);

		benchmark_F32.evaluateAll();
	}
}

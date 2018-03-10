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

package boofcv.alg.background;

import boofcv.factory.background.ConfigBackgroundBasic;
import boofcv.factory.background.ConfigBackgroundGaussian;
import boofcv.factory.background.ConfigBackgroundGmm;
import boofcv.factory.background.FactoryBackgroundModel;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Profiles the time to run each algorithm but ignores the time to load an image frame from the video
 *
 * @author Peter Abeles
 */
public class BenchmarkBackgroundStationary {

	File file;
	ImageType imageType;

	public BenchmarkBackgroundStationary(File file, ImageType imageType) {
		this.file = file;
		this.imageType = imageType;
	}

	public void benchmark() {
		List<BackgroundBase> algs = new ArrayList<>();
		algs.add(new Basic());
		algs.add(new Gaussian());
		algs.add(new GMM());

		for( BackgroundBase b : algs ) {
			b.process();
			System.out.printf("%20s FPS = %.2f\n",b.getName(),b.getFPS());
		}
	}

	public class Basic extends BackgroundBase {

		@Override
		public BackgroundModelStationary create() {
			ConfigBackgroundBasic config = new ConfigBackgroundBasic(12);

			return FactoryBackgroundModel.stationaryBasic(config,imageType);
		}
	}

	public class Gaussian extends BackgroundBase {

		@Override
		public BackgroundModelStationary create() {
			ConfigBackgroundGaussian config = new ConfigBackgroundGaussian(12);

			return FactoryBackgroundModel.stationaryGaussian(config,imageType);
		}
	}

	public class GMM extends BackgroundBase {
		@Override
		public BackgroundModelStationary create() {
			ConfigBackgroundGmm config = new ConfigBackgroundGmm();

			return FactoryBackgroundModel.stationaryGmm(config,imageType);
		}
	}


	public abstract class BackgroundBase {
		SimpleImageSequence sequence;

		long totalTime;
		int frames;

		GrayU8 background = new GrayU8(1,1);

		public abstract BackgroundModelStationary create();

		public void process() {
			sequence = DefaultMediaManager.INSTANCE.openVideo(file.getAbsolutePath(),imageType);
			background.reshape(sequence.getNextWidth(),sequence.getNextHeight());

			totalTime = 0;
			frames = 0;
			BackgroundModelStationary model = create();

			while( sequence.hasNext() ) {
				ImageBase image = sequence.next();

				long time0 = System.nanoTime();
				model.updateBackground(image,background);
				long time1 = System.nanoTime();

				totalTime += (time1-time0);
				frames++;
			}
		}

		public double getFPS() {
			return frames/(totalTime*1e-9);
		}

		public String getName() {
			return getClass().getSimpleName();
		}
	}

	public static void main(String[] args) {
		File file = new File(UtilIO.pathExample("background/street_intersection.mp4"));

		List<ImageType> imageTypes = new ArrayList<>();
		imageTypes.add(ImageType.single(GrayU8.class));
		imageTypes.add(ImageType.il(3,InterleavedU8.class));
		imageTypes.add(ImageType.pl(3,GrayU8.class));
		imageTypes.add(ImageType.single(GrayF32.class));
		imageTypes.add(ImageType.il(3,InterleavedF32.class));
		imageTypes.add(ImageType.pl(3,GrayF32.class));


		for( ImageType type : imageTypes ) {
			System.out.println("Image Type: "+type.toString());
			BenchmarkBackgroundStationary b = new BenchmarkBackgroundStationary(file, type);
			b.benchmark();
		}
	}
}

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

package gecv.alg.detect.interest.benchmark;

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.abst.detect.interest.InterestPointDetector;
import gecv.core.image.ConvertBufferedImage;
import gecv.io.image.UtilImageIO;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class BenchmarkInterestSpeed<T extends ImageBase, D extends ImageBase> {
	static long TEST_TIME = 1000;

	BenchmarkInterestParameters<T,D> param;
	List<InterestMetrics<T>> algs;

	public BenchmarkInterestSpeed(BenchmarkInterestParameters<T,D> param,
								  List<InterestMetrics<T>> algs)
	{
		this.param = param;
		this.algs = algs;
	}

	public static class PerformTest<T extends ImageBase> extends PerformerBase {

		InterestPointDetector<T> detector;
		T input;

		public PerformTest(InterestPointDetector<T> detector, T input) {
			this.detector = detector;
			this.input = input;
		}

		@Override
		public void process() {
			detector.detect(input);
		}
	}

	public void performBenchmark( BufferedImage image) {

		T input = ConvertBufferedImage.convertFrom(image,null,param.imageType);

		for( InterestMetrics<T> m : algs ) {
			PerformTest<T> p = new PerformTest<T>(m.detector,input);

			double opsPerSec = ProfileOperation.profileOpsPerSec(p,TEST_TIME, true);
			System.out.printf("%15s  ops/sec %6.1f  ms = %5d\n",m.name,opsPerSec,(int)(1000/opsPerSec));
		}
	}

	public static void main( String args[] ) {
		// specify test images
		String imageNames[] = new String[]{"outdoors01.jpg","indoors01.jpg","scale/beach01.jpg","sunflowers.png"};

		BenchmarkInterestParameters<ImageFloat32,ImageFloat32> param =
				new BenchmarkInterestParameters<ImageFloat32,ImageFloat32>();
		param.imageType = ImageFloat32.class;
		param.derivType = ImageFloat32.class;


		// evaluate each image individually
		for( String s : imageNames ) {
			BufferedImage image = UtilImageIO.loadImage("evaluation/data/"+s);

			System.out.println("Processing: "+s);
			System.out.println("            "+image.getWidth()+" x "+image.getHeight());

			List<InterestMetrics<ImageFloat32>> algs = BenchmarkInterestStability_Noise.createAlgs(param);
			BenchmarkInterestSpeed<ImageFloat32,ImageFloat32> benchmark
					= new BenchmarkInterestSpeed<ImageFloat32,ImageFloat32>(param,algs);

			benchmark.performBenchmark(image);
		}

	}
}

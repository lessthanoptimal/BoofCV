/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.disparity;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.segmentation.cc.ConnectedNaiveSpeckleFiller_F32;
import boofcv.alg.segmentation.cc.ConnectedNaiveSpeckleFiller_Int;
import boofcv.alg.segmentation.cc.ConnectedTwoRowSpeckleFiller_F32;
import boofcv.alg.segmentation.cc.ConnectedTwoRowSpeckleFiller_U8;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.shapes.Rectangle2D_I32;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkSpeckleFilter {

	@Param({"800"})
	public int size;

	public double tolerance = 1.0;
	public int maximumArea = 1000;
	public double fillColor = 40.0;

	GrayF32 inputF32 = new GrayF32(1, 1);
	GrayF32 outputF32 = new GrayF32(1, 1);
	GrayU8 inputU8 = new GrayU8(1, 1);
	GrayU8 outputU8 = new GrayU8(1, 1);

	ConnectedTwoRowSpeckleFiller_F32 dualRow_F32 = new ConnectedTwoRowSpeckleFiller_F32();
	ConnectedNaiveSpeckleFiller_F32 naive_F32 = new ConnectedNaiveSpeckleFiller_F32();
	ConnectedTwoRowSpeckleFiller_U8 dualRow_U8 = new ConnectedTwoRowSpeckleFiller_U8();
	ConnectedNaiveSpeckleFiller_Int<GrayU8> naive_U8 = new ConnectedNaiveSpeckleFiller_Int<>(ImageType.SB_U8);

	@Setup public void setup() {
		Random rand = new Random(234);

		renderImage(rand, inputF32);
		renderImage(rand, inputU8);
	}

	/**
	 * This is intended to crudely simulate a disparity image. In a disparity images there are large patches with
	 * speckle noise that should be one region. The difference is speed between the two methods is about what
	 * is seen in real world cases. When it was mostly random the naive method was slightly faster.
	 */
	private void renderImage( Random rand, ImageGray<?> image ) {
		image.reshape(size,size);

		Rectangle2D_I32 rect = new Rectangle2D_I32();
		for (int i = 0; i < 20; i++) {
			rect.x0 = rand.nextInt(size-40);
			rect.y0 = rand.nextInt(size-40);

			rect.x1 = rect.x0 + 40+rand.nextInt(size/2);
			rect.y1 = rect.y0 + 40+rand.nextInt(size/2);

			rect.x1 = Math.min(rect.x1,size);
			rect.y1 = Math.min(rect.y1,size);

			double color = rand.nextDouble()*fillColor;

			GImageMiscOps.fillRectangle(image, color, rect.x0, rect.y0, rect.getWidth(), rect.getHeight());
		}

		GImageMiscOps.addGaussian(image, rand, 0.7, 0, 255);

		for (int i = 0; i < 1000; i++) {
			GeneralizedImageOps.set(image,rand.nextInt(size), rand.nextInt(size), fillColor);
		}
	}

	@Benchmark public void dualRow_F32() {
		// copy the image since it's modified
		outputF32.setTo(inputF32);
		dualRow_F32.process(outputF32, maximumArea, tolerance, fillColor);
	}

	@Benchmark public void naive_F32() {
		// copy the image since it's modified
		outputF32.setTo(inputF32);
		naive_F32.process(outputF32, maximumArea, tolerance, fillColor);
	}

	@Benchmark public void dualRow_U8() {
		// copy the image since it's modified
		outputU8.setTo(inputU8);
		dualRow_U8.process(outputU8, maximumArea, tolerance, fillColor);
	}

	@Benchmark public void naive_U8() {
		// copy the image since it's modified
		outputU8.setTo(inputU8);
		naive_U8.process(outputU8, maximumArea, tolerance, fillColor);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkSpeckleFilter.class.getSimpleName())
				.build();

		new Runner(opt).run();
	}
}

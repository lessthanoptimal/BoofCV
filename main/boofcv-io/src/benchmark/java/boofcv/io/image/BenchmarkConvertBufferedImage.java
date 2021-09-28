/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.io.image;

import boofcv.io.image.impl.ImplConvertRaster;
import boofcv.struct.image.GrayU8;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks related to converting to and from BufferedImage.
 *
 * @author Peter Abeles
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 2)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkConvertBufferedImage {
	@Param({"3BYTE_BGR", "INT_RGB", "BYTE_GRAY"})
	public String bufferedType;

	@Param({"1000"})
	public int size;

	static BufferedImage imgBuff;
	static GrayU8 grayU8;

	@Setup public void setup() {
		Random rand = new Random(342543);
		grayU8 = new GrayU8(size, size);

		// quick way to fill it
		for (int i = 0; i < grayU8.data.length; i++) {
			grayU8.data[i] = (byte)(i%256);
		}

		int type = switch(bufferedType) {
			case "3BYTE_BGR" -> BufferedImage.TYPE_3BYTE_BGR;
			case "INT_RGB" -> BufferedImage.TYPE_INT_RGB;
			case "BYTE_GRAY" -> BufferedImage.TYPE_BYTE_GRAY;
			default -> throw new RuntimeException("Unknown type");
		};
		imgBuff = new BufferedImage(size, size, type);

		// randomize it to prevent some pathological condition
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				imgBuff.setRGB(j, i, rand.nextInt());
			}
		}
	}

	@Benchmark public void convertFrom_U8(){ConvertBufferedImage.convertFrom(imgBuff, grayU8, true);}
	@Benchmark public void convertTo_U8(){ConvertBufferedImage.convertTo(grayU8, imgBuff, true);}
	@Benchmark public void FromInt8ToGenericBuff(){ImplConvertRaster.grayToBuffered(grayU8, imgBuff);}
//	@Benchmark public void ExtractImageInt8(){ConvertBufferedImage.extractGrayU8(imgBuff);}
//	@Benchmark public void ExtractBuffered(){ConvertBufferedImage.extractBuffered(imgInt8);}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkConvertBufferedImage.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}

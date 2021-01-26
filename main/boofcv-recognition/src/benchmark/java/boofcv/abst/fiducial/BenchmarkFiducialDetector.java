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

package boofcv.abst.fiducial;

import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkFiducialDetector<T extends ImageGray<T>> {

	Class<T> imageType = (Class)GrayU8.class;

	List<T> images = new ArrayList<>();

	int numIterations = 5;

	FiducialDetector<T> detector = FactoryFiducial.squareBinary(
			new ConfigFiducialBinary(0.2), ConfigThreshold.fixed(100) , imageType);

	@Setup public void setup() {
		String directory = UtilIO.pathExample("fiducial/binary");
		addImage(directory + "/image0000.jpg");
		addImage(directory + "/image0001.jpg");
		addImage(directory + "/image0002.jpg");
	}

	@Benchmark public void SquareBinary() {
		for (int i = 0; i < numIterations; i++) {
			for (int j = 0; j < images.size(); j++) {
				detector.detect(images.get(j));
			}
		}
	}

	public void addImage( String path ) {
		T image = (T)UtilImageIO.loadImage(path,detector.getInputType().getImageClass());
		if( image == null )
			throw new IllegalArgumentException("Can't find image "+path);
		images.add(image);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkFiducialDetector.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}

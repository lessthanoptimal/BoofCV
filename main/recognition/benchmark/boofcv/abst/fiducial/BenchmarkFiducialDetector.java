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

package boofcv.abst.fiducial;

import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class BenchmarkFiducialDetector<T extends ImageGray> {

	FiducialDetector<T> detector;
	List<T> images = new ArrayList<>();

	public BenchmarkFiducialDetector(FiducialDetector<T> detector) {
		this.detector = detector;
	}

	public void addImage( String path ) {
		T image = (T)UtilImageIO.loadImage(path,detector.getInputType().getImageClass());
		if( image == null )
			throw new IllegalArgumentException("Can't find image "+path);
		images.add(image);
	}

	public double benchmark( int numIterations ) {

		long before = System.nanoTime();
		for (int i = 0; i < numIterations; i++) {
			for (int j = 0; j < images.size(); j++) {
				detector.detect(images.get(j));
			}
		}
		long after = System.nanoTime();
		double seconds = (after-before)/1e9;
		return (numIterations*images.size())/seconds;
	}

	private static void perform(String directory, FiducialDetector detector) {
		CameraPinholeRadial intrinsic = CalibrationIO.load(new File(directory , "intrinsic.yaml"));

//		intrinsic.radial = null;
//		intrinsic.t1 = intrinsic.t2 = 0;

		BenchmarkFiducialDetector benchmark = new BenchmarkFiducialDetector(detector);
		benchmark.addImage(directory + "image0000.jpg");
		benchmark.addImage(directory + "image0001.jpg");
		benchmark.addImage(directory + "image0002.jpg");

		System.out.println("FPS = "+benchmark.benchmark(600));
	}

	public static void main(String[] args) {
		String directory = UtilIO.pathExample("fiducial/binary/");

		FiducialDetector detector = FactoryFiducial.squareBinary(
				new ConfigFiducialBinary(0.2), ConfigThreshold.fixed(100) , GrayU8.class);
		perform(directory, detector);

//		detector = FactoryFiducial.
//				squareBinaryRobust(new ConfigFiducialBinary(0.2), 6, GrayU8.class);
//		perform(directory, detector);
	}
}

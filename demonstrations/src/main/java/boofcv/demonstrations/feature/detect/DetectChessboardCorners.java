/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.feature.detect;

import boofcv.abst.filter.binary.BinaryContourFinderLinearExternal;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.feature.detect.intensity.GradientCornerIntensity;
import boofcv.alg.filter.binary.ContourPacked;
import boofcv.alg.misc.ImageStatistics;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPointAlg;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.struct.ConfigLength;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.geometry.UtilPoint2D_I32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

import java.util.List;

public class DetectChessboardCorners<T extends ImageGray<T>, D extends ImageGray<D>> {

	int radius = 2;
	int width = radius*2+1;

	GradientCornerIntensity<D> cornerIntensity;

	GrayF32 intensity = new GrayF32(1,1);
	GrayU8 binary = new GrayU8(1,1);
	InputToBinary<GrayF32> inputToBinary;

	BinaryContourFinderLinearExternal contourFinder = new BinaryContourFinderLinearExternal();

	FastQueue<Point2D_F64> corners = new FastQueue<>(Point2D_F64.class,true);
	FastQueue<Point2D_I32> contour = new FastQueue<>(Point2D_I32.class,true);


	public DetectChessboardCorners( Class<T>  inputType , Class<D> derivType ) {
		cornerIntensity =  FactoryIntensityPointAlg.shiTomasi(radius, false, derivType);

		inputToBinary = FactoryThresholdBinary.localMean(ConfigLength.fixed(10),0.8,false,GrayF32.class);
//		inputToBinary = FactoryThresholdBinary.blockOtsu(ConfigLength.fixed(20),0.5,false,true,false,0,GrayF32.class);

		contourFinder.setMaxContour((width+1)*4+4);
		contourFinder.setMinContour((width-1)*4-4);
		contourFinder.setConnectRule(ConnectRule.EIGHT);
	}

	public void process( T input , D derivX , D deriveY ) {
		cornerIntensity.process(derivX,deriveY,intensity);

		float threshold = ImageStatistics.max(intensity)*0.1f;
		int N = intensity.width*input.height;
		for (int i = 0; i < N; i++) {
			if( intensity.data[i] <= threshold ) {
				intensity.data[i] = 0f;
			}
		}

		inputToBinary.process(intensity,binary);

		contourFinder.process(binary);

		corners.reset();
		List<ContourPacked> packed = contourFinder.getContours();
		for (int i = 0; i < packed.size(); i++) {
			contourFinder.loadContour(i,contour);

			UtilPoint2D_I32.mean(contour.toList(),corners.grow());
		}
	}

	public GrayF32 getIntensity() {
		return intensity;
	}

	public GrayU8 getBinary() {
		return binary;
	}

	public FastQueue<Point2D_F64> getCorners() {
		return corners;
	}
}

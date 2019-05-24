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

package boofcv.abst.feature.detect.line;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.line.LineParametric2D_F32;

import java.util.List;

/**
 * Wrapper around {@link DetectEdgeLines} that allows it to be used by {@link DetectLine} interface
 *
 * @author Peter Abeles
 */
public class DetectBinaryLinesToLines<T extends ImageGray<T>>
	implements DetectLine<T>
{
	// line detector
	DetectLineHoughPolarBinary detector;

	// computes image gradient
	InputToBinary<T> binarization;

	GrayU8 binary = new GrayU8(1,1);

	public DetectBinaryLinesToLines(DetectLineHoughPolarBinary detector ,
									InputToBinary<T> binarization ) {
		this.detector = detector;
		this.binarization = binarization;
	}

	@Override
	public List<LineParametric2D_F32> detect(T input) {
		binary.reshape(input.width,input.height);
		binarization.process(input,binary);

		detector.detect(binary);

		return detector.getFoundLines();
	}

	public InputToBinary<T> getBinarization() {
		return binarization;
	}

	public GrayU8 getBinary() {
		return binary;
	}

	@Override
	public ImageType<T> getInputType() {
		return binarization.getInputType();
	}
}

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
import boofcv.alg.feature.detect.line.HoughTransformBinary;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.line.LineParametric2D_F32;

import java.util.List;

/**
 * Converts {@link HoughTransformBinary} into {@link DetectLine}
 *
 * @see HoughTransformBinary
 * @see boofcv.alg.feature.detect.line.HoughParametersPolar
 *
 * @author Peter Abeles
 */
public class HoughBinary_to_DetectLine<T extends ImageGray<T>> implements DetectLine<T> {
	HoughTransformBinary hough;
	InputToBinary<T> thresholder;
	GrayU8 binary = new GrayU8(1,1);

	public HoughBinary_to_DetectLine(HoughTransformBinary hough, InputToBinary<T> thresholder ) {
		this.hough = hough;
		this.thresholder = thresholder;
	}

	@Override
	public List<LineParametric2D_F32> detect(T input) {
		thresholder.process(input,binary);
		hough.transform(binary);
		return hough.getLinesMerged();
	}

	@Override
	public ImageType<T> getInputType() {
		return thresholder.getInputType();
	}

	public HoughTransformBinary getHough() {
		return hough;
	}

	public InputToBinary<T> getThresholder() {
		return thresholder;
	}

	public GrayU8 getBinary() {
		return binary;
	}
}

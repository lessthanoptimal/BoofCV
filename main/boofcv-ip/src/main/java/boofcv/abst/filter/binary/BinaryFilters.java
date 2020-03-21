/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter.binary;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;

/**
 * Class for binary filters
 *
 * @author Peter Abeles
 */
public abstract class BinaryFilters implements FilterImageInterface<GrayU8,GrayU8> {
	// size of horizontal and vertical borders
	boolean outsideZero;

	// number of times the oeprator is applied
	int numTimes;

	ImageType<GrayU8> imageType = ImageType.single(GrayU8.class);

	public static class Invert extends BinaryFilters {
		@Override
		public void process(GrayU8 input, GrayU8 output) {
			BinaryImageOps.invert(input,output);
		}
	}

	public static class Erode4 extends BinaryFilters {
		public Erode4(int numTimes) {
			this.numTimes = numTimes;
		}

		@Override
		public void process(GrayU8 input, GrayU8 output) {
			BinaryImageOps.erode4(input,numTimes,output);
		}
	}

	public static class Dilate4 extends BinaryFilters {
		public Dilate4(int numTimes) {
			this.numTimes = numTimes;
		}

		@Override
		public void process(GrayU8 input, GrayU8 output) {
			BinaryImageOps.dilate4(input,numTimes,output);
		}
	}

	public static class Edge4 extends BinaryFilters {
		public Edge4(boolean outsideZero) {
			this.outsideZero = outsideZero;
		}

		@Override
		public void process(GrayU8 input, GrayU8 output) {
			BinaryImageOps.edge4(input,output,outsideZero);
		}
	}

	public static class Erode8 extends BinaryFilters {
		public Erode8(int numTimes) {
			this.numTimes = numTimes;
		}
		@Override
		public void process(GrayU8 input, GrayU8 output) {
			BinaryImageOps.erode8(input,numTimes,output);
		}
	}

	public static class Dilate8 extends BinaryFilters {
		public Dilate8(int numTimes) {
			this.numTimes = numTimes;
		}
		@Override
		public void process(GrayU8 input, GrayU8 output) {
			BinaryImageOps.dilate8(input,numTimes,output);
		}
	}

	public static class Edge8 extends BinaryFilters {
		public Edge8(boolean outsideZero) {
			this.outsideZero = outsideZero;
		}
		@Override
		public void process(GrayU8 input, GrayU8 output) {
			BinaryImageOps.edge8(input,output,outsideZero);
		}
	}

	public static class RemovePointNoise extends BinaryFilters {
		@Override
		public void process(GrayU8 input, GrayU8 output) {
			BinaryImageOps.removePointNoise(input,output);
		}
	}

	public static class Thin extends BinaryFilters {
		public Thin(int maxTimes) {
			this.numTimes = maxTimes;
		}
		@Override
		public void process(GrayU8 input, GrayU8 output) {
			BinaryImageOps.thin(input,numTimes,output);
		}
	}

	@Override
	public int getBorderX() {
		return 0;
	}

	@Override
	public int getBorderY() {
		return 0;
	}

	@Override
	public ImageType<GrayU8> getInputType() {
		return imageType;
	}

	@Override
	public ImageType<GrayU8> getOutputType() {
		return imageType;
	}

	public boolean isOutsideZero() {
		return outsideZero;
	}

	public void setOutsideZero(boolean outsideZero) {
		this.outsideZero = outsideZero;
	}

	public int getNumTimes() {
		return numTimes;
	}

	public void setNumTimes(int numTimes) {
		this.numTimes = numTimes;
	}
}

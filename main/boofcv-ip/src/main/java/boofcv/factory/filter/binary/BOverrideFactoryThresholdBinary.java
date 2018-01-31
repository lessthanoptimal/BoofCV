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

package boofcv.factory.filter.binary;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.override.BOverrideClass;
import boofcv.override.BOverrideManager;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.ImageGray;

/**
 * @author Peter Abeles
 */
public class BOverrideFactoryThresholdBinary extends BOverrideClass {

	public static GlobalEntropy globalEntropy;
	public static GlobalFixed globalFixed;
	public static GlobalOtsu globalOtsu;
	public static LocalGaussian localGaussian;
	public static LocalSauvola localSauvola;
	public static LocalMean localMean;
	public static LocalOtsu localOtsu;
	public static LocalBlockMinMax blockMinMax;
	public static LocalBlockMean blockMean;
	public static LocalBlockOtsu blockOtsu;

	static {
		BOverrideManager.register(BOverrideFactoryThresholdBinary.class);
	}

	public interface GlobalEntropy {
		<T extends ImageGray<T>>
		InputToBinary<T> handle(int minValue, int maxValue, boolean down, Class<T> inputType);
	}

	public interface GlobalFixed {
		<T extends ImageGray<T>>
		InputToBinary<T> handle(double threshold, boolean down, Class<T> inputType);
	}

	public interface GlobalOtsu {
		<T extends ImageGray<T>>
		InputToBinary<T> handle(double minValue, double maxValue, boolean down, Class<T> inputType);
	}

	public interface LocalGaussian {
		<T extends ImageGray<T>>
		InputToBinary<T> handle(ConfigLength regionWidth, double scale, boolean down, Class<T> inputType);
	}

	public interface LocalSauvola {
		<T extends ImageGray<T>>
		InputToBinary<T> handle(ConfigLength regionWidth, float k, boolean down, Class<T> inputType);
	}

	public interface LocalMean {
		<T extends ImageGray<T>>
		InputToBinary<T> handle(ConfigLength regionWidth, double scale, boolean down, Class<T> inputType);
	}

	public interface LocalBlockMinMax {
		<T extends ImageGray<T>>
		InputToBinary<T> handle(ConfigLength regionWidth, double scale , boolean down,
								double minimumSpread, boolean thresholdFromLocalBlocks, Class<T> inputType);
	}

	public interface LocalBlockMean {
		<T extends ImageGray<T>>
		InputToBinary<T> handle(ConfigLength regionWidth, double scale , boolean down, boolean thresholdFromLocalBlocks,
								Class<T> inputType);
	}

	public interface LocalBlockOtsu {
		<T extends ImageGray<T>>
		InputToBinary<T> handle(boolean otsu2, ConfigLength regionWidth , double tuning, double scale,
								boolean down, boolean thresholdFromLocalBlocks,  Class<T> inputType);
	}

	public interface LocalOtsu {
		<T extends ImageGray<T>>
		InputToBinary<T> handle(boolean otsu2, ConfigLength regionWidth , double tuning, double scale, boolean down, Class<T> inputType);
	}

}

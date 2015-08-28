/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.scene;

import java.io.Serializable;

/**
 * Histogram which represents the frequency of different types of words in a single image.
 * Typically used during scene classification.
 *
 * @author Peter Abeles
 */
public class HistogramScene implements Serializable {
	/**
	 * Normalized word frequency histogram.  Normalized so that it sums up to 1.
	 */
	public double histogram[];
	/**
	 * The type of scene the image was representative of
	 */
	public int type;

	public HistogramScene(int numWords) {
		histogram = new double[numWords];
	}

	public HistogramScene() {
	}

	public void setHistogram( double original[] ) {
		System.arraycopy(original,0,histogram,0,histogram.length);
	}

	public double[] getHistogram() {
		return histogram;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
}

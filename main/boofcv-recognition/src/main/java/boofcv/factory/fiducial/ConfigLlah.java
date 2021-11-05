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

package boofcv.factory.fiducial;

import boofcv.struct.Configuration;

/**
 * Configuration for Locally Likely Arrangement Hashing (LLAH). Default values are taken from affine(8,7) parameters
 * presented in the paper [1].
 *
 * <ol>
 *     <li>Nakai, Tomohiro, Koichi Kise, and Masakazu Iwamura.
 *     "Use of affine invariants in locally likely arrangement hashing for camera-based document image retrieval."
 *     International Workshop on Document Analysis Systems. Springer, Berlin, Heidelberg, 2006.</li>
 * </ol>
 *
 * @author Peter Abeles
 * @see boofcv.alg.feature.describe.llah.LlahOperations
 */
public class ConfigLlah implements Configuration {
	/** Number of nearest neighbors it will search for */
	public int numberOfNeighborsN = 8;

	/** Size of combination set from the set of neighbors */
	public int sizeOfCombinationM = 7;

	/** Level of quantization of the invariant */
	public int quantizationK = 7;

	/** Size of the hash table */
	public int hashTableSize = (int)1.28e8;

	/** Type of invariant used to compute the hash code */
	public HashType hashType = HashType.AFFINE;

	public ConfigLlah() {}

	public ConfigLlah setTo( ConfigLlah src ) {
		this.numberOfNeighborsN = src.numberOfNeighborsN;
		this.sizeOfCombinationM = src.sizeOfCombinationM;
		this.quantizationK = src.quantizationK;
		this.hashTableSize = src.hashTableSize;
		this.hashType = src.hashType;
		return this;
	}

	@Override
	public void checkValidity() {
		if (numberOfNeighborsN <= 0)
			throw new IllegalArgumentException("Must specify numberOfNeighborsN");
		if (sizeOfCombinationM <= 0)
			throw new IllegalArgumentException("Must specify numberOfNeighborsN");
	}

	public enum HashType {
		AFFINE,
		CROSS_RATIO
	}
}

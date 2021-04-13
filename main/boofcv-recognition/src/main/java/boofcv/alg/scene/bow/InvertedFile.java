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

package boofcv.alg.scene.bow;

import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_I32;

/**
 * The inverted file is a list of images that were observed in a particular node. Images are
 * referenced by array index. This class extends DogArray_I32 to remove the need to store
 * an additional java object. might be pre-mature optimization.
 *
 * <p>
 * [1] Nister, David, and Henrik Stewenius. "Scalable recognition with a vocabulary tree."
 * 2006 IEEE Computer Society Conference on Computer Vision and Pattern Recognition (CVPR'06). Vol. 2. Ieee, 2006.
 * </p>
 */
public class InvertedFile extends DogArray_I32 {
	// The word weights. In this paper this is d[i] = m[i]*w[i], where w[i] is the weight
	// assigned to a node. m[i] is the number of occurrences of this word in this image
	// In the paper [1] they store m[i] and not d[i] in the inverted file.
	public final DogArray_F32 weights = new DogArray_F32();

	public InvertedFile() {
		super(1);
	}

	public void addImage( int index, float weight ) {
		add(index);
		weights.add(weight);
	}

	@Override
	public void reset() {
		super.reset();
		weights.reset();
	}
}

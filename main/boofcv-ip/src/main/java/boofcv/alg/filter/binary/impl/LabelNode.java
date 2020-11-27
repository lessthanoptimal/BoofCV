/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.binary.impl;

import org.ddogleg.struct.DogArray_I32;


/**
 * Used to keep track of connected nodes while labeling binary images.
 *
 * @author Peter Abeles
 */
public class LabelNode {

	public int index;
	public int maxIndex;
	public DogArray_I32 connections = new DogArray_I32(5);

	public LabelNode(int index) {
		this.index = index;
		this.maxIndex = index;
	}
}

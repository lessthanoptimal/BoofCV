/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.slam;

import boofcv.alg.structure.LookUpSimilarImages;

/**
 * Batch Simultaneous Location and Mapping (SLAM) system which assumed a known multi camera system is viewing the world.
 * A feature based approach is used were first salient image features are found then matched to each other using
 * descriptors.
 *
 * @author Peter Abeles
 */
public class BatchSlamMultiCameras {
	public void initialize( MultiCameraSystem sensor ) {

	}

	public void process( LookUpSimilarImages similarImages ) {

	}
}

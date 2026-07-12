/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.flow;

import boofcv.alg.flow.HornSchunck;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedF32;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of {@link DenseOpticalFlow} for {@link boofcv.alg.flow.HornSchunck}.
 *
 * @author Peter Abeles
 */
public class HornSchunck_to_DenseOpticalFlow<T extends ImageBase<T>, D extends ImageBase<D>>
		implements DenseOpticalFlow<T> {
	HornSchunck<T, D> hornSchunck;

	ImageType<T> imageType;

	public HornSchunck_to_DenseOpticalFlow( HornSchunck<T, D> hornSchunck,
											ImageType<T> imageType ) {
		this.hornSchunck = hornSchunck;
		this.imageType = imageType;
	}

	@Override
	public void process( T source, T destination, InterleavedF32 flow ) {
		flow.reshape(source.width, source.height, 2);
		hornSchunck.process(source, destination, flow);
	}

	@Override
	public @Nullable InterleavedF32 getAttributes() {
		return null;
	}

	@Override
	public ImageType<T> getInputType() {
		return imageType;
	}
}

/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.io.ffmpeg;

import boofcv.io.image.SimpleImageSequence;
import boofcv.io.video.VideoInterface;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

public class FfmpegVideo implements VideoInterface {
	@Override
	public <T extends ImageBase<T>> SimpleImageSequence<T>
	load( String fileName, ImageType<T> imageType ) {
		return new FfmpegVideoImageSequence<>(fileName, imageType);
	}
}

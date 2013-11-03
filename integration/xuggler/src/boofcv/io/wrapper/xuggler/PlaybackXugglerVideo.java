/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.io.wrapper.xuggler;

import boofcv.gui.image.PlaybackImageSequence;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;

/**
 * Displays a video after reading it in using xuggler.
 *
 * @author Peter Abeles
 */
public class PlaybackXugglerVideo {

	public static void main(String args[]) {
		String fileName;

		if (args.length == 0) {
			fileName = "../data/applet/vo/backyard/left.mjpeg";
		} else {
			fileName = args[0];
		}

		XugglerSimplified<ImageUInt8> sequence = new XugglerSimplified<ImageUInt8>(fileName, ImageType.single(ImageUInt8.class));

		PlaybackImageSequence<ImageUInt8> player = new PlaybackImageSequence<ImageUInt8>(sequence);

		player.process();

		System.out.println("Finished");
	}
}

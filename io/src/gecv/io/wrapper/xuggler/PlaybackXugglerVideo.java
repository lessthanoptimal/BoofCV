/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.io.wrapper.xuggler;

import gecv.io.image.PlaybackImageSequence;
import gecv.struct.image.ImageUInt8;

/**
 * Displays a video after reading it in using xuggler.
 *
 * @author Peter Abeles
 */
public class PlaybackXugglerVideo {

	public static void main(String args[]) {
		String fileName;

		if (args.length == 0) {
			fileName = "/home/pja/uav_video.avi";
		} else {
			fileName = args[0];
		}

		XugglerSimplified<ImageUInt8> sequence = new XugglerSimplified<ImageUInt8>(fileName, ImageUInt8.class);

		PlaybackImageSequence<ImageUInt8> player = new PlaybackImageSequence<ImageUInt8>(sequence);

		player.process();

		System.out.println("Finished");
	}
}

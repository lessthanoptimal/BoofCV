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

package gecv.struct.image;

/**
 * @author Peter Abeles
 */
public class TestImageInterleavedInt8 extends StandardImageInterleavedTests {


	@Override
	public ImageInterleaved createImage(int width, int height, int numBands) {
		return new ImageInterleavedInt8(width, height, numBands);
	}

	@Override
	public Number randomNumber() {
		return (byte) (rand.nextInt(255) - 126);
	}
}

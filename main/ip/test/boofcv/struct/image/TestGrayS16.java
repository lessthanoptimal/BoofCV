/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.image;

/**
 * @author Peter Abeles
 */
public class TestGrayS16 extends StandardImageIntegerTests<GrayS16> {

	public TestGrayS16() {
		super(true);
	}

	@Override
	public GrayS16 createImage(int width, int height) {
		return new GrayS16(width, height);
	}

	@Override
	public GrayS16 createImage() {
		return new GrayS16();
	}

	@Override
	public Number randomNumber() {
		return (short) (rand.nextInt(Short.MAX_VALUE - Short.MIN_VALUE) - Short.MIN_VALUE);
	}
}

/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.segmentation;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public abstract class GeneralImageSegmentationChecks<T extends ImageBase> {

	ImageType imageTypes[];

	public GeneralImageSegmentationChecks(ImageType ...types ) {
		this.imageTypes = types;
	}

	public abstract ImageSegmentation<T> createAlg( ImageType<T> imageType );

	/**
	 * Make sure subimages produce the same results
	 */
	@Test
	public void subimage() {
		fail("Implement");
	}

	/**
	 * Makes sure the segments are sequential
	 */
	@Test
	public void sequentialNumbers() {
		fail("Implement");
	}

	/**
	 * Makes sure the number of regions returned is correct
	 */
	@Test
	public void regionCounts() {
		fail("Implement");
	}

	/**
	 * Produces the same results when run multiple times
	 */
	@Test
	public void multipleCalls() {
		fail("Implement");
	}

	/**
	 * Won't blow up if input image size is changed
	 */
	@Test
	public void changeInImageSize() {
		fail("Implement");
	}

}

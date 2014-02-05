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

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GeneralImageSegmentationChecks<T extends ImageBase> {

	ImageType imageTypes[];

	Random rand = new Random(234);

	int width = 20;
	int height = 30;

	public GeneralImageSegmentationChecks(ImageType ...types ) {
		this.imageTypes = types;
	}

	public abstract ImageSegmentation<T> createAlg( ImageType<T> imageType );

	/**
	 * Make sure subimages produce the same results
	 */
	@Test
	public void subimage() {
		for( ImageType<T> t : imageTypes ) {
//			System.out.println("Image type "+t);

			ImageSegmentation<T> alg = createAlg(t);

			T input = t.createImage(width,height);
			ImageSInt32 expected = new ImageSInt32(width,height);

			GImageMiscOps.fillUniform(input,rand,0,100);

			alg.segment(input,expected);

			// provide an output which is a sub-image
			ImageSInt32 found = new ImageSInt32(width+3,height+2).subimage(2,1,width+2,height+1,null);
			alg.segment(input,found);
			BoofTesting.assertEquals(expected,found,0);

			// Now make the input image an output
			input = BoofTesting.createSubImageOf(input);
			found = new ImageSInt32(width,height);

			alg.segment(input,found);
			BoofTesting.assertEquals(expected,found,0);
		}
	}

	/**
	 * Makes sure that there really are regions 0 N-1 in the output image
	 */
	@Test
	public void sequentialNumbers() {
		for( ImageType<T> t : imageTypes ) {

			ImageSegmentation<T> alg = createAlg(t);

			T input = t.createImage(width,height);
			ImageSInt32 output = new ImageSInt32(width,height);
			GImageMiscOps.fillUniform(input,rand,0,100);

			alg.segment(input,output);

			int N = alg.getTotalSegments();
			assertTrue(N > 2);

			boolean found[] = new boolean[N];

			for( int y = 0; y < height; y++ ) {
				for( int x = 0; x < width; x++ ) {
					found[ output.get(x,y) ] = true;
				}
			}

			for( int i = 0; i < N; i++ ) {
				assertTrue(found[i]);
			}
		}
	}

	/**


	/**
	 * Produces the same results when run multiple times
	 */
	@Test
	public void multipleCalls() {
		for( ImageType<T> t : imageTypes ) {

			ImageSegmentation<T> alg = createAlg(t);

			T input = t.createImage(width,height);
			ImageSInt32 output = new ImageSInt32(width,height);
			GImageMiscOps.fillUniform(input,rand,0,100);

			alg.segment(input,output);

			ImageSInt32 output2 = new ImageSInt32(width,height);
			alg.segment(input,output2);

			for( int y = 0; y < height; y++ ) {
				for( int x = 0; x < width; x++ ) {
					assertEquals(output.get(x,y),output2.get(x,y));
				}
			}
		}
	}

	/**
	 * See if it won't blow up if input image size is changed
	 */
	@Test
	public void changeInImageSize() {
		for( ImageType<T> t : imageTypes ) {

			ImageSegmentation<T> alg = createAlg(t);

			T input = t.createImage(width/2,height/2);
			ImageSInt32 output = new ImageSInt32(width/2,height/2);
			GImageMiscOps.fillUniform(input,rand,0,100);

			alg.segment(input,output);

			input = t.createImage(width,height);
			output = new ImageSInt32(width,height);

			alg.segment(input,output);
		}
	}

}

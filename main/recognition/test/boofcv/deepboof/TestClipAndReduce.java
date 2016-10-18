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

package boofcv.deepboof;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestClipAndReduce {

	Random rand = new Random(234);
	ImageType types[] = new ImageType[]{ImageType.single(GrayF32.class),ImageType.pl(3,GrayF32.class)};

	@Test
	public void massage_clipped() {
		for( ImageType type : types ) {
			massage_written_to(true,type);
			massage_distorted(true,type);
		}
	}

	@Test
	public void massage_scaled() {
		for( ImageType type : types ) {
			massage_written_to(false,type);
			massage_distorted(false,type);
		}
	}

	private void massage_written_to( boolean clipped , ImageType type ) {
		ImageBase input = type.createImage(40,30);
		GImageMiscOps.fillUniform(input,rand,-1,1);

		ImageBase output = type.createImage(20,20);

		ClipAndReduce alg = new ClipAndReduce(clipped,type);

		alg.massage(input,output);

		// make sure every pixel has been written to
		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				double value = GeneralizedImageOps.get(output,x,y,0);

				assertTrue( value != 0 );
			}
		}
	}

	/**
	 * fill the center of the image with a single color.  If clipped correctly the output should be that color
	 * entirely.  If not only the center
	 */
	private void massage_distorted( boolean clipped , ImageType type ) {
		ImageBase input = type.createImage(40,30);
		GImageMiscOps.fill(input.subimage(5,0,35,30),255);

		ImageBase output = type.createImage(20,20);

		ClipAndReduce alg = new ClipAndReduce(clipped,type);

		alg.massage(input,output);

		if( clipped ) {
			assertEquals(255, GeneralizedImageOps.get(output,0,10,0), 1e-4);
			assertEquals(255, GeneralizedImageOps.get(output,10,10,0), 1e-4);
			assertEquals(255, GeneralizedImageOps.get(output,19,10,0), 1e-4);
		} else {
			assertEquals(0, GeneralizedImageOps.get(output,0,10,0), 2);
			assertEquals(255, GeneralizedImageOps.get(output,10,10,0), 1e-4);
			assertEquals(0, GeneralizedImageOps.get(output,19,10,0), 2);
		}
	}

	@Test
	public void clipInput() {
		for( ImageType type : types ) {
			clipInput(type,30,40,50,30);
			clipInput(type,50,30,30,40);
		}
	}

	public void clipInput( ImageType type , int inW , int inH , int outW , int outH ) {
		ImageBase input = type.createImage(inW,inH);
		GImageMiscOps.fillUniform(input,rand,-1,1);

		ClipAndReduce alg = new ClipAndReduce(true,type);

		ImageBase output =  type.createImage(outW,outH);
		GImageMiscOps.fillUniform(output,rand,-1,1);

		ImageBase found = alg.clipInput(input,output);

		double foundAspect = found.width/(double)found.height;
		double expectedAspect = output.width/(double)output.height;

		// won't be perfect
		assertEquals(expectedAspect,foundAspect,0.05);
	}
}

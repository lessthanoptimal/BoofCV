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

package boofcv.core.image.border;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.FactoryGImageSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * Common tests for implementers of {@link ImageBorder}.
 *
 * @author Peter Abeles
 */
public abstract class GenericImageBorderTests {

	Random rand = new Random(234);

	int width = 20;
	int height = 25;

	public abstract ImageBorder_I32 wrap( ImageUInt8 image );

	public abstract ImageBorder_F32 wrap( ImageFloat32 image );

	public abstract Number get( GImageSingleBand img , int x , int y );

	/**
	 * The border was set to the specified value.  See if it had the expected affect
	 */
	public abstract void checkBorderSet( int x , int y , Number val ,
										 GImageSingleBand border,
										 GImageSingleBand orig );

	@Test
	public void get_I8() {
		ImageUInt8 imgA = new ImageUInt8(width,height);
		ImageMiscOps.fillUniform(imgA,rand, 0, 100);

		ImageBorder_I32 fooA = wrap(imgA);

		GImageSingleBand orig = FactoryGImageSingleBand.wrap(imgA);
		GImageSingleBand border = FactoryGImageSingleBand.wrap(fooA);

		checkGet(orig, border);
	}

	@Test
	public void get_F32() {
		ImageFloat32 imgA = new ImageFloat32(width,height);
		ImageMiscOps.fillUniform(imgA,rand,0,5);

		ImageBorder_F32 fooA = wrap(imgA);

		GImageSingleBand orig = FactoryGImageSingleBand.wrap(imgA);
		GImageSingleBand border = FactoryGImageSingleBand.wrap(fooA);

		checkGet(orig, border);
	}

	private void checkGet(GImageSingleBand orig, GImageSingleBand border) {
		// test the image's inside where there is no border condition
		assertEquals(orig.get(1,1),border.get(1,1));
		assertEquals(orig.get(0,0),border.get(0,0));
		assertEquals(orig.get(width-1,height-1),border.get(width-1,height-1));

		// test border conditions
		checkBorder(-1,0,border,orig);
		checkBorder(-2,0,border,orig);
		checkBorder(0,-1,border,orig);
		checkBorder(0,-2,border,orig);

		checkBorder(width,height-1,border,orig);
		checkBorder(width+1,height-1,border,orig);
		checkBorder(width-1,height,border,orig);
		checkBorder(width-1,height+1,border,orig);
	}

	private void checkBorder( int x , int y , GImageSingleBand border , GImageSingleBand orig ) {
		assertEquals(get(orig,x,y).floatValue(),border.get(x,y).floatValue(),1e-4f);
	}

	@Test
	public void set_I8() {
		ImageUInt8 imgA = new ImageUInt8(width,height);
		ImageMiscOps.fillUniform(imgA,rand, 0, 100);

		ImageBorder_I32 fooA = wrap(imgA);

		GImageSingleBand orig = FactoryGImageSingleBand.wrap(imgA);
		GImageSingleBand border = FactoryGImageSingleBand.wrap(fooA);

		checkSet(orig, border);
	}

	@Test
	public void set_F32() {
		ImageFloat32 imgA = new ImageFloat32(width,height);
		ImageMiscOps.fillUniform(imgA,rand,0,5);

		ImageBorder_F32 fooA = wrap(imgA);

		GImageSingleBand orig = FactoryGImageSingleBand.wrap(imgA);
		GImageSingleBand border = FactoryGImageSingleBand.wrap(fooA);

		checkSet(orig, border);
	}

	private void checkSet(GImageSingleBand orig, GImageSingleBand border) {
		// test the image's inside where there is no border condition
		border.set(0,0,1);
		border.set(width-1,height-1,2);
		assertEquals(1.0f,orig.get(0,0).floatValue(),1e-4f);
		assertEquals(2.0f,orig.get(width-1,height-1).floatValue(),1e-4f);

		// test border conditions
		border.set(-1,0,2);
		checkBorderSet(-1,0,2,border,orig);
		border.set(-2,0,3);
		checkBorderSet(-2,0,3,border,orig);

		border.set(0,-1,4);
		checkBorderSet(0,-1,4,border,orig);
		border.set(0,-2,5);
		checkBorderSet(0,-2,5,border,orig);

		border.set(width,height-1,2);
		checkBorderSet(width,height-1,2,border,orig);
		border.set(width+1,height-1,3);
		checkBorderSet(width+1,height-1,3,border,orig);

		border.set(width-1,height,4);
		checkBorderSet(width-1,height,4,border,orig);
		border.set(width-1,height+1,5);
		checkBorderSet(width-1,height+1,5,border,orig);
	}
}

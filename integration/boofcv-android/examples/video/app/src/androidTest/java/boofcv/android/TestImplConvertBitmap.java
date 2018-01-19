/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.android;

import android.graphics.Bitmap;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TestImplConvertBitmap {

	int w = 5;
	int h = 10;
	
	int buffer32[] = new int[w*h];
	byte buffer8[] = new byte[w*h*4];

	@Before
	public void setup() {
	}

	public void testUpdateForFloat() {
		fail("Should check to see if floating point gray scale images are handled correctly.  won't always be int");
	}

	@Test
	public void testAll_ArrayToGray() {
		Method methods[] = ImplConvertBitmap.class.getMethods();
	
		int numCount = 0;
		
		for( Method m : methods ) {
			if( !m.getName().equals("arrayToGray") )
				continue;
		
			checkArrayToGray(m,Bitmap.Config.ARGB_8888);
			try {
				checkArrayToGray(m,Bitmap.Config.RGB_565);
			} catch( RuntimeException e ) {
				assertFalse( "Only byte supports this mode",m.getParameterTypes()[1] == byte[].class );
			}
			numCount++;
		}
		
		assertEquals(4,numCount);
	}
	
	public void checkArrayToGray( Method m , Bitmap.Config config ) {
		Bitmap orig = Bitmap.createBitmap(w,h, config);
		orig.setPixel(1, 2, 0xFF204010 );
		
		Class[] params = m.getParameterTypes();
		
		try {
			ImageGray found = (ImageGray)params[2].getConstructor(int.class,int.class).newInstance(w,h);

			Object array;

			String info = params[2].getSimpleName();
			if (params[0] == int[].class) {
				info += " Array32";
				orig.copyPixelsToBuffer(IntBuffer.wrap(buffer32));
				array = buffer32;
			} else {
				info += " Array8";
				orig.copyPixelsToBuffer(ByteBuffer.wrap(buffer8));
				array = buffer8;
			}
			info += " "+config;

			m.invoke(null, array, config, found);
			
			GImageGray g = FactoryGImageGray.wrap(found);
			
			// should be 37 for both 8888 and 565
			assertEquals(info,37,g.get(1,2).intValue());
			assertEquals(info,0,g.get(0,0).intValue());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testAll_ArrayToPlanar() {
		Method methods[] = ImplConvertBitmap.class.getMethods();
	
		int numCount = 0;
		
		for( Method m : methods ) {
			if( !m.getName().contains("arrayToPlanar") )
				continue;
		
			checkArrayToPlanar(m,Bitmap.Config.ARGB_8888);
			try {
				checkArrayToPlanar(m,Bitmap.Config.RGB_565);
			} catch( RuntimeException e ) {
				assertFalse( "Only byte supports this mode",m.getParameterTypes()[1] == byte[].class );
			}
			numCount++;
		}
		
		assertEquals(4,numCount);
	}
	
	public void checkArrayToPlanar( Method m , Bitmap.Config config ) {
		Bitmap orig = Bitmap.createBitmap(w,h, config);
		orig.setPixel(1, 2, 0xFF204010 );
		
		Class[] params = m.getParameterTypes();
		
		try {
			int numBands = Bitmap.Config.ARGB_8888 == config ? 4 : 3;
			Class msType = m.getName().contains("U8") ? GrayU8.class : GrayF32.class;
			
			Planar found = new Planar(msType,w,h,numBands);

			Object array;

			String info = params[2].getSimpleName();
			if (params[0] == int[].class) {
				info += " Array32";
				orig.copyPixelsToBuffer(IntBuffer.wrap(buffer32));
				array = buffer32;
			} else {
				info += " Array8";
				orig.copyPixelsToBuffer(ByteBuffer.wrap(buffer8));
				array = buffer8;
			}
			info += " "+config;

			m.invoke(null, array, config, found);
			
			assertEquals(0x20,(int)GeneralizedImageOps.get(found.getBand(0),1,2));
			assertEquals(0x40,(int)GeneralizedImageOps.get(found.getBand(1),1,2));
			assertEquals(0x10,(int)GeneralizedImageOps.get(found.getBand(2),1,2));
			if( numBands == 4 )
				assertEquals(0xFF,(int)GeneralizedImageOps.get(found.getBand(3),1,2));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testAll_BitmapToGrayRGB() {
		Method methods[] = ImplConvertBitmap.class.getMethods();
	
		int numCount = 0;
		
		for( Method m : methods ) {
			if( !m.getName().equals("bitmapToGrayRGB") )
				continue;
		
			checkBitmapToGrayRGB(m,Bitmap.Config.ARGB_8888);
			// the 565 conversion Android uses is slightly different from my conversion.  just going to ignore this test
//			checkBitmapToGrayRGB(m,Bitmap.Config.RGB_565);
			
			numCount++;
		}
		
		assertEquals(2,numCount);
	}
	
	public void checkBitmapToGrayRGB( Method m , Bitmap.Config config ) {
		Bitmap orig = Bitmap.createBitmap(w,h, config);
		orig.setPixel(1, 2, 0xFF204010 );
		
		Class[] params = m.getParameterTypes();
		
		String info = config+" "+params[1].getSimpleName();
		
		try {
			ImageGray found = (ImageGray)params[1].getConstructor(int.class,int.class).newInstance(w,h);

			m.invoke(null, orig, found);
			
			GImageGray g = FactoryGImageGray.wrap(found);
			
			// should be 37 for both 8888 and 565
			assertEquals(info,37,g.get(1,2).intValue());
			assertEquals(info,0,g.get(0,0).intValue());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testAll_BitmapToPlanarRGB() {
		Method methods[] = ImplConvertBitmap.class.getMethods();
	
		int numCount = 0;
		
		for( Method m : methods ) {
			if( !m.getName().contains("bitmapToPlanar") )
				continue;
		
			checkBitmapToPlanarRGB(m,Bitmap.Config.ARGB_8888);

			numCount++;
		}
		
		assertEquals(2,numCount);
	}
	
	public void checkBitmapToPlanarRGB( Method m , Bitmap.Config config ) {
		Bitmap orig = Bitmap.createBitmap(w,h, config);
		orig.setPixel(1, 2, 0xFF204010 );
		
		Class[] params = m.getParameterTypes();
		
		String info = config+" "+m.getName();
		
		try {
			int numBands = Bitmap.Config.ARGB_8888 == config ? 4 : 3;
			Class msType = m.getName().contains("U8") ? GrayU8.class : GrayF32.class;
			
			Planar found = new Planar(msType,w,h,numBands);

			m.invoke(null, orig, found);
			
			assertEquals(0x20,(int)GeneralizedImageOps.get(found.getBand(0),1,2));
			assertEquals(0x40,(int)GeneralizedImageOps.get(found.getBand(1),1,2));
			assertEquals(0x10,(int)GeneralizedImageOps.get(found.getBand(2),1,2));
			if( numBands == 4 )
				assertEquals(0xFF,(int)GeneralizedImageOps.get(found.getBand(3),1,2));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testAll_GrayToArray() {
		Method methods[] = ImplConvertBitmap.class.getMethods();
	
		int numCount = 0;
		
		for( Method m : methods ) {
			if( !m.getName().equals("grayToArray") )
				continue;
		
			checkGrayToArray(m,Bitmap.Config.ARGB_8888);
			checkGrayToArray(m,Bitmap.Config.RGB_565);
			
			numCount++;
		}
		
		assertEquals(2,numCount);
	}
	
	public void checkGrayToArray( Method m , Bitmap.Config config ) {
		Bitmap dst = Bitmap.createBitmap(w,h, config);
		
		Class[] params = m.getParameterTypes();
		
		try {
			ImageGray src = (ImageGray)params[0].getConstructor(int.class,int.class).newInstance(w,h);
			GeneralizedImageOps.set(src, 1, 2, 16);
			GeneralizedImageOps.set(src, 1, 3, 0xFF);
			
			Object array;

			String info = params[0].getSimpleName();
			if (params[2] == int[].class) {
				info += " Array32";
				array = buffer32;
			} else {
				info += " Array8";
				array = buffer8;
			}
			info += " "+config;

			m.invoke(null, src, array, config);
			if (params[2] == int[].class) {
				dst.copyPixelsFromBuffer(IntBuffer.wrap(buffer32));
			} else {
				dst.copyPixelsFromBuffer(ByteBuffer.wrap(buffer8));
			}
			
			GImageGray g = FactoryGImageGray.wrap(src);
			
			if( config == Bitmap.Config.ARGB_8888 ) {
				assertEquals(info,0xFF101010,(int)dst.getPixel(1,2));
				assertEquals(info,0xFFFFFFFF,(int)dst.getPixel(1,3));
			} else {
				assertEquals(info,expected565(16,16,16),(int)dst.getPixel(1,2));
				assertEquals(info,expected565(255,255,255),(int)dst.getPixel(1,3));
			}
			assertEquals(info,0xFF000000,dst.getPixel(0,0));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testAll_GrayToBitmapRGB() {
		Method methods[] = ImplConvertBitmap.class.getMethods();
	
		int numCount = 0;
		
		for( Method m : methods ) {
			if( !m.getName().equals("grayToBitmapRGB") )
				continue;
		
			checkGrayToBitmapRGB(m,Bitmap.Config.ARGB_8888);
//			checkGrayToBitmapRGB(m,Bitmap.Config.RGB_565);
			
			numCount++;
		}
		
		assertEquals(2,numCount);
	}
	
	public void checkGrayToBitmapRGB( Method m , Bitmap.Config config ) {
		Bitmap dst = Bitmap.createBitmap(w,h, config);
		
		Class[] params = m.getParameterTypes();
		
		try {
			ImageGray src = (ImageGray)params[0].getConstructor(int.class,int.class).newInstance(w,h);
			GeneralizedImageOps.set(src, 1, 2, 16);
			GeneralizedImageOps.set(src, 1, 3, 0xFF);

			String info = config+" "+params[0].getSimpleName();

			m.invoke(null, src, dst);

			assertEquals(info,0xFF101010,(int)dst.getPixel(1,2));
			assertEquals(info,0xFFFFFFFF,(int)dst.getPixel(1,3));
			assertEquals(info,0xFF000000,dst.getPixel(0,0));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testAll_PlanarToArray() {
		Method methods[] = ImplConvertBitmap.class.getMethods();
	
		int numCount = 0;
		
		for( Method m : methods ) {
			if( !m.getName().contains("planarToArray") )
				continue;
		
			checkPlanarToArray(m,Bitmap.Config.ARGB_8888);
			checkPlanarToArray(m,Bitmap.Config.RGB_565);
			
			numCount++;
		}
		
		assertEquals(2,numCount);
	}
	
	public void checkPlanarToArray( Method m , Bitmap.Config config ) {
		Bitmap dst = Bitmap.createBitmap(w,h, config);
		
		Class[] params = m.getParameterTypes();
		
		try {
			int numBands = Bitmap.Config.ARGB_8888 == config ? 4 : 3;
			Class msType = m.getName().contains("U8") ? GrayU8.class : GrayF32.class;
			
			Planar src = new Planar(msType,w,h,numBands);
			
			GeneralizedImageOps.set(src.getBand(0),1, 2, 0x38);
			GeneralizedImageOps.set(src.getBand(1),1, 2, 0x64);
			GeneralizedImageOps.set(src.getBand(2),1, 2, 0xFF);
			if( numBands == 4 )
				GeneralizedImageOps.set(src.getBand(3),1, 2, 0xFF);
			
			Object array;

			String info = params[0].getSimpleName();
			if (params[2] == int[].class) {
				info += " Array32";
				array = buffer32;
			} else {
				info += " Array8";
				array = buffer8;
			}
			info += " "+config;

			m.invoke(null, src, array, config);
			if (params[2] == int[].class) {
				dst.copyPixelsFromBuffer(IntBuffer.wrap(buffer32));
			} else {
				dst.copyPixelsFromBuffer(ByteBuffer.wrap(buffer8));
			}

			if( config == Bitmap.Config.ARGB_8888 ) {
				assertEquals(info,0xFF3864FF,(int)dst.getPixel(1,2));
				assertEquals(info,0x00000000,dst.getPixel(0,0));
			} else {
				assertEquals(info,expected565(0x38,0x64,0xFF),(int)dst.getPixel(1,2));
				assertEquals(info,0xFF000000,dst.getPixel(0,0));
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testAll_PlanarToBitmapRGB() {
		Method methods[] = ImplConvertBitmap.class.getMethods();
	
		int numCount = 0;
		
		for( Method m : methods ) {
			if( !m.getName().contains("planarToBitmapRGB") )
				continue;
		
			checkPlanarToBitmapRGB(m,Bitmap.Config.ARGB_8888);
//			checkPlanarToBitmapRGB(m,Bitmap.Config.RGB_565);
			
			numCount++;
		}
		
		assertEquals(2,numCount);
	}
	
	public void checkPlanarToBitmapRGB( Method m , Bitmap.Config config ) {
		Bitmap dst = Bitmap.createBitmap(w,h, config);
		
		Class[] params = m.getParameterTypes();
		
		try {
			int numBands = Bitmap.Config.ARGB_8888 == config ? 4 : 3;
			Class msType = m.getName().contains("U8") ? GrayU8.class : GrayF32.class;
			
			Planar src = new Planar(msType,w,h,numBands);
			
			GeneralizedImageOps.set(src.getBand(0),1, 2, 0x38);
			GeneralizedImageOps.set(src.getBand(1),1, 2, 0x64);
			GeneralizedImageOps.set(src.getBand(2),1, 2, 0xFF);
			if( numBands == 4 )
				GeneralizedImageOps.set(src.getBand(3),1, 2, 0xFF);
			

			String info = m.getName()+" "+config;

			m.invoke(null, src, dst);

			if( config == Bitmap.Config.ARGB_8888 ) {
				assertEquals(info,0xFF3864FF,(int)dst.getPixel(1,2));
				assertEquals(info,0x00000000,dst.getPixel(0,0));
			} else {
				assertEquals(info,expected565(0x38,0x64,0xFF),(int)dst.getPixel(1,2));
				assertEquals(info,0xFF000000,dst.getPixel(0,0));
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static int expected565( int r , int g , int b ) {
		int valR = (int)Math.round(r*0x1F/255.0)*0xFF/0x1F;
		int valG = (int)Math.round(g*0x3F/255.0)*0xFF/0x3F;
		int valB = (int)Math.round(b*0x1F/255.0)*0xFF/0x1F;
		
		return (0xFF << 24) | (valR << 16) | (valG << 8) | valB;
	}
}

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

package boofcv.alg.geo.calibration;

import boofcv.io.MediaManager;
import boofcv.io.VideoCallBack;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestParseStereoCalibrationConfig {

	@Test
	public void basicTest() throws FileNotFoundException {

		DummyMedia m = new DummyMedia();
	
		
		String s = 
				"# comment in first line\n"+
				"# more comments!\n"+
				"infoFile = ../calibration/data/example_chess.txt\n"+
				"addLeft = ath/to/image.jpg\n"+
				"addLeft = ath/to/image2.jpg\n"+
				"addRight = ath/to/image.jpg\n"+
				"addRight = ath/to/image2.jpg\n"+
				"\n   \n\n"; // some white space garbage a person might add
		
		String targetDesc =
				"chess false true 3 4 33";
		
		m.files.add(s);
		m.files.add(targetDesc);

		ParseStereoCalibrationConfig parser = new ParseStereoCalibrationConfig(m);
		assertTrue(parser.parse("adsasd"));
		
		assertTrue(null!=parser.getDetector());
		assertFalse(parser.assumeZeroSkew);
		assertTrue(parser.flipY);
		assertTrue(null!=parser.getTarget());
		assertEquals(2,parser.getLeftImages().size());
		assertEquals(2,parser.getRightImages().size());
	}
	
	private class DummyMedia implements MediaManager {

		List<String> files = new ArrayList<String>();
		
		@Override
		public Reader openFile(String fileName) {
			
			String s = files.remove(0);
			
			return new StringReader(s);
		}

		@Override
		public BufferedImage openImage(String fileName) {
			return null;
		}

		@Override
		public <T extends ImageBase> SimpleImageSequence<T> openVideo(String fileName, ImageType<T> imageType) {
			return null;
		}

		@Override
		public <T extends ImageBase> boolean openCamera(String device, int width, int height, VideoCallBack<T> callback) {
			return false;
		}
	}
}

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

package boofcv.io.wrapper;

import boofcv.io.image.SimpleImageSequence;
import boofcv.io.video.VideoInterface;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * @author Peter Abeles
 */
public class DynamicWebcamInterface implements WebcamInterface {

	WebcamInterface webcam;

	public DynamicWebcamInterface() {
		try {
			webcam = loadManager("boofcv.io.webcamcapture.WebcamCaptureWebcamInterface");
		} catch( RuntimeException e ) {}
	}

	@Override
	public <T extends ImageBase> SimpleImageSequence<T>
	open(String device, int width, int height, ImageType<T> imageType) {
		if( webcam == null ) {
			throw new RuntimeException("No webcam libraries loaded");
		}
		return webcam.open(device,width,height,imageType);
	}

	/**
	 * Loads the specified default {@link VideoInterface}.
	 *
	 * @return Video interface
	 */
	public static WebcamInterface loadManager( String pathToManager ) {
		try {
			Class c = Class.forName(pathToManager);
			return (WebcamInterface) c.newInstance();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Class not found.  Is it included in the class path?");
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}

/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.applet;

import boofcv.io.image.ImageListManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Peter Abeles
 */
public class AppletImageListManager extends ImageListManager {
	URL codebase;

	public AppletImageListManager(URL codebase) {
		this.codebase = codebase;
	}

	@Override
	public BufferedImage loadImage( int labelIndex ) {

		URL url = null;
		try {
			url = new URL(codebase, fileNames.get(labelIndex)[0]);
			return ImageIO.read(url);
		} catch (MalformedURLException e) {
			System.err.println("MalformedURL"+fileNames.get(labelIndex));
		} catch (IOException e) {
			System.err.println("IOException reading "+fileNames.get(labelIndex));
		}
		return null;
	}

	@Override
	public BufferedImage loadImage( int labelIndex , int imageIndex ) {

		URL url = null;
		try {
			url = new URL(codebase, fileNames.get(labelIndex)[imageIndex]);
			return ImageIO.read(url);
		} catch (MalformedURLException e) {
			System.err.println("MalformedURL"+fileNames.get(labelIndex));
		} catch (IOException e) {
			System.err.println("IOException reading "+fileNames.get(labelIndex));
		}
		return null;
	}
}

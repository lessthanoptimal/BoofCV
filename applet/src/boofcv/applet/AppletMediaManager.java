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

import boofcv.io.MediaManager;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageBase;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Peter Abeles
 */
public class AppletMediaManager implements MediaManager {
	URL codebase;

	String homeDirectory = "";
	
	// TODO REMOVE THIS MASSIVE HACK
	boolean first = true;
	
	public AppletMediaManager(URL codebase) {
		this.codebase = codebase;
	}

	public void setHomeDirectory(String homeDirectory) {
		this.homeDirectory = homeDirectory;
	}

	@Override
	public Reader openFile(String fileName) {
		try {
			if( !first ) {
				fileName = homeDirectory + fileName;
			} else {
				first = false;
			}

			return new InputStreamReader(new URL(codebase,fileName).openStream());
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public BufferedImage openImage(String fileName) {
		URL url = null;
		try {
			if( !first ) {
				fileName = homeDirectory + fileName;
			} else {
				first = false;
			}

			url = new URL(codebase, fileName);
			return ImageIO.read(url);
		} catch (MalformedURLException e) {
			System.err.println("MalformedURL"+fileName);
		} catch (IOException e) {
			System.err.println("IOException reading "+fileName);
		}
		return null;
	}

	@Override
	public <T extends ImageBase> SimpleImageSequence<T> openVideo(String fileName, Class<T> imageType) {
		throw new RuntimeException("NOpe not implemented yet");
	}
}

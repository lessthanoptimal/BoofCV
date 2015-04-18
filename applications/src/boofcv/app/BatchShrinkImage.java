/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.app;

import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class BatchShrinkImage {
	public static void main(String[] args) {

		String startDirectory = "/home/pja/Desktop/scene";

		MultiSpectral<ImageUInt8> inputColor3 = new MultiSpectral<ImageUInt8>(ImageUInt8.class,1,1,3);
		MultiSpectral<ImageUInt8> outputColor3 = new MultiSpectral<ImageUInt8>(ImageUInt8.class,1,1,3);

		List<File> openDirectories = new ArrayList<File>();

		File current = new File(startDirectory);
		if( !current.isDirectory() )
			throw new IllegalArgumentException("Start point must be a directory");

		while( true ) {
			File[] files = current.listFiles();

			for( File f : files ) {
				if( f.isDirectory() ) {
					System.out.println("directory = "+f.getName());
				} else {
					System.out.println("file = "+f.getName());
				}
			}
		}
	}
}

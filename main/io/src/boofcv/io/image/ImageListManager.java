/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.io.image;

import boofcv.io.InputListManager;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * List of preselected images and their labels.
 *
 * @author Peter Abeles
 */
public class ImageListManager implements InputListManager {
	protected List<String> imageLabels = new ArrayList<String>();
	protected List<String[]> fileNames = new ArrayList<String[]>();

	public void add( String label , String ...names ) {
		imageLabels.add(label);
		fileNames.add(names.clone());
	}

	public int size() {
		return imageLabels.size();
	}

	public List<String> getLabels() {
		return imageLabels;
	}

	public String getLabel( int index ) {
		return imageLabels.get(index);
	}

	public BufferedImage loadImage( int index ) {
		BufferedImage image = UtilImageIO.loadImage(fileNames.get(index)[0]);
		if( image == null ) {
			System.err.println("Can't load image "+fileNames.get(index)[0]);
		}
		return image;
	}

	public BufferedImage loadImage( int labelIndex , int imageIndex ) {
		BufferedImage image = UtilImageIO.loadImage(fileNames.get(labelIndex)[imageIndex]);
		if( image == null ) {
			System.err.println("Can't load image "+fileNames.get(labelIndex)[0]);
		}
		return image;
	}
}

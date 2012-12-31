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

package boofcv.evaluation;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageSingleBand;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Returns a sequence of images from files.
 *
 * @author Peter Abeles
 */
public class FileImageSequence<T extends ImageSingleBand> implements EvaluationImageSequence<T> {

	List<String> fileNames = new ArrayList<String>();
	int index = 0;

	BufferedImage image;
	String name;
	String prefix="";
	T output;
	Class<T> imageType;

	public FileImageSequence( Class<T> imageType , String... names) {
		for (String s : names) {
			fileNames.add(s);
		}
		output = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		this.imageType = imageType;
	}

	public FileImageSequence() {
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public boolean next() {
		if (index < fileNames.size()) {
			name = fileNames.get(index++);
			image = UtilImageIO.loadImage(prefix+name);
			if (image == null)
				throw new RuntimeException("Couldn't open " + (prefix+name));
			return true;
		} else
			return false;
	}

	@Override
	public T getImage() {
		output.reshape(image.getWidth(), image.getHeight());
		return ConvertBufferedImage.convertFromSingle(image, output, imageType);
	}

	@Override
	public String getName() {
		return name;
	}
}

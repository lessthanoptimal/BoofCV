/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.evaluation;

import gecv.core.image.ConvertBufferedImage;
import gecv.core.image.ConvertImage;
import gecv.io.image.UtilImageIO;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Returns a sequence of images from files.
 *
 * @author Peter Abeles
 */
public class FileImageSequence implements EvaluationImageSequence {

	List<String> fileNames = new ArrayList<String>();
	int index = 0;

	BufferedImage image;
	String name;

	public FileImageSequence(String... names) {
		for (String s : names) {
			fileNames.add(s);
		}
	}

	public FileImageSequence() {
	}

	@Override
	public boolean next() {
		if (index < fileNames.size()) {
			name = fileNames.get(index++);
			image = UtilImageIO.loadImage(name);
			if (image == null)
				throw new RuntimeException("Couldn't open " + name);
			return true;
		} else
			return false;
	}

	@Override
	public ImageUInt8 getImage_I8() {
		ImageUInt8 img8 = new ImageUInt8(image.getWidth(), image.getHeight());
		ConvertBufferedImage.convertFrom(image, img8);
		return img8;
	}

	@Override
	public ImageFloat32 getImage_F32() {
		ImageUInt8 img8 = new ImageUInt8(image.getWidth(), image.getHeight());
		ImageFloat32 imgF32 = new ImageFloat32(image.getWidth(), image.getHeight());
		ConvertBufferedImage.convertFrom(image, img8);
		ConvertImage.convert(img8, imgF32);
		return imgF32;
	}

	@Override
	public String getName() {
		return name;
	}
}

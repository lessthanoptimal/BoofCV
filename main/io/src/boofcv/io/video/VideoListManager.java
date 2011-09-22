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

package boofcv.io.video;

import boofcv.io.InputListManager;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.images.JpegByteImageSequence;
import boofcv.struct.image.ImageBase;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * List of preselected videos and their labels.
 *
 * @author Peter Abeles
 */
public class VideoListManager<T extends ImageBase> implements InputListManager {
	protected List<String> labels = new ArrayList<String>();
	protected List<String> videoType = new ArrayList<String>();
	protected List<String[]> fileNames = new ArrayList<String[]>();

	protected Class<T> imageType;

	public VideoListManager(Class<T> imageType) {
		this.imageType = imageType;
	}

	public void add(String label, String type, String... names) {
		labels.add(label);
		videoType.add(type);
		fileNames.add(names.clone());
	}

	public int size() {
		return labels.size();
	}

	public List<String> getLabels() {
		return labels;
	}

	public String getLabel( int index ) {
		return labels.get(index);
	}

	public SimpleImageSequence<T> loadSequence( int index ) {
		return loadSequence(index,0);
	}

	public SimpleImageSequence<T> loadSequence( int labelIndex , int imageIndex ) {
		String type = videoType.get(labelIndex);
		if( type == null || type.compareToIgnoreCase("video") == 0 ) {
			return BuboVideoManager.loadManagerDefault().load(fileNames.get(labelIndex)[imageIndex], imageType);
		} else if( type.compareToIgnoreCase("JPEG_ZIP") == 0 ) {
			try {
				VideoJpegZipCodec codec = new VideoJpegZipCodec();
				List<byte[]> data = codec.read(new FileInputStream(fileNames.get(labelIndex)[imageIndex]));
				return new JpegByteImageSequence<T>(imageType,data,true);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		} else {
			throw new IllegalArgumentException("Unknown video type: "+type);
		}
//		File directory = new File(fileNames.get(labelIndex)[imageIndex]);
//		return new BufferedFileImageSequence<T>(imageType,directory,"jpg");
	}
}

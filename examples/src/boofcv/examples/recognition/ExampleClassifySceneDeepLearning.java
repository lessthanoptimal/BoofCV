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

package boofcv.examples.recognition;

import boofcv.abst.scene.SceneClassifier;
import boofcv.deepboof.SceneClassifierVggLike;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import deepboof.io.DatabaseOps;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * // TODO comment
 *
 * @author Peter Abeles
 */
public class ExampleClassifySceneDeepLearning {

	public static File downloadModel( File path ) {
		if( !path.isDirectory() )
			path = path.getParentFile();

		File pathToModel = new File(path,"likevgg_cifar10");
		if( !path.exists() ) {
			if (!path.mkdirs())
				throw new IllegalArgumentException("Failed to make path");
		} else {

			// check to see if the data already exists.  If so just return
			if( new File(pathToModel,"YuvStatistics.txt").exists() &&
					new File(pathToModel,"model.net").exists() )
				return pathToModel;

			// TODO check md5sum
		}

		System.out.println("Obtaining network model.  size = 125 MB");
		DatabaseOps.download("http://heanet.dl.sourceforge.net/project/deepboof/networks/v1/likevgg_cifar10.zip",path);
		DatabaseOps.decompressZip(new File(path,"likevgg_cifar10.zip"),path,true);



		return pathToModel;
	}

	public static void main(String[] args) throws IOException {
		File modelHome = downloadModel( new File("download_data") );

		SceneClassifier<Planar<GrayF32>> classifier = new SceneClassifierVggLike();
		classifier.loadModel(modelHome);

		// TODO get test images
		String images[] = new String[]{"horse6.jpg","airplane.jpg"};

		BufferedImage buffered = UtilImageIO.loadImage(images[1]);
		if( buffered == null)
			throw new RuntimeException("Couldn't find input image");

		Planar<GrayF32> image = new Planar<>(GrayF32.class,buffered.getWidth(), buffered.getHeight(), 3);
		ConvertBufferedImage.convertFromMulti(buffered,image,true,GrayF32.class);

		classifier.classify(image);

		List<String> categories = classifier.getCategories();
		System.out.println();
		System.out.println("Selected "+categories.get( classifier.getBestResult()));
		System.out.println();

		for( SceneClassifier.Score score : classifier.getAllResults() ) {
			System.out.printf("%20s  %f\n",categories.get(score.category),score.score);
		}
	}
}

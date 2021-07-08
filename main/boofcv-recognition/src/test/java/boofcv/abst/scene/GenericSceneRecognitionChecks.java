/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.scene;

import boofcv.abst.scene.SceneRecognition.Match;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 **/
public abstract class GenericSceneRecognitionChecks<T extends ImageBase<T>> extends BoofStandardJUnit {

	int width = 640;
	int height = 480;

	List<T> images = new ArrayList<>();
	protected ImageType<T> imageType;

	/**
	 * Creates an instance of the algorithm being tested
	 */
	protected abstract SceneRecognition<T> createAlg();

	protected GenericSceneRecognitionChecks( ImageType<T> imageType ) {
		this.imageType = imageType;
	}

	private void createImages() {
		T image = imageType.createImage(width, height);
		GImageMiscOps.fillUniform(image, rand, 0, 200);

		// Add several images which should be distinctive
		images.add(image.clone());
		T modified = image.createSameShape();
		GBlurImageOps.gaussian(image, modified, 0, 4, null);
		images.add(modified.clone());
		GImageMiscOps.fillRectangle(image, 100, 20, 20, 200, 190);
		images.add(image.clone());
		GImageMiscOps.fillRectangle(image, 50, 240, 20, 200, 190);
		images.add(image.clone());
		GImageMiscOps.addUniform(image, rand, -20, 20);
		images.add(image.clone());
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				GImageMiscOps.fillRectangle(image, y*5, 0, y, width, 1);
			}
		}
		GImageMiscOps.addUniform(image, rand, -30, 30);
		images.add(image.clone());
	}

	/**
	 * Give it images with no features. It shouldn't blow up.
	 */
	@Test void imageWithNoFeatures_learn() {
		for (int i = 0; i < 10; i++) {
			images.add(imageType.createImage(width, height));
		}

		SceneRecognition<T> alg = createAlg();
		alg.learnModel(images.iterator());
	}

	/**
	 * Give it images to learn, then see if it can look up the originals.
	 *
	 * Also tests clearDatabase(). We do that here since a model is required.
	 */
	@Test void learn_then_select() {
		createImages();

		int maxMatches = 5; // maximum allowed matches
		SceneRecognition<T> alg = createAlg();

		// Learn a descriptor
		alg.learnModel(images.iterator());

		// Pass in the images
		for (int i = 0; i < images.size(); i++) {
			alg.addImage("" + i, images.get(i));
		}
		assertEquals(images.size(), alg.getImageIds(null).size());

		// Look up the images and see if the first result is the original
		DogArray<Match> matches = new DogArray<>(Match::new);
		for (int i = 0; i < images.size(); i++) {
			assertTrue(alg.query(images.get(i), ( id ) -> true, maxMatches, matches));
			assertEquals(i, Integer.parseInt(matches.get(0).id));
		}

		// now clear the database
		alg.clearDatabase();
		assertEquals(0, alg.getImageIds(null).size());
		for (int i = 0; i < images.size(); i++) {
			assertFalse(alg.query(images.get(i), ( id ) -> true, maxMatches, matches));
			assertEquals(0, matches.size);
		}

		// Add the images
		for (int i = 0; i < images.size(); i++) {
			alg.addImage("" + i, images.get(i));
		}

		// It should be able to find them again
		for (int i = 0; i < images.size(); i++) {
			assertTrue(alg.query(images.get(i), ( id ) -> true, maxMatches, matches));
			assertEquals(i, Integer.parseInt(matches.get(0).id));
			assertTrue(matches.size <= maxMatches);
		}
	}
}

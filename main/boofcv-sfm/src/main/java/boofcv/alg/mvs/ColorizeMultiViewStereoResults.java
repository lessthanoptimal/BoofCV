/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.mvs;

import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.distort.brown.LensDistortionBrown;
import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.alg.mvs.MultiViewStereoFromKnownSceneStructure.ViewInfo;
import boofcv.core.image.LookUpColorRgb;
import boofcv.misc.BoofLambdas;
import boofcv.misc.LookUpImages;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.PointIndex4D_F64;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;

import java.util.ArrayList;
import java.util.List;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * Helper class which handles all the data structure manipulations for extracting RGB color values from a point
 * cloud computed by {@link MultiViewStereoFromKnownSceneStructure}. Color information is extracted only
 * using the first view a point was seen inside of.
 *
 * @author Peter Abeles
 */
public class ColorizeMultiViewStereoResults<T extends ImageBase<T>> {

	/** Extracts RGB color information from an image for a given point cloud */
	@Getter final ColorizeCloudFromImage<T> colorizer;
	/** Loads an image given a string id */
	@Getter final LookUpImages lookupImages;

	// Storage for loaded image
	final T image;

	// Storage for coordinate systems transforms
	Se3_F64 world_to_view = new Se3_F64();
	Se3_F64 tmp = new Se3_F64();

	// Camera model. Don't use bundle adjustment camera model directly since some models include image center
	// and other don't, which can cause problems
	CameraPinholeBrown intrinsic = new CameraPinholeBrown();

	/**
	 * Specifies configurations
	 *
	 * @param colorLookup Convert pixels into RGB color
	 * @param lookupImages Load images given a string iD
	 */
	public ColorizeMultiViewStereoResults( LookUpColorRgb<T> colorLookup, LookUpImages lookupImages ) {
		this.colorizer = new ColorizeCloudFromImage<>(colorLookup);
		this.lookupImages = lookupImages;
		this.image = colorizer.getColorLookup().getImageType().createImage(1, 1);
	}

	/**
	 * Extracts color information for the point cloud on a view by view basis.
	 *
	 * @param scene (Input) Geometric description of the scene
	 * @param mvs (Input) Contains the 3D point cloud
	 * @param indexColor (Output) RGB values are passed through to this function.
	 */
	public void processMvsCloud( SceneStructureMetric scene,
								 MultiViewStereoFromKnownSceneStructure<?> mvs,
								 BoofLambdas.IndexRgbConsumer indexColor ) {

		// Get a list of views that were used as "centers"
		List<ViewInfo> centers = mvs.getListCenters();

		// Get the point cloud
		DogArray<Point3D_F64> cloud = mvs.getDisparityCloud().getCloud();

		// Step through each "center" view
		for (int centerIdx = 0; centerIdx < centers.size(); centerIdx++) {
			ViewInfo center = centers.get(centerIdx);

			if (!lookupImages.loadImage(center.relations.id, image))
				throw new RuntimeException("Couldn't find image: " + center.relations.id);

			// Which points came from this view/center
			int idx0 = mvs.getDisparityCloud().viewPointIdx.get(centerIdx);
			int idx1 = mvs.getDisparityCloud().viewPointIdx.get(centerIdx + 1);

			// Setup the camera projection model using bundle adjustment model directly
			BundleAdjustmentOps.convert(scene.getViewCamera(center.metric).model, image.width, image.height, intrinsic);
			Point2Transform2_F64 norm_to_pixel = new LensDistortionBrown(intrinsic).distort_F64(false, true);

			// Get the transform from world/cloud to this view
			scene.getWorldToView(center.metric, world_to_view, tmp);

			// Grab the colorized points from this view
			colorizer.process3(image, cloud.toList(), idx0, idx1, world_to_view, norm_to_pixel, indexColor);
		}
	}

	/**
	 * Looks up the colors for all the points in the scene by reprojecting them back onto their original images.
	 *
	 * @param scene (Input) Scene's structure
	 * @param indexToId (Input) Convert view index to view ID
	 * @param indexColor (Output) RGB values are passed through to this function.
	 */
	public void processScenePoints( SceneStructureMetric scene,
									BoofLambdas.IndexToString indexToId,
									BoofLambdas.IndexRgbConsumer indexColor ) {

		// Loading images is expensive so when we get the color of each pixel we want to process all features
		// inside the same image at once. Unfortunately there is no fast way to look up all features by image.
		// So a lookup table is constructed below
		List<DogArray_I32> lookupPointsByView = new ArrayList<>();
		for (int i = 0; i < scene.views.size; i++) {
			lookupPointsByView.add(new DogArray_I32());
		}
		// Add the first view each point was seen in to the list
		for (int pointIdx = 0; pointIdx < scene.points.size; pointIdx++) {
			SceneStructureCommon.Point p = scene.points.get(pointIdx);
			lookupPointsByView.get(p.views.get(0)).add(pointIdx);
		}

		// TODO in the future generalize this for 3D and 4D points
		var iterator = new ScenePointsSetIterator<>(new PointIndex4D_F64());
		var world_to_view = new Se3_F64();
		for (int viewIdx = 0; viewIdx < lookupPointsByView.size(); viewIdx++) {
			// Load the image
			checkTrue(lookupImages.loadImage(indexToId.process(viewIdx), image), "Failed to load image");

			// Set up the iterator for this image
			iterator.initialize(scene, lookupPointsByView.get(viewIdx));

			// Get the view that is being processed
			SceneStructureMetric.View v = scene.views.get(viewIdx);

			// Setup the camera projection model using bundle adjustment model directly
			BundleAdjustmentOps.convert(scene.getViewCamera(v).model, image.width, image.height, intrinsic);
			Point2Transform2_F64 norm_to_pixel = new LensDistortionBrown(intrinsic).distort_F64(false, true);

			// Get the transform from world/cloud to this view
			scene.getWorldToView(v, world_to_view, tmp);

			// Grab the colorized points from this view
			colorizer.process4(image, iterator, world_to_view, norm_to_pixel, indexColor);
		}
	}
}

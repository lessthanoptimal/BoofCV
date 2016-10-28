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

package boofcv.alg.distort.spherical;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionWideFOV;
import boofcv.alg.distort.PixelTransformCached_F32;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GImageStatistics;
import boofcv.alg.misc.GPixelMath;
import boofcv.alg.misc.PixelMath;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.Point2Transform3_F32;
import boofcv.struct.distort.Point3Transform2_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.geometry.GeometryMath_F32;
import georegression.geometry.UtilPoint2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Vector3D_F32;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;

/**
 * Fuses information from multiple camera to create a single equirectangular image
 *
 * @author Peter Abeles
 */
public class MultiCameraToRequirectangular<T extends ImageBase<T>> {

	EquirectangularTools_F32 tools = new EquirectangularTools_F32();
	private int equiWidth, equHeight;

	List<Camera> cameras = new ArrayList<>();

	T averageImage;
	T workImage;
	T cameraRendered;
	GrayF32 weightImage;

	ImageDistort<T,T> distort;

	public MultiCameraToRequirectangular(ImageDistort<T,T> distort , int width , int height , ImageType<T> imageType ) {

		if( imageType.getDataType().isInteger() )
			throw new IllegalArgumentException("Must be a floating point image");

		this.distort = distort;
		this.equiWidth = width;
		this.equHeight = height;

		tools.configure(width, height);

		weightImage = new GrayF32(width,height);
		averageImage = imageType.createImage(width, height);
		workImage = averageImage.createSameShape();
		cameraRendered = averageImage.createSameShape();
	}

	/**
	 * Adds a camera and attempts to compute the mask from the provided distortion model.  if a pixel is rendered
	 * outside the bounds in the input image then it is masked out.  If the forwards/backwards transform is too
	 * different then it is masked out.
	 *
	 * @param cameraToCommon Rigid body transform from this camera to the common frame the equirectangular image
	 *                       is in
	 * @param factory Distortion model
	 * @param width Input image width
	 * @param height Input image height
	 */
	public void addCamera(Se3_F64 cameraToCommon , LensDistortionWideFOV factory , int width , int height ) {
		Point2Transform3_F32 p2s = factory.undistortPtoS_F32();
		Point3Transform2_F32 s2p = factory.distortStoP_F32();

		EquiToCamera equiToCamera = new EquiToCamera(cameraToCommon.getR(),s2p);
		CameraToEqui cameraToEqui = new CameraToEqui(cameraToCommon.getR(),p2s);

		Point2D_F32 cameraPixel = new Point2D_F32();
		Point2D_F32 equiPixel = new Point2D_F32();
		GrayU8 cameraMask = new GrayU8(width, height);

		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				cameraToEqui.compute(col,row, equiPixel);
				equiToCamera.compute(equiPixel.x, equiPixel.y, cameraPixel);

				double distance = UtilPoint2D_F32.distance(col,row, cameraPixel.x, cameraPixel.y);

				if( distance < 0.5 ) {
					cameraMask.unsafe_set(col,row,1);
				}
			}
		}

		GrayF32 equiMask = new GrayF32(equiWidth, equHeight);

		PixelTransform2_F32 transformEquiToCam = new PixelTransformCached_F32(equiWidth, equHeight,
				new PointToPixelTransform_F32(equiToCamera));


//		for (int row = 0; row < equHeight; row++) {
//			for (int col = 0; col < equiWidth; col++) {
//				transformEquiToCam.compute(col, row);
//				float pixelX = transformEquiToCam.distX;
//				float pixelY = transformEquiToCam.distY;
//				if( pixelX >= 0 && pixelX <= width-1 && pixelY >= 0 && pixelY <= height-1) {
//					int index = ((int)pixelY)*width + (int)pixelX;
//					cameraMask.data[index]++;
//				}
//			}
//		}
		for (int row = 0; row < equHeight; row++) {
			for (int col = 0; col < equiWidth; col++) {
				transformEquiToCam.compute(col,row);

				float pixelX = transformEquiToCam.distX;
				float pixelY = transformEquiToCam.distY;

				// see if it is contained inside the image
				float weight = 0;
				if( pixelX >= 0 && pixelX <= width-1 && pixelY >= 0 && pixelY <= height-1) {

					if( cameraMask.unsafe_get((int)pixelX, (int)pixelY) == 1 ) {
						weight = 1.0f;
					}
				}

				equiMask.set(col,row,weight);
			}
		}

		cameras.add( new Camera(equiMask, transformEquiToCam));
	}

	/**
	 * Adds a camera and attempts to compute the mask from the provided distortion model.  if a pixel is rendered
	 * outside the bounds in the input image then it is masked out.  If the forwards/backwards transform is too
	 * different then it is masked out.
	 *
	 * @param cameraToCommon Rigid body transform from this camera to the common frame the equirectangular image
	 *                       is in
	 * @param factory Distortion model
	 * @param validMask Binary mask with invalid pixels marked as not zero.  Pixels are in camera image frame.
	 */
	public void addCamera(Se3_F64 cameraToCommon , LensDistortionWideFOV factory , GrayU8 validMask ) {

		int width = validMask.width;
		int height = validMask.height;

		Point3Transform2_F32 s2p = factory.distortStoP_F32();

		GrayF32 mask = new GrayF32(equiWidth, equHeight);

		EquiToCamera equiToCamera = new EquiToCamera(cameraToCommon.getR(),s2p);
		PixelTransform2_F32 transform = new PixelTransformCached_F32(equiWidth, equHeight,
				new PointToPixelTransform_F32(equiToCamera));


		for (int row = 0; row < equHeight; row++) {
			for (int col = 0; col < equiWidth; col++) {
				transform.compute(col,row);

				float pixelX = transform.distX;
				float pixelY = transform.distY;

				// see if it is contained inside the image
				float weight = 0;
				if( pixelX >= 0 && pixelX < width-1 && pixelY >= 0 && pixelY < height-1) {
					if( validMask.get((int)pixelX, (int)pixelY) != 0 ) {
						weight = 1.0f;
					}

				}

				mask.set(col,row,weight);
			}
		}

		cameras.add( new Camera(mask, transform));
	}

	/**
	 * Provides recent images from all the cameras (should be time and lighting synchronized) and renders them
	 * into an equirectangular image.  The images must be in the same order that the cameras were added.
	 *
	 * @param cameraImages List of camera images
	 */
	public void render( List<T> cameraImages ) {
		if( cameraImages.size() != cameras.size())
			throw new IllegalArgumentException("Image image count doesn't equal camera count");

		// avoid divide by zero errors by initializing it to a small non-zero value
		GImageMiscOps.fill(weightImage,1e-4);
		GImageMiscOps.fill(averageImage,0);

		for (int i = 0; i < cameras.size(); i++) {
			Camera c = cameras.get(i);
			T cameraImage = cameraImages.get(i);

			distort.setModel(c.equiToCamera);
			distort.apply(cameraImage,cameraRendered);

			double sum = GImageStatistics.sum(cameraRendered);
			System.out.println("cameraRendered sum "+sum);

			/// sum up the total weight for each pixel
			PixelMath.add(weightImage,c.mask,weightImage);

			sum = GImageStatistics.sum(weightImage);
			System.out.println("weightImage sum "+sum);

			// apply the weight for this image to the rendered image
			GPixelMath.multiply(c.mask,cameraRendered,workImage);

			sum = GImageStatistics.sum(workImage);
			System.out.println("workImage sum "+sum);

			// add the result to the average image
			GPixelMath.add(workImage, averageImage, averageImage);

			sum = GImageStatistics.sum(averageImage);
			System.out.println("averageImage sum "+sum);
		}

		// comput the final output by dividing
		GPixelMath.divide(averageImage,weightImage,averageImage);

		double sum = GImageStatistics.sum(averageImage);
		System.out.println("final averageImage sum "+sum);
	}

	public T getRenderedImage() {
		return averageImage;
	}


	/**
	 * Returns the mask for a specific camera
	 * @param which index of the camera
	 * @return Mask image.  pixel values from 0 to 1
	 */
	public GrayF32 getMask( int which ) {
		return cameras.get(which).mask;
	}

	private static class Camera {
		// weighted pixel mask in equi image.  0 = ignore pixel.  1 = 100% contribution
		GrayF32 mask;

		PixelTransform2_F32 equiToCamera;

		public Camera(GrayF32 mask, PixelTransform2_F32 equiToCamera) {
			this.mask = mask;
			this.equiToCamera = equiToCamera;
		}
	}

	/**
	 * Transform from equirectangular image to camera image pixels
	 */
	private class EquiToCamera implements Point2Transform2_F32 {

		DenseMatrix64F cameraToCommon;
		Point3Transform2_F32 s2p;

		Point3D_F32 unitCam = new Point3D_F32();
		Vector3D_F32 unitCommon = new Vector3D_F32();

		public EquiToCamera(DenseMatrix64F cameraToCommon, Point3Transform2_F32 s2p) {
			this.cameraToCommon = cameraToCommon;
			this.s2p = s2p;
		}

		@Override
		public void compute(float x, float y, Point2D_F32 out) {
			// go from equirectangular pixel to unit sphere in common frame
			tools.equiToNormFV(x,y, unitCommon);

			// rotate the point into camera frame
			GeometryMath_F32.multTran(cameraToCommon, unitCommon, unitCam);

			// input camera image pixels
			s2p.compute(unitCam.x, unitCam.y, unitCam.z , out);
		}
	}

	/**
	 * Transform from camera image to equirectangular image pixels
	 */
	private class CameraToEqui implements Point2Transform2_F32 {

		DenseMatrix64F cameraToCommon;
		Point2Transform3_F32 p2s;

		Point3D_F32 unitCam = new Point3D_F32();
		Point3D_F32 unitCommon = new Point3D_F32();

		public CameraToEqui(DenseMatrix64F cameraToCommon, Point2Transform3_F32 p2s) {
			this.cameraToCommon = cameraToCommon;
			this.p2s = p2s;
		}

		@Override
		public void compute(float x, float y, Point2D_F32 out) {
			// camera pixel to unit camera
			p2s.compute(x,y,unitCam);

			// rotate the point into common frame
			GeometryMath_F32.mult(cameraToCommon, unitCam, unitCommon);

			// unit sphere common to equirectangular
			tools.normToEquiFV(unitCommon.x,unitCommon.y,unitCommon.z, out);
		}
	}

}

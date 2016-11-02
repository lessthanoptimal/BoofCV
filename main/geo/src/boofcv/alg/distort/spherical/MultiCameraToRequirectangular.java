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
 * Fuses information from multiple camera to create a single equirectangular image.  Each image
 * is rendered independently and added to the output image, but weighted by the mask.  The mask
 * describes the region of pixels in the equirectangular image which it represents.
 *
 * @author Peter Abeles
 */
public class MultiCameraToRequirectangular<T extends ImageBase<T>> {

	private EquirectangularTools_F32 tools = new EquirectangularTools_F32();
	private int equiWidth, equHeight;

	private List<Camera> cameras = new ArrayList<>();

	private T averageImage;
	private T workImage;
	private T cameraRendered;
	private GrayF32 weightImage;

	private ImageDistort<T,T> distort;

	// how close to original pixel does it need to be to not be masked out
	private float maskTolerancePixels = 2.0f;

	public MultiCameraToRequirectangular(ImageDistort<T,T> distort , int width , int height , ImageType<T> imageType ) {

		if( imageType.getDataType().isInteger() || imageType.getDataType().getNumBits() != 32 )
			throw new IllegalArgumentException("Must be a 32 bit floating point image");

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

		Point2D_F32 equiPixel = new Point2D_F32();

		GrayF32 equiMask = new GrayF32(equiWidth, equHeight);

		PixelTransform2_F32 transformEquiToCam = new PixelTransformCached_F32(equiWidth, equHeight,
				new PointToPixelTransform_F32(equiToCamera));

		float tol = maskTolerancePixels*maskTolerancePixels;

		for (int row = 0; row < equHeight; row++) {
			for (int col = 0; col < equiWidth; col++) {
				transformEquiToCam.compute(col,row);
				float x = transformEquiToCam.distX;
				float y = transformEquiToCam.distY;

				// make sure it is contained inside the camera image
				if( x < 0f || y < 0f || x > width-1 || y > height-1 )
					continue;

				// need to check this case too
				if( Double.isNaN(x) || Double.isNaN(y))
					continue;

				// go back to equirectangular and see if it returns the same result
				cameraToEqui.compute(x,y, equiPixel);

				float distance2 = UtilPoint2D_F32.distanceSq(equiPixel.x,equiPixel.y,col,row);

				if( distance2 < tol ) {
					equiMask.set(col,row,1);
				}

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

			/// sum up the total weight for each pixel
			PixelMath.add(weightImage,c.mask,weightImage);

			// apply the weight for this image to the rendered image
			GPixelMath.multiply(c.mask,cameraRendered,workImage);

			// add the result to the average image
			GPixelMath.add(workImage, averageImage, averageImage);
		}

		// comput the final output by dividing
		GPixelMath.divide(averageImage,weightImage,averageImage);
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

	public float getMaskTolerancePixels() {
		return maskTolerancePixels;
	}

	public void setMaskTolerancePixels(float maskTolerancePixels) {
		this.maskTolerancePixels = maskTolerancePixels;
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

		EquiToCamera(DenseMatrix64F cameraToCommon, Point3Transform2_F32 s2p) {
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

		CameraToEqui(DenseMatrix64F cameraToCommon, Point2Transform3_F32 p2s) {
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

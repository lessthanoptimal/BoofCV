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
import georegression.geometry.UtilVector3D_F32;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point3D_F32;
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
public class MultiCameraToEquirectangular<T extends ImageBase<T>> {

	private EquirectangularTools_F32 tools = new EquirectangularTools_F32();
	private int equiWidth, equHeight;

	List<Camera> cameras = new ArrayList<>();

	private T averageImage;
	private T workImage;
	private T cameraRendered;
	private GrayF32 weightImage;

	private ImageDistort<T,T> distort;

	// how close two spherical coordinates need to be to be considered a match when doing back and forth validation
	// in radians
	private float maskToleranceAngle = UtilAngle.radian(0.1f);

	/**
	 * Configuration constructor
	 * @param distort Used to apply image distortion from different input images
	 * @param equiWidth Width of output equirectangular image
	 * @param equiHeight Height of output equirectangular image
	 * @param imageType Type of image it processes and outputs.  Must be floating point.  Hmm why isn't this fixed?
	 */
	public MultiCameraToEquirectangular(ImageDistort<T,T> distort , int equiWidth , int equiHeight , ImageType<T> imageType ) {

		if( imageType.getDataType().isInteger() || imageType.getDataType().getNumBits() != 32 )
			throw new IllegalArgumentException("Must be a 32 bit floating point image");

		this.distort = distort;
		this.equiWidth = equiWidth;
		this.equHeight = equiHeight;

		tools.configure(equiWidth, equiHeight);

		weightImage = new GrayF32(equiWidth,equiHeight);
		averageImage = imageType.createImage(equiWidth, equiHeight);
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

		GrayF32 equiMask = new GrayF32(equiWidth, equHeight);

		PixelTransform2_F32 transformEquiToCam = new PixelTransformCached_F32(equiWidth, equHeight,
				new PointToPixelTransform_F32(equiToCamera));

		Point3D_F32 p3b = new Point3D_F32();
		Point2D_F32 p2 = new Point2D_F32();

		for (int row = 0; row < equHeight; row++) {
			for (int col = 0; col < equiWidth; col++) {
				equiToCamera.compute(col,row,p2);

				int camX = (int)(p2.x+0.5f);
				int camY = (int)(p2.y+0.5f);

				if( Double.isNaN(p2.x) || Double.isNaN(p2.y) ||
						camX < 0 || camY < 0 || camX >= width || camY >= height )
					continue;

				p2s.compute(p2.x,p2.y,p3b);

				if( Double.isNaN(p3b.x) || Double.isNaN(p3b.y) || Double.isNaN(p3b.z))
					continue;

				double angle = UtilVector3D_F32.acute(equiToCamera.unitCam,p3b);

				if( angle < maskToleranceAngle) {
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
	 * @param camMask Binary mask with invalid pixels marked as not zero.  Pixels are in camera image frame.
	 */
	public void addCamera(Se3_F64 cameraToCommon , LensDistortionWideFOV factory , GrayU8 camMask ) {

		Point2Transform3_F32 p2s = factory.undistortPtoS_F32();
		Point3Transform2_F32 s2p = factory.distortStoP_F32();

		EquiToCamera equiToCamera = new EquiToCamera(cameraToCommon.getR(),s2p);

		GrayF32 equiMask = new GrayF32(equiWidth, equHeight);

		PixelTransform2_F32 transformEquiToCam = new PixelTransformCached_F32(equiWidth, equHeight,
				new PointToPixelTransform_F32(equiToCamera));

		int width = camMask.width;
		int height = camMask.height;

		Point3D_F32 p3b = new Point3D_F32();
		Point2D_F32 p2 = new Point2D_F32();
		for (int row = 0; row < equHeight; row++) {
			for (int col = 0; col < equiWidth; col++) {
				equiToCamera.compute(col,row,p2);

				int camX = (int)(p2.x+0.5f);
				int camY = (int)(p2.y+0.5f);

				if( Double.isNaN(p2.x) || Double.isNaN(p2.y) ||
						camX < 0 || camY < 0 || camX >= width || camY >= height )
					continue;

				if( camMask.unsafe_get(camX,camY) == 1 ) {
					p2s.compute(p2.x,p2.y,p3b);

					if( Double.isNaN(p3b.x) || Double.isNaN(p3b.y) || Double.isNaN(p3b.z))
						continue;

					double angle = UtilVector3D_F32.acute(equiToCamera.unitCam,p3b);

					if( angle < maskToleranceAngle) {
						equiMask.set(col,row,1);
					}
				}
			}
		}

		cameras.add( new Camera(equiMask, transformEquiToCam));
	}

	/**
	 * Provides recent images from all the cameras (should be time and lighting synchronized) and renders them
	 * into an equirectangular image.  The images must be in the same order that the cameras were added.
	 *
	 * @param cameraImages List of camera images
	 */
	public void render( List<T> cameraImages ) {
		if( cameraImages.size() != cameras.size())
			throw new IllegalArgumentException("Input camera image count doesn't equal the expected number");

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

	public float getMaskToleranceAngle() {
		return maskToleranceAngle;
	}

	/**
	 * Specify the tolerance that the circle normal angle must be invertible in radians
	 *
	 * @param maskToleranceAngle tolerance in radians
	 */
	public void setMaskToleranceAngle(float maskToleranceAngle) {
		this.maskToleranceAngle = maskToleranceAngle;
	}

	static class Camera {
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
		Point3D_F32 unitCommon = new Point3D_F32();

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
}

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

package boofcv.alg.sfm.d2;

import boofcv.abst.sfm.d2.ImageMotion2D;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.ImageBase;
import georegression.metric.Area2D_F64;
import georegression.struct.InvertibleTransform;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.RectangleLength2D_I32;

/**
 * Stitches together sequences of images using {@link ImageMotion2D}, typically used for image stabilization
 * and creating mosaics.  Internally any motion model in the Homogeneous family can be used.  For convenience,
 * those models are converted into a {@link Homography2D_F64} on output.
 *
 * The size of the stitch region is specified using {@link #configure(int, int, georegression.struct.InvertibleTransform)}
 * which must be called before any images are processed.  One of the parameters include an initial transform.  The
 * initial transform can be used to scale/translate/other the input image.
 *
 * A sudden change or jump in the shape of the view area can be an indication of a bad motion estimate.  If a large
 * jump larger than the user specified threshold is detected then {@link #process(boofcv.struct.image.ImageBase)}
 * will return false.
 *
 * @author Peter Abeles
 */

public class StitchingFromMotion2D<I extends ImageBase, IT extends InvertibleTransform>
{
	// REFERENCE FRAME NOTES:
	//
	// World references to the stitched image
	// Initial is the first video frame in video coordinates
	// Current is the current video frame in video coordinates

	// estimates image motion
	private ImageMotion2D<I,IT> motion;
	// renders the distorted image according to results from motion
	private ImageDistort<I,I> distorter;
	// converts different types of motion models into other formats
	private StitchingTransform<IT> converter;

	// Transform from first video frame to the initial location in the stitched image
	private IT worldToInit;
	// size of the stitch image
	private int widthStitch, heightStitch;

	// Largest allowed fractional change in area
	private double maxJumpFraction;
	// image corners are used to detect large motions
	private Corners corners = new Corners();
	// size of view area in previous update
	private double previousArea;

	// storage for the transform from current frame to the initial frame
	private IT worldToCurr;

	private PixelTransform2_F32 tranWorldToCurr;
	private PixelTransform2_F32 tranCurrToWorld;

	// storage for the stitched image
	private I stitchedImage;
	private I workImage;

	// first time that it has been called
	private boolean first = true;

	/**
	 * Provides internal algorithms and tuning parameters.
	 *
	 * @param motion Estimates image motion
	 * @param distorter Applies found transformation to stitch images
	 * @param converter Converts internal model into a homogenous transformation
	 * @param maxJumpFraction If the view area changes by more than this fraction a fault is declared
	 */
	public StitchingFromMotion2D(ImageMotion2D<I, IT> motion,
								 ImageDistort<I,I> distorter,
								 StitchingTransform<IT> converter ,
								 double maxJumpFraction )
	{
		this.motion = motion;
		this.distorter = distorter;
		this.converter = converter;
		this.maxJumpFraction = maxJumpFraction;

		worldToCurr = (IT)motion.getFirstToCurrent().createInstance();
	}

	/**
	 * Specifies size of stitch image and the location of the initial coordinate system.
	 *
	 * @param widthStitch Width of the image being stitched into
	 * @param heightStitch Height of the image being stitched into
	 * @param worldToInit (Option) Used to change the location of the initial frame in stitched image.
	 *                    null means no transform.
	 */
	public void configure( int widthStitch, int heightStitch , IT worldToInit ) {
		this.worldToInit = (IT)worldToCurr.createInstance();
		if( worldToInit != null )
			this.worldToInit.set(worldToInit);
		this.widthStitch = widthStitch;
		this.heightStitch = heightStitch;
	}

	/**
	 * Estimates the image motion and updates stitched image.  If it is unable to estimate the motion then false
	 * is returned and the stitched image is left unmodified. If false is returned then in most situations it is
	 * best to call {@link #reset()} and start over.
	 *
	 * @param image Next image in the sequence
	 * @return True if the stitched image is updated and false if it failed and was not
	 */
	public boolean process( I image ) {
		if( stitchedImage == null ) {
			stitchedImage = (I)image.createNew(widthStitch, heightStitch);
			workImage = (I)image.createNew(widthStitch, heightStitch);
		}

		if( motion.process(image) ) {
			update(image);

			// check to see if an unstable and improbably solution was generated
			return !checkLargeMotion(image.width, image.height);
		} else {
			return false;
		}
	}

	/**
	 * Throws away current results and starts over again
	 */
	public void reset() {
		if( stitchedImage != null )
			GImageMiscOps.fill(stitchedImage, 0);
		motion.reset();
		worldToCurr.reset();
		first = true;
	}

	/**
	 * Looks for sudden large changes in corner location to detect motion estimation faults.
	 * @param width image width
	 * @param height image height
	 * @return true for fault
	 */
	private boolean checkLargeMotion( int width , int height ) {
		if( first ) {
			getImageCorners(width,height,corners);
			previousArea = computeArea(corners);
			first = false;
		} else {
			getImageCorners(width,height,corners);

			double area = computeArea(corners);

			double change = Math.max(area/previousArea,previousArea/area)-1;
			if( change > maxJumpFraction ) {
				return true;
			}
			previousArea = area;
		}

		return false;

	}

	private double computeArea( Corners c ) {
		return Area2D_F64.triangle(c.p0,c.p1,c.p2) +
				Area2D_F64.triangle(c.p0,c.p2,c.p3);
	}

	/**
	 * Adds the latest image into the stitched image
	 *
	 * @param image
	 */
	private void update(I image) {
		computeCurrToInit_PixelTran();

		// only process a cropped portion to speed up processing
		RectangleLength2D_I32 box = DistortImageOps.boundBox(image.width, image.height,
				stitchedImage.width, stitchedImage.height, tranCurrToWorld);

		int x0 = box.x0;
		int y0 = box.y0;
		int x1 = box.x0 + box.width;
		int y1 = box.y0 + box.height;

		distorter.setModel(tranWorldToCurr);
		distorter.apply(image, stitchedImage,x0,y0,x1,y1);
	}

	private void computeCurrToInit_PixelTran() {
		IT initToCurr = motion.getFirstToCurrent();
		worldToInit.concat(initToCurr, worldToCurr);

		tranWorldToCurr = converter.convertPixel(worldToCurr,tranWorldToCurr);

		IT currToWorld = (IT) this.worldToCurr.invert(null);

		tranCurrToWorld = converter.convertPixel(currToWorld, tranCurrToWorld);
	}

	/**
	 * Sets the current image to be the origin of the stitched coordinate system.  The background is filled
	 * with a value of 0.
	 * Must be called after {@link #process(boofcv.struct.image.ImageBase)}.
	 */
	public void setOriginToCurrent() {
		IT currToWorld = (IT)worldToCurr.invert(null);
		IT oldWorldToNewWorld = (IT) worldToInit.concat(currToWorld,null);

		PixelTransform2_F32 newToOld = converter.convertPixel(oldWorldToNewWorld,null);

		// fill in the background color
		GImageMiscOps.fill(workImage, 0);
		// render the transform
		distorter.setModel(newToOld);
		distorter.apply(stitchedImage, workImage);

		// swap the two images
		I s = workImage;
		workImage = stitchedImage;
		stitchedImage = s;

		// have motion estimates be relative to this frame
		motion.setToFirst();
		first = true;

		computeCurrToInit_PixelTran();
	}

	/**
	 * Resizes the stitch image.  If no transform is provided then the old stitch region is simply
	 * places on top of the new one and copied.  Pixels which do not exist in the old image are filled with zero.
	 *
	 * @param widthStitch The new width of the stitch image.
	 * @param heightStitch The new height of the stitch image.
	 * @param newToOldStitch (Optional) Transform from new stitch image pixels to old stick pixels.  Can be null.
	 */
	public void resizeStitchImage( int widthStitch, int heightStitch , IT newToOldStitch ) {

		// copy the old image into the new one
		workImage.reshape(widthStitch,heightStitch);
		GImageMiscOps.fill(workImage, 0);
		if( newToOldStitch != null ) {
			PixelTransform2_F32 newToOld = converter.convertPixel(newToOldStitch,null);
			distorter.setModel(newToOld);
			distorter.apply(stitchedImage, workImage);

			// update the transforms
			IT tmp = (IT)worldToCurr.createInstance();
			newToOldStitch.concat(worldToInit, tmp);
			worldToInit.set(tmp);

			computeCurrToInit_PixelTran();
		} else {
			int overlapWidth = Math.min(widthStitch,stitchedImage.width);
			int overlapHeight = Math.min(heightStitch,stitchedImage.height);
			GImageMiscOps.copy(0,0,0,0,overlapWidth,overlapHeight,stitchedImage,workImage);
		}
		stitchedImage.reshape(widthStitch,heightStitch);
		I tmp = stitchedImage;
		stitchedImage = workImage;
		workImage = tmp;

		this.widthStitch = widthStitch;
		this.heightStitch = heightStitch;
	}

	/**
	 * Returns the location of the input image's corners inside the stitch image.
	 *
	 * @return image corners
	 */
	public Corners getImageCorners( int width , int height , Corners corners ) {

		if( corners == null )
			corners = new Corners();

		int w = width;
		int h = height;

		tranCurrToWorld.compute(0,0); corners.p0.set(tranCurrToWorld.distX, tranCurrToWorld.distY);
		tranCurrToWorld.compute(w,0); corners.p1.set(tranCurrToWorld.distX, tranCurrToWorld.distY);
		tranCurrToWorld.compute(w,h); corners.p2.set(tranCurrToWorld.distX, tranCurrToWorld.distY);
		tranCurrToWorld.compute(0,h); corners.p3.set(tranCurrToWorld.distX, tranCurrToWorld.distY);

		return corners;
	}

	/**
	 * Transform from world coordinate system into the current image frame.
	 *
	 * @return Transformation
	 */
	public Homography2D_F64 getWorldToCurr( Homography2D_F64 storage ) {
		return converter.convertH(worldToCurr,storage);
	}

	public IT getWorldToCurr() {
		return worldToCurr;
	}

	public I getStitchedImage() {
		return stitchedImage;
	}

	public ImageMotion2D<I, IT> getMotion() {
		return motion;
	}

	public static class Corners {
		public Point2D_F64 p0 = new Point2D_F64();
		public Point2D_F64 p1 = new Point2D_F64();
		public Point2D_F64 p2 = new Point2D_F64();
		public Point2D_F64 p3 = new Point2D_F64();
	}

}

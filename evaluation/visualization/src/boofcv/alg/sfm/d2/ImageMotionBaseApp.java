/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.alg.sfm.robust.DistanceAffine2DSq;
import boofcv.alg.sfm.robust.DistanceHomographySq;
import boofcv.alg.sfm.robust.GenerateAffine2D;
import boofcv.alg.sfm.robust.GenerateHomographyLinear;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.VideoProcessAppBase;
import boofcv.gui.VisualizeApp;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.io.image.SimpleImageSequence;
import boofcv.numerics.fitting.modelset.DistanceFromModel;
import boofcv.numerics.fitting.modelset.ModelFitter;
import boofcv.numerics.fitting.modelset.ModelGenerator;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.numerics.fitting.modelset.ransac.Ransac;
import boofcv.struct.FastQueue;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.InvertibleTransform;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.homo.Homography2D_F32;
import georegression.struct.homo.Homography2D_F64;
import georegression.struct.homo.UtilHomography;
import georegression.struct.point.Point2D_F32;
import georegression.transform.homo.HomographyPointOps_F32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Parent class for applications which estimate image motion based upon fit parameters to a model on extracted
 * point features.  Only gray scale images are processed, but the output can be in gray scale or color.
 *
 * @author Peter Abeles
 */
public abstract class ImageMotionBaseApp<I extends ImageSingleBand,
		T extends InvertibleTransform>
		extends VideoProcessAppBase<I> implements VisualizeApp
{
	// If the input and output images are being shown, this is the width of a border between them.
	private final static int outputBorder = 10;

	// tracks feature in the video stream
	protected ImagePointTracker<I> tracker;
	// finds the best fit model parameters to describe feature motion
	protected ModelMatcher<T,AssociatedPair> modelMatcher;
	// batch refinement algorithm
	protected ModelFitter<T,AssociatedPair> modelRefiner;

	// computes motion across multiple frames intelligently
	// MUST be declared by child class
	protected ImageMotionPointKey<I,T> distortAlg;

	// render the found motion on the output image
	protected RenderImageMotion<I,?> motionRender;

	BufferedImage distortedImage;

	protected ImageMotionInfoPanel infoPanel;
	private DisplayPanel gui = new DisplayPanel();

	// dimension of output image, specified by child
	protected int outputWidth;
	protected int outputHeight;

	// dimension of input image, extracted from stream
	protected int inputWidth;
	protected int inputHeight;

	// data type which is being fit
	T fitModel;
	// type of gray scale
	Class<I> imageType;
	boolean colorOutput = true;

	// crude performance statistics
	protected int totalKeyFrames = 0;

	// display options
	protected boolean showInput;
	protected boolean showImageView = true;

	/**
	 * Specifies setup
	 *
	 * @param showInput Will the input be shown along with the distorted image?
	 * @param imageType Type of gray scale image being processed
	 * @param numAlgFamilies Number of algs in GUI bar
	 */
	public ImageMotionBaseApp(boolean showInput, Class<I> imageType, int numAlgFamilies)
	{
		super(numAlgFamilies,imageType);
		this.showInput = showInput;
		this.imageType = imageType;

		infoPanel = new ImageMotionInfoPanel();
		infoPanel.setMaximumSize(infoPanel.getPreferredSize());
		gui.addMouseListener(this);

		add(infoPanel, BorderLayout.WEST);
		setMainGUI(gui);
	}

	/**
	 * Specifies the size of the distorted output image.
	 */
	protected void setOutputSize( int width , int height ) {
		this.outputWidth = width;
		this.outputHeight = height;

		motionRender = new RenderImageMotion<I,ImageBase>(outputWidth,outputHeight,imageType,colorOutput);
		distortedImage= new BufferedImage(outputWidth,outputHeight,BufferedImage.TYPE_INT_RGB);

		if(showInput) {
			gui.setPreferredSize(new Dimension(inputWidth+outputBorder+outputWidth, Math.max(inputHeight,outputHeight)));
		} else {
			gui.setPreferredSize(new Dimension(outputWidth, outputHeight));
		}
		gui.setMinimumSize(gui.getPreferredSize());
	}

	/**
	 * Start processing the image sequence
	 * 
	 * @param sequence Image sequence being processed
	 */
	@Override
	protected void process(SimpleImageSequence<I> sequence) {
		if( !sequence.hasNext() )
			return;
		// stop the image processing code
		stopWorker();

		this.sequence = sequence;
		sequence.setLoop(true);
		
		// save the input image dimension
		I input = sequence.next();
		inputWidth = input.width;
		inputHeight = input.height;

		// start everything up and resume processing
		doRefreshAll();
	}

	/**
	 * Updates the distortion estimation, but no GUI work here
	 * @param frame gray scale frame being processed
	 * @param buffImage color image being processed
	 */
	@Override
	protected void updateAlg(I frame, BufferedImage buffImage) {
		if( distortAlg == null )
			return;

		// update the image motion estimation
		if( !distortAlg.process(frame) ) {
			handleFatalError();
		}

		// render the results onto the distorted image
		renderCurrentTransform(frame, buffImage);
	}

	/**
	 * Adds the current frame onto the distorted image
	 */
	protected void renderCurrentTransform(I frame, BufferedImage buffImage) {
		T worldToCurr = distortAlg.getWorldToCurr();
		PixelTransform_F32 pixelTran = UtilImageMotion.createPixelTransform(worldToCurr);
		PixelTransform_F32 pixelTranInv = UtilImageMotion.createPixelTransform(worldToCurr.invert(null));

		motionRender.update(frame, buffImage, pixelTran,pixelTranInv);
	}


	@Override
	protected void updateAlgGUI(I frame, BufferedImage imageGUI, final double fps) {

		// switch between B&W and color mosaic modes
		if( infoPanel.getColor() != motionRender.getColorOutput() ) {
			synchronized (motionRender) {
				motionRender.setColorOutput(infoPanel.getColor());
			}
		}

		T worldToCurr = distortAlg.getWorldToCurr();
		final T currToWorld = (T)worldToCurr.invert(null);

		ConvertBufferedImage.convertTo(motionRender.getMosaic(), distortedImage);

		// toggle on and off the view window
		showImageView = infoPanel.getShowView();
		
		// toggle on and off showing the active tracks
		if( infoPanel.getShowInliers())
			gui.setInliers(modelMatcher.getMatchSet());
		else
			gui.setInliers(null);
		if( infoPanel.getShowAll())
			gui.setAllTracks(tracker.getActiveTracks(null));
		else
			gui.setAllTracks(null);

		Homography2D_F32 H = convertToHomography(currToWorld);
		gui.setCurrToWorld(H);
		gui.setImages(imageGUI,distortedImage);

		final int numAssociated = modelMatcher.getMatchSet().size();
		final int numFeatures = tracker.getActiveTracks(null).size();

		// update GUI
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				infoPanel.setFPS(fps);
				infoPanel.setNumInliers(numAssociated);
				infoPanel.setNumTracks(numFeatures);
				infoPanel.setKeyFrames(totalKeyFrames);
				infoPanel.repaint();

				gui.repaint();
			}
		});
	}

	/**
	 * Convert other motion models that are 64 bit into a 32 bit homography
	 * @param m 64F motion model
	 * @return Homography2D_F32
	 */
	protected Homography2D_F32 convertToHomography(T m) {

		Homography2D_F32 H = new Homography2D_F32();
		
		if( m instanceof Affine2D_F64) {
			Affine2D_F64 affine = (Affine2D_F64)m;

			H.a11 = (float)affine.a11;
			H.a12 = (float)affine.a12;
			H.a21 = (float)affine.a21;
			H.a22 = (float)affine.a22;
			H.a13 = (float)affine.tx;
			H.a23 = (float)affine.ty;
			H.a31 = 0;
			H.a32 = 0;
			H.a33 = 1;
		} else if( m instanceof Homography2D_F64) {
			Homography2D_F64 h = (Homography2D_F64)m;

			UtilHomography.convert(h, H);

		} else {
			throw new RuntimeException("Unexpected type: "+m.getClass().getSimpleName());
		}
		
		return H;
	}

	/**
	 * Draws the location of the current image onto the distorted image as a red quadrilateral.
	 * 
	 * @param scale The scale from distorted image to its display output
	 * @param offsetX  Offset in the display output
	 * @param offsetY  Offset in the display output
	 * @param width  Width of input image
	 * @param height Height of input image
	 * @param currToGlobal Transform from current frame to global reference frame
	 * @param g2 Drawing object
	 */
	protected void drawImageBounds( float scale , int offsetX , int offsetY , int width , int height, Homography2D_F32 currToGlobal, Graphics g2) {
		Point2D_F32 a = new Point2D_F32(0,0);
		Point2D_F32 b = new Point2D_F32(0+width,0);
		Point2D_F32 c = new Point2D_F32(width,height);
		Point2D_F32 d = new Point2D_F32(0,height);

		HomographyPointOps_F32.transform(currToGlobal, a, a);
		HomographyPointOps_F32.transform(currToGlobal,b,b);
		HomographyPointOps_F32.transform(currToGlobal,c,c);
		HomographyPointOps_F32.transform(currToGlobal,d,d);

		a.x = offsetX + a.x*scale; a.y = offsetY + a.y*scale;
		b.x = offsetX + b.x*scale; b.y = offsetY + b.y*scale;
		c.x = offsetX + c.x*scale; c.y = offsetY + c.y*scale;
		d.x = offsetX + d.x*scale; d.y = offsetY + d.y*scale;

		g2.setColor(Color.RED);
		g2.drawLine((int)a.x,(int)a.y,(int)b.x,(int)b.y);
		g2.drawLine((int)b.x,(int)b.y,(int)c.x,(int)c.y);
		g2.drawLine((int)c.x,(int)c.y,(int)d.x,(int)d.y);
		g2.drawLine((int)d.x,(int)d.y,(int)a.x,(int)a.y);
	}

	/**
	 * Draw features after applying a homography transformation.
	 */
	protected void drawFeatures( float scale , int offsetX , int offsetY ,
								 FastQueue<Point2D_F32> all,
								 FastQueue<Point2D_F32> inliers,
								 Homography2D_F32 currToGlobal, Graphics2D g2 ) {

		Point2D_F32 distPt = new Point2D_F32();

		for( int i = 0; i < all.size; i++  ) {
			HomographyPointOps_F32.transform(currToGlobal,all.get(i),distPt);

			distPt.x = offsetX + distPt.x*scale;
			distPt.y = offsetY + distPt.y*scale;

			VisualizeFeatures.drawPoint(g2, (int) distPt.x, (int) distPt.y, Color.RED);
		}

		for( int i = 0; i < inliers.size; i++  ) {
			HomographyPointOps_F32.transform(currToGlobal,inliers.get(i),distPt);

			distPt.x = offsetX + distPt.x*scale;
			distPt.y = offsetY + distPt.y*scale;

			VisualizeFeatures.drawPoint(g2, (int) distPt.x, (int) distPt.y, Color.BLUE);
		}
	}

	/**
	 * Draw features with no extra transformation
	 */
	protected void drawFeatures( float scale , int offsetX , int offsetY ,
								 FastQueue<Point2D_F32> all,
								 FastQueue<Point2D_F32> inliers,
								 Graphics2D g2 ) {

		Point2D_F32 distPt = new Point2D_F32();

		for( int i = 0; i < all.size; i++  ) {
			Point2D_F32 p = all.get(i);

			distPt.x = offsetX + p.x*scale;
			distPt.y = offsetY + p.y*scale;

			VisualizeFeatures.drawPoint(g2, (int) distPt.x, (int) distPt.y, Color.RED);
		}

		for( int i = 0; i < inliers.size; i++  ) {
			Point2D_F32 p = inliers.get(i);

			distPt.x = offsetX + p.x*scale;
			distPt.y = offsetY + p.y*scale;

			VisualizeFeatures.drawPoint(g2, (int) distPt.x, (int) distPt.y, Color.BLUE);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return distortAlg != null && distortAlg.getTotalProcessed() > 0;
	}

	@Override
	public void refreshAll(Object[] cookies) {
		stopWorker();

		tracker = (ImagePointTracker<I>)cookies[0];
		fitModel = (T)cookies[1];

		startEverything();
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( sequence == null || modelMatcher == null )
			return;

		stopWorker();

		switch( indexFamily ) {
			case 0:
				tracker = (ImagePointTracker<I>)cookie;
				break;
			
			case 1:
				fitModel = (T)cookie;
				break;
		}

		// restart the video
		sequence.reset();

		startEverything();
	}

	/**
	 * Displays the results as either just the distorted output image or as the input and distorted image
	 * side by side.
	 */
	private class DisplayPanel extends JPanel
	{
		// original input image
		BufferedImage orig;
		// rendered distorted image
		BufferedImage distorted;

		// copies of feature location for GUI thread
		FastQueue<Point2D_F32> inliers = new FastQueue<Point2D_F32>(300,Point2D_F32.class,true);
		FastQueue<Point2D_F32> allTracks = new FastQueue<Point2D_F32>(300,Point2D_F32.class,true);

		Homography2D_F32 currToWorld = new Homography2D_F32();

		public void setImages( BufferedImage orig , BufferedImage stabilized )
		{
			this.orig = orig;
			this.distorted = stabilized;
		}

		public synchronized void setInliers(java.util.List<AssociatedPair> list) {
			inliers.reset();

			if( list != null ) {
				for( AssociatedPair p : list ) {
					inliers.grow().set((float)p.p2.x,(float)p.p2.y);
				}
			}
		}

		public synchronized void setAllTracks(java.util.List<PointTrack> list) {
			allTracks.reset();

			if( list != null ) {
				for( PointTrack p : list ) {
					allTracks.grow().set((float)p.x,(float)p.y);
				}
			}
		}

		@Override
		public synchronized void paintComponent(Graphics g) {
			super.paintComponent(g);

			if( orig == null || distorted == null )
				return;

			Graphics2D g2 = (Graphics2D)g;

			if (showInput) {
				drawBoth( g2 );
			} else {
				drawJustDistorted( g2 );
			}
		}
		
		private void drawJustDistorted( Graphics2D g2 ) {
			int w = getWidth();
			int h = getHeight();

			double scaleX = w/(double) distorted.getWidth();
			double scaleY = h/(double) distorted.getHeight();

			double scale = Math.min(scaleX,scaleY);
			if( scale > 1 ) scale = 1;

			int scaledWidth = (int)(scale* distorted.getWidth());
			int scaledHeight = (int)(scale* distorted.getHeight());

			// draw stabilized on right
			g2.drawImage(distorted,0,0,scaledWidth,scaledHeight,0,0, distorted.getWidth(), distorted.getHeight(),null);

			drawFeatures((float)scale,0,0,allTracks,inliers,currToWorld,g2);

			if(showImageView)
				drawImageBounds((float)scale,0,0,orig.getWidth(),orig.getHeight(), currToWorld,g2);
		}
		
		private void drawBoth( Graphics2D g2 ) {
			
			int desiredWidth = orig.getWidth()+ distorted.getWidth();
			int desiredHeight = Math.max(orig.getHeight(), distorted.getHeight());

			int w = getWidth()-outputBorder;
			int h = getHeight();

			double scaleX = w/(double)desiredWidth;
			double scaleY = h/(double)desiredHeight;

			double scale = Math.min(scaleX,scaleY);
			if( scale > 1 ) scale = 1;

			int scaledInputWidth = (int)(scale*orig.getWidth());
			int scaledInputHeight = (int)(scale*orig.getHeight());

			int scaledOutputWidth = (int)(scale* distorted.getWidth());
			int scaledOutputHeight = (int)(scale* distorted.getHeight());

			//  draw undistorted on left
			g2.drawImage(orig,0,0,scaledInputWidth,scaledInputHeight,0,0,orig.getWidth(),orig.getHeight(),null);

			// draw distorted on right
			g2.drawImage(distorted,scaledInputWidth+outputBorder,0,scaledInputWidth+scaledOutputWidth+outputBorder,scaledOutputHeight,0,0,outputWidth,outputHeight,null);

			drawFeatures((float)scale,0,0,allTracks,inliers,g2);
			drawFeatures((float)scale,scaledInputWidth+outputBorder,0,allTracks,inliers,currToWorld,g2);

			if(showImageView)
				drawImageBounds((float)scale,scaledInputWidth+outputBorder,0,orig.getWidth(),orig.getHeight(), currToWorld,g2);
		}

		public void setCurrToWorld(Homography2D_F32 currToWorld) {
			this.currToWorld.set(currToWorld);
		}
	}

	/**
	 * Create a {@link ModelMatcher} for the type of model it is fitting to
	 *
	 * @param maxIterations Maximum number of iterations in RANSAC
	 * @param thresholdFit Inlier fit threshold
	 */
	protected void createModelMatcher( int maxIterations , double thresholdFit ) {

		ModelGenerator fitter;
		DistanceFromModel distance;
		
		if( fitModel instanceof Homography2D_F64 ) {
			GenerateHomographyLinear mf = new GenerateHomographyLinear(true);
			fitter = mf;
			modelRefiner = (ModelFitter)mf;
			distance = new DistanceHomographySq();
		} else if( fitModel instanceof Affine2D_F64 ) {
			GenerateAffine2D mf = new GenerateAffine2D();
			fitter = mf;
			distance = new DistanceAffine2DSq();
			modelRefiner = (ModelFitter)mf;
		} else {
			throw new RuntimeException("Unknown model type");
		}

		modelMatcher = new Ransac(123123,fitter,distance,maxIterations,thresholdFit);

	}

	protected abstract void startEverything();
	
	protected abstract void handleFatalError();
}

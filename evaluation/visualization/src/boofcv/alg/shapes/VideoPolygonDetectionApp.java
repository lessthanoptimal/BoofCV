/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.shapes;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.distort.AdjustmentType;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.distort.PixelTransformCached_F32;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.shapes.polygon.BinaryPolygonConvexDetector;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.VideoProcessAppBase;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

/**
 * Let's the user select a region in the image and tracks it using different algorithms.
 *
 * @author Peter Abeles
 */
public class VideoPolygonDetectionApp<I extends ImageSingleBand>
		extends VideoProcessAppBase<MultiSpectral<I>>
{

	Class<I> imageClass;

	ImagePanel panel = new ImagePanel();

	I gray;
	ImageUInt8 binary = new ImageUInt8(1,1);

	BinaryPolygonConvexDetector<I> detector;
	InputToBinary<I> inputToBinary;

	IntrinsicParameters intrinsic;

	PointTransform_F64 pointUndistToDist;

	boolean processedInputImage = false;
	boolean firstFrame = true;

	public VideoPolygonDetectionApp(Class<I> imageType) {
		super(0, ImageType.ms(3, imageType));
		this.imageClass = imageType;

		gray = GeneralizedImageOps.createSingleBand(imageType,1,1);

		panel.setPreferredSize(new Dimension(640, 480));
		panel.setFocusable(true);
		panel.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				isPaused = !isPaused;
			}

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {}

			@Override
			public void mouseEntered(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {}
		});

		setMainGUI(panel);

		periodSpinner.setValue(33);

		ConfigPolygonDetector config = new ConfigPolygonDetector(4);
		config.refineWithLines = true;
		config.refineWithCorners = false;

//		inputToBinary = FactoryThresholdBinary.adaptiveSquare(6, 0, true,imageType);
		inputToBinary = FactoryThresholdBinary.globalOtsu(0, 256, true, imageType);

		detector = FactoryShapeDetector.polygon(config,imageType);
	}

	@Override
	public void process( final SimpleImageSequence<MultiSpectral<I>> sequence ) {

		// stop the image processing code
		stopWorker();

		this.sequence = sequence;
		sequence.setLoop(true);
		setPause(false);
		if( !sequence.hasNext() )
			throw new IllegalArgumentException("Empty sequence");

		// start everything up and resume processing
		doRefreshAll();
	}

	@Override
	public void refreshAll(Object[] cookies) {

		firstFrame = true;
		setPause(false);

		startWorkerThread();
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		stopWorker();

		sequence.reset();

		refreshAll(null);
	}

	int count = 0;
	@Override
	protected void updateAlg(MultiSpectral<I> frame, BufferedImage buffImage) {

		GConvertImage.average(frame,gray);
		inputToBinary.process(gray,binary);
		detector.process(gray,binary);

		// frame 212 it isn't detecting a square
		System.out.println(count+"  detected "+detector.getFound().size);

		if( count == 212 ) {
			UtilImageIO.saveImage(buffImage,"saved.png");
		}

		count++;

		processedInputImage = true;
	}

	@Override
	protected void updateAlgGUI(MultiSpectral<I> frame, BufferedImage imageGUI, double fps) {
		if( firstFrame ) {
			panel.setPreferredSize(new Dimension(imageGUI.getWidth(),imageGUI.getHeight()));
			firstFrame = false;
		}

		Graphics2D g2 = imageGUI.createGraphics();

		FastQueue<Polygon2D_F64> found = detector.getFound();
		g2.setColor(new Color(255,0,0,200));
		g2.setStroke(new BasicStroke(4));

		for (int i = 0; i < found.size(); i++) {

			Polygon2D_F64 poly = found.get(i);
			for( int j = 0; j < poly.size(); j++ ) {
				Point2D_F64 p = poly.get(j);
				pointUndistToDist.compute(p.x,p.y,p);
			}

			VisualizeShapes.drawPolygon(found.get(i), true, g2);
		}
		panel.setBufferedImageSafe(imageGUI);
		panel.repaint();
	}

	@Override
	public void changeInput(String name, int index) {

		stopWorker();
		processedInputImage = false;

		String videoName = inputRefs.get(index).getPath();
		String path = videoName.substring(0,videoName.lastIndexOf('/'));

		intrinsic = UtilIO.loadXML(media.openFile(path+"/intrinsic.xml"));

		gray.reshape(intrinsic.width,intrinsic.height);
		binary.reshape(intrinsic.width,intrinsic.height);

		this.pointUndistToDist = LensDistortionOps.transform_F64(AdjustmentType.FULL_VIEW, intrinsic, null, true);

		PointTransform_F32 pointDistToUndist = LensDistortionOps.
				transform_F32(AdjustmentType.FULL_VIEW, intrinsic, null, false);
		PointTransform_F32 pointUndistToDist = LensDistortionOps.
				transform_F32(AdjustmentType.FULL_VIEW, intrinsic, null, true);
		PixelTransform_F32 distToUndist = new PointToPixelTransform_F32(pointDistToUndist);
		PixelTransform_F32 undistToDist = new PointToPixelTransform_F32(pointUndistToDist);

		distToUndist = new PixelTransformCached_F32(intrinsic.width, intrinsic.height, distToUndist);
		undistToDist = new PixelTransformCached_F32(intrinsic.width, intrinsic.height, undistToDist);

		detector.setLensDistortion(intrinsic.width,intrinsic.height,distToUndist,undistToDist);

		SimpleImageSequence<MultiSpectral<I>> video = media.openVideo(videoName, ImageType.ms(3, imageClass));

		process(video);
	}

	@Override
	protected void handleRunningStatus(int status) {

	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public boolean getHasProcessedImage() {
		return processedInputImage;
	}


	public static void main(String[] args) {
//		Class type = ImageFloat32.class;
		Class type = ImageUInt8.class;

		VideoPolygonDetectionApp app = new VideoPolygonDetectionApp(type);

		app.setBaseDirectory("../data/applet/fiducial/");
		app.loadInputData("../data/applet/fiducial/fiducial.txt");

//		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();
//		inputs.add(new PathLabel(SQUARE_NUMBER, "../data/applet/fiducial/binary/movie.mjpeg"));
//		inputs.add(new PathLabel(SQUARE_PICTURE, "../data/applet/fiducial/image/movie.mjpeg"));
//		inputs.add(new PathLabel(CALIB_CHESS, "../data/applet/fiducial/calibration/movie.mjpeg"));
//
//		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Tracking Square", true);
	}
}

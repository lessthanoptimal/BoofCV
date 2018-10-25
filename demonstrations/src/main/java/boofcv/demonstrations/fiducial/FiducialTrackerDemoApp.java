/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.fiducial;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.abst.fiducial.FiducialStability;
import boofcv.abst.fiducial.SquareImage_to_FiducialDetector;
import boofcv.abst.fiducial.calib.*;
import boofcv.alg.distort.radtan.LensDistortionRadialTangential;
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.ConfigFiducialImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.fiducial.VisualizeFiducial;
import boofcv.gui.image.ImagePanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Polygon2D_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Let's the user select a region in the image and tracks it using different algorithms.
 *
 * @author Peter Abeles
 */
public class FiducialTrackerDemoApp<I extends ImageGray<I>>
		extends DemonstrationBase
{
	private static final String SQUARE_NUMBER = "Square Number";
	private static final String SQUARE_PICTURE = "Square Picture";
	private static final String CALIB_CHESS = "Chessboard";
	private static final String CALIB_SQUARE_GRID = "Square Grid";
	private static final String CALIB_SQUARE_BINARY_GRID = "Square Binary Grid";
	private static final String CALIB_CIRCLE_HEXAGONAL_GRID = "Circle Hexagonal Grid";
	private static final String CALIB_CIRCLE_REGULAR_GRID = "Circle Regular Grid";

	private static final Font font = new Font("Serif", Font.BOLD, 14);

	private Class<I> imageClass;

	private ImagePanel panel = new ImagePanel();
	private ControlPanel controls = new ControlPanel();

	private FiducialDetector detector;

	private CameraPinholeRadial intrinsic;

	private FiducialStability stability = new FiducialStability();

	private List<FiducialInfo> fiducialInfo = new ArrayList<FiducialInfo>();
	private FiducialStability stabilityMax = new FiducialStability();

	private BufferedImage imageCopy;

	public FiducialTrackerDemoApp(List<PathLabel> examples, Class<I> imageType) {
		super(false,false,examples, ImageType.single(imageType));
		this.imageClass = imageType;

		panel.setPreferredSize(new Dimension(640, 480));
		panel.setFocusable(true);
		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				streamPaused = !streamPaused;
			}
		});

		add(BorderLayout.CENTER, panel);
		add(BorderLayout.WEST, controls);
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase input)
	{
		detector.detect((I)input);

		Graphics2D g2 = buffered.createGraphics();

		Se3_F64 targetToSensor = new Se3_F64();
		Point2D_F64 center = new Point2D_F64();
		Polygon2D_F64 polygon = new Polygon2D_F64();
		for (int i = 0; i < detector.totalFound(); i++) {
			long id = detector.getId(i);

			if( controls.showBoundary ) {
				detector.getBounds(i,polygon);
				g2.setStroke(new BasicStroke(11));
				g2.setColor(Color.BLUE);
				VisualizeShapes.drawPolygon(polygon,true,g2,true);
			}

			if( controls.showCenter ) {
				detector.getCenter(i,center);
				VisualizeFeatures.drawPoint(g2,center.x,center.y,10,Color.MAGENTA,true);
			}

			if( controls.show3D ) {
				detector.getFiducialToCamera(i, targetToSensor);
				double width = detector.getWidth(i);
				VisualizeFiducial.drawLabelCenter(targetToSensor, intrinsic, "" + id, g2);
				VisualizeFiducial.drawCube(targetToSensor, intrinsic, width, 5, g2);
			}

			if( controls.showStability ) {
				handleStability(input.height, g2, i, id);
			}

		}

		imageCopy = ConvertBufferedImage.checkCopy(buffered,imageCopy);
		panel.setImageUI(imageCopy);
	}

	@Override
	protected void configureVideo( int which , SimpleImageSequence sequence ) {
		sequence.setLoop(true);
	}

	/**
	 * Computes and visualizes the stability
	 */
	private void handleStability(int height, Graphics2D g2, int index, long fiducialID) {
		FiducialInfo info = findFiducial(fiducialID);
		info.totalObserved++;

		g2.setFont(font);
		if (detector.computeStability(index, 0.25, stability)) {
			stabilityMax.location = Math.max(stability.location, stabilityMax.location);
			stabilityMax.orientation = Math.max(stability.orientation, stabilityMax.orientation);
		}

		if (info.totalObserved > 20) {

			double fractionLocation = stability.location / stabilityMax.location;
			double fractionOrientation = stability.orientation / stabilityMax.orientation;

			int maxHeight = (int) (height * 0.15);

			int x = info.grid * 60;
			int y = 10 + maxHeight;

			g2.setColor(Color.BLUE);
			int h = (int) (fractionLocation * maxHeight);
			g2.fillRect(x, y - h, 20, h);

			g2.setColor(Color.CYAN);
			x += 25;
			h = (int) (fractionOrientation * maxHeight);
			g2.fillRect(x, y - h, 20, h);

			g2.setColor(Color.RED);
			g2.drawString("" + info.id, x - 20, y + 20);
		}
	}

	private FiducialInfo findFiducial( long id ) {
		for (int i = 0; i < fiducialInfo.size(); i++) {
			FiducialInfo info = fiducialInfo.get(i);
			if( info.id == id ) {
				return info;
			}
		}

		FiducialInfo found = new FiducialInfo();
		found.id = id;
		found.grid = fiducialInfo.size();
		fiducialInfo.add(found);

		return found;
	}

	@Override
	public void openExample( Object o ) {
		// stop everything because all the data structures about about to be changed
		stopAllInputProcessing();

		PathLabel example = (PathLabel)o;

		String name = example.label;

		String videoName = example.getPath();
		int location = videoName.lastIndexOf(File.separatorChar);
		if( location == -1 ) { // windows vs unix issue
			location = videoName.lastIndexOf('/');
		}
		String path = videoName.substring(0, location);

		ConfigThreshold configThreshold = ConfigThreshold.local(ThresholdType.LOCAL_MEAN, 21);

		boolean stability = true;

		if( name.compareTo(SQUARE_NUMBER) == 0 ) {
			detector = FactoryFiducial.squareBinary(new ConfigFiducialBinary(0.1), configThreshold, imageClass);
		} else if( name.compareTo(SQUARE_PICTURE) == 0 ) {
			double length = 0.1;
			detector = FactoryFiducial.squareImage(new ConfigFiducialImage(), configThreshold, imageClass);

			SquareImage_to_FiducialDetector<I> d = (SquareImage_to_FiducialDetector<I>)detector;

			String pathImg = new File(path,"../patterns").getPath();
			List<String> names = new ArrayList<>();
			names.add("chicken.png");
			names.add("yinyang.png");

			for( String foo : names ) {
				BufferedImage img = media.openImage(new File(pathImg,foo).getPath());
				if( img == null )
					throw new RuntimeException("Can't find file "+new File(pathImg,foo).getPath());
				d.addPatternImage(ConvertBufferedImage.convertFromSingle(img, null, imageClass), 125, length);
			}

		} else if( name.compareTo(CALIB_CHESS) == 0 ) {
			detector = FactoryFiducial.calibChessboard(new ConfigChessboard(7, 5, 0.03), imageClass);
		} else if( name.compareTo(CALIB_SQUARE_GRID) == 0 ) {
			detector = FactoryFiducial.calibSquareGrid(new ConfigSquareGrid(4, 3, 0.03, 0.03), imageClass);
		} else if( name.compareTo(CALIB_SQUARE_BINARY_GRID) == 0 ) {
			File configFile = new File(path,"description_4x3_3x3_4cm_2cm.txt");
			try {
				ConfigSquareGridBinary config =
						ConfigSquareGridBinary.parseSimple(new BufferedReader(new FileReader(configFile)));
				detector = FactoryFiducial.calibSquareGridBinary(config, imageClass);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else if( name.compareTo(CALIB_CIRCLE_HEXAGONAL_GRID) == 0 ) {
			stability = false;
			detector = FactoryFiducial.calibCircleHexagonalGrid(new ConfigCircleHexagonalGrid(24, 28, 1, 1.2), imageClass);
		} else if( name.compareTo(CALIB_CIRCLE_REGULAR_GRID) == 0 ) {
			stability = false;
			detector = FactoryFiducial.calibCircleRegularGrid(new ConfigCircleRegularGrid(10, 8, 1.5, 2.5), imageClass);
		} else {
			throw new RuntimeException("Unknown selection");
		}
		controls.setShowStability(stability);

		intrinsic = CalibrationIO.load(media.openFile(path+"/intrinsic.yaml"));

		detector.setLensDistortion(new LensDistortionRadialTangential(intrinsic),intrinsic.width,intrinsic.height);

		fiducialInfo.clear();
		// give it some initial values so that it doesn't look like there is huge errors right off the bat
		stabilityMax.location = 0.01;
		stabilityMax.orientation = 0.02;

		openVideo(false,videoName);
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height) {
		panel.setPreferredSize(new Dimension(width,height));
	}

	class ControlPanel extends StandardAlgConfigPanel implements ActionListener {
		private JCheckBox checkStability;
		private JCheckBox check3D;
		private JCheckBox checkBoundary;
		private JCheckBox checkCenter;

		// TODO add text info box

		boolean showStability = true;
		boolean show3D = true;
		boolean showBoundary = false;
		boolean showCenter = false;

		public ControlPanel() {
			checkStability = checkbox("Stability",showStability);
			check3D = checkbox("Box 3D",show3D);
			checkBoundary = checkbox("Boundary",showBoundary);
			checkCenter = checkbox("center",showCenter);

			addAlignLeft(checkStability);
			addAlignLeft(check3D);
			addAlignLeft(checkBoundary);
			addAlignLeft(checkCenter);
		}

		public void setShowStability(boolean value ) {
			showStability = value;
			BoofSwingUtil.invokeNowOrLater(()->checkStability.setSelected(value));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if( e.getSource() == checkStability ) {
				showStability = checkStability.isSelected();
			} else if( e.getSource() == check3D ) {
				show3D = check3D.isSelected();
			} else if( e.getSource() == checkBoundary ) {
				showBoundary = checkBoundary.isSelected();
			} else if( e.getSource() == checkCenter ) {
				showCenter = checkCenter.isSelected();
			}
		}
	}

	private class FiducialInfo {
		int totalObserved;
		long id;
		int grid;
	}

	public static void main(String[] args) {
//		Class type = GrayF32.class;
		Class type = GrayU8.class;

		java.util.List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel(SQUARE_NUMBER, UtilIO.pathExample("fiducial/binary/movie.mjpeg")));
		inputs.add(new PathLabel(SQUARE_PICTURE, UtilIO.pathExample("fiducial/image/video/movie.mjpeg")));
		inputs.add(new PathLabel(CALIB_CHESS, UtilIO.pathExample("fiducial/chessboard/movie.mjpeg")));
		inputs.add(new PathLabel(CALIB_SQUARE_GRID, UtilIO.pathExample("fiducial/square_grid/movie.mp4")));
//		inputs.add(new PathLabel(CALIB_SQUARE_BINARY_GRID, UtilIO.pathExample("fiducial/binary_grid/movie.mp4")));
		inputs.add(new PathLabel(CALIB_CIRCLE_HEXAGONAL_GRID, UtilIO.pathExample("fiducial/circle_hexagonal/movie.mp4")));
		inputs.add(new PathLabel(CALIB_CIRCLE_REGULAR_GRID, UtilIO.pathExample("fiducial/circle_regular/movie.mp4")));


		FiducialTrackerDemoApp app = new FiducialTrackerDemoApp(inputs,type);

		app.openExample(inputs.get(0));
		app.display("Fiducial Demonstrations");
	}
}

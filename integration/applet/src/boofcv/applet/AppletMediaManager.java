/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.applet;

import boofcv.io.MediaManager;
import boofcv.io.VideoCallBack;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.video.VideoMjpegCodec;
import boofcv.io.wrapper.images.ImageStreamSequence;
import boofcv.io.wrapper.images.JpegByteImageSequence;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class AppletMediaManager implements MediaManager {
	URL codebase;

	String homeDirectory = "";

	Component ownerGUI;
	boolean done;

	int totalDownloaded;

	public AppletMediaManager(URL codebase) {
		this.codebase = codebase;
	}

	public void setHomeDirectory(String homeDirectory) {
		this.homeDirectory = homeDirectory;
	}

	public void setOwnerGUI(Component ownerGUI) {
		this.ownerGUI = ownerGUI;
	}

	@Override
	public Reader openFile(String fileName) {
		try {
			fileName = homeDirectory + fileName;

			return new InputStreamReader(new URL(codebase,fileName).openStream());
		} catch (IOException e) {
			System.err.println("IOException opening file "+fileName);
			System.err.println("  homeDirectory = "+homeDirectory);
			return null;
		}
	}

	@Override
	public BufferedImage openImage(String fileName) {
		URL url = null;
		try {
			fileName = homeDirectory + fileName;

			url = new URL(codebase, fileName);
			return ImageIO.read(url);
		} catch (MalformedURLException e) {
			System.err.println("MalformedURL"+fileName);
		} catch (IOException e) {
			System.err.println("IOException opening image = "+fileName);
			System.err.println("  homeDirectory = "+homeDirectory);
		}
		return null;
	}

	@Override
	public <T extends ImageBase> SimpleImageSequence<T> openVideo(String fileName, ImageType<T> imageType) {

		int where = fileName.indexOf('.');

		String type = fileName.substring(where+1);
		URL url = null;
		BufferedInputStream stream = null;

		int fileSize = -1;

		try {
			url = new URL(codebase, homeDirectory + fileName);
			fileSize = url.openConnection().getContentLength();
			stream = new BufferedInputStream(url.openStream());
		} catch (MalformedURLException e) {
			System.err.println("MalformedURL"+fileName);
			return null;
		} catch (IOException e) {
			System.err.println("IOException opening video = "+fileName);
			System.err.println("  homeDirectory = "+homeDirectory);
			return null;
		}

		// first read in the data byte stream and update the download status while doing so
		ByteArrayInputStream byteStream;
		try {
			byteStream = readNetworkData(stream,fileSize);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// now convert it into some other format
		if( type.compareToIgnoreCase("MJPEG") == 0 ) {
			VideoMjpegCodec codec = new VideoMjpegCodec();
			List<byte[]> data = codec.read(byteStream);
//			System.out.println("Loaded "+data.size()+" jpeg images!");
			return new JpegByteImageSequence<T>(imageType,data,true);
		} else if( type.compareToIgnoreCase("MPNG") == 0 ) {
			return new ImageStreamSequence<T>(byteStream,true,imageType);
		} else {
			System.err.println("Unknown video type: "+type);
		}

		return null;
	}

	@Override
	public <T extends ImageBase> boolean openCamera(String device, int width, int height, VideoCallBack<T> callback) {
		return false;
	}

	private ByteArrayInputStream readNetworkData(InputStream streamIn , int fileSize ) throws IOException {
		totalDownloaded = 0;
		done = false;
		new ProcessThread(fileSize).start();

		ByteArrayOutputStream temp = new ByteArrayOutputStream(1024);
		byte[] data = new byte[ 1024 ];
		int length;
		while( ( length = streamIn.read(data)) != -1 ) {
			totalDownloaded += length;
			temp.write(data,0,length);
		}
		done = true;
		return new ByteArrayInputStream(temp.toByteArray());
	}

	/**
	 * Displays a progress monitor and updates its state periodically
	 */
	public class ProcessThread extends Thread
	{
		ProgressMonitor progressMonitor;
		public ProcessThread( int fileSize ) {
			String text = String.format("Loading Data %d KiB",(fileSize/1024));
			progressMonitor = new ProgressMonitor(ownerGUI, text, "", 0, fileSize);
		}

		@Override
		public void run() {
			while( !done ) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						progressMonitor.setProgress(totalDownloaded);
					}});
				synchronized ( this ) {
					try {
						wait(100);
					} catch (InterruptedException e) {
					}
				}
			}
			progressMonitor.close();
		}
	}
}

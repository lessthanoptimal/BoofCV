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

package boofcv.io.gstwebcamcapture;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.PadProbeType;
import org.freedesktop.gstreamer.device.Device;
import org.freedesktop.gstreamer.device.DeviceMonitor;

/**
 * GStreamer Manager responsible for control of all pipelines and starting and stopping cameras
 * @author Devin Willis
 */
public class GStreamerManager implements CameraOpenListenerInterface, CameraClosedListenerInterface, Runnable {

    public static void main(String[] args) throws Exception {
        Webcam camOne = Webcam.getCamera("/dev/video0");
        camOne.setViewSize(new Dimension(640, 480));
        Webcam camTwo = Webcam.getCamera("/dev/video2");
        camTwo.setViewSize(new Dimension(640, 480));
        camOne.open();
        camTwo.open();
    }

    private ArrayList<GStreamerCameraInterface> cameras = new ArrayList<>();
    private static GStreamerManager currentManager;
    private boolean pipelinesStarted = false;
    private boolean startPipeline = false;
    private DeviceMonitor dm;

    /**
     * Creates new GStreamerManager and initializes a new DeviceMonitor
     */
    GStreamerManager() {
        dm = new DeviceMonitor();
    }

    /**
     * Method to get static reference of GStreamerManager
     * @return GStreamerManager
     */
    public static GStreamerManager getManager() {
        if (currentManager == null) {
            Gst.init("GSTManager", "");
            currentManager = new GStreamerManager();
            Thread thread = new Thread(currentManager);
            thread.start();
        }
        return currentManager;
    }

    /**
     * Add camera to GStreamerManager
     * @param cam
     * @return 
     */
    public boolean addCamera(GStreamerCameraInterface cam) {
        cameras.add(cam);
        return true;
    }

    /**
     * Start Pipeline
     */
    public void pipelineStart() {
        if (pipelinesStarted == false) {
            startPipeline = true;
        } else {
            Gst.quit();
            pipelinesStarted = false;
            startPipeline = true;
        }
    }

    /**
     * Get list of video/source devices found by GStreamer
     * @return 
     */
    public List<Device> getDevices() {
        int filterId = dm.addFilter("Video/Source", null);
        dm.start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(GStreamerManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        List<Device> devices = dm.getDevices();
        dm.stop();
        dm.removeFilter(filterId);
        return devices;
    }

    /**
     * Camera opened called event that restarts the pipeline
     * @param cam 
     */
    @Override
    public void cameraOpened(GStreamerCameraInterface cam) {
        addCamera(cam);
        pipelineStart();
    }

    /**
     * Main thread running the GST main loop
     */
    @Override
    public void run() {
        while (true) {
            if (startPipeline) {
                startPipeline = false;
                pipelinesStarted = true;
                for (GStreamerCameraInterface cam : cameras) {
                    Pipeline pipeline = cam.getPipeline();
                    if (pipeline == null) {
                        System.out.println(cam.getPipelineString());
                        pipeline = (Pipeline) Gst.parseLaunch(cam.getPipelineString());
                        Element identity = pipeline.getElementByName(cam.getName());

                        GStreamerImagePipeline imagePipeline = new GStreamerImagePipeline(cam.getResolution().width, cam.getResolution().height, identity);
                        cam.setImagePipeline(imagePipeline);

                        identity.getStaticPad("sink").addProbe(PadProbeType.BUFFER, cam.getImagePipeline());
                        if(cam instanceof ImageListener){
                            cam.getImagePipeline().addImageListener((ImageListener) cam);
                        }

                        cam.setPipeline(pipeline);
                        cam.getPipeline().getBus().connect((Bus.ERROR) ((source, code, message) -> {
                            System.out.println(message);
                            Gst.quit();
                        }));
                        cam.getPipeline().getBus().connect((Bus.EOS) (source) -> Gst.quit());
                        cam.getPipeline().play();
                    }
                }
                Gst.main();
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Logger.getLogger(GStreamerManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Camera closed called event that removes the camera from the pipeline
     * @param cam 
     */
    @Override
    public void cameraClosed(GStreamerCameraInterface cam) {
        Pipeline pipeline = cam.getPipeline();
        if(pipeline!=null){
            if(pipeline.isPlaying()){
                pipeline.stop();
                pipeline.close();
                cam.setImagePipeline(null);
            }
        }
        cameras.remove(cam);
    }
}

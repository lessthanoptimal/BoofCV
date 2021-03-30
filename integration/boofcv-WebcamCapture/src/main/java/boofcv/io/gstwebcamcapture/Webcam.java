/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package boofcv.io.gstwebcamcapture;

import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.WebcamInterface;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.freedesktop.gstreamer.Caps;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Structure;
import org.freedesktop.gstreamer.device.Device;
import org.freedesktop.gstreamer.device.DeviceMonitor;

/**
 *
 * @author Devin Willis
 */
public class Webcam implements GStreamerCameraInterface, ImageListener, ImageProducer {

    private Pipeline cameraPipeline;
    private GStreamerImagePipeline imagePipeline;
    private Dimension resolution;
    private ArrayList<CameraOpenListenerInterface> cameraOpenListeners = new ArrayList<>();
    private ArrayList<CameraClosedListenerInterface> cameraCloseListeners = new ArrayList<>();
    private String stringPipeline;
    private String cameraName;
    private Dimension[] supportedResolutions;
    private BufferedImage currentImg;

    /**
     * Creates new webcam given a webcam name
     * @param deviceName camera name on computer
     */
    Webcam(String deviceName) {
        cameraName = deviceName;
        this.addCameraOpenListener(GStreamerManager.getManager());
        this.addCameraCloseListener(GStreamerManager.getManager());
    }

    /**
     * Gets and instantiates a list of webcams for every video source on the computer
     * @return 
     */
    public static List<Webcam> getWebcams() {
        List<Device> devices = GStreamerManager.getManager().getDevices();
        List<Webcam> webcams = new ArrayList<>();
        for (int w = 0; w < devices.size(); w++) {
            Webcam cam = Webcam.getCamera(devices.get(w).getProperties().getString("device.path"));

            Caps caps = devices.get(w).getCaps();
            Dimension[] resolutions = new Dimension[caps.size()];
            for (int i = 0; i < caps.size(); i++) {
                Structure structure = caps.getStructure(i);
                resolutions[i] = new Dimension(structure.getInteger("width"), structure.getInteger("height"));
            }

            cam.setCustomViewSizes(resolutions);
            webcams.add(cam);
        }
        return webcams;
    }

    /**
     * Gets all webcams on system and returns the first in the list
     * @return 
     */
    public static Webcam getDefault() {
        return getWebcams().get(0);
    }

    /**
     * Static method to create and return a webcam with the given name
     * @param deviceName
     * @return 
     */
    public static Webcam getCamera(String deviceName) {
        return new Webcam(deviceName);
    }

    /**
     * Construct GStreamer string given camera parameters
     */
    private void constructString() {
        if (OSHelper.isWindows()) {
            String C270CameraCaps = "video/x-raw, width=" + resolution.width + ", height=" + resolution.height + ", format=RGBA";
            String sinkSource = "! fakesink";
            stringPipeline = "ksvideosrc device-path=\\\\\\\\\\?\\\\usb\\#vid_046d\\&pid_0825\\&mi_00\\#8\\&3680356\\&0\\&0000\\#\\{6994ad05-93ef-11d0-a3cc-00a0c9223196\\}\\\\global ! videoconvert ! videoscale ! video/x-raw,width=" + resolution.width + ", height=" + resolution.height + ", format=NV12 ! queue max-size-buffers=1 leaky=downstream ! videoconvert ! " + C270CameraCaps + " ! identity name=left " + sinkSource;
        } else {
            String C270CameraCapsFirst = "video/x-raw, width=" + resolution.width + ", height=" + resolution.height + ", format=YUY2"; //RGBA
            String C270CameraCapsSecond = "video/x-raw, format=RGBA"; //RGBA
            String sinkSource = "! fakesink";
            stringPipeline = "v4l2src device=" + cameraName + " ! " + C270CameraCapsFirst + " ! queue max-size-buffers=1 leaky=downstream ! videoconvert ! " + C270CameraCapsSecond + " ! identity name=" + cameraName + " " + sinkSource;
        }
    }

    /**
     * Opens camera by calling all camera open listeners
     * @return 
     */
    @Override
    public boolean open() {
        if (resolution == null) {
            resolution = supportedResolutions[0];
        }
        constructString();
        for (CameraOpenListenerInterface listener : cameraOpenListeners) {
            listener.cameraOpened(this);
        }
        return true;
    }

    /**
     * Return current pipeline object
     * @return 
     */
    @Override
    public Pipeline getPipeline() {
        return cameraPipeline;
    }

    /**
     * Set current pipeline
     * @param pipeline
     * @return
     */
    @Override
    public boolean setPipeline(Pipeline pipeline) {
        cameraPipeline = pipeline;
        return true;
    }

    /**
     * Add new camera open listener
     * @param listener
     * @return 
     */
    @Override
    public boolean addCameraOpenListener(CameraOpenListenerInterface listener) {
        cameraOpenListeners.add(listener);
        return true;
    }

    /**
     * Return generated GST Pipeline string
     * @return 
     */
    @Override
    public String getPipelineString() {
        return stringPipeline;
    }

    /**
     * Return name of camera
     * @return 
     */
    @Override
    public String getName() {
        return cameraName;
    }

    /**
     * Return current image pipeline which generates images
     * @return 
     */
    @Override
    public GStreamerImagePipeline getImagePipeline() {
        return imagePipeline;
    }

    /**
     * Set current image pipeline for handling images from pipeline
     * @param imagePipeline
     * @return 
     */
    @Override
    public boolean setImagePipeline(GStreamerImagePipeline imagePipeline) {
        this.imagePipeline = imagePipeline;
        return true;
    }

    /**
     * Return current resolution of the camera
     * @return 
     */
    @Override
    public Dimension getResolution() {
        return resolution;
    }

    /**
     * Return list of potential resolutions
     * @return 
     */
    @Override
    public Dimension[] getCustomViewSizes() {
        return supportedResolutions;
    }

    /**
     * Set list of potential resolutions
     * @param dimensionList
     * @return 
     */
    @Override
    public boolean setCustomViewSizes(Dimension[] dimensionList) {
        supportedResolutions = dimensionList;
        return true;
    }

    /**
     * Set current resolution
     * @param resolution
     * @return 
     */
    @Override
    public boolean setViewSize(Dimension resolution) {
        if (supportedResolutions != null) {
            for (Dimension dimension : supportedResolutions) {
                if (resolution.equals(dimension)) {
                    this.resolution = dimension;
                }
            }
        } else {
            this.resolution = resolution;
        }
        return true;
    }

    /**
     * Called when a new image has been received by the Image Pipeline
     * @param image 
     */
    @Override
    public void newImage(BufferedImage image) {
        currentImg = image;
        for (ImageListener listener : imageListeners) {
            listener.newImage(image);
        }
    }

    private ArrayList<ImageListener> imageListeners = new ArrayList<>();

    /**
     * Adds a new image listener that receives all new images
     * @param imageListener
     * @return 
     */
    @Override
    public ImageProducer addImageListener(ImageListener imageListener) {
        imageListeners.add(imageListener);
        return this;
    }

    /**
     * Return instance of camera device
     * @return 
     */
    @Override
    public GStreamerCameraInterface getDevice() {
        return this;
    }

    /**
     * Return an image per request
     * @return 
     */
    @Override
    public BufferedImage getImage() {
        while(currentImg==null){
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Logger.getLogger(Webcam.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return currentImg;
    }

    /**
     * Close camera by calling all camera closed interfaces
     * @return 
     */
    @Override
    public boolean close() {
        for (CameraClosedListenerInterface listener : cameraCloseListeners) {
            listener.cameraClosed(this);
        }
        return true;
    }

    /**
     * Add new camera closed listener
     * @param manager
     * @return 
     */
    @Override
    public boolean addCameraCloseListener(CameraClosedListenerInterface manager) {
        cameraCloseListeners.add(manager);
        return true;
    }

    /**
     * Get current resolution
     * @return 
     */
    @Override
    public Dimension getViewSize() {
        return this.getResolution();
    }

}

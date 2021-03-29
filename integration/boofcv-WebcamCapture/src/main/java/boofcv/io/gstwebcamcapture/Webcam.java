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
 * @author techgarage
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

    Webcam(String deviceName) {
        cameraName = deviceName;
        this.addCameraOpenListener(GStreamerManager.getManager());
        this.addCameraCloseListener(GStreamerManager.getManager());
    }

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

    public static Webcam getDefault() {
        return getWebcams().get(0);
    }

    public static Webcam getCamera(String deviceName) {
        return new Webcam(deviceName);
    }

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

    @Override
    public Pipeline getPipeline() {
        return cameraPipeline;
    }

    /**
     *
     * @param pipeline
     * @return
     */
    @Override
    public boolean setPipeline(Pipeline pipeline) {
        cameraPipeline = pipeline;
        return true;
    }

    @Override
    public boolean addCameraOpenListener(CameraOpenListenerInterface listener) {
        cameraOpenListeners.add(listener);
        return true;
    }

    @Override
    public String getPipelineString() {
        return stringPipeline;
    }

    @Override
    public String getName() {
        return cameraName;
    }

    @Override
    public GStreamerImagePipeline getImagePipeline() {
        return imagePipeline;
    }

    @Override
    public boolean setImagePipeline(GStreamerImagePipeline imagePipeline) {
        this.imagePipeline = imagePipeline;
        return true;
    }

    @Override
    public Dimension getResolution() {
        return resolution;
    }

    @Override
    public Dimension[] getCustomViewSizes() {
        return supportedResolutions;
    }

    @Override
    public boolean setCustomViewSizes(Dimension[] dimensionList) {
        supportedResolutions = dimensionList;
        return true;
    }

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

    @Override
    public void newImage(BufferedImage image) {
        currentImg = image;
        for (ImageListener listener : imageListeners) {
            listener.newImage(image);
        }
    }

    private ArrayList<ImageListener> imageListeners = new ArrayList<>();

    @Override
    public ImageProducer addImageListener(ImageListener imageListener) {
        imageListeners.add(imageListener);
        return this;
    }

    @Override
    public GStreamerCameraInterface getDevice() {
        return this;
    }

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

    @Override
    public boolean close() {
        for (CameraClosedListenerInterface listener : cameraCloseListeners) {
            listener.cameraClosed(this);
        }
        return true;
    }

    @Override
    public boolean addCameraCloseListener(CameraClosedListenerInterface manager) {
        cameraCloseListeners.add(manager);
        return true;
    }

    @Override
    public Dimension getViewSize() {
        return this.getResolution();
    }

}

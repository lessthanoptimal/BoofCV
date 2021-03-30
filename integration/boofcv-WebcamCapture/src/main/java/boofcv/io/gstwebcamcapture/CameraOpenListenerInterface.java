/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package boofcv.io.gstwebcamcapture;

/**
 * Interface that can listen to cameraOpened events from a webcam
 * 
 * @author Devin Willis
 */
public interface CameraOpenListenerInterface {
    public void cameraOpened(GStreamerCameraInterface cam);
}

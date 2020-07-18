///////////////////////////////////////////////////////////////////////////////
//PROJECT:       PWS Plugin for Micro-Manager
//
//-----------------------------------------------------------------------------
//
// AUTHOR:      Nick Anthony 2019
//
// COPYRIGHT:    Northwestern University, Evanston, IL 2019
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
//

package edu.bpl.pwsplugin.acquisitionManagers;

import edu.bpl.pwsplugin.Globals;
import edu.bpl.pwsplugin.fileSavers.ImSaverRaw;
import edu.bpl.pwsplugin.PWSAlbum;
import edu.bpl.pwsplugin.fileSavers.MMSaver;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import org.micromanager.internal.utils.ReportingUtils;
import java.util.concurrent.LinkedBlockingQueue;
import mmcorej.StrVector;
import org.micromanager.Studio;
import org.micromanager.data.Image;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import mmcorej.org.json.JSONArray;
import mmcorej.org.json.JSONObject;
import org.micromanager.data.Coords;
import org.micromanager.data.Pipeline;
import org.micromanager.data.PipelineErrorException;


public class PWSAcqManager implements AcquisitionManager{
    int[] wv; //The array of wavelengths to image at.
    String filtLabel; // The string identifying the spectral filter device of the pws system in micromanager.
    final String filtProp  = "Wavelength"; //The property name of the filter that we want to tune.
    Boolean hardwareSequence; // Whether or not to attempt to use TTL triggering between the camera and spectral filter.
    Boolean useExternalTrigger; // Whether or not to let the spectral filter TTL trigger a new camera frame when it is done tuning.
    double exposure_; // The camera exposure.
    PWSAlbum album_;
    
    public PWSAcqManager(PWSAlbum album) {
        album_ = album;
    }
    
    public void setSequenceSettings(double exposure, boolean externalTrigger, 
            boolean hardwareTrigger, int[] Wv, String filterLabel) throws Exception {
        exposure_ = exposure;
        useExternalTrigger = externalTrigger;
        wv = Wv;
        filtLabel = filterLabel;
        hardwareSequence =  hardwareTrigger;           
        
        if (hardwareSequence) {
            if (!Globals.mm().core().isPropertySequenceable(filtLabel, filtProp)){
                throw new Exception("The filter device does not have a sequenceable 'Wavelength' property.");
            }
            if (Globals.mm().core().getPropertySequenceMaxLength(filtLabel, filtProp) < wv.length) {
                throw new Exception("The filter device does not support sequencing as many wavelengths as have been specified. Max is " + Integer.toString(Globals.core().getPropertySequenceMaxLength(filtLabel, filtProp)));
            }
            StrVector strv = new StrVector();
            for (int i = 0; i < wv.length; i++) {   //Convert wv from int to string for sending to the device.
                strv.add(String.valueOf(wv[i]));
            }
            Globals.mm().core().loadPropertySequence(filtLabel, filtProp, strv);
        }  
    }
    
    @Override
    public int getExpectedFrames() {
        return wv.length;
    }
    
    @Override
    public String getFilePrefix() {
        return "pws";
    }
    
    @Override
    public String getSavePath(String savePath, int cellNum) throws FileAlreadyExistsException{
        Path path = Paths.get(savePath).resolve("Cell" + String.valueOf(cellNum)).resolve("PWS");
        if (Files.isDirectory(path)){
            throw new FileAlreadyExistsException("Cell " + cellNum + " PWS already exists.");
        } 
        return path.toString();
    }
      
    @Override
    public void acquireImages(String savePath, int cellNum, LinkedBlockingQueue imagequeue, JSONObject metadata) {
        try {album_.clear();} catch (IOException e) {ReportingUtils.logError(e, "Error from PWSALBUM");}
        double initialWv = 550;
        try {    
            initialWv = Double.valueOf(Globals.core().getProperty(filtLabel, filtProp)); //Get initial wavelength
            String cam = Globals.core().getCameraDevice();
            Globals.core().waitForDevice(cam);
            Globals.core().clearCircularBuffer();     
            Globals.core().setExposure(cam, exposure_);
            Pipeline pipeline = Globals.mm().data().copyApplicationPipeline(Globals.mm().data().createRAMDatastore(), true);
            
            //Prepare metadata and start imsaver
            JSONArray WV = new JSONArray();
            for (int i = 0; i < wv.length; i++) {
                WV.put(wv[i]);
            }        
            metadata.put("wavelengths", WV);
            metadata.put("exposure", Globals.core().getExposure()); //This must happen after we have set the camera to our desired exposure.
            MMSaver imSaver_ = new MMSaver(this.getSavePath(savePath, cellNum), imagequeue, this.getExpectedFrames(), this.getFilePrefix());
            imSaver_.setMetadata(metadata);
            imSaver_.start();
            
            long now = System.currentTimeMillis();
            
            if (hardwareSequence) {
                String origCameraTrigger="";  
                if (Globals.core().getDeviceName(cam).equals("HamamatsuHam_DCAM")){
                    origCameraTrigger = Globals.core().getProperty(cam, "TRIGGER SOURCE");
                }
                try {
                    double delayMs = Globals.core().getDeviceDelayMs(filtLabel); //Use the delay defined by the tunable filter's device adapter.
                    if (useExternalTrigger) {
                        if (Globals.core().getDeviceName(cam).equals("HamamatsuHam_DCAM")) { 
                            Globals.core().setProperty(cam, "TRIGGER SOURCE", "EXTERNAL");
                            Globals.core().setProperty(cam, "TRIGGER DELAY", delayMs/1000); //This is in units of seconds.
                            Globals.core().startSequenceAcquisition(wv.length, 0, false); //The hamamatsu adapter throws an eror if the interval is not 0.
                            int currWv = Integer.parseInt(Globals.core().getProperty(filtLabel, filtProp));
                            //Globals.core().setProperty(filtLabel, filtProp, currWv+1); //Trigger a pulse which sets the whole thing off.
                            Globals.core().startPropertySequence(filtLabel, filtProp); //This should trigger a pulse which sets the whole thing off.
                        }   
                    }
                    else { //Since we're not using an external trigger we need to have the camera control the timing.
                        double exposurems = Globals.core().getExposure();
                        double readoutms = 10; //This is based on the frame rate calculation portion of the 13440-20CU camera. 9.7 us per line, reading two lines at once, 2048 lines -> 0.097*2048/2 ~= 10 ms
                        double intervalMs = (exposurems+readoutms+delayMs);
                        Globals.core().startPropertySequence(filtLabel, filtProp);
                        if (Globals.core().getDeviceName(cam).equals("HamamatsuHam_DCAM")) { //This device adapter doesn't seem to support delays in the sequence acquisition. We instead set the master pulse interval.
                            Globals.core().setProperty(cam, "TRIGGER SOURCE", "MASTER PULSE"); //Make sure that Master Pulse is triggering the camera.
                            Globals.core().setProperty(cam, "MASTER PULSE INTERVAL", intervalMs/1000.0); //In units of seconds
                            Globals.core().startSequenceAcquisition(wv.length, 0, false); //The hamamatsu adapter throws an error if the interval is not 0.
                        } else{
                            Globals.core().startSequenceAcquisition(wv.length, intervalMs, false); //Supposedly having a non-zero interval acqually only works for Andor cameras.
                        }
                    }
                    boolean canExit = false;
                    int i = 0;
                    int oldi = -1;
                    long lastImTime = System.currentTimeMillis();
                    while (true) {
                        boolean remaining = (Globals.core().getRemainingImageCount() > 0);
                        boolean running = (Globals.core().isSequenceRunning(cam));
                        if ((!remaining) && (canExit)) {
                            break;  //Everything is taken care of.
                        }
                        if (remaining) {    //Process images
                            Image im = Globals.mm().data().convertTaggedImage(Globals.core().popNextTaggedImage());
                            addImage(im, i, album_, pipeline, imSaver_.queue);
                            i++;
                            lastImTime = System.currentTimeMillis();
                        }
                        if ((System.currentTimeMillis() - lastImTime) > 10000) { //Check for timeout if for some reason the acquisition is stalled.
                            ReportingUtils.showError("PWSAcquisition timed out while waiting for images from camera.");
                            canExit = true;
                        }
                        if (!running) {
                            canExit = true;
                        }
                     }
                }
                finally {
                    Globals.core().stopSequenceAcquisition();
                    if (Globals.core().getDeviceName(cam).equals("HamamatsuHam_DCAM")){
                        Globals.core().setProperty(cam, "TRIGGER SOURCE", origCameraTrigger); //Set the trigger source back ot what it was originally
                    }
                    try {
                        Globals.core().stopPropertySequence(filtLabel, filtProp);//Got to make sure to stop the sequencing behaviour.
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        ReportingUtils.logMessage("ERROR: PWSPlugin: Stopping property sequence: " + ex.getMessage());
                    }
                }
            }
            else {  //Software sequenced acquisition
                for (int i=0; i<wv.length; i++) {
                    Globals.core().setProperty(filtLabel, filtProp, wv[i]);
                    while (Globals.core().deviceBusy(filtLabel)) {Thread.sleep(1);} //Wait until the device says it is tuned.
                    Globals.core().snapImage();
                    Image im = Globals.mm().data().convertTaggedImage(Globals.core().getTaggedImage());
                    addImage(im, i, album_, pipeline, imSaver_.queue);
                }
            }
            long itTook = System.currentTimeMillis() - now;  
            imSaver_.join();
        } catch (Exception ex) {
            ex.printStackTrace();
            ReportingUtils.logMessage("ERROR: PWSPlugin: " + ex.getMessage());
        } finally {
            try{
                Globals.core().setProperty(filtLabel, filtProp, initialWv); //Set back to initial wavelength
            } catch (Exception ex) {
                ReportingUtils.showError(ex);
                ex.printStackTrace();
                ReportingUtils.logMessage("ERROR: PWSPlugin: " + ex.getMessage());
            }
        }
    }
    
    
    private void addImage(Image im, int idx, PWSAlbum album, Pipeline pipeline, LinkedBlockingQueue imageQueue) throws IOException, PipelineErrorException{
        Coords newCoords = im.getCoords().copyBuilder().t(idx).build();
        im = im.copyAtCoords(newCoords);
        pipeline.insertImage(im); //Add image to the data pipeline for processing
        im = pipeline.getDatastore().getImage(newCoords); //Retrieve the processed image.
        album.addImage(im); //Add the image to the album for display
        imageQueue.add(im); //Add the image to a queue for multithreaded saving.
    }
}
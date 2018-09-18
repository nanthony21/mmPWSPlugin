package edu.bpl.pwsplugin;

/*
 * Copyright © 2009 – 2013, Marine Biological Laboratory
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of 
 * the authors and should not be interpreted as representing official policies, 
 * either expressed or implied, of any organization.
 * 
 * Multiple-Frame Averaging plug-in for Micro-Manager
 * @author Amitabh Verma (averma@mbl.edu), Grant Harris (gharris@mbl.edu)
 * Marine Biological Laboratory, Woods Hole, Mass.
 * 
 */
import org.micromanager.internal.utils.MDUtils;
import org.micromanager.internal.utils.ReportingUtils;

import org.micromanager.data.Processor;
import org.micromanager.PropertyMap;
import org.micromanager.Studio;
import org.micromanager.data.ProcessorContext;
import org.micromanager.data.Image;
import org.micromanager.data.SummaryMetadata;
import org.micromanager.acquisition.internal.TaggedImageQueue;
import org.micromanager.data.Coords;
import org.micromanager.data.Metadata;
import org.micromanager.data.internal.DefaultImage;

public class PWSProcessor extends Processor {
 
    Studio studio_;
    int numAverages_;
    TaggedImageQueue imageQueue;
    boolean debugLogEnabled_ = true;
    Image[] imageArray;
    Integer[] wv;
    String filtLabel;
    String filtProp;
    
    public PWSProcessor(Studio studio, PropertyMap settings) {
        studio_ = studio;
        wv = settings.getIntArray("wv");
        filtLabel = settings.getString("filtLabel");
        filtProp = "Wavelength";
        imageArray = new Image[wv.length];
        studio_.acquisitions().attachRunnable(-1, -1, -1, -1, new PWSRunnable(this)); 
        imageQueue = new TaggedImageQueue();
        
        try {
            if (!studio_.core().isPropertySequenceable(filtLabel, filtProp)){
                ReportingUtils.showError("The filter device does not have a sequenceable 'Wavelength' property.");
            }
            if (studio_.core().getPropertySequenceMaxLength(filtLabel, filtProp) < wv.length) {
                ReportingUtils.showError("The filter device does not support sequencing as many wavelenghts as have been specified. Max is " + Integer.toString(studio_.core().getPropertySequenceMaxLength(filtLabel, filtProp)));
            }
        }
        catch (Exception ex) {
            ReportingUtils.showError(ex);
        }
                    
    }
    
    @Override
    public void cleanup(ProcessorContext context) {
            studio_.acquisitions().clearRunnables();
    }
    
    @Override
    public SummaryMetadata processSummaryMetadata(SummaryMetadata metadata) {
        SummaryMetadata.SummaryMetadataBuilder builder = metadata.copy();
        builder.userName("PWSAcquisition");
        return builder.build();
    }
    
    @Override
    public void processImage(Image image, ProcessorContext context) {
        Image imageOnError = image;
        try {  
            if (studio_.acquisitions().isAcquisitionRunning()) {
                if (debugLogEnabled_) {
                    ReportingUtils.logMessage("Queue has" + Integer.toString(imageQueue.size()));
                }
                int i = 0;  //The original image is just going to be thrown out :( .
                while (!imageQueue.isEmpty()) {
                    imageArray[i++] = studio_.data().convertTaggedImage(imageQueue.take()); //Lets make an array with the queued images.
                }
            }
            // Only applies for Live - MultiD and Snap collect images elsewhere (in Runnable and Poison-Image-Delay thread)
            // when in Live collect (n-1) required images from stream
            // when averaging array is filled skip this step and continue to
            // compute and produce avg. image
            else if (studio_.live().getIsLiveModeOn()) { //Rolling average for live mode.
                for (int i = 1; i < numAverages_; i++) { 
                    imageArray[i-1] = imageArray[i];
                }
                imageArray[numAverages_-1] = image;
            }
            // if we are not in a state where we have acquired some frames for averaging
            // this case would be for Snap or end of Live routine where additional images
            // are needed to be acquired to fill the averaging array
            // a Poison image indicates EOL
            else { //We are not in an acquisition or live mode so we must be taking a snap shot.
                acquireImages();
                imageArray[0] = image;
                int i = 1;
                while (!imageQueue.isEmpty()) {
                    imageArray[i++] = studio_.data().convertTaggedImage(imageQueue.take()); //Lets make an array with the acquisition image first and then the queued images after.
                }
            }                                   
            Image avg = getAverage(imageArray); // on to computing avg. frame
            context.outputImage(avg);
        } catch (Exception ex) {
            context.outputImage(imageOnError);            
            ReportingUtils.logError("FrameAvg, in Process: " + ex.toString());
            imageQueue.clear();
        }
    }

    private Image getAverage(Image[] imArray) {
        try {
            if (debugLogEnabled_) {
                ReportingUtils.logMessage("FrameAvg: computing...");
            }
            int width = imArray[0].getHeight();
            int height = imArray[0].getWidth();
            int imgDepth = imArray[0].getBytesPerPixel();
            
            if (imgDepth > 2) {
                studio_.logs().showError("Frame Averager Plugin does not support images with greater than 16 bit bitdepth.");
            }

            int dimension = width * height;
            byte[] pixB;
            byte[] retB = new byte[dimension];
            short[] pixS;
            short[] retS = new short[dimension];
            float[] retF = new float[dimension];
            Object result = null;

            for (int i = 0; i < imArray.length; i++) {
                if (imgDepth == 1) {
                    pixB = (byte[]) imArray[i].getRawPixels();
                    for (int j = 0; j < dimension; j++) {
                        retF[j] = (float) (retF[j] + (int) (pixB[j] & 0xff));
                    }
                } else if (imgDepth == 2) {
                    pixS = (short[]) imArray[i].getRawPixels();
                    for (int j = 0; j < dimension; j++) {
                        retF[j] = (float) (retF[j] + (int) (pixS[j] & 0xffff));
                    }
                }
            }
            if (imgDepth == 1) {
                for (int j = 0; j < dimension; j++) {
                    retB[j] = (byte) (int) (retF[j] / imArray.length);
                }
                result = retB;
            } else if (imgDepth == 2) {
                for (int j = 0; j < dimension; j++) {
                    retS[j] = (short) (int) (retF[j] / imArray.length);
                }
                result = retS;
            }

            Metadata md = imArray[0].getMetadata();
            Coords co = imArray[0].getCoords();
            Image averagedImage = new DefaultImage(result,width,height,imgDepth,1,co,md);
            if (debugLogEnabled_) {
                ReportingUtils.logMessage("FrameAvg: produced averaged image");
            }
            return averagedImage;

        } catch (Exception ex) {
            ReportingUtils.logError("Error: FrameAvg, while producing averaged img: "+ ex.toString());
            return imArray[0];
        }
    }
         
    public void acquireImages() {
        try {
            studio_.core().waitForDevice(studio_.core().getCameraDevice());
            studio_.core().clearCircularBuffer();
            String cam = studio_.core().getCameraDevice();
            
//          CMMCore::startSequenceAcquisition(long numImages, double intervalMs, bool stopOnOverflow)
//          @param numImages Number of images requested from the camera
//          @param intervalMs interval between images, currently only supported by Andor cameras
//          @param stopOnOverflow whether or not the camera stops acquiring when the circular buffer is full
            
            studio_.core().startPropertySequence(filtLabel, filtProp);
                            
            studio_.core().startSequenceAcquisition(wv.length, 0, false);
            
            long now = System.currentTimeMillis();
            int frame = 1;// keep 0 free for the image from engine
            // reference BurstExample.bsh
            
            boolean canExit = false;
            while (true) {
                boolean remaining = (studio_.core().getRemainingImageCount() > 0);
                boolean running = (studio_.core().isSequenceRunning(cam));
                if ((!remaining) && (canExit)) {
                    break;  //Everything is taken care of.
                }
                if (remaining) {    //Process images
                   imageQueue.add(studio_.core().popNextTaggedImage());
                   frame++;
                   /*
                    if (proc_.display_ != null) {
                        if (proc_.display_.acquisitionIsRunning()) {
                            proc_.display_.displayStatusLine("Image Avg. Acquiring No. " + frame);
                        }
                    }
                    */
                }
                if (!running) {
                    studio_.core().stopPropertySequence(filtLabel, filtProp);
                    canExit = true;
                }
             }
            long itTook = System.currentTimeMillis() - now;
            try {
                studio_.core().stopSequenceAcquisition();  
            } catch (Exception ex) {
                ex.printStackTrace();
                ReportingUtils.logMessage("ERROR: FrameAvg: " + ex.getMessage());
            }          
            if (debugLogEnabled_) {
                ReportingUtils.logMessage("Averaging Acquisition took: " + itTook + " milliseconds for "+numAverages_ + " frames");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            ReportingUtils.logMessage("FrameAvg Error");
        }
    }
}
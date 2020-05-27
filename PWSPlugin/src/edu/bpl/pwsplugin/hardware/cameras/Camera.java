/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.hardware.cameras;

import edu.bpl.pwsplugin.hardware.MMDeviceException;
import edu.bpl.pwsplugin.settings.CamSettings;
import java.util.List;
import org.micromanager.data.Image;

/**
 *
 * @author N2-LiveCell
 */
public abstract class Camera {
    public abstract void initialize() throws MMDeviceException;
    
    public abstract boolean supportsExternalTriggering(); //True if the camera can have new image acquisitions triggered by an incoming TTL signal
    
//public abstract void configureExternalTriggering(boolean enable, double triggerDelayMs) throws MMDeviceException; //Turn external triggering on or off.
    
    public abstract boolean supportsTriggerOutput(); //True if the camera can send a TTL trigger at the end of each new image it acquires.
    
    public abstract void configureTriggerOutput(boolean enable) throws MMDeviceException; //Turn transmission of TTL pulses on or off.
    
    public abstract String getName(); //Get the device name used in Micro-Manager.
    
    public abstract void startSequence(int numImages, double delayMs, boolean externalTriggering) throws MMDeviceException; //If the camera support "Trigger output" then this should start the seqeunce
    
    public abstract void stopSequence() throws MMDeviceException; // Clean up and reset the sequence. Only needed for cameras that support trigger output.
    
    public abstract void setExposure(double exposureMs) throws MMDeviceException;
    
    public abstract double getExposure() throws MMDeviceException;
    
    public abstract Image snapImage() throws MMDeviceException;
    
    public abstract CamSettings getSettings();
    
    public abstract List<String> validate(); //Return a list of strings for every error detected in the configuration. return empty list if no errors found.
    
    public static Camera getInstance(CamSettings settings) {
        if (settings.camType == Types.HamamatsuOrca4V3) {
            return new HamamatsuOrcaFlash4v3(settings);
        } else if (settings.camType == Types.HamamatsuEMCCD) {
            return new HamamatsuEMCCD(settings);
        } else if (settings.camType == Types.Simulated) {
            return new SimulatedCamera(settings);
        } else {
            return null; //This shouldn't ever happen.
        }
    }
    
    public enum Types {
        HamamatsuOrca4V3,
        HamamatsuEMCCD,
        Simulated;
    }
}

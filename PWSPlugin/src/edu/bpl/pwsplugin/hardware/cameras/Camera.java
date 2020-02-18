/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.hardware.cameras;

import edu.bpl.pwsplugin.settings.PWSPluginSettings;
import java.util.List;
import org.micromanager.data.Image;

/**
 *
 * @author N2-LiveCell
 */
public abstract class Camera {
    public abstract void initialize() throws Exception;
    public abstract boolean supportsExternalTriggering(); //True if the camera can have new image acquisitions triggered by an incoming TTL signal
    //public abstract void configureExternalTriggering(boolean enable, double triggerDelayMs) throws Exception; //Turn external triggering on or off.
    public abstract boolean supportsTriggerOutput(); //True if the camera can send a TTL trigger at the end of each new image it acquires.
    public abstract void configureTriggerOutput(boolean enable) throws Exception; //Turn transmission of TTL pulses on or off.
    public abstract String getName(); //Get the device name used in Micro-Manager.
    public abstract void startSequence(int numImages, double intervalMs, boolean externalTriggering) throws Exception;
    public abstract void stopSequence() throws Exception;
    public abstract void setExposure(double exposureMs) throws Exception;
    public abstract double getExposure() throws Exception;
    public abstract Image snapImage() throws Exception;
    public abstract PWSPluginSettings.HWConfiguration.CamSettings getSettings();
    public abstract List<String> validate(); //Return a list of strings for every error detected in the configuration. return empty list if no errors found.
    
    public static Camera getInstance(PWSPluginSettings.HWConfiguration.CamSettings settings) {
        if (settings.type == Types.HAMAMATSUORCA4V3) {
            return new HamamatsuOrcaFlash4v3(settings);
        } else {
            return null; //This shouldn't ever happen.
        }
    }
    
    public enum Types {
        HAMAMATSUORCA4V3;
    }
}

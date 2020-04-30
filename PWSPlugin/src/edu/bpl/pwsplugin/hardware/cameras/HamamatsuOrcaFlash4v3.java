/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.hardware.cameras;

import edu.bpl.pwsplugin.Globals;
import edu.bpl.pwsplugin.settings.CamSettings;
import edu.bpl.pwsplugin.settings.PWSPluginSettings;
import java.util.ArrayList;
import java.util.List;
import org.micromanager.data.Image;

/**
 *
 * @author N2-LiveCell
 */
public class HamamatsuOrcaFlash4v3 extends Camera{
    String _devName;
    CamSettings _settings;
    
    public HamamatsuOrcaFlash4v3(CamSettings settings) {
        _settings = settings;
        _devName = settings.name;
    }
    
    @Override
    public void initialize() throws Exception {        
        Globals.core().setProperty(this._devName, "TRIGGER SOURCE", "INTERNAL");
        Globals.core().setProperty(this._devName, "MASTER PULSE TRIGGER SOURCE", "SOFTWARE");
        Globals.core().setProperty(this._devName, "MASTER PULSE MODE", "CONTINUOUS");
        Globals.core().setProperty(this._devName, "OUTPUT TRIGGER SOURCE[0]", "READOUT END");
        Globals.core().setProperty(this._devName, "OUTPUT TRIGGER POLARITY[0]", "POSITIVE");
    }
    
    @Override
    public boolean supportsExternalTriggering() { return true; }
    
    @Override
    public boolean supportsTriggerOutput() { return true; }
    
    @Override
    public void configureTriggerOutput(boolean enable) throws Exception {
        if (enable) {
            Globals.core().setProperty(_devName, "OUTPUT TRIGGER SOURCE[0]", "READOUT END");
            Globals.core().setProperty(_devName, "OUTPUT TRIGGER POLARITY[0]", "POSITIVE");
            Globals.core().setProperty(_devName, "OUTPUT TRIGGER KIND[0]", "PROGRAMMABLE");
            Globals.core().setProperty(_devName, "OUTPUT TRIGGER PERIOD[0]", 0.001); //The default is shorter than this and it is often missed by other devices.
        } else {
            Globals.core().setProperty(_devName, "OUTPUT TRIGGER KIND[0]", "LOW");
        }
    }
    
    @Override
    public String getName() { //Get the device name used in Micro-Manager.
        return this._devName;
    }
    
    @Override
    public void startSequence(int numImages, double delayMs, boolean externalTriggering) throws Exception{
        if (externalTriggering) {
            Globals.core().setProperty(this._devName, "TRIGGER SOURCE", "EXTERNAL");
            Globals.core().setProperty(this._devName, "TRIGGER DELAY", delayMs/1000); //This is in units of seconds.
        } else {
            double exposurems = this.getExposure();
            double readoutms = 12; //This is based on the frame rate calculation portion of the 13440-20CU camera. 9.7 us per line, reading two lines at once, 2048 lines -> 0.097*2048/2 ~= 10 ms. However testing has shown if we set this exactly then we end up missing every other frame and getting half our frame rate add a buffer of 2ms to be safe.
            double intervalMs = (exposurems+readoutms+delayMs);
            Globals.core().setProperty(this._devName, "TRIGGER SOURCE", "MASTER PULSE"); //Make sure that Master Pulse is triggering the camera.
            Globals.core().setProperty(this._devName, "MASTER PULSE INTERVAL", intervalMs/1000.0); //In units of seconds
        }
        Globals.core().startSequenceAcquisition(this._devName, numImages, 0, false); //The hamamatsu adapter throws an error if the interval is not 0.
    }
    
    @Override
    public void stopSequence() throws Exception {
        Globals.core().stopSequenceAcquisition(this._devName);
        Globals.core().setProperty(this._devName, "TRIGGER SOURCE", "MASTER PULSE"); //Set the trigger source back ot what it was originally
    }
    
    @Override
    public CamSettings getSettings() {
        return _settings;
    }
    
    @Override
    public void setExposure(double exposureMs) throws Exception {
        Globals.core().setExposure(_devName, exposureMs);
    }
    
    @Override
    public double getExposure() throws Exception {
        return Globals.core().getExposure(_devName);
    }
    
    @Override
    public Image snapImage() throws Exception {
        //TODO what if we are not set as the core camera at this point.
        Globals.core().snapImage();
        return Globals.mm().data().convertTaggedImage(Globals.core().getTaggedImage());
    }
    
    @Override
    public List<String> validate() {
        List<String> errs = new ArrayList<>();
        try {
            if (!Globals.core().getDeviceName(this._devName).equals("HamamatsuHam_DCAM")) {
                errs.add(_devName + " is not a HamamatsuHam_DCAM device");
            }
        } catch (Exception e) {
            errs.add(e.getMessage());
        }
        return errs;
    }
}

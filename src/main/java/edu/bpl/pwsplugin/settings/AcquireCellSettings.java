/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.settings;

import edu.bpl.pwsplugin.utils.JsonableParam;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Nick Anthony <nickmanthony at hotmail.com>
 */
public class AcquireCellSettings extends JsonableParam {
    /*Contains all settings for a single "Cell" folder acquisition. Can include
    One PWS acquisition, one Dynamics acquisition, and multiple fluorescence acquisitions.
    */
    
    public boolean pwsEnabled = true;
    public PWSSettings pwsSettings = new PWSSettings();
    
    public boolean dynEnabled = true;
    public DynSettings dynSettings = new DynSettings();
    
    public boolean fluorEnabled = true;
    public List<FluorSettings> fluorSettings = Arrays.asList(new FluorSettings());
    
    
    
    public static AcquireCellSettings getDefaultSettings(PWSSettingsConsts.Systems sys) {
        //This convenience function is used to generate the default settings for various systems used by Northwestern.
        AcquireCellSettings settings = new AcquireCellSettings();
        PWSSettings pwsSettings = settings.pwsSettings;
        DynSettings dynSettings = settings.dynSettings;
        
        settings.pwsEnabled = true;
        settings.dynEnabled = false;
        settings.fluorEnabled = false;
        
        //For now we use the same default settings for all systems for dynamics.
        dynSettings.exposure = 50;
        dynSettings.numFrames = 200;
        dynSettings.wavelength = 550;
        
        //Much of the pws settings are also the same between systems.
        pwsSettings.wvStart = 500;
        pwsSettings.wvStop = 700;
        pwsSettings.wvStep = 2;
        
        switch (sys) {
            case LCPWS1:
                pwsSettings.exposure = 50;
                pwsSettings.externalCamTriggering = false;
                pwsSettings.ttlTriggering = false;
                return settings;
            case LCPWS2:
                pwsSettings.exposure = 100;
                pwsSettings.externalCamTriggering = false;
                pwsSettings.ttlTriggering = true;
                return settings;
            case LCPWS3:
                pwsSettings.exposure = 100;
                pwsSettings.externalCamTriggering = true;
                pwsSettings.ttlTriggering = true;
                return settings;
            case STORM:
                pwsSettings.exposure = 100;
                pwsSettings.externalCamTriggering = false;
                pwsSettings.ttlTriggering = false;
                return settings;
        }
        throw new RuntimeException("Programming error");
    }
}

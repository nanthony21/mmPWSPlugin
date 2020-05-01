package edu.bpl.pwsplugin;


import edu.bpl.pwsplugin.hardware.configurations.ImagingConfiguration;
import edu.bpl.pwsplugin.settings.HWConfigurationSettings;
import edu.bpl.pwsplugin.settings.ImagingConfigurationSettings;
import java.util.HashMap;
import java.util.Map;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Nick Anthony <nickmanthony at hotmail.com>
 */
public class HWConfiguration {
    HWConfigurationSettings settings;
    Map<String, ImagingConfiguration> imConfigs;
    
    public HWConfiguration(HWConfigurationSettings settings) {
        this.settings = settings;
        imConfigs = new HashMap<>();
        for (int i=0; i < settings.configs.size(); i++) {
            ImagingConfigurationSettings s = settings.configs.get(i);
            imConfigs.put(s.name, ImagingConfiguration.getInstance(s));
        }
    }
    
    public HWConfigurationSettings getSettings() {
        return this.settings;
    }
    
    public ImagingConfiguration getConfigurationByName(String name) {
        return this.imConfigs.get(name);
    }
}

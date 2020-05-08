package edu.bpl.pwsplugin.hardware;


import edu.bpl.pwsplugin.hardware.configurations.ImagingConfiguration;
import edu.bpl.pwsplugin.settings.HWConfigurationSettings;
import edu.bpl.pwsplugin.settings.ImagingConfigurationSettings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
    
    public ImagingConfiguration getImagingConfigurationByName(String name) {
        return this.imConfigs.get(name);
    }
    
    public List<ImagingConfiguration> getImagingConfigurations() {
        return new ArrayList<>(this.imConfigs.values());
    }
}
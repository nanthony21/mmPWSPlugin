/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer.factory;

import edu.bpl.pwsplugin.acquisitionSequencer.steps.Step;
import edu.bpl.pwsplugin.UI.utils.BuilderJPanel;
import edu.bpl.pwsplugin.UI.utils.SingleBuilderJPanel;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerConsts;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerSettings;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.AcquireTimeSeries;
import edu.bpl.pwsplugin.utils.JsonableParam;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JLabel;
import edu.bpl.pwsplugin.UI.utils.ImprovedComponents;
import javax.swing.SpinnerNumberModel;
import net.miginfocom.swing.MigLayout;


/**
 *
 * @author Nick Anthony <nickmanthony at hotmail.com>
 */
public class AcquireTimeSeriesFactory extends StepFactory {
    @Override
    public Class<? extends BuilderJPanel> getUI() {
        return TimeSeriesUI.class;
    }
    
    @Override
    public Class<? extends JsonableParam> getSettings() {
        return SequencerSettings.AcquireTimeSeriesSettings.class;
    }
    
    @Override
    public Class<? extends Step> getStep() {
        return AcquireTimeSeries.class;
    }
    
    @Override
    public String getDescription() {
        return "Perform enclosed steps at multiple time points.";
    }
    
    @Override
    public String getName() {
        return "Time Series";
    }
    
    @Override
    public String getCategory() {
        return "Sequencing";
    }

    @Override
    public SequencerConsts.Type getType() {
        return SequencerConsts.Type.TIME;
    }
}


class TimeSeriesUI extends SingleBuilderJPanel<SequencerSettings.AcquireTimeSeriesSettings> {
    ImprovedComponents.Spinner numFrames;
    ImprovedComponents.Spinner frameIntervalMinutes;
    
    public TimeSeriesUI() {
        super(new MigLayout("insets 5 0 0 0"), SequencerSettings.AcquireTimeSeriesSettings.class);
        
        numFrames = new ImprovedComponents.Spinner(new SpinnerNumberModel(1, 1, 1000000000, 1));
        frameIntervalMinutes = new ImprovedComponents.Spinner(new SpinnerNumberModel(1.0, 0.0, 1000000000.0, 1.0));
        ((ImprovedComponents.Spinner.DefaultEditor) numFrames.getEditor()).getTextField().setColumns(6);
        ((ImprovedComponents.Spinner.DefaultEditor) frameIntervalMinutes.getEditor()).getTextField().setColumns(6);

                
        
        this.add(new JLabel("Number of time frames:"), "gapleft push");
        this.add(numFrames, "wrap");
        this.add(new JLabel("Frame Interval (minutes):"), "gapleft push");
        this.add(frameIntervalMinutes);
    }
    
    @Override
    public Map<String, Object> getPropertyFieldMap() {
        HashMap<String, Object> m = new HashMap<>();
        m.put("numFrames", numFrames);
        m.put("frameIntervalMinutes", frameIntervalMinutes);
        return m;
    }
}


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer.factories;

import edu.bpl.pwsplugin.Globals;
import edu.bpl.pwsplugin.UI.utils.BuilderJPanel;
import edu.bpl.pwsplugin.UI.utils.SingleBuilderJPanel;
import edu.bpl.pwsplugin.acquisitionSequencer.AcquisitionStatus;
import edu.bpl.pwsplugin.acquisitionSequencer.Consts;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.ContainerStep;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.SequencerFunction;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.SequencerSettings;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.Step;
import edu.bpl.pwsplugin.utils.JsonableParam;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JSpinner;
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
    public Consts.Category getCategory() {
        return Consts.Category.SEQ;
    }

    @Override
    public Consts.Type getType() {
        return Consts.Type.TIME;
    }
}


class TimeSeriesUI extends SingleBuilderJPanel<SequencerSettings.AcquireTimeSeriesSettings> {
    JSpinner numFrames;
    JSpinner frameIntervalMinutes;
    
    public TimeSeriesUI() {
        super(new MigLayout(), SequencerSettings.AcquireTimeSeriesSettings.class);
        
        numFrames = new JSpinner(new SpinnerNumberModel(1, 1, 1000000000, 1));
        frameIntervalMinutes = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 1000000000.0, 1.0));
        
        this.add(new JLabel("Number of time frames:"));
        this.add(numFrames, "wrap");
        this.add(new JLabel("Frame Interval (minutes):"));
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

class AcquireTimeSeries extends ContainerStep {
    public AcquireTimeSeries() {
        super(Consts.Type.TIME);
    }
    
    @Override 
    public SequencerFunction getFunction() {
        SequencerFunction stepFunction = super.getSubstepsFunction();
        SequencerSettings.AcquireTimeSeriesSettings settings = (SequencerSettings.AcquireTimeSeriesSettings) this.getSettings();
        return new SequencerFunction() {
            @Override
            public AcquisitionStatus applyThrows(AcquisitionStatus status) throws Exception {
                //TIMESERIES execute acquisitionFunHandle repeatedly at a specified time
                //interval. the handle must take as input the Cell number to start at. It
                //will return the number of new acquisitions that it tood.
                double lastAcqTime = 0;
                for (int k=0; k<settings.numFrames; k++) {
                    // wait for the specified frame interval before proceeding to next frame
                    if (k!=0) { //No pause for the first iteration
                        int count = 0;
                        while ((System.currentTimeMillis() - lastAcqTime)/60000 < settings.frameIntervalMinutes) {
                            String msg = String.format("Waiting %.1f seconds before acquiring next frame", settings.frameIntervalMinutes - (System.currentTimeMillis() - lastAcqTime)/60000);
                            Globals.statusAlert().setText(msg);
                            count++;
                            Thread.sleep(500);
                        }   
                        if (count == 0) {
                            Globals.statusAlert().setText(String.format("Acquistion took %.1f seconds. Longer than the frame interval.", (System.currentTimeMillis() - lastAcqTime)/1000));
                        }
                    }
                    lastAcqTime = System.currentTimeMillis(); //Save the current time so we can figure out when to start the next acquisition.
                    status = stepFunction.apply(status);
                    status.update(String.format("Finished time set %d of %d", k, settings.numFrames), status.currentCellNum);
                }
                return status;
            }
        };
    }  
}

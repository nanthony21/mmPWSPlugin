///////////////////////////////////////////////////////////////////////////////
//PROJECT:       PWS Plugin
//
//-----------------------------------------------------------------------------
//
// AUTHOR:       Nick Anthony, 2021
//
// COPYRIGHT:    Northwestern University, 2021
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
package edu.bpl.pwsplugin.acquisitionSequencer.factory;

import edu.bpl.pwsplugin.acquisitionSequencer.steps.RootStep;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.Step;
import edu.bpl.pwsplugin.UI.utils.BuilderJPanel;
import edu.bpl.pwsplugin.UI.utils.DirectorySelector;
import edu.bpl.pwsplugin.UI.utils.ImprovedComponents;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerConsts;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerSettings;
import edu.bpl.pwsplugin.utils.JsonableParam;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author Nick Anthony <nickmanthony at hotmail.com>
 */
public class RootStepFactory extends StepFactory{
    //Should only exist once as the root of each experiment, sets the needed root parameters.
    @Override
    public Class<? extends BuilderJPanel> getUI() {
        return RootStepUI.class;
    }
    
    @Override
    public Class<? extends JsonableParam> getSettings() {
        return SequencerSettings.RootStepSettings.class;
    }
    
    @Override
    public Class<? extends Step> getStep() {
        return RootStep.class;
    }
    
    @Override
    public String getDescription() {
        return "Initial settings for the experiment.";
    }
    
    @Override
    public String getName() {
        return "Initialization";
    }
    
    @Override
    public String getCategory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SequencerConsts.Type getType() {
        return SequencerConsts.Type.ROOT;
    }
}

class RootStepUI extends BuilderJPanel<SequencerSettings.RootStepSettings> {
    DirectorySelector directory = new DirectorySelector(DirectorySelector.DefaultMMFunctions.MMDataSetDirectory);
    JTextArea description = new JTextArea("Experiment description here.");
    JTextField author = new JTextField("");
    JTextField project = new JTextField("");
   
    public RootStepUI() {
        super(new MigLayout("insets 0 0 0 0"), SequencerSettings.RootStepSettings.class);
        
        

        JScrollPane scroll = new JScrollPane(description);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(new JLabel("Root Folder:"), "wrap, spanx");
        this.add(directory, "wrap, growx, spanx");
        this.add(new JLabel("Author:"), "wrap");
        this.add(author, "wrap, pushx, growx");
        this.add(new JLabel("Project:"), "wrap");
        this.add(project, "wrap, pushx, growx");
        this.add(new JLabel("Description:"), "wrap, spanx");
        this.add(scroll, "grow, push, spanx");
        
        description.setEditable(true);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setBorder(BorderFactory.createLoweredSoftBevelBorder());
        
    }
    
    @Override
    public void populateFields(SequencerSettings.RootStepSettings settings) {
        directory.setText(settings.directory);
        if (!settings.description.equals("")) { //No point populating an empty string, just leave the default.
            description.setText(settings.description);
        }
        project.setText(settings.project);
        author.setText(settings.author);
    }
    
    @Override
    public SequencerSettings.RootStepSettings build() {
        SequencerSettings.RootStepSettings settings = new SequencerSettings.RootStepSettings();
        settings.directory = this.directory.getText();
        settings.description = description.getText();
        settings.project  = project.getText();
        settings.author = author.getText();
        return settings;
    }
}
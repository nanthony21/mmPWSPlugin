/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer.UI.tree;

import edu.bpl.pwsplugin.acquisitionSequencer.steps.ContainerStep;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.EndpointStep;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.IteratingContainerStep;
import java.awt.Color;
import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
/**
 *
 * @author nick
 */
public class TreeRenderers {
    //This class contains a collection of renderers that determine how the items of a JTree will look.
    
    public static class SequenceTreeRenderer extends DefaultTreeCellRenderer {
        //Empty ContainerSteps that are empty are rendered as red text with a special icon.
        //ContainerSteps and Endpoint steps each have a custom icon.
        //A red outline is drawn around active steps.
        public SequenceTreeRenderer() {
            super();
            setBorderSelectionColor(Color.RED); //This helps us to see which tree is active when there are multiple trees in use.
        } 

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean isLeaf, int row, boolean focused) {
            JLabel comp = (JLabel) super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, focused);
            if (value instanceof ContainerStep) {
                ContainerStep node = (ContainerStep) value;
                if (node.getChildCount() == 0) { //The container step is empty, set the icon to draw attention to this issue.
                    ImageIcon image = new ImageIcon(getClass().getResource("/edu/bpl/icons/emptyContainerNode.png"));
                    if (image == null) {
                        throw new RuntimeException("Icon failed to load");
                    }
                    comp.setIcon(image);
                    comp.setText("<html>" + comp.getText() + "<font color=red> (empty)</font></html>");
                    return comp;
                } else {
                    ImageIcon image = new ImageIcon(getClass().getResource("/edu/bpl/icons/containerNode.png"));
                    if (image == null) {
                        throw new RuntimeException("Icon failed to load");
                    }
                    comp.setIcon(image);
                    return comp;
                }
            } else if (value instanceof EndpointStep) {
                ImageIcon image = new ImageIcon(getClass().getResource("/edu/bpl/icons/endpointNode.png"));
                if (image == null) {
                    throw new RuntimeException("Icon failed to load");
                }
                comp.setIcon(image);
                return comp;
            } else {
                return comp; //Just return the default rendered componenent.
            }
        }
    }
    
    public static class NewStepsTreeRenderer extends DefaultTreeCellRenderer {
        //This is used for a tree displaying which Step types are available. It is the same as `SequenceTreeRenderer` except that
        //empty ContainerSteps aren't rendered as red or with a special error icon.
        public NewStepsTreeRenderer() {
            super();
            setBorderSelectionColor(Color.RED); //This helps us to see which tree is active when there are multiple trees in use.
        } 
        
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean isLeaf, int row, boolean focused) {
            JLabel comp = (JLabel) super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, focused);
            if (value instanceof ContainerStep) {
                ImageIcon image = new ImageIcon(getClass().getResource("/edu/bpl/icons/containerNode.png"));
                if (image == null) {
                    throw new RuntimeException("Icon failed to load");
                }
                comp.setIcon(image);
                return comp;
            } else if (value instanceof EndpointStep) {
                ImageIcon image = new ImageIcon(getClass().getResource("/edu/bpl/icons/endpointNode.png"));
                if (image == null) {
                    throw new RuntimeException("Icon failed to load");
                }
                comp.setIcon(image);
                return comp;
            } else {
                return comp; //Just return the default rendered componenent.
            }
        }
    }
    
    public static class SequenceRunningTreeRenderer extends SequenceTreeRenderer {
        
        public SequenceRunningTreeRenderer() {
            super();
        }
        
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean isLeaf, int row, boolean focused) {
            JLabel comp = (JLabel) super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, focused);
            if (value instanceof IteratingContainerStep) {
              
                String initText = comp.getText();
                int currentIteration = ((IteratingContainerStep) value).getCurrentIteration();
                int totalIterations = ((IteratingContainerStep) value).getTotalIterations();
                initText = String.format("%s (%d/%d)", initText, currentIteration + 1, totalIterations);
                comp.setText(initText);
                return comp;
            } else {
                return comp; //Just return the default rendered componenent.
            }
        }
    }
}

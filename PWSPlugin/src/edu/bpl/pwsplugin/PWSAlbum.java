///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//SUBSYSTEM:     mmstudio
//-----------------------------------------------------------------------------
//
// AUTHOR:       Chris Weisiger, 2015
//
// COPYRIGHT:    University of California, San Francisco, 2006-2015
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

package edu.bpl.pwsplugin;

import java.io.IOException;
import org.micromanager.data.Coords;
import org.micromanager.data.Datastore;
import org.micromanager.data.DatastoreFrozenException;
import org.micromanager.data.DatastoreRewriteException;
import org.micromanager.data.Image;
import org.micromanager.display.DisplayWindow;
import org.micromanager.internal.utils.ReportingUtils;
import org.micromanager.Studio;
import javax.swing.SwingUtilities;
import org.micromanager.data.RewritableDatastore;

public class PWSAlbum {
   private RewritableDatastore store_;
   private Studio studio_;
   private int idx = 0;
   private DisplayWindow display = null;
   
   PWSAlbum(Studio studio) {
       studio_ = studio;
       store_ = studio_.data().createRewritableRAMDatastore();
   }
   
   public Datastore getDatastore() {
      return store_;
   }
   
   public void clear() throws IOException{
       idx = 0;
       store_.deleteAllImages();
   }

   public void addImage(Image image){   
        if ((display==null) || (display.isClosed())) {
            display = studio_.displays().createDisplay(store_);
            display.setCustomTitle("PWS");
        }
        Coords newCoords = image.getCoords().copyBuilder().t(idx).build();
        idx++;

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    store_.putImage(image.copyAtCoords(newCoords));
                }
                catch (DatastoreFrozenException e) {
                   ReportingUtils.showError(e, "Album datastore is locked.");
                }
                catch (DatastoreRewriteException e) {
                   // This should never happen.
                   ReportingUtils.showError(e, "Unable to add image at " + newCoords + 
                           " to album as another image with those coords already exists.");
                }
                catch (IOException e) {
                    ReportingUtils.showError(e, "PWSAlbum IOException");
                }
            }
        });  
    }
}

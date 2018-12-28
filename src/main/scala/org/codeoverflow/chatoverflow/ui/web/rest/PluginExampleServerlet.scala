package org.codeoverflow.chatoverflow.ui.web.rest

import org.codeoverflow.chatoverflow.ui.web

/**
  * This is just an example servlet to show how to provide a rest interface for e.g. a gui.
  */
class PluginExampleServerlet extends web.JsonServlet {

  get("/plugins") {
    //ChatOverflow.getPlugins
  }

}

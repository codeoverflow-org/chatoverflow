package org.codeoverflow.chatoverflow2.ui.web.rest

import org.codeoverflow.chatoverflow2.ui.web

/**
  * This is just an example servlet to show how to provide a rest interface for e.g. a gui.
  */
class PluginExampleServerlet extends web.JsonServlet {

  get("/plugins") {
    //ChatOverflow.getPlugins
  }

}

package org.codeoverflow.chatoverflow.web.rest

import org.codeoverflow.chatoverflow.web.JsonServlet

/**
  * This is just an example servlet to show how to provide a rest interface for e.g. a gui.
  */
class PluginExampleServerlet extends JsonServlet {

  get("/plugins") {
    //ChatOverflow.getPlugins
  }

}

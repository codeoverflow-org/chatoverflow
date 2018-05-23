# Chat Overflow
Chat Overflow is a framework to enable better interaction in livestream events by connecting several inputs and outputs, e.g. chat messages, sub notifications and screen overlays.
The software is developed by three computer science students @ [KIT, Karlsruhe](http://www.kit.edu/) and is headed by [skate702](http://skate702.de). For more information please visit http://codeoverflow.org.

## Chat Overflow Framework
The framework contains all information to connect to livestream inputs and outputs. Combined with the [API Project](https://github.com/codeoverflow-org/chatoverflow-api), plugins can be executed. They provide defined behavior, e.g. the [public plugins](https://github.com/codeoverflow-org/chatoverflow-plugins).

## Getting Started

### Pro

1. Clone the main repository. Then clone the api into folder `codeoverflow/api` and public plugins repository into `codeoverflow/plugins-public`.
2. Setup the main repository as imported scala project, e.g. using IntelliJ. Make sure to refresh all sbt content and load the custom run configurations.
3. You're done. Happy coding!

### Not so Pro

1. Download and install [git](https://git-scm.com/) (obviously). Make sure that you can run it using the console.
2. Clone the main repository using `git clone https://github.com/codeoverflow-org/chatoverflow.git`.
3. Navigate to the created chatoverflow-folder, e.g. using `cd chatoverflow`.
4. Clone the [api repository](https://github.com/codeoverflow-org/chatoverflow-api) into a folder named "api". using `git clone https://github.com/codeoverflow-org/chatoverflow-api.git api`.
5. Clone the [public plugins repository](https://github.com/codeoverflow-org/chatoverflow-plugins) into a folder named "plugins-public" using `git clone https://github.com/codeoverflow-org/chatoverflow-plugins.git plugins-public`.

    Site note: You can name the folders whatever you want. But if you do so, update the names in the main [build-file](https://github.com/codeoverflow-org/chatoverflow/blob/42b9469fe489fe5efeb4aa70f278e3558fccab7d/build.sbt#L64).

6. Open up your most favorite IDE for java (and scala) development. I recommend using [IntelliJ IDEA](https://www.jetbrains.com/idea/). There is also a free community version available!
7. Make sure, IntelliJ IDEA and the scala plugin are up to date. Every time I open this software, a new update is available...
8. Choose open project and select the freshly cloned `chatoverflow` folder (with the main build.sbt) in it. Make sure, auto-import is NOT enabled.
9. Click on `Refresh sbt project`. Make sure to use a up-to-date java 1.8 SDK, do not change any other settings in the dialog window.
10. Wait for 5.2 years. In the background, 3 projects are build, ressources will be downloaded and a lot of magic happens!
11. When the process finished, you should see the project and its children in the project view. Get to know them (or wait till I introduce them later).

    Site note: The run configurations contain a lot of sbt commands and presets for plugin development. You will probably never need more. Brief documentation can be found in the main build.sbt file.

12. Select the run configuration `Fetch plugins (sbt fetch)` and execute it. A file named `plugins.sbt` should be generated, containing all references to plugins and the api project.
13. Use sbt reload (the refresh icon in the upper left corner of the sbt window, opened e.g. by View -> Tool Windows -> SBT) to reload the whole project. Now, the child projects should be recognized correctly.
14. Have a look at the application run configurations. Do they have an red X? Then they are broken. Click on `Edit configurations` and select `root` as module ("Use classpath as module"). Now, they should be happy.
15. Execute the run configuration `[Advanced] Full Reload and Run ChatOverflow` (and pray). This is only needed for the first startup and when you create a new plugin!

    * A lot happens now. First, all target folders and the build environment is cleaned.
    * Then, the project is freshly build and all plugins are fetched. The plugins.sbt file is generated. Then, the whole build environment is reloaded.
    * Next, all plugin versions and the api version are printed for debug reasons.
    * Last, all plugins compiled code is packaged into several .JAR-Archives and copied to the `plugins`-folder.
    * Everything is set up, and the chat overflow framework is executed. It checks the plugin folder and tries to load the plugins. Done!

    Site note: You do not have to repeat this step after each simple change. There is also a `[Simple]` run configuration for this.

    Yet another site note: The projects folder structure is realy easy.

    * the api folder contains the API-project with all common chat overflow modelling.
    * the plugins folder contains packaged plugins. Just leave him alone.
    * the plugins-public folder contains all official/public chat overflow plugins. Each plugin has its own sub project.
    * the project folder contains build code for the plugin framework.
    * the src folder contains the chat overflow framework code.
    * the target folder contains the build framework.

16. You're done. Happy coding!
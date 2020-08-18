<p align="center">
  <img src = "chatoverflow-logo.png"/>
</p>

# Deprecated

The **ChatOverflow** framework started as simple idea of three computer science students. Fast forward, three years later, we built a large framework which exceeded our expectations. Unfortunately, the choice of *Scala* as main programming language limited the number of volunteers which are crucial to an open-source project like this. Additionally, the distance to modern web-based frameworks with good integrations of used APIs impaired the progress.

We choose to abandon **ChatOverflow** but not the idea behind it. We still aim to create a framework for simplified development of streaming-centric services. We choose to build upon [node-cg](https://nodecg.com/), a popular framework for creating overlays and integrations for livestream events. We created [node-cg-io](https://github.com/codeoverflow-org/nodecg-io) which realizes the vision of **ChatOverflow** using node-cg.

**TL;DR** **ChatOverflow** is deprecated. Use our successor [node-cg-io](https://github.com/codeoverflow-org/nodecg-io) instead.

# Legacy Readme

What if you could **combine** the power of
- **Chat bots** like *nightbot*, *moobot* and *botler*
- **Supporting services** like *StreamElements*, *Streamlabs*, *TipeeeStream*, *Loots* and
- **Social Media** and Chat software, e.g. *Twitter*, *Discord*, *YouTube* etc.

with your interactive chat in your livestream. What if you could easily react on events, e.g.

- Automatically **share** your new subscribers on twitter
- Automatically **control** your studio's lighting colors through chat messages
- Automatically **post** an user's cheer on your minecraft server
- Automatically **upload** a youtube video with stream highlights when your stream stops

and so much more. We know, there is [IFTTT](https://ifttt.com/). But sometimes, building blocks are to generic and services aren't optimized for your streaming environment.

The alternative: Develop everything by yourself and waste hundreds of hours with API-integration. We already solved this problem for you. This is **Chat Overflow**.

## The ChatOverflow Project

**Chat Overflow** is a plugin framework, which offers ready-to-use platform integrations for all* major streaming- and social-media-sites.

**Chat Overflow** enables you to to level up your stream by writing simple, platform-independent plugins in java or scala**.

It's getting even better: The **Chat Overflow** license allows you to sell your custom plugins, creating new services for other streamers. 

And it's so easy. Here is all the code to get started with a simple twitch chat bot:

```java
 twitchChat = require.input.twitchChat()
 twitchChat.get.registerMessageHandler(msg => ...)
```

**Chat Overflow** fills the gap between simple but limited services like IFTTT and own from-scratch developed applications.

*Current development state: **Pre-Alpha***

\* There are still missing platforms. This is a open-source project. You can [help](https://github.com/codeoverflow-org/chatoverflow/issues), too!

\** The API is written in java. So, every JVM-compatible language is possible. Java, Scala, Kotlin, etc.

### Installation / Releases
Head over to [releases](https://github.com/codeoverflow-org/chatoverflow/releases). 

Just download the newest zip file, make sure that java is installed and launch the framework.

Note that you'll have to develop your own plugins or search for plugins online (e.g. on our [Discord Server](https://discord.gg/p2HDsme)). **Chat Overflow** is only the framework.

### Development

Start with the [Installation](https://github.com/codeoverflow-org/chatoverflow/wiki/Installation). Then learn more about the [CLI](https://github.com/codeoverflow-org/chatoverflow/wiki/Using-the-CLI).

Please consult the wiki to learn how to code new [platform sources](https://github.com/codeoverflow-org/chatoverflow/wiki/Adding-a-new-platform-source) and new [plugins](https://github.com/codeoverflow-org/chatoverflow/wiki/Writing-a-plugin).

***Pre-Alpha note***: Please note that the development workflow and the documentation will be updated soon.

### Discord

The perfect place if you need help with the framework, found a bug or want to share your own plugin:

[![discord](https://discordapp.com/api/guilds/577412066994946060/widget.png?style=banner2)](https://discord.gg/p2HDsme)

### About Code Overflow

Code Overflow started as a coding-livestream project from 3 computer science students @ [KIT, Karlsruhe](http://www.kit.edu/).
Now, it is a team of free open-source developers, sitting all over the world (really, lol). 
The project is headed by [skate702](http://skate702.de).

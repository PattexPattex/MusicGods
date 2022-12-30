<img align="right" src=".github/image.png" height="200" width="200">

<h1>MusicGods</h1>

[![CodeFactor](https://www.codefactor.io/repository/github/pattexpattex/musicgods/badge?style=flat-square)](https://www.codefactor.io/repository/github/pattexpattex/musicgods)
[![Lines of code](https://img.shields.io/tokei/lines/github/PattexPattex/MusicGods?label=lines%20of%20code&style=flat-square)](https://github.com/PattexPattex/MusicGods/)
[![License](https://img.shields.io/github/license/PattexPattex/MusicGods?style=flat-square)](https://github.com/PattexPattex/MusicGods/blob/master/LICENSE)
[![Latest release](https://img.shields.io/github/v/release/PattexPattex/MusicGods?include_prereleases&style=flat-square)](https://github.com/PattexPattex/MusicGods/releases/latest)
[![Issues](https://img.shields.io/github/issues/PattexPattex/MusicGods?style=flat-square)](https://github.com/PattexPattex/MusicGods/issues)
[![Ko-fi](https://img.shields.io/badge/donate-Ko--fi-red?style=flat-square&logo=kofi)](https://ko-fi.com/pattexpattex)

<h2>A powerful Discord music bot that you can easily host yourself!</h2>

Some of its features include:
- Advanced music playback
- Native Spotify support
- Native YouTube/Spotify track downloader
- Custom equalizer settings 
- An easy interface with the user
- Works purely with slash commands

...and more!
 
This project heavily depends on the [JDA](https://github.com/DV8FromTheWorld/JDA) and [Lavaplayer](https://github.com/Walkyst/lavaplayer-fork) libraries and was inspired by [JMusicBot](https://github.com/jagrosh/MusicBot).

<h2>Table of contents</h2>

<!-- TOC -->
  * [Getting started](#getting-started)
  * [Supported audio sources](#supported-audio-sources)
    * [Formats](#formats)
  * [Setup](#setup)
    * [Download](#download)
    * [Config](#config)
      * [Example config](#example-config)
      * [Notes](#notes)
    * [Code evaluation](#code-evaluation)
      * [IntelliJ setup](#intellij-setup)
    * [Runtime flags](#runtime-flags)
    * [Update migrations](#update-migrations)
  * [Submitting an issue](#submitting-an-issue)
  * [Editing, building and contributing](#editing-building-and-contributing)
<!-- TOC -->

## Getting started

- To see a list of all commands, use `/help` and select a group from the selection menu.
- Start playing music with `/play` or `/playfirst`. The bot will join your voice channel and start playing music!
- Search for music with `/search`. Select a track to start playing it.
- A basic GUI to control music is available with the `/queue` command.

## Supported audio sources

Basically everything that is supported by [Lavaplayer](https://github.com/sedmelluq/lavaplayer).

- YouTube,
- **Spotify**,
- SoundCloud,
- Bandcamp,
- Vimeo,
- HTTP(s) URLs...

### Formats

- MP3,
- FLAC,
- WAV,
- WebM (AAC / Opus / Vorbis),
- MP4 / M4A (AAC),
- OGG (Opus / Vorbis / FLAC),
- AAC...

## Setup

**This bot requires [Java 17](https://adoptium.net/temurin/releases/), along with [ffmpeg](https://ffmpeg.org/download.html) and [youtube-dl](http://ytdl-org.github.io/youtube-dl/download.html) (in the working directory or the PATH).**

### Download

Grab the [latest release](https://github.com/PattexPattex/MusicGods/releases/latest), the `.zip` file or the `.jar` file.
Place the file in a suitable directory.
Then just start the jar in the command line: `java -jar MusicGods-X.X.X.jar` (replace `X.X.X` with the [latest version](https://github.com/PattexPattex/MusicGods/releases/latest)).

Example of what the output should look on normal startup:
```
> java -jar MusicGods-X.X.X.jar
[18:32:18.048] [main] [com.pattexpattex.musicgods.Launcher] [INFO] - Using flags ''
[18:32:18.060] [main] [com.pattexpattex.musicgods.Launcher] [INFO] - Found ffmpeg in filesystem...
[18:32:18.064] [main] [com.pattexpattex.musicgods.Launcher] [INFO] - Found youtube-dl in filesystem...

 __  __           _       _____           _
|  \/  |         (_)     / ____|         | |
| \  / |_   _ ___ _  ___| |  __  ___   __| |___
| |\/| | | | / __| |/ __| | |_ |/ _ \ / _` / __|
| |  | | |_| \__ \ | (__| |__| | (_) | (_| \__ \  X.X.X
|_|  |_|\__,_|___/_|\___|\_____|\___/ \__,_|___/

  (https://github.com/PattexPattex/MusicGods)

[18:32:18.046] [main] [com.pattexpattex.musicgods.Bot] [INFO] - Starting MusicGods...
[18:32:18.071] [main] [c.p.musicgods.config.Config] [INFO] - Successfully read config from 'config.json'...
[18:32:19.300] [main] [net.dv8tion.jda.api.JDA] [INFO] - Login Successful!
[18:32:19.688] [JDA MainWS-ReadThread] [n.d.j.i.requests.WebSocketClient] [INFO] - Connected to WebSocket
[18:32:19.984] [JDA MainWS-ReadThread] [net.dv8tion.jda.api.JDA] [INFO] - Finished Loading!
[18:32:19.984] [JDA MainWS-ReadThread] [c.p.musicgods.ApplicationManager] [INFO] - Available guilds: 1 | Unavailable guilds: 0 | Total guilds: 1
```

### Config

On first startup, the bot should create a new `config.json` file and exit:

`[16:47:26.122] [main] [c.p.musicgods.config.Config] [INFO] - Created new config file in 'config.json', please fill it out.`

If you encounter any exceptions, usually after updating, make sure your config is up-to-date. 
You can compare it with the [default](https://github.com/PattexPattex/MusicGods/blob/master/src/main/resources/assets/ref-config.json) if you are stuck.

#### Example config
```json5
{
  "basic": { // General config
    "token": "...", // Bot token
    "owner": 53908232506183680, // Your user snowflake ID
    "eval": true, // Enable if you want to run arbitrary Java code from the bot
    "update_alerts": false, // Enable if you want to receive automatic update alerts
    "use_aliases": true // Enable to use command aliases
  },

  "presence": { // Presence config
    "status": "idle", // The bot's status, can be one of online/idle/dnd/invisible

    "activity": { // Activity config
      "type": "listening", // The bot's activity, can be one of playing/listening/watching/streaming/competing
      "text": "/help" // Custom part of the activity
    } // In this case, the bot's activity would be "Listening to /help"
  },

  "music": { // Music config
    "spotify": { // Spotify config
      "id": "...", // Spotify application ID
      "secret": "..." // Spotify application secret
    },

    "lyrics": "MusixMatch", // A lyrics provider, can be one of MusixMatch/Genius/LyricsFreak
    "alone": 300 // Time in seconds before the bot leaves a voice channel due to inactivity
  }
}
```

#### Notes
- If you are struggling at creating a bot, check [this guide](https://github.com/DV8FromTheWorld/JDA/wiki/3%29-Getting-Started#creating-a-discord-bot).
- [How to get your user snowflake ID](https://jmusicbot.com/finding-your-user-id/)
- [How to create a Spotify application](https://developer.spotify.com/documentation/general/guides/authorization/app-settings/)
- The Spotify application ID/secret can be `null`, but support for playing music from Spotify will be disabled in that case.
- Lyrics are currently broken, so setting a provider has no effect.

**Warning: Leave `eval` set to `false` if you don't know what you are doing. This is used purely for debugging. If someone wants you to enable this, there is an 11/10 chance they are trying to scam you.**

### Code evaluation

The bot can compile & run Kotlin DSL code at runtime with the `/system eval` command. You can access the majority of the bot's library via the supplied variables:
- `manager` - [`ApplicationManager`](https://github.com/PattexPattex/MusicGods/blob/master/src/main/java/com/pattexpattex/musicgods/ApplicationManager.java#L54)
- `ctx` - [`GuildContext`](https://github.com/PattexPattex/MusicGods/blob/master/src/main/java/com/pattexpattex/musicgods/GuildContext.java#L9)
- `event` - [`ButtonInteractionEvent`](https://ci.dv8tion.net/job/JDA5/javadoc/net/dv8tion/jda/api/events/interaction/component/ButtonInteractionEvent.html)

#### IntelliJ setup

For easier scripting, you can create Kotlin scripts in IntelliJ.

1. Clone this project and open it in IntelliJ.
2. Go to `File > New > Scratch File` or press `Ctrl + Alt + Shift + Insert`.
3. As the language, select `Kotlin` and hit `Enter`.
4. At the top of the editor, select `MusicGods.main` in the `Use classpath of module` dropdown menu.
5. Add these lines to the scratch file:

```kotlin
import com.pattexpattex.musicgods.ApplicationManager
import com.pattexpattex.musicgods.GuildContext
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
        
val event: ButtonInteractionEvent = TODO()
val manager: ApplicationManager = TODO()
val ctx: GuildContext = TODO()

/* Your code goes here */
```

And that's it!

To run the code, use the `/system eval` command. 
If the `code` option is left empty, a modal will popup with a bigger text input field.

Make sure to include any additional imports as they are not added automatically.

Upon successful evaluation, the bot will print the value returned by the script, e. g.:
- `2+2` returns `4`
- `println("Hello eval!")` returns `null` as the [return type of `println()` is `Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/println.html).

Example that sends you a DM:
```kotlin
event.user.openPrivateChannel().queue {
	it.sendMessage("Hello DMs!").queue()
}
```

### Runtime flags

Runtime flags are used as arguments for the bot.

Usage: `java -jar MusicGods-X.X.X.jar [FLAGS]`

List of flags: 

| Long        | Short | Description                                               |
|-------------|-------|-----------------------------------------------------------|
| `--lazy`    | `-l`  | Do not update slash commands on startup                   |
| `--update`  | `-up` | Perform update migrations (if there are any) and shutdown |
| `--verbose` | `-v`  | Set log level to ALL                                      |

### Update migrations

When updating your bot, you can start it with the `-up` flag. If present, the bot will automatically perform any migration tasks,
such as updating the config structure, delete some files or something else. This is simply to reduce any incompatibilities when updating.

```
> java -jar MusicGods-X.X.X.jar -up
[18:28:43.628] [main] [com.pattexpattex.musicgods.Launcher] [INFO] - Using flags 'update'
[18:28:43.638] [main] [com.pattexpattex.musicgods.Launcher] [INFO] - Found ffmpeg...
[18:28:43.642] [main] [com.pattexpattex.musicgods.Launcher] [INFO] - Found youtube-dl...

 __  __           _       _____           _
|  \/  |         (_)     / ____|         | |
| \  / |_   _ ___ _  ___| |  __  ___   __| |___
| |\/| | | | / __| |/ __| | |_ |/ _ \ / _` / __|
| |  | | |_| \__ \ | (__| |__| | (_) | (_| \__ \  X.X.X
|_|  |_|\__,_|___/_|\___|\_____|\___/ \__,_|___/

  (https://github.com/PattexPattex/MusicGods)

[18:28:43.624] [main] [com.pattexpattex.musicgods.Bot] [INFO] - Starting MusicGods...
[18:28:43.650] [main] [c.p.musicgods.config.Config] [INFO] - Successfully read config from 'config.json'...
[18:28:44.881] [main] [net.dv8tion.jda.api.JDA] [INFO] - Login Successful!
[18:28:45.229] [JDA MainWS-ReadThread] [n.d.j.i.requests.WebSocketClient] [INFO] - Connected to WebSocket
[18:28:45.505] [JDA MainWS-ReadThread] [net.dv8tion.jda.api.JDA] [INFO] - Finished Loading!
[18:28:45.506] [JDA MainWS-ReadThread] [c.p.musicgods.ApplicationManager] [INFO] - Available guilds: 1 | Unavailable guilds: 0 | Total guilds: 1
[18:28:45.506] [JDA MainWS-ReadThread] [c.p.musicgods.ApplicationManager] [INFO] - Performing update migration...
[18:28:45.509] [JDA MainWS-ReadThread] [c.p.musicgods.ApplicationManager] [INFO] - Finished migration, please restart.
```

## Submitting an issue

**Before submitting anything, please check the issues list for any similar issues.** 
If you have a suggestion or need help, please start a discussion.
If you would like to suggest a feature or file a bug report, open an issue.

## Editing, building and contributing

This bot and its source code may be hard to read for inexperienced programmers. 
If you want to edit the source code, please do so under the conditions of the MIT license.

After importing the project, simply execute `gradlew build` in the command line.

If you wish to contribute code to this bot, please open a pull request with details of your contribution.

# MusicGods

#### A powerful Discord music bot that you can easily host yourself!

Some of its features include:
- Advanced music playback
- Native Spotify support
- Native YouTube/Spotify track downloader
- Custom equalizer settings 
- An easy interface with the user
- Works purely with slash commands

...and more!

**The bot is currently still in alpha and is in active development!**
 
This project heavily depends on the [JDA](https://github.com/DV8FromTheWorld/JDA) and [Lavaplayer](https://github.com/sedmelluq/lavaplayer) libraries and was inspired by [JMusicBot](https://github.com/jagrosh/MusicBot).

## Getting started

- To see a list of all commands, use `/help` and select a group from the selection menu.
- Start playing music with `/play` or `/playfirst`. The bot will join your voice channel and start playing music!
- Search YouTube with `/search`. Select a track to start playing it.
- A basic GUI to control music is available with the `/queue` command.

## Supported audio sources

Basically everything that is supported by [Lavaplayer](https://github.com/sedmelluq/lavaplayer).

- YouTube,
- **Spotify**,
- SoundCloud,
- Bandcamp,
- Vimeo,
- HTTP(s) URLs...

##### Formats:

- MP3,
- FLAC,
- WAV,
- WebM (AAC / Opus / Vorbis),
- MP4 / M4A (AAC),
- OGG (Opus / Vorbis / FLAC),
- AAC...

## Setup

**This bot requires [Java 17](https://adoptium.net/temurin/releases/), along with [ffmpeg](https://ffmpeg.org) and [youtube-dl](https://github.com/ytdl-org/youtube-dl) (available in the PATH).**

FFMpeg and youtube-dl are bundled with the bot, but they work only on Windows.

### Download

Grab the latest release from the [releases](https://github.com/PattexPattex/MusicGods/releases), either the `.zip` file or the `.jar` file.
Place the file in a suitable directory.

##### If you downloaded the .jar file:

... just start the jar in the command line: `java -jar MusicGods-X.X.X.jar`.

_If you are restarting the bot frequently, you can add the argument `-l` or `--lazy` to the end of the command line to 
prevent the bot updating slash commands on startup._

##### If you downloaded the .zip file:

... extract the file, then navigate to `/bin` and run the `MusicGods.bat` file. 
The batch file will automatically warn you if you have an invalid java installation.

### Config

On the first startup, the bot should create a new `config.json` file and exit:

``
[16:47:26.122] [main] [c.p.musicgods.config.Config] [INFO] - Created new config file in 'config.json', please fill it out.
``

#### Config fields

##### Basic
- `token`: Your bot token. If you don't have a bot, [create one](https://github.com/DV8FromTheWorld/JDA/wiki/3%29-Getting-Started#creating-a-discord-bot).
- `owner`: Your user ID. If you don't know your ID, check [this guide](https://jmusicbot.com/finding-your-user-id/).
- `eval`: This is currently unused, leave it as-is.

##### Presence
- `status`: The bot status. Valid options are `online`, `idle`, `dnd` and `invisible`.
- `activity/type`: The bot activity. Valid options are `playing`, `listening`, `watching`, `streaming` and `competing`.
- `activity/text`: The bot activity custom text.

##### Music
- `spotify/id`: Your Spotify application ID. If you don't have an application check [this guide](https://developer.spotify.com/documentation/general/guides/authorization/app-settings/).
- `spotify/secret`: Your Spotify application secret.
- `lyrics`: A lyrics' provider. Valid options are `A-Z Lyrics`, `MusixMatch`, `Genius` and `LyricsFreak` 
but I recommend sticking with A-Z Lyrics, since it is the most accurate one.
- `alone`: The timeout in seconds before the bot leaves a voice channel when alone.

## Submitting an issue

**Before submitting anything, please check the issues list for any similar issues.** 
If you have a suggestion or need help, please start a discussion. 
If you would like to suggest a feature or file a bug report, open an issue.

## Editing, building and contributing

This bot and its source code may be hard to read for inexperienced programmers. 
If you want to edit the source code, please do so under the conditions of the MIT license.

After importing the project, simply execute `gradlew build` in the command line.
To build a working distribution, use `gradlew assembleDist` or `gradlew assembleShadowDist`.

If you wish to contribute code to this bot, please open a pull request.

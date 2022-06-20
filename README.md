# MusicGods

[![CodeFactor](https://www.codefactor.io/repository/github/pattexpattex/musicgods/badge?style=flat-square)](https://www.codefactor.io/repository/github/pattexpattex/musicgods)
![Lines of code](https://img.shields.io/tokei/lines/github/PattexPattex/MusicGods?label=lines%20of%20code&style=flat-square)
[![License](https://img.shields.io/github/license/PattexPattex/MusicGods?style=flat-square)](https://github.com/PattexPattex/MusicGods/blob/master/LICENSE)
[![Latest release](https://img.shields.io/github/v/release/PattexPattex/MusicGods?include_prereleases&style=flat-square)](https://github.com/PattexPattex/MusicGods/releases/latest)
[![Issues](https://img.shields.io/github/issues/PattexPattex/MusicGods?style=flat-square)](https://github.com/PattexPattex/MusicGods/issues)
[![Ko-fi](https://img.shields.io/badge/donate-Ko--fi-red?style=flat-square&logo=kofi)](https://ko-fi.com/pattexpattex)

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
 
This project heavily depends on the [JDA](https://github.com/DV8FromTheWorld/JDA) and [Lavaplayer](https://github.com/Walkyst/lavaplayer-fork) libraries and was inspired by [JMusicBot](https://github.com/jagrosh/MusicBot).

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

Grab the [latest release](https://github.com/PattexPattex/MusicGods/releases/latest), the `.zip` file or the `.jar` file.
Place the file in a suitable directory.

##### If you downloaded the .jar file:

... just start the jar in the command line: `java -jar MusicGods-X.X.X.jar`.

_If you are restarting the bot frequently, you can add the argument `-l` or `--lazy` to the end of the command line to 
prevent the bot updating slash commands on startup._

##### If you downloaded the .zip file:

... extract the file, then navigate to `/bin` and run the `MusicGods.bat` file. 
The batch file will automatically warn you if you have an invalid Java installation.

### Config

On the first startup, the bot should create a new `config.json` file and exit:

``
[16:47:26.122] [main] [c.p.musicgods.config.Config] [INFO] - Created new config file in 'config.json', please fill it out.
``

| Name             | Description                                                                                                                                                                | Default           | Options                                                      |
|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------|--------------------------------------------------------------|
| `token`          | Your bot token. If you don't have a bot, [create one](https://github.com/DV8FromTheWorld/JDA/wiki/3%29-Getting-Started#creating-a-discord-bot).                            | `null`            | /                                                            |
| `owner`          | Your user ID. If you don't know your ID, check [this guide](https://jmusicbot.com/finding-your-user-id/).                                                                  | `0`               | /                                                            |
| `eval`           | Enable this if you want to run arbitrary Java code from the bot.                                                                                                           | `false`           | `true, false`                                                |
| `status`         | The status displayed on the bot's account.                                                                                                                                 | `online`          | `online`, `idle`, `dnd`, `invisible`                         |
| `activity/type`  | The bot's activity.                                                                                                                                                        | `playing`         | `playing`, `listening`, `watching`, `streaming`, `competing` |
| `activity/text`  | The custom part of the activity.                                                                                                                                           | `/help`           | /                                                            |
| `spotify/id`     | Your Spotify application ID. If you don't have an application, check [this guide](https://developer.spotify.com/documentation/general/guides/authorization/app-settings/). | `null`            | /                                                            |
| `spotify/secret` | Your Spotify application secret.                                                                                                                                           | `null`            | /                                                            |
| `lyrics`         | A provider for lyrics. I recommend A-Z Lyrics, since it is the most accurate one.                                                                                          | `A-Z Lyrics`      | `A-Z Lyrics`, `MusixMatch`, `Genius`, `LyricsFreak`          |
| `alone`          | The timeout in seconds before the bot leaves a voice channel when it is alone.                                                                                             | `300` - 5 minutes | /                                                            |

**Warning: Leave `eval` set to `false` if you don't know what you are doing. This is used purely for debugging. If someone wants you to enable this, there is an 11/10 chance they are trying to scam you.**

## Submitting an issue

**Before submitting anything, please check the issues list for any similar issues.** 
If you have a suggestion or need help, please start a discussion.
If you would like to suggest a feature or file a bug report, open an issue.

## Editing, building and contributing

This bot and its source code may be hard to read for inexperienced programmers. 
If you want to edit the source code, please do so under the conditions of the MIT license.

After importing the project, simply execute `gradlew build` in the command line.

If you wish to contribute code to this bot, please open a pull request with details of your contribution.

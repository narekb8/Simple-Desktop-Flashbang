# Simple-Desktop-Flashbang
A lightweight program that can, either randomly or with twitch integration, create a flashbang effect on your screen.

## What It Works With
~~The program is extremely finnicky. To my knowledge, as long as your game is running in Borderless Windowed mode, you can probably get it to overlay above it properly. It probably won't work with a game that auto-pauses when alt tabbing. It definitely won't work if you're actively clicking with your mouse, as you'll click onto the overlayed window. Basically, borderless window and a controller means it should be good to go(?)~~

With release 1.1.0, the app should work with most games when set to borderless window now! The flash effect now no longer blocks mouse clicks from passing through to the game below it, and is about as unobstructive as possible. I've only tested it on a handful of games, so if you find one that doesn't work or some cool ones that do feel free to let me know!

Known good games: osu!, unnamed sdvx clone, Rivals of Aether.

## How To Use
Program requires Java 8 or newer.

For non-Twitch integrated use, simply open the program and hit the Flashes On button to begin!

For Twitch integration, open the program and press the "Connect to Twitch" button. It will take you to an authorization page.

### DO NOT CLOSE OUT OF THIS PAGE!!! THE PROGRAM WILL HANG

After hitting allow, the program should load all the information in. Make sure to tick the Twitch Integration box, and select the options you would like to listen for. When ready, click the Flashes On button. To turn off Twitch Integration and use the local random timer, simply untick the Twitch Integration checkbox.

Settings will save when closing the program as long as the program isn't actively listening for/generating flashbangs.

### Slight warning, I haven't tried using it with gifted subs at all. All I can say is good luck.

[Download the .jar here]([https://github.com/narekb8/Simple-Desktop-Flashbang/releases/tag/v1.0.0](https://github.com/narekb8/Simple-Desktop-Flashbang/releases/download/v1.1.0/Simple-Desktop-Flashbang.jar))

## Special Credits / Shoutouts

I have to give two major shoutouts for making this program possible in the state that it is in. Firstly, to Matthew Bell for their old Java Twitch API Wrapper (https://github.com/urgrue/Java-Twitch-Api-Wrapper) which is the backbone for the user-friendly Twitch connections. Secondly to Puncia, who made their own version of my program in C# with all of the extended window hooks and settings, making my life a lot easier for figuring all of this out. Here's the link to their github repo: https://github.com/Puncia/TwitchFlashbang

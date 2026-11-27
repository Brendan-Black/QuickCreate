# QuickCreate

![Minecraft 26.3](https://img.shields.io/badge/Minecraft-26.3-informational)
![Java 25](https://img.shields.io/badge/Java-25-informational)
[![License: Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

One-line server commands for the things that otherwise take a page of them: a crowd of mobs, a fountain of tamed pets,
a player made immortal. A [Graft](https://github.com/Brendan-Black/Graft) mod for **the server only**: everything it
makes is vanilla, so players join with their usual game and nothing to install.

## Commands

For operators. Players default to whoever runs the command.

```
/qc spawn <mob> [count] [players]                 a crowd around each player, each mob in a place of its own
/qc fountain <mob> [count] [players]              one a tick, thrown up from above each player as they move
/qc pets cats|dogs [count] [players] [babies]     a fountain of pets: tamed to the player, immortal, every coat in
                                                  turn and a random collar; babies stay babies
/qc immortal [players]                            Resistance V and the rest, for good (the void and /kill still kill)
/qc heal [players]                                full health and food
```

For example, `/qc pets cats 100 Alex babies` gives Alex 100 kittens, and `/qc spawn zombie 20` surrounds you
with twenty zombies.

Counts are 1 to 500 for each player, and at most 1,000 for one command. Every mob is made on its own, never by copying
others, so a count is exactly that many. Pets are immortal, so a crowd of them can't crush itself.

## Installing

Graft 0.1.1 or later on the server (the first to read `side=server`), Minecraft 26.3. Put the QuickCreate jar in the
server's `mods/` folder and restart it; with [GraftKit](https://github.com/Brendan-Black/GraftKit), `graftkit mods build
QuickCreate` builds and publishes it. Players' games don't need it, and a Graft server doesn't ask joining players for
it.

## Building

You need JDK 25, or let Gradle download it. Graft's API, loader and Gradle plugin come from JitPack.

```console
$ ./gradlew check       # formatting, compiler warnings, nulls, unit tests and game tests
$ ./gradlew runServer   # a server with QuickCreate
$ ./run.sh              # from WSL: copies the source to the prison (F:\prison\QuickCreate) and runs it on Windows
```

The code follows [Black Java](https://github.com/Brendan-Black/black-java), and the build enforces it: `./gradlew
format` formats every Java file.

`/reload` rebuilds the server's commands without anyone's mods' commands; QuickCreate puts `/qc` back on the next tick.
Graft has no command event yet, and `CommandInstall` is the one class that will change when it does.

## License

[Apache-2.0](LICENSE). Minecraft is Mojang's; QuickCreate isn't affiliated with Mojang or Microsoft.

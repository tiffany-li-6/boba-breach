Boba Breach -- Teabug Studios (Team 8)
Golden Master Release
May 17, 2026
CS 3152
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# 🧋 Boba Breach

An interactive 2D Boba Action-Shooter game developed as part of CS 3152 at Cornell University.

### 🔗 Quick Links
* **[Official Website & Downloads](https://gdiac.cs.cornell.edu/gdiac/showcase/games/boba_breach/)** — Play the game and view project info.


Game Controls

You may move the currently-selected helper bug with right mouse button (left mouse button shoots from the slingshot).
For levels with multiple helper bugs, you can toggle between them with Space.
You can also press Q and E to control the first/second helper bugs 
respectively (in case you're on a trackpad). 

W/S to move slingshot up/down (can be done in any mode)
Mouse/Trackpad - angle the slingshot

Slingshot mode:
LMB - fire boba projectile

Helper bug mode:
RMB - set the destination for helper bug; cannot set destination inside a non-ingredient crate
If selected destination is either a crate with ingredients or the cup, the helper
will pick up an ingredient (crate) or drop off a carried ingredient (cup)

Helpers can also return their carried ingredient to a crate of the proper type.

# The following was the README.md content given by the course

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

This project was generated with a template including simple application launchers and an `ApplicationAdapter` extension that draws libGDX logo.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.

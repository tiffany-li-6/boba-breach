/*
 * GDXRoot.java
 *
 * This is the primary class file for running the game. It is the "static main"
 * of LibGDX. In this lab we once again return to using Game (instead of
 * ApplicationAdapter), as scene management is so much easier. Once again, take
 * note of the use of ScreenListener to allow scene switching.
 *
 * Based on the original PhysicsDemo Lab by Don Holden, 2007
 *
 * Author:  Walker M. White
 * Version: 2/8/2025
 */
package game.bobabreach;

import com.badlogic.gdx.*;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.assets.*;
import edu.cornell.gdiac.graphics.*;
import com.badlogic.gdx.audio.Music;
import game.bobabreach.GameObjects.Level;
//import edu.cornell.cis3152.physics.ragdoll.*;

interface MusicController {
    void refreshMusicVolume();
}

/**
 * Root class for a LibGDX.
 *
 * This class is technically not the ROOT CLASS. Each platform has another class
 * above this (e.g. PC games use DesktopLauncher) which serves as the true root.
 * However, those classes are unique to each platform, while this class is the
 * same across all plaforms. In addition, this functions as the root class all
 * intents and purposes, and you would draw it as a root class in an
 * architecture specification.
 */
public class GDXRoot extends Game implements ScreenListener, MusicController {
    /** AssetManager to load game assets (textures, sounds, etc.) */
    AssetDirectory directory;
    /** The spritebatch to draw the screen (VIEW CLASS) */
    private SpriteBatch batch;
    /** Scene for the asset loading screen (CONTROLLER CLASS) */
    private LoadingScene loading;
    /** Scene for the main menu */
    private MainMenuScene mainMenu;
    /** Scene for the pause menu*/
    private PauseMenuScene pauseMenu;
    /** Scene for the level selection menu */
    private LevelSelectScene levelSelect;
    /** Scene for the level completion screen*/
    private LevelCompleteScene levelComplete;
    /** Scene for the settings screen*/
    private SettingsScene settings;
    /** Music that may overlap in between scenes and needs to be handled here!*/
    private Music menuMusic;
    private Music gameplayMusic;
    private Music levelCompleteMusic;
    private Music levelFailMusic;
    /** The player progress! */
    private SaveData saveData;
    /** The currently selected level*/
    private int selectedLevelIndex;
    /** Player mode for the game proper (CONTROLLER CLASS) */
    private int current;
    /** The WorldController */
    private GameScene controller;

    /**
     * Creates a new game from the configuration settings.
     *
     * This method configures the asset manager, but does not load any assets
     * or assign any screen.
     */
    public GDXRoot() { }

    /**
     * Called when the Application is first created.
     *
     * This is method immediately loads assets for the loading screen, and
     * prepares the asynchronous loader for all other assets.
     */
    public void create() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
        });

        batch  = new SpriteBatch();

        // Create the loading scene
        loading = new LoadingScene("assets.json",batch,1);
        loading.setScreenListener(this);
        setScreen(loading);
    }

    /**
     * Called when the Application is destroyed.
     *
     * This is preceded by a call to pause().
     */
    public void dispose() {
        // Call dispose on our children
        setScreen(null);
        if (loading != null) {
            loading.dispose();
        }
        if (controller != null) {
            controller.dispose();
        }
        if (pauseMenu != null) {
            pauseMenu.dispose();
        }
        if (levelSelect != null) {
            levelSelect.dispose();
        }
        if (mainMenu != null){
            mainMenu.dispose();
        }
        if(menuMusic!=null){
            menuMusic.stop();
            menuMusic.dispose();
            menuMusic = null;
        }
        if(gameplayMusic!=null){
            gameplayMusic.stop();
            gameplayMusic.dispose();
            gameplayMusic = null;
        }
        if(levelCompleteMusic!=null){
            levelCompleteMusic.stop();
            levelCompleteMusic.dispose();
            levelCompleteMusic = null;
        }
        if(levelFailMusic!=null){
            levelFailMusic.stop();
            levelFailMusic.dispose();
            levelFailMusic = null;
        }
        if(settings != null) {
            settings.dispose();
            settings = null;
        }

        batch.dispose();
        batch = null;

        // Unload all of the resources
        if (directory != null) {
            directory.unloadAssets();
            directory.dispose();
            directory = null;
        }
        super.dispose();
    }

    /**
     * Called when the Application is resized.
     *
     * This can happen at any point during a non-paused state but will never
     * happen before a call to create().
     *
     * @param width  The new width in pixels
     * @param height The new height in pixels
     */
    public void resize(int width, int height) {
        if (loading != null) {
            loading.resize(width,height);
        }
        if (controller!= null) {
            controller.resize(width,height);
        }
        if(mainMenu!= null){
            mainMenu.resize(width, height);
        }
        if(pauseMenu != null){
            pauseMenu.resize(width, height);
        }
        if(levelSelect != null){
            levelSelect.resize(width,height);
        }
        if(levelComplete != null){
            levelComplete.resize(width, height);
        }
    }

    /**
     * Responds to a request from a child scene.
     *
     * Typically this is used to have a scene exit its player mode. The value
     * exitCode can be also used to implement menu options.
     *
     * @param screen   The screen requesting to exit
     * @param exitCode The state of the screen upon exit
     */
    public void exitScreen(Screen screen, int exitCode) {
        if (screen == loading) {
            directory = loading.getAssets();
            loading.dispose();
            loading = null;
            saveData = new SaveData();
            mainMenu = new MainMenuScene(directory, batch);
            mainMenu.setScreenListener(this);
            playMenuMusic();
            setScreen(mainMenu);
        } else if (screen == mainMenu) {
            if(exitCode == MainMenuScene.MENU_PLAY){
                levelSelect = new LevelSelectScene(directory, batch, saveData, saveData.getUnlockedUpTo());
                levelSelect.setScreenListener(this);
                levelSelect.setSpriteBatch(batch);
                setScreen(levelSelect);
            }else if(exitCode == MainMenuScene.MENU_EXIT){
                Gdx.app.exit();
            }else if(exitCode == MainMenuScene.MENU_SETTINGS){
                //mainMenu.dispose();
                //mainMenu = null;
                settings = new SettingsScene(directory, batch, saveData, false, this);
                settings.setScreenListener(this);
                settings.setSpriteBatch(batch);
                setScreen(settings);
            }
        } else if (screen == levelSelect) {
            if(exitCode == LevelSelectScene.LEVEL_SELECT_EXIT){
                levelSelect.dispose();
                levelSelect = null;
                if(mainMenu != null){
                    setScreen(mainMenu);
                }
                else{
                    mainMenu = new MainMenuScene(directory, batch);
                    mainMenu.setScreenListener(this);
                    playMenuMusic();
                    setScreen(mainMenu);
                }
            }else if(exitCode == LevelSelectScene.LEVEL_SELECT_PLAY){
                selectedLevelIndex = levelSelect.getSelectedLevel();
                levelSelect.dispose();
                levelSelect = null;
                if(mainMenu != null){
                    mainMenu.dispose();
                    mainMenu = null;
                }
                // Initialize the game world
                //controllers = new PhysicsScene[3];
                controller = new GameScene(directory, selectedLevelIndex, saveData);
                controller.setScreenListener(this);
                controller.setSpriteBatch(batch);
                playGameplayMusic();
                setScreen(controller);
            }
        } else if(screen == controller && exitCode == 99){
            pauseMenu = new PauseMenuScene(directory, batch, controller);
            pauseMenu.setScreenListener(this);
            pauseMenu.setSpriteBatch(batch);
            setScreen(pauseMenu);
        }else if(screen == pauseMenu) {
            if (exitCode == PauseMenuScene.PAUSE_RESUME) {
                controller.setPaused(false);
                setScreen(controller);
                pauseMenu.dispose();
                pauseMenu = null;
            } else if (exitCode == PauseMenuScene.PAUSE_EXIT_MENU) {
                controller.dispose();
                controller = null;
                pauseMenu.dispose();
                pauseMenu = null;
                mainMenu = new MainMenuScene(directory, batch);
                mainMenu.setScreenListener(this);
                playMenuMusic();
                setScreen(mainMenu);
            } else if (exitCode == PauseMenuScene.PAUSE_SETTINGS) {
                //placeholder cus no settings yet, same thing as resume
                settings = new SettingsScene(directory, batch, saveData, true,
                    this);
                settings.setScreenListener(this);
                setScreen(settings);
            } else if (exitCode == PauseMenuScene.PAUSE_LEVEL_SELECT) {
                controller.dispose();
                controller = null;
                pauseMenu.dispose();
                pauseMenu = null;
                levelSelect = new LevelSelectScene(directory, batch, saveData, selectedLevelIndex);
                levelSelect.setScreenListener(this);
                playMenuMusic();
                setScreen(levelSelect);
            }
        }else if(screen == settings){
            settings.dispose();
            settings = null;
            if (exitCode == SettingsScene.EXIT_TO_MAIN_MENU) {
                if (mainMenu == null) {
                    mainMenu = new MainMenuScene(directory, batch);
                    mainMenu.setScreenListener(this);
                    playMenuMusic();
                }
                setScreen(mainMenu);
            } else if (exitCode == SettingsScene.EXIT_TO_PAUSE) {
                if (pauseMenu == null) {
                    pauseMenu = new PauseMenuScene(directory, batch, controller);
                    pauseMenu.setScreenListener(this);
                    pauseMenu.setSpriteBatch(batch);
                }
                setScreen(pauseMenu);
            } else if (exitCode == SettingsScene.EXIT_RESET){
                if(controller != null){
                    controller.dispose();
                    controller = null;
                }
                if(pauseMenu != null){
                    pauseMenu.dispose();
                    pauseMenu = null;
                }
                mainMenu = new MainMenuScene(directory, batch);
                mainMenu.setScreenListener(this);
                playMenuMusic();
                setScreen(mainMenu);
            }

        }else if(screen == controller){
            if (exitCode == GameScene.EXIT_NEXT) {
                controller.reset();
            } else if (exitCode == GameScene.EXIT_PREV) {
                controller.reset();
            } else if (exitCode == GameScene.EXIT_QUIT) {
                // We quit the main application
                Gdx.app.exit();
            } else if (exitCode == GameScene.EXIT_WIN) {
                saveData.updateLevel(selectedLevelIndex, controller.getNumStars(),
                    controller.getTimeElapsed(), controller.getNumStunned(),
                    controller.getBugsKilled(), controller.getAccuracy());
                controller.pause();
                levelComplete = new LevelCompleteScene(directory, batch, controller, true);
                levelComplete.setScreenListener(this);
                playLevelCompleteMusic();
                setScreen(levelComplete);
            } else if (exitCode == GameScene.EXIT_LOSS){
                controller.pause();
                levelComplete = new LevelCompleteScene(directory, batch, controller, false);
                levelComplete.setScreenListener(this);
                playLevelFailMusic();
                setScreen(levelComplete);
            }
        }else if(screen == levelComplete){
            levelComplete.dispose();
            levelComplete = null;
            if(exitCode == LevelCompleteScene.EXIT_RETRY){
                controller.reset();
                playGameplayMusic();
                setScreen(controller);
            } else if (exitCode == LevelCompleteScene.EXIT_HOME){
                controller.dispose();
                controller = null;
                mainMenu = new MainMenuScene(directory, batch);
                mainMenu.setScreenListener(this);
                playMenuMusic();
                setScreen(mainMenu);
            } else if (exitCode == LevelCompleteScene.EXIT_LEVEL){
                controller.dispose();
                controller = null;
                levelSelect = new LevelSelectScene(directory, batch, saveData, selectedLevelIndex);
                playMenuMusic();
                levelSelect.setScreenListener(this);
                setScreen(levelSelect);
            }
        }
    }

    private void playMenuMusic(){
        stopGameplayMusic();
        stopLevelCompleteMusic();
        stopLevelFailMusic();
        if(menuMusic == null){
            menuMusic = Gdx.audio.newMusic(Gdx.files.internal("songs/music_maintitle.ogg"));
            menuMusic.setLooping(true);
        }
        menuMusic.setVolume(saveData.getMusicGain());
        if(!menuMusic.isPlaying()){
            menuMusic.play();
        }
    }

    private void playGameplayMusic() {
        stopMenuMusic();
        stopLevelCompleteMusic();
        stopLevelFailMusic();
        if(gameplayMusic == null){
            gameplayMusic = Gdx.audio.newMusic(Gdx.files.internal("songs/music_gameplay.ogg"));
            gameplayMusic.setLooping(true);
        }
        gameplayMusic.setVolume(saveData.getMusicGain());
        if(!gameplayMusic.isPlaying()){
            gameplayMusic.play();
        }

    }

    private void playLevelCompleteMusic() {
        stopGameplayMusic();
        stopMenuMusic();
        stopLevelFailMusic();
        if(levelCompleteMusic == null){
            levelCompleteMusic = Gdx.audio.newMusic(Gdx.files.internal("songs/music_levelcomplete.ogg"));
            levelCompleteMusic.setLooping(true);
        }
        levelCompleteMusic.setVolume(saveData.getMusicGain());
        if(!levelCompleteMusic.isPlaying()){
            levelCompleteMusic.play();
        }
    }

    private void playLevelFailMusic() {
        stopGameplayMusic();
        stopMenuMusic();
        stopLevelCompleteMusic();
        if(levelFailMusic == null){
            levelFailMusic = Gdx.audio.newMusic(Gdx.files.internal("songs/music_levelLost.ogg"));
            levelFailMusic.setLooping(true);
        }
        levelFailMusic.setVolume(saveData.getMusicGain());
        if(!levelFailMusic.isPlaying()){
            levelFailMusic.play();
        }
    }

    public void stopMenuMusic() {
        if(menuMusic != null){
            menuMusic.stop();
        }
    }

    public void stopGameplayMusic() {
        if(gameplayMusic != null){
            gameplayMusic.stop();
        }
    }

    public void stopLevelCompleteMusic() {
        if(levelCompleteMusic != null){
            levelCompleteMusic.stop();
        }
    }

    public void stopLevelFailMusic() {
        if(levelFailMusic != null){
            levelFailMusic.stop();
        }
    }

    @Override
    public void refreshMusicVolume() {
        float v = saveData.getMusicGain();
        if(menuMusic!=null){
            menuMusic.setVolume(v);
        }
        if(gameplayMusic!=null){
            gameplayMusic.setVolume(v);
        }
        if(levelCompleteMusic!=null){
            levelCompleteMusic.setVolume(v);
        }
        if(levelFailMusic!=null){
            levelFailMusic.setVolume(v);
        }
    }

}

package game.bobabreach;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.backend.GDXAppSettings;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextLayout;
import edu.cornell.gdiac.util.ScreenListener;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import edu.cornell.gdiac.graphics.TextAlign;

public class SettingsScene implements Screen {

    //exit codes
    public static final int EXIT_TO_MAIN_MENU = 0;
    public static final int EXIT_TO_PAUSE = 1;
    public static final int EXIT_RESET = 2;

    private static final int VOLUME_MIN = 0;
    private static final int VOLUME_MAX = 100;
    private static final int VOLUME_DEFAULT = 50;

    private static final float HOLD_DELAY = 0.3f;
    private static final float HOLD_INTERVAL = 0.07f;

    private final AssetDirectory directory;
    private SpriteBatch batch;
    private ScreenListener listener;
    private boolean active;
    private final boolean fromPause;
    private SaveData saveData;
    private MusicController musicController;

    private Music currentMusic;
    private Sound previewSound;

    private OrthographicCamera camera;
    private Viewport viewport;
    private float logicalWidth;
    private float logicalHeight;

    private final com.badlogic.gdx.math.Vector3 mouseCoords = new com.badlogic.gdx.math.Vector3();

    private TextureRegion settings_background;
    private TextureRegion arrowLeft;
    private TextureRegion arrowLeft_hover;
    private TextureRegion arrowRight;
    private TextureRegion arrowRight_hover;

    private TextureRegion backBtn;
    private TextureRegion backBtn_hover;
    private TextureRegion resetBtn;
    private TextureRegion resetBtn_hover;

    private Rectangle musicLeftRect;
    private Rectangle musicRightRect;
    private Rectangle sfxLeftRect;
    private Rectangle sfxRightRect;
    private Rectangle backRect;
    private Rectangle resetRect;

    private int musicVolume;
    private int sfxVolume;

    private enum ArrowBtn {MUSIC_LEFT, MUSIC_RIGHT, SFX_LEFT, SFX_RIGHT, NONE}

    private ArrowBtn held = ArrowBtn.NONE;
    private float holdTimer = 0f;
    private float repeatAccum = 0f;

    private BitmapFont uiFont;
    private TextLayout musicValueLayout;
    private TextLayout sfxValueLayout;

    private float leftX, rightX, topY, bottomY;
    private float arrowW, arrowH;
    private float valueDisplayX;
    private float buttonW, buttonH;
    private float resetX, resetY;
    private float backX, backY;
    private float volumeCenterX;

    public SettingsScene(AssetDirectory directory, SpriteBatch batch, SaveData saveData, boolean fromPause, MusicController musicController){
        this.directory = directory;
        this.batch = batch;
        this.saveData = saveData;
        this.fromPause = fromPause;
        this.musicController = musicController;
        this.musicVolume = saveData.getMusicVolume();
        this.sfxVolume = saveData.getSfxVolume();

        musicController.refreshMusicVolume();

        loadAssets();

        musicLeftRect = new Rectangle(leftX, topY, arrowW, arrowH);
        musicRightRect = new Rectangle(rightX, topY, arrowW, arrowH);
        sfxLeftRect = new Rectangle(leftX, bottomY, arrowW, arrowH);
        sfxRightRect = new Rectangle(rightX, bottomY, arrowW, arrowH);
        resetRect = new Rectangle(resetX, resetY, buttonW, buttonH);
        backRect = new Rectangle(backX, backY, buttonW, buttonH);

        musicValueLayout = new TextLayout();
        musicValueLayout.setFont(uiFont);
        musicValueLayout.setAlignment(TextAlign.topCenter);
        musicValueLayout.setColor(Color.BLACK);

        sfxValueLayout = new TextLayout();
        sfxValueLayout.setFont(uiFont);
        sfxValueLayout.setAlignment(TextAlign.topCenter);
        sfxValueLayout.setColor(Color.BLACK);

        camera = new OrthographicCamera();
        viewport = new FitViewport(logicalWidth, logicalHeight, camera);
        camera.position.set(logicalWidth/2f, logicalHeight/2f, 0);
        camera.update();

        active = false;
    }

    private void loadAssets() {
        JsonValue constants = directory.getEntry("constants", JsonValue.class);
        JsonValue set = constants.get("settings menu");
        logicalWidth = set.getFloat("logical width");
        logicalHeight = set.getFloat("logical height");
        leftX = set.getFloat("left arrow x");
        rightX = set.getFloat("right arrow x");
        topY = set.getFloat("top arrow y");
        bottomY = set.getFloat("bottom arrow y");
        buttonW = set.getFloat("button width");
        buttonH = set.getFloat("button height");
        resetX = set.getFloat("reset x");
        resetY = set.getFloat("reset y");
        backX = set.getFloat("back x");
        backY = set.getFloat("back y");
        valueDisplayX = set.getFloat("volume display x");
        arrowW = set.getFloat("arrow width");
        arrowH = set.getFloat("arrow height");

        settings_background = new TextureRegion(directory.getEntry("set_bg", Texture.class));
        arrowRight = new TextureRegion(directory.getEntry("set_right", Texture.class));
        arrowLeft = new TextureRegion(directory.getEntry("set_left", Texture.class));
        backBtn = new TextureRegion(directory.getEntry("set_back", Texture.class));
        backBtn_hover = new TextureRegion(directory.getEntry("set_back_hover", Texture.class));
        resetBtn = new TextureRegion(directory.getEntry("set_reset", Texture.class));
        resetBtn_hover = new TextureRegion(directory.getEntry("set_reset_hover", Texture.class));

        uiFont = directory.getEntry("settings font", BitmapFont.class);
    }

    @Override
    public void show() {
        active = true;
        Gdx.input.setCursorCatched(false);
    }

    @Override
    public void hide() {
        active = false;
        Gdx.input.setCursorCatched(true);
    }

    @Override
    public void render(float delta){
        if (active){
            update(delta);
            draw();
        }
    }

    @Override
    public void resize(int width, int height){
        if(viewport != null) {
            viewport.update(width, height, true);
            camera.position.set(logicalWidth /2f, logicalHeight/2f, 0);
            camera.update();
        }
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {}

    private void update(float delta) {
        if(!active){
            return;
        }

        float mx = getLogicalMouseX();
        float my = getLogicalMouseY();

        ArrowBtn hovered = ArrowBtn.NONE;
        if (musicLeftRect.contains(mx, my)){
            hovered = ArrowBtn.MUSIC_LEFT;
        } else if (musicRightRect.contains(mx, my)){
            hovered = ArrowBtn.MUSIC_RIGHT;
        } else if (sfxLeftRect.contains(mx, my)){
            hovered = ArrowBtn.SFX_LEFT;
        } else if (sfxRightRect.contains(mx, my)){
            hovered = ArrowBtn.SFX_RIGHT;
        }

        boolean justClicked = Gdx.input.justTouched();
        boolean mouseDown = Gdx.input.isTouched();

        if(justClicked) {
            if(backRect.contains(mx, my)){
                saveData.save();
                int code = fromPause ? EXIT_TO_PAUSE : EXIT_TO_MAIN_MENU;
                listener.exitScreen(this, code);
                return;
            }
            if(resetRect.contains(mx, my)){
                saveData.reset();
                musicVolume = saveData.getMusicVolume();
                sfxVolume = saveData.getSfxVolume();
                musicController.refreshMusicVolume();
                listener.exitScreen(this, EXIT_RESET);
                return;
            }

            held = hovered;
            applyArrowTick(held);

            holdTimer = 0f;
            repeatAccum = 0f;
        }
        if(mouseDown && held != ArrowBtn.NONE) {
            holdTimer += delta;
            if (holdTimer >= HOLD_DELAY) {
                repeatAccum += delta;
                while (repeatAccum >= HOLD_INTERVAL) {
                    applyArrowTick(held);
                    repeatAccum -= HOLD_INTERVAL;
                }
            }
        } else {
            held = ArrowBtn.NONE;
            holdTimer = 0f;
            repeatAccum = 0f;
        }
    }

    private void applyArrowTick(ArrowBtn btn) {
        switch (btn) {
            case MUSIC_LEFT:
                musicVolume = Math.max(VOLUME_MIN, musicVolume - 1);
                saveData.setMusicVolume(musicVolume);
                musicController.refreshMusicVolume();
                break;
            case MUSIC_RIGHT:
                musicVolume = Math.min(VOLUME_MAX, musicVolume + 1);
                saveData.setMusicVolume(musicVolume);
                musicController.refreshMusicVolume();
                break;
            case SFX_LEFT:
                sfxVolume = Math.max(VOLUME_MIN, sfxVolume - 1);
                saveData.setSfxVolume(sfxVolume);
                break;
            case SFX_RIGHT:
                sfxVolume = Math.min(VOLUME_MAX, sfxVolume + 1);
                saveData.setSfxVolume(sfxVolume);
                break;
            default:
                break;
        }
    }

    private void draw(){
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin(camera);

        float mx = getLogicalMouseX();
        float my = getLogicalMouseY();

        batch.draw(settings_background, 0, 0, logicalWidth, logicalHeight);

        batch.draw(arrowLeft, leftX, topY, arrowW, arrowH);
        batch.draw(arrowLeft, leftX, bottomY, arrowW, arrowH);
        batch.draw(arrowRight, rightX, topY, arrowW, arrowH);
        batch.draw(arrowRight, rightX, bottomY, arrowW, arrowH);

        musicValueLayout.setText(musicVolume + "%");
        musicValueLayout.layout();
        batch.drawText(musicValueLayout, valueDisplayX, topY+75f);

        sfxValueLayout.setText(sfxVolume + "%");
        sfxValueLayout.layout();
        batch.drawText(sfxValueLayout, valueDisplayX, bottomY+75f);

        batch.draw(backRect.contains(mx, my) ? backBtn_hover : backBtn, backRect.x, backRect.y, backRect.width, backRect.height);
        batch.draw(resetRect.contains(mx, my) ? resetBtn_hover : resetBtn, resetRect.x, resetRect.y, resetRect.width, resetRect.height);

        batch.end();
    }

    private float getLogicalMouseX() {
        mouseCoords.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouseCoords, viewport.getScreenX(), viewport.getScreenY(),
            viewport.getScreenWidth(), viewport.getScreenHeight());
        return mouseCoords.x;
    }

    private float getLogicalMouseY() {
        mouseCoords.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouseCoords, viewport.getScreenX(), viewport.getScreenY(),
            viewport.getScreenWidth(), viewport.getScreenHeight());
        return mouseCoords.y;
    }

    public void setScreenListener(ScreenListener listener){
        this.listener = listener;
    }

    public void setSpriteBatch(SpriteBatch batch) {
        this.batch = batch;
    }
}

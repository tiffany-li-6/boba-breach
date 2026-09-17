package game.bobabreach;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.util.ScreenListener;

public class PauseMenuScene implements Screen {

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ScreenListener listener;
    private AssetDirectory directory;
    private boolean active;

    private GameScene gameSceneRef;

    private final com.badlogic.gdx.math.Vector3 mouseCoords = new com.badlogic.gdx.math.Vector3();

    private float buttonWidth;
    private float buttonHeight;
    private float buttonCenterY;
    private float buttonCenterX;
    private float buttonSpacing;
    private float menuWidth;
    private float menuHeight;
    private float menuX;
    private float menuY;

    private TextureRegion resumeBtn;
    private TextureRegion resumeBtn_trans;
    private TextureRegion exitBtn;
    private TextureRegion exitBtn_trans;
    private TextureRegion settingsBtn;
    private TextureRegion settingsBtn_trans;
    private TextureRegion levelBtn;
    private TextureRegion levelBtn_trans;
    private TextureRegion pauseBackground;
    private TextureRegion overlay_texture;

    private Rectangle resumeRect;
    private Rectangle levelRect;
    private Rectangle exitRect;
    private Rectangle settingsRect;

    private float logicalWidth;
    private float logicalHeight;

    //exit codes!
    public static final int PAUSE_RESUME = 0;
    public static final int PAUSE_EXIT_MENU = 1;
    public static final int PAUSE_SETTINGS = 2;
    public static final int PAUSE_LEVEL_SELECT = 3;

    public PauseMenuScene(AssetDirectory directory, SpriteBatch batch, GameScene gameScene){
        this.directory = directory;
        this.batch = batch;
        this.gameSceneRef = gameScene;

        JsonValue constants = directory.getEntry("constants", JsonValue.class);
        JsonValue pauseConstants = constants.get("pause menu");

        buttonWidth = pauseConstants.getFloat("button width");
        buttonHeight = pauseConstants.getFloat("button height");
        buttonCenterX = pauseConstants.getFloat("button center x");
        buttonCenterY = pauseConstants.getFloat("button base y");
        buttonSpacing = pauseConstants.getFloat("button spacing");

        menuWidth = pauseConstants.getFloat("menu width");
        menuHeight = pauseConstants.getFloat("menu height");
        menuX = pauseConstants.getFloat("menu x");
        menuY = pauseConstants.getFloat("menu y");

        logicalWidth = pauseConstants.getFloat("logical width");
        logicalHeight = pauseConstants.getFloat("logical height");

        float bx = buttonCenterX - buttonWidth/2f;
        settingsRect = new Rectangle(bx, buttonCenterY, buttonWidth, buttonHeight);
        exitRect = new Rectangle(bx, buttonCenterY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight);
        levelRect = new Rectangle(bx, buttonCenterY + (buttonHeight+buttonSpacing)*2, buttonWidth, buttonHeight);
        resumeRect = new Rectangle(bx, buttonCenterY + (buttonHeight+buttonSpacing)*3, buttonWidth, buttonHeight);

        pauseBackground = new TextureRegion(directory.getEntry("pause_background", Texture.class));

        resumeBtn = new TextureRegion(directory.getEntry("btn_resume", Texture.class));
        resumeBtn_trans = new TextureRegion(directory.getEntry("btn_resume_trans", Texture.class));
        exitBtn = new TextureRegion(directory.getEntry("btn_exit", Texture.class));
        exitBtn_trans = new TextureRegion(directory.getEntry("btn_exit_trans", Texture.class));
        settingsBtn = new TextureRegion(directory.getEntry("btn_settings", Texture.class));
        settingsBtn_trans = new TextureRegion(directory.getEntry("btn_settings_trans", Texture.class));
        levelBtn = new TextureRegion(directory.getEntry("btn_level", Texture.class));
        levelBtn_trans = new TextureRegion(directory.getEntry("btn_level_trans", Texture.class));
        overlay_texture = new TextureRegion(directory.getEntry("pm_game_overlay", Texture.class));

        camera = new OrthographicCamera();
        viewport = new FitViewport(logicalWidth, logicalHeight, camera);
        camera.position.set(logicalWidth/2f, logicalHeight/2f, 0);
        camera.update();

        active = false;

    }

    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
    }

    public void setSpriteBatch(SpriteBatch batch) {
        this.batch = batch;
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

    @Override
    public void render(float delta){
        if (active){
            gameSceneRef.draw(delta);
            update(delta);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.position.set(logicalWidth / 2f, logicalHeight /2f, 0);
        camera.update();
    }

    public void update(float delta){
        if(!active){
            return;
        }

        float mx = getLogicalMouseX();
        float my = getLogicalMouseY();

        boolean clickedThisFrame = Gdx.input.justTouched();

        if(resumeRect.contains(mx,my) && clickedThisFrame){
            listener.exitScreen(this, PAUSE_RESUME);
            return;
        }
        if(exitRect.contains(mx,my) && clickedThisFrame){
            listener.exitScreen(this, PAUSE_EXIT_MENU);
            return;
        }
        if(settingsRect.contains(mx,my) && clickedThisFrame){
            listener.exitScreen(this, PAUSE_SETTINGS);
            return;
        }
        if(levelRect.contains(mx,my) && clickedThisFrame){
            listener.exitScreen(this, PAUSE_LEVEL_SELECT);
            return;
        }

        draw(mx, my);
    }

    private void draw(float mx, float my){
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin(camera);

        batch.setColor(0f,0f,0f,0.55f);
        batch.draw(overlay_texture, 0,0,logicalWidth,logicalHeight);
        batch.setColor(Color.WHITE);

        batch.draw(pauseBackground, menuX, menuY, menuWidth, menuHeight);

        TextureRegion settingsTex = settingsRect.contains(mx,my) ? settingsBtn : settingsBtn_trans;
        batch.draw(settingsTex, settingsRect.x, settingsRect.y, settingsRect.width, settingsRect.height);

        TextureRegion exitTex = exitRect.contains(mx,my) ? exitBtn : exitBtn_trans;
        batch.draw(exitTex, exitRect.x, exitRect.y, exitRect.width, exitRect.height);

        TextureRegion resumeTex = resumeRect.contains(mx,my) ? resumeBtn : resumeBtn_trans;
        batch.draw(resumeTex, resumeRect.x, resumeRect.y, resumeRect.width, resumeRect.height);

        TextureRegion levelTex = levelRect.contains(mx,my) ? levelBtn : levelBtn_trans;
        batch.draw(levelTex, levelRect.x, levelRect.y, levelRect.width, levelRect.height);

        batch.end();
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
    public void pause(){}

    @Override
    public void resume(){}

    @Override
    public void dispose(){
    }
}

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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.util.ScreenListener;

public class MainMenuScene implements Screen {
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ScreenListener listener;
    private AssetDirectory directory;
    private boolean active;

    private final com.badlogic.gdx.math.Vector3 mouseCoords = new com.badlogic.gdx.math.Vector3();

    private float buttonWidth;
    private float buttonHeight;
    private float buttonCenterY;
    private float buttonCenterX;
    private float buttonSpacing;

    private Texture background;
    private TextureRegion startBtn;
    private TextureRegion startBtn_trans;
    private TextureRegion exitBtn;
    private TextureRegion exitBtn_trans;
    private TextureRegion settingsBtn;
    private TextureRegion settingsBtn_trans;

    private Rectangle startRect;
    private Rectangle exitRect;
    private Rectangle settingsRect;

    private float logicalWidth;
    private float logicalHeight;

    public static final int MENU_SETTINGS = 0;
    public static final int MENU_PLAY = 1;
    public static final int MENU_EXIT = 2;

    //which button is hovered (-1 = none, 0 = start, 1 = level select, 2 = settings)
    private int hovered = -1;
    //which button was just clicked
    private int clicked = -1;

    public MainMenuScene(AssetDirectory directory, SpriteBatch batch) {
        this.directory = directory;
        this.batch = batch;

        JsonValue menuConstants = directory.getEntry("constants", JsonValue.class).get("main menu");

        buttonWidth = menuConstants.getFloat("button width");
        buttonHeight = menuConstants.getFloat("button height");
        buttonCenterX = menuConstants.getFloat("button center x");
        buttonCenterY = menuConstants.getFloat("button base y");
        buttonSpacing = menuConstants.getFloat("button spacing");

        float bx = buttonCenterX - buttonWidth/2f;
        exitRect = new Rectangle(bx, buttonCenterY, buttonWidth, buttonHeight);
        settingsRect = new Rectangle(bx, buttonCenterY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight);
        startRect = new Rectangle(bx, buttonCenterY + (buttonHeight+buttonSpacing)*2, buttonWidth, buttonHeight);

        background = directory.getEntry("mainmenu_background", Texture.class);
        startBtn = new TextureRegion(directory.getEntry("btn_start", Texture.class));
        startBtn_trans = new TextureRegion(directory.getEntry("btn_start_trans", Texture.class));
        exitBtn = new TextureRegion(directory.getEntry("btn_menu_exit", Texture.class));
        exitBtn_trans = new TextureRegion(directory.getEntry("btn_menu_exit_trans", Texture.class));
        settingsBtn = new TextureRegion(directory.getEntry("btn_settings", Texture.class));
        settingsBtn_trans = new TextureRegion(directory.getEntry("btn_settings_trans", Texture.class));

        logicalWidth = menuConstants.getFloat("logical width");
        logicalHeight =menuConstants.getFloat("logical height");

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
        if (!active){
            return;
        }

        float mx = getLogicalMouseX();
        float my = getLogicalMouseY();

        boolean clickedThisFrame = Gdx.input.justTouched();

        if(startRect.contains(mx,my) && clickedThisFrame){
            listener.exitScreen(this, MENU_PLAY);
            return;
        }

        if(exitRect.contains(mx,my) && clickedThisFrame){
            listener.exitScreen(this, MENU_EXIT);
            return;
        }

        if(settingsRect.contains(mx,my) && clickedThisFrame){
            listener.exitScreen(this, MENU_SETTINGS);
            return;
        }

        draw(mx, my);
    }

    private void draw(float mx, float my) {
        ScreenUtils.clear(0f,0f,0f,1f);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin(camera);

        batch.draw(background, 0, 0, logicalWidth, logicalHeight);

        TextureRegion settingsTex = settingsRect.contains(mx,my) ? settingsBtn : settingsBtn_trans;
        batch.draw(settingsTex, settingsRect.x, settingsRect.y, settingsRect.width, settingsRect.height);

        TextureRegion exitTex = exitRect.contains(mx,my) ? exitBtn : exitBtn_trans;
        batch.draw(exitTex, exitRect.x, exitRect.y, exitRect.width, exitRect.height);

        TextureRegion startTex = startRect.contains(mx,my) ? startBtn : startBtn_trans;
        batch.draw(startTex, startRect.x, startRect.y, startRect.width, startRect.height);

        batch.end();
    }

    @Override
    public void show(){
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
    public void resume() {}

    @Override
    public void dispose() {}
}

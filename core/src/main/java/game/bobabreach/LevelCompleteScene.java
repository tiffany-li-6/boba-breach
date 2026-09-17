package game.bobabreach;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextAlign;
import edu.cornell.gdiac.graphics.TextLayout;
import edu.cornell.gdiac.util.ScreenListener;

public class LevelCompleteScene implements Screen {

    public static final int EXIT_RETRY = 0;
    public static final int EXIT_HOME = 1;
    public static final int EXIT_LEVEL = 2;

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ScreenListener listener;
    private AssetDirectory directory;
    private boolean active;

    //whether or not the level was completed
    private boolean isWin;

    private GameScene gameSceneRef;

    private final com.badlogic.gdx.math.Vector3 mouseCoords = new com.badlogic.gdx.math.Vector3();

    private float logicalWidth;
    private float logicalHeight;
    private float ticketX, ticketY, ticketWidth, ticketHeight;
    private float starsX, starsY, starsWidth, starsHeight;
    private float retryX, retryY, retryWidth, retryHeight;
    private float homeX, homeY, homeWidth, homeHeight;
    private float levelX, levelY, levelWidth, levelHeight;
    private float textCenterX, textY;

    private float statsX, statsY, numbersX, numbersSpace;

    private TextureRegion ticketTexture;
    private TextureRegion starsTexture;
    private TextureRegion retryBtn;
    private TextureRegion retryBtn_trans;
    private TextureRegion homeBtn;
    private TextureRegion homeBtn_trans;
    private TextureRegion levelSelectBtn;
    private TextureRegion levelSelectBtn_trans;
    private TextureRegion game_overlay;

    private Rectangle retryRect;
    private Rectangle homeRect;
    private Rectangle levelRect;

    private TextLayout headerText;
    private BitmapFont headerFont;
    private TextLayout statsText;
    private BitmapFont statsFont;

    private int time_mins;
    private String time_secs;
    private int stuns;
    private int kills;
    private float accuracy;

    public LevelCompleteScene(AssetDirectory directory, SpriteBatch batch, GameScene gameScene, boolean isWin) {
        this.directory = directory;
        this.batch = batch;
        this.gameSceneRef = gameScene;
        this.isWin = isWin;

        JsonValue constants = directory.getEntry("constants", JsonValue.class);
        JsonValue lc = constants.get("level complete");

        logicalWidth = lc.getFloat("logical width");
        logicalHeight = lc.getFloat("logical height");
        ticketX = lc.getFloat("ticket x");
        ticketY = lc.getFloat("ticket y");
        ticketWidth = lc.getFloat("ticket width");
        ticketHeight = lc.getFloat("ticket height");
        starsX = lc.getFloat("stars x");
        starsY = lc.getFloat("stars y");
        starsWidth = lc.getFloat("stars width");
        starsHeight = lc.getFloat("stars height");
        retryX = lc.getFloat("retry x");
        retryY = lc.getFloat("retry y");
        retryWidth = lc.getFloat("retry width");
        retryHeight = lc.getFloat("retry height");
        homeX = lc.getFloat("home x");
        homeY = lc.getFloat("home and level y");
        homeWidth = lc.getFloat("home width");
        homeHeight = lc.getFloat("home and level height");
        levelX = lc.getFloat("level x");
        levelY = lc.getFloat("home and level y");
        levelWidth = lc.getFloat("level width");
        levelHeight = lc.getFloat("home and level height");
        textCenterX = lc.getFloat("text center x");
        textY = lc.getFloat("text y");
        statsX = lc.getFloat("stats x");
        statsY = lc.getFloat("stats y");
        statsX = lc.getFloat("stats x");
        numbersX = lc.getFloat("numbers x");
        numbersSpace = lc.getFloat("stats spacing");

        ticketTexture = new TextureRegion(directory.getEntry("lc_ticket", Texture.class));
        retryBtn = new TextureRegion(directory.getEntry("lc_btn_retry", Texture.class));
        retryBtn_trans = new TextureRegion(directory.getEntry("lc_btn_retry_trans", Texture.class));
        homeBtn = new TextureRegion(directory.getEntry("lc_btn_home", Texture.class));
        homeBtn_trans = new TextureRegion(directory.getEntry("lc_btn_home_trans", Texture.class));
        levelSelectBtn = new TextureRegion(directory.getEntry("lc_btn_level", Texture.class));
        levelSelectBtn_trans = new TextureRegion(
            directory.getEntry("lc_btn_level_trans", Texture.class));
        game_overlay = new TextureRegion(directory.getEntry("lc_game_overlay", Texture.class));

        retryRect = new Rectangle(retryX, retryY, retryWidth, retryHeight);
        homeRect = new Rectangle(homeX, homeY, homeWidth, homeHeight);
        levelRect = new Rectangle(levelX, levelY, levelWidth, levelHeight);

        headerFont = directory.getEntry("tiltWarp", BitmapFont.class);
        headerText = new TextLayout();
        headerText.setFont(headerFont);
        headerText.setAlignment(TextAlign.topCenter);
        headerText.setColor(Color.BLACK);

        statsFont = directory.getEntry("outfit", BitmapFont.class);
        statsText = new TextLayout();
        statsText.setFont(statsFont);
        statsText.setAlignment(TextAlign.topLeft);
        statsText.setColor(Color.BLACK);

        camera = new OrthographicCamera();
        viewport = new FitViewport(logicalWidth, logicalHeight, camera);
        camera.position.set(logicalWidth / 2f, logicalHeight / 2f, 0);
        camera.update();

        int starCount = gameSceneRef.getNumStars();
        if(isWin){
            if(starCount == 3){
                starsTexture = new TextureRegion(directory.getEntry("lc_3stars", Texture.class));
            }else if(starCount == 2){
                starsTexture = new TextureRegion(directory.getEntry("lc_2stars", Texture.class));
            }else if(starCount == 1){
                starsTexture = new TextureRegion(directory.getEntry("lc_1stars", Texture.class));
            }
        }else{
            starsTexture = new TextureRegion(directory.getEntry("lc_0stars", Texture.class));
        }

        int time = (int) gameSceneRef.getTimeElapsed();
        time_mins = time / 60;
        int secs = time % 60;
        time_secs = secs < 10 ? "0" + secs : String.valueOf(secs);
        stuns = gameSceneRef.getNumStunned();
        kills = gameSceneRef.getBugsKilled();
        accuracy = gameSceneRef.getAccuracy();

        active = false;
    }

    public void setScreenListener(ScreenListener listener){
        this.listener = listener;
    }

    public void setSpriteBatch(SpriteBatch batch){
        this.batch = batch;
    }

    @Override
    public void show(){
        active = true;
        Gdx.input.setCursorCatched(false);
    }

    public void hide() {
        active = false;
        Gdx.input.setCursorCatched(true);
    }

    @Override
    public void render(float delta){
        if (!active) return;
        gameSceneRef.draw(delta);
        update(delta);
    }

    @Override
    public void resize(int width, int height){
        viewport.update(width, height, true);
        camera.position.set(logicalWidth/2f, logicalHeight/2f, 0);
        camera.update();
        if(gameSceneRef != null) {
            gameSceneRef.resize(width, height);
        }
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
    }

    private void update(float delta){
        float mx = getLogicalMouseX();
        float my = getLogicalMouseY();
        boolean clicked = Gdx.input.justTouched();

        if(retryRect.contains(mx,my) && clicked){
            listener.exitScreen(this, EXIT_RETRY);
            return;
        }
        if(homeRect.contains(mx,my) && clicked){
            listener.exitScreen(this, EXIT_HOME);
            return;
        }
        if(levelRect.contains(mx,my) && clicked){
            listener.exitScreen(this, EXIT_LEVEL);
            return;
        }
        draw(mx,my);
    }

    private void draw(float mx, float my){
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin(camera);

        batch.setColor(0f,0f,0f,0.55f);
        batch.draw(game_overlay, 0,0,logicalWidth,logicalHeight);
        batch.setColor(Color.WHITE);
        batch.draw(ticketTexture, ticketX, ticketY, ticketWidth, ticketHeight);
        batch.draw(starsTexture, starsX, starsY, starsWidth, starsHeight);

        String header = isWin ? "Order Complete!" : "Order Failed";
        headerText.setText(header);
        headerText.layout();
        batch.drawText(headerText, textCenterX, textY);

        statsText.setText("Time Elapsed");
        statsText.layout();
        batch.drawText(statsText, statsX, statsY);
        statsText.setAlignment(TextAlign.topRight);
        if(!gameSceneRef.isFirstStar() || !gameSceneRef.isSecondStar()){
            statsText.setColor(Color.RED);
        }
        statsText.setText(time_mins + ":" + time_secs);
        statsText.layout();
        batch.drawText(statsText, numbersX, statsY);
        statsText.setAlignment(TextAlign.topLeft);
        statsText.setColor(Color.BLACK);

        statsText.setText("Helpers Stunned");
        statsText.layout();
        batch.drawText(statsText, statsX, statsY - numbersSpace);
        statsText.setAlignment(TextAlign.topRight);
        if(!gameSceneRef.isThirdStar()){
            statsText.setColor(Color.RED);
        }
        statsText.setText(String.valueOf(stuns));
        statsText.layout();
        batch.drawText(statsText, numbersX, statsY - numbersSpace);
        statsText.setAlignment(TextAlign.topLeft);
        statsText.setColor(Color.BLACK);

        statsText.setText("Enemy Bugs Killed");
        statsText.layout();
        batch.drawText(statsText, statsX, statsY - numbersSpace*2);
        statsText.setAlignment(TextAlign.topRight);
        //if(!gameSceneRef.isThirdStar()){
        //    statsText.setColor(Color.RED);
        //}
        statsText.setText(String.valueOf(kills));
        statsText.layout();
        batch.drawText(statsText, numbersX, statsY - numbersSpace*2);
        statsText.setAlignment(TextAlign.topLeft);
        statsText.setColor(Color.BLACK);

        statsText.setText("Slingshot Accuracy");
        statsText.layout();
        batch.drawText(statsText, statsX, statsY - numbersSpace*3);
        statsText.setAlignment(TextAlign.topRight);
        //if(!gameSceneRef.isThirdStar()){
        //    statsText.setColor(Color.RED);
        //}

        statsText.setText(accuracy > 0 ? String.format("%.2f", accuracy*100)+"%" : "--%");
        statsText.layout();
        batch.drawText(statsText, numbersX, statsY - numbersSpace*3);
        statsText.setAlignment(TextAlign.topLeft);
        statsText.setColor(Color.BLACK);


        TextureRegion retryTex = retryRect.contains(mx,my)?retryBtn:retryBtn_trans;
        TextureRegion levelTex = levelRect.contains(mx,my)?levelSelectBtn:levelSelectBtn_trans;
        TextureRegion homeTex = homeRect.contains(mx,my)?homeBtn:homeBtn_trans;

        batch.draw(retryTex, retryX, retryY, retryWidth, retryHeight);
        batch.draw(homeTex, homeX, homeY, homeWidth, homeHeight);
        batch.draw(levelTex, levelX, levelY, levelWidth, levelHeight);

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
}

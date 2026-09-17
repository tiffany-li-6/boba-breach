package game.bobabreach;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextAlign;
import edu.cornell.gdiac.graphics.TextLayout;
import edu.cornell.gdiac.util.ScreenListener;

public class LevelSelectScene implements Screen {

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ScreenListener listener;
    private AssetDirectory directory;
    private boolean active;
    private final SaveData saveData;

    private final com.badlogic.gdx.math.Vector3 mouseCoords = new com.badlogic.gdx.math.Vector3();

    private float logicalWidth;
    private float logicalHeight;
    private float leftButtonX;
    private float rightButtonX;
    private float buttonY;
    private float buttonSize;
    private float playButtonX;
    private float playButtonY;
    private float playButtonWidth;
    private float playButtonHeight;
    private float homeButtonX;
    private float homeButtonY;
    private float homeButtonSize;
    private float levelTextX;
    private float levelTextY;
    private float starsX;
    private float starsY;
    private float starsW;
    private float starsH;
    private float statsLeftX;
    private float statsRightX;
    private float statsStartingY;
    private float statsOffset;
    private float previewX;
    private float previewY;
    private float previewW;
    private float previewH;

    private Texture background;

    private int[] levelStars = new int[6];

    private TextureRegion playBtn;
    private TextureRegion playBtn_trans;
    private TextureRegion homeBtn;
    private TextureRegion homeBtn_trans;
    private TextureRegion rightBtn;
    private TextureRegion leftBtn;
    private TextureRegion rightBtn_gray;
    private TextureRegion leftBtn_hover;
    private TextureRegion rightBtn_hover;
    private TextureRegion zerostars;
    private TextureRegion onestars;
    private TextureRegion twostars;
    private TextureRegion threestars;

    private TextureRegion[] levelPreviews = new TextureRegion[SaveData.NUM_LEVELS + 1];

    private Rectangle playBtnRect;
    private Rectangle homeBtnRect;
    private Rectangle rightBtnRect;
    private Rectangle leftBtnRect;

    private TextLayout statLayout;
    private TextLayout levelText;

    private int selectedLevel;

    public static final int LEVEL_SELECT_EXIT = 0;
    public static final int LEVEL_SELECT_PLAY = 1;

    public LevelSelectScene(AssetDirectory directory, SpriteBatch batch, SaveData saveData, int level){
        this.directory = directory;
        this.batch = batch;
        this.saveData = saveData;
        this.selectedLevel = level;

        JsonValue constants = directory.getEntry("constants", JsonValue.class);
        JsonValue levelSelectConstants = constants.get("level select");

        logicalWidth = levelSelectConstants.getFloat("logical width");
        logicalHeight = levelSelectConstants.getFloat("logical height");
        leftButtonX = levelSelectConstants.getFloat("left button x");
        buttonY = levelSelectConstants.getFloat("button y");
        rightButtonX = levelSelectConstants.getFloat("right button x");
        buttonSize = levelSelectConstants.getFloat("button size");
        playButtonX = levelSelectConstants.getFloat("play button x");
        playButtonY = levelSelectConstants.getFloat("play button y");
        playButtonWidth = levelSelectConstants.getFloat("play button width");
        playButtonHeight = levelSelectConstants.getFloat("play button height");
        homeButtonX = levelSelectConstants.getFloat("home button x");
        homeButtonY = levelSelectConstants.getFloat("home button y");
        homeButtonSize = levelSelectConstants.getFloat("home button size");
        levelTextX = levelSelectConstants.getFloat("level text x");
        levelTextY = levelSelectConstants.getFloat("level text y");
        starsX = levelSelectConstants.getFloat("stars x");
        starsY = levelSelectConstants.getFloat("stars y");
        starsW = levelSelectConstants.getFloat("stars width");
        starsH = levelSelectConstants.getFloat("stars height");
        statsLeftX = levelSelectConstants.getFloat("stats left x");
        statsRightX = levelSelectConstants.getFloat("stats right x");
        statsStartingY = levelSelectConstants.getFloat("stats starting y");
        statsOffset = levelSelectConstants.getFloat("stats offset");
        previewX = levelSelectConstants.getFloat("preview x");
        previewY = levelSelectConstants.getFloat("preview y");
        previewW = levelSelectConstants.getFloat("preview w");
        previewH = levelSelectConstants.getFloat("preview h");

        background = directory.getEntry("levelselect_background", Texture.class);

        playBtn = new TextureRegion(directory.getEntry("btn_play", Texture.class));
        playBtn_trans = new TextureRegion(directory.getEntry("btn_play_trans", Texture.class));
        homeBtn = new TextureRegion(directory.getEntry("btn_home", Texture.class));
        homeBtn_trans = new TextureRegion(directory.getEntry("btn_home_trans", Texture.class));

        rightBtn = new TextureRegion(directory.getEntry("ls_right", Texture.class));
        rightBtn_gray = new TextureRegion(directory.getEntry("ls_right_gray", Texture.class));
        rightBtn_hover = new TextureRegion(directory.getEntry("ls_right_hover", Texture.class));
        leftBtn = new TextureRegion(directory.getEntry("ls_left", Texture.class));
        leftBtn_hover = new TextureRegion(directory.getEntry("ls_left_hover", Texture.class));
        zerostars = new TextureRegion(directory.getEntry("lc_0stars", Texture.class));
        onestars = new TextureRegion(directory.getEntry("lc_1stars", Texture.class));
        twostars = new TextureRegion(directory.getEntry("lc_2stars", Texture.class));
        threestars = new TextureRegion(directory.getEntry("lc_3stars", Texture.class));

        for(int i=1; i<=SaveData.NUM_LEVELS; i++){
            levelPreviews[i] = new TextureRegion(directory.getEntry("level"+i, Texture.class));
        }

        float baseY = levelSelectConstants.getFloat("button base y");

        homeBtnRect = new Rectangle(homeButtonX, homeButtonY, homeButtonSize, homeButtonSize);
        playBtnRect = new Rectangle(playButtonX, playButtonY, playButtonWidth, playButtonHeight);
        leftBtnRect = new Rectangle(leftButtonX, buttonY, buttonSize, buttonSize);
        rightBtnRect = new Rectangle(rightButtonX, buttonY, buttonSize, buttonSize);

        statLayout = new TextLayout();
        statLayout.setFont(directory.getEntry("stats font", BitmapFont.class));
        statLayout.setAlignment(TextAlign.topLeft);
        statLayout.setColor(Color.BLACK);

        levelText = new TextLayout();
        levelText.setFont(directory.getEntry("level font", BitmapFont.class));
        levelText.setAlignment(TextAlign.topCenter);
        levelText.setColor(Color.BLACK);

        camera = new OrthographicCamera();
        viewport = new FitViewport(logicalWidth, logicalHeight, camera);
        camera.position.set(logicalWidth/2f, logicalHeight/2f, 0);
        camera.update();

        active = false;
    }
    public void setScreenListener(ScreenListener listener){
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
        if(active){
            update(delta);
            draw();
        }
    }

    @Override
    public void resize(int width, int height){
        viewport.update(width, height, true);
        camera.position.set(logicalWidth/2f, logicalHeight/2f, 0);
        camera.update();
    }

    public void update(float delta){
        if(!active){
            return;
        }

        float mx = getLogicalMouseX();
        float my = getLogicalMouseY();
        boolean clickedThisFrame = Gdx.input.justTouched();

        if(!clickedThisFrame){
            return;
        }

        if(leftBtnRect.contains(mx,my) && selectedLevel>1){
            selectedLevel--;
            return;
        }

        if(rightBtnRect.contains(mx,my) && canGoRight()){
            selectedLevel++;
            return;
        }

        if(homeBtnRect.contains(mx,my) && clickedThisFrame){
            listener.exitScreen(this, LEVEL_SELECT_EXIT);
            return;
        }

        if(playBtnRect.contains(mx,my) && clickedThisFrame){
            listener.exitScreen(this, LEVEL_SELECT_PLAY);
            return;
        }

    }

    private void draw(){
        ScreenUtils.clear(0f,0f,0f,1f);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin(camera);

        float mx = getLogicalMouseX();
        float my = getLogicalMouseY();

        batch.draw(background, 0,0,logicalWidth, logicalHeight);

        levelText.setText("Level " + selectedLevel);
        levelText.layout();
        batch.drawText(levelText, levelTextX, levelTextY);

        batch.draw(levelPreviews[selectedLevel], previewX, previewY, previewW, previewH);

        if(playBtnRect.contains(mx, my)){
            batch.draw(playBtn_trans, playBtnRect.x, playBtnRect.y, playBtnRect.width, playBtnRect.height);
        }else{
            batch.draw(playBtn, playBtnRect.x, playBtnRect.y, playBtnRect.width, playBtnRect.height);
        }

        if(homeBtnRect.contains(mx, my)){
            batch.draw(homeBtn, homeBtnRect.x, homeBtnRect.y, homeBtnRect.width, homeBtnRect.height);
        }else{
            batch.draw(homeBtn_trans, homeBtnRect.x, homeBtnRect.y, homeBtnRect.width, homeBtnRect.height);
        }

        drawNavigation(mx,my);

        int levelStars = saveData.getStars(selectedLevel);
        boolean done = saveData.isCompleted(selectedLevel);
        float bestTime = saveData.getBestTime(selectedLevel);
        int bestStunned = saveData.getBestStunned(selectedLevel);
        int mostKills = saveData.getBestKills(selectedLevel);
        float accuracy = saveData.getBestAccuracy(selectedLevel);

        if(levelStars == 0){
            batch.draw(zerostars, starsX, starsY, starsW, starsH);
        } else if(levelStars == 1){
            batch.draw(onestars, starsX, starsY, starsW, starsH);
        } else if(levelStars == 2){
            batch.draw(twostars, starsX, starsY, starsW, starsH);
        } else if(levelStars == 3){
            batch.draw(threestars, starsX, starsY, starsW, starsH);
        }

        float statsY = statsStartingY;
        drawStatRow("Best Time", done ? formatTime(bestTime) : "--:--", statsY);
        statsY -= statsOffset;
        drawStatRow("Fewest Bugs Stunned", bestStunned >= 0 ? String.valueOf(bestStunned) : "--", statsY);
        statsY -= statsOffset;
        drawStatRow("Most Enemies Killed", mostKills >= 0 ? String.valueOf(mostKills) : "--", statsY);
        statsY -= statsOffset;
        drawStatRow("Best Accuracy", accuracy > 0 ? String.format("%.2f", accuracy*100)+"%" : "--", statsY);

        batch.end();
    }

    private void drawStatRow(String label, String value, float topY){
        statLayout.setAlignment(TextAlign.topLeft);
        statLayout.setText(label);
        statLayout.layout();
        batch.drawText(statLayout, statsLeftX, topY);

        statLayout.setAlignment(TextAlign.topRight);
        statLayout.setText(value);
        statLayout.layout();
        batch.drawText(statLayout, statsRightX, topY);
    }

    private void drawNavigation(float mx, float my){
        boolean canLeft = selectedLevel > 1;
        if(canLeft){
            if(leftBtnRect.contains(mx,my)){
                batch.draw(leftBtn_hover, leftButtonX, buttonY, buttonSize, buttonSize);
            }else{
                batch.draw(leftBtn, leftButtonX, buttonY, buttonSize, buttonSize);
            }
        }
        boolean canRight = canGoRight();
        if(canRight){
            if(rightBtnRect.contains(mx,my)){
                batch.draw(rightBtn_hover, rightButtonX, buttonY, buttonSize, buttonSize);
            }else{
                batch.draw(rightBtn, rightButtonX, buttonY, buttonSize, buttonSize);
            }
        }else{
            batch.draw(rightBtn_gray, rightButtonX, buttonY, buttonSize, buttonSize);
        }
    }

    private boolean canGoRight(){
        return selectedLevel < SaveData.NUM_LEVELS && (selectedLevel + 1 <= saveData.getUnlockedUpTo());
    }

    private static String formatTime(float seconds){
        int total = (int) seconds;
        int mins = total / 60;
        int secs = total % 60;
        return mins + ":" + String.format("%02d", secs);
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
    public void resume() {}

    @Override
    public void dispose() {
    }

    public int getSelectedLevel(){
        return selectedLevel;
    }
}


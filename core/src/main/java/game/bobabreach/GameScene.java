package game.bobabreach;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Json;

import java.util.Comparator;
import java.util.Iterator;
import java.util.EnumMap;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.*;
import com.badlogic.gdx.audio.Sound;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.graphics.*;
import edu.cornell.gdiac.physics2.*;
import game.bobabreach.AIControllers.*;
import game.bobabreach.AIControllers.HelperAIController;
import game.bobabreach.GameObjects.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class GameScene implements Screen {
    public static final int EXIT_QUIT = 0;
    public static final int EXIT_NEXT = 1;
    public static final int EXIT_PREV = 2;
    public static final int EXIT_WIN = 3;
    public static final int EXIT_LOSS = 4;
    public static final int EXIT_COUNT = 180;

    private Level level;
    private Tutorial tutorial;
    private int levelSelected;
    private BitmapFont displayFont;
    private TextLayout crateCounter;
    private TextLayout digitalClock;
    private TextLayout recipeTracking;
    private TextLayout controlling;
    private float ppm;
    private float units;
    private TextureRegion crosshairTexture;
    private int whichHelperControlled;
    private TextureRegion helperCrosshairTexture;
    private TextureRegion helper1Indicator;
    private TextureRegion helper2Indicator;
    private Texture helperSheet;
    private TextureRegion[][] helper1Cross;
    private TextureRegion[][] helper1CrossCrate;
    private TextureRegion[][] helper1Ind;
    private TextureRegion[][] helper1IndCrate;
    private TextureRegion[][] helper2Cross;
    private TextureRegion[][] helper2CrossCrate;
    private TextureRegion[][] helper2Ind;
    private TextureRegion[][] helper2IndCrate;
    private float indicatorOffset;
    private int animYIndex = 0;
    private int animXIndex = 0;
    private float timeSinceLastAnim = 0;
    private TextureRegion gridCell;
    private TextureRegion indicatorDot;
    private Sound popSound;
    private Sound deathSound;
    private Sound stunSound;
    private float timeElapsed;
    private float inputIgnoreTimer = 0f;
    private static final float INPUT_IGNORE_DURATION = 0.2f;
    private int spawnIndex;
    private float[] spawnTimes;
    private TextureRegion milkIcon;
    private TextureRegion jellyIcon;
    private TextureRegion bobaIcon;
    private Array<Vector2> trajectoryPoints = new Array<>();
    private ShapeRenderer trajectoryRenderer;
    protected AssetDirectory directory;
    protected SaveData saveData;
    protected OrthographicCamera camera;
    protected SpriteBatch batch;
    protected float width;
    protected float height;
    protected JsonValue constants;
    protected JsonValue levelData;
    protected JsonValue uiData;
    public static final int WORLD_VELOC = 6;
    public static final int WORLD_POSIT = 2;
    private ScreenListener listener;
    protected World world;
    protected World simWorld;
    protected boolean paused;
    protected Rectangle bounds;
    protected Rectangle crosshairBounds;
    protected Vector2 scale;
    protected boolean active;
    protected boolean complete;
    protected boolean failed;
    protected boolean debug;
    protected int countdown;
    private ObstacleSelector selector;
    private ObstacleSelector helperSelector;
    private Player avatar;
    private Slingshot slingshot;
    private Cup cup;
    private Array<Helper> helpers;
    private int activeHelperIndex = 0;
    private CollisionController collisionController;
    private EnemyAIController enemyController;
    private HelperAIController helperController;
    private HelperAIControllerSteering helperAIControllerSteering;
    private HelperAIControllerPathFollow helperControllerAlt;
    private int spawnLimit;
    private int initialSpawnTime;
    private float nextBlockerSpawn;
    private float blockerSpawnInterval;
    private int randomFactor;
    private int spawnCooldown;
    private int nextSpawn;
    private boolean initialSpawns = true;
    private Vector2 slingshotPos;
    private Vector2 bobaPos;
    private Vector2 simBobaPos;
    private Array<Vector2> simTrajectoryPoints = new Array<>();
    private Vector2 cursorPos;
    private Vector2 shootDirection;
    private Viewport viewport;
    private Texture background;
    private Texture wallBackground;
    private TextureRegion timer_1star;
    private TextureRegion timer_2stars;
    private TextureRegion timer_3stars;
    private int timerX;
    private int timerY;
    private int timerH;
    private int timerW;
    private Texture column_background;
    private final Array<ObstacleSprite> drawingArray = new Array<>();
    private final Comparator<ObstacleSprite> yComparator = new Comparator<>() {
        @Override
        public int compare(ObstacleSprite a, ObstacleSprite b) {
            return Float.compare(b.getObstacle().getY(), a.getObstacle().getY());
        }
    };
    private int numStunned;
    private int bugsKilled;
    private int bobaShot;
    private int bobaHit;
    private boolean quickControls = true;

    //for tutorial in level1
    private int bugsKilledSoFar = 0;
    private int stunnedSoFar = 0;
    private int bobaInCupSoFar = 0;

    public boolean isDebug() { return debug; }
    public void setDebug(boolean value) { debug = true; }
    public boolean isComplete() { return complete; }
    public void setComplete(boolean value) { if (value) { countdown = EXIT_COUNT; } complete = value; }
    public boolean isFailure() { return failed; }
    public void setFailure(boolean value) { if (value) { countdown = EXIT_COUNT; } failed = value; }
    public boolean isActive() { return active; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean p) { paused = p; if(!p){ inputIgnoreTimer = INPUT_IGNORE_DURATION; } }
    public SpriteBatch getSpriteBatch() { return batch; }
    public void setSpriteBatch(SpriteBatch batch) { this.batch = batch; }
    public int getNumStunned() { return numStunned; }
    public int getBugsKilled() { return bugsKilled; }
    public float getAccuracy() { if(bobaShot > 0){ return (float) bobaHit / (float) bobaShot; } else return -1f; }
    public float getTimeElapsed() { return timeElapsed; }
    public boolean isFirstStar() { return timeElapsed <= level.getTimeLimit2(); }
    public boolean isSecondStar() { return timeElapsed <= level.getTimeLimit1(); }
    public boolean isThirdStar() { return true; }
    public int getNumStars() { int s1=isFirstStar()?1:0; int s2=isSecondStar()?1:0; int s3=isThirdStar()?1:0; return s1+s2+s3; }

    protected GameScene(AssetDirectory directory, int levelSelected, SaveData saveData) {
        crosshairTexture = new TextureRegion(directory.getEntry("crosshair", Texture.class));
        // Helper 1 now uses yellowarrow textures (always bright; no dark/gray-out variants).
        helperSheet = new Texture(Gdx.files.internal("yellowarrow-circle.png"));
        helper1Cross = TextureRegion.split(helperSheet, helperSheet.getWidth() / 4, helperSheet.getHeight() / 4);
        helperSheet = new Texture(Gdx.files.internal("yellowarrow.png"));
        helper1CrossCrate = TextureRegion.split(helperSheet, helperSheet.getWidth() / 4, helperSheet.getHeight() / 4);
        helperSheet = new Texture(Gdx.files.internal("yellowarrow-circle.png"));
        helper1Ind = TextureRegion.split(helperSheet, helperSheet.getWidth() / 4, helperSheet.getHeight() / 4);
        helperSheet = new Texture(Gdx.files.internal("yellowarrow.png"));
        helper1IndCrate = TextureRegion.split(helperSheet, helperSheet.getWidth() / 4, helperSheet.getHeight() / 4);
        // Helper 2 now uses bluearrow textures (always bright; no dark/gray-out variants).
        helperSheet = new Texture(Gdx.files.internal("bluearrow-circle.png"));
        helper2Cross = TextureRegion.split(helperSheet, helperSheet.getWidth() / 4, helperSheet.getHeight() / 4);
        helperSheet = new Texture(Gdx.files.internal("bluearrow.png"));
        helper2CrossCrate = TextureRegion.split(helperSheet, helperSheet.getWidth() / 4, helperSheet.getHeight() / 4);
        helperSheet = new Texture(Gdx.files.internal("bluearrow-circle.png"));
        helper2Ind = TextureRegion.split(helperSheet, helperSheet.getWidth() / 4, helperSheet.getHeight() / 4);
        helperSheet = new Texture(Gdx.files.internal("bluearrow.png"));
        helper2IndCrate = TextureRegion.split(helperSheet, helperSheet.getWidth() / 4, helperSheet.getHeight() / 4);
        gridCell = new TextureRegion(directory.getEntry("grid cell", Texture.class));
        indicatorDot = new TextureRegion(directory.getEntry("indicator", Texture.class));
        this.directory = directory;
        this.saveData = saveData;
        constants = directory.getEntry("constants", JsonValue.class);
        JsonValue defaults = constants.get("world");
        ppm = defaults.getFloat("ppm");
        this.levelSelected = levelSelected;
        String levelName = "level" + levelSelected;
        levelData = directory.getEntry(levelName, JsonValue.class);
        uiData = constants.get("gameplay ui");
        whichHelperControlled = 0;
        milkIcon = new TextureRegion(directory.getEntry("milk", Texture.class));
        jellyIcon = new TextureRegion(directory.getEntry("jelly", Texture.class));
        bobaIcon = new TextureRegion(directory.getEntry("boba", Texture.class));
        popSound = directory.getEntry("pop", Sound.class);
        deathSound = directory.getEntry("death", Sound.class);
        stunSound = directory.getEntry("stun", Sound.class);
        scale = new Vector2();
        slingshotPos = new Vector2();
        bobaPos = new Vector2();
        simBobaPos = new Vector2();
        simTrajectoryPoints = new Array<>();
        for (int i = 0; i < constants.get("boba").getInt("sim points"); i++) {
            simTrajectoryPoints.add(new Vector2());
        }
        cursorPos = new Vector2();
        shootDirection = new Vector2();
        bounds = new Rectangle(0, 0, defaults.get("bounds").getFloat(0), defaults.get("bounds").getFloat(1));
        crosshairBounds = new Rectangle(defaults.getFloat("crosshair x bound"), 0, 0, defaults.get("bounds").getFloat(1));
        camera = new OrthographicCamera(bounds.width, bounds.height);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        complete = false; failed = false; debug = false; active = false; paused = false;
        countdown = -1; timeElapsed = 0; spawnIndex = 0;
        spawnTimes = constants.get("vents").get("spawn_times").asFloatArray();
        spawnCooldown = 0;
        spawnLimit = constants.get("vents").getInt("spawn_limit");
        initialSpawnTime = constants.get("vents").getInt("initial_spawn");
        randomFactor = constants.get("vents").getInt("random_factor");
        initialSpawns = false;
        JsonValue blockerData = constants.get("blocker");
        blockerSpawnInterval = blockerData.getFloat("spawn_interval");
        nextBlockerSpawn = blockerSpawnInterval;
        background = directory.getEntry("background", Texture.class);
        wallBackground = directory.getEntry("wall", Texture.class);
        column_background = directory.getEntry("column_background", Texture.class);
        timer_1star = new TextureRegion(directory.getEntry("timer_1star", Texture.class));
        timer_2stars = new TextureRegion(directory.getEntry("timer_2stars", Texture.class));
        timer_3stars = new TextureRegion(directory.getEntry("timer_3stars", Texture.class));
        timerX = uiData.getInt("timer x");
        timerY = uiData.getInt("timer y");
        timerW = uiData.getInt("timer w");
        timerH = uiData.getInt("timer h");
    }

    public void dispose() {
        level.dispose();
        if (trajectoryRenderer != null) { trajectoryRenderer.dispose(); }
        if (helperSheet != null) { helperSheet.dispose(); }
        bounds = null; scale = null; world = null; batch = null;
    }

    public boolean inBounds(ObstacleSprite sprite) {
        Obstacle obj = sprite.getObstacle();
        boolean horiz = (bounds.x <= obj.getX() && obj.getX() <= bounds.x + bounds.width);
        boolean vert = (bounds.y <= obj.getY() && obj.getY() <= bounds.y + bounds.height);
        return horiz && vert;
    }

    public void reset() {
        float units = height / bounds.height;
        JsonValue values = constants.get("world");
        Vector2 gravity = new Vector2(0, values.getFloat("gravity"));
        timeElapsed = 0; spawnIndex = 0;
        if (collisionController != null) {
            collisionController.resetNumStunned();
            collisionController.resetBugsKilled();
            collisionController.resetBobaHits();
            bobaShot = 0;
        }
        spawnCooldown = 0; initialSpawns = true;
        nextBlockerSpawn = blockerSpawnInterval;
        if (world == null) {
            world = new World(Vector2.Zero, false);
            simWorld = new World(Vector2.Zero, false);
            collisionController = new CollisionController(world);
            world.setContactListener(collisionController);
            level = new Level(constants, levelData, directory, world, simWorld);
        }
        if (trajectoryRenderer == null) { trajectoryRenderer = new ShapeRenderer(); }
        level.resetLevel(units, bounds.width, bounds.height);
        setComplete(false); setFailure(false);
        displayFont = directory.getEntry("joyful", BitmapFont.class);
        displayFont.setColor(Color.BLACK);
        crateCounter = new TextLayout();
        crateCounter.setFont(displayFont);
        crateCounter.setAlignment(TextAlign.topCenter);
        digitalClock = new TextLayout();
        digitalClock.setFont(directory.getEntry("digital clock", BitmapFont.class));
        digitalClock.setColor(Color.WHITE);
        digitalClock.setAlignment(TextAlign.topCenter);
        recipeTracking = new TextLayout();
        recipeTracking.setFont(directory.getEntry("joyfulRecipe", BitmapFont.class));
        recipeTracking.setAlignment(TextAlign.topLeft);
        recipeTracking.setColor(Color.BLACK);
        controlling = new TextLayout();
        whichHelperControlled = 0;
        avatar = level.getPlayer();
        slingshot = level.getSlingshot();
        cup = level.getCup();
        helpers = new Array<>();
        for (ObstacleSprite h : level.getHelpers()) { helpers.add((Helper) h); }
        collisionController.updatePlayer(avatar);
        collisionController.updateSlingshot(slingshot);
        collisionController.updateCrates(level.getCrates());
        collisionController.updateLevel(level);
        collisionController.updateCup(cup);
        collisionController.updatePortals(level.getPortals());
        collisionController.updateCobwebs(level.getCobwebs());
        collisionController.updateHoneys(level.getHoneys());
        selector = new ObstacleSelector(world);
        helperSelector = new ObstacleSelector(world);
        selector.setTexture(directory.getEntry("crosshair", Texture.class));
        helperSelector.setTexture(directory.getEntry("helper crosshair", Texture.class));
        selector.setPhysicsUnits(units);
        helperSelector.setPhysicsUnits(units);
        enemyController = new EnemyAIController(level.getCrates(), level.getGraphs(), cup, level.getVents(), level.getHelpers(), world);
        enemyController.setLevel(level);
        helperController = new HelperAIController(level.getCrates(), level.getGraphs(), cup, helperSelector, world, level);
        helperAIControllerSteering = new HelperAIControllerSteering(level.getCrates(), level.getGraphs(), cup, helperSelector, world);
        helperControllerAlt = new HelperAIControllerPathFollow(level.getCrates(), level.getGraphs(), cup, helperSelector, world);

        tutorial = new Tutorial(directory, levelSelected);
    }

    public boolean preUpdate(float dt) {
        InputController input = InputController.getInstance(viewport);
        input.sync(bounds, scale, camera);
        if (listener == null) { return true; }
        if (input.didDebug()) { debug = !debug; }
        if (input.didPause()) { paused = !paused; if (paused) { listener.exitScreen(this, 99); } }
        if (input.didReset()) { reset(); }
        if (input.didExit()) { pause(); listener.exitScreen(this, EXIT_QUIT); return false; }
        return true;
    }

    private float crosshairX;
    private float crosshairY;
    private boolean ind1Crate = false;
    private boolean ind2Crate = false;

    public void update(float dt) {
        if (paused) { return; }
        if (inputIgnoreTimer > 0f){ inputIgnoreTimer -= dt; return; }
        if (tutorial != null && tutorial.isActive() && levelSelected > 2){
            tutorial.update();
            return;
        }else if (tutorial != null && tutorial.isActive()){
            if(levelSelected == 1) {
                if (level.getCupCount(IngredientType.BOBA) > bobaInCupSoFar) {
                    tutorial.notifyBobaInCup();
                    bobaInCupSoFar = level.getCupCount(IngredientType.BOBA);
                }
                //avoids soft lockout if they finish boba immediately
                if (level.getCupCount(IngredientType.BOBA) == level.getRequired(IngredientType.BOBA)){
                    tutorial.notifyBobaInCup();
                }
                tutorial.update();
            }
            else if(levelSelected == 2){
                if (helpers.get(0).hasIngredient()){
                    tutorial.notifyPickup();
                }
                if(level.getCupCount(IngredientType.MILK)>0 || level.getCupCount(IngredientType.JELLY)>0){
                    tutorial.notifyDropoff();
                }
                tutorial.update();
            }
        }

        if (getBugsKilled() > bugsKilledSoFar) {
            if(levelSelected == 1 && tutorial != null && tutorial.isActive()){
                tutorial.notifyBugShot();
            }
            deathSound.play(saveData.getSfxGain());
            bugsKilledSoFar = getBugsKilled();
        }

        if (getNumStunned() > stunnedSoFar) {
            //this is NOT normalized i CANT MAKE IT QUIETER IT WONT LET ME
            stunSound.play(saveData.getSfxGain());
            stunnedSoFar = getNumStunned();
        }

        InputController input = InputController.getInstance(viewport);
        Gdx.input.setCursorCatched(true);

        int modeToSwitch = -1;
        if (!quickControls) {
            if (input.didOne()) { modeToSwitch = 0; }
            else if (input.didTwo()) { if (helpers.size > 0) { modeToSwitch = 1; } }
            else if (input.didThree()) { if (helpers.size > 1) { modeToSwitch = 2; } }
        } else {
            modeToSwitch = activeHelperIndex + 1;
            if (input.didPrimary()) {
                if (activeHelperIndex == 0 && helpers.size > 1) { modeToSwitch = 2; }
                else { modeToSwitch = 1; }
            }
        }

        slingshot.update(dt);

        if (!quickControls) {
            float scrollAmount = input.getScrollInput();
            if (Math.abs(scrollAmount) > 0.1f) {
                int currentMode = avatar.isControlled() ? 0 : (activeHelperIndex + 1);
                int totalModes = 1 + helpers.size;
                if (scrollAmount > 0) { executeModeSwitch((currentMode + 1) % totalModes); }
                else if (scrollAmount < 0) { executeModeSwitch((currentMode - 1 + totalModes) % totalModes); }
                input.clearScrollInput();
            }
        }

        if (!quickControls) {
            if (modeToSwitch == 0) {
                if (!avatar.isControlled()) {
                    avatar.setControlled(true); avatar.setShootingMode(true);
                    avatar.setMovement(0, 0); avatar.lockVelocity(); slingshot.lockVelocity();
                    for (int i = 0; i < helpers.size; i++) { helpers.get(i).setControlled(false); helpers.get(i).lockVelocity(); }
                }
            } else if (modeToSwitch > 0 && modeToSwitch <= helpers.size) {
                int helperIndex = modeToSwitch - 1;
                if (helpers.size > helperIndex) {
                    avatar.setControlled(false); avatar.setShootingMode(false);
                    avatar.setMovement(0, 0); avatar.lockVelocity(); slingshot.lockVelocity();
                    for (int i = 0; i < helpers.size; i++) { helpers.get(i).setControlled(i == helperIndex); if (i != helperIndex) helpers.get(i).lockVelocity(); }
                    activeHelperIndex = helperIndex;
                }
            }
        } else {
            if (modeToSwitch > 0 && modeToSwitch <= helpers.size) {
                int helperIndex = modeToSwitch - 1;
                if (helpers.size > helperIndex) {
                    for (int i = 0; i < helpers.size; i++) { helpers.get(i).setControlled(i == helperIndex); }
                    activeHelperIndex = helperIndex;
                }
            }
        }

        crosshairX = input.getCrossHair().x;
        crosshairY = input.getCrossHair().y;
        selector.moveTo(crosshairX, crosshairY);
        helperSelector.moveTo(crosshairX, crosshairY);

        boolean isShootableAngle = false;
        if (true) {
            slingshotPos.x = slingshot.getXPos();
            slingshotPos.y = slingshot.getYPos();
            cursorPos.x = crosshairX * selector.getPhysicsUnits();
            cursorPos.y = crosshairY * selector.getPhysicsUnits();
            if (cursorPos.x >= slingshotPos.x) {
                shootDirection = cursorPos.sub(slingshotPos);
                shootDirection.nor();
                slingshot.setRotation(shootDirection.angleDeg());
                isShootableAngle = true;
            }
            slingshot.setMovement(input.getVertical() * slingshot.getForce());
            slingshot.applyForce();

            float bobaSpeed = 50f;
            if (constants != null && constants.get("boba") != null) {
                bobaSpeed = constants.get("boba").getFloat("speed", 50f);
            }
            float unitsPerPixel = bounds.height / height;
            Vector2 physicsSlingshotPos = new Vector2(slingshotPos.x * unitsPerPixel, slingshotPos.y * unitsPerPixel);
            JsonValue bobaConfig = constants.get("boba");
            float bobaOffset = bobaConfig.getFloat("offset", 0);
            Vector2 offsetDir = new Vector2(shootDirection).nor();
            physicsSlingshotPos.add(offsetDir.x * bobaOffset, offsetDir.y * bobaOffset);
            simBobaPos.set(slingshotPos.x / units, slingshotPos.y / units);
            level.createBobaSim(simBobaPos, shootDirection);
            shootDirection.nor();
        } else {
            trajectoryPoints.clear();
        }

        if (isShootableAngle) {
            avatar.setShooting(input.didTertiary());
            if (avatar.isControlled() && avatar.isShooting()) {
                bobaPos.set(slingshotPos.x / units, slingshotPos.y / units);
                level.createBoba(bobaPos, shootDirection);
                bobaShot++;
                popSound.play(saveData.getSfxGain());
                trajectoryPoints.clear();
                slingshot.startShooting(shootDirection.angleDeg(), 16);
            }
        } else {
            avatar.setShooting(false);
        }

        if (avatar.isControlled() && !avatar.getShootingMode() && input.didSecondary()) {
            if (avatar.canPickupIngredient
                ()) {
                Crate crate = avatar.getCrateTouching();
                if (crate != null && level.getAvailable(crate.getIngredient()) > 0) {
                    avatar.giveIngredient(crate.getIngredient());
                    level.takeFromCrate(crate.getIngredient());
                }
            }
            if (avatar.hasIngredient() && avatar.getTouchingCup()) {
                level.addToCup(avatar.getHeldIngredient());
                avatar.removeIngredient();
            }
        }
        for (Helper h : helpers) {
            if (h.hasIngredient() && h.getTouchingCup()) {
                level.addToCup(h.getHeldIngredient());
                h.removeIngredient();
            }
        }

        if (avatar.isControlled() && !avatar.getShootingMode()) {
            avatar.setMovement(input.getHorizontal(), input.getVertical());
            avatar.applyForce();
        }

        int helperIndex = 0;
        timeSinceLastAnim += dt;
        if (timeSinceLastAnim > 0.2f) { timeSinceLastAnim = 0; }
        if (timeSinceLastAnim >= 0.035f) {
            timeSinceLastAnim -= 0.035f;
            animYIndex = (animXIndex == 3) ? animYIndex + 1 : animYIndex;
            animYIndex = (animYIndex > 3) ? 0 : animYIndex;
            animXIndex = (animXIndex == 3) ? 0 : animXIndex + 1;
        }

        if (quickControls) {
            //System.out.println(helpers.get(0).getCollidingCrateIdx());

            // Q commands helper 1.
            if (helpers.size >= 1 && !helpers.get(0).isStunned() && input.didQ()) {
                activeHelperIndex = 0;
                for (int i = 0; i < helpers.size; i++) { helpers.get(i).setControlled(i == 0); }
                Helper h = helpers.get(0);
                float x = crosshairX; float y = crosshairY;
                int crateIdx = collisionController.getSeletedCrate(x, y);
                boolean isSelectingCup = collisionController.isSelectingCup(x, y);
                if (crateIdx != -1 || isSelectingCup) { helperCrosshairTexture = helper1CrossCrate[animYIndex][animXIndex]; }
                if (crateIdx == -2) {
                    // do nothing
                } else if (crateIdx != -1) {
                    if (h.getCollidingCrateIdx() == crateIdx) {
                        Crate crate = (Crate) level.getCrates().get(crateIdx);
                        if (h.hasIngredient() && h.getHeldIngredient() == crate.getIngredient()) { h.removeIngredient(); level.returnToCrate(crate.getIngredient()); }
                        else if (!h.hasIngredient() && level.getAvailable(crate.getIngredient()) > 0) { h.giveIngredient(crate.getIngredient()); level.takeFromCrate(crate.getIngredient()); }
                    } else {
                        h.setTarget(crateIdx + 1); h.setState(HelperSteerable.HelperFSMState.PICKUP); ind1Crate = true;
                    }
                } else if (isSelectingCup) {
                    h.setState(HelperSteerable.HelperFSMState.DROPOFF); ind1Crate = true;
                } else {
                    h.setState(HelperSteerable.HelperFSMState.MOVE); h.setMove(x, y); ind1Crate = false;
                }
            }

            // E commands helper 2.
            if (helpers.size >= 2 && (!helpers.get(1).isStunned()) && input.didE()) {
                activeHelperIndex = 1;
                for (int i = 0; i < helpers.size; i++) { helpers.get(i).setControlled(i == 1); }
                Helper h = helpers.get(1);
                float x = crosshairX; float y = crosshairY;
                int crateIdx = collisionController.getSeletedCrate(x, y);
                boolean isSelectingCup = collisionController.isSelectingCup(x, y);
                if (crateIdx != -1 || isSelectingCup) { helperCrosshairTexture = helper2CrossCrate[animYIndex][animXIndex]; }
                if (crateIdx == -2) {
                    // do nothing
                } else if (crateIdx != -1) {
                    if (h.getCollidingCrateIdx() == crateIdx) {
                        Crate crate = (Crate) level.getCrates().get(crateIdx);
                        if (h.hasIngredient() && h.getHeldIngredient() == crate.getIngredient()) { h.removeIngredient(); level.returnToCrate(crate.getIngredient()); }
                        else if (!h.hasIngredient() && level.getAvailable(crate.getIngredient()) > 0) { h.giveIngredient(crate.getIngredient()); level.takeFromCrate(crate.getIngredient()); }
                    } else {
                        h.setTarget(crateIdx + 1); h.setState(HelperSteerable.HelperFSMState.PICKUP); ind2Crate = true;
                    }
                } else if (isSelectingCup) {
                    h.setState(HelperSteerable.HelperFSMState.DROPOFF); ind2Crate = true;
                } else {
                    h.setState(HelperSteerable.HelperFSMState.MOVE); h.setMove(x, y); ind2Crate = false;
                }
            }
        } else {
            for (Helper h : helpers) {
                if (h.isControlled() && !h.isStunned()) {
                    helperCrosshairTexture = (helperIndex == 0) ? helper1Cross[animYIndex][animXIndex] : helper2Cross[animYIndex][animXIndex];
                    float x = crosshairX; float y = crosshairY;
                    int crateIdx = collisionController.getSeletedCrate(x, y);
                    boolean isSelectingCup = collisionController.isSelectingCup(x, y);
                    if (crateIdx != -1 || isSelectingCup) {
                        helperCrosshairTexture = (helperIndex == 0) ? helper1CrossCrate[animYIndex][animXIndex] : helper2CrossCrate[animYIndex][animXIndex];
                    }
                    if (input.didLeftClick() && !h.isStunned()) {
                        if (crateIdx == -2) { continue; }
                        else if (crateIdx != -1) {
                            if (h.getCollidingCrateIdx() == crateIdx) {
                                Crate crate = (Crate) level.getCrates().get(crateIdx);
                                if (h.hasIngredient() && h.getHeldIngredient() == crate.getIngredient()) { h.removeIngredient(); level.returnToCrate(crate.getIngredient()); }
                                else if (!h.hasIngredient() && level.getAvailable(crate.getIngredient()) > 0) { h.giveIngredient(crate.getIngredient()); level.takeFromCrate(crate.getIngredient()); }
                            } else {
                                h.setTarget(crateIdx + 1); h.setState(HelperSteerable.HelperFSMState.PICKUP);
                                if (helperIndex == 0) { ind1Crate = true; } else { ind2Crate = true; }
                            }
                        } else if (isSelectingCup) {
                            h.setState(HelperSteerable.HelperFSMState.DROPOFF);
                            if (helperIndex == 0) { ind1Crate = true; } else { ind2Crate = true; }
                        } else {
                            h.setState(HelperSteerable.HelperFSMState.MOVE); h.setMove(x, y);
                            if (helperIndex == 0) { ind1Crate = false; } else { ind2Crate = false; }
                        }
                    }
                }
                helperIndex++;
            }
        }

        if (helpers.size >= 1 && ind1Crate) { helper1Indicator = helper1IndCrate[animYIndex][animXIndex]; }
        else if (helpers.size >= 1) { helper1Indicator = helper1Ind[animYIndex][animXIndex]; }
        if (helpers.size >= 2 && ind2Crate) { helper2Indicator = helper2IndCrate[animYIndex][animXIndex]; }
        else if (helpers.size >= 2) { helper2Indicator = helper2Ind[animYIndex][animXIndex]; }

        for (ObstacleSprite o : level.getBoba()) { ((Boba) o).update(); }
        level.removeBoba();
        level.removeEnemies();

        for (ObstacleSprite e : level.getEnemies()) {
            Enemy enemy = (Enemy) e;
            enemy.applyPendingKinematic();
            enemyController.setEnemy(enemy);
            enemyController.setEnemyMovement();
            enemy.applyForce();
            if (!enemy.getMark() && enemy.getState() == Enemy.EnemyFSMState.ESCAPE &&
                level.getGraphs().get(0).getNodeAtObs(enemy.getObstacle()).equals(level.getGraphs().get(0).getNodeAtObs(level.getVents().get(enemy.getVent()).getObstacle()))) {
                if (enemy.hasIngredient()) {
                    level.ingredientStolen(enemy.getHeldIngredient());
                }
                return;
            }
        }

        for (ObstacleSprite h : level.getHelpers()) {
            Helper helper = (Helper) h;
            if (helper.getState() != Helper.HelperFSMState.IDLE && helper.getState() != Helper.HelperFSMState.STUNNED) {
                helperController.setHelper(helper);
                helperController.setHelperMovement();
                helper.applyForce();
            }
        }

        for (ObstacleSprite o : level.getBlockers()) { ((Blocker) o).update(dt); }

        for (int i = 0; i < level.getVents().size(); i++) {
            SpawnerVent vent = (SpawnerVent) level.getVents().get(i);
            if (vent.shouldAlert(timeElapsed)) { vent.getAlert().trigger(); vent.markWarned(); }
            vent.getAlert().update(dt);
            if (vent.shouldSpawn(timeElapsed)) { level.createEnemy(i); }
        }

        if (!complete && level.isRecipeComplete()) { setComplete(true); listener.exitScreen(this, EXIT_WIN); }
        //TODO: uncomment this once all levels have time limits
        //if (timeElapsed > level.getTimeLimit2()) { listener.exitScreen(this, EXIT_LOSS); }
        if (level.isRecipeImpossible()) { listener.exitScreen(this, EXIT_LOSS); }

        level.createBlockerSnapshots();
        level.createHelperSnapshots();
    }

    private void executeModeSwitch(int modeToSwitch) {
        if (modeToSwitch == 0) {
            if (!avatar.isControlled()) {
                avatar.setControlled(true); avatar.setShootingMode(true);
                avatar.setMovement(0, 0); avatar.lockVelocity(); slingshot.lockVelocity();
                for (int i = 0; i < helpers.size; i++) { helpers.get(i).setControlled(false); helpers.get(i).lockVelocity(); }
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);
            }
        } else if (modeToSwitch > 0 && modeToSwitch <= helpers.size) {
            int helperIndex = modeToSwitch - 1;
            if (helpers.size > helperIndex) {
                avatar.setControlled(false); avatar.setShootingMode(false);
                avatar.setMovement(0, 0); avatar.lockVelocity(); slingshot.lockVelocity();
                for (int i = 0; i < helpers.size; i++) { helpers.get(i).setControlled(i == helperIndex); if (i != helperIndex) helpers.get(i).lockVelocity(); }
                activeHelperIndex = helperIndex;
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);
            }
        }
    }

    float accumulator = 0f;
    public void postUpdate(float dt) {
        if (paused) { return; }
        level.addFromQueue();
        level.addFromSimQueue();
        world.step(dt, WORLD_VELOC, WORLD_POSIT);

        int simPoints = constants.get("boba").getInt("sim points");
        float maxSimTime = constants.get("boba").getInt("sim time") * 60f;
        float timeStepNum = maxSimTime / simPoints;
        for (int i = 0; i < simPoints; i++) {
            simWorld.step(timeStepNum / maxSimTime, WORLD_VELOC, WORLD_POSIT);
            if (level.getSimBoba() != null) { simTrajectoryPoints.get(i).set(level.getSimBoba().getObstacle().getPosition()); }
        }

        collisionController.processTeleports();

        Iterator<PooledList<ObstacleSprite>.Entry> iterator = level.getSprites().entryIterator();
        while (iterator.hasNext()) {
            PooledList<ObstacleSprite>.Entry entry = iterator.next();
            ObstacleSprite sprite = entry.getValue();
            Obstacle obj = sprite.getObstacle();
            if (obj.isRemoved()) { obj.deactivatePhysics(world); entry.remove(); }
            else { obj.update(dt); }
        }
        iterator = level.getTempSimObjects().entryIterator();
        while (iterator.hasNext()) {
            PooledList<ObstacleSprite>.Entry entry = iterator.next();
            entry.getValue().getObstacle().deactivatePhysics(simWorld);
            entry.remove();
        }

        numStunned = collisionController.getNumStunned();
        bugsKilled = collisionController.getBugsKilled();
        bobaHit = collisionController.getBobaHits();
    }

    public void draw(float dt) {
        float units = height / bounds.height;
        for (Helper h : helpers){ h.updateDir(dt); }

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin(camera);

        batch.draw(background, 0, 0, width, 1012.5f);
        batch.draw(wallBackground, 0, 1012.5f, width, 1350f-1012.5f);
        batch.setColor(Color.WHITE);

        batch.end();
        if (avatar.isControlled() && avatar.getShootingMode()) { /* drawBobaTrajectory(); */ }
        batch.begin(camera);

        // Debug pathfinding graph

//        for (int i = 0; i < 32; i += 1) {
//            for (int j = 0; j < 18; j += 1) {
//                if (level.getGraphs().get(0).isReachableNode(i, j)) {
//                    batch.setColor(Color.BLUE);
//                } else {
//                    batch.setColor(Color.RED);
//                }
//                batch.draw(gridCell, i * width / bounds.width, j * width / bounds.width,
//                    width / bounds.width, height / bounds.height);
//            }
//        }
//        batch.setColor(Color.WHITE);


        drawingArray.clear();
        for (ObstacleSprite obj : level.getSprites()) {
            if (obj.getName().equals("vent") || obj.getName().equals("station") || obj.getName().equals("honey") || obj.getName().equals("slingshot")) {
                obj.draw(batch);
            } else if (!obj.getName().startsWith("wall")) {
                drawingArray.add(obj);
            }
        }
        drawingArray.sort(yComparator);
        for (ObstacleSprite b : level.getBlockers()) { b.draw(batch); }
        for (ObstacleSprite obj : drawingArray) {
            if (obj.getName().equals("blocker") || (obj == avatar && !avatar.isControlled())) { continue; }
            obj.draw(batch);
        }

        for (int i = 0; i < level.getVents().size(); i++) { ((SpawnerVent) level.getVents().get(i)).getAlert().draw(batch); }
        if (debug) { for (ObstacleSprite obj : level.getSprites()) { obj.drawDebug(batch); } }

        InputController input = InputController.getInstance(viewport);
        if (avatar.isControlled() && avatar.getShootingMode()) {
            float x = crosshairX * units - units / 2;
            float y = crosshairY * units - units / 2;
            batch.draw(crosshairTexture, x, y, units, units);
            float alpha = 1; float alphaScl = -0.07f;
            for (Vector2 pos : simTrajectoryPoints) {
                batch.setColor(1, 1, 1, alpha);
                x = pos.x * units - units / 2; y = pos.y * units - units / 2;
                batch.draw(indicatorDot, x, y, units, units);
                alpha += alphaScl;
            }
        }
        batch.setColor(Color.WHITE);

        if (avatar.hasIngredient()) {
            float iconSize = avatar.getSize() * 0.5f;
            if (avatar.getHeldIngredient() == IngredientType.MILK) { batch.draw(milkIcon, avatar.getXPos() - iconSize/2f, avatar.getYPos() - iconSize/2f, iconSize, iconSize); }
            if (avatar.getHeldIngredient() == IngredientType.JELLY) { batch.draw(jellyIcon, avatar.getXPos() - iconSize/2f, avatar.getYPos() - iconSize/2f, iconSize, iconSize); }
        }
        for (Helper h : helpers) {
            if (h.hasIngredient() && !h.isAnimLocked()) {
                float iconSize = h.getSize() * 0.3f;
                float xOffset = h.getSize() * 0.1f * h.getDirection().x;
                float yOffset = h.getSize() * 0.15f * h.getDirection().y;
                if (h.getHeldIngredient() == IngredientType.MILK) { batch.draw(milkIcon, h.getXPos()-iconSize/2f + xOffset, h.getYPos()-iconSize/2f + yOffset, iconSize, iconSize); }
                if (h.getHeldIngredient() == IngredientType.JELLY) { batch.draw(jellyIcon, h.getXPos()-iconSize/2f + xOffset, h.getYPos()-iconSize/2f + yOffset, iconSize, iconSize); }
            }
        }
        for (ObstacleSprite o : level.getEnemies()) {
            Enemy enemy = (Enemy) o;
            if (enemy.hasIngredient() && !enemy.isAnimLocked()) {
                float iconSize = enemy.getSize() * 0.3f;
                float xOffset = enemy.getSize() * 0.1f * enemy.getDirection().x;
                float yOffset = enemy.getSize() * 0.15f * enemy.getDirection().y;
                if (enemy.getHeldIngredient() == IngredientType.MILK) { batch.draw(milkIcon, enemy.getXPos()-iconSize/2f + xOffset, enemy.getYPos()-iconSize/2f + yOffset, iconSize, iconSize); }
                if (enemy.getHeldIngredient() == IngredientType.JELLY) { batch.draw(jellyIcon, enemy.getXPos()-iconSize/2f + xOffset, enemy.getYPos()-iconSize/2f + yOffset, iconSize, iconSize); }
            }
        }

        float iconSize = 0f;
        for (ObstacleSprite c : level.getCrates()) {
            Crate crate = (Crate) c;
            iconSize = crate.getSize() * 0.4f;
            IngredientType type = crate.getIngredient();
            int remaining = level.getAvailable(type);
            String message = String.valueOf(remaining);
            crateCounter.setText(message); crateCounter.layout();
            if (type == IngredientType.MILK) { batch.drawText(crateCounter, crate.getXPos(), crate.getYPos() + iconSize * 0.55f); }
            if (type == IngredientType.JELLY) { batch.drawText(crateCounter, crate.getXPos(), crate.getYPos() + iconSize * 0.5f); }
        }

        float receiptStartX = uiData.getFloat("receipt starting x");
        float receiptY = uiData.getFloat("receipt y");
        float receiptW = uiData.getFloat("receipt width");
        float receiptH = uiData.getFloat("receipt height");
        float receiptSpacing = uiData.getFloat("receipt spacing");
        float receiptX = receiptStartX;
        for (IngredientType type : IngredientType.values()) {
            if (level.getRequired(type) != 0) {
                int required = level.getRequired(type); if (required == 0) continue;
                int inCup = level.getCupCount(type);
                boolean complete = (inCup >= required);
                String textureName;
                if (type == IngredientType.MILK){ textureName = complete ? "milkreceipt_complete" : "milkreceipt"; }
                else if (type == IngredientType.JELLY){ textureName = complete ? "jellyreceipt_complete" : "jellyreceipt"; }
                else { textureName = complete ? "bobareceipt_complete" : "bobareceipt"; }
                TextureRegion receiptTex = new TextureRegion(directory.getEntry(textureName, Texture.class));
                batch.draw(receiptTex, receiptX, receiptY, receiptW, receiptH);
                String text = inCup + " / " + required;
                recipeTracking.setAlignment(TextAlign.topCenter);
                recipeTracking.setText(text); recipeTracking.layout();
                batch.drawText(recipeTracking, receiptX + receiptW/2f, receiptY+75f);
                receiptX += receiptSpacing;
            }
        }

        float iconX = uiData.getFloat("controlled x");
        float iconY = uiData.getFloat("controlled y");
        float iconSize2 = uiData.getFloat("icon size");
        float iconSpacing = uiData.getFloat("controlled spacing");

        controlling.setText("Controls:");
        BitmapFont controlFont = directory.getEntry("baby tiltWarp", BitmapFont.class);
        controlling.setFont(controlFont); controlling.setColor(Color.BLACK);
        controlling.setAlignment(TextAlign.topLeft); controlling.layout();
        batch.drawText(controlling, iconX, iconY + 190);

        batch.draw(new TextureRegion(directory.getEntry("slingshotIcon", Texture.class)), iconX, iconY, iconSize2, iconSize2);
        if (helpers.size >= 1) { batch.draw(new TextureRegion(directory.getEntry("helper1Icon", Texture.class)), iconX + iconSpacing, iconY, iconSize2, iconSize2); }
        if (helpers.size >= 2) { batch.draw(new TextureRegion(directory.getEntry("helper2Icon", Texture.class)), iconX + iconSpacing * 2, iconY, iconSize2, iconSize2); }

        boolean anyHelperControlled = false;
        for (Helper h : helpers) { if (h.isControlled()) { anyHelperControlled = true; break; } }
        if (anyHelperControlled && !quickControls) {
            float scl = 3; float yOffset = units * 0.5f;
            float x = crosshairX * units - units * scl / 2;
            float y = crosshairY * units - units * scl / 2 + yOffset;
            batch.draw(helperCrosshairTexture, x, y, units * scl, units * scl);
        }

        int helperIdx = 0;
        for (Helper h : helpers) {
            float scl = 3; float yOffset = units * 0.5f; float x; float y;
            if (helperIdx == 0) {
                if (h.getState() == Helper.HelperFSMState.MOVE) { x = h.getMoveTarget().x*units-units*scl/2; y = h.getMoveTarget().y*units-units*scl/2+yOffset; batch.draw(helper1Indicator, x, y, units*scl, units*scl); }
                else if (h.getState() == Helper.HelperFSMState.PICKUP) { x = level.getCrates().get(h.getTarget()-1).getObstacle().getPosition().x*units-units*scl/2; y = level.getCrates().get(h.getTarget()-1).getObstacle().getPosition().y*units-units*scl/2+yOffset; batch.draw(helper1Indicator, x, y, units*scl, units*scl); }
                else if (h.getState() == Helper.HelperFSMState.DROPOFF) { x = level.getCup().getObstacle().getPosition().x*units-units*scl/2; y = level.getCup().getObstacle().getPosition().y*units-units*scl/2+yOffset; batch.draw(helper1Indicator, x, y, units*scl, units*scl); }
            } else if (helperIdx == 1) {
                if (h.getState() == Helper.HelperFSMState.MOVE) { x = h.getMoveTarget().x*units-units*scl/2; y = h.getMoveTarget().y*units-units*scl/2+yOffset; batch.draw(helper2Indicator, x, y, units*scl, units*scl); }
                else if (h.getState() == Helper.HelperFSMState.PICKUP) { x = level.getCrates().get(h.getTarget()-1).getObstacle().getPosition().x*units-units*scl/2; y = level.getCrates().get(h.getTarget()-1).getObstacle().getPosition().y*units-units*scl/2+yOffset; batch.draw(helper2Indicator, x, y, units*scl, units*scl); }
                else if (h.getState() == Helper.HelperFSMState.DROPOFF) { x = level.getCup().getObstacle().getPosition().x*units-units*scl/2; y = level.getCup().getObstacle().getPosition().y*units-units*scl/2+yOffset; batch.draw(helper2Indicator, x, y, units*scl, units*scl); }
            }
            helperIdx++;
        }

        int totalSeconds = (int) timeElapsed;
        if(timeElapsed < level.getTimeLimit1()){
            batch.draw(timer_3stars, timerX, timerY, timerW, timerH);
        }else if(timeElapsed < level.getTimeLimit2()){
            batch.draw(timer_2stars, timerX, timerY, timerW, timerH);
        }else{
            batch.draw(timer_1star, timerX, timerY, timerW, timerH);
        }
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String timeText = String.format("%d:%02d", minutes, seconds);
        digitalClock.setText(timeText);
        digitalClock.layout();
        batch.drawText(digitalClock, timerX+(timerW/2), timerY+68);

        batch.end();

        if(tutorial != null) {
            tutorial.draw(batch, camera);
        }
//
//        GraphDebugRenderer renderer = new GraphDebugRenderer();
//        renderer.render(level.getGraphs().get(0), camera, units);
//        renderer.drawPath(level.getHelpers().get(0).getPath());
    }

    public void resize(int width, int height) {
        this.width = 1800f; this.height = 1012.5f;
        this.units = this.height / bounds.height;
        if (camera == null) { camera = new OrthographicCamera(); }
        if (viewport == null) { viewport = new FitViewport(1800f, 1350f, camera); }
        viewport.update(width, height, true);
        camera.position.set(900f, 675f, 0); camera.update();
        scale.x = this.width / bounds.width;
        scale.y = this.height / bounds.height;
        if (world == null) { reset(); }
    }

    public void render(float delta) {
        if (active) {
            boolean tutorialActive = levelSelected > 2 && (tutorial != null) && (tutorial.isActive());
            if (!paused && !tutorialActive) { timeElapsed += delta; }
            if (preUpdate(delta)) { update(delta); postUpdate(delta); }
            draw(delta);
        }
    }

    private void updateBobaTrajectory(Vector2 startPos, Vector2 direction, float bobaSpeed) {
        trajectoryPoints.clear();
        if (direction.len2() < 0.001f) { return; }
        JsonValue bobaSettings = constants.get("boba");
        if (bobaSettings == null) { return; }
        float bounceDamping = bobaSettings.getFloat("restitution", 0.85f);
        int maxBounces = bobaSettings.getInt("max bounces", 3);
        final int SEGMENTS = 15; final float MAX_DISTANCE = 20f; final float DT = 0.05f;
        final float COLLISION_OFFSET = 0.3f; final float MIN_VELOCITY = 0.05f;
        Vector2 currentPos = new Vector2(startPos);
        Vector2 currentVelocity = new Vector2(direction).scl(bobaSpeed);
        float distanceTraveled = 0f;
        trajectoryPoints.add(new Vector2(currentPos));
        int bounceCount = 0;
        for (int i = 0; i < SEGMENTS && distanceTraveled < MAX_DISTANCE && bounceCount < maxBounces; i++) {
            Vector2 nextPos = new Vector2(currentPos); nextPos.add(currentVelocity.x * DT, currentVelocity.y * DT);
            TrajectoryRaycastCallback rayCallback = new TrajectoryRaycastCallback();
            world.rayCast(rayCallback, currentPos, nextPos);
            if (rayCallback.hit) {
                trajectoryPoints.add(new Vector2(rayCallback.hitPoint));
                distanceTraveled += currentPos.dst(rayCallback.hitPoint);
                Vector2 normal = new Vector2(rayCallback.hitNormal).scl(-1f);
                float dotProduct = currentVelocity.dot(normal);
                Vector2 reflected = new Vector2(currentVelocity); reflected.sub(normal.scl(2.0f * dotProduct)); reflected.scl(bounceDamping);
                currentVelocity.set(reflected);
                currentPos.set(rayCallback.hitPoint); currentPos.add(rayCallback.hitNormal.scl(COLLISION_OFFSET));
                bounceCount++;
                if (currentVelocity.len2() < MIN_VELOCITY * MIN_VELOCITY) { trajectoryPoints.add(new Vector2(currentPos)); break; }
            } else {
                distanceTraveled += currentPos.dst(nextPos); currentPos.set(nextPos);
                if (currentVelocity.len2() < MIN_VELOCITY * MIN_VELOCITY) { trajectoryPoints.add(new Vector2(currentPos)); break; }
            }
        }
        if (currentVelocity.len2() >= MIN_VELOCITY * MIN_VELOCITY && trajectoryPoints.size > 0) {
            Vector2 lastPoint = trajectoryPoints.get(trajectoryPoints.size - 1);
            if (!lastPoint.equals(currentPos)) { trajectoryPoints.add(new Vector2(currentPos)); }
        }
    }

    private class TrajectoryRaycastCallback implements RayCastCallback {
        Vector2 hitPoint = new Vector2(); Vector2 hitNormal = new Vector2(); boolean hit = false;
        @Override
        public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
            if (fixture.isSensor()) { return -1; }
            Object userData = fixture.getBody().getUserData();
            if (userData instanceof Slingshot || userData instanceof Player || userData instanceof Helper || userData instanceof Boba) { return -1; }
            hitPoint.set(point); hitNormal.set(normal); hit = true;
            return 0;
        }
    }

    private void drawBobaTrajectory() {
        if (trajectoryPoints.size < 2) { return; }
        float unitsPerPixel = bounds.height / height; float pixelsPerUnit = 1f / unitsPerPixel;
        trajectoryRenderer.setProjectionMatrix(camera.combined); trajectoryRenderer.setColor(Color.RED);
        trajectoryRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < trajectoryPoints.size - 1; i++) {
            Vector2 p1 = trajectoryPoints.get(i); Vector2 p2 = trajectoryPoints.get(i + 1);
            trajectoryRenderer.line(p1.x*pixelsPerUnit, p1.y*pixelsPerUnit, p2.x*pixelsPerUnit, p2.y*pixelsPerUnit);
        }
        trajectoryRenderer.end();
        trajectoryRenderer.begin(ShapeRenderer.ShapeType.Filled); trajectoryRenderer.setColor(1, 1, 0, 0.6f);
        for (Vector2 point : trajectoryPoints) { trajectoryRenderer.circle(point.x*pixelsPerUnit, point.y*pixelsPerUnit, 0.5f); }
        trajectoryRenderer.end();
    }

    public void pause() { }
    public void resume() { }

    public void show() {
        active = true; inputIgnoreTimer = INPUT_IGNORE_DURATION;
    }

    public void hide() { active = false; }
    public void setScreenListener(ScreenListener listener) { this.listener = listener; }
}

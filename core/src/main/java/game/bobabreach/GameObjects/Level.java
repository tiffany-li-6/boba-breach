package game.bobabreach.GameObjects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics2.CapsuleObstacle;
import edu.cornell.gdiac.physics2.Obstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.physics2.WheelObstacle;
import edu.cornell.gdiac.util.PooledList;
import edu.cornell.gdiac.util.RandomGenerator;
import game.bobabreach.AnimationLibrary;
import game.bobabreach.BugAnimations;
import game.bobabreach.DirectionalAnimation;
import game.bobabreach.GameObjects.Enemy.EnemyType;
import game.bobabreach.WorldGraph;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the state of a single level, including all recipe ingredients and the
 * number of ingredients being held, available in crates, and currently in the cup.
 */
public class Level {

    private Player avatar;
    private Slingshot slingshot;
    private Cup cup;
    private Array<Helper> helpers;

    private float units;

    /**
     * Required ingredients amounts to complete the level
     */
    private EnumMap<IngredientType, Integer> recipe;

    /**
     * Ingredients currently available in crates (or boba left to shoot)
     */
    private EnumMap<IngredientType, Integer> available;

    /**
     * Ingredients currently being held by Pearl, helper, or enemy bugs
     */
    private EnumMap<IngredientType, Integer> beingHeld;

    /**
     * Ingredients already handed into the cup
     */
    private EnumMap<IngredientType, Integer> inCup;

    /**
     * List of ingredients that can be found in crates
     */
    private IngredientType[] crateTypes = {
        IngredientType.MILK,
        IngredientType.JELLY
    };

    /**
     * If true, stealer bugs target the closest crate with available
     * ingredients instead of a random one. Set via the Tiled map property
     * "stealer_target" = "closest". Defaults to false (random).
     */
    private boolean closestCrateMode = false;

    /**
     * Challenging time limit (in seconds) to earn a star.
     */
    private int timeLimit1;

    /**
     * More forgiving time limit (in seconds) to earn a star
     */
    private int timeLimit2;

    /**
     * How many times a helper can be stunned by an attacker before losing a star
     */
    private int maxStuns;

    /**
     * All the objects in the world.
     */
    protected PooledList<ObstacleSprite> sprites = new PooledList<ObstacleSprite>();

    /**
     * All the objects in the simulated physics world.
     */
    protected PooledList<ObstacleSprite> simSprites = new PooledList<ObstacleSprite>();

    protected ObstacleSprite simBoba;

    /**
     * All the BOBA objects in the world. Used to optimize deletion
     */
    protected PooledList<ObstacleSprite> bobaList = new PooledList<ObstacleSprite>();

    /**
     * All the CRATE objects in the world. Used to assign targets
     */
    protected PooledList<ObstacleSprite> crateList = new PooledList<ObstacleSprite>();

    /**
     * All the VENT objects in the world. Used to spawn enemies
     */
    protected PooledList<ObstacleSprite> ventList = new PooledList<ObstacleSprite>();

    /**
     * All the HELPERSPAWN objects in the world.
     */
    protected PooledList<ObstacleSprite> stationList = new PooledList<ObstacleSprite>();

    /**
     * All the ENEMY objects in the world.
     */
    protected PooledList<ObstacleSprite> enemyList = new PooledList<ObstacleSprite>();

    /**
     * Queue for adding objects
     */
    protected PooledList<ObstacleSprite> addQueue = new PooledList<ObstacleSprite>();

    /**
     * Queue for adding objects to the simulated world
     */
    protected PooledList<ObstacleSprite> addQueueSim = new PooledList<ObstacleSprite>();

    /**
     * All temporary objects in the simulated world. Delete after each frame
     */
    protected PooledList<ObstacleSprite> tempSimObjects = new PooledList<ObstacleSprite>();

    /**
     * The list of world graphs
     */
    private final Array<WorldGraph> worldGraphs = new Array<>();

    /**
     * All the portals in the world
     */
    protected PooledList<ObstacleSprite> portalList = new PooledList<ObstacleSprite>();

    /**
     * All blocker bugs
     */
    protected PooledList<ObstacleSprite> blockerList = new PooledList<ObstacleSprite>();

    /**
     * All the cobweb objects in the world.
     */
    protected PooledList<ObstacleSprite> cobwebList = new PooledList<ObstacleSprite>();

    /**
     * All the honey objects in the world.
     */
    protected PooledList<ObstacleSprite> honeyList = new PooledList<ObstacleSprite>();

    /**
     * All the decoration objects in the world
     * Same physics as empty crates; purely visual variation
     */
    protected PooledList<ObstacleSprite> decorationList = new PooledList<ObstacleSprite>();

    /**
     * Reference to the Box2D world
     */
    protected World world;

    protected World simWorld;

    /**
     * The JSON file to read from
     */
    protected JsonValue constants;


    /**
     * The JSON file containing this level's data
     */
    protected JsonValue levelData;

    /**
     * The file containing all textures
     */
    protected AssetDirectory directory;

    /**
     * Bugs atlas, used by setObstacles()
     *
     * @return
     */
    private TextureAtlas bugsAtlas;

    /**
     * orange helper anims
     */
    private DirectionalAnimation helperOrangeAnim;

    /**
     * slingshot atlas, used for animation
     *
     * @return
     */
    private TextureAtlas slingshotAtlas;


    // Getters

    public PooledList<ObstacleSprite> getSprites() {
        return sprites;
    }

    public PooledList<ObstacleSprite> getTempSimObjects() {
        return tempSimObjects;
    }

    public ObstacleSprite getSimBoba() {
        return simBoba;
    }

    public PooledList<ObstacleSprite> getCrates() {
        return crateList;
    }

    public Array<WorldGraph> getGraphs() {
        return worldGraphs;
    }

    public PooledList<ObstacleSprite> getVents() {
        return ventList;
    }

    public PooledList<ObstacleSprite> getPortals() {
        return portalList;
    }

    public PooledList<ObstacleSprite> getEnemies() {
        return enemyList;
    }

    public Player getPlayer() {
        return avatar;
    }

    public PooledList<ObstacleSprite> getBoba() {
        return bobaList;
    }

    public Slingshot getSlingshot() {
        return slingshot;
    }

    public Cup getCup() {
        return cup;
    }

    public PooledList<ObstacleSprite> getBlockers() {
        return blockerList;
    }

    public PooledList<ObstacleSprite> getCobwebs() {
        return cobwebList;
    }

    public PooledList<ObstacleSprite> getHoneys() {
        return honeyList;
    }

    public Array<Helper> getHelpers() {
        return helpers;
    }

    public int getTimeLimit1() {
        return timeLimit1;
    }

    public int getTimeLimit2() {
        return timeLimit2;
    }

    public int getMaxStuns() {
        return maxStuns;
    }

    public PooledList<ObstacleSprite> getDecorations() {
        return decorationList;
    }


    /**
     * Creates a new level with the given recipe and available ingredients
     *
     * @param constants JSON file to load level elements from
     * @param world     Box2D world we're using
     */
    public Level(JsonValue constants, JsonValue levelData, AssetDirectory directory, World world, World simWorld) {
        this.constants = constants;
        this.levelData = levelData;
        this.world = world;
        this.simWorld = simWorld;
        this.directory = directory;
        setIngredients();

//        JsonValue rec = levelData.get("recipe");
//        recipe.put(IngredientType.MILK,rec.getInt("milk"));
//        recipe.put(IngredientType.JELLY,rec.getInt("jelly"));
//        recipe.put(IngredientType.BOBA,rec.getInt("boba"));

        //available ingredients set here
//        available = new EnumMap<>(IngredientType.class);
//        JsonValue avail = levelData.get("available");
//        available.put(IngredientType.MILK,avail.getInt("milk"));
//        available.put(IngredientType.JELLY,avail.getInt("jelly"));
//        available.put(IngredientType.BOBA,avail.getInt("boba"));

        //set all ingredients in the cup to 0
        this.inCup = new EnumMap<>(IngredientType.class);
        for (IngredientType type : IngredientType.values()) {
            inCup.put(type, 0);
        }

        this.beingHeld = new EnumMap<>(IngredientType.class);
        for (IngredientType type : IngredientType.values()) {
            beingHeld.put(type, 0);
        }
    }

    /**
     * Dispose of all game objects in the world, and set it null
     */
    public void dispose() {
        if (world != null) {
            for (ObstacleSprite sprite : sprites) {
                Obstacle obj = sprite.getObstacle();
                obj.deactivatePhysics(world);
            }
            for (ObstacleSprite sprite : simSprites) {
                Obstacle obj = sprite.getObstacle();
                obj.deactivatePhysics(simWorld);
            }
        }
        simSprites.clear();
        sprites.clear();
        addQueue.clear();
        world.dispose();
        simWorld.dispose();
        addQueue = null;
        sprites = null;
        world = null;
    }

    /**
     *
     * Adds a physics sprite in to the insertion queue.
     * <p>
     * Objects on the queue are added just before collision processing. We do
     * this to control object creation.
     * <p>
     * param sprite The sprite to add
     */
    public void addQueuedObject(ObstacleSprite sprite) {
        addQueue.add(sprite);
    }

    public void addQueuedObjectSim(ObstacleSprite sprite) {
        addQueueSim.add(sprite);
    }


    /**
     * Immediately adds a physics sprite to the physics world
     * <p>
     * param sprite The sprite to add
     */
    protected void addSprite(ObstacleSprite sprite) {
        sprites.add(sprite);
        sprite.getObstacle().activatePhysics(world);
    }

    /**
     * Immediately adds a physics sprite to the SIMULATED physics world
     * <p>
     * param sprite The sprite to add
     */
    protected void addSpriteSim(ObstacleSprite sprite) {
        simSprites.add(sprite);
        sprite.getObstacle().activatePhysics(simWorld);
    }

    /**
     * Reset level state
     */
    public void resetLevel(float units, float width, float height) {
        this.units = units;

        if (world != null) {
            for (ObstacleSprite sprite : sprites) {
                Obstacle obj = sprite.getObstacle();
                sprite.getObstacle().deactivatePhysics(world);
            }
        }
        if (simWorld != null && simSprites != null) {
            for (ObstacleSprite sprite : simSprites) {
                Obstacle obj = sprite.getObstacle();
                sprite.getObstacle().deactivatePhysics(simWorld);
            }
        }
        sprites.clear();
        addQueue.clear();
        bobaList.clear();
        crateList.clear();
        cobwebList.clear();
        honeyList.clear();
        worldGraphs.clear();
        blockerList.clear();
        enemyList.clear();
        ventList.clear();
        stationList.clear();
        portalList.clear();
        decorationList.clear();

        helpers = new Array<>();
        setIngredients();

        //required ingredients set here
//        recipe = new EnumMap<>(IngredientType.class);
//        JsonValue rec = levelData.get("recipe");
//        recipe.put(IngredientType.MILK,rec.getInt("milk"));
//        recipe.put(IngredientType.JELLY,rec.getInt("jelly"));
//        recipe.put(IngredientType.BOBA,rec.getInt("boba"));
//
//        //available ingredients set here
//        available = new EnumMap<>(IngredientType.class);
//        JsonValue avail = levelData.get("available");
//        available.put(IngredientType.MILK,avail.getInt("milk"));
//        available.put(IngredientType.JELLY,avail.getInt("jelly"));
//        available.put(IngredientType.BOBA,avail.getInt("boba"));

        //set all ingredients in the cup to 0
        this.inCup = new EnumMap<>(IngredientType.class);
        for (IngredientType type : IngredientType.values()) {
            inCup.put(type, 0);
        }

        this.beingHeld = new EnumMap<>(IngredientType.class);
        for (IngredientType type : IngredientType.values()) {
            beingHeld.put(type, 0);
        }

        if (world != null) {
            Array<Body> bodies = new Array<Body>();
            world.getBodies(bodies);
            for (Body b : bodies) {
                world.destroyBody(b);
            }
            //world.dispose();
            //world = null;
        }
        populate(units, width, height);
    }


    private void populate(float units, float width, float height) {
        /*

        this.bugsAtlas =
            new TextureAtlas(Gdx.files.internal("assets/atlases/bugs.atlas"));



        //set DirectionalAnimation or AngularAnimation or etc. for all animations
        this.helperOrangeAnim =
            new DirectionalAnimation(bugsAtlas, "helper_orange_fly", 0.08f);

        for (Helper h : helpers) {
            h.setAnimations(AnimationLibrary.bugAnims.get(h.getType())); //h.getType() returns a BugType enum

        }

         */


        //end of animation texture loading

        // Create (invisible) walls
        Texture texture = directory.getEntry("wall tile", Texture.class);
        Wall wall;
        Wall wall2;
        JsonValue walls = constants.get("walls");
        JsonValue wallConsts = constants.get("walls");
        float thickness = wallConsts.getFloat("thickness");

//        JsonValue customWalls = levelData.get("custom_walls");
//        if (customWalls != null && customWalls.has("pos")) {
//            float[] wallData = customWalls.get("pos").asFloatArray();
//
//            for (int i = 0; i + 3 < wallData.length; i += 4) {
//                Wall customWall = new Wall(wallData[i], wallData[i + 1],
//                    wallData[i + 2] / units, wallData[i + 3] / units, units, walls);
//                customWall.getObstacle().setName("wall");
//                addSprite(customWall);
//            }
//        }

        // Left wall
        // wall = new Wall(0, height / units / 2, thickness, bounds.height, units, walls);
        wall = new Wall(0, height / 2, thickness, height + 1, units, walls);
        wall.getObstacle().setName("wall1");
        // wall.setTexture( texture );
        addSprite(wall);
        wall2 = new Wall(0, height / 2, thickness, height + 1, units, walls);
        wall2.getObstacle().setName("wall1");
        addSpriteSim(wall2);

        // Right wall
        // wall = new Wall(width / units, height / units / 2, thickness, bounds.height, units, walls);
        wall = new Wall(width, height / 2, thickness, height + 1, units, walls);
        wall.getObstacle().setName("wall2");
        // wall.setTexture( texture );
        addSprite(wall);
        wall2 = new Wall(width, height / 2, thickness, height + 1, units, walls);
        wall2.getObstacle().setName("wall1");
        addSpriteSim(wall2);

        // Top wall
        // wall = new Wall(width / units / 2, height / units, bounds.width, thickness, units, walls);
        wall = new Wall(width / 2, height + 1, width + 1, thickness, units, walls);
        wall.getObstacle().setName("wall3");
        // wall.setTexture( texture );
        addSprite(wall);
        wall2 = new Wall(width / 2, height + 1, width + 1, thickness, units, walls);
        wall2.getObstacle().setName("wall1");
        addSpriteSim(wall2);

        // Bottom wall
        // wall = new Wall(width / units / 2, 0, bounds.width, thickness, units, walls);
        wall = new Wall(width / 2, 0, width + 1, thickness, units, walls);
        wall.getObstacle().setName("wall4");
        // wall.setTexture( texture );
        addSprite(wall);
        wall2 = new Wall(width / 2, 0, width + 1, thickness, units, walls);
        wall2.getObstacle().setName("wall1");
        addSpriteSim(wall2);

        setObstacles(width, height); // Handles all static level elements (obstacles, spawners)

        // Create one-way portal pairs from level data
        createPortals();

        /**
         // Create ingredient crates
         JsonValue crates = levelData.get("crates");
         JsonValue crateConsts = constants.get("crates");
         JsonValue boxjv = crates.get("positions");
         int ingredientInd = 0;
         int crateIndex = 0;
         for (int ii = 0; ii < boxjv.size; ii += 2) {
         // int id = RandomGenerator.getInt(0, 1) + 1;
         texture = directory.getEntry("jelly_crate_full", Texture.class);
         IngredientType type = crateTypes[(ii/2) % crateTypes.length];
         Crate crate = new Crate(boxjv.getFloat(ii), boxjv.getFloat(ii + 1),
         units, crateConsts, type, crateIndex);
         crate.getObstacle().setName("crate");

         crate.setTexture(texture);
         addSprite(crate);
         crateList.add(crate);
         crateIndex++;
         crate.createSensor();
         crate.createHitbox();
         // System.out.println(world.getFixtureCount());
         }

         // Create Cup
         texture = directory.getEntry("cup", Texture.class);
         JsonValue cupData = constants.get("cup");
         cup = new Cup(units, constants.get("cup"), cupData);
         //        float cupSize = constants.get("cup").getFloat("size");
         //        float cupX = bounds.width - cupSize;
         //        float cupY = bounds.height / 3f;
         //        cup.getObstacle().setPosition(cupX, cupY);
         cup.setTexture(texture);
         addSprite(cup);


         // Create obstacles
         // setObstacles();

         //        JsonValue obstacles = levelData.get("level_obstacles");
         //        JsonValue obstacleConsts = constants.get("levelobstacle");
         //        if (obstacles != null) {
         //
         //            JsonValue pos = obstacles.get("positions");
         //            Texture obsttexture = directory.getEntry("levelobstacle", Texture.class);
         //
         //            for (int ii = 0; ii < pos.size; ii += 2) {
         //
         //                float x = pos.getFloat(ii);
         //                float y = pos.getFloat(ii + 1);
         //
         //                LevelObstacle obj = new LevelObstacle(x, y, units, obstacleConsts);
         //
         //                obj.getObstacle().setName("levelobstacle");
         //                obj.setTexture(obsttexture);
         //                addSprite(obj);
         //                obj.createSensor();
         //            }
         //        }


         // Create graphs AFTER all static obstacles are created (one for each crate, and one without any crates)
         worldGraphs.add(new WorldGraph(constants.get("world"), world, width, height));
         for (ObstacleSprite o : crateList) {
         worldGraphs.add(new WorldGraph(constants.get("world"), world, width, height, (Crate) o, false));
         // worldGraphs.add(new WorldGraph(constants.get("world"), world, bounds.width, bounds.height, (Crate) o, true));
         }



         // Create enemy spawner vents
         JsonValue vents = constants.get("vents");
         JsonValue ventConsts = constants.get("vents");
         float[] ventPos = vents.get("positions").asFloatArray();
         JsonValue ventTargCrate = vents.get("target_crate");
         texture = directory.getEntry("vent", Texture.class);
         for (int ii = 0; ii < ventPos.length; ii += 2) {
         SpawnerVent vent = new SpawnerVent(ventPos[ii], ventPos[ii + 1], ventTargCrate.getInt(ii / 2), units, ventConsts);
         vent.getObstacle().setName("vent");
         vent.setTexture(texture);
         addSprite(vent);
         ventList.add(vent);
         }


         // Create helper spawner vents & helpers
         JsonValue helperVents = constants.get("helper spawn");
         JsonValue helperData = constants.get("helper");
         float[] spawnPos = helperVents.get("positions").asFloatArray();
         texture = directory.getEntry("helper spawn", Texture.class);
         Texture helperTexture = directory.getEntry("helper", Texture.class);
         //Texture helperOutlineTexture = directory.getEntry("helper outline", Texture.class);
         helpers = new Array<>();

         for (int ii = 0; ii < spawnPos.length; ii += 2) {
         HelperSpawn station = new HelperSpawn(spawnPos[ii], spawnPos[ii + 1], units, helperVents);
         station.getObstacle().setName("station");
         station.setTexture(texture);
         addSprite(station);
         stationList.add(station);

         Helper helper = new Helper(
         station.getObstacle().getX(),
         station.getObstacle().getY(),
         units,
         helperData,
         0,
         (HelperSpawn) stationList.getTail(),
         helperTexture,
         helperOutlineTexture
         );

         helper.setTexture(helperTexture);
         helper.setAnimations(this.pearlFlyAnimations);
         addSprite(helper);
         helpers.add(helper);
         }
         */

        // Create slingshot
        //texture = directory.getEntry("slingshot", Texture.class);
        this.slingshot = new Slingshot(units, constants.get("slingshot"));
        slingshot.setAnimations(AnimationLibrary.slingshotAnims);
        //slingshot.setTexture(texture);
        addSprite(slingshot);
        slingshot.adjustFix();

        // Create player
        texture = directory.getEntry("player", Texture.class);
        this.avatar = new Player(units, constants.get("player"));
        avatar.setTexture(texture);

        avatar.setShootingMode(true); // DISABLE WALKING
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);

        addSprite(avatar);
        // Have to do after body is created
        avatar.createSensor();
    }

    /**
     * Creates one-way portal pairs from the level data.
     *
     * Reads from the "Portals" Tiled object layer if present. Each portal is
     * a point object with custom properties:
     *   - role:    "entry" or "exit"
     *   - pair_id: int; matching value links an entry to its exit
     *   - dir:     "left" / "right" / "up" / "down"
     *              On exits: controls both the rendered pipe orientation AND
     *              the direction boba is launched. Required.
     *              On entries: purely visual (which way the pipe sprite
     *              faces). Boba can still enter from any angle. Defaults to
     *              "right" if absent.
     *   - color:   "blue", "green", etc. Picks which pipe texture set to
     *              use. Defaults to "blue" if absent. Both endpoints of a
     *              pair are expected to share the same color, but the entry
     *              and exit each carry their own copy of the property.
     *
     * Texture lookup: "<color>_portal_<dir>" (dir lowercased), e.g.
     * "blue_portal_up", "green_portal_left". Same key format for entries
     * and exits.
     *
     * Pixel-to-world coordinates use the same transform as vents and helpers
     * elsewhere in setObstacles().
     *
     * Mismatched pairs (entry without exit, or vice versa) are logged and
     * skipped rather than crashing the load. If the "Portals" layer is
     * missing or empty, this method does nothing.
     */
    private void createPortals() {
        JsonValue layers = levelData.get("layers");
        if (layers == null) return;

        // Find the Portals object layer by name
        JsonValue portalLayer = null;
        for (JsonValue layer : layers) {
            if ("Portals".equals(layer.getString("name", ""))) {
                portalLayer = layer;
                break;
            }
        }
        if (portalLayer == null) return;

        JsonValue objects = portalLayer.get("objects");
        if (objects == null || objects.size == 0) return;

        JsonValue portalConsts = constants.get("portals");
        float portalWidth  = portalConsts != null ? portalConsts.getFloat("width",  0.4f) : 0.4f;
        float portalHeight = portalConsts != null ? portalConsts.getFloat("height", 1.2f) : 1.2f;

        // Bucket objects by pair_id, keeping entries and exits separate
        HashMap<Integer, JsonValue> entriesByPair = new HashMap<>();
        HashMap<Integer, JsonValue> exitsByPair   = new HashMap<>();

        for (JsonValue obj : objects) {
            JsonValue properties = obj.get("properties");
            if (properties == null) {
                System.err.println("Portal object " + obj.getInt("id", -1) +
                    " has no properties; skipping");
                continue;
            }

            String role = null;
            int pairId = -1;
            for (JsonValue prop : properties) {
                String name = prop.getString("name", "");
                if (name.equals("role")) {
                    role = prop.getString("value");
                } else if (name.equals("pair_id")) {
                    pairId = prop.getInt("value");
                }
            }

            if (role == null || pairId < 0) {
                System.err.println("Portal object " + obj.getInt("id", -1) +
                    " missing role or pair_id; skipping");
                continue;
            }

            if (role.equals("entry")) {
                entriesByPair.put(pairId, obj);
            } else if (role.equals("exit")) {
                exitsByPair.put(pairId, obj);
            } else {
                System.err.println("Portal object " + obj.getInt("id", -1) +
                    " has unknown role '" + role + "'; skipping");
            }
        }

        // Build a portal pair for each matched (entry, exit)
        for (Map.Entry<Integer, JsonValue> e : entriesByPair.entrySet()) {
            int pairId = e.getKey();
            JsonValue entryObj = e.getValue();
            JsonValue exitObj  = exitsByPair.get(pairId);
            if (exitObj == null) {
                System.err.println("Portal pair_id " + pairId +
                    " has entry but no exit; skipping");
                continue;
            }

            // Same pixel -> world transform used for vents and helpers
            float entryX = entryObj.getFloat("x") / 600f * 2f + 1f;
            float entryY = 18f - ((entryObj.getFloat("y") / 520f - 1f) * 1.8f) - 1f;
            float exitX  = exitObj.getFloat("x")  / 600f * 2f + 1f;
            float exitY  = 18f - ((exitObj.getFloat("y")  / 520f - 1f) * 1.8f) - 1f;

            // Read color and visual direction from each endpoint
            Portal.Direction entryDir = readDirection(entryObj, Portal.Direction.RIGHT);
            String           entryColor = readColor(entryObj, "blue");
            Portal.Direction exitDir  = readDirection(exitObj,  Portal.Direction.RIGHT);
            String           exitColor  = readColor(exitObj,  "blue");

            // entryDir is visual only; exitDir drives boba launch direction
            Portal entry = new Portal(entryX, entryY, portalWidth, portalHeight,
                units, entryDir, true);
            Portal exit  = new Portal(exitX,  exitY,  portalWidth, portalHeight,
                units, exitDir, false);

            // ONE-WAY: only link entry -> exit
            entry.setLinkedPortal(exit);

            // Texture keys: <color>_portal_<dir>
            String entryKey = entryColor + "_portal_" + entryDir.name().toLowerCase();
            String exitKey  = exitColor  + "_portal_" + exitDir.name().toLowerCase();
            Texture entryTex = directory.getEntry(entryKey, Texture.class);
            Texture exitTex  = directory.getEntry(exitKey,  Texture.class);
            if (entryTex == null) {
                System.err.println("Missing portal texture: " + entryKey);
            } else {
                entry.setTexture(entryTex);
            }
            if (exitTex == null) {
                System.err.println("Missing portal texture: " + exitKey);
            } else {
                exit.setTexture(exitTex);
            }

            addSprite(entry);
            addSprite(exit);
            portalList.add(entry);
            portalList.add(exit);
        }

        // Warn about exits with no matching entry
        for (Integer pairId : exitsByPair.keySet()) {
            if (!entriesByPair.containsKey(pairId)) {
                System.err.println("Portal pair_id " + pairId +
                    " has exit but no entry");
            }
        }
    }

    /**
     * Reads the "dir" custom property from a Tiled object, or returns the
     * fallback if absent or unparseable.
     */
    private Portal.Direction readDirection(JsonValue obj, Portal.Direction fallback) {
        JsonValue properties = obj.get("properties");
        if (properties == null) return fallback;
        for (JsonValue prop : properties) {
            if ("dir".equals(prop.getString("name", ""))) {
                try {
                    return Portal.Direction.fromString(prop.getString("value"));
                } catch (IllegalArgumentException ex) {
                    System.err.println("Portal object " + obj.getInt("id", -1) +
                        " has invalid dir '" + prop.getString("value") +
                        "'; using " + fallback);
                    return fallback;
                }
            }
        }
        return fallback;
    }

    /**
     * Reads the "color" custom property from a Tiled object, or returns the
     * fallback if absent.
     */
    private String readColor(JsonValue obj, String fallback) {
        JsonValue properties = obj.get("properties");
        if (properties == null) return fallback;
        for (JsonValue prop : properties) {
            if ("color".equals(prop.getString("name", ""))) {
                return prop.getString("value", fallback);
            }
        }
        return fallback;
    }

    /**
     * Adds a new boba to the world and send it in the right direction.
     */
    public void createBoba(Vector2 slingshotPos, Vector2 shootDirection) {
        JsonValue bulletjv = constants.get("boba");

        Texture texture = directory.getEntry("boba", Texture.class);
        Boba boba = new Boba(units, bulletjv, slingshotPos, shootDirection);
        // Boba boba = new Boba(units, bulletjv, slingshotPos.scl(1 / units), shootDirection);
        boba.setTexture(texture);
        addQueuedObject(boba);
        bobaList.add(boba);

        // Ingredient tracking stuff
        int before = available.get(IngredientType.BOBA);
        if (before > 0) {
            available.put(IngredientType.BOBA, before - 1);
        }
    }

    /**
     * Add a simulated boba projectile firing from the slingshot.
     */
    public void createBobaSim(Vector2 slingshotPos, Vector2 shootDirection) {
        JsonValue bulletjv = constants.get("boba");

        float xOffset = bulletjv.getFloat("offset", 0);
        xOffset *= shootDirection.x;
        float yOffset = bulletjv.getFloat("offset", 0);
        yOffset *= shootDirection.y;
        float s = bulletjv.getFloat("size");
        float radius = s * units / 2.0f;

        // Texture texture = directory.getEntry("boba", Texture.class);
        // Boba boba = new Boba(units, bulletjv, slingshotPos, shootDirection);
        Obstacle obstacle = new WheelObstacle(slingshotPos.x + xOffset, slingshotPos.y + yOffset, s / 2 * 0.5f);

        // Square obstacle for testing
        // obstacle = new BoxObstacle(pos.x + xOffset, pos.y + yOffset, s, s);
//        System.out.println("Boba x: " + (pos.x + xOffset));
//        System.out.println("Boba y: " + pos.y);
        obstacle.setDensity(bulletjv.getFloat("density", 0));
        obstacle.setPhysicsUnits(units);
        obstacle.setBullet(true);
        obstacle.setRestitution(bulletjv.getFloat("restitution", 1));
        obstacle.setGravityScale(0);
        // obstacle.setUserData( this );
        obstacle.setFixedRotation(true);
        obstacle.getFilterData().groupIndex = -1;
        // obstacle.setName( "boba" );

        float speed = bulletjv.getFloat("speed", 0);
        obstacle.setLinearVelocity(shootDirection.scl(speed));
        // Boba boba = new Boba(units, bulletjv, slingshotPos.scl(1 / units), shootDirection);
        ObstacleSprite simBoba = new ObstacleSprite(obstacle);
        addQueuedObjectSim(simBoba);
        tempSimObjects.add(simBoba);
        this.simBoba = simBoba;
    }

    /**
     * Spawns in a new enemy to the world from a vent.
     */
    public void createEnemy(int v) {
        SpawnerVent vent = (SpawnerVent) ventList.get(v);
        if (vent.getEnemyType() == 2) { // blocker spawning is handled elsewhere
            createBlocker(vent.getObstacle().getX());
            return;
        }

        // Cannot spawn a stealer/attacker without any crates to target.
        // Prevents IllegalArgumentException from RandomGenerator.getInt(1, 0)
        // when the level has spawner vents but no crates.
        if (crateList.isEmpty()) {
            return;
        }


        int ingredientIdx;
        IngredientType ingredient;

        if (closestCrateMode) {
            // Target the closest crate to the spawning vent that still has
            // available ingredients. Falls back to crate 1 if all are empty.
            float ventX = vent.getObstacle().getX();
            float ventY = vent.getObstacle().getY();
            float minDist = Float.MAX_VALUE;
            ingredientIdx = 1; // fallback
            for (int i = 0; i < crateList.size(); i++) {
                Crate crate = (Crate) crateList.get(i);
                if (getAvailable(crate.getIngredient()) <= 0) continue;
                float dx = crate.getObstacle().getX() - ventX;
                float dy = crate.getObstacle().getY() - ventY;
                float dist = dx * dx + dy * dy; // squared distance, no sqrt needed
                if (dist < minDist) {
                    minDist = dist;
                    ingredientIdx = i + 1; // 1-based to match enemy target convention
                }
            }
            ingredient = ((Crate) crateList.get(ingredientIdx - 1)).getIngredient();
        } else {
            // Default: pick a random crate that has available ingredients.
            int attempts = 0;
            do {
                ingredientIdx = RandomGenerator.getInt(1, crateList.size());
                ingredient = IngredientType.values()[ingredientIdx - 1];
                attempts++;
            } while (getAvailable(ingredient) <= 0 && attempts <= 10);
        }

        // Enemy.EnemyType type = RandomGenerator.getInt(0, 1) == 0 ? Enemy.EnemyType.STEALER : Enemy.EnemyType.ATTACKER;
        Enemy.EnemyType type = vent.getEnemyType() == 0 ? Enemy.EnemyType.STEALER : Enemy.EnemyType.ATTACKER;
        BugType bugType;
        if (type == EnemyType.ATTACKER) {
            bugType = BugType.ATTACKERBUG;

        } else if (type == EnemyType.STEALER) {

            bugType = BugType.STEALERBUG;

        } else {
            System.err.println("enemytype: " + type + " was not BugType AttackerBug or StealerBug");
            bugType = BugType.STEALERBUG;

        }
        BugAnimations anims = AnimationLibrary.bugAnims.get(bugType);
        //System.out.println("Spawning " + bugType + ", anims hashcode: " + System.identityHashCode(anims));


        if (anims == null) {
            System.out.println(bugType + " enemy anim is null");
        }

//        System.out.println("About to create enemy: type=" + type + " bugType=" + bugType
//            + " anims hashcode=" + System.identityHashCode(anims)
//            + " anims.move hashcode=" + System.identityHashCode(anims.move));


        Enemy enemy = new Enemy(units, vent.getObstacle().getX(), vent.getObstacle().getY(), constants.get("enemy"),
            ingredientIdx, v, type, bugType, anims);
        enemy.setAnimations(anims);
        enemy.triggerSpawn();

        Texture warnTexture = directory.getEntry("alert", Texture.class);
        enemy.setWarningTexture(warnTexture);
        // after enemy is constructed
        /*
        System.out.println("Created enemy id=" + System.identityHashCode(enemy)
            + " bugType=" + bugType
            + " anims.move hashcode=" + System.identityHashCode(anims.move));

         */

        addQueuedObject(enemy);
        enemyList.add(enemy);
    }

    public void createBlocker(float xPos) {

        BugAnimations anims = AnimationLibrary.bugAnims.get(BugType.BLOCKERBUG);
        if (anims == null) {
            System.out.println("blockerbug anim is null");
        }

        JsonValue blockerjv = constants.get("blocker");

        Blocker blocker = new Blocker(units, blockerjv, xPos, anims);
        blocker.setAnimations(anims);

        addQueuedObject(blocker);

        blockerList.add(blocker);
    }

    /**
     * Creates static snapshots of the blocker bugs for the simulation world
     */
    public void createBlockerSnapshots() {
        for (ObstacleSprite obs : blockerList) {
            JsonValue settings = constants.get("blocker");
            float width = settings.getFloat("width");
            float height = settings.getFloat("height");

            Obstacle obstacle = new CapsuleObstacle(obs.getObstacle().getX(), obs.getObstacle().getY(), width, height);

            obstacle.setDensity(settings.getFloat("density", 1));
            obstacle.setFriction(settings.getFloat("friction", 0));
            obstacle.setRestitution(settings.getFloat("restitution", 1));

            obstacle.setBodyType(BodyDef.BodyType.KinematicBody);
            obstacle.setPhysicsUnits(units);
            obstacle.getFilterData().groupIndex = -2;

            ObstacleSprite blockerSnapshot = new ObstacleSprite(obstacle);
            addQueuedObjectSim(blockerSnapshot);
            tempSimObjects.add(blockerSnapshot);
        }
    }

    /**
     * Creates static snapshots of the helper bugs for the simulation world
     */
    public void createHelperSnapshots() {
        for (ObstacleSprite obs : helpers) {
            JsonValue settings = constants.get("helper");

            float s = settings.getFloat("size");
//            float width = s * settings.get("inner").getFloat(0);
//            float height = s * settings.get("inner").getFloat(1);
            float radius = s * settings.getFloat("radius");

            Obstacle obstacle = new WheelObstacle(obs.getObstacle().getX(), obs.getObstacle().getY(), radius);

            obstacle.setDensity(settings.getFloat("density", 1));
            obstacle.setFriction(settings.getFloat("friction", 0));
            obstacle.setRestitution(settings.getFloat("restitution", 1));

            // obstacle.setBodyType(BodyDef.BodyType.KinematicBody);
            obstacle.setPhysicsUnits(units);
            obstacle.setFixedRotation(true);
            obstacle.getFilterData().groupIndex = -2;
            obstacle.setAngle(obs.getObstacle().getAngle());
            // obstacle.setSensor(true);
            obstacle.setName("helper snapshot");

            ObstacleSprite helperSnapshot = new ObstacleSprite(obstacle);
            addQueuedObjectSim(helperSnapshot);
            tempSimObjects.add(helperSnapshot);
        }
    }

    /**
     * Removes an entity from the world.
     *
     * @param o the entity to remove
     */
    public void removeEntity(ObstacleSprite o) {
        o.getObstacle().markRemoved(true);
    }

    public void addFromQueue() {
        // Add any objects created by actions
        while (!addQueue.isEmpty()) {
            ObstacleSprite o = addQueue.poll();
            addSprite(o);
            if (o.getName().equals("enemy")) {
                ((Enemy) o).createSensor();
            }
            if (o.getName().equals("blocker")) {
                ((Blocker) o).createSensor();
            }
        }
    }

    public void addFromSimQueue() {
        // Add any objects created by actions
        while (!addQueueSim.isEmpty()) {
            ObstacleSprite sprite = addQueueSim.poll();
            addSpriteSim(sprite);
//            if (sprite.getObstacle().getName() != null && sprite.getObstacle().getName().equals("helper snapshot")) {
//                Obstacle obstacle = sprite.getObstacle();
//                JsonValue settings = constants.get("helper");
//                Vector2 hitboxCenter = Vector2.Zero;
//                FixtureDef hitboxDef = new FixtureDef();
//                hitboxDef.density = obstacle.getDensity();
//                hitboxDef.friction = 0f;
//
//                CircleShape shape = new CircleShape();
//                shape.setPosition(hitboxCenter);
//                float r = settings.getFloat("hitbox radius");
//                shape.setRadius(r);
//                hitboxDef.shape = shape;
//
//                // Fixture to represent hitbox
//                Body body = obstacle.getBody();
//                Fixture hitboxFix = body.createFixture(hitboxDef);
//                hitboxFix.getFilterData().groupIndex = -2;
//            }
        }
    }

    public void removeBoba() {
        for (ObstacleSprite o : bobaList) {
            Boba boba = (Boba) o;
            if (boba.getMark()) {
                bobaList.remove(boba);
                removeEntity(boba);
            }
        }
    }

    public void removeEnemies() {
        for (ObstacleSprite e : enemyList) {
            Enemy enemy = (Enemy) e;
            if (enemy.getMark()) {
                enemyList.remove(enemy);
                removeEntity(enemy);
            }
        }
        for (ObstacleSprite o : blockerList) {
            Blocker blocker = (Blocker) o;
            if (blocker.getMark()) {
                blockerList.remove(blocker);
                removeEntity(blocker);
            }
        }
    }

    // RECIPE / INGREDIENT TRACKING

    /**
     * Removes one ingredient (if there is one available) from its associated crate, and
     * returns whether it was removed
     *
     * @param type the ingredient to remove
     * @return true if it successfully removed an ingredient, false if there were none available
     */
    public boolean takeFromCrate(IngredientType type) {
        int count = available.get(type);
        if (count > 0) {
            available.put(type, count - 1);
            beingHeld.put(type, beingHeld.get(type) + 1);
            return true;
        }
        return false;
    }

    /**
     * Adds an additional ingredient back to the crate (used when enemy bugs holding ingredients
     * are killed.
     *
     * @param type The ingredient type to return.
     */
    public void returnToCrate(IngredientType type) {
        available.put(type, available.get(type) + 1);
        beingHeld.put(type, beingHeld.get(type) - 1);
    }

    /**
     * Removes an ingredient from being held when an enemy bug escapes with it.
     *
     * @param type The ingredient type that was stolen.
     */
    public void ingredientStolen(IngredientType type) {
        beingHeld.put(type, beingHeld.get(type) - 1);
    }

    /**
     * Adds a held ingredient into the cup.
     *
     * @param type The ingredient type to put into the cup.
     */
    public void addToCup(IngredientType type) {
        inCup.put(type, inCup.get(type) + 1);
        beingHeld.put(type, beingHeld.get(type) - 1);
    }

    /**
     * Returns the quantity of a given ingredient currently in the cup.
     *
     * @param type The desired ingredient type.
     * @return The number of this ingredient currently deposited in the cup.
     */
    public int getCupCount(IngredientType type) {
        return inCup.get(type);
    }

    /**
     * Returns the required quantity of a certain ingredient for this level's recipe.
     *
     * @param type The desired ingredient type.
     * @return The required quantity of this ingredient needed for this level's recipe.
     */
    public int getRequired(IngredientType type) {
        return recipe.get(type);
    }

    /**
     * Returns the quantity of a given ingredient currently available in this level.
     *
     * @param type The desired ingredient type.
     * @return The amount of this recipe still available in crates.
     */
    public int getAvailable(IngredientType type) {
        return available.get(type);
    }

    /**
     * Returns whether the recipe has been completed.
     *
     * @return true if for each required ingredient, inCup[type] >= recipe[type]
     */
    public boolean isRecipeComplete() {
        for (IngredientType type : recipe.keySet()) {
            if (inCup.get(type) < recipe.get(type)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether the recipe is impossible to complete with current available ingredients
     *
     * @return true if for any ingredient, the amounts of these ingredients in the cup, held by bugs,
     * and available in crates is less than the amount needed for this level's recipe. Returns false
     * otherwise.
     */
    public boolean isRecipeImpossible() {
        for (IngredientType type : recipe.keySet()) {
            int possible = inCup.get(type) + beingHeld.get(type) + available.get(type);
            if (possible < recipe.get(type)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Adds one boba directly into the cup.
     */
    public void bobaInCup() {
        inCup.put(IngredientType.BOBA, inCup.get(IngredientType.BOBA) + 1);
    }

    private void setIngredients() {
        // ingredient stuff set here
        recipe = new EnumMap<>(IngredientType.class);
        available = new EnumMap<>(IngredientType.class);
        JsonValue levelProperties = levelData.get("properties");
        int val_index = 0;
        for (JsonValue val : levelProperties) { // i am sorry for this abomination - jason
            //its ok jason - talia

            switch (val.getString("name")) {
                case "boba_required":
                    recipe.put(IngredientType.BOBA, val.getInt("value"));
                    break;
                case "jelly_avail":
                    available.put(IngredientType.JELLY, val.getInt("value"));
                    break;
                case "jelly_required":
                    recipe.put(IngredientType.JELLY, val.getInt("value"));
                    break;
                case "milk_avail":
                    available.put(IngredientType.MILK, val.getInt("value"));
                    break;
                case "milk_required":
                    recipe.put(IngredientType.MILK, val.getInt("value"));
                    break;
                case "time_1":
                    timeLimit1 = val.getInt("value");
                    break;
                case "time_2":
                    timeLimit2 = val.getInt("value");
                    break;
                case "stun_num":
                    maxStuns = val.getInt("value");
                    break;
                case "stealer_target":
                    closestCrateMode = "closest".equals(val.getString("value", "random"));
                    break;
            }
            available.put(IngredientType.BOBA, 99999); // TODO: find a more permanent solution to this

            /**
             if (val.getString("name").equals("available")) {
             // available ingredients set here
             JsonValue nums = val.get("value");
             int ingredient_index = 0;
             for (JsonValue num : nums) {
             if (ingredient_index == 0) {
             available.put(IngredientType.MILK, num.getInt("value"));
             } else if (ingredient_index == 1) {
             available.put(IngredientType.JELLY, num.getInt("value"));
             } else if (ingredient_index == 2) {
             available.put(IngredientType.BOBA, num.getInt("value"));
             }
             available.put(IngredientType.BOBA, 99999);
             ingredient_index++;
             }
             } else if (val.getString("name").equals("recipe")) {
             // recipe requirements set here
             JsonValue nums = val.get("value");
             int ingredient_index = 0;
             for (JsonValue num : nums) {
             if (ingredient_index == 0) {
             recipe.put(IngredientType.MILK, num.getInt("value"));
             } else if (ingredient_index == 1) {
             recipe.put(IngredientType.JELLY, num.getInt("value"));
             } else if (ingredient_index == 2) {
             recipe.put(IngredientType.BOBA, num.getInt("value"));
             }
             ingredient_index++;
             }
             }

             val_index++;
             */
        }
    }

    /**
     * Spawns a decoration with the given texture asset name. Decorations have
     * the same physics as empty crates (static, in the crates collision group),
     * so pathfinding and bugs treat them like real obstacles automatically.
     *
     * @param x world x position
     * @param y world y position
     * @param textureName asset key in directory (e.g. "stool", "plant1", "woodcrate")
     * @param decorationConsts the "decoration" JSON config block from constants.json
     */
    private void spawnDecoration(float x, float y, String textureName,
                                 JsonValue decorationConsts) {
        if (decorationConsts == null) {
            System.err.println("WARNING: 'decoration' not found in constants.json; "
                + "skipping decoration '" + textureName + "'");
            return;
        }
        Texture tex = directory.getEntry(textureName, Texture.class);
        if (tex == null) {
            System.err.println("WARNING: decoration texture '" + textureName
                + "' not found in asset directory; skipping");
            return;
        }

        DecorationObstacle dec = new DecorationObstacle(x, y, units, decorationConsts);
        dec.setTexture(tex);
        addSprite(dec);

        // Also add to the simulation world so trajectory previews collide correctly.
        DecorationObstacle decSim = new DecorationObstacle(x, y, units, decorationConsts);
        addSpriteSim(decSim);

        decorationList.add(dec);
        dec.createSensor();
    }

    private void setObstacles(float width, float height) {
        float yOffset = 0f;
        JsonValue decorationConsts = constants.get("decoration");
        JsonValue crateConsts = constants.get("crates");
        JsonValue obstacleConsts = constants.get("levelobstacle");
        JsonValue cobwebConsts = constants.get("cobweb");
        JsonValue honeyConsts = constants.get("honey");
        JsonValue layers = levelData.get("layers");
        if (layers == null) return;

        // Look up layers by name rather than by index. This makes the loader
        // robust to adding new object layers (e.g. Portals) or reordering
        // them in Tiled, which would otherwise shift the index assignments
        // and cause Vents to be parsed as Helpers and so on.
        JsonValue cratesLayer  = null;
        JsonValue ventsLayer   = null;
        JsonValue helpersLayer = null;
        for (JsonValue layer : layers) {
            String name = layer.getString("name", "");
            if (name.equals("Crates"))       cratesLayer  = layer;
            else if (name.equals("Vents"))   ventsLayer   = layer;
            else if (name.equals("Helpers")) helpersLayer = layer;
        }

        // Crates tilelayer
        if (cratesLayer != null) {
            int crateIndex = 0;
            int[] data = cratesLayer.get("data").asIntArray();
            for (int i = 0; i < data.length; i++) {
                int tileIndex = data[i];
                int x = (i % 16) * 2 + 1;
                float y = 18 - ((i / 16) * (18f / 10)) - (18f / 10 / 2) + 0.1f;
                if (y > 18) {  // don't place anything in the top row
                    continue;
                }
                if (tileIndex == 17 || tileIndex == 25 || tileIndex == 33 || tileIndex == 41 || tileIndex == 49) {
                    Texture obsttexture = directory.getEntry("crate_plain", Texture.class);
                    LevelObstacle obj = new LevelObstacle(x, y + yOffset, units, obstacleConsts);
                    obj.getObstacle().setName("obstacle");
                    obj.setTexture(obsttexture);
                    addSprite(obj);

                    LevelObstacle obj2 = new LevelObstacle(x, y + yOffset, units, obstacleConsts);
                    obj2.getObstacle().setName("obstacle");
                    addSpriteSim(obj2);

                    obj.createSensor();
                } else if (tileIndex == 18 || tileIndex == 26 || tileIndex == 34 || tileIndex == 42 || tileIndex == 50) {
                    Texture obsttexture = directory.getEntry("crate_empty", Texture.class);
                    LevelObstacle obj = new LevelObstacle(x, y + yOffset, units, obstacleConsts);
                    obj.getObstacle().setName("obstacle");
                    obj.setTexture(obsttexture);
                    addSprite(obj);

                    LevelObstacle obj2 = new LevelObstacle(x, y + yOffset, units, obstacleConsts);
                    obj2.getObstacle().setName("obstacle");
                    addSpriteSim(obj2);

                    obj.createSensor();
                } else if (tileIndex == 19 || tileIndex == 27 || tileIndex == 35 || tileIndex == 43 || tileIndex == 51) {
                    Texture texture = directory.getEntry("jelly_crate_full", Texture.class);
                    IngredientType type = IngredientType.JELLY;
                    Crate crate = new Crate(x, y + yOffset,
                        units, crateConsts, type, crateIndex);
                    crate.getObstacle().setName("crate");

                    Crate crate2 = new Crate(x, y + yOffset,
                        units, crateConsts, type, crateIndex);
                    crate2.getObstacle().setName("crate");
                    addSpriteSim(crate2);

                    crate.setTexture(texture);
                    addSprite(crate);
                    crateList.add(crate);
                    crateIndex++;
                    crate.createSensor();
                    crate.createHitbox();
                } else if (tileIndex == 20 || tileIndex == 28 || tileIndex == 36 || tileIndex == 44 || tileIndex == 52) {
                    Texture texture = directory.getEntry("milk_crate_full", Texture.class);
                    IngredientType type = IngredientType.MILK;
                    Crate crate = new Crate(x, y + yOffset,
                        units, crateConsts, type, crateIndex);
                    crate.getObstacle().setName("crate");

                    Crate crate2 = new Crate(x, y + yOffset,
                        units, crateConsts, type, crateIndex);
                    crate2.getObstacle().setName("crate");
                    addSpriteSim(crate2);

                    crate.setTexture(texture);
                    addSprite(crate);
                    crateList.add(crate);
                    crateIndex++;
                    crate.createSensor();
                    crate.createHitbox();
                } else if (tileIndex == 23) {
                    // Cobweb tiles
                    Texture cobwebTexture = directory.getEntry("cobweb", Texture.class);

                    if (cobwebConsts == null) {
                        System.err.println("WARNING: 'cobweb' not found in constants.json");
                        continue;
                    }

                    int cobwebIndex = cobwebList.size();
                    Cobweb cobweb = new Cobweb(x, y + yOffset, units, cobwebConsts, cobwebIndex);
                    cobweb.getObstacle().setName("cobweb");
                    cobweb.setTexture(cobwebTexture);
                    addSprite(cobweb);

                    // createSensor() creates the cobweb_hitbox fixture that CollisionController
                    // uses to fire setCollidingCrate(true) on helpers, making them path around
                    // cobwebs the same way they path around empty crates.
                    cobwebList.add(cobweb);
                    cobweb.createSensor();
                } else if (tileIndex == 24) {
                    // Honey tiles
                    Texture honeyTexture = directory.getEntry("honey", Texture.class);

                    if (honeyConsts == null) {
                        System.err.println("Honey not found in constants.json");
                        continue;
                    }

                    int honeyIndex = honeyList.size();
                    Honey honey = new Honey(x, y + yOffset, units, honeyConsts, honeyIndex);
                    honey.getObstacle().setName("honey");
                    honey.setTexture(honeyTexture);
                    addSprite(honey);
                    // honey.createSensor();

                    // Add to simulation world
//                        Honey honey2 = new Honey(x, y + yOffset, units, honeyConsts, honeyIndex);
//                        honey2.getObstacle().setName("honey");
//                        addSpriteSim(honey2);
//
//                        honeyList.add(honey);
//                        honey.createSensor();
                }
                else if (tileIndex == 101 || tileIndex == 29 || tileIndex == 37 || tileIndex == 45 || tileIndex == 53) {
                    spawnDecoration(x, y + yOffset, "stool", decorationConsts);
                } else if (tileIndex == 102 || tileIndex == 30 || tileIndex == 38 || tileIndex == 46 || tileIndex == 54) {
                    spawnDecoration(x, y + yOffset, "woodcrate", decorationConsts);
                } else if (tileIndex == 117 || tileIndex == 63 || tileIndex == 71 || tileIndex == 79 || tileIndex == 87) {
                    spawnDecoration(x, y + yOffset, "plant1", decorationConsts);
                } else if (tileIndex == 118 || tileIndex == 64 || tileIndex == 72 || tileIndex == 80 || tileIndex == 88) {
                    spawnDecoration(x, y + yOffset, "plant2", decorationConsts);
                } else if (tileIndex == 119 || tileIndex == 65 || tileIndex == 73 || tileIndex == 81 || tileIndex == 89) {
                    spawnDecoration(x, y + yOffset, "plant3", decorationConsts);
                }
            }
            Texture texture = directory.getEntry("cup", Texture.class);
            JsonValue cupData = constants.get("cup");
            float cupSize = constants.get("cup").getFloat("size");
            float cupX = width - cupSize + 2;
            float cupY = height / 2f;
            cup = new Cup(cupX, cupY, units, cupData);
            cup.getObstacle().setPosition(cupX, cupY);
            cup.setTexture(texture);
            addSprite(cup);

            Cup cup2 = new Cup(cupX, cupY, units, cupData);
            cup2.getObstacle().setPosition(cupX, cupY);
            addSpriteSim(cup2);

            // Create graphs AFTER all static obstacles are created (one for each crate, and one without any crates)
            worldGraphs.add(new WorldGraph(constants.get("world"), world, width, height));
            for (ObstacleSprite o : crateList) {
                worldGraphs.add(new WorldGraph(constants.get("world"), world, width, height, (Crate) o, false));
                // worldGraphs.add(new WorldGraph(constants.get("world"), world, bounds.width, bounds.height, (Crate) o, true));
            }
            worldGraphs.add(new WorldGraph(constants.get("world"), world, width, height, (Cup) cup));
        }

        // Vents object layer
        if (ventsLayer != null) {
            JsonValue objects = ventsLayer.get("objects");
            JsonValue ventConsts = constants.get("vents");
            int ventIdx = 0;
            for (JsonValue obj : objects) {
                float x = obj.getFloat("x") / 600 * 2 + 1;
                float y = 18 - ((obj.getFloat("y") / 520 - 1) * (1.8f)) - 1;
                int bugType = 0;
                int firstSpawn = 0;
                int spawnDelay = 0;
                int propertyIndex = 0;
                JsonValue properties = obj.get("properties");
                for (JsonValue prop : properties) {
                    if (prop.getString("name").equals("bug_type")) {
                        if (prop.getString("value").equals("stealer")) {
                            bugType = 0;
                        } else if (prop.getString("value").equals("attacker")) {
                            bugType = 1;
                        } else if (prop.getString("value").equals("blocker")) {
                            bugType = 2;
                        } else { // default to stealer
                            bugType = 0;
                        }
                    } else if (prop.getString("name").equals("first_spawn")) {
                        firstSpawn = prop.getInt("value");
                    } else if (prop.getString("name").equals("spawn_delay")) {
                        spawnDelay = prop.getInt("value");
                    }
                }
                SpawnerVent vent = new SpawnerVent(x, y, 0, ventIdx, units, ventConsts,
                    bugType, firstSpawn, spawnDelay);
                vent.getObstacle().setName("vent");
                // TODO: use different textures for different bug spawns
                Texture texture = directory.getEntry("vent", Texture.class);
                vent.setTexture(texture);
                Texture alertTexture = directory.getEntry("alert", Texture.class);
                vent.setAlertTexture(alertTexture);
                addSprite(vent);
                ventList.add(vent);
                ventIdx++;
            }
        }

        // Helpers object layer
        if (helpersLayer != null) {
            JsonValue objects = helpersLayer.get("objects");
            JsonValue helperConsts = constants.get("helper");
            Texture helperTexture = directory.getEntry("helper", Texture.class);
            //Texture helperOutlineTexture = directory.getEntry("helper outline", Texture.class);
            int helperIdx = 0;
            for (JsonValue obj : objects) {
                float x = obj.getFloat("x") / 600 * 2 + 1;
                float y = 18 - ((obj.getFloat("y") / 520 - 1) * (1.8f)) - 1;
                BugType helperColor = (helperIdx == 0) ? BugType.HELPER_ORANGE : BugType.HELPER_BLUE;
                Helper helper = new Helper(
                    x,
                    y,
                    units,
                    helperConsts,
                    0,
                    helperColor
                );
                //helper.setTexture(helperTexture);
                BugAnimations anims = AnimationLibrary.bugAnims.get(helper.getType());
                helper.setAnimations(anims);
                addSprite(helper);
                helper.createHitbox();
                helpers.add(helper);
                helperIdx++;
            }
        }
    }

}

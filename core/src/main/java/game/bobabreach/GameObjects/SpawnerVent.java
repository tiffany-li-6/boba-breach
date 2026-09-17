package game.bobabreach.GameObjects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

/**
 * An object representing a bug spawner vent, spawning enemies at specified times.
 */
public class SpawnerVent extends ObstacleSprite {
    /** Physics units we're using, to convert back to screen coordinates */
    private float units;
    /** Width and height of the crate in Box2D units */
    private float size;
    /** Index of this vent */
    private int index;

    /** Target Crate for enemies spawned from this vent */
    private int targetCrate;

    /** Number representing type of enemy to spawn */
    private int enemyType;

    /** How many seconds between enemy spawns for this vent */
    private int spawnDelay;

    /** How many seconds before first enemy spawns from this vent */
    private int firstSpawn;

    /** When this vent should next spawn an enemy */
    private int nextSpawn;

    private JsonValue data;

    /**
     * stores Alert symbol object for this vent
     */
    private Alert alert;

    private float lastAlertedSpawn = -1f;



    /**
     * Creates a spawner vent with the given physics units and settings
     *
     * The physics units are used to size the mesh relative to the physics
     * body. All other attributes are defined by the JSON file
     *
     * @param units     The physics units
     * @param settings  The vent physics constants
     */
    public SpawnerVent(float x, float y, int targetCrate, int index, float units, JsonValue settings,
                       int enemyType, int firstSpawn, int spawnDelay){
        super();

        data = settings;
        float s = settings.getFloat("size");
        float size = s * units;
        this.units = units;
        this.size = s;
        this.targetCrate = targetCrate; // not currently used
        this.index = index;
        this.enemyType = enemyType;
        this.firstSpawn = firstSpawn;
        nextSpawn = firstSpawn;
        this.spawnDelay = spawnDelay;


        obstacle = new BoxObstacle(x,y,s,s);
        obstacle.setBodyType(BodyDef.BodyType.StaticBody);
        obstacle.setSensor(true);
        obstacle.setDensity( settings.getFloat( "density", 0 ) );
        obstacle.setFriction( settings.getFloat( "friction", 0 ) );
        obstacle.setRestitution( settings.getFloat( "restitution", 0 ) );
        obstacle.setPhysicsUnits( units );
        obstacle.setUserData( this );
        obstacle.setName("vent");
        obstacle.getFilterData().categoryBits = 0x0010;
        obstacle.getFilterData().maskBits = (short) (0xFFFF & ~0x0008);

        debug = ParserUtils.parseColor(settings.get("debug"), Color.WHITE);

        // Create a rectangular mesh the same size as the vent, adjusted by
        // the physics units. For all meshes attached to a physics body, we
        // want (0,0) to be in the center of the mesh. So the method call below
        // is (x,y,w,h) where x, y is the bottom left.
        mesh.set(-size/2.0f,-size/2.0f,size,size);

        alert = new Alert(x, y, s, units, null, 3, 0f, 0.5f);
        // spawn alert texture is not set here, it isset later by setWarningTexture()

    }

    public void createVentHitbox() {
        Vector2 sensorCenter = new Vector2(0, 0);
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = 1;
        sensorDef.isSensor = true;

        JsonValue sensorjv = data.get("sensor");
        float w = sensorjv.getFloat("xFactor", 0) * size;
        float h = sensorjv.getFloat("yFactor", 0) * size;
        PolygonShape sensorShape = new PolygonShape();
        sensorShape.setAsBox(w, h, sensorCenter, 0.0f);
        sensorDef.shape = sensorShape;

        // Sensor to represent hitbox
        Body body = obstacle.getBody();
        Fixture sensorFixture = body.createFixture(sensorDef);
        String sensorName = "vent_hitbox";
        sensorFixture.setUserData(sensorName);

        // Finally, we need a debug outline
        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
        Path2 sensorOutline = new Path2();
        factory.makeRect((sensorCenter.x - w / 2) * u, (sensorCenter.y - h / 2) * u, w * u, h * u, sensorOutline);
    }

    /**
     * Get index of vent
     * @return index int of vent
     */
    public int getIndex() {
        return index;
    }

    /**
     * Whether this vent should spawn an enemy this frame
     * @param currentTime current time in seconds
     * @return whether enemy should spawn from this
     */
    public boolean shouldSpawn(float currentTime) {
        if (currentTime >= nextSpawn) {
            nextSpawn += spawnDelay;
            lastAlertedSpawn = -1f;
            alert.reset();
            return true;
        }
        return false;
    }

    /**
     * Returns an index representing the enemy type this vent spawns.
     * @return 0 for stealers, 1 for attackers, 2 for blockers
     */
    public int getEnemyType() {
        return enemyType;
    }


    /**
     * Gets the X position of the vent in screen coordinates.
     * MAY NEED FOR AI CONTROLLER and enemy spawning
     * @return X position of the vent in screen coordinates
     */
    public float getXPos() {
        return obstacle.getX() * units;
    }

    /**
     * Gets the Y position of the vent in screen coordinates.
     * MAY NEED FOR AI CONTROLLER and enemy spawning
     * @return Y position of the vent in screen coordinates
     */
    public float getYPos() {
        return obstacle.getY() * units;
    }

    /**
     * Gets the width and height of the vent in Box2D units.
     * Used for pathfinding
     * @return size of the vent
     */
    public float getSize() {
        return size;
    }

    /**
     * Gets the target crate for enemies from this vent
     * needed for enemy spawning
     * @return target crate of the vent
     */
    public int getTargetCrate() {
        return targetCrate;
    }

    public Alert getAlert() {
        return alert;
    }

    public void setAlertTexture(Texture texture) {
        alert.setTexture(texture);
    }

    /**
     * Whether this vent is within the warning window before its next spawn.
     * @param currentTime current time in seconds
     */
    public boolean shouldAlert(float currentTime) {
        float alertStart = nextSpawn - alert.getWarningLead();
        return currentTime >= alertStart
            && currentTime < nextSpawn
            && lastAlertedSpawn != nextSpawn;
    }

    public void markWarned() {
        lastAlertedSpawn = nextSpawn;
    }
}

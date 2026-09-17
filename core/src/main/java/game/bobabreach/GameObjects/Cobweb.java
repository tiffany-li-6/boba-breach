package game.bobabreach.GameObjects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

/**
 * A static cobweb obstacle that destroys boba on contact.
 *
 * Cobwebs act as impassable obstacles for all entities but have a special
 * interaction with boba: they destroy boba on collision instead of bouncing it.
 * Other moving entities (helpers, enemies, blockers) treat cobwebs as solid obstacles
 * and cannot pass through them.
 */
public class Cobweb extends ObstacleSprite {

    /** Physics units we're using, to convert back to screen coordinates */
    private float units;

    /** Width and height of the cobweb in Box2D units */
    private float size;

    /** Index of the cobweb (for level tracking) */
    private int index;

    private JsonValue data;


    /**
     * Creates a cobweb with the given physics units and settings
     *
     * The physics units are used to size the mesh relative to the physics
     * body. All other attributes are defined by the JSON file
     *
     * @param x         X position in Box2D units
     * @param y         Y position in Box2D units
     * @param units     The physics units (screenview / box2d)
     * @param settings  The cobweb physics constants
     * @param index     Index for tracking this cobweb
     */
    public Cobweb(float x, float y, float units, JsonValue settings, int index) {
        super();

        float s = settings.getFloat("size");
        float size = s * units;
        this.units = units;
        this.size = s;
        this.index = index;
        data = settings;

        // Create a static box obstacle for the cobweb
        obstacle = new BoxObstacle(x, y, s, s*1.1f); // x-pos, y-pos of box center, width, height all in box2d units
        obstacle.getFilterData().groupIndex = -3; // Same collision group as crates to block movement
        obstacle.setBodyType(BodyDef.BodyType.StaticBody);
        obstacle.setDensity(settings.getFloat("density", 0));
        obstacle.setFriction(settings.getFloat("friction", 0));
        obstacle.setRestitution(settings.getFloat("restitution", 0));
        obstacle.setPhysicsUnits(units);
        obstacle.setUserData(this);
        obstacle.setName("cobweb");
        obstacle.getFilterData().maskBits = (short) (0xFFFF & ~0x0006);

        debug = ParserUtils.parseColor(settings.get("debug"), Color.WHITE);

        // Create a rectangular mesh for rendering
        float meshScale = settings.getFloat("img scale", 1.0f);
        float meshSize = size * meshScale;
        mesh.set(-meshSize / 2.0f, -meshSize / 2.0f, meshSize, meshSize);
    }

    /**
     * Creates the sensor/hitbox for the cobweb.
     * This allows collision detection with boba and other entities.
     */
    public void createSensor() {
        Vector2 sensorCenter = new Vector2(0, 0);
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = 1;
        sensorDef.isSensor = false; // Not a sensor - we want physical collisions for blocking

        // Use the full size of the cobweb as the hitbox
        float w = size / 2.0f;
        float h = size / 2.0f;

        PolygonShape sensorShape = new PolygonShape();
        sensorShape.setAsBox(w, h, sensorCenter, 0.0f);
        sensorDef.shape = sensorShape;

        // Create the main collision fixture
        Body body = obstacle.getBody();
        Fixture sensorFixture = body.createFixture(sensorDef);
        String sensorName = "cobweb_hitbox";
        sensorFixture.setUserData(sensorName);

        // Debug outline
        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
        Path2 sensorOutline = new Path2();
        factory.makeRect((sensorCenter.x - w / 2) * u, (sensorCenter.y - h / 2) * u, w * u, h * u, sensorOutline);
    }

    /**
     * Returns the index of this cobweb.
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets the X position of the cobweb in Box2D units.
     * @return X position in Box2D units
     */
    public float getX() {
        return obstacle.getX();
    }

    /**
     * Gets the Y position of the cobweb in Box2D units.
     * @return Y position in Box2D units
     */
    public float getY() {
        return obstacle.getY();
    }

    /**
     * Gets the X position of the cobweb in screen coordinates.
     * @return X position of the cobweb in screen coordinates
     */
    public float getXPos() {
        return obstacle.getX() * units;
    }

    /**
     * Gets the Y position of the cobweb in screen coordinates.
     * @return Y position of the cobweb in screen coordinates
     */
    public float getYPos() {
        return obstacle.getY() * units;
    }

    /**
     * Gets the size of the cobweb in Box2D units.
     * @return size of the cobweb
     */
    public float getSize() {
        return size;
    }
}

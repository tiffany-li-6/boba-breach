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
 * A honey splash on the floor that slows down enemy bugs.
 *
 * Honey acts as a sensor (non-solid) obstacle that enemies can walk through,
 * but when they're on it, their movement speed is reduced by a slowdown factor.
 * Helpers and boba are not affected by honey.
 */
public class Honey extends ObstacleSprite {

    /** Physics units to convert back to screen coordinates */
    private float units;

    /** Width and height of the honey splash in Box2D units */
    private float size;

    /** Index of the honey (for level tracking) */
    private int index;

    /** Speed multiplier for enemies on this honey */
    private float slowdownFactor;

    private JsonValue data;


    /**
     * Creates a honey splash with the given physics units and settings
     *
     * The physics units are used to size the mesh relative to the physics
     * body. All other attributes are defined by the JSON file
     *
     * @param x         X position in Box2D units
     * @param y         Y position in Box2D units
     * @param units     The physics units (screenview / box2d)
     * @param settings  The honey physics constants
     * @param index     Index for tracking this honey
     */
    public Honey(float x, float y, float units, JsonValue settings, int index) {
        super();

        float s = settings.getFloat("size");
        float size = s * units;
        this.units = units;
        this.size = s;
        this.index = index;
        this.slowdownFactor = settings.getFloat("slowdown_factor", 0.5f); // Default to 50% speed
        data = settings;

        // Create a static box obstacle for the honey
        obstacle = new BoxObstacle(x, y, s, s); // x-pos, y-pos of box center, width, height all in box2d units
        obstacle.getFilterData().groupIndex = -4; // Different collision group from crates/cobwebs
        obstacle.setBodyType(BodyDef.BodyType.StaticBody);
        obstacle.setDensity(settings.getFloat("density", 0));
        obstacle.setFriction(settings.getFloat("friction", 0));
        obstacle.setRestitution(settings.getFloat("restitution", 0));
        obstacle.setPhysicsUnits(units);
        obstacle.setUserData(this);
        obstacle.setSensor(true);
        obstacle.setName("honey");
        obstacle.getFilterData().categoryBits = 0x0003;
        obstacle.getFilterData().maskBits = (short) (0xFFFF & ~0x0008);

        debug = ParserUtils.parseColor(settings.get("debug"), Color.YELLOW);

        // Create a rectangular mesh for rendering
        float meshScale = settings.getFloat("img scale", 1.0f);
        float meshSize = size * meshScale;
        mesh.set(-meshSize / 2.0f, -meshSize / 2.0f, meshSize, meshSize);
    }

    /**
     * Creates the sensor/hitbox for the honey.
     * Honey is a sensor, so enemies can walk through it while being slowed.
     */
    public void createSensor() {
        Vector2 sensorCenter = new Vector2(0, 0);
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = 1;
        sensorDef.isSensor = true;

        // Use the full size of the honey splash as the hitbox
        float w = size / 2.0f;
        float h = size / 2.0f;

        PolygonShape sensorShape = new PolygonShape();
        sensorShape.setAsBox(w, h, sensorCenter, 0.0f);
        sensorDef.shape = sensorShape;

        // Create the sensor fixture
        Body body = obstacle.getBody();
        Fixture sensorFixture = body.createFixture(sensorDef);
        String sensorName = "honey_hitbox";
        sensorFixture.setUserData(sensorName);

        // Debug outline
        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
        Path2 sensorOutline = new Path2();
        factory.makeRect((sensorCenter.x - w / 2) * u, (sensorCenter.y - h / 2) * u, w * u, h * u, sensorOutline);
    }

    /**
     * Returns the index of this honey.
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets the X position of the honey in Box2D units.
     * @return X position in Box2D units
     */
    public float getX() {
        return obstacle.getX();
    }

    /**
     * Gets the Y position of the honey in Box2D units.
     * @return Y position in Box2D units
     */
    public float getY() {
        return obstacle.getY();
    }

    /**
     * Gets the X position of the honey in screen coordinates.
     * @return X position of the honey in screen coordinates
     */
    public float getXPos() {
        return obstacle.getX() * units;
    }

    /**
     * Gets the Y position of the honey in screen coordinates.
     * @return Y position of the honey in screen coordinates
     */
    public float getYPos() {
        return obstacle.getY() * units;
    }

    /**
     * Gets the size of the honey splash in Box2D units.
     * @return size of the honey
     */
    public float getSize() {
        return size;
    }

    /**
     * Gets the slowdown factor for this honey.
     * This is a multiplier applied to enemy speed (0.0 = fully stopped, 1.0 = no slowdown)
     * @return the slowdown factor
     */
    public float getSlowdownFactor() {
        return slowdownFactor;
    }
}

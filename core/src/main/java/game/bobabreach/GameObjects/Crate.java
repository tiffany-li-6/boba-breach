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
import game.bobabreach.GameObjects.IngredientType;

/**
 * A rectangular obstacle. Taken from Lab 4.
 *
 * This class represents an ingredient crate. */
public class Crate extends ObstacleSprite {

    /** Physics units we're using, to convert back to screen coordinates */
    private float units;


    /** Ingredient that this crate holds */
    private IngredientType ingredient;

    /** Width and height of the crate in Box2D units */
    private float size;

    /** Index of the crate */
    private int index;

    private JsonValue data;


    /**
     * Creates a box with the given physics units and settings
     *
     * The physics units are used to size the mesh relative to the physics
     * body. All other attributes are defined by the JSON file
     *
     * @param units     The physics units (screenview / box2d)
     * @param settings  The crate physics constants
     */
    public Crate(float x, float y, float units, JsonValue settings, IngredientType ingredientType, int index) {
        super();

        float s = settings.getFloat( "size" );
        float size = s*units;
        this.units = units;
        this.ingredient = ingredientType;
        this.size = s;
        this.index = index;
        data = settings;

        obstacle = new BoxObstacle(x, y, s*1.15f, s*1.15f); //x-pos, y-pos of box center, width, height all in box2d units
        obstacle.getFilterData().groupIndex = -3;
        obstacle.getFilterData().categoryBits = 0x0002;
        obstacle.setBodyType(BodyDef.BodyType.StaticBody);
        obstacle.setDensity( settings.getFloat( "density", 0 ) );
        obstacle.setFriction( settings.getFloat( "friction", 0 ) );
        obstacle.setRestitution( settings.getFloat( "restitution", 0 ) );
        obstacle.setPhysicsUnits( units );
        obstacle.setUserData( this );
        obstacle.setName("crate");
        obstacle.getFilterData().maskBits = (short) (0xFFFF & ~0x0006);

        debug = ParserUtils.parseColor( settings.get("debug"),  Color.WHITE);

        // Create a rectangular mesh the same size as the door, adjusted by
        // the physics units. For all meshes attached to a physics body, we
        // want (0,0) to be in the center of the mesh. So the method call below
        // is (x,y,w,h) where x, y is the bottom left.
        float meshScale = settings.getFloat("img scale");
        float meshSize = size * meshScale;
        mesh.set(-meshSize/2.0f,-meshSize/2.0f, meshSize,meshSize*1.75f);
    }

    /**
     * Creates the sensor for the crate.
     */
    public void createSensor() {
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
        String sensorName = "crate_hitbox";
        sensorFixture.setUserData(sensorName);

        // Finally, we need a debug outline
        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
        Path2 sensorOutline = new Path2();
        factory.makeRect((sensorCenter.x - w / 2) * u, (sensorCenter.y - h / 2) * u, w * u, h * u, sensorOutline);
    }

    /**
     * Creates the second hitbox for the crate.
     */
    public void createHitbox() {
        Vector2 sensorCenter = new Vector2(0, 0);
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = 1;
        sensorDef.isSensor = false;

        float r = data.getFloat("radius", 0) * size;
        CircleShape shape = new CircleShape();
        shape.setPosition(sensorCenter);
        shape.setRadius(r);
        sensorDef.shape = shape;

        Body body = obstacle.getBody();
        Fixture fix = body.createFixture(sensorDef);
        String name = "circle_hitbox";
        fix.setUserData(name);

        // Finally, we need a debug outline
        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
        Path2 sensorOutline = new Path2();
        factory.makeCircle((sensorCenter.x - r / 2) * u, (sensorCenter.y - r / 2) * u, r,  sensorOutline);
    }

    /** Returns the index of this crate. */
    public int getIndex() {
        return index;
    }

    /**
     * Gets the X position of the crate in screen coordinates.
     * MAY NEED FOR AI CONTROLLER
     * @return X position of the crate in screen coordinates
     */
    public float getXPos() {
        return obstacle.getX() * units;
    }

    /**
     * Gets the Y position of the crate in screen coordinates.
     * @return Y position of the crate in screen coordinates
     */
    public float getYPos() {
        return obstacle.getY() * units;
    }

    /**
     * Returns the ingredient that this crate holds.
     * @Return the IngredientType corresponding to this crate's ingredient
     */
    public IngredientType getIngredient(){ return this.ingredient; }

    /**
     * Gets the width and height of the crate in Box2D units.
     * Used for pathfinding
     * @return size of the crate
     */
    public float getSize() { return mesh.computeBounds().getWidth(); }
}

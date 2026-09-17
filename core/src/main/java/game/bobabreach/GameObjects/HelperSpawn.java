package game.bobabreach.GameObjects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

/**
 * An object representing a bug spawner vent, spawning enemies at specified times.
 */
public class HelperSpawn extends ObstacleSprite {
    /** Physics units we're using, to convert back to screen coordinates */
    private float units;
    /** Width and height of the crate in Box2D units */
    private float size;


    /**
     * Creates a spawner vent with the given physics units and settings
     *
     * The physics units are used to size the mesh relative to the physics
     * body. All other attributes are defined by the JSON file
     *
     * @param units     The physics units
     * @param settings  The vent physics constants
     */
    public HelperSpawn(float x, float y, float units, JsonValue settings){
        super();

        float s = settings.getFloat("size");
        float size = s * units;
        this.units = units;
        this.size = s;

        obstacle = new BoxObstacle(x,y,s,s);
        obstacle.setBodyType(BodyDef.BodyType.StaticBody);
        obstacle.setSensor(true);
        obstacle.setDensity( settings.getFloat( "density", 0 ) );
        obstacle.setFriction( settings.getFloat( "friction", 0 ) );
        obstacle.setRestitution( settings.getFloat( "restitution", 0 ) );
        obstacle.setPhysicsUnits( units );
        obstacle.setUserData( this );
        obstacle.setName("station");

        debug = ParserUtils.parseColor(settings.get("debug"), Color.WHITE);

        // Create a rectangular mesh the same size as the vent, adjusted by
        // the physics units. For all meshes attached to a physics body, we
        // want (0,0) to be in the center of the mesh. So the method call below
        // is (x,y,w,h) where x, y is the bottom left.
        mesh.set(-size/2.0f,-size/2.0f,size,size);
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

}

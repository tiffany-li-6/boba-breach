package game.bobabreach.GameObjects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

/**
 * A rectangular cup object, acting as a drop-off location for
 * any of the Player's held ingredients.
 */
public class Cup extends ObstacleSprite {

    /** Physics units we're using, to convert back to screen coordinates */
    private float units;

    /**
     * Creates a box with the given physics units and settings
     *
     * The physics units are used to size the mesh relative to the physics
     * body. All other attributes are defined by the JSON file
     *
     * @param units     The physics units
     * @param settings  The door physics constants
     */
    public Cup(float x, float y, float units, JsonValue settings){
        super();

        float s = settings.getFloat("size");
        float size = s * units;
        this.units = units;

//        float x = levelData.getFloat("x");
//        float y = levelData.getFloat("y");

        obstacle = new BoxObstacle(x,y,s,s);
        obstacle.setBodyType(BodyDef.BodyType.StaticBody);
        obstacle.setDensity( settings.getFloat( "density", 0 ) );
        obstacle.setFriction( settings.getFloat( "friction", 0 ) );
        obstacle.setRestitution( settings.getFloat( "restitution", 0 ) );
        obstacle.setPhysicsUnits( units );
        obstacle.setUserData( this );
        obstacle.setName("cup");
        obstacle.getFilterData().maskBits = (short) (0xFFFF & ~0x0006);

        debug = ParserUtils.parseColor(settings.get("debug"), Color.WHITE);

        // Create a rectangular mesh the same size as the door, adjusted by
        // the physics units. For all meshes attached to a physics body, we
        // want (0,0) to be in the center of the mesh. So the method call below
        // is (x,y,w,h) where x, y is the bottom left.
        mesh.set(-size/2.0f,-size/2.0f,size,size);
    }

    /**
     * returns the width of this cup in world units.
     *
     * @return the width of the object in world units
     */
    public float getSize(){
        return mesh.computeBounds().getWidth();
    }

    /**
     * Gets the X position of the cup in screen coordinates.
     * MAY NEED FOR AI CONTROLLER
     * @return X position of the crate in screen coordinates
     */
    public float getXPos() {
        return obstacle.getX() * units;
    }

    /**
     * Gets the Y position of the cup in screen coordinates.
     * @return Y position of the cup in screen coordinates
     */
    public float getYPos() {
        return obstacle.getY() * units;
    }

}

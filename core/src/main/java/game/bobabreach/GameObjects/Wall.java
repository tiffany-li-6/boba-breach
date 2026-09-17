package game.bobabreach.GameObjects;

/*
 * Door.java
 *
 * This class is a ObstacleSprite referencing a rectangular obstacle such as a
 * crate or the "win door". All it does is override the constructor. We do this
 * for organizational purposes. Otherwise we have to put a lot of initialization
 * code in the scene, and that just makes the scene too long and unreadable.
 *
 * Based on the original PhysicsDemo Lab by Don Holden, 2007
 *
 * Author:  Walker M. White
 * Version: 2/8/2025
 */

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

/**
 * A rectangular obstacle. Taken from Lab 4.
 *
 */
public class Wall extends ObstacleSprite {
    /**
     * Creates a box with the given physics units and settings
     *
     * The physics units are used to size the mesh relative to the physics
     * body. All other attributes are defined by the JSON file
     *
     * @param units     The physics units
     * @param settings  The door physics constants
     */
    public Wall(float x, float y, float width, float height, float units, JsonValue settings) {
        super();

        float adjWidth = width * units;
        float adjHeight = height * units;

        obstacle = new BoxObstacle(x, y, adjWidth, adjHeight);
        obstacle.setBodyType( BodyDef.BodyType.StaticBody );
        obstacle.setDensity( settings.getFloat( "density", 0 ) );
        obstacle.setFriction( settings.getFloat( "friction", 0 ) );
        obstacle.setRestitution( settings.getFloat( "restitution", 0 ) );
        obstacle.setPhysicsUnits( units );
        obstacle.setUserData( this );
        obstacle.getFilterData().groupIndex = -3;
        obstacle.getFilterData().maskBits = (short) (0xFFFF & ~0x0006);

        debug = ParserUtils.parseColor( settings.get("debug"),  Color.WHITE);

        // Create a rectangular mesh the same size as the door, adjusted by
        // the physics units. For all meshes attached to a physics body, we
        // want (0,0) to be in the center of the mesh. So the method call below
        // is (x,y,w,h) where x, y is the bottom left.
        mesh.set(-adjWidth/2.0f,-adjHeight/2.0f,adjWidth,adjHeight);
    }

}

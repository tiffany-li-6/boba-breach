/*
 * Bullet.java
 *
 * This class is a ObstacleSprite referencing a bullet. All it does is override
 * the constructor. We do this for organizational purposes. Otherwise we have
 * to put a lot of initialization code in the scene, and that just makes the
 * scene too long and unreadable.
 *
 * Based on the original PhysicsDemo Lab by Don Holden, 2007
 *
 * Author:  Walker M. White
 * Version: 2/8/2025
 */
package game.bobabreach.GameObjects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.physics2.WheelObstacle;

/**
 * A boba projectile fired by the player from the slingshot.
 */
public class Boba extends ObstacleSprite {
    /** How long the boba stays on the screen after launch. */
    private int lifetime;

    /** How fast the boba travels. */
    private float speed;

    /** How long the boba has been on screen. */
    private int time;

    /** How many times the boba can bounce before it is destroyed */
    private int maxBounces;

    /** How many times the boba has currently bounced */
    private int bounces;

    /** Whether this boba should be destroyed by the next update */
    private boolean markedForDestruction;

    /** Size of boba */
    private float baseRadius;

    /** How much to size down boba over time. */
    private float scale;

    /**
     * Creates a bullet with the given physics units and settings
     *
     * The physics units are used to size the mesh relative to the physics
     * body. The other attributes (pos, right) are used to position the bullet
     * relative to the slingshot.
     *
     * @param units     The physics units
     * @param settings  The boba physics constants
     * @param pos       Slingshot's position
     * @param direction  Unit vector representing the direction the boba should travel
     */
    public Boba (float units, JsonValue settings, Vector2 pos, Vector2 direction) {
        float xOffset = settings.getFloat( "offset", 0 );
        xOffset *= direction.x;
        float yOffset = settings.getFloat( "offset", 0 );
        yOffset *= direction.y;
        float s = settings.getFloat( "size" );
        float radius = s * units / 2.0f;
        baseRadius = radius;

        lifetime = settings.getInt("lifetime", 1);
        speed = settings.getFloat("speed", 0);
        scale = 1f;
        maxBounces = settings.getInt("max bounces", 1);
        bounces = 0;

        // Create a circular obstacle
        obstacle = new WheelObstacle( pos.x + xOffset, pos.y + yOffset, s/2 * 0.5f );

        // Square obstacle for testing
        // obstacle = new BoxObstacle(pos.x + xOffset, pos.y + yOffset, s, s);
//        System.out.println("Boba x: " + (pos.x + xOffset));
//        System.out.println("Boba y: " + pos.y);
        obstacle.setDensity( settings.getFloat( "density", 0 ) );
        obstacle.setPhysicsUnits( units );
        obstacle.setBullet( true );
        obstacle.setRestitution(settings.getFloat("restitution", 1));
        obstacle.setGravityScale( 0 );
        obstacle.setUserData( this );
        obstacle.setFixedRotation(true);
        obstacle.getFilterData().groupIndex = -1; // Boba balls don't collide with each other
        obstacle.getFilterData().categoryBits = 0x0005;
        obstacle.getFilterData().maskBits = (short) (0xFFFF & ~0x0008); // Don't register collisions w/ slingshot
        obstacle.setName( "boba" );

        float speed = settings.getFloat( "speed", 0 );
        obstacle.setLinearVelocity(direction.scl(speed));

        debug = ParserUtils.parseColor( settings.get( "debug" ), Color.WHITE );

        // While the bullet is a circle, we want to create a rectangular mesh.
        // That is because the image is a rectangle. The width/height of the
        // rectangle should be the same as the diameter of the circle (adjusted
        // by the physics units). Note that radius has ALREADY been multiplied
        // by the physics units. In addition, for all meshes attached to a
        // physics body, we want (0,0) to be in the center of the mesh. So
        // the method call below is (x,y,w,h) where x, y is the bottom left.
        mesh.set( -radius, -radius, 2 * radius, 2 * radius );
    }

    public void update() {
        time++;
        if (time > lifetime) {
            markedForDestruction = true;
        }
//        obstacle.setLinearVelocity(obstacle.getLinearVelocity().nor().scl(speed));

        scale = Math.max(scale * 0.997f, 0.2f);

        float newRadius = baseRadius * scale;

        mesh.set(-newRadius, -newRadius, 2 * newRadius, 2 * newRadius);
    }

    /** Increase the number of bounces this boba has had by 1.
     * If this puts it over the max, mark it for destruction.
     */
    public void increaseBounces() {
        bounces++;

        float scale = (float)Math.pow(0.9, bounces);

        scale = Math.max(scale, 0.2f);

        float newRadius = baseRadius * scale;

        mesh.set(-newRadius, -newRadius, 2 * newRadius, 2 * newRadius);

        if (bounces > maxBounces) {
            markedForDestruction = true;
        }
    }

    /**
     * Immediately mark a boba for destruction.
     */
    public void markForDestruction() {
        markedForDestruction = true;
    }

    /**
     * Whether this boba is marked for destruction
     */
    public boolean getMark() {
        return markedForDestruction;
    }

}

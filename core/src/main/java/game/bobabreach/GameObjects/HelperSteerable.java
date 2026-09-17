package game.bobabreach.GameObjects;

import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.steer.behaviors.Arrive;
import com.badlogic.gdx.ai.steer.behaviors.FollowPath;
import com.badlogic.gdx.ai.steer.behaviors.PrioritySteering;
import com.badlogic.gdx.ai.steer.behaviors.RaycastObstacleAvoidance;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.ai.steer.utils.rays.CentralRayWithWhiskersConfiguration;
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.Texture2D;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.CapsuleObstacle;
import game.bobabreach.AIControllers.Box2dRaycastCollisionDetector;
import game.bobabreach.AIControllers.TargetLocation;

/**
 * Model class for the enemies. Code borrowed from Lab 4.
 */
public class HelperSteerable extends Helper implements Steerable<Vector2> {

    /**
     * Enumeration to encode the steering behaviors
     */
    public enum HelperBehaviorState {
        /**
         * The helper is doing nothing
         */
        IDLE,
        PATH,
        ARRIVE
    }

    /** Where the player character can face? */
    private static enum FaceDirection {
        RIGHT,
        UP_RIGHT,
        UP,
        UP_LEFT,
        LEFT,
        DOWN_LEFT,
        DOWN,
        DOWN_RIGHT
    }
    /** The initializing data (to avoid magic numbers) */
    private final JsonValue data;
    /** The width of the helper avatar */
    private float width;
    /** The height of helper avatar */
    private float height;
    /** Physics units we're using */
    private float units;

    /** The current state of the helper (to be used by AI controller) */
    private HelperFSMState state;

    /** The factor to multiply by the input */
    private float force;
    /** The amount to slow the character down */
    private float damping;
    /** The maximum character speed */
    private float maxspeed;

    /** The current horizontal movement of the character */
    private float horizontal;
    /** The current vertical movement of the character */
    private float vertical;
    /** The overall movement of the character */
    private Vector2 movement;
    /** Where the character is facing */
    private FaceDirection facing;

    /** Default texture */
    private Texture defaultTexture;
    /** Texture to use when helper is selected */
    private Texture selectTexture;

    /** The outline of the sensor obstacle */
    private Path2 sensorOutline;
    /** The debug color for the sensor */
    private Color sensorColor;
    /** The name of the sensor fixture */
    private String sensorName;

    /** Cache for internal force calculations */
    private final Vector2 forceCache = new Vector2();
    /** Cache for the affine flip */
    private final Affine2 flipCache = new Affine2();

    /**
     * Whether this character is under player control
     */
    private boolean isControlled;
    /**
     * Cooldown (in animation frames) for switching btw. helper & Pearl
     */
    private int controlLimit;
    /**
     * How long until we can take control again
     */
    private int controlCooldown;

    /**
     * Cooldown (in animation frames) for picking up ingredients
     */
    private int pickupLimit;
    /**
     * How long until we can pick up again
     */
    private int pickupCooldown;

    /** The integer index of the crate this bug wants to steal from. Starts at 0. */
    private int target;
    /** Whether the helper is currently holding an ingredient*/
    private boolean hasIngredient;
    /** The IngredientType of the ingredient the helper is holding.*/
    private IngredientType heldIngredient;
    private HelperSpawn spawn;

    private boolean touchingCup;

    /**
     * If the helper is MOVING, where is it targeting? (Box2d coordinates)
     */
    private Vector2 moveTarget;


    public int getTarget() {
        return target;
    }

    /**
     * Set new target crate for helper.
     * @param n index of new crate
     */
    public void setTarget(int n) {
        target = n;
    }

    /**
     * Set new target move pos for helper.
     * @param x x coord of target
     * @param y y coord of target
     */
    public void setMove(float x, float y) {
        moveTarget.set(x, y);
        locCache.setPosition(x, y);
    }

    public HelperSpawn getSpawn() { return spawn; }

    /**
     * Sets whether the player is currently controlling this character.
     * @param val whether this character should now be under player control
     */
    public void setControlled(boolean val) {
        isControlled = val;
        controlCooldown = 0;
        if (val) {
            setTexture(selectTexture);
        }
        else {
            setTexture(defaultTexture);
        }
    }

    /**
     * Returns whether this character is under player control
     * @return whether the helper is under player control
     */
    public boolean isControlled() {
        return isControlled;
    }

    /**
     * Returns whether we can swap to this character.
     * @return if we can swap character control
     */
    public boolean canSwapCharacters() {
        return controlCooldown <= 0;
    }

    /**
     * Returns whether we can pick up another ingredient (or drop it off back to its crate)
     * @return if we can pick up another ingredient from a crate
     */
    public boolean canPickup() {
        return pickupCooldown <= 0;
    }

    /**
     * Returns the movement of this character.
     *
     * This is the result of input times force.
     *
     * @return the movement of this character.
     */
    public Vector2 getMovement() {
        return movement;
    }

    /**
     * Sets how much and where this character should be moving.
     *
     * This is the result of input times force.
     *
     * @param horizontal the left/right movement of this character.
     * @param vertical the up/down movement of this character.
     */

    /**
     * Stops ALL the player's current movement.
     */
    public void lockVelocity() {
        obstacle.setLinearVelocity(Vector2.Zero);
    }

    /**
     * Sets the player's position to a set of screen coordinates.
     * @param x Screen coordinates of player's new x
     * @param y Screen coordinates of player's new y
     *          @param y Screen coordinates of player's new y
     */
    public void setPosition(float x, float y) {
        obstacle.setPosition(x / units, y / units);
    }

    /**
     * Returns how much force to apply to get player moving
     *
     * Multiply this by the input to get the movement value.
     *
     * @return how much force to apply to get player moving
     */
    public float getForce() {
        return force;
    }

    /**
     * Returns how hard the brakes are applied to stop player moving
     *
     * @return how hard the brakes are applied to stop player moving
     */
    public float getDamping() {
        return damping;
    }

    /**
     * Returns the upper limit on player's movement.
     *
     * @return the upper limit on player's movement.
     */
    public float getMaxSpeed() {
        return maxspeed;
    }


    /**
     * Returns the name of the player hitbox
     *
     * This is used by the ContactListener. Because we do not associate the
     * sensor with its own obstacle,
     *
     * @return the name of the ground sensor
     */
    public String getSensorName() {
        return sensorName;
    }

    /**
     * Gets the current state of the enemy.
     *
     * @return current state of enemy
     */
    public Helper.HelperFSMState getState() {
        return state;
    }

    /**
     * Sets the current state of the enemy.
     * @param state new state
     */
    public void setState(HelperFSMState state) {
        this.state = state;
        if(state == HelperFSMState.IDLE) {
            lockVelocity();
        }
    }

    /**
     * returns whether the player is currently holding an ingredient.
     *
     * @return true is the player is holding an ingredient, false otherwise.
     */
    public boolean hasIngredient() {
        return hasIngredient;
    }

    /**
     * Gives the player an ingredient by setting hasIngredient to true
     */
    public void giveIngredient(IngredientType ingredient) {
        hasIngredient = true;
        heldIngredient = ingredient;
        pickupCooldown = pickupLimit;
    }

    /**
     * Removes the player's ingredient by setting hasIngredient to false
     */
    public void removeIngredient() {
        hasIngredient = false;
        heldIngredient = null;
        pickupCooldown = pickupLimit;
    }

    /**
     * Returns the ingredient that the player is currently holding.
     * @return the IngredientType of the player's current held ingredient
     */
    public IngredientType getHeldIngredient(){
        return heldIngredient;
    }

    /**
     * Returns where the helper is currently trying to move to
     * @return Vector representing helper's movement target
     */
    public Vector2 getMoveTarget() {
        return moveTarget;
    }

    SteeringBehavior<Vector2> behavior;

    SteeringAcceleration<Vector2> steerOutput;


    /**
     * Creates a new enemy with the given physics data
     *
     * The physics units are used to size the mesh relative to the physics
     * body. All other attributes are defined by the JSON file. Because of
     * transparency around the image file, the physics object will be slightly
     * thinner than the mesh in order to give a tighter hitbox.
     *
     * @param units     The physics units
     * @param data      The physics constants for the enemy
     */
    public HelperSteerable(float x, float y, float units, JsonValue data, int target, BugType type) {
        super(x, y, units, data, target, type);
        this.data = data;
        JsonValue debugInfo = data.get("debug");

        this.units = units;
        float s = data.getFloat( "size" );
        float size = s*units;

        this.target = target;
        this.spawn = spawn;

        touchingCup = false;
        moveTarget = new Vector2();

        // The capsule is smaller than the image
        // "inner" is the fraction of the original size for the capsule
        width = s*data.get("inner").getFloat(0);
        height = s*data.get("inner").getFloat(1);
        obstacle = new CapsuleObstacle(x, y, width, height);
        ((CapsuleObstacle)obstacle).setTolerance( debugInfo.getFloat("tolerance", 0.5f) );

        obstacle.setDensity( data.getFloat( "density", 0 ) );
        obstacle.setFriction( data.getFloat( "friction", 0 ) );
        obstacle.setRestitution( data.getFloat( "restitution", 0 ) );
        // obstacle.setFixedRotation(true);
        obstacle.setPhysicsUnits( units );
        obstacle.setUserData( this );
        obstacle.setName("helper");
        obstacle.getFilterData().groupIndex = -2;

        this.defaultTexture = defaultTexture;
        this.selectTexture = selectTexture;

        controlLimit = data.getInt("control_cool", 0);
        isControlled = false;
        pickupLimit = data.getInt("pickup_cool", 0);
        pickupCooldown = pickupLimit;

        debug = ParserUtils.parseColor( debugInfo.get("avatar"),  Color.WHITE);
        sensorColor = ParserUtils.parseColor( debugInfo.get("sensor"),  Color.WHITE);

        maxspeed = data.getFloat("max_speed", 0);
        damping = data.getFloat("damping", 0);
        force = data.getFloat("force", 0);

        // Steerable values
        maxLinearSpeed = data.getFloat("max_speed", 0);
        minLinearSpeed = data.getFloat("min_speed", 0);
        maxLinearAccel = data.getFloat("max_linear_accel", 0);
        maxAngSpeed = data.getFloat("max_ang_speed", 0);
        maxAngAccel = data.getFloat("max_ang_accel", 0);
        tagged = false;
        steerOutput = new SteeringAcceleration<>(new Vector2());

        locCache = new TargetLocation();
        World w = new World(new Vector2(), false);
        detector = new Box2dRaycastCollisionDetector(w);
        arriveSB = new Arrive<>(this, locCache);
        avoidSB = new RaycastObstacleAvoidance<>(this, new CentralRayWithWhiskersConfiguration<>
            (this, 2f, 1f, (float) Math.PI/8f),
            detector);
        helperSB = new PrioritySteering<>(this);
        helperSB.add(avoidSB);
        helperSB.add(arriveSB);
        behavior = helperSB;

        arriveSB.setArrivalTolerance(0.1f);
        arriveSB.setTimeToTarget(0.01f);
        arriveSB.setDecelerationRadius(1f);


        movement = new Vector2();
        facing = FaceDirection.RIGHT;

        state = HelperFSMState.IDLE;

        // Create a rectangular mesh for the enemy.
        mesh.set(-size/2.0f,-size/2.0f,size,size);
    }

    /**
     * Creates the sensor for the helper.
     */
    public void createSensor() {
        Vector2 sensorCenter = new Vector2(0, 0);
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = data.getFloat("density",0);
        sensorDef.isSensor = true;

        JsonValue sensorjv = data.get("sensor");
        float w = sensorjv.getFloat("xFactor",0)*width;
        float h = sensorjv.getFloat("yFactor",0)*height;
        PolygonShape sensorShape = new PolygonShape();
        sensorShape.setAsBox(w, h, sensorCenter, 0.0f);
        sensorDef.shape = sensorShape;

        // Sensor to represent hitbox
        Body body = obstacle.getBody();
        Fixture sensorFixture = body.createFixture( sensorDef );
        sensorName = "player_hitbox";
        sensorFixture.setUserData(sensorName);

        // Finally, we need a debug outline
        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
        sensorOutline = new Path2();
        factory.makeRect( (sensorCenter.x-w/2)*u,(sensorCenter.y-h/2)*u, w*u, h*u,  sensorOutline);
    }


    /**
     * Applies the force to the body of the helper. RIGHT NOW,
     * just directly sets the enemy's linear velocity to its current
     * movement.
     *
     * This method should be called after the force attribute is set.
     */
    // Could refactor this into setMovement()
    public void applyForce() {
        if (!obstacle.isActive()) {
            return;
        }

        Vector2 pos = obstacle.getPosition();
        float vx = obstacle.getVX();
        float vy = obstacle.getVY();
        Body body = obstacle.getBody();

        movement.nor();
        movement.scl(force);
        body.setLinearVelocity(movement.x, movement.y);

//        // Don't want to be moving. Damp out player motion
//        if (getMovement() == Vector2.Zero) {
//            forceCache.set(-getDamping()*vx,-getDamping()*vy);
//            body.applyForce(forceCache,pos,true);
//        }
//
//        // Velocity too high, clamp it
//        if (Math.abs(vx) >= getMaxSpeed() || Math.abs(vy) >= getMaxSpeed()) {
//            if (Math.abs(vx) >= getMaxSpeed()) {
//                obstacle.setVX(Math.signum(vx)*getMaxSpeed());
//            }
//            if (Math.abs(vy) >= getMaxSpeed()) {
//                obstacle.setVY(Math.signum(vy)*getMaxSpeed());
//            }
//        } else {
//            forceCache.set(getMovement());
//            body.applyForce(forceCache,pos,true);
//        }
    }

    private TargetLocation locCache;
    private Arrive<Vector2> arriveSB;
    private PrioritySteering<Vector2> helperSB;
    private RaycastObstacleAvoidance<Vector2> avoidSB;
    private Box2dRaycastCollisionDetector detector;

    private FollowPath<Vector2, LinePath.LinePathParam> pathSB;
    private LinePath<Vector2> linePath;

    public void setPath(LinePath<Vector2> path) {
        linePath = path;
        if (pathSB == null) {
            pathSB = new FollowPath<>(this, linePath, 1);
            pathSB.setArrivalTolerance(0.1f);
            pathSB.setTimeToTarget(0.01f);
            pathSB.setDecelerationRadius(1f);
        }
        else {
            pathSB.setPath(path);
        }

    }


    /**
     * Updates the object's physics state (NOT GAME LOGIC).
     * <p>
     * We use this method to reset cooldowns.
     *
     * @param dt Number of seconds since last animation frame
     */
    @Override
    public void update(float dt) {
        controlCooldown = Math.max(0, controlCooldown - 1);
        pickupCooldown = Math.max(0, pickupCooldown - 1);

        // STEERING
        locCache.setPosition(getMoveTarget().x, getMoveTarget().y);

        if (behavior != null && state != HelperFSMState.IDLE) {
            behavior.calculateSteering(steerOutput);
            applySteering(dt);
        }
    }


    public void setBehavior(HelperBehaviorState state) {
        switch (state) {
            case IDLE:
                behavior = null;
                break;
            case PATH:
                behavior = pathSB;
                break;
            default:
                behavior = null;
                break;
        }
    }

    /**
     * Sets how much and where this character should be moving.
     *
     * This is the result of input times force.
     *
     * @param horizontal the left/right movement of this character.
     * @param vertical the up/down movement of this character.
     */
    public void setMovement(float horizontal, float vertical) {
        movement.x = horizontal;
        movement.y = vertical;

        // Change facing if appropriate
//        if (horizontal < 0 && vertical == 0) {
//            facing = FaceDirection.LEFT;
//        } else if (horizontal < 0 && vertical > 0) {
//            facing = FaceDirection.UP_LEFT;
//        } else if (horizontal < 0 && vertical < 0) {
//            facing = FaceDirection.DOWN_LEFT;
//        } else if (horizontal == 0 && vertical > 0) {
//            facing = FaceDirection.UP;
//        } else if (horizontal == 0 && vertical < 0) {
//            facing = FaceDirection.DOWN;
//        } else if (horizontal > 0 && vertical == 0) {
//            facing = FaceDirection.RIGHT;
//        } else if (horizontal > 0 && vertical > 0) {
//            facing = FaceDirection.UP_RIGHT;
//        } else if (horizontal > 0 && vertical < 0) {
//            facing = FaceDirection.DOWN_RIGHT;
//        }
    }

    // Credit: https://www.youtube.com/watch?v=JoCZ8hPQnUE
    private void applySteering(float dt) {
        boolean anyAccelerations = false;

        if (!steerOutput.linear.isZero()) {
             Vector2 force = steerOutput.linear.scl(dt);
             obstacle.getBody().applyForceToCenter(force, true);
            // setMovement(steerOutput.linear.x, steerOutput.linear.y);
            anyAccelerations = true;
        }

        if (steerOutput.angular != 0) {
            obstacle.getBody().applyTorque(steerOutput.angular * dt, true);
            anyAccelerations = true;
        }
        else {
            Vector2 linVel = getLinearVelocity();
            if (!linVel.isZero()) {
                float newOrientation = vectorToAngle(linVel);
                obstacle.getBody().setAngularVelocity((newOrientation - getAngularVelocity()) * dt);
                obstacle.getBody().setTransform(obstacle.getBody().getPosition(), newOrientation);
                // obstacle.getBody().setTransform(obstacle.getBody().getPosition(), calculateOrientationFromLinearVelocity());
            }
        }

        if (anyAccelerations) {
            Vector2 velocity = obstacle.getLinearVelocity();
            float currentSpeedSquare = velocity.len2();
            if (currentSpeedSquare > maxLinearSpeed * maxLinearSpeed) {
                obstacle.getBody().setLinearVelocity(velocity.scl(maxLinearSpeed /
                    (float) Math.sqrt(currentSpeedSquare)));
            }
            if (obstacle.getBody().getAngularVelocity() > maxAngSpeed) {
                obstacle.getBody().setAngularVelocity(maxAngSpeed);
            }
        }
    }

    public float calculateOrientationFromLinearVelocity () {
        // If we haven't got any velocity, then we can do nothing.
        if (getLinearVelocity().isZero(getZeroLinearSpeedThreshold())) {
            return getOrientation();
        }

        return vectorToAngle(getLinearVelocity());
    }

    /**
     * Gets the X position of the player in screen coordinates.
     * @return X position of the player in screen coordinates
     */
    public float getXPos() {
        return obstacle.getX() * units;
    }

    /**
     * Gets the Y position of the player in screen coordinates.
     * @return Y position of the player in screen coordinates
     */
    public float getYPos() {
        return obstacle.getY() * units ;
    }


    /**
     * Draws the physics object.
     *
     * This method is overridden from ObstacleSprite. We need to rotate the
     * texture depending on the player's movement. We do that by creating
     * a reflection affine transform.
     *
     * @param batch The sprite batch to draw to
     */
    @Override
    public void draw(SpriteBatch batch) {
        if (!isControlled) {
            batch.setColor(Color.GRAY);
        }
        super.draw(batch);
        batch.setColor(Color.WHITE);
    }

    /**
     * Draws the outline of the physics object.
     *
     * This method is overridden from ObstacleSprite. By default, that method
     * only draws the outline of the main physics obstacle. We also want to
     * draw the outline of the sensor, and in a different color. Since it
     * is not an obstacle, we have to draw that by hand.
     *
     * @param batch The sprite batch to draw to
     */
    @Override
    public void drawDebug(SpriteBatch batch) {
        super.drawDebug( batch );

        if (sensorOutline != null) {
            batch.setTexture( Texture2D.getBlank() );
            batch.setColor( sensorColor );

            Vector2 p = obstacle.getPosition();
            float a = obstacle.getAngle();
            float u = obstacle.getPhysicsUnits();

            // transform is an inherited cache variable
            transform.idt();
            transform.preRotate( (float) (a * 180.0f / Math.PI) );
            transform.preTranslate( p.x * u, p.y * u );

            //
            batch.outline( sensorOutline, transform );
        }
    }
    /**
     * returns the width of this enemy in world units.
     * @return the width of the object in world units
     */
    public float getSize(){
        return mesh.computeBounds().getWidth();
    }

    /**
     * Sets whether or not the helper is currently touching a cup.
     * This value is controlled by collision detection.
     *
     * @param val true if the player is colliding with the cup, false otherwise.
     */
    public void setTouchingCup(boolean val) { touchingCup = val; }

    /**
     * Returns whether the helper is currently touching a cup.
     *
     * @return true if the helper is colliding with a cup, false otherwise.
     */
    public boolean getTouchingCup() { return touchingCup; }

    // METHODS FROM STEERABLE

    private boolean tagged;

    /**
     * Returns the vector indicating the linear velocity of this Steerable.
     */
    @Override
    public Vector2 getLinearVelocity() {
        return obstacle.getLinearVelocity();
    }

    /**
     * Returns the float value indicating the the angular velocity in radians of this Steerable.
     */
    @Override
    public float getAngularVelocity() {
        return obstacle.getAngularVelocity();
    }

    /**
     * Returns the bounding radius of this Steerable.
     */
    @Override
    public float getBoundingRadius() {
        return getSize();
    }

    /**
     * Returns {@code true} if this Steerable is tagged; {@code false} otherwise.
     */
    @Override
    public boolean isTagged() {
        return tagged;
    }

    /**
     * Tag/untag this Steerable. This is a generic flag utilized in a variety of ways.
     *
     * @param tagged the boolean value to set
     */
    @Override
    public void setTagged(boolean tagged) {
        this.tagged = tagged;
    }

    private float minLinearSpeed = 0;

    /**
     * Returns the threshold below which the linear speed can be considered zero. It must be a small positive value near to zero.
     * Usually it is used to avoid updating the orientation when the velocity vector has a negligible length.
     */
    @Override
    public float getZeroLinearSpeedThreshold() {
        return minLinearSpeed;
    }

    /**
     * Sets the threshold below which the linear speed can be considered zero. It must be a small positive value near to zero.
     * Usually it is used to avoid updating the orientation when the velocity vector has a negligible length.
     *
     * @param value
     */
    @Override
    public void setZeroLinearSpeedThreshold(float value) {
        minLinearSpeed = value;
    }

    private float maxLinearSpeed;

    /**
     * Returns the maximum linear speed.
     */
    @Override
    public float getMaxLinearSpeed() {
        return maxLinearSpeed;
    }

    /**
     * Sets the maximum linear speed.
     *
     * @param maxLinearSpeed
     */
    @Override
    public void setMaxLinearSpeed(float maxLinearSpeed) {
        this.maxLinearSpeed = maxLinearSpeed;
    }

    private float maxLinearAccel;
    /**
     * Returns the maximum linear acceleration.
     */
    @Override
    public float getMaxLinearAcceleration() {
        return maxLinearAccel;
    }

    /**
     * Sets the maximum linear acceleration.
     *
     * @param maxLinearAcceleration
     */
    @Override
    public void setMaxLinearAcceleration(float maxLinearAcceleration) {
        maxLinearAccel = maxLinearAcceleration;
    }

    private float maxAngSpeed;

    /**
     * Returns the maximum angular speed.
     */
    @Override
    public float getMaxAngularSpeed() {
        return maxAngSpeed;
    }

    /**
     * Sets the maximum angular speed.
     *
     * @param maxAngularSpeed
     */
    @Override
    public void setMaxAngularSpeed(float maxAngularSpeed) {
        maxAngSpeed = maxAngularSpeed;
    }

    private float maxAngAccel;

    /**
     * Returns the maximum angular acceleration.
     */
    @Override
    public float getMaxAngularAcceleration() {
        return maxAngAccel;
    }

    /**
     * Sets the maximum angular acceleration.
     *
     * @param maxAngularAcceleration
     */
    @Override
    public void setMaxAngularAcceleration(float maxAngularAcceleration) {
        maxAngAccel = maxAngularAcceleration;
    }

    /**
     * Returns the vector indicating the position of this location.
     */
    @Override
    public Vector2 getPosition() {
        return obstacle.getPosition();
    }

    /**
     * Returns the float value indicating the orientation of this location. The orientation is the angle in radians representing
     * the direction that this location is facing.
     */
    @Override
    public float getOrientation() {
        return obstacle.getAngle();
    }

    /**
     * Sets the orientation of this location, i.e. the angle in radians representing the direction that this location is facing.
     *
     * @param orientation the orientation in radians
     */
    @Override
    public void setOrientation(float orientation) {
        obstacle.setAngle(orientation);
    }

    /**
     * Returns the angle in radians pointing along the specified vector.
     *
     * @param vector the vector
     */
    @Override
    public float vectorToAngle(Vector2 vector) {
        return (float)Math.atan2(-vector.x, vector.y);
    }

    /**
     * Returns the unit vector in the direction of the specified angle expressed in radians.
     *
     * @param outVector the output vector.
     * @param angle     the angle in radians.
     * @return the output vector for chaining.
     */
    @Override
    public Vector2 angleToVector(Vector2 outVector, float angle) {
        outVector.x = -(float)Math.sin(angle);
        outVector.y = (float)Math.cos(angle);
        return outVector;
    }

    /**
     * Creates a new location.
     * <p>
     * This method is used internally to instantiate locations of the correct type parameter {@code T}. This technique keeps the API
     * simple and makes the API easier to use with the GWT backend because avoids the use of reflection.
     *
     * @return the newly created location.
     */
    @Override
    public Location<Vector2> newLocation() {
        return this;
    }





}

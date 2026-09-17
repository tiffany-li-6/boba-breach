package game.bobabreach.GameObjects;

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.pfa.SmoothableGraphPath;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.Texture2D;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.*;
import game.bobabreach.BugAnimations;
import game.bobabreach.Direction;
import game.bobabreach.DirectionalAnimation;
import game.bobabreach.GameObjects.Enemy.AnimState;
import game.bobabreach.GameObjects.Enemy.EnemyFSMState;
import game.bobabreach.WorldGraph;

/**
 * Model class for the enemies. Code borrowed from Lab 4.
 */
public class Helper extends ObstacleSprite {
    /**
     * Enumeration to encode the finite state machine.
     */
    public enum HelperFSMState {
        /**
         * The helper is doing nothing
         */
        IDLE,
        /**
         * The helper is moving to a position
         */
        MOVE,
        /**
         * The helper is moving to its designated crate
         */
        PICKUP,
        /**
         * The helper is dropping the ingredient off at the cup
         */
        DROPOFF,
        /**
         * The helper is returning to its spawn
         */
        RETURN,
        /**
         * The helper has been touched by an enemy bug and is stunned!
         * */
        STUNNED
    }

    /** Where the player character can face */
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

    /**
     * color of helper, for texture selection
     */
    private BugType type;

    public enum AnimState { MOVE_ANIM, PICKUP_ANIM, DEATH_ANIM } //never dies

    private Helper.AnimState animState = Helper.AnimState.MOVE_ANIM;
    private boolean animLocked = false;

    private HelperFSMState pendingState = null;

    public void setPendingState(HelperFSMState state) {
        pendingState = state;
    }


    /** The initializing data (to avoid magic numbers) */
    private final JsonValue data;
    private float radius;
    /** The width of the helper avatar */
    private float width;
    /** The height of helper avatar */
    private float height;
    /** Physics units we're using */
    private float units;
    private float drawWidth;
    private float drawHeight;

    /** The current state of the helper (to be used by AI controller) */
    private HelperFSMState state;
    private int moveMode;

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
    /** Slow bug while carrying ingredient */
    private float carrySpeedMult;
    /** Where we spawned from */
    private HelperSpawn spawn;

    private boolean touchingCup;
    private int collidingCrateIdx;
    private int lastCollidingCrateIdx;

    private float stun_duration;
    private float stun_flashing_interval;
    private float stunTimer;
    private boolean stunVisible = true;
    private float stunFlashAccum = 0f;

    private SmoothableGraphPath<WorldGraph.IndexNode, Vector2> path;
    private int pathIndex;

    /**
     * Don't spin around when getting pushed by blocker
     */
    private boolean isCollidingWithBlocker;

    /**
     * Avoid running into corners when close to crate
     */
    private boolean isCollidingWithCrate;

    private int collisionUpdateTime;
    private int collisionUpdateCooldown = 0;
    private boolean setTrueThisFrame;

    private int switchMovementTime;
    private int switchMovementCooldown = 0;
    /** 0 for raycast direct movement, 1 for pathfinding */
    private int lastMoveMode;

    /**
     * If the helper is MOVING, where is it targeting? (Box2d coordinates)
     */
    private Vector2 moveTarget;

    /**
     * Sprite object stored inside the helper ObstacleSprite, used for changing animation texture
     */
    Sprite sprite;

    /**
     * time in a state/animation
     * increment each frame
     */
    float stateTime;

    /**
     * current direction being faced
     * need to update each frame corresponding to movement
     */
    Direction currentDir;

    /**
     * store anim for this helper
     */
    private BugAnimations anims;



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
        path = null;
        pathIndex = 0;
        moveMode = 0;
        moveTarget.set(x, y);
    }

    public HelperSpawn getSpawn() { return spawn; }

    /**
     * Sets whether the player is currently controlling this character.
     * @param val whether this character should now be under player control
     */
    public void setControlled(boolean val) {
        isControlled = val;
        controlCooldown = 0;
        /*
        if (val) {
            setTexture(selectTexture);
        }
        else {
            setTexture(defaultTexture);
        }
        */

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

    public void resetCollidingCrateIdx() {
        collidingCrateIdx = -1;
    }

    public void setCollidingCrateIdx(int index) {
        collidingCrateIdx = index;
        lastCollidingCrateIdx = index;
    }

    public int getCollidingCrateIdx() {
        return collidingCrateIdx;
    }

    /**
     * Sets how much and where this character should be moving.
     *
     * This is the result of input times force.
     *
     * @param horizontal the left/right movement of this character.
     * @param vertical the up/down movement of this character.
     */
    public void setMovement(float horizontal, float vertical, int type) {
        if (type == lastMoveMode)  {
            movement.x = horizontal;
            movement.y = vertical;
        }
        else if (switchMovementCooldown <= 0) {
            movement.x = horizontal;
            movement.y = vertical;
            switchMovementCooldown = switchMovementTime;
        }

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

    public float getOrientation() {
        return obstacle.getAngle();
    }

    public void setOrientation(float orientation) {
        obstacle.setAngle(orientation);
    }

    public float calculateOrientationFromLinearVelocity () {
        // If we haven't got any velocity, then we can do nothing.
        if (obstacle.getLinearVelocity().isZero()) {
            return getOrientation();
        }
        return vectorToAngle(obstacle.getLinearVelocity());
    }

    /**
     * returns angle in radians, with upwards being 0, and positive going around counterclockwise
     * @param vector
     * @return
     */
    public float vectorToAngle(Vector2 vector) {
        return (float)Math.atan2(-vector.x, vector.y);
    }

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
     * @return current state of enemy
     */
    public HelperFSMState getState() {
        return state;
    }

    /**
     * Sets the current state of the enemy.
     * @param state new state
     */
    public void setState(HelperFSMState state) {
        if (this.state != state) {
            this.path = null;
            pathIndex = 0;
            moveMode = 0;
        }
        this.state = state;
        if(state == HelperFSMState.IDLE) {
            lockVelocity();
        }
    }

    public int getMoveMode() {
        return moveMode;
    }

    public void setMoveMode(int i) {
        moveMode = i;
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

    public SmoothableGraphPath<WorldGraph.IndexNode, Vector2> getPath() {
        return path;
    }
    public void setPath(SmoothableGraphPath<WorldGraph.IndexNode, Vector2> path) {
        this.path = path;
    }
    public int getPathIndex() {return pathIndex;}
    public void setPathIndex(int i) {pathIndex = i;}

    public void setCollidingBlocker(boolean val) {
        isCollidingWithBlocker = val;
    }
    public void setCollidingCrate(boolean val) {
        if (collisionUpdateCooldown <= 0) {
            if (val) {
                setTrueThisFrame = true;
                isCollidingWithCrate = val;
                collisionUpdateCooldown = collisionUpdateTime;
            }
            else {
                if (!setTrueThisFrame) {
                    isCollidingWithCrate = val;
                    collisionUpdateCooldown = collisionUpdateTime;
                }
            }
        }
    }

    public boolean getCollidingCrate() {
        return isCollidingWithCrate;
    }


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
    public Helper (float x, float y, float units, JsonValue data, int target, BugType type) {
        this.data = data;
        JsonValue debugInfo = data.get("debug");
        moveMode = 0; // path-follow

        this.units = units;
        float s = data.getFloat( "size" );
        float size = s*units;

        this.target = target;
        // this.spawn = spawn;

        touchingCup = false;
        moveTarget = new Vector2();
        collidingCrateIdx = -1;

        // The capsule is smaller than the image
        // "inner" is the fraction of the original size for the capsule
        width = s*data.get("inner").getFloat(0);
        height = s*data.get("inner").getFloat(1);
        radius = s * data.getFloat("radius");
        drawWidth = s*data.getFloat("draw_width");
        drawHeight = s*data.getFloat("draw_height");
        // obstacle = new CapsuleObstacle(x, y, width, height);
        obstacle = new WheelObstacle(x, y, radius);
        // ((CapsuleObstacle)obstacle).setTolerance( debugInfo.getFloat("tolerance", 0.5f) );
        ((WheelObstacle)obstacle).setTolerance( debugInfo.getFloat("tolerance", 0.5f) );

        obstacle.setDensity( data.getFloat( "density", 0 ) );
        obstacle.setFriction( data.getFloat( "friction", 0 ) );
        obstacle.setRestitution( data.getFloat( "restitution", 0 ) );
        obstacle.setFixedRotation(true);
        obstacle.setPhysicsUnits( units );
        obstacle.setUserData( this );
        obstacle.setName("helper");
        // obstacle.setSensor(true);
//        Filter filter = new Filter();
//        filter.maskBits = 0;
//        obstacle.setFilterData(filter);
        obstacle.getFilterData().groupIndex = -3;
        obstacle.getFilterData().categoryBits = 0x0003;
        obstacle.getFilterData().maskBits = (short) ((0xFFFF & ~0x0006 & ~0x0008) | 0x0004);

        controlLimit = data.getInt("control_cool", 0);
        isControlled = false;
        pickupLimit = data.getInt("pickup_cool", 0);
        pickupCooldown = pickupLimit;
        carrySpeedMult = data.getFloat("carry_speed_mult", 1);

        stun_duration = data.getFloat("stun_duration");
        stun_flashing_interval = data.getFloat("stun_flashing_interval");

        collisionUpdateTime = data.getInt("collision_update_cool");
        collisionUpdateCooldown = 0;
        setTrueThisFrame = false;
        switchMovementTime = data.getInt("move_update_cool");
        switchMovementCooldown = 0;
        lastMoveMode = 0;

        debug = ParserUtils.parseColor( debugInfo.get("avatar"),  Color.WHITE);
        sensorColor = ParserUtils.parseColor( debugInfo.get("sensor"),  Color.WHITE);

        maxspeed = data.getFloat("maxspeed", 0);
        damping = data.getFloat("damping", 0);
        force = data.getFloat("force", 0);
        movement = new Vector2();

        //used for animation
        this.type = type;
        this.currentDir = Direction.E;


        facing = FaceDirection.RIGHT;

        state = HelperFSMState.IDLE;
        isCollidingWithBlocker = false;
        isCollidingWithCrate = false;

        // Create a rectangular mesh for the enemy.
        mesh.set(-size/2.0f,-size/2.0f,size, size);
    }

    public void createHitbox() {
        Vector2 hitboxCenter = new Vector2(0, 0);
        FixtureDef hitboxDef = new FixtureDef();
        hitboxDef.density = obstacle.getDensity();
        hitboxDef.friction = 0f;

//        float w = ((CapsuleObstacle) obstacle).getWidth() * 0.5f;
//        float h = ((CapsuleObstacle) obstacle).getHeight() * 0.5f;
        CircleShape shape = new CircleShape();
        shape.setPosition(hitboxCenter);
        float r = data.getFloat("hitbox radius");
        shape.setRadius(r);
        hitboxDef.shape = shape;
        hitboxDef.filter.categoryBits = 0x0003;
        hitboxDef.filter.maskBits = (short) ((0xFFFF & ~0x0006) & ~0x0008);

        // Fixture to represent hitbox
        Body body = obstacle.getBody();
        Fixture hitboxFix = body.createFixture(hitboxDef);
        String hitboxName = "helper_hitbox";
        hitboxFix.setUserData(hitboxName);
        hitboxFix.setSensor(true);
        hitboxFix.getFilterData().groupIndex = -3;

        // Finally, we need a debug outline
        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
        Path2 sensorOutline = new Path2();
        factory.makeCircle((hitboxCenter.x - r / 2) * u, (hitboxCenter.y - r / 2) * u, r, sensorOutline);
    }

    public void stun() {
        state = HelperFSMState.STUNNED;
        stunTimer = stun_duration;
        lockVelocity();
        obstacle.setSensor(true);
    }

    /**
     * Returns whether this helper bug is currently stunned.
     * @return whether this helper bug is currently stunned.
     */
    public boolean isStunned() { return state == HelperFSMState.STUNNED; }

    public boolean isStunVisible() { return isStunned() && stunVisible; }

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
        if (!obstacle.isActive() || isStunned()) {
            return;
        }

        Vector2 pos = obstacle.getPosition();
        float vx = obstacle.getVX();
        float vy = obstacle.getVY();
        Body body = obstacle.getBody();

        movement.nor();
        movement.scl(force);
        if (hasIngredient) {
            movement.scl(carrySpeedMult);
        }
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

    /**
     * Updates the object's physics state (NOT GAME LOGIC).
     * <p>
     * We use this method to reset cooldowns.
     *
     * @param dt Number of seconds since last animation frame
     */
    @Override
    public void update(float dt) {

        if (isCollidingWithBlocker || state == HelperFSMState.IDLE) {
            setMovement(0, 0, -1);
            lockVelocity();
        }
        stateTime += dt;

        if (animLocked) {
            //lockVelocity();
            DirectionalAnimation active = getActiveAnim();
            if (active != null && active.isFinished(stateTime)) {
                animLocked = false;
                animState = Helper.AnimState.MOVE_ANIM;
                stateTime = 0f;
                if (pendingState != null) {
                    setState(pendingState);
                    pendingState = null;
                }
            }
            return;
        }

        if (obstacle.getLinearVelocity().len2() > 0.1f) {
            updateDir(dt);
        } else {
            // Optional: Reset stateTime when idle if you want them to start
            // the flapping loop from the beginning when they move.
        }

        controlCooldown = Math.max(0, controlCooldown - 1);
        pickupCooldown = Math.max(0, pickupCooldown - 1);
        collisionUpdateCooldown = Math.max(0, collisionUpdateCooldown - 1);
        switchMovementCooldown = Math.max(0, switchMovementCooldown - 1);
        setTrueThisFrame = false;

        //update stun timer
        if (state == HelperFSMState.STUNNED) {
            stunTimer -= dt;
            stunFlashAccum += dt;
            if(stunFlashAccum >= stun_flashing_interval){
                stunVisible = !stunVisible;
                stunFlashAccum = 0f;
            }
            lockVelocity();
            if(stunTimer <= 0){
                state = HelperFSMState.IDLE;
                stunTimer = 0;
                stunVisible = true;
                stunFlashAccum = 0f;
                obstacle.setSensor(false);
            }
        }
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
     * Draws the sprite object handling texture.
     *
     * This method is overridden from ObstacleSprite. We need to select the texture from the animation frame based on direction and time in the animation.
     *
     * @param batch The sprite batch to draw to
     */
    @Override
    public void draw(SpriteBatch batch) {
        // get current animation frame
        if (anims == null) {
            throw new RuntimeException("No animations for helper type: " + type);
        }
        DirectionalAnimation active = getActiveAnim();
        if (active == null) active = anims.move; //safety
        TextureRegion frame = active.getFrame(currentDir, stateTime);
        //TextureRegion frame = anims.move.getFrame(currentDir, stateTime);


        // Flashes different color if stunned
        Color oldColor = batch.getColor();
        if (state == HelperFSMState.STUNNED) {
            if (!stunVisible) return; // Flash effect
            batch.setColor(Color.RED);
        }

        if (frame != null) {
            // Use the width and height of the Obstacle (scaled by units)
            // to force the animation to fit the physics body.
            float drawWidth = this.drawWidth * units;
            float drawHeight = this.drawHeight * units;


            batch.draw(frame,
                getXPos() - drawWidth / 2f,
                getYPos() - drawHeight / 2f,
                drawWidth,
                drawHeight);
        } //else {
        // Fallback to default texture if animation fails
        //batch.draw(defaultTexture, getXPos() - width*units/2, getYPos() - height*units/2, width*units, height*units);
        //}
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

    private Vector2 cache = new Vector2();
    private Vector2 edge1 = new Vector2();
    private Vector2 edge2 = new Vector2();

    /**
     * For raycasting when pathfinding. Call AFTER getting an edge.
     * @return offset from center
     */
    public Vector2 getOffset() {
        return cache;
    }

    /**
     * Get outer edge position of helper, for raycasting when pathfinding
     * @return outer edge pos
     */
    public Vector2 getEdge1() {
        cache.x = -(float)Math.sin(getOrientation());
        cache.y = (float)Math.cos(getOrientation());
        cache.set(cache.y, -cache.x); // perpendicular vector
        cache.nor();
        // cache.scl(radius);
        edge1.set(obstacle.getPosition().add(cache));
        return edge1;
    }

    /**
     * Get second outer edge position of helper, for raycasting when pathfinding
     * @return outer edge pos
     */
    public Vector2 getEdge2() {
        cache.x = -(float)Math.sin(getOrientation());
        cache.y = (float)Math.cos(getOrientation());
        cache.set(-cache.y, cache.x); // perpendicular vector
        cache.nor();
        // cache.scl(radius);
        edge2.set(obstacle.getPosition().add(cache));
        return edge2;
    }


    /**
     * update animation
     */
    public void updateDir(float dt) {
        Vector2 vel = obstacle.getLinearVelocity();

        //Ignore small movements to prevent flickering
        if (vel.len2() < 0.2f) return;

        // 2. Get angle (0 is East, 90 is North, 180 is West, 270 is South)
        float angle = vel.angleDeg();

        // 3. Normalize angle to be strictly between 0 and 360
        while (angle < 0) angle += 360;
        while (angle >= 360) angle -= 360;

        // 4. Use 45-degree slices centered on the direction
        // Adding 22.5 shifts the "slice" so East is 337.5 to 22.5
        int sector = (int)((angle + 22.5f) / 45f) % 8;

        switch (sector) {
            case 0: currentDir = Direction.E;  break;
            case 1: currentDir = Direction.NE; break;
            case 2: currentDir = Direction.N;  break;
            case 3: currentDir = Direction.NW; break;
            case 4: currentDir = Direction.W;  break;
            case 5: currentDir = Direction.SW; break;
            case 6: currentDir = Direction.S;  break;
            case 7: currentDir = Direction.SE; break;
        }
    }


    /**
     * Set new texture for the obstacle based on
     */
    public void setTexture(Texture newT){
        if (sprite == null) {
            sprite = new Sprite(newT);
        } else {
            sprite.setRegion(newT);
        }
    }

    /**
     * sets the animation for this helper based on level creating the helpers
     * @param anims
     */
    public void setAnimations(BugAnimations anims) {
        this.anims = anims;
    }


    /**
     * return helper type (color)
     */
    public BugType getType() {
        return type;
    }

    /**
     * return (x,y) Vector2 representing the where the ingredient should be offset to.
     */
    public Vector2 getDirection() {
        switch (currentDir) {
            case N:
                return new Vector2(0,-0.6f);
            case NE:
                return new Vector2(-1,-0.1f);
            case E:
                return new Vector2(-1,0.2f);
            case SE:
                return new Vector2(-1,1.3f);
            case S:
                return new Vector2(0,1.5f);
            case SW:
                return new Vector2(1,1.3f);
            case W:
                return new Vector2(1,0.2f);
            case NW:
                return new Vector2(1,-0.1f);
            default:
                return new Vector2(0,0); //just draw ingredient in center of bug body
        }
    }

    public void setAnimState(Helper.AnimState newState) {
        System.out.println("setAnimState called: " + animState + " -> " + newState + " locked=" + animLocked);
        if (animLocked) return;
        animState = newState;
        stateTime = 0f;
    }

    private DirectionalAnimation getActiveAnim() {
        return switch (animState) {
            case PICKUP_ANIM -> anims.pickup;
            case DEATH_ANIM -> anims.death;
            default -> anims.move;
        };
    }

    public void lockAnim() { animLocked = true; }

    public boolean isAnimLocked() {
        return animLocked;
    }
}

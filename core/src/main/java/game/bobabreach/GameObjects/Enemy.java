package game.bobabreach.GameObjects;

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.pfa.SmoothableGraphPath;
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
import game.bobabreach.WorldGraph;

/**
 * Model class for the enemies. Code borrowed from Lab 4.
 */
public class Enemy extends ObstacleSprite {
    /**
     * Enumeration to encode the finite state machine.
     */
    public enum EnemyFSMState {
        /**
         * The enemy is moving to its designated crate
         */
        PICKUP,
        /**
         * The enemy has stolen an ingredient and is moving back to its spawn
         */
        ESCAPE,
        /**
         * An attacker has spotted an enemy
         */
        ATTACK,
        /**
         * Enemy is moving around randomly
         */
        WANDER,
        /**
         * Enemy is currently doing nothing
         */
        IDLE
    }

    /**
     * Enumeration to encode enemy types
     */
    public enum EnemyType {
        STEALER,
        ATTACKER
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
    /** The width of the enemy avatar */
    private float width;
    /** The height of enemy avatar */
    private float height;
    private float stealerRadius;
    private float attackerRadius;
    /** Physics units we're using */
    private float units;
    private float stealerDrawWidth;
    private float stealerDrawHeight;
    private float attackerDrawWidth;
    private float attackerDrawHeight;
    private float stealerDeathDrawWidth;
    private float stealerDeathDrawHeight;
    private float attackerDeathDrawWidth;
    private float attackerDeathDrawHeight;
    private float attackerSpawnDrawWidth;
    private float attackerSpawnDrawHeight;
    private float stealerSpawnDrawWidth;
    private float stealerSpawnDrawHeight;

    /** The current state of the enemy (to be used by AI controller) */
    private EnemyFSMState state;
    /** The type of the enemy (to be used by AI controller) */
    private EnemyType type;

    /**
     * The BugType of the enemy (used for animations)
     */
    private BugType bugType;

    /**
     * current direction being faced
     * need to update each frame corresponding to movement
     */
    Direction currentDir;

    /**
     * store anim for this enemy
     */
    private BugAnimations anims;

    /**
     * time in a state/animation
     * increment each frame
     */
    float stateTime;

    public enum AnimState { MOVE_ANIM, PICKUP_ANIM, DEATH_ANIM, SPAWN_ANIM}

    private AnimState animState = AnimState.MOVE_ANIM;
    private boolean animLocked = false;

    private EnemyFSMState pendingState = null;

    public void setPendingState(EnemyFSMState state) {
        pendingState = state;
    }

    /**
     * flashes a sign when spots a helper target and gets into attack state
    */
    private Alert attackerWarning;


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

    /** Texture to use when not doing anything special. */
    private Texture defaultTexture;
    /** Stealer texture */
    private Texture stealerTexture;
    /** Attacker texture */
    private Texture attackerTexture;
    /** Texture to use when chasing helper */
    private Texture chaseTexture;

    // Wander stuff
    private int timesWandered;
    private WorldGraph.IndexNode wanderTarget;

    // Pathfinding stuff
    private SmoothableGraphPath<WorldGraph.IndexNode, Vector2> path;
    private int pathIndex;
    private boolean endNodeChanged;

    /** The integer index of the crate this bug wants to steal from. Starts at 1. */
    private int target;
    /** The integer index of the vent this bug wants to escape to. Starts at 0. */
    private int vent;
    /** The integer index of the helper this bug wants to attack. Starts at 0. */
    private int attackTarget;
    /** How far away (in world units) this enemy, if attacking, will chase a helper */
    private float aggroRange;
    /** How many frames an attacker won't stun a helper when it spawns */
    private int spawnForgiveness;
    /** Whether the enemy is touching their targeted crate. */
    private boolean isTouchingCrate;
    /** Whether the enemy is currently holding an ingredient*/
    private boolean hasIngredient;
    /** The IngredientType of the ingredient the enemy is holding.*/
    private IngredientType heldIngredient;
    /** How many frames the enemy should idle after reaching its wander target */
    private int idleDuration;
    /** How many frames frames left for the enemy to idle */
    private int idleTime;

    /** Enemies are slower while carrying ingredients. */
    private float carrySpeedMult;
    /** Attacker enemies are faster than usual. */
    private float attackerMult;

    private boolean markedForDestruction;

    private boolean isOnVent;

    private boolean hasAttacked;

    /**
     * Sets whether the enemy is currently on honey and applies slowdown
     */
    private boolean onHoney = false;
    private int numHoneyColliding = 0;
    private float honeySlowdownFactor;

    public boolean getIsOnVent() {return isOnVent;}

    public void setIsOnVent(boolean val) {isOnVent = val;}

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }
    public int getVent() {
        return vent;
    }

    public void setOnHoney(boolean onHoney) {
        numHoneyColliding = onHoney ? numHoneyColliding + 1 : numHoneyColliding - 1;
    }

    public boolean isOnHoney() {
        return onHoney;
    }

    public float getHoneySlowdownFactor() {
        return honeySlowdownFactor;
    }

    /**
     * Increase # of times this enemy has wandered to a point.
     */
    public void incTimesWandered() {
        timesWandered++;
    }

    /**
     * Get # of times enemy has wandered to a point.
     * @return # times wandered
     */
    public int getTimesWandered() {
        return timesWandered;
    }

    public WorldGraph.IndexNode getWanderTarget() {
        return wanderTarget;
    }

    public void setWanderTarget(WorldGraph.IndexNode wanderTarget) {
        path = null;
        pathIndex = 0;
        this.wanderTarget = wanderTarget;
        endNodeChanged = true;
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
     * Sets whether the enemy is touching their targeted crate
     * @param bool whether enemy is touching crate; called by CollisionController
     */
    public void setIsTouchingCrate(boolean bool) {
        isTouchingCrate = bool;
    }

    /**
     * Whether the enemy is touching their targeted crate
     * @return whether enemy is touching crate
     */
    public boolean isTouchingCrate() {
        return isTouchingCrate;
    }

    public boolean isDoneIdling() {
        return idleDuration <= 0;
    }

    /**
     * Whether the enemy can attack.
     * @return whether enemy can attack
     */
    public boolean canAttack() {
        return type == EnemyType.ATTACKER && spawnForgiveness >= 0;
    }

    public boolean hasAttacked() {return hasAttacked;}

    public void setHasAttacked(boolean val) {hasAttacked = val;}

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
        if (horizontal < 0 && vertical == 0) {
            facing = FaceDirection.LEFT;
        } else if (horizontal < 0 && vertical > 0) {
            facing = FaceDirection.UP_LEFT;
        } else if (horizontal < 0 && vertical < 0) {
            facing = FaceDirection.DOWN_LEFT;
        } else if (horizontal == 0 && vertical > 0) {
            facing = FaceDirection.UP;
        } else if (horizontal == 0 && vertical < 0) {
            facing = FaceDirection.DOWN;
        } else if (horizontal > 0 && vertical == 0) {
            facing = FaceDirection.RIGHT;
        } else if (horizontal > 0 && vertical > 0) {
            facing = FaceDirection.UP_RIGHT;
        } else if (horizontal > 0 && vertical < 0) {
            facing = FaceDirection.DOWN_RIGHT;
        }
    }

    /**
     * Stops ALL the enemy's current movement.
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
    public EnemyFSMState getState() {
        return state;
    }

    /**
     * Sets the current state of the enemy.
     * @param state new state
     */
    public void setState(EnemyFSMState state) {
        EnemyFSMState previous = this.state;
        this.state = state;
        path = null;
        pathIndex = 0;
        endNodeChanged = true;
        if (state == EnemyFSMState.IDLE) {
            lockVelocity();
            // movement = Vector2.Zero;
            setMovement(0, 0);
            idleDuration = idleTime;
        }
        /*
        if (state == EnemyFSMState.ESCAPE){
            // System.out.println("set enemy State to ESCAPE");
        }

         */
        if (state == EnemyFSMState.ATTACK && previous != EnemyFSMState.ATTACK) {
            attackerWarning.trigger();
        }
    }

    public EnemyType getType() {
        return type;
    }

    public SmoothableGraphPath<WorldGraph.IndexNode, Vector2> getPath() {
        return path;
    }
    public void setPath(SmoothableGraphPath<WorldGraph.IndexNode, Vector2> path) {
        this.path = path;
    }
    public int getPathIndex() {return pathIndex;}
    public void setPathIndex(int i) {pathIndex = i;}

    public boolean getEndNodeChanged() {
        return endNodeChanged;
    }

    public void setEndNodeChanged(boolean val) {
        endNodeChanged = val;
    }

    // Attack stuff

    /**
     * Set helper to target.
     */
    public void setAttackTarget(int target) {
        attackTarget = target;
    }

    /**
     * Get helper to target.
     */
    public int getAttackTarget() {
        return attackTarget;
    }

    public float getAggroRange() {
        return aggroRange;
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
    }

    /**
     * Removes the player's ingredient by setting hasIngredient to false
     */
    public void removeIngredient() {
        hasIngredient = false;
        heldIngredient = null;
    }

    /**
     * Returns the ingredient that the player is currently holding.
     * @return the IngredientType of the player's current held ingredient
     */
    public IngredientType getHeldIngredient(){
        return heldIngredient;
    }


    /**
     * Creates a new enemy with the given physics data
     * <p>
     * The physics units are used to size the mesh relative to the physics
     * body. All other attributes are defined by the JSON file. Because of
     * transparency around the image file, the physics object will be slightly
     * thinner than the mesh in order to give a tighter hitbox.
     *
     * @param units    The physics units
     * @param data     The physics constants for the enemy
      */
    public Enemy(float units, float x, float y, JsonValue data, int target, int vent, EnemyType t, BugType bugType, BugAnimations anims) {
        this.data = data;
        JsonValue debugInfo = data.get("debug");

        this.units = units;
        this.type = t;
        this.bugType = bugType;

        //this.anims = anims;
        this.currentDir = Direction.E;

        carrySpeedMult = data.getFloat("carry_speed_mult");
        attackerMult = data.getFloat("attacker_mult");

        timesWandered = 0;
        idleTime = data.getInt("idle_time");
        aggroRange = data.getFloat("aggro_range");
        spawnForgiveness = data.getInt("spawn_forgiveness");
        endNodeChanged = false;
        isOnVent = true;
        hasAttacked = false;

        float s = data.getFloat( "size" );
        float size = s*units;

        this.target = target;
        this.vent = vent;
        isTouchingCrate = false;



        // The capsule is smaller than the image
        // "inner" is the fraction of the original size for the capsule
        width = s*data.get("inner").getFloat(0);
        height = s*data.get("inner").getFloat(1);
        float radius = bugType == BugType.STEALERBUG ? s * data.getFloat("stealer radius") :
            s * data.getFloat("attacker radius");
        // stealerRadius = s * data.getFloat("stealer radius");
        // obstacle = new CapsuleObstacle(x, y, width, height);
        // obstacle = new CapsuleObstacle(x, y, height, width);
        obstacle = new WheelObstacle(x, y, radius);
        // ((CapsuleObstacle)obstacle).setTolerance( debugInfo.getFloat("tolerance", 0.5f) );
        ((WheelObstacle)obstacle).setTolerance( debugInfo.getFloat("tolerance", 0.5f) );

        obstacle.setDensity( data.getFloat( "density", 0 ) );
        obstacle.setFriction( data.getFloat( "friction", 0 ) );
        obstacle.setRestitution( data.getFloat( "restitution", 0 ) );
        obstacle.setSensor(true);
        obstacle.setFixedRotation(true);
        obstacle.setPhysicsUnits( units );
        obstacle.setUserData( this );
        obstacle.setName("enemy");
        obstacle.getFilterData().groupIndex = -2;
        obstacle.getFilterData().categoryBits = 0x0009;
        obstacle.getFilterData().maskBits = (short) ((0xFFFF & ~0x0006) & ~0x0008);

        debug = ParserUtils.parseColor( debugInfo.get("avatar"),  Color.WHITE);
        sensorColor = ParserUtils.parseColor( debugInfo.get("sensor"),  Color.WHITE);

        maxspeed = data.getFloat("maxspeed", 0);
        damping = data.getFloat("damping", 0);
        force = data.getFloat("force", 0);
        honeySlowdownFactor = data.getFloat("honey_mult", 1);
        movement = new Vector2();
        facing = FaceDirection.RIGHT;

        state = EnemyFSMState.IDLE;
        //state = EnemyFSMState.IDLE; //used to be WANDER, but need to be IDLE until the spawn animation finishes, and then it is set to wander or pickup
        markedForDestruction = false;

        stealerDrawWidth = data.getFloat("stealer_draw_width");
        stealerDrawHeight = data.getFloat("stealer_draw_height");
        attackerDrawWidth = data.getFloat("attacker_draw_width");
        attackerDrawHeight = data.getFloat("attacker_draw_height");

        stealerDeathDrawWidth = data.getFloat("stealer_death_draw_width");
        stealerDeathDrawHeight = data.getFloat("stealer_death_draw_height");
        attackerDeathDrawWidth = data.getFloat("attacker_death_draw_width");
        attackerDeathDrawHeight = data.getFloat("attacker_death_draw_height");

        stealerSpawnDrawWidth = data.getFloat("stealer_spawn_draw_width");
        stealerSpawnDrawHeight = data.getFloat("stealer_spawn_draw_height");
        attackerSpawnDrawWidth = data.getFloat("attacker_spawn_draw_width");
        attackerSpawnDrawHeight = data.getFloat("attacker_spawn_draw_height");
        // Create a rectangular mesh for the enemy.
        mesh.set(-size/2.0f,-size/2.0f,size,size);

        attackerWarning = new Alert(x, y, data.getFloat("size") * 0.4f, units, null, 2, units * 0.6f, 0.2f);

    }

    /**
     * Creates the sensor for the enemy.
     */
    public void createSensor() {
        float yOffset = bugType == BugType.STEALERBUG ? data.getFloat("stealer honey offset") :
            data.getFloat("attacker honey offset");
        Vector2 sensorCenter = new Vector2(0, yOffset);
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = data.getFloat("density",0);
        sensorDef.isSensor = true;

//        JsonValue sensorjv = data.get("sensor");
//        float w = sensorjv.getFloat("xFactor",0)*width;
//        float h = sensorjv.getFloat("yFactor",0)*height;
        float radius = bugType == BugType.STEALERBUG ? data.getFloat("stealer honey radius") :
            data.getFloat("attacker honey radius");
        CircleShape sensorShape = new CircleShape();
        // sensorShape.setAsBox(w, h, sensorCenter, 0.0f);
        sensorShape.setRadius(radius);
        sensorShape.setPosition(sensorCenter);
        sensorDef.shape = sensorShape;

        // Sensor to represent hitbox
        Body body = obstacle.getBody();
        Fixture sensorFixture = body.createFixture( sensorDef );
        sensorName = "enemy_honey_hitbox";
        sensorFixture.setUserData(sensorName);

        // Finally, we need a debug outline
        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
        sensorOutline = new Path2();
        factory.makeCircle( (sensorCenter.x-radius/2)*u,(sensorCenter.y-radius/2)*u, radius);
    }


    /**
     * Applies the force to the body of the enemy. RIGHT NOW,
     * just directly sets the enemy's linear velocity to its current
     * movement.
     *
     * This method should be called after the force attribute is set.
     */
    // Could refactor this into setMovement()
    public void applyForce() {
        if (animLocked) return; //for animation purposes
        if (!obstacle.isActive()) {
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
        else if (type == EnemyType.ATTACKER) {
            movement.scl(attackerMult);
        }
        if (numHoneyColliding >= 1) {
            movement.scl(honeySlowdownFactor);
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

        stateTime += dt;
        // System.out.println("update stateTime after increment=" + stateTime + " animLocked=" + animLocked);

        if (animLocked) {
            DirectionalAnimation active = getActiveAnim();
            if (active != null && active.isFinished(stateTime)) {
                AnimState finishedState = animState;
                animLocked = false;
                animState = AnimState.MOVE_ANIM;
                stateTime = 0f;
                if (finishedState == AnimState.DEATH_ANIM) {
                    markForDestruction();
                } else if (pendingState != null) {
                    setState(pendingState);
                    pendingState = null;
                }
                if (finishedState == AnimState.DEATH_ANIM) {
                    markForDestruction();
                }
                else if (finishedState == AnimState.SPAWN_ANIM) {
                    if (getBugType() == BugType.STEALERBUG) {
                        setState(
                            EnemyFSMState.PICKUP); // begin first FSM state for stealer = pickup
                    } else {
                        setState(
                            EnemyFSMState.WANDER); // begin first FSM state for attacker = wander
                    }
                }
                 else if (pendingState != null) {
                        setState(pendingState);
                        pendingState = null;
                    }

            }
            return;
        }

        if (obstacle.getLinearVelocity().len2() > 0.1f) {
            updateDir(dt);
        }
/*
        if (state == EnemyFSMState.ATTACK) {
            setTexture(chaseTexture);
        }
        else {
            setTexture(defaultTexture);
        }

 */
        endNodeChanged = false;
        idleDuration = Math.max(0, idleDuration - 1);
        spawnForgiveness = Math.max(0, spawnForgiveness - 1);

        //for the attacker warning sign
        attackerWarning.setPosition(obstacle.getX(), obstacle.getY());
        attackerWarning.update(dt);
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
     * This method is overridden from ObstacleSprite. Drawing specific texture
     * for the direction faced and frame in the animation cycle.
     *
     * @param batch The sprite batch to draw to
     */
    @Override
    public void draw(SpriteBatch batch) {

        DirectionalAnimation active = getActiveAnim();
        if (active == null) active = anims.move; //safety
        TextureRegion frame = active.getFrame(currentDir, stateTime);
        //TextureRegion frame = anims.move.getFrame(currentDir, stateTime);
        if (bugType ==  BugType.ATTACKERBUG && state == EnemyFSMState.IDLE && animState != AnimState.DEATH_ANIM && animState != AnimState.SPAWN_ANIM){ //attacker bug stops moving when idle, unless spawning
            frame = anims.move.getFrame(currentDir, 0);
        }


        if (frame != null) {
            // Use the width and height of the Obstacle (scaled by units)
            // to force the animation to fit the physics body.
            float drawWidth = width * units;
            float drawHeight = height * units;

            if (bugType == BugType.STEALERBUG) {
                if (animState == AnimState.DEATH_ANIM) {
                    drawWidth = stealerDeathDrawWidth * units;
                    drawHeight = stealerDeathDrawHeight * units;
                } else if (animState == AnimState.SPAWN_ANIM) {
                    drawWidth = stealerSpawnDrawWidth * units;
                    drawHeight = stealerSpawnDrawHeight * units;
                } else {
                    drawWidth = stealerDrawWidth * units;
                    drawHeight = stealerDrawHeight * units;
                }
            } else if (bugType == BugType.ATTACKERBUG) {
                if (animState == AnimState.DEATH_ANIM) {
                    drawWidth = attackerDeathDrawWidth * units;
                    drawHeight = attackerDeathDrawHeight * units;
                } else if (animState == AnimState.SPAWN_ANIM) {
                    drawWidth = attackerSpawnDrawWidth * units;
                    drawHeight = attackerSpawnDrawHeight * units;
                } else {
                    drawWidth = attackerDrawWidth * units;
                    drawHeight = attackerDrawHeight * units;
                }
            }
            float xOffset = 0f;
            float yOffset = 0f;
//            if (animState == AnimState.SPAWN_ANIM && bugType == BugType.ATTACKERBUG) {
//                xOffset = 0.05f * units;
//                yOffset = 0.16f * units;
//            }
//            if (animState == AnimState.SPAWN_ANIM && bugType == BugType.STEALERBUG) {
//                xOffset = 0 * units;
//                yOffset = -0.6f * units;
//            }

            batch.draw(frame,
                getXPos() - drawWidth / 2f + xOffset,
                getYPos() - drawHeight / 2f + yOffset,
                drawWidth,
                drawHeight);
        }// else {
            // Fallback to default texture if animation fails
            //batch.draw(defaultTexture, getXPos() - width*units/2, getYPos() - height*units/2, width*units, height*units);
        //}

        attackerWarning.draw(batch);

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

    /**
     * returns the width of this enemy in world units.
     * @return the width of the object in world units
     */
    public float getSize(){
        return mesh.computeBounds().getWidth();
    }

    public float getOrientation() {
        return obstacle.getAngle();
    }

    // Raycast stuff for enemies
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
     * return enemy type (attacker vs stealer)
     */
    public BugType getBugType() {
        return bugType;
    }

    /**
     * sets the animation for this enemy based on level creating the enemies
     * @param anims
     */
    public void setAnimations(BugAnimations anims) {
        this.anims = anims;
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

    public void setAnimState(AnimState newState) {
        System.out.println("setAnimState called: " + animState + " -> " + newState + " locked=" + animLocked);
        if (animLocked) return;
        animState = newState;
        stateTime = 0f;
    }

    private DirectionalAnimation getActiveAnim() {
        return switch (animState) {
            case PICKUP_ANIM -> anims.pickup != null ? anims.pickup : anims.move;
            case DEATH_ANIM -> anims.death != null ? anims.death : anims.move;
            case SPAWN_ANIM   -> anims.spawn   != null ? anims.spawn   : anims.move;

            default -> anims.move;
        };
    }

    public void lockAnim() { animLocked = true; }

    public boolean isAnimLocked() {
        return animLocked;
    }

    public void setWarningTexture(Texture texture) {
        attackerWarning.setTexture(texture);
    }

    public Alert getWarningSign() {
        return attackerWarning;
    }

    public void triggerDeath() {

        if (getAnimState() == AnimState.DEATH_ANIM) return; //interrupts any animation/state unless it is death animation
        animLocked = false;
        animState = AnimState.DEATH_ANIM; // bypass setAnimState's lock check
        animLocked = true;
        stateTime = 0f; // force reset even if stateTime is large
        setPendingKinematic();
        pendingState = null;
        removeIngredient(); //remove ingredient when the death is triggered
    }

    public void triggerSpawn() {
        if (getAnimState() == AnimState.DEATH_ANIM) return;
        animLocked = false;
        animState = AnimState.SPAWN_ANIM;
        animLocked = true;
        stateTime = 0f;
        // no pendingState needed — FSM starts after anim finishes via update()
    }


    public AnimState getAnimState() {
        return animState;
    }

    private boolean pendingKinematic = false;

    public void setPendingKinematic() {
        pendingKinematic = true;
    }

    public boolean hasPendingKinematic() {
        return pendingKinematic;
    }

    public void applyPendingKinematic() {
        if (pendingKinematic) {
            obstacle.getBody().setType(BodyDef.BodyType.KinematicBody);
            lockVelocity();
            pendingKinematic = false;
        }
    }

}

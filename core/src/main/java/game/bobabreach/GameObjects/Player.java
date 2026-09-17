package game.bobabreach.GameObjects;

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
import game.bobabreach.GameObjects.IngredientType;
import game.bobabreach.GameObjects.Crate;

import javax.swing.*;

/**
 * Model class for the player. Code borrowed from Lab 4.
 */
public class Player extends ObstacleSprite {
    /**
     * Where the player character can face?
     */
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
     * The initializing data (to avoid magic numbers)
     */
    private final JsonValue data;
    /**
     * The width of the player avatar
     */
    private float width;
    /**
     * The height of player avatar
     */
    private float height;
    /**
     * Physics units we're using
     */
    private float units;

    /**
     * The factor to multiply by the input
     */
    private float force;
    /**
     * The amount to slow the character down
     */
    private float damping;
    /**
     * The maximum character speed
     */
    private float maxspeed;

    /**
     * The current horizontal movement of the character
     */
    private float horizontal;
    /**
     * The current vertical movement of the character
     */
    private float vertical;
    /**
     * The overall movement of the character
     */
    private Vector2 movement;
    /**
     * Where the character is facing
     */
    private FaceDirection facing;

    /**
     * Cooldown (in animation frames) for shooting
     */
    private int shotLimit;
    /**
     * Cooldown (in animation frames) for switching btw. shooting and moving
     */
    private int switchLimit;
    /**
     * Cooldown (in animation frames) for switching btw. helper & Pearl
     */
    private int controlLimit;

    /**
     * Whether the player can enter shooting mode or not. Modified
     * by CollisionController
     */
    private boolean canEnterShooting;
    /**
     * Whether the player is in firing mode or not
     */
    private boolean isShootingMode;
    /**
     * Whether the player is actively shooting
     */
    private boolean isShooting;
    /**
     * Whether the player is actively switching modes
     */
    private boolean isSwitching;
    /**
     * How long until we can shoot again
     */
    private int shootCooldown;
    /**
     * How long until we can switch again
     */
    private int switchCooldown;

    /**
     * Whether this character is under player control
     */
    private boolean isControlled;
    /**
     * How long until we can take control again
     */
    private int controlCooldown;


    /** Whether or not the player is touching a crate*/
    private boolean canPickup;
    /**The crate the player is currently touching*/
    private Crate crateTouching;
    /** Whether the player is currently holding an ingredient*/
    private boolean hasIngredient;
    /** The IngredientType of the ingredient the player is holding.*/
    private IngredientType heldIngredient;
    /** Whether or not the player is touching a cup*/
    private boolean touchingCup;


    /** The outline of the sensor obstacle */

    private Path2 sensorOutline;
    /**
     * The debug color for the sensor
     */
    private Color sensorColor;
    /**
     * The name of the sensor fixture
     */
    private String sensorName;

    /**
     * Cache for internal force calculations
     */
    private final Vector2 forceCache = new Vector2();
    /**
     * Cache for the affine flip
     */
    private final Affine2 flipCache = new Affine2();


    /**
     * Returns the movement of this character.
     * <p>
     * This is the result of input times force.
     *
     * @return the movement of this character.
     */
    public Vector2 getMovement() {
        return movement;
    }

    /**
     * Sets how much and where this character should be moving.
     * <p>
     * This is the result of input times force.
     *
     * @param horizontal the left/right movement of this character.
     * @param vertical   the up/down movement of this character.
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
     * Stops ALL the player's current movement.
     */
    public void lockVelocity() {
        obstacle.setLinearVelocity(Vector2.Zero);
    }

    /**
     * Sets the player's position to a set of screen coordinates.
     *
     * @param x Screen coordinates of player's new x
     * @param y Screen coordinates of player's new y
     * @param y Screen coordinates of player's new y
     */
    public void setPosition(float x, float y) {
        obstacle.setPosition(x / units, y / units);
    }

    /**
     * Returns how much force to apply to get player moving
     * <p>
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
     * Returns whether the player is in firing mode or not
     *
     * @return whether player is in firing mode (otherwise, they are in moving mode)
     */
    public boolean getShootingMode() {
        return isShootingMode;
    }

    /**
     * Sets whether the player can ENTER firing mode or not (i.e. they are colliding w/ the slingshot).
     * Called by CollisionController
     *
     * @param val whether player can enter firing  or not
     */
    public void setCanEnterShooting(boolean val) {
        canEnterShooting = val;
    }

    /**
     * Returns whether the player can actually enter firing mode or not; also
     * takes cooldown into account.
     *
     * @return whether player can enter firing mode
     */
    public boolean canEnterShooting() {
        return canEnterShooting && switchCooldown <= 0;
    }

    /**
     * Returns whether the player can exit firing mode or not;
     * takes cooldown into account.
     *
     * @return whether player can exit firing mode
     */
    public boolean canExitShooting() {
        return switchCooldown <= 0;
    }


    /**
     * Sets whether player is in firing mode or not
     *
     * @param shooting Should player be in shooting mode or not?
     */
    public void setShootingMode(boolean shooting) {
        switchCooldown = switchLimit; // Set switch cooldown
        isShootingMode = shooting;
        if (shooting) {
            obstacle.setActive(false);
        } else {
            obstacle.setActive(true);
        }
    }

    /**
     * Whether the player is actively shooting from the slingshot
     *
     * @return Whether player is shooting
     */
    public boolean isShooting() {
        return getShootingMode() && isShooting && shootCooldown <= 0;
    }

    /**
     * Sets whether player is shooting from slingshot
     *
     * @param val Whether player is shooting
     */
    public void setShooting(boolean val) {
        isShooting = val;
    }

    /**
     * Sets whether the player is currently controlling this character.
     * @param val whether this character should now be under player control
     */
    public void setControlled(boolean val) {
        isControlled = val;
        controlCooldown = 0;
    }

    /**
     * Returns whether this character is under player control
     * @return whether Pearl is under player control
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
     * Returns the name of the player hitbox
     * <p>
     * This is used by the ContactListener. Because we do not associate the
     * sensor with its own obstacle,
     *
     * @return the name of the ground sensor
     */
    public String getSensorName() {
        return sensorName;
    }

    /**
     * Sets whether the player is currently able to pick up an ingredient
     * from a crate, controlled by collision detection (whether the player
     * is colliding with an ingredient crate)
     *
     * @param val true if the player is colliding with ingredient crate, false otherwise
     */
    public void setCanPickup(boolean val) { canPickup = val; }

    /**
     * Returns whether the player is currently able to pick up an ingredient.
     *
     * The player can pick up an ingredient only if it is colliding with a crate AND if
     * they are not already holding an ingredient.
     *
     * @return true if the player can pick up an ingredient, false otherwise
     */
    public boolean canPickupIngredient() {
        return canPickup && !hasIngredient;
    }

    /**
     * returns the width of this player in world units.
     * @return the width of the object in world units
     */
    public float getSize(){
        return mesh.computeBounds().getWidth();
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
     * Sets whether or not the player is currently touching a cup.
     * This value is controlled by collision detection.
     *
     * @param val true if the player is colliding with the cup, false otherwise.
     */
    public void setTouchingCup(boolean val) { touchingCup = val; }

    /**
     * Returns whether the player is currently touching a cup.
     *
     * @return true if the player is colliding with a cup, false otherwise.
     */
    public boolean getTouchingCup() { return touchingCup; }

    /**
     * Returns the crate that this player is colliding with.
     * @return the Crate object that this player is colliding with
     */
    public Crate getCrateTouching() { return crateTouching; }

    /**
     * Sets crateTouching to a specific Crate object that the player is colliding with.
     * @param crate the Crate object the player is colliding with
     */
    public void setCrateTouching(Crate crate) { crateTouching = crate; }


    /**
     * Creates a new player avatar with the given physics data
     * <p>
     * The physics units are used to size the mesh relative to the physics
     * body. All other attributes are defined by the JSON file. Because of
     * transparency around the image file, the physics object will be slightly
     * thinner than the mesh in order to give a tighter hitbox.
     *
     * @param units The physics units
     * @param data  The physics constants for the player
     */
    public Player(float units, JsonValue data) {
        this.data = data;
        JsonValue debugInfo = data.get("debug");

        this.units = units;
        float x = data.get("pos").getFloat(0);
        float y = data.get("pos").getFloat(1);
        float s = data.getFloat("size");
        float size = s * units;

        // The capsule is smaller than the image
        // "inner" is the fraction of the original size for the capsule
        width = s * data.get("inner").getFloat(0);
        height = s * data.get("inner").getFloat(1);
        obstacle = new CapsuleObstacle(x, y, width, height);
        ((CapsuleObstacle) obstacle).setTolerance(debugInfo.getFloat("tolerance", 0.5f));

        shotLimit = data.getInt("shot_cool", 0);
        shootCooldown = 1;
        switchLimit = data.getInt("switch_cool", 0);
        controlLimit = data.getInt("control_cool", 0);
        isControlled = true;

        obstacle.setDensity(data.getFloat("density", 0));
        obstacle.setFriction(data.getFloat("friction", 0));
        obstacle.setRestitution(data.getFloat("restitution", 0));
        obstacle.setFixedRotation(true);
        obstacle.setPhysicsUnits(units);
        obstacle.setUserData(this);
        obstacle.setName("player");
        obstacle.setSensor(true);

        debug = ParserUtils.parseColor(debugInfo.get("avatar"), Color.WHITE);
        sensorColor = ParserUtils.parseColor(debugInfo.get("sensor"), Color.WHITE);

        maxspeed = data.getFloat("maxspeed", 0);
        damping = data.getFloat("damping", 0);
        force = data.getFloat("force", 0);
        movement = new Vector2();
        facing = FaceDirection.RIGHT;

        // Create a rectangular mesh for the player.
        mesh.set(-size / 2.0f, -size / 2.0f, size, size);
    }

    /**
     * Creates the sensor for the player.
     */
    public void createSensor() {
        Vector2 sensorCenter = new Vector2(0, 0);
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = data.getFloat("density", 0);
        sensorDef.isSensor = true;

        JsonValue sensorjv = data.get("sensor");
        float w = sensorjv.getFloat("xFactor", 0) * width;
        float h = sensorjv.getFloat("yFactor", 0) * height;
        PolygonShape sensorShape = new PolygonShape();
        sensorShape.setAsBox(w, h, sensorCenter, 0.0f);
        sensorDef.shape = sensorShape;

        // Sensor to represent hitbox
        Body body = obstacle.getBody();
        Fixture sensorFixture = body.createFixture(sensorDef);
        sensorName = "player_hitbox";
        sensorFixture.setUserData(sensorName);

        // Finally, we need a debug outline
        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
        sensorOutline = new Path2();
        factory.makeRect((sensorCenter.x - w / 2) * u, (sensorCenter.y - h / 2) * u, w * u, h * u, sensorOutline);
    }


    /**
     * Applies the force to the body of the player. RIGHT NOW,
     * just directly sets the player's linear velocity to its current
     * movement.
     * <p>
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

    /**
     * Gets the X position of the player in screen coordinates.
     *
     * @return X position of the player in screen coordinates
     */
    public float getXPos() {
        return obstacle.getX() * units;
    }

    /**
     * Gets the Y position of the player in screen coordinates.
     *
     * @return Y position of the player in screen coordinates
     */
    public float getYPos() {
        return obstacle.getY() * units;
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
        switch (facing) {
            case UP:
                obstacle.setAngle(0);
                break;
            case UP_LEFT:
                obstacle.setAngle((float) Math.PI / 4);
                break;
            case LEFT:
                obstacle.setAngle((float) Math.PI / 2);
                break;
            case DOWN_LEFT:
                obstacle.setAngle((float) Math.PI / 4 * 3);
                break;
            case DOWN:
                obstacle.setAngle((float) Math.PI);
                break;
            case DOWN_RIGHT:
                obstacle.setAngle(-(float) Math.PI / 4 * 3);
                break;
            case RIGHT:
                obstacle.setAngle(-(float) Math.PI / 2);
                break;
            case UP_RIGHT:
                obstacle.setAngle(-(float) Math.PI / 4);
                break;
            default:
                obstacle.setAngle(0);
                break;
        }
        // Apply cooldowns
        if (isControlled && isShooting()) {
            shootCooldown = shotLimit;
        } else {
            shootCooldown = Math.max(0, shootCooldown - 1);
        }

        switchCooldown = Math.max(0, switchCooldown - 1);
        controlCooldown = Math.max(0, controlCooldown - 1);

        super.update(dt);
    }


    /**
     * Draws the physics object.
     * <p>
     * This method is overridden from ObstacleSprite. We need to rotate the
     * texture depending on the player's movement. We do that by creating
     * a reflection affine transform.
     *
     * @param batch The sprite batch to draw to
     */
    @Override
    public void draw(SpriteBatch batch) {
        // Hide player model if they're shooting
        if (!isControlled) {
            batch.setColor(Color.GRAY);
        }
        if (!isShootingMode) {
            super.draw(batch, flipCache);
        }
        batch.setColor(Color.WHITE);
    }

    /**
     * Draws the outline of the physics object.
     * <p>
     * This method is overridden from ObstacleSprite. By default, that method
     * only draws the outline of the main physics obstacle. We also want to
     * draw the outline of the sensor, and in a different color. Since it
     * is not an obstacle, we have to draw that by hand.
     *
     * @param batch The sprite batch to draw to
     */
    @Override
    public void drawDebug(SpriteBatch batch) {
        super.drawDebug(batch);

        if (sensorOutline != null) {
            batch.setTexture(Texture2D.getBlank());
            batch.setColor(sensorColor);

            Vector2 p = obstacle.getPosition();
            float a = obstacle.getAngle();
            float u = obstacle.getPhysicsUnits();

            // transform is an inherited cache variable
            transform.idt();
            transform.preRotate((float) (a * 180.0f / Math.PI));
            transform.preTranslate(p.x * u, p.y * u);

            //
            batch.outline(sensorOutline, transform);
        }
    }
}

package game.bobabreach.GameObjects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import game.bobabreach.BugAnimations;
import game.bobabreach.Direction;
import game.bobabreach.SlingshotAnimations;

/**
 * A rectangular obstacle. Taken from Lab 4.
 * <p>
 * This class represents the slingshot the player can shoot from.
 */
public class Slingshot extends ObstacleSprite {

    /**
     * Physics units we're using, to convert back to screen coordinates
     */
    private float units;

    /**
     * Our rotation in degrees, for drawing purposes
     */
    private float rotation;

    /**
     * The factor to multiply by the input
     */
    private float force;

    /**
     * Cache for the affine rotation
     */
    private final Affine2 flipCache = new Affine2();

    /**
     * The overall movement of the slingshot (up/down)
     */
    private final Vector2 movement = new Vector2();

    /**
     * If the slingshot cannot move up anymore
     */
    private boolean isAtTop;

    /**
     * If the slingshot cannot move down anymore
     */
    private boolean isAtBottom;

    //for animation
    /**
     * time in a state/animation
     * increment each frame
     */
    float stateTime;


    /**
     * store anim for this helper
     */
    private SlingshotAnimations anims;

    /**
     * state used for animation
     */
    public enum SlingshotAnimState {
        IDLE, SHOOT
    }

    private SlingshotAnimState animState = SlingshotAnimState.IDLE; //start idle
    private boolean shootLocked = false;

    // store the angle at shoot-start, so it doesn't change mid-animation
    //angle is NOT in [0,180], will be converted when getting texture frame
    private float currentAngle = 0f; // store the angle at shoot-start, so it doesn't change mid-animation

    private int startFrame = 0;

    private JsonValue settings;


    /**
     * Creates a box with the given physics units and settings
     * <p>
     * The physics units are used to size the mesh relative to the physics
     * body. All other attributes are defined by the JSON file
     *
     * @param units    The physics units
     * @param settings The slingshot physics constants
     */
    public Slingshot(float units, JsonValue settings) {
        super();

        float x = settings.get("position").getFloat(0);
        float y = settings.get("position").getFloat(1);
        float s = settings.getFloat("size");
        float size = s * units;
        this.units = units;

        rotation = 0;
        isAtTop = false;
        isAtBottom = false;
        this.settings = settings;

        obstacle = new BoxObstacle(x, y, s, s);
        obstacle.setBodyType(BodyDef.BodyType.DynamicBody);
        obstacle.setSensor(true);
        // obstacle.getFilterData().maskBits = 0x0002;
        obstacle.getFilterData().categoryBits = 0x0008;
        obstacle.getFilterData().maskBits = (short) ((0xFFFF & ~0x0005) & ~0x0003
         & ~0x0006 & ~0x0009); // don't register collisions w/ boba or helpers
        obstacle.setFixedRotation(true);
        obstacle.setDensity(settings.getFloat("density", 1000));
        obstacle.setFriction(settings.getFloat("friction", 0));
        obstacle.setRestitution(settings.getFloat("restitution", 0));
        obstacle.setPhysicsUnits(units);
        obstacle.setUserData(this);
        obstacle.setName("slingshot");

        force = settings.getFloat("force", 0);

        debug = ParserUtils.parseColor(settings.get("debug"), Color.WHITE);

        // Create a rectangular mesh the same size as the slingshot, adjusted by
        // the physics units. For all meshes attached to a physics body, we
        // want (0,0) to be in the center of the mesh. So the method call below
        // is (x,y,w,h) where x, y is the bottom left.
        mesh.set(-size / 2.0f, -size / 2.0f, size, size);
    }

    public void adjustFix() {
        float hitboxYOffset = settings.getFloat("hitbox y offset");
        Vector2 sensorCenter = new Vector2(0, hitboxYOffset);
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = 1;
        // sensorDef.filter.maskBits = 0x0002;
        sensorDef.filter.categoryBits = 0x0008;
        // sensorDef.filter.maskBits = (short) ((0xFFFF & ~0x0005) & ~0x0003);
        sensorDef.isSensor = true;

        PolygonShape sensorShape = new PolygonShape();
        float s = settings.getFloat("hitbox size");
        sensorShape.setAsBox(s, s, sensorCenter, 0.0f);
        sensorDef.shape = sensorShape;

        // Sensor to represent hitbox
        Body body = obstacle.getBody();
        Fixture sensorFixture = body.createFixture(sensorDef);
        String sensorName = "slingshot_hitbox";
        sensorFixture.setUserData(sensorName);

        // Finally, we need a debug outline
        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
        Path2 sensorOutline = new Path2();
        factory.makeRect((sensorCenter.x - s / 2) * u, (sensorCenter.y - s / 2) * u, s * u, s * u, sensorOutline);
    }

    //angle NOT yet in [0,180]; use shootDirection.angleDeg() input
    public void startShooting(float angle, int startFrame) {

        if (shootLocked) return; // already shooting
        animState = SlingshotAnimState.SHOOT;
        shootLocked = true;
        currentAngle = angle;
        stateTime = 0f;
        this.startFrame = startFrame;

        //System.out.println("startShooting called, angle=" + angle + ", shoot anim null? " + (anims.shoot == null));

    }

    /**
     * Returns how much force to apply to get slingshot moving
     * <p>
     * Multiply this by the input to get the movement value.
     *
     * @return how much force to apply to get slingshot moving
     */
    public float getForce() {
        return force;
    }

    /**
     * Sets how much and where this character should be moving.
     * <p>
     * This is the result of input times force.
     *
     * @param vertical the up/down movement of this slingshot.
     */
    public void setMovement(float vertical) {
        movement.x = 0;
        movement.y = vertical;
    }

    /**
     * Sets whether the slingshot cannot move up anymore
     *
     * @param val whether the slingshot cannot move up anymore
     */
    public void setAtTop(boolean val) {
        isAtTop = val;
    }

    public boolean getAtTop() { return isAtTop; }

    /**
     * Sets whether the slingshot cannot move down anymore
     *
     * @param val whether the slingshot cannot move down anymore
     */
    public void setAtBottom(boolean val) {
        isAtBottom = val;
    }

    public boolean getAtBottom() { return isAtBottom; }

    /**
     * Applies the force to the slingshot. RIGHT NOW,
     * just directly sets the slingshot's linear velocity to its current
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

        if (isAtTop && movement.y > 0) {
            body.setLinearVelocity(0, 0);
            return;
        } else if (isAtBottom && movement.y < 0) {
            body.setLinearVelocity(0, 0);
            return;
        }
        body.setLinearVelocity(movement.x, movement.y);
    }

    /**
     * Stops ALL the player's current movement.
     */
    public void lockVelocity() {
        obstacle.setLinearVelocity(Vector2.Zero);
    }

    /**
     * Gets the X position of the slingshot in screen coordinates.
     * MAY NEED FOR AI CONTROLLER (if they're stealing boba from here)
     *
     * @return X position of the slingshot in screen coordinates
     */
    public float getXPos() {
        return obstacle.getX() * units;
    }

    /**
     * Gets the Y position of the slingshot in screen coordinates.
     *
     * @return Y position of the slingshot in screen coordinates
     */
    public float getYPos() {
        return obstacle.getY() * units;
    }

    /**
     * Sets the rotation of the object in degrees, for drawing
     *
     * @param deg The angle to rotate to
     */
    public void setRotation(float deg) {
        rotation = deg;
    }

    /**
     * sets slingshot animations (of all states) to the slingshot
     */
    public void setAnimations(SlingshotAnimations anims) {
        this.anims = anims;
    }

    @Override
    public void draw(SpriteBatch batch) {

        if (anims == null) {
            throw new RuntimeException("No animations for slingshot");
        }
        TextureRegion frame;

        //System.out.println("draw: animState=" + animState + ", shootLocked=" + shootLocked);

        if (animState == SlingshotAnimState.SHOOT && anims.shoot != null) {
            frame = anims.shoot.getFrame(currentAngle, stateTime, startFrame);
        } else {
            frame = anims.idle.getFrame(rotation);
        }

        if (frame == null) return;
        float yOffset = 0.8f * units;
        batch.draw(
            frame,
            getXPos() - frame.getRegionWidth() / 2f,
            getYPos() - frame.getRegionHeight() / 2f - yOffset
        );
        //super.draw(batch, flipCache);
    }

    @Override
    public void update(float dt) {
        stateTime += dt;
//        if (shootLocked && anims.shoot != null) {
//            System.out.println("shoot stateTime=" + stateTime + ", isFinished=" + anims.shoot.isFinished(stateTime, currentAngle, startFrame));
//        }
        if (shootLocked && anims.shoot != null && anims.shoot.isFinished(stateTime, currentAngle, startFrame)) {
            shootLocked = false;
            animState = SlingshotAnimState.IDLE;
            stateTime = 0f;
            startFrame = 0;
        }
    }
}

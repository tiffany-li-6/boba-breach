package game.bobabreach.GameObjects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.CapsuleObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.assets.ParserUtils;
import game.bobabreach.BugAnimations;
import game.bobabreach.Direction;

/**
 * Blocker bug implemented as a temporary moving platform that travels vertically across the screen.
 */
public class Blocker extends ObstacleSprite {

    /** Whether blocker should be destroyed */
    private boolean markedForRemoval;

    private boolean isCollidingHelper;

    /** Movement speed */
    private float speed;

    /** What Y-coordinate to delete this bug at */
    private float deleteThreshold;

    /** Movement speed vector */
    private Vector2 speedCache;

    /** The width of the enemy hitbox */
    private float width;
    /** The height of enemy hitbox */
    private float height;

    private float drawWidth;
    private float drawHeight;
    private float drawXOffset;

    private float units;


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

    /**
     * Whether the blocker is currently on honey
     */
    private boolean onHoney = false;

    private int numHoneyColliding = 0;

    /**
     * Speed multiplier when on honey (0.0 = fully stopped, 1.0 = no slowdown)
     */
    private float honeySlowdownFactor;

    public Blocker(float units, JsonValue settings, float xPos, BugAnimations anims) {

        this.units = units;
        this.width = settings.getFloat("width");
        this.height = settings.getFloat("height");
        drawWidth = settings.getFloat("draw_width");
        drawHeight = settings.getFloat("draw_height");
        drawXOffset = settings.getFloat("draw_x_offset");
        this.currentDir = Direction.N;
        float speed = settings.getFloat("speed");
        honeySlowdownFactor = settings.getFloat("honey_mult");
        deleteThreshold = settings.getFloat("delete threshold");
        speedCache = new Vector2();

        obstacle = new CapsuleObstacle(xPos, -height, width, height);

        obstacle.setDensity(settings.getFloat("density",1));
        obstacle.setFriction(settings.getFloat("friction",0));
        obstacle.setRestitution(settings.getFloat("restitution",1));

        obstacle.setBodyType(BodyDef.BodyType.DynamicBody);
        obstacle.setFixedRotation(true);
        obstacle.setSensor(true);
        obstacle.setPhysicsUnits(units);
        obstacle.setName("blocker");
        obstacle.setUserData(this);
        obstacle.getFilterData().groupIndex = -2;
        obstacle.getFilterData().categoryBits = 0x0004;
        obstacle.getFilterData().maskBits = 0x0003; // Only collide w/ helper, honey

        float radiusX = width * units / 2f;
        float radiusY = height * units / 2f;

        mesh.set(-radiusX, -radiusY, radiusX*2, radiusY*2);

        this.speed = speed;

    }


    /**
     * Used for detecting collision w/ honey, since the obstacle itself is a kinematic body
     * (doesn't collide w/ static bodies)
     */
    public void createSensor() {
        Vector2 sensorCenter = new Vector2(0, 0);
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = 0;
        sensorDef.restitution = 1;
        sensorDef.friction = 0;
        // sensorDef.isSensor = true;

//        JsonValue sensorjv = data.get("sensor");
//        float w = sensorjv.getFloat("xFactor",0)*width;
//        float h = sensorjv.getFloat("yFactor",0)*height;

        Shape sensorShape = obstacle.getBody().getFixtureList().get(0).getShape();
        sensorDef.shape = sensorShape;
        sensorDef.filter.categoryBits = 0x0006;
        sensorDef.filter.maskBits = 0x0005; // Only collide w/ boba

        // Sensor to represent hitbox
        Body body = obstacle.getBody();
        Fixture sensorFixture = body.createFixture( sensorDef );
        String sensorName = "blocker_phys_hitbox";
        sensorFixture.setUserData(sensorName);

        // Finally, we need a debug outline
        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
        Path2 sensorOutline = new Path2();
        factory.makeRect((sensorCenter.x - width / 2) * u, (sensorCenter.y - height / 2) * u,
            width * u, height * u, sensorOutline);

    }



    public void updateMovement() {
        speedCache.set(0, speed);
        obstacle.setLinearVelocity(speedCache);
    }

    public void markForRemoval() {
        markedForRemoval = true;
    }

    public boolean getMark() {
        return markedForRemoval;
    }

    public void setCollidingHelper(boolean val) {
        this.isCollidingHelper = val;
    }

    @Override
    public void update(float dt) {

        stateTime += dt;
        if (obstacle.getLinearVelocity().len2() > 0.1f) {
            updateDir(dt);
        }

        speedCache.set(0, speed);
        if (numHoneyColliding >= 1) {
            speedCache.scl(honeySlowdownFactor);
        }
        obstacle.setLinearVelocity(speedCache);

        // Apply honey slowdown if blocker is on honey
        // applyHoneySlowdown();

        // System.out.println(obstacle.getY());
        if (obstacle.getY() >= deleteThreshold) {
            markedForRemoval = true;
        }
//        if (isCollidingHelper) {
//            obstacle.setLinearVelocity(Vector2.Zero);
//        }
//        else {
//            obstacle.setLinearVelocity(new Vector2(0, speed));
//        }
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

        TextureRegion frame = anims.move.getFrame(currentDir, stateTime);

        /*
        System.out.println("Drawing enemy id=" + System.identityHashCode(this)
            + " bugType=" + bugType
            + " anims.move hashcode=" + System.identityHashCode(anims.move));

         */

        if (frame != null) {
            // Use the width and height of the Obstacle (scaled by units)
            // to force the animation to fit the physics body.
            float drawWidth = this.drawWidth * units;
            float drawHeight = this.drawHeight * units;

            batch.draw(frame,
                getXPos() - drawWidth / 2f + drawXOffset * units,
                getYPos() - drawHeight / 2f,
                drawWidth,
                drawHeight);
        }// else {
        // Fallback to default texture if animation fails
        //batch.draw(defaultTexture, getXPos() - width*units/2, getYPos() - height*units/2, width*units, height*units);
        //}
        batch.setColor(Color.WHITE);
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
     * Sets whether the blocker is currently on honey and applies slowdown
     * @param onHoney whether the blocker is on honey
     */
    public void setOnHoney(boolean onHoney) {
        numHoneyColliding = onHoney ? numHoneyColliding + 1 : numHoneyColliding - 1;
    }

    /**
     * Returns whether the blocker is currently on honey
     * @return true if on honey, false otherwise
     */
    public boolean isOnHoney() {
        return onHoney;
    }

    /**
     * Gets the speed multiplier for this blocker when on honey
     * @return the slowdown factor (0.0 to 1.0)
     */
    public float getHoneySlowdownFactor() {
        return honeySlowdownFactor;
    }

    /**
     * Applies honey slowdown to the blocker's velocity
     */
    private void applyHoneySlowdown() {
        if (isOnHoney()) {
            float slowdownFactor = getHoneySlowdownFactor();
            Vector2 velocity = obstacle.getLinearVelocity();
            velocity.scl(slowdownFactor);
            obstacle.setLinearVelocity(velocity);
        }
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
     * sets the animation for this enemy based on level creating the enemies
     * @param anims
     */
    public void setAnimations(BugAnimations anims) {
        this.anims = anims;
    }
}

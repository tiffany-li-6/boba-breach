package game.bobabreach.GameObjects;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.physics2.*;

/**
 * Model class for portal. One-way teleporter: a projectile entering an
 * "entry" portal is emitted from its linked "exit" portal along the exit
 * portal's facing direction, regardless of the original travel angle.
 * Exit portals are solid from the world side, so projectiles approaching
 * them from outside bounce off rather than teleporting back.
 *
 * Facing is restricted to the four cardinal directions; see {@link Direction}.
 */
public class Portal extends ObstacleSprite {

    /**
     * Cardinal direction a portal faces. The (x, y) values are unit vectors
     * in world space pointing in the named direction.
     */
    public enum Direction {
        LEFT (-1,  0),
        RIGHT( 1,  0),
        UP   ( 0,  1),
        DOWN ( 0, -1);

        public final float x;
        public final float y;

        Direction(float x, float y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Parses a direction from a JSON string. Case-insensitive.
         *
         * @param s one of "left", "right", "up", "down"
         * @return the matching Direction
         * @throws IllegalArgumentException if {@code s} is null or unrecognized
         */
        public static Direction fromString(String s) {
            if (s == null) {
                throw new IllegalArgumentException("Portal direction is null");
            }
            switch (s.toLowerCase()) {
                case "left":  return LEFT;
                case "right": return RIGHT;
                case "up":    return UP;
                case "down":  return DOWN;
                default:
                    throw new IllegalArgumentException(
                        "Unknown portal direction: " + s);
            }
        }
    }

    /** Cooldown time (in seconds) after teleporting to prevent re-entry. */
    private static final float TELEPORT_COOLDOWN = 0.3f;

    /** How far outside the portal center to spawn exiting projectiles
     *  (world units). Must exceed half the portal thickness so the
     *  spawned projectile does not start inside the solid exit fixture. */
    private static final float EXIT_OFFSET = 0.5f;

    /** The portal this one is linked to (the exit, if this is an entry). */
    private Portal linkedPortal;

    /** Whether this portal accepts incoming projectiles. Entry portals
     *  are sensors; exit portals are solid bodies. Fixed at construction
     *  because it determines fixture setup. */
    private final boolean canEnter;

    /** Unit vector pointing in the direction projectiles exit this portal.
     *  Always one of the four cardinal directions. */
    private final Vector2 facing = new Vector2(1, 0);

    /** How long until this portal can teleport again (entry side only). */
    private float teleportCooldown = 0;

    /** The physics units for coordinate conversion. */
    private float units;

    /**
     * Creates a new portal.
     *
     * @param x          X-position in world units
     * @param y          Y-position in world units
     * @param width      Width of the portal hitbox (world units). Choose
     *                   this to match the portal's orientation: short along
     *                   the facing axis, long perpendicular to it.
     * @param height     Height of the portal hitbox (world units)
     * @param drawWidth  Width of the rendered sprite (world units). May
     *                   differ from the hitbox to make portals appear
     *                   larger than the area that actually triggers teleport.
     * @param drawHeight Height of the rendered sprite (world units)
     * @param units      Physics units (screen-to-world conversion)
     * @param facing     Cardinal direction projectiles exit this portal
     * @param canEnter   true to make this an entry portal (sensor),
     *                   false to make it an exit-only portal (solid)
     */
    public Portal(float x, float y, float width, float height,
                  float drawWidth, float drawHeight, float units,
                  Direction facing, boolean canEnter) {
        this.units = units;
        this.canEnter = canEnter;
        // Direction (x, y) are already unit vectors.
        this.facing.set(facing.x, facing.y);

        // Create a box obstacle for the portal hitbox
        obstacle = new BoxObstacle(x, y, width, height);
        // StaticBody: portals are level geometry and must never be pushed
        // around by collisions with helpers, enemies, or boba.
        obstacle.setBodyType(BodyDef.BodyType.StaticBody);
        obstacle.setDensity(0);
        obstacle.setFriction(0);
        obstacle.setRestitution(0);
        obstacle.setFixedRotation(true);
        obstacle.setPhysicsUnits(units);
        obstacle.setUserData(this);
        obstacle.setName("portal");
        obstacle.getFilterData().categoryBits = 0x0010;
        obstacle.getFilterData().maskBits = (short) (0xFFFF & ~0x0008);

        // Entry: sensor (pass-through, fires contact). Exit: solid (bounces).
        obstacle.setSensor(canEnter);

        // Mesh uses draw size, independent of hitbox size.
        float drawSize = Math.max(drawWidth, drawHeight) * units;
        mesh.set(-drawSize / 2, -drawSize / 2, drawSize, drawSize);
    }

    /**
     * Convenience constructor for portals whose hitbox and sprite are the
     * same size. Equivalent to calling the full constructor with
     * {@code drawWidth = width} and {@code drawHeight = height}.
     */
    public Portal(float x, float y, float width, float height, float units,
                  Direction facing, boolean canEnter) {
        this(x, y, width, height, width, height, units, facing, canEnter);
    }

    /**
     * Links this portal to another portal. For one-way behavior, only call
     * this on the entry portal pointing at the exit; do not link the exit
     * back to the entry.
     *
     * @param other The portal to link to
     */
    public void setLinkedPortal(Portal other) {
        this.linkedPortal = other;
    }

    /**
     * Gets the linked portal.
     *
     * @return The portal this one connects to
     */
    public Portal getLinkedPortal() {
        return linkedPortal;
    }

    /**
     * Returns whether this portal accepts incoming projectiles.
     *
     * @return true if this is an entry portal (sensor), false if exit-only (solid)
     */
    public boolean canEnter() {
        return canEnter;
    }

    /**
     * Returns the (read-only) facing unit vector. Do not mutate.
     *
     * @return unit vector pointing in the direction projectiles exit
     */
    public Vector2 getFacing() {
        return facing;
    }

    /**
     * Writes the spawn position for an outgoing projectile into {@code out}.
     * The point is offset from the portal center along the facing direction
     * so the projectile appears just outside the (solid) exit body.
     *
     * @param out vector to receive the result
     * @return {@code out}, for chaining
     */
    public Vector2 getExitPosition(Vector2 out) {
        return out.set(facing).scl(EXIT_OFFSET)
            .add(obstacle.getX(), obstacle.getY());
    }

    /**
     * Returns whether this portal can teleport right now.
     *
     * @return true if cooldown has expired, false otherwise
     */
    public boolean canTeleport() {
        return teleportCooldown <= 0;
    }

    /**
     * Activates the teleport cooldown to prevent re-entry.
     */
    public void activateCooldown() {
        teleportCooldown = TELEPORT_COOLDOWN;
    }

    /**
     * Gets the X position of this portal in world units.
     *
     * @return X position in world units
     */
    public float getX() {
        return obstacle.getX();
    }

    /**
     * Gets the Y position of this portal in world units.
     *
     * @return Y position in world units
     */
    public float getY() {
        return obstacle.getY();
    }

    /**
     * Gets the physics units for coordinate conversion.
     *
     * @return the physics units
     */
    public float getPhysicsUnits() {
        return units;
    }

    /**
     * Draws the portal. Entry portals tint orange, exit portals tint blue,
     * to make pairing obvious during play and debugging.
     *
     * @param batch The sprite batch to draw to
     */
    @Override
    public void draw(SpriteBatch batch) {
        if (canEnter) {
            batch.setColor(1.0f, 0.647f, 0.0f, 1.0f);
        } else {
            batch.setColor(0.0f, 0.6f, 1.0f, 1.0f);
        }
        super.draw(batch);
        batch.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Updates the portal's cooldown timer.
     *
     * @param dt Number of seconds since last animation frame
     */
    @Override
    public void update(float dt) {
        teleportCooldown = Math.max(0, teleportCooldown - dt);
        super.update(dt);
    }
}

package game.bobabreach.GameObjects;

import com.badlogic.gdx.graphics.Texture;
import edu.cornell.gdiac.graphics.SpriteBatch;

/**
 * A flashing alert symbol as UI above enemy spawn vents,
 * flashing x times for x seconds each before the vent spawns an enemy.
 */
public class Alert {

    private float flashDuration;
    private int totalFlashes;

    public static float warningLead;

    private Texture texture;

    /** World-space position (same as parent vent) */
    private float x, y;
    /** Render size in screen pixels */
    private float width, height;
    /** Physics units, to convert Box2D coords to screen coords */
    private float units;

    private float yOffset;

    private boolean active    = false; //if alerting
    private boolean visible   = false;   // if texture is currently visible during the alert
    private int     flashCount = 0;      // completed visible phases so far
    private float   stateTimer = 0f;

    public Alert(float x, float y, float size, float units, Texture texture, int totalFlashes, float yOffset, float flashDuration) {
        this.x       = x;
        this.y       = y;
        this.width   = size * units;
        this.height  = size * units;
        this.units   = units;
        this.texture = texture;
        this.totalFlashes = totalFlashes;
        this.yOffset = yOffset;
        this.flashDuration = flashDuration;

        warningLead = flashDuration * totalFlashes * 2.5f;
    }

    /** called when the warning period starts  */
    public void trigger() {
        if (active) return;
        active     = true;
        visible    = true;   // start ON
        flashCount = 0;
        stateTimer = 0f;
    }

    /** for level resets */
    public void reset() {
        active     = false;
        visible    = false;
        flashCount = 0;
        stateTimer = 0f;
    }

    public boolean isActive() { return active; }

    public void update(float delta) {
        if (!active) return;

        stateTimer += delta;

        if (stateTimer >= flashDuration) {
            stateTimer -= flashDuration;  // carry over overshoot

            if (visible) {
                // just finished an ON phase
                flashCount++;
                if (flashCount >= totalFlashes) {
                    // all flashes done — hide and deactivate before spawn
                    visible = false;
                    active  = false;
                } else {
                    visible = false;  // go to OFF
                }
            } else {
                visible = true;  // go back to ON
            }
        }
    }

    /**
     * Draw the alert
     * Converts Box2D world coords to screen coords via units.
     */
    public void draw(SpriteBatch batch) {
        if (!active || !visible) return;
        float screenX = x * units - width  / 2f;
        float screenY = y * units - height / 2f + yOffset;
        batch.draw(texture, screenX, screenY, width, height);
    }

    public void setTexture(Texture texture) { this.texture = texture; }
    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    public float getWarningLead() {
        return flashDuration * totalFlashes;
    }
}

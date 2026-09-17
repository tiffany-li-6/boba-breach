package game.bobabreach.GameObjects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

/**
 * A purely decorative static obstacle.
 *
 * <p>Physically identical to an empty crate: a static box body in the
 * crates collision group (-3) that helpers, enemies, and pathfinding
 * treat as a wall. The only difference is the name ("decoration") and
 * the texture, which is provided by the caller per asset (stool,
 * plant1, plant2, plant3, woodcrate, etc).
 *
 * <p>The collision footprint is sized by the "size" config (and optional
 * "inner" tightening factor). The visual mesh is sized to match the
 * texture's aspect ratio so non-square assets (e.g. 600x1040 plant PNGs)
 * don't get squished.
 *
 * <p>The pathfinding graph automatically picks these up as blocking
 * cells via the AABB query in {@code WorldGraph}, since their
 * fixtures are non-sensor and their name doesn't start with "wall".
 */
public class DecorationObstacle extends ObstacleSprite {

    private final float units;

    /** Physics body width, in world units. */
    private final float width;

    /** Physics body height, in world units. */
    private final float height;

    /** Configured base size from JSON ("size" field), in world units. */
    private final float size;

    /** Image scale multiplier from JSON ("img scale" field). Renders the
     *  sprite slightly larger than the physics body, matching the crate convention. */
    private final float imgScale;

    private Path2 sensorOutline;
    private Color sensorColor;
    private String sensorName;

    public DecorationObstacle(float x, float y, float units, JsonValue data) {
        this.units = units;
        this.size = data.getFloat("size");
        this.imgScale = data.getFloat("img scale", 1.0f);

        // Physics hitbox: match the Crate / LevelObstacle convention of 1.15x
        // the base size, optionally tightened by "inner" if provided.
        JsonValue inner = data.get("inner");
        if (inner != null) {
            width = size * inner.getFloat(0);
            height = size * inner.getFloat(1);
        } else {
            width = size * 1.15f;
            height = size * 1.15f;
        }

        obstacle = new BoxObstacle(x, y, width, height);
        obstacle.setBodyType(BodyDef.BodyType.StaticBody);  // immovable on collision
        obstacle.setDensity(data.getFloat("density", 0));
        obstacle.setFriction(data.getFloat("friction", 0));
        obstacle.setRestitution(data.getFloat("restitution", 0));
        obstacle.setFixedRotation(true);
        obstacle.setPhysicsUnits(units);
        obstacle.setUserData(this);
        obstacle.setName("decoration");

        // Same collision group as crates, so pathfinding and bug behavior
        // treat decorations identically to empty crates.
        obstacle.getFilterData().groupIndex = -3;

        JsonValue debugInfo = data.get("debug");
        if (debugInfo != null) {
            debug = ParserUtils.parseColor(debugInfo.get("avatar"), Color.WHITE);
            sensorColor = ParserUtils.parseColor(debugInfo.get("sensor"), Color.WHITE);
        }

        // Initial mesh sized like Crate/LevelObstacle (img-scaled, bottom-left
        // anchor at -meshSize/2, height = meshSize * 1.75). resizeMeshForTexture
        // overrides this once a texture is set.
        float meshSize = size * units * imgScale;
        mesh.set(-meshSize / 2f, -meshSize / 2f, meshSize, meshSize * 1.75f);
    }

    /**
     * Sets the texture and resizes the rendering mesh to preserve the
     * texture's aspect ratio. Width is fixed at {@code size * units}; height
     * scales by the texture's height/width ratio.
     *
     * <p>Tall sprites (e.g. plants) will render taller than the collision
     * footprint — boba and helpers only collide with the configured hitbox,
     * while the visual sprite extends above it. This matches how real-world
     * tall decorations work: solid base, leafy/decorative top.
     */
    @Override
    public void setTexture(Texture t) {
        super.setTexture(t);
        if (t != null) {
            resizeMeshForTexture(t.getWidth(), t.getHeight());
        }
    }

    private void resizeMeshForTexture(int texW, int texH) {
        if (texW <= 0 || texH <= 0) return;

        // Match the Crate / LevelObstacle convention so decorations align with
        // crates at the same tile position:
        //   - Apply "img scale" multiplier to the base size for the mesh width.
        //   - Anchor the mesh with its bottom-left at (-meshSize/2, -meshSize/2),
        //     so the mesh is centered horizontally but extends upward more
        //     than downward. Crates use a fixed 1.75 height ratio; we replace
        //     that with the texture's actual aspect ratio so non-square
        //     decoration art doesn't get squished.
        float meshSize = size * units * imgScale;
        float aspect = (float) texH / (float) texW;
        float h = meshSize * aspect;
        mesh.set(-meshSize / 2f, -meshSize / 2f, meshSize, h);
    }

    /**
     * Creates a sensor fixture matching the body, so collision systems
     * can detect "touching a decoration" without it being a solid contact.
     * Mirrors the pattern in Crate/Cobweb.
     */
    public void createSensor() {
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = obstacle.getDensity();
        sensorDef.isSensor = true;

        PolygonShape sensorShape = new PolygonShape();
        sensorShape.setAsBox(width * 0.5f, height * 0.5f, new Vector2(0, 0), 0f);
        sensorDef.shape = sensorShape;

        Body body = obstacle.getBody();
        Fixture sensorFixture = body.createFixture(sensorDef);
        sensorName = "decoration_hitbox";
        sensorFixture.setUserData(sensorName);

        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
        sensorOutline = new Path2();
        factory.makeRect(-width / 2f * u, -height / 2f * u, width * u, height * u, sensorOutline);
    }

    public String getSensorName() {
        return sensorName;
    }
}

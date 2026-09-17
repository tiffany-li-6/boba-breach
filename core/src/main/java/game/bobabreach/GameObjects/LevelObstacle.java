package game.bobabreach.GameObjects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

public class LevelObstacle extends ObstacleSprite {

    private float size;
    private JsonValue data;

    private float worldSize;

    public LevelObstacle(float x, float y, float units, JsonValue settings) {

        float s = settings.getFloat("size"); //box2d
        worldSize = s;
        size = s * units; //screen
        data = settings;

        obstacle = new BoxObstacle(x, y, s*1.15f, s*1.15f);

        obstacle.setBodyType(BodyDef.BodyType.StaticBody);
        // obstacle.getFilterData().groupIndex = -3;
        obstacle.setDensity(settings.getFloat("density", 0));
        obstacle.setFriction(settings.getFloat("friction", 0.0f));
        obstacle.setRestitution(settings.getFloat("restitution", 0.0f));
        obstacle.getFilterData().groupIndex = -3;
        obstacle.getFilterData().categoryBits = 0x0002;
        obstacle.getFilterData().maskBits = (short) (0xFFFF & ~0x0006);

        obstacle.setPhysicsUnits(units);
        obstacle.setName("obstacle");
        obstacle.setUserData(this);

        // EXACTLY like crate-style mesh
        debug = ParserUtils.parseColor(settings.get("debug"), Color.GRAY);

        float meshScale = settings.getFloat("img scale");
        float meshSize = size * meshScale;
        mesh.set(-meshSize/2.0f,-meshSize/2.0f, meshSize,meshSize*1.75f);

    }

    /**
     * Creates the sensor for the crate.
     */
    public void createSensor() {
        Vector2 sensorCenter = new Vector2(0, 0);
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = 1;
        sensorDef.isSensor = true;

        JsonValue sensorjv = data.get("sensor");
        float w = sensorjv.getFloat("xFactor", 0) * worldSize;
        float h = sensorjv.getFloat("yFactor", 0) * worldSize;
        PolygonShape sensorShape = new PolygonShape();
        sensorShape.setAsBox(w, h, sensorCenter, 0.0f);
        sensorDef.shape = sensorShape;

        // Sensor to represent hitbox
        Body body = obstacle.getBody();
        Fixture sensorFixture = body.createFixture(sensorDef);
        String sensorName = "obstacle_hitbox";
        sensorFixture.setUserData(sensorName);

        // Finally, we need a debug outline
        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
        Path2 sensorOutline = new Path2();
        factory.makeRect((sensorCenter.x - w / 2) * u, (sensorCenter.y - h / 2) * u, w * u, h * u, sensorOutline);
    }
}

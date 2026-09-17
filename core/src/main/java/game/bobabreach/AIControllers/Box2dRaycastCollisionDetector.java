/*******************************************************************************
 * Copyright 2014 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package game.bobabreach.AIControllers;

import com.badlogic.gdx.ai.utils.Collision;
import com.badlogic.gdx.ai.utils.Ray;
import com.badlogic.gdx.ai.utils.RaycastCollisionDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.physics2.Obstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

/** A raycast collision detector for box2d.
 *
 * @author davebaol
 * Modified by Jason Jang.*/

public class Box2dRaycastCollisionDetector implements RaycastCollisionDetector<Vector2> {

    World world;
    Box2dRaycastCallback callback;
    Box2dRaycastCallback2 callback2;
    Array<Obstacle> excluded;

    public Box2dRaycastCollisionDetector (World world) {
        this(world, new Box2dRaycastCallback(null));
    }

    public Box2dRaycastCollisionDetector (World world, Array<Obstacle> excluded) {
        this(world, new Box2dRaycastCallback(excluded));
        this.excluded = excluded;
        callback2 = new Box2dRaycastCallback2(excluded);
    }

    public Box2dRaycastCollisionDetector (World world, Array<Obstacle> excluded, int i) {
        this(world, new Box2dRaycastCallback(excluded));
        this.excluded = excluded;
        callback2 = new Box2dRaycastCallback2(excluded);
    }

    public Box2dRaycastCollisionDetector (World world, Box2dRaycastCallback callback) {
        this.world = world;
        this.callback = callback;
    }

    @Override
    public boolean collides (Ray<Vector2> ray) {
        return findCollision(null, ray);
    }

    public boolean collides2 (Ray<Vector2> ray) {
        return findCollisionHelper(null, ray);
    }



    @Override
    public boolean findCollision (Collision<Vector2> outputCollision, Ray<Vector2> inputRay) {
        callback.collided = false;
        if (!inputRay.start.epsilonEquals(inputRay.end, MathUtils.FLOAT_ROUNDING_ERROR)) {
            callback.outputCollision = outputCollision;
            world.rayCast(callback, inputRay.start, inputRay.end);
        }
        return callback.collided;
    }

    public boolean findCollisionHelper (Collision<Vector2> outputCollision, Ray<Vector2> inputRay) {
        callback2.collided = false;
        if (!inputRay.start.epsilonEquals(inputRay.end, MathUtils.FLOAT_ROUNDING_ERROR)) {
            callback2.outputCollision = outputCollision;
            world.rayCast(callback2, inputRay.start, inputRay.end);
        }
        return callback2.collided;
    }

    public static class Box2dRaycastCallback implements RayCastCallback {
        public Collision<Vector2> outputCollision;
        public boolean collided;
        public Array<Obstacle> excluded;

        public Box2dRaycastCallback (Array<Obstacle> excluded) {
            this.excluded = excluded;
        }

        @Override
        public float reportRayFixture (Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
            Body body = fixture.getBody();
            ObstacleSprite obs = (ObstacleSprite) body.getUserData();
            if (fixture.getUserData() != null && fixture.getUserData().equals("crate_hitbox")) {
                collided = true;
                return 0;
            }
            if (obs == null || obs.getName().startsWith("enemy") // wall?
                || fixture.isSensor() || obs.getName().startsWith("helper") || obs.getName().startsWith("boba")
                || ( excluded != null && excluded.contains(obs.getObstacle(), false))) {
                return 1;
            }
            if (outputCollision != null) outputCollision.set(point, normal);
            collided = true;
            return 0;
        }
    }

    public static class Box2dRaycastCallback2 implements RayCastCallback {
        public Collision<Vector2> outputCollision;
        public boolean collided;
        public Array<Obstacle> excluded;

        public Box2dRaycastCallback2 (Array<Obstacle> excluded) {
            this.excluded = excluded;
        }

        @Override
        public float reportRayFixture (Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
            Body body = fixture.getBody();
            ObstacleSprite obs = (ObstacleSprite) body.getUserData();
            if (fixture.getUserData() != null && fixture.getUserData().equals("crate_hitbox")) {
                collided = true;
                return 0;
            }
            if (obs.getName().startsWith("wall") || obs.getName().startsWith("enemy")
                || fixture.isSensor() || obs.getName().startsWith("helper") || obs.getName().startsWith("boba")
                || ( excluded != null && excluded.contains(obs.getObstacle(), false))) {
                return 1;
            }
            if (outputCollision != null) outputCollision.set(point, normal);
            collided = true;
            return 0;
        }
    }

    public static class Box2dRaycastCallback3 implements RayCastCallback {
        public Collision<Vector2> outputCollision;
        public boolean collided;
        public Array<Obstacle> excluded;

        public Box2dRaycastCallback3 (Array<Obstacle> excluded) {
            this.excluded = excluded;
        }

        @Override
        public float reportRayFixture (Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
            Body body = fixture.getBody();
            ObstacleSprite obs = (ObstacleSprite) body.getUserData();
            if (fixture.getUserData() != null && obs.getName().startsWith("helper")) {
                collided = true;
                return 0;
            }
            return 1;
        }
    }
}

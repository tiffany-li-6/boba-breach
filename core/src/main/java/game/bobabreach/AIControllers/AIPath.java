package game.bobabreach.AIControllers;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.steer.utils.Path;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.math.Vector;
import com.badlogic.gdx.math.Vector2;
import game.bobabreach.WorldGraph;

public class AIPath extends DefaultGraphPath<WorldGraph.IndexNode> implements Path<Vector2, LinePath.LinePathParam> {

    public AIPath(DefaultGraphPath<WorldGraph.IndexNode> path) {

    }

    /**
     * Returns a new instance of the path parameter.
     */
    @Override
    public LinePath.LinePathParam createParam() {
        return new LinePath.LinePathParam();
    }

    /**
     * Returns {@code true} if this path is open; {@code false} otherwise.
     */
    @Override
    public boolean isOpen() {
        return true;
    }

    /**
     * Returns the length of this path.
     */
    @Override
    public float getLength() {
        return nodes.size;
    }

    private Vector2 cache = new Vector2();

    /**
     * Returns the first point of this path.
     */
    @Override
    public Vector2 getStartPoint() {
        cache.set(nodes.get(0).getXPos(), nodes.get(0).getYPos());
        return cache;
    }

    /**
     * Returns the last point of this path.
     */
    @Override
    public Vector2 getEndPoint() {
        cache.set(nodes.peek().getXPos(), nodes.peek().getYPos());
        return cache;
    }

    /**
     * Maps the given position to the nearest point along the path using the path parameter to ensure coherence and returns the
     * distance of that nearest point from the start of the path.
     *
     * @param position a location in game space
     * @param param    the path parameter
     * @return the distance of the nearest point along the path from the start of the path itself.
     */
    @Override
    public float calculateDistance(Vector2 position, LinePath.LinePathParam param) {
        return 0;
    }

    /**
     * Calculates the target position on the path based on its distance from the start and the path parameter.
     *
     * @param out            the target position to calculate
     * @param param          the path parameter
     * @param targetDistance the distance of the target position from the start of the path
     */
    @Override
    public void calculateTargetPosition(Vector2 out, LinePath.LinePathParam param, float targetDistance) {

    }

}

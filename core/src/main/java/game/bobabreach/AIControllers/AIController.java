package game.bobabreach.AIControllers;

import com.badlogic.gdx.ai.pfa.*;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.ai.utils.Collision;
import com.badlogic.gdx.ai.utils.Ray;
import com.badlogic.gdx.ai.utils.RaycastCollisionDetector;
import com.badlogic.gdx.math.Vector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.physics2.Obstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.util.PooledList;
import game.bobabreach.GameObjects.Crate;
import game.bobabreach.GameObjects.Cup;
import game.bobabreach.WorldGraph;

import java.util.Iterator;

/**
 * Controls the stealer bug AI.
 */
public class AIController {
    /**
     * The Box2D world
     */
    protected World world;

    /**
     * Reference to cup
     */
    protected final Cup cup;

    /**
     * List of crates
     */
    protected final PooledList<ObstacleSprite> crates;

    /**
     * The list of world graphs
     */
    protected final Array<WorldGraph> worldGraphs;

    /**
     * The list of pathfinders
     */
    protected final Array<Pathfinder> pathfinders;



    /**
     * Reference to the world graph currently being used.
     */
    protected WorldGraph graph;

    /**
     * Reference to the pathfinder currently being used.
     */
    protected Pathfinder pathfinder;

    protected Heuristic<WorldGraph.IndexNode> heuristic = new ManhattanHeuristic();

    SmoothableGraphPath<WorldGraph.IndexNode, Vector2> path = new SmoothPath();

    public AIController(PooledList<ObstacleSprite> crates, Array<WorldGraph> graphs, Cup cup) {
        worldGraphs = graphs;
        this.crates = crates;
        pathfinders = new Array<>();
        for (int i = 0; i < worldGraphs.size; i++) {
            pathfinders.add(new Pathfinder(worldGraphs.get(i)));
        }

        this.cup = cup;
    }

//    private WorldGraph.IndexNode findBestEndNode(Crate target) {
//        WorldGraph.IndexNode currNode = graph.getNodeAtObs(target.getObstacle());
//        int x = graph.getNodeX(target.getObstacle());
//        int y = graph.getNodeY(target.getObstacle());
//        float size = target.getSize();
//        int numSquares = (int) Math.ceil(size / graph.getSquareSize());
//        int squaresToSearch = (int) Math.ceil(numSquares / 2.0) + 1;
//
//        int enemyNodeX = graph.getNodeX(enemy.getObstacle());
//        int enemyNodeY = graph.getNodeY(enemy.getObstacle());
//        int dst = Integer.MAX_VALUE;
//        WorldGraph.IndexNode ret = currNode;
//
//        for (int i = -squaresToSearch; i < squaresToSearch; i++) {
//            for (int j = -squaresToSearch; j < squaresToSearch; j++) {
//                WorldGraph.IndexNode node = graph.getNodeAt(x + i, y + j);
//                if (node != null && node.isOpen()) {
//                    int currDst = Math.abs(((x + i) - enemyNodeX)) + Math.abs((y + j) - enemyNodeY);
//                    if (currDst < dst) {
//                        ret = node;
//                    }
//                }
//            }
//        }
//        return ret;
//    }

    protected static class DistHeuristic implements Heuristic<WorldGraph.IndexNode> {
        /**
         * Calculates an estimated cost to reach the goal node from the given node.
         *
         * @param node    the start node
         * @param endNode the end node
         * @return the estimated cost
         */
        @Override
        public float estimate(WorldGraph.IndexNode node, WorldGraph.IndexNode endNode) {
            return Math.abs((endNode.getXPos()) - node.getXPos()) * 10 +
                Math.abs((endNode.getYPos()) - node.getYPos()) * 10;
        }
    }

    protected static class ManhattanHeuristic implements Heuristic<WorldGraph.IndexNode> {
        /**
         * Calculates an estimated cost to reach the goal node from the given node.
         *
         * @param node    the start node
         * @param endNode the end node
         * @return the estimated cost
         */
        @Override
        public float estimate(WorldGraph.IndexNode node, WorldGraph.IndexNode endNode) {
            int xDist = endNode.getX() - node.getX();
            xDist = xDist < 0 ? -xDist : xDist;
            int yDist = endNode.getY() - node.getY();
            yDist = yDist < 0 ? -yDist : yDist;
            return xDist * 10 + yDist * 10;
        }
    }

    protected static class Pathfinder extends IndexedAStarPathFinder<WorldGraph.IndexNode> {
        public Pathfinder(WorldGraph graph) {
            super(graph.getGraph());
        }

        @Override
        public boolean searchNodePath(WorldGraph.IndexNode startNode, WorldGraph.IndexNode endNode,
                                      Heuristic<WorldGraph.IndexNode> heuristic,
                                      GraphPath<WorldGraph.IndexNode> outPath) {
            return super.searchNodePath(startNode, endNode, heuristic, outPath);
        }
    }

    Array<Vector2> waypoints = new Array<>();

    public LinePath<Vector2> generateLinePath(DefaultGraphPath<WorldGraph.IndexNode> path) {
        waypoints.clear();
        if (path.getCount() < 2) {
            return null;
        }
        for (int i = 0; i < path.getCount(); i++) {
            WorldGraph.IndexNode node = path.get(i);
            waypoints.add(new Vector2(node.getXPos(), node.getYPos()));
        }
        return new LinePath<>(waypoints);
    }

    protected static class MyRaycastCallback implements RayCastCallback {
        public boolean collided = false;
        private Array<Obstacle> excluded;
        int count = 0;

        public MyRaycastCallback(Array<Obstacle> excluded) {
            this.excluded = excluded;
        }
        public void reset() {
            collided = false;
        }
        public void setExcluded(Array<Obstacle> excluded) {
            this.excluded = excluded;
        }
        /**
         * Called for each fixture found in the query. You control how the ray cast proceeds by returning a float: return -1: ignore
         * this fixture and continue return 0: terminate the ray cast return fraction: clip the ray to this point return 1: don't clip
         * the ray and continue.
         * <p>
         * The {@link Vector2} instances passed to the callback will be reused for future calls so make a copy of them!
         *
         * @param fixture  the fixture hit by the ray
         * @param point    the point of initial intersection
         * @param normal   the normal vector at the point of intersection
         * @param fraction
         * @return -1 to filter, 0 to terminate, fraction to clip the ray for closest hit, 1 to continue
         **/
        @Override
        public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
            Body body = fixture.getBody();
            ObstacleSprite obs = (ObstacleSprite) body.getUserData();
//            if (fixture.getUserData() != null && fixture.getUserData().equals("hitbox_crate")) {
//                collided = true;
//                count++;
//                System.out.println("crate hitbox hit x " + count);
//                return 0;
//            }
            if (obs.getName().startsWith("wall") || obs.getName().startsWith("enemy")
                || fixture.isSensor() || obs.getName().startsWith("helper") || obs.getName().startsWith("boba")
                || ( excluded != null && excluded.contains(obs.getObstacle(), false))) {
                return 1;
            }
            collided = true;
            return 0;
        }
    }

    protected static class BoxQueryCallback implements QueryCallback {
        public boolean collided = false;
        private Array<Obstacle> excluded;
        int count = 0;

        public BoxQueryCallback(Array<Obstacle> excluded) {
            this.excluded = excluded;
        }
        public void reset() {
            collided = false;
        }
        public void setExcluded(Array<Obstacle> excluded) {
            this.excluded = excluded;
        }

        /**
         * Called for each fixture found in the query AABB.
         *
         * @param fixture
         * @return false to terminate the query.
         */
        @Override
        public boolean reportFixture(Fixture fixture) {
            Body body = fixture.getBody();
            ObstacleSprite obs = (ObstacleSprite) body.getUserData();
            if (fixture.getUserData() != null && fixture.getUserData().equals("obstacle_hitbox")) {
                collided = true;
                return false;
            }
            if (fixture.getUserData() != null && fixture.getUserData().equals("crate_hitbox")) {
                if (!(excluded != null && excluded.contains(obs.getObstacle(), false))) {
                    collided = true;
                    return false;
                }
            }
            if (obs == null || obs.getName().startsWith("wall") || obs.getName().startsWith("enemy")
                || fixture.isSensor() || obs.getName().startsWith("helper") || obs.getName().startsWith("boba")
                || ( excluded != null && excluded.contains(obs.getObstacle(), false))) {
                return true;
            }
            collided = true;
            return false;

        }
    }

    protected static class SmoothPath extends DefaultGraphPath<WorldGraph.IndexNode>
    implements SmoothableGraphPath<WorldGraph.IndexNode, Vector2> {

        /**
         * Returns the position of the node at the given index.
         *
         * @param index the index of the node you want to know the position
         */
        @Override
        public Vector2 getNodePosition(int index) {
            return nodes.get(index).getPos();
        }

        /**
         * Swaps the specified nodes of this path.
         *
         * @param index1 index of the first node to swap
         * @param index2 index of the second node to swap
         */
        @Override
        public void swapNodes(int index1, int index2) {
            nodes.swap(index1, index2);
        }

        /**
         * Reduces the size of this path to the specified length (number of nodes). If the path is already smaller than the specified
         * length, no action is taken.
         *
         * @param newLength the new length
         */
        @Override
        public void truncatePath(int newLength) {
            nodes.truncate(newLength);
        }

        /**
         * Adds an item at the end of this path.
         *
         * @param node
         */
        @Override
        public void add(WorldGraph.IndexNode node) {
            nodes.add(node);
        }
    }

}

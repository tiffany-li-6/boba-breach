package game.bobabreach.AIControllers;

import com.badlogic.gdx.ai.pfa.PathSmoother;
import com.badlogic.gdx.ai.utils.Ray;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.bullet.collision.GIM_HASH_NODE_CMP_KEY_MACRO;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.physics2.Obstacle;
import edu.cornell.gdiac.physics2.ObstacleSelector;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.util.PooledList;
import game.bobabreach.GameObjects.*;
import game.bobabreach.WorldGraph;

public class HelperAIController extends AIController {

    /**
     * Reference to the helper currently being controlled.
     */
    private Helper helper;

    /**
     * Reference to the helper selector indicator.
     */
    private ObstacleSelector helperSelector;

    private Box2dRaycastCollisionDetector detector;

    private Array<Obstacle> excluded;

    private Vector2 raycastTarget;

    private PathSmoother<WorldGraph.IndexNode, Vector2> smoother;

    private Level level;


    public HelperAIController(PooledList<ObstacleSprite> crates, Array<WorldGraph> graphs, Cup cup,
                              ObstacleSelector selector, World world, Level level) {
        super(crates, graphs, cup);
        helperSelector = selector;
        this.world = world;
        excluded = new Array<>();
        detector = new Box2dRaycastCollisionDetector(world,excluded);
        raycastTarget = new Vector2();
        smoother = new PathSmoother<>(detector);
        this.level = level;
        // System.out.println(graphs.size);
    }

    public void setHelper(Helper h) {
        helper = h;
    }

    private Vector2 posCache = new Vector2();
    MyRaycastCallback callback = new MyRaycastCallback(excluded);
    BoxQueryCallback callback2 = new BoxQueryCallback(excluded);

    /**
     * Sets the currently stored helper's movement.
     */
    public void setHelperMovement() {
        WorldGraph.IndexNode startNode;
        WorldGraph.IndexNode endNode;

        switch (helper.getState()) {
            case MOVE:
                excluded.clear();
                graph = worldGraphs.get(0);
                pathfinder = pathfinders.get(0);
//        WorldGraph.IndexNode endNode = findBestEndNode((Crate) crates.get(enemy.getTarget()));
                startNode = graph.findClosestReachableNode(helper.getObstacle());
                endNode = graph.findClosestReachableNode(helper.getMoveTarget().x, helper.getMoveTarget().y);
                raycastTarget.set(helper.getMoveTarget().x, helper.getMoveTarget().y);
                break;
            case PICKUP:
                excluded.clear();
                excluded.add((crates.get(helper.getTarget() - 1).getObstacle()));
                graph = worldGraphs.get(helper.getTarget());
                pathfinder = pathfinders.get(helper.getTarget());
//        WorldGraph.IndexNode endNode = findBestEndNode((Crate) crates.get(enemy.getTarget()));
                startNode = graph.findClosestReachableNode(helper.getObstacle());
                endNode = graph.getNodeAtObs((crates.get(helper.getTarget() - 1).getObstacle()));
                raycastTarget.set(crates.get(helper.getTarget() - 1).getObstacle().getPosition());
                break;
            case DROPOFF:
                excluded.clear();
                excluded.add(cup.getObstacle());
                graph = worldGraphs.get(worldGraphs.size - 1);
                pathfinder = pathfinders.get(worldGraphs.size - 1);
                startNode = graph.findClosestReachableNode(helper.getObstacle());
                endNode = graph.findClosestReachableNode(cup.getObstacle());
                raycastTarget.set(cup.getObstacle().getPosition());
                break;
            case RETURN:
                excluded.clear();
                graph = worldGraphs.get(0);
                pathfinder = pathfinders.get(0);
                startNode = graph.findClosestReachableNode(helper.getObstacle());
                endNode = graph.getNodeAtObs(helper.getSpawn().getObstacle());
                raycastTarget.set(helper.getSpawn().getObstacle().getPosition());
                break;
            default:
                excluded.clear();
                graph = worldGraphs.get(0);
                pathfinder = pathfinders.get(0);
                startNode = graph.findClosestReachableNode(helper.getObstacle());
                endNode = graph.getNodeAtObs(helper.getObstacle());
                raycastTarget.set(helper.getObstacle().getPosition());
                break;
        }


        Ray ray = new Ray<>(helper.getObstacle().getPosition(), raycastTarget);

        if( Math.abs((raycastTarget.x - helper.getObstacle().getPosition().x)) + Math.abs((
            helper.getMoveTarget().y - helper.getObstacle().getPosition().y)) < 0.05) {
            helper.setPath(null);
            helper.setPathIndex(0);
            helper.setMovement(0, 0, -1);
            if (!helper.isStunned()) {
                helper.setState(Helper.HelperFSMState.IDLE);
            }
            return;
        }

        // System.out.println(helper.getObstacle().getAngle());
        // Facing up = 0

        if (helper.getMoveMode() == 1) {
            helper.setMovement(raycastTarget.x - helper.getObstacle().getX(),
                raycastTarget.y - helper.getObstacle().getY(), 0);
            return;
        }


        // Make helper move directly to cursor if not close to a box, and no box is in the way
        callback.reset();
        callback2.reset();
        callback.setExcluded(excluded);
        callback2.setExcluded(excluded);
        world.rayCast(callback, helper.getObstacle().getPosition(), raycastTarget);
        world.QueryAABB(callback2, helper.getObstacle().getX() - 1f, helper.getObstacle().getY() - 1f,
            helper.getObstacle().getX() + 1f, helper.getObstacle().getY() + 1f);

//        if (!callback.collided && !
//            (helper.getObstacle().getX() <= 1 || helper.getObstacle().getX() >= 31 ||
//                helper.getObstacle().getY() <= 1 || helper.getObstacle().getY() >= 31)) {
//            // System.out.println("Direct path found");
//            helper.setPath(null);
//            helper.setMovement(raycastTarget.x - helper.getObstacle().getX(),
//                raycastTarget.y - helper.getObstacle().getY(), 0);
//            return;
//        }


        callback.reset();
        callback2.reset();
        callback.setExcluded(excluded);
        callback2.setExcluded(excluded);
        world.rayCast(callback, helper.getObstacle().getPosition(), raycastTarget);
        world.QueryAABB(callback2, helper.getObstacle().getX() - 0.1f, helper.getObstacle().getY() -0.1f,
            helper.getObstacle().getX() + 0.1f, helper.getObstacle().getY() + 0.1f);
//        System.out.println("Helper pos: " + helper.getObstacle().getPosition() + "\n");
//        System.out.println("Helper edge pos: " + helper.getEdge1() + "\n");
        // System.out.println(helper.getCollidingCrate());
        if (!callback.collided && !helper.getCollidingCrate()) {
            callback.reset();
            helper.getEdge1();
            posCache.set(raycastTarget);
            posCache.add(helper.getOffset());
            world.rayCast(callback, helper.getEdge1(), posCache);
            if (!callback.collided ) {
                // System.out.println("Original target: " + raycastTarget + "\n");
                // System.out.println("First target: " + posCache + "\n");
                callback.reset();
                helper.getEdge2();
                posCache.set(raycastTarget);
                posCache.add(helper.getOffset());
                world.rayCast(callback, helper.getEdge2(), posCache);
                if(!callback.collided) {
                    // System.out.println("Direct path found");
                    // System.out.println("Second target: " + posCache + "\n");
                    helper.setPath(null);
                    helper.setMoveMode(1); // direct movement
                    helper.setMovement(raycastTarget.x - helper.getObstacle().getX(),
                        raycastTarget.y - helper.getObstacle().getY(), 0);
                    return;
                }
            }
        }


//        if (!(detector.collides(new Ray<>(helper.getObstacle().getPosition(), raycastTarget)))) {
//            System.out.println("Direct path found");
//            helper.setPath(null);
//            helper.setMovement(raycastTarget.x - helper.getObstacle().getX(),
//                raycastTarget.y - helper.getObstacle().getY());
//            return;
//        }

        if (startNode.equals(endNode)) { // If destination reached, set helper idle
            System.out.println("destination reached!");
            if (helper.getState() == Helper.HelperFSMState.PICKUP) {
                Crate crate = (Crate) crates.get(helper.getTarget() - 1);
                helper.setState(Helper.HelperFSMState.IDLE);
                if (helper.hasIngredient() && helper.getHeldIngredient() == crate.getIngredient()) {
                    helper.removeIngredient();
                    level.returnToCrate(crate.getIngredient());
                } else if (!helper.hasIngredient() && level.getAvailable(crate.getIngredient()) > 0) {
                    helper.giveIngredient(crate.getIngredient());
                    level.takeFromCrate(crate.getIngredient());
                }
            }
            helper.setPath(null);
            helper.setPathIndex(0);
            helper.setMovement(0, 0, -1);
            if (!helper.isStunned()) {
                helper.setState(Helper.HelperFSMState.IDLE);
            }
            return;
        }

        if (helper.getState() == Helper.HelperFSMState.IDLE || helper.isStunned()) {
            return;
        }

        System.out.println("START: " +
            (startNode == null ? "null" :
                startNode.getX() + "," + startNode.getY()));

        System.out.println("END: " +
            (endNode == null ? "null" :
                endNode.getX() + "," + endNode.getY()));
        System.out.println(helper.getPath() != null);


        // Don't recompute path if helper already has one

        // If we have a cached path, follow it — don't recompute
        if (helper.getPath() != null) {
            advanceAlongPath();
            return;
        }

        // No path yet — compute one
        path = new AIController.SmoothPath();
        if (!pathfinder.searchNodePath(startNode, endNode, heuristic, path)) {
            System.out.println("No path found");
            helper.setMovement(0, 0, 1);
            return;
        }
        if (path.getCount() <= 1) {
            helper.setPath(null);
            helper.setMovement(0, 0, -1);
            if (!helper.isStunned()) helper.setState(Helper.HelperFSMState.IDLE);
            return;
        }


        smoother.smoothPath(path);

        helper.setPath(path);
        helper.setPathIndex(0); // reset index on new path
        advanceAlongPath();
        return;
    }

    private static final float WAYPOINT_RADIUS = 0.45f; // tune to your tile size

    private void advanceAlongPath() {
        var path = helper.getPath();
        int pathIndex = helper.getPathIndex();
        if (path == null || path.getCount() == 0) {
            System.out.println("Cannot advance along path");
            return;
        }

        Vector2 agentPos = helper.getObstacle().getPosition();

        // Advance index while close enough to current waypoint
        while (pathIndex < path.getCount()) {
            WorldGraph.IndexNode node = path.get(pathIndex);
            float dx = node.getXPos() - agentPos.x;
            float dy = node.getYPos() - agentPos.y;
            if (dx * dx + dy * dy > WAYPOINT_RADIUS * WAYPOINT_RADIUS) break;
            pathIndex++;
        }

        // Reached the end
        if (pathIndex >= path.getCount()) {
            helper.setPath(null);
            helper.setPathIndex(0);
            helper.setMovement(0, 0, -1);
            if (!helper.isStunned()) helper.setState(Helper.HelperFSMState.IDLE);
            return;
        }

        WorldGraph.IndexNode target = path.get(pathIndex);
        float dx = target.getXPos() - agentPos.x;
        float dy = target.getYPos() - agentPos.y;
        System.out.println(path.getCount());
        helper.setMovement(dx, dy, 1);
        helper.setPathIndex(pathIndex);
    }

}

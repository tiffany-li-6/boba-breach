package game.bobabreach.AIControllers;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.steer.behaviors.FollowPath;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.physics2.ObstacleSelector;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.util.PooledList;
import game.bobabreach.GameObjects.Cup;
import game.bobabreach.GameObjects.Helper;
import game.bobabreach.GameObjects.HelperSteerable;
import game.bobabreach.WorldGraph;

public class HelperAIControllerPathFollow extends AIController {

    /**
     * Reference to the helper currently being controlled.
     */
    private HelperSteerable helper;

    /**
     * Reference to the helper selector indicator.
     */
    private ObstacleSelector helperSelector;

    private LinePath<Vector2> linePath;

    public HelperAIControllerPathFollow(PooledList<ObstacleSprite> crates, Array<WorldGraph> graphs, Cup cup,
                                        ObstacleSelector selector, World world) {
        super(crates, graphs, cup);
        helperSelector = selector;
        this.world = world;
//        pathSB = new FollowPath<>(helper, linePath, 1);
//        pathSB.setArrivalTolerance(0.1f);
//        pathSB.setTimeToTarget(0.01f);
//        pathSB.setDecelerationRadius(1f);

        // System.out.println(graphs.size);
    }

    public void setHelper(HelperSteerable h) {
        helper = h;
        // pathSB.setOwner(helper);
    }

    /**
     * Sets the currently stored helper's movement.
     */
    public void setHelperMovement() {
        WorldGraph.IndexNode startNode;
        WorldGraph.IndexNode endNode;

        switch (helper.getState()) {
            case MOVE:
                graph = worldGraphs.get(0);
                pathfinder = pathfinders.get(0);
//        WorldGraph.IndexNode endNode = findBestEndNode((Crate) crates.get(enemy.getTarget()));
                startNode = graph.getNodeAtObs(helper.getObstacle());
                endNode = graph.getNodeAtPos(helper.getMoveTarget().x, helper.getMoveTarget().y);
                break;
            case PICKUP:
                graph = worldGraphs.get(helper.getTarget());
                pathfinder = pathfinders.get(helper.getTarget());
//        WorldGraph.IndexNode endNode = findBestEndNode((Crate) crates.get(enemy.getTarget()));
                startNode = graph.getNodeAtObs(helper.getObstacle());
                endNode = graph.getNodeAtObs((crates.get(helper.getTarget() - 1).getObstacle()));
                break;
            case DROPOFF:
                graph = worldGraphs.get(0);
                pathfinder = pathfinders.get(0);
                startNode = graph.getNodeAtObs(helper.getObstacle());
                endNode = graph.getNodeAtObs(cup.getObstacle());
                break;
            case RETURN:
                graph = worldGraphs.get(0);
                pathfinder = pathfinders.get(0);
                startNode = graph.getNodeAtObs(helper.getObstacle());
                endNode = graph.getNodeAtObs(helper.getSpawn().getObstacle());
                break;
            default:
                graph = worldGraphs.get(0);
                pathfinder = pathfinders.get(0);
                startNode = graph.getNodeAtObs(helper.getObstacle());
                endNode = graph.getNodeAtObs(helper.getObstacle());
                break;
        }

        if (startNode.equals(endNode)) { // If destination reached, set helper idle
            System.out.println("destination reached!");
            // helper.setMovement(0, 0);
            helper.setBehavior(HelperSteerable.HelperBehaviorState.IDLE);
            helper.setState(HelperSteerable.HelperFSMState.IDLE);
            return;
        }

        if( Math.abs((helper.getMoveTarget().x - helper.getPosition().x) + (
                    helper.getMoveTarget().y - helper.getPosition().y)) <= 2) {
            helper.setBehavior(HelperSteerable.HelperBehaviorState.ARRIVE);
            return;
        }


        if (!pathfinder.searchNodePath(startNode, endNode, heuristic, path)) {
            // System.out.println("oopsies");
            helper.setBehavior(HelperSteerable.HelperBehaviorState.IDLE);
            helper.setState(HelperSteerable.HelperFSMState.IDLE);
            return;
            // helper.setMovement(0, 0);
        }
        else {
            if (path.getCount() <= 1) {
                // helper.setMovement(0, 0);
                helper.lockVelocity();
                helper.setState(HelperSteerable.HelperFSMState.IDLE);
                System.out.println("destination reached!");
                return;
            }

            LinePath<Vector2> linePath = generateLinePath((DefaultGraphPath<WorldGraph.IndexNode>) path);
            // linePath = generateLinePath((DefaultGraphPath<WorldGraph.IndexNode>) path);
            // pathSB.setPath(linePath);
            // FollowPath<Vector2, LinePath.LinePathParam> pathSB = new FollowPath<>(helper, linePath, 1);

            helper.setPath(linePath);
            helper.setBehavior(HelperSteerable.HelperBehaviorState.PATH);

            /**
            WorldGraph.IndexNode first = path.get(0);
            WorldGraph.IndexNode second = path.get(1);
            WorldGraph.IndexNode third = null;
            if (path.getCount() >= 3) {
                third = path.get(2);
            }

            float x = second.getXPos() - first.getXPos();
            float y = second.getYPos() - first.getYPos();
            if (third == null) {
                // helper.setMovement(x, y);
                return;
            }
            int firstX = graph.getNodeX(first.getIndex());
            int firstY = graph.getNodeY(first.getIndex());
            int thirdX = graph.getNodeX(third.getIndex());
            int thirdY = graph.getNodeY(third.getIndex());
            if (Math.abs(thirdX - firstX) == 1 && Math.abs(thirdY - firstY) == 1) {
                // helper.setMovement(thirdX - firstX, thirdY - firstY);
            }
            else {
                // helper.setMovement(x, y);
            }
            */
        }
    }

}

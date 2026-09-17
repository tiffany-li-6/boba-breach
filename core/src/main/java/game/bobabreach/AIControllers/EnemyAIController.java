package game.bobabreach.AIControllers;

import com.badlogic.gdx.ai.pfa.PathSmoother;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.physics2.Obstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.util.PooledList;
import game.bobabreach.GameObjects.*;
import game.bobabreach.GameObjects.Cup;
import game.bobabreach.GameObjects.Enemy;
import game.bobabreach.GameObjects.Helper;
import game.bobabreach.WorldGraph;

public class EnemyAIController extends AIController {

    /**
     * Reference to the enemy currently being controlled.
     */
    private Enemy enemy;

    /**
     * Reference to list of vents
     */
    private final PooledList<ObstacleSprite> ventList;

    /**
     * Reference to list of helpers
     */
    private final Array<Helper> helperList;

    /**
     * Reference to the level (for ingredient-availability queries during fallback).
     */
    private Level level;

    private Vector2 raycastTarget;

    private Box2dRaycastCollisionDetector detector;
    private Array<Obstacle> excluded;
    private PathSmoother<WorldGraph.IndexNode, Vector2> smoother;

    private Vector2 posCache = new Vector2();
    private float smallestDistToHelper;

    MyRaycastCallback callback = new MyRaycastCallback(null);
    BoxQueryCallback callback2 = new BoxQueryCallback(null);

    /** Scratch path used to test reachability for fallback crate selection. */
    private final SmoothPath fallbackProbePath = new SmoothPath();


    public EnemyAIController(PooledList<ObstacleSprite> crates, Array<WorldGraph> graphs, Cup cup,
                             PooledList<ObstacleSprite> vents, Array<Helper> helpers, World world) {
        super(crates, graphs, cup);
        ventList = vents;
        helperList = helpers;
        excluded = new Array<>();
        detector = new Box2dRaycastCollisionDetector(world,excluded);
        raycastTarget = new Vector2();
        smoother = new PathSmoother<>(detector);
        smallestDistToHelper = -1;
        this.world = world;
        // System.out.println(graphs.size);
    }

    public void setEnemy(Enemy e) {
        enemy = e;
    }

    /**
     * Provides the level reference so fallback crate selection can filter to
     * crates that still have ingredients available. If never called, the
     * fallback will not run (a stealer with an unreachable original target
     * will fall through to its existing no-path behavior).
     */
    public void setLevel(Level lev) {
        this.level = lev;
    }

    /**
     * Sets the currently stored enemy's movement.
     */
    public void setEnemyMovement() {
        if (enemy.isAnimLocked()) {
            return; // don't process movement while animation is playing
        }
        if (enemy.getAnimState() == Enemy.AnimState.DEATH_ANIM) {
            return;
        }

        WorldGraph.IndexNode startNode;
        WorldGraph.IndexNode endNode;
        WorldGraph.IndexNode nodeToReset;

        switch (enemy.getState()) {
            case WANDER:
                excluded.clear();
                graph = worldGraphs.get(0);
                pathfinder = pathfinders.get(0);
                startNode = graph.getNodeAtObs(enemy.getObstacle());
                if (enemy.getWanderTarget() != null) {
                    endNode = enemy.getWanderTarget();
                }
                else {
                    endNode = graph.getRandomNode();
                    enemy.setWanderTarget(endNode);
                }
                break;
            case PICKUP:
                excluded.clear();
                excluded.add((crates.get(enemy.getTarget() - 1).getObstacle()));
                graph = worldGraphs.get(enemy.getTarget());
                pathfinder = pathfinders.get(enemy.getTarget());
//        WorldGraph.IndexNode endNode = findBestEndNode((Crate) crates.get(enemy.getTarget()));
                startNode = graph.findClosestReachableNode(enemy.getObstacle());
                endNode = graph.findClosestReachableNode((crates.get(enemy.getTarget() - 1).getObstacle()));
                break;
            case ESCAPE:
                excluded.clear();
                graph = worldGraphs.get(0);
                pathfinder = pathfinders.get(0);
                startNode = graph.findClosestReachableNode(enemy.getObstacle());
                endNode = graph.findClosestReachableNode((ventList.get(enemy.getVent()).getObstacle()));
                break;
            case ATTACK:
                excluded.clear();
                graph = worldGraphs.get(0);
                pathfinder = pathfinders.get(0);
                startNode = graph.findClosestReachableNode(enemy.getObstacle());
                endNode = graph.findClosestReachableNode(helperList.get(enemy.getAttackTarget()).getObstacle());
                break;
            case IDLE:
                startNode = null;
                endNode = null;
                break;
            default:
                graph = worldGraphs.get(0);
                pathfinder = pathfinders.get(0);
                startNode = graph.getNodeAtObs(enemy.getObstacle());
                endNode = null;
                break;
        }

//        if (startNode.equals(endNode)) {
//            // System.out.println("target reached!");
//            enemy.setMovement(0, 0);
//            return;
//        }

        // Wander logic
        if (enemy.getState() == Enemy.EnemyFSMState.WANDER) {
            // TODO: experiment w/ stealers not wandering at all
            if (enemy.getType() == Enemy.EnemyType.STEALER) {
                enemy.setState(Enemy.EnemyFSMState.PICKUP);
            }
            else if (startNode.equals(endNode)) {
                enemy.incTimesWandered();
                switch (enemy.getType()) {
                    case STEALER:
                        if (enemy.getTimesWandered() >= 1) {
                            // enemy.setState(Enemy.EnemyFSMState.IDLE);
                            enemy.setState(Enemy.EnemyFSMState.PICKUP);
                        }
                        break;
                    case ATTACKER:
                        enemy.setState(Enemy.EnemyFSMState.IDLE);
//                        if (enemy.getTimesWandered() >= 3) {
//                            enemy.setState(Enemy.EnemyFSMState.IDLE);
//                            // enemy.setState(Enemy.EnemyFSMState.ESCAPE);
//                            // System.out.println("attacker escaping");
//                        }
                        break;
                }
                enemy.setWanderTarget(null);
            }
        }

        // Attack logic
        if (enemy.getType() == Enemy.EnemyType.ATTACKER && enemy.canAttack()) {
            smallestDistToHelper = -1;
            switch (enemy.getState()) {
                case WANDER, IDLE, ESCAPE:
                    if (enemy.hasAttacked()) {
                        break;
                    }
                    for (int i = 0; i < helperList.size; i++) {
                        Helper helper = helperList.get(i);
                        if (helper.isStunned()) {
                            continue;
                        }
                        raycastTarget.set(helper.getObstacle().getPosition());

                        callback.reset();
                        callback2.reset();
                        world.rayCast(callback, enemy.getObstacle().getPosition(), raycastTarget);
//                        world.QueryAABB(callback2, enemy.getObstacle().getX() - 0.1f, enemy.getObstacle().getY() -0.1f,
//                            enemy.getObstacle().getX() + 0.1f, enemy.getObstacle().getY() + 0.1f);
                        if (!callback.collided) {
                            float tempDst = enemy.getObstacle().getPosition().dst2(raycastTarget);
                            // System.out.println("Target in sight, range: " + tempDst);
                            if (tempDst <= enemy.getAggroRange()) {
                                // System.out.println("Target detected! Helper index: " + i);
                                enemy.setState(Enemy.EnemyFSMState.ATTACK);
                                enemy.setWanderTarget(null);
                                if (smallestDistToHelper == -1 || tempDst < smallestDistToHelper) {
                                    smallestDistToHelper = tempDst;
                                    enemy.setAttackTarget(i);
                                    enemy.setMovement(raycastTarget.x - enemy.getObstacle().getX(),
                                        raycastTarget.y - enemy.getObstacle().getY());
                                    enemy.setPath(null);
                                    enemy.setPathIndex(0);
                                    callback.reset();
                                    return;
                                }
                            }
                            callback.reset();
//                            enemy.getEdge1();
//                            posCache.set(raycastTarget);
//                            posCache.add(enemy.getOffset());
//                            world.rayCast(callback, enemy.getEdge1(), posCache);
//                            if (!callback.collided ) {
//                                // System.out.println("Original target: " + raycastTarget + "\n");
//                                // System.out.println("First target: " + posCache + "\n");
//                                callback.reset();
//                                enemy.getEdge2();
//                                posCache.set(raycastTarget);
//                                posCache.add(enemy.getOffset());
//                                world.rayCast(callback, enemy.getEdge2(), posCache);
//                                if (!callback.collided) {
//                                    float tempDst = enemy.getObstacle().getPosition().dst2(raycastTarget);
//                                    System.out.println("Target in sight, range: " + tempDst);
//                                    if (tempDst <= enemy.getAggroRange()) {
//                                        System.out.println("Target detected! Helper index: " + i);
//                                        enemy.setState(Enemy.EnemyFSMState.ATTACK);
//                                        enemy.setWanderTarget(null);
//                                        if (smallestDistToHelper == -1 || tempDst < smallestDistToHelper) {
//                                            smallestDistToHelper = tempDst;
//                                            enemy.setAttackTarget(i);
//                                        }
//                                    }
//                                }
//                            }
                        }
                    }
                    break;
                case ATTACK:
                    Helper helper = helperList.get(enemy.getAttackTarget());
                    if (helper.isStunned()) {
                        enemy.setState(Enemy.EnemyFSMState.WANDER);
                        break;
                    }
                    float tempDst = enemy.getObstacle().getPosition().dst2(helper.getObstacle().getPosition());
                    // System.out.println("Aggroed to target, range: " + tempDst);
                    if (tempDst > enemy.getAggroRange()) {
                        // System.out.println("Target out of range");
                        enemy.setState(Enemy.EnemyFSMState.WANDER);
                        break;
                    }
                    raycastTarget.set(helper.getObstacle().getPosition());

                    callback.reset();
                    world.rayCast(callback, enemy.getObstacle().getPosition(), raycastTarget);
//                        world.QueryAABB(callback2, enemy.getObstacle().getX() - 0.1f, enemy.getObstacle().getY() -0.1f,
//                            enemy.getObstacle().getX() + 0.1f, enemy.getObstacle().getY() + 0.1f);
                    if (!callback.collided) {
                        enemy.setMovement(raycastTarget.x - enemy.getObstacle().getX(),
                            raycastTarget.y - enemy.getObstacle().getY());
                        enemy.setPath(null);
                        enemy.setPathIndex(0);
                        return;
                        // break;
                    }
                    else {
                        callback.reset();
                        enemy.getEdge1();
                        posCache.set(raycastTarget);
                        posCache.add(enemy.getOffset());
                        world.rayCast(callback, enemy.getEdge1(), posCache);
                        if (!callback.collided ) {
                            enemy.setMovement(raycastTarget.x - enemy.getObstacle().getX(),
                                raycastTarget.y - enemy.getObstacle().getY());
                            enemy.setPath(null);
                            enemy.setPathIndex(0);
                            return;
                            // break;
                        }
                        else {
                            // System.out.println("Original target: " + raycastTarget + "\n");
                            // System.out.println("First target: " + posCache + "\n");
                            callback.reset();
                            enemy.getEdge2();
                            posCache.set(raycastTarget);
                            posCache.add(enemy.getOffset());
                            world.rayCast(callback, enemy.getEdge2(), posCache);
                            if (!callback.collided) {
                                enemy.setMovement(raycastTarget.x - enemy.getObstacle().getX(),
                                    raycastTarget.y - enemy.getObstacle().getY());
                                enemy.setPath(null);
                                enemy.setPathIndex(0);
                                return;
                                // break;
                            }
                            else {
                                // System.out.println("Target out of LOS");
                                enemy.setState(Enemy.EnemyFSMState.WANDER);
                            }
                        }
                    }
            }
        }

        // Idle logic (transition back to wandering or escaping)
        if (enemy.getState() == Enemy.EnemyFSMState.IDLE) {
            if (!enemy.isDoneIdling()) {
                return;
            }
            switch(enemy.getType()) {
                case STEALER:
                    System.out.println(enemy.getTimesWandered());
                    if (enemy.getTimesWandered() >= 0) {
                        enemy.setState(Enemy.EnemyFSMState.PICKUP);
                    }
                    else {
                        enemy.setState(Enemy.EnemyFSMState.WANDER);
                    }
                    break;
                case ATTACKER:
                    if (enemy.getTimesWandered() >= 3) {
                        enemy.setState(Enemy.EnemyFSMState.ESCAPE);
                    }
                    else {
                        enemy.setState(Enemy.EnemyFSMState.WANDER);
                    }
                    break;
            }
        }

        if (endNode == null) {
            return;
        }

        // Don't recompute path if enemy already has one
        /**
         if (enemy.getPath() != null && !enemy.getEndNodeChanged())  {
         int nodeIdx = 0;
         for (WorldGraph.IndexNode node : enemy.getPath()) {
         if (node.equals(startNode)) {
         break;
         }
         nodeIdx++;
         }
         if (nodeIdx + 1 >= enemy.getPath().getCount()) {
         System.out.println("no valid node found");
         }
         else {
         WorldGraph.IndexNode first = enemy.getPath().get(nodeIdx);
         WorldGraph.IndexNode second = enemy.getPath().get(nodeIdx + 1);

         float x = second.getXPos() - first.getXPos();
         float y = second.getYPos() - first.getYPos();
         enemy.setMovement(x, y);
         return;
         }
         }
         */

        // If we have a cached path, follow it — don't recompute
        if (enemy.getPath() != null && !enemy.getState().equals(Enemy.EnemyFSMState.ATTACK)) {
            advanceAlongPath();
            return;
        }

        // No path yet — compute one
        path = new AIController.SmoothPath();
        if (!pathfinder.searchNodePath(startNode, endNode, heuristic, path)) {
            System.out.println("[Stealer] A* failed. state=" + enemy.getState()
                + " type=" + enemy.getType()
                + " target=" + enemy.getTarget()
                + " levelNull=" + (level == null));

            // Stealer fallback: if the original crate is unreachable, redirect
            // to the nearest reachable crate that still has ingredients. The
            // switch sets the enemy's target so subsequent PICKUP frames re-
            // pathfind to the new crate naturally, and pickup-on-touch in the
            // CollisionController uses the same target index.
            if (enemy.getState() == Enemy.EnemyFSMState.PICKUP
                && enemy.getType() == Enemy.EnemyType.STEALER
                && level != null) {

                boolean switched = trySwitchToNearestReachableCrate();
                System.out.println("[Stealer] fallback switched=" + switched
                    + " newTarget=" + enemy.getTarget());

                if (switched) {
                    // Recompute end node/graph against the new target. Start
                    // node is unchanged (enemy hasn't moved). One more A* this
                    // frame.
                    excluded.clear();
                    excluded.add(crates.get(enemy.getTarget() - 1).getObstacle());
                    graph = worldGraphs.get(enemy.getTarget());
                    pathfinder = pathfinders.get(enemy.getTarget());
                    startNode = graph.findClosestReachableNode(enemy.getObstacle());
                    endNode = graph.findClosestReachableNode(
                        crates.get(enemy.getTarget() - 1).getObstacle());

                    path = new AIController.SmoothPath();
                    boolean foundAfterSwitch = pathfinder.searchNodePath(
                        startNode, endNode, heuristic, path);
                    System.out.println("[Stealer] post-switch A* found=" + foundAfterSwitch
                        + " pathLen=" + path.getCount());

                    if (!foundAfterSwitch || path.getCount() <= 1) {
                        // Even the fallback didn't work this frame — give up
                        // and try again next frame. The target has already
                        // been switched so we won't endlessly retry the same
                        // dead end.
                        enemy.setMovement(0, 0);
                        return;
                    }
                    // Fall through to install the new path below.
                } else {
                    enemy.setMovement(0, 0);
                    return;
                }
            }
            else {
                // System.out.println("No path found");
                enemy.setMovement(0, 0);
                if (enemy.getState().equals(Enemy.EnemyFSMState.WANDER)) {
                    enemy.setWanderTarget(null);
                }
                return;
            }
        }
        if (path.getCount() <= 1) {
            enemy.setPath(null);
            enemy.setMovement(0, 0);
            if (enemy.getState().equals(Enemy.EnemyFSMState.WANDER)){
                enemy.setWanderTarget(null);
            }
            return;
        }

        // smoother.smoothPath(path);

        enemy.setPath(path);
        enemy.setPathIndex(0); // reset index on new path
        advanceAlongPath();

        // Apply honey slowdown if enemy is on honey
        applyHoneySlowdown(enemy);
        return;
    }

    /**
     * Stealer fallback: when the enemy's current target crate is unreachable,
     * try to switch its target to the Euclidean-nearest crate that
     *   (a) still has at least one ingredient available, and
     *   (b) is reachable via A* from the enemy's current position.
     *
     * Order: sort candidates by squared Euclidean distance ascending, then for
     * each candidate run a probe A* and take the first that succeeds. This
     * matches the chosen behavior "Euclidean distance, then verify reachable;
     * try next-closest on failure."
     *
     * Returns true if a new target was installed (caller should re-resolve
     * graph/pathfinder/start/end and retry pathfinding), false otherwise.
     */
    private boolean trySwitchToNearestReachableCrate() {
        int originalTargetIdx = enemy.getTarget(); // 1-indexed
        Vector2 enemyPos = enemy.getObstacle().getPosition();

        int n = crates.size();
        System.out.println("[Stealer] trySwitchToNearestReachableCrate: numCrates=" + n
            + " originalTarget=" + originalTargetIdx);
        int[] order = new int[n];           // 1-indexed crate target values
        float[] dists = new float[n];
        int count = 0;
        for (int i = 0; i < n; i++) {
            int targetIdx = i + 1;
            if (targetIdx == originalTargetIdx) continue;

            ObstacleSprite sprite = crates.get(i);
            if (!(sprite instanceof Crate)) continue;
            Crate crate = (Crate) sprite;
            if (level.getAvailable(crate.getIngredient()) <= 0) {
                System.out.println("[Stealer] skip crate " + targetIdx + " (no ingredient)");
                continue;
            }

            float dx = crate.getObstacle().getX() - enemyPos.x;
            float dy = crate.getObstacle().getY() - enemyPos.y;
            order[count] = targetIdx;
            dists[count] = dx * dx + dy * dy;
            count++;
        }
        System.out.println("[Stealer] candidate count=" + count);

        // Simple insertion sort — n is small (number of crates).
        for (int i = 1; i < count; i++) {
            int keyOrder = order[i];
            float keyDist = dists[i];
            int j = i - 1;
            while (j >= 0 && dists[j] > keyDist) {
                order[j + 1] = order[j];
                dists[j + 1] = dists[j];
                j--;
            }
            order[j + 1] = keyOrder;
            dists[j + 1] = keyDist;
        }

        // Try each candidate in distance order; first reachable wins.
        for (int i = 0; i < count; i++) {
            int candidateTarget = order[i];

            // Use the candidate's own graph (which excludes that crate as an
            // obstacle, like a normal PICKUP for it would).
            WorldGraph candidateGraph = worldGraphs.get(candidateTarget);
            WorldGraph.IndexNode candidateStart =
                candidateGraph.findClosestReachableNode(enemy.getObstacle());
            WorldGraph.IndexNode candidateEnd =
                candidateGraph.findClosestReachableNode(
                    crates.get(candidateTarget - 1).getObstacle());

            if (candidateStart == null || candidateEnd == null) {
                System.out.println("[Stealer] probe " + candidateTarget
                    + " null nodes (start=" + candidateStart + " end=" + candidateEnd + ")");
                continue;
            }

            fallbackProbePath.clear();
            boolean found = pathfinders.get(candidateTarget)
                .searchNodePath(candidateStart, candidateEnd, heuristic, fallbackProbePath);
            System.out.println("[Stealer] probe " + candidateTarget
                + " found=" + found + " pathLen=" + fallbackProbePath.getCount());
            if (found && fallbackProbePath.getCount() > 1) {
                enemy.setTarget(candidateTarget);
                enemy.setPath(null);     // clear any stale cached path
                enemy.setPathIndex(0);
                return true;
            }
        }

        return false;
    }

    /**
     * Applies honey slowdown to an enemy's velocity
     * @param enemy The enemy to apply slowdown to
     */
    private void applyHoneySlowdown(Enemy enemy) {
        if (enemy.isOnHoney()) {
            float slowdownFactor = enemy.getHoneySlowdownFactor();
            Vector2 velocity = enemy.getObstacle().getLinearVelocity();
            velocity.scl(slowdownFactor);
            enemy.getObstacle().setLinearVelocity(velocity);
        }
    }

    private static final float WAYPOINT_RADIUS = 0.15f; // tune to your tile size

    private void advanceAlongPath() {
        var path = enemy.getPath();
        int pathIndex = enemy.getPathIndex();
        if (path == null || path.getCount() == 0) {
            System.out.println("Cannot advance along path");
            return;
        }

        Vector2 agentPos = enemy.getObstacle().getPosition();

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
            enemy.setPath(null);
            enemy.setPathIndex(0);
            enemy.setMovement(0, 0);
            return;
        }

        WorldGraph.IndexNode target = path.get(pathIndex);
        float dx = target.getXPos() - agentPos.x;
        float dy = target.getYPos() - agentPos.y;
        enemy.setMovement(dx, dy);
        enemy.setPathIndex(pathIndex);
    }

}

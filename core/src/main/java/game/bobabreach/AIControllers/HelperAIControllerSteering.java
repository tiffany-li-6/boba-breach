package game.bobabreach.AIControllers;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.steer.Proximity;
import com.badlogic.gdx.ai.steer.behaviors.*;
import com.badlogic.gdx.ai.steer.utils.RayConfiguration;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.ai.steer.utils.rays.CentralRayWithWhiskersConfiguration;
import com.badlogic.gdx.ai.steer.utils.rays.SingleRayConfiguration;
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.physics2.ObstacleSelector;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.util.PooledList;
import game.bobabreach.CollisionController;
import game.bobabreach.GameObjects.Cup;
import game.bobabreach.GameObjects.Helper;
import game.bobabreach.GameObjects.HelperSteerable;
import game.bobabreach.WorldGraph;

public class HelperAIControllerSteering extends AIController {

    /**
     * Reference to the helper currently being controlled.
     */
    private HelperSteerable helper;

    /**
     * Reference to the helper selector indicator.
     */
    private ObstacleSelector helperSelector;

    /** Steering Behaviors */

    private World world;

    private TargetLocation locCache = new TargetLocation();

//    private PrioritySteering<Vector2> helperSB = new PrioritySteering<>(helper);;

    private Seek<Vector2> seekSB = new Seek<>(helper, locCache);
    private Arrive<Vector2> arriveSB = new Arrive<>(helper, locCache);

    private RaycastObstacleAvoidance<Vector2> avoidSB;
    private Box2dRaycastCollisionDetector detector;



    public HelperAIControllerSteering(PooledList<ObstacleSprite> crates, Array<WorldGraph> graphs, Cup cup,
                                      ObstacleSelector selector, World world) {
        super(crates, graphs, cup);
        this.world = world;
        helperSelector = selector;
        arriveSB.setArrivalTolerance(0.1f);
        arriveSB.setTimeToTarget(0.01f);
        arriveSB.setDecelerationRadius(1f);
        detector = new Box2dRaycastCollisionDetector(world);
        // System.out.println(graphs.size);
    }

    public void setHelper(HelperSteerable h) {
        helper = h;
        // arriveSB.setOwner(helper);
    }

    /**
     * Sets the currently stored helper's movement.
     */
    public void setHelperMovement() {
        switch (helper.getState()) {
            case MOVE:
                if( Math.abs((helper.getMoveTarget().x - helper.getPosition().x) + (
                    helper.getMoveTarget().y - helper.getPosition().y)) < 0.1) {
                    helper.setState(HelperSteerable.HelperFSMState.IDLE);
                    // helper.setBehavior(null);
                    break;
                }
//                PrioritySteering<Vector2> helperSB = new PrioritySteering<>(helper);
//
//                locCache.setPosition(helper.getMoveTarget().x, helper.getMoveTarget().y);
////                seekSB.setOwner(helper);
////                seekSB.setTarget(locCache);
//                arriveSB.setOwner(helper);
//                arriveSB.setTarget(locCache);
//
//                avoidSB = new RaycastObstacleAvoidance<>(helper, new SingleRayConfiguration<>(helper, 1.5f),
//                    detector);

//                helperSB.add(avoidSB);
//                helperSB.add(arriveSB);
//
////                helper.setBehavior(helperSB);
//                helper.setBehavior(arriveSB);
                break;
            case PICKUP:

                break;
            case DROPOFF:

                break;
            case RETURN:

                break;
            default:
                break;
        }



    }

}

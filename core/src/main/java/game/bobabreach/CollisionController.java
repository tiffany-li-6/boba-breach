package game.bobabreach;

import game.bobabreach.GameObjects.Enemy.AnimState;
import java.util.Iterator;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.graphics.*;
import edu.cornell.gdiac.physics2.*;
import game.bobabreach.GameObjects.*;

public class CollisionController implements ContactListener {
    /**
     * A reference to the physics world
     */
    private World world;

    /**
     * A reference to the player character
     */
    private Player playerRef;

    /**
     * A reference to the slingshot
     */
    private Slingshot slingshotRef;

    /**
     * A reference to the cup
     */
    private Cup cupRef;

    /**
     * A reference to the list of crates
     */
    private PooledList<ObstacleSprite> crateList;

    /**
     * A reference to the portal list
     */
    private PooledList<ObstacleSprite> portalList;

    /**
     * A reference to the cobweb list
     */
    private PooledList<ObstacleSprite> cobwebList;

    /**
     * A reference to the honey list
     */
    private PooledList<ObstacleSprite> honeyList;

    /**
     * A reference to the current level
     */
    private Level level;

    /**
     * Portal queued for teleportation (to avoid modifying world during collision)
     */
    private Portal queuedTeleportPortal = null;

    /**
     * Boba queued for teleportation
     */
    private Boba queuedTeleportBoba = null;

    /**
     * How many times a helper has been stunned this instance
     */
    private int numStunned = 0;

    /**
     * How many bugs a player has killed in this instance
     */
    private int bugsKilled = 0;

    private int bobaHits = 0;

    public CollisionController(World world) {
        this.world = world;
    }

    /**
     * Sets the reference to the player avatar
     */
    public void updatePlayer(Player player) {
        playerRef = player;
    }

    /**
     * Sets the reference to the slingshot
     */
    public void updateSlingshot(Slingshot sling) {
        slingshotRef = sling;
    }

    /**
     * Sets the reference to the crate list
     */
    public void updateCrates(PooledList<ObstacleSprite> list) {
        crateList = list;
    }

    /**
     * Sets the reference to the cup
     */
    public void updateCup(Cup cup) {
        this.cupRef = cup;
    }

    /**
     * Sets the reference to the portal list
     */
    public void updatePortals(PooledList<ObstacleSprite> portals) {
        portalList = portals;
    }

    /**
     * Sets the reference to the cobweb list
     */
    public void updateCobwebs(PooledList<ObstacleSprite> list) {
        cobwebList = list;
    }

    /**
     * Sets the reference to the honey list
     */
    public void updateHoneys(PooledList<ObstacleSprite> list) {
        honeyList = list;
    }

    /**
     * Sets the reference to the Level
     */
    public void updateLevel(Level lev) {
        level = lev;
    }

    /**
     * Gets how many times a helper has been stunned this level
     * @return Number of times helper has been stunned (blocker or attacker)
     */
    public int getNumStunned() {return numStunned;}

    public void resetNumStunned() {numStunned = 0;}

    /**
     * Gets how many bugs died to the boba slingshot this level
     * @return Number of bugs that have been killed by boba (stealer or attacker)
     */
    public int getBugsKilled() {return bugsKilled;}

    public void resetBugsKilled() {bugsKilled = 0;}

    /**
     * Gets how many boba hit a target in this level (cup or killable enemy)
     * @return Number of bobas that hit a target in this level
     */
    public int getBobaHits() {return bobaHits;}

    public void resetBobaHits() {bobaHits = 0;}


    /**
     * Process any queued teleportations.
     * Call this in GameScene.postUpdate() after physics step.
     *
     * The boba's incoming direction is discarded; it exits along the
     * destination portal's facing vector at the same speed it entered.
     * The boba spawns at an offset outside the destination's solid body
     * so it does not start intersecting the exit fixture.
     */
    public void processTeleports() {
        if (queuedTeleportPortal == null || queuedTeleportBoba == null) return;

        Portal portal = queuedTeleportPortal;
        Boba boba = queuedTeleportBoba;
        queuedTeleportPortal = null;
        queuedTeleportBoba = null;

        Portal linkedPortal = portal.getLinkedPortal();
        if (linkedPortal == null) return;

        // Preserve speed magnitude, replace direction with exit's facing.
        Vector2 currentVel = boba.getObstacle().getLinearVelocity();
        float speed = currentVel.len();
        Vector2 facing = linkedPortal.getFacing();

        // Spawn just outside the (solid) exit body.
        Vector2 spawn = linkedPortal.getExitPosition(new Vector2());

        boba.getObstacle().setPosition(spawn.x, spawn.y);
        boba.getObstacle().setLinearVelocity(
            new Vector2(facing.x * speed, facing.y * speed));

        portal.activateCooldown();
        // Exit portal does not need a cooldown: it is solid, so its
        // beginContact will not queue a teleport even if boba touches it.
    }


    /**
     * Called when two fixtures begin to touch.
     *
     * @param contact The two touching fixtures.
     */
    @Override
    public void beginContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        try {
            ObstacleSprite bd1 = (ObstacleSprite) body1.getUserData();
            ObstacleSprite bd2 = (ObstacleSprite) body2.getUserData();

            // Test whether player hitbox overlaps slingshot
            if (bd1 == playerRef && bd2.getName().equals("slingshot")) {
                playerRef.setCanEnterShooting(true);
            }

            if (bd2 == playerRef && bd1.getName().equals("slingshot")) {
                playerRef.setCanEnterShooting(true);
            }

            // Handle player/crate collisions
            if (bd1 == playerRef && bd2.getName().startsWith("crate")) {
                playerRef.setCanPickup(true);
                playerRef.setCrateTouching((Crate) bd2);
            }
            if (bd2 == playerRef && bd1.getName().startsWith("crate")) {
                playerRef.setCanPickup(true);
                playerRef.setCrateTouching((Crate) bd1);
            }

            // Handle player/enemy collisions
            if (bd1 == playerRef && bd2.getName().startsWith("enemy")) {
                playerRef.removeIngredient();
            }
            if (bd2 == playerRef && bd1.getName().startsWith("enemy")) {
                playerRef.removeIngredient();
            }

            // Handle player/cup collisions
            if (bd1 == playerRef && bd2.getName().equals("cup")) {
                playerRef.setTouchingCup(true);
            }
            if (bd2 == playerRef && bd1.getName().equals("cup")) {
                playerRef.setTouchingCup(true);
            }

            // Handle boba / portal collisions (one-way)
            // Entry portals (sensors, canEnter == true) queue a teleport.
            // Exit portals (solid, canEnter == false) act as walls; the
            // physics solver bounces boba and we just record the bounce.
            if (bd1.getName().equals("boba") && bd2.getName().equals("portal")) {
                Portal portal = (Portal) bd2.getObstacle().getUserData();
                Boba boba = (Boba) bd1.getObstacle().getUserData();

                if (portal != null) {
                    if (portal.canEnter() && portal.canTeleport()) {
                        queuedTeleportPortal = portal;
                        queuedTeleportBoba = boba;
                    } else if (!portal.canEnter()) {
                        // Hit an exit portal from outside: solid bounce.
                        boba.increaseBounces();
                    }
                }
            }

            if (bd2.getName().equals("boba") && bd1.getName().equals("portal")) {
                Portal portal = (Portal) bd1.getObstacle().getUserData();
                Boba boba = (Boba) bd2.getObstacle().getUserData();

                if (portal != null) {
                    if (portal.canEnter() && portal.canTeleport()) {
                        queuedTeleportPortal = portal;
                        queuedTeleportBoba = boba;
                    } else if (!portal.canEnter()) {
                        boba.increaseBounces();
                    }
                }
            }


            /**
             // Handle boba collisions with player
             if (bd1 == playerRef && bd2.getName().equals("boba")) {
             contact.setEnabled(false);
             Boba boba = (Boba) bd2.getObstacle().getUserData();
             boba.markForDestruction();
             }
             if (bd2 == playerRef && bd1.getName().equals("boba")) {
             contact.setEnabled(false);
             Boba boba = (Boba) bd1.getObstacle().getUserData();
             boba.markForDestruction();
             }
             */
            //Handle boba collisions with cup
            if (bd1.getName().equals("boba") && bd2.getName().equals("cup")) {
                if (level.getCupCount(IngredientType.BOBA) < level.getRequired(IngredientType.BOBA)) {
                    contact.setEnabled(false);
                    Boba boba = (Boba) bd1.getObstacle().getUserData();
                    if(!boba.getMark()){
                        level.bobaInCup();
                        bobaHits++;
                    }
                    boba.markForDestruction();
                }
            }
            if (bd2.getName().equals("boba") && bd1.getName().equals("cup")) {
                if (level.getCupCount(IngredientType.BOBA) < level.getRequired(IngredientType.BOBA)) {
                    contact.setEnabled(false);
                    Boba boba = (Boba) bd2.getObstacle().getUserData();
                    if(!boba.getMark()){
                        level.bobaInCup();
                        bobaHits++;
                    }
                    boba.markForDestruction();
                }
            }

            // Handle boba collisions with enemy
            if (bd1.getName().equals("enemy") && bd2.getName().equals("boba")) {
                contact.setEnabled(false);
                Boba boba = (Boba) bd2.getObstacle().getUserData();
                Enemy enemy = (Enemy) bd1.getObstacle().getUserData();
                if (enemy.hasIngredient() && !boba.getMark()) {  // Check if carrying ingredient & return it
                    level.returnToCrate(enemy.getHeldIngredient());
                }
                if(!boba.getMark()){
                    bobaHits++;
                }
                if(!enemy.getMark()){
                    bugsKilled++;
                }
                boba.markForDestruction();
                //enemy.markForDestruction();
                //instead play death animation and will destruct it afterwards
                enemy.triggerDeath();
            } else if (bd2.getName().equals("enemy") && bd1.getName().equals("boba")) {
                contact.setEnabled(false);
                Boba boba = (Boba) bd1.getObstacle().getUserData();
                Enemy enemy = (Enemy) bd2.getObstacle().getUserData();
                if (enemy.hasIngredient() && !boba.getMark()) { // Check if carrying ingredient & return it
                    level.returnToCrate(enemy.getHeldIngredient());
                }
                if(!boba.getMark()){
                    bobaHits++;
                }
                if(!enemy.getMark()){
                    bugsKilled++;
                }
                boba.markForDestruction();
                //enemy.markForDestruction();
                //instead play death animation and will destruct it afterwards
                enemy.triggerDeath();
            }

            if (bd1.getName().equals("helper") && bd2.getName().equals("blocker")) {
                Helper helper = (Helper) bd1.getObstacle().getUserData();
                Blocker blocker = (Blocker) bd2.getObstacle().getUserData();
                helper.setCollidingBlocker(true);
                // helper.setState(Helper.HelperFSMState.IDLE);
                blocker.setCollidingHelper(true);
            }
            if (bd2.getName().equals("helper") && bd1.getName().equals("blocker")) {
                Helper helper = (Helper) bd2.getObstacle().getUserData();
                Blocker blocker = (Blocker) bd1.getObstacle().getUserData();
                helper.setCollidingBlocker(true);
                // helper.setState(Helper.HelperFSMState.IDLE);
                blocker.setCollidingHelper(true);
            }

            // Helper collides w/ crate HITBOX (for pathfinding purposes only)

            if (bd1.getName().equals("helper") && fix2.getUserData() != null &&
                fix2.getUserData().equals("crate_hitbox")) {
                Crate crate = (Crate) bd2.getObstacle().getUserData();
                Helper helper = (Helper) bd1.getObstacle().getUserData();
                if (helper.getState() != Helper.HelperFSMState.PICKUP ||
                    (helper.getState() == Helper.HelperFSMState.PICKUP &&
                        !crate.equals(crateList.get(helper.getTarget() - 1)))) {
                    helper.setCollidingCrate(true);
                }
            }
            if (bd2.getName().equals("helper") && fix1.getUserData() != null &&
                fix1.getUserData().equals("crate_hitbox")) {
                Crate crate = (Crate) bd1.getObstacle().getUserData();
                Helper helper = (Helper) bd2.getObstacle().getUserData();
                if (helper.getState() != Helper.HelperFSMState.PICKUP ||
                    (helper.getState() == Helper.HelperFSMState.PICKUP &&
                        !crate.equals(crateList.get(helper.getTarget() - 1)))) {
                    helper.setCollidingCrate(true);
                }
            }

            // Helper collides w/ obstacle HITBOX (for pathfinding purposes only)

            if (bd1.getName().equals("helper") && fix2.getUserData() != null &&
                fix2.getUserData().equals("obstacle_hitbox")) {
                Helper helper = (Helper) bd1.getObstacle().getUserData();
                helper.setCollidingCrate(true);
            }

            if (bd2.getName().equals("helper") && fix1.getUserData() != null &&
                fix1.getUserData().equals("obstacle_hitbox")) {
                Helper helper = (Helper) bd2.getObstacle().getUserData();
                helper.setCollidingCrate(true);
            }

            // ============ BOBA / COBWEB COLLISIONS ============
            // Handle boba collisions with cobweb - destroy boba, no bounce
            if (bd1.getName().equals("boba") && bd2.getName().equals("cobweb")) {
                contact.setEnabled(false);
                Boba boba = (Boba) bd1.getObstacle().getUserData();
                boba.markForDestruction();
            }

            if (bd2.getName().equals("boba") && bd1.getName().equals("cobweb")) {
                contact.setEnabled(false);
                Boba boba = (Boba) bd2.getObstacle().getUserData();
                boba.markForDestruction();
            }

            // Helper collides w/ cobweb hitbox
            if (bd1.getName().equals("helper") && fix2.getUserData() != null &&
                fix2.getUserData().equals("cobweb_hitbox")) {
                Helper helper = (Helper) bd1.getObstacle().getUserData();
                helper.setCollidingCrate(true);
            }

            if (bd2.getName().equals("helper") && fix1.getUserData() != null &&
                fix1.getUserData().equals("cobweb_hitbox")) {
                Helper helper = (Helper) bd2.getObstacle().getUserData();
                helper.setCollidingCrate(true);
            }

            // Handle enemy collisions with honey
            if (fix1.getUserData() != null && fix1.getUserData().equals("enemy_honey_hitbox") &&
                bd2.getName().equals("honey")) {
                Enemy enemy = (Enemy) bd1.getObstacle().getUserData();
                Honey honey = (Honey) bd2.getObstacle().getUserData();
                enemy.setOnHoney(true);
            }

            if (fix2.getUserData() != null && fix2.getUserData().equals("enemy_honey_hitbox") &&
                bd1.getName().equals("honey")) {
                Enemy enemy = (Enemy) bd2.getObstacle().getUserData();
                Honey honey = (Honey) bd1.getObstacle().getUserData();
                enemy.setOnHoney(true);
            }

            // Handle blocker collisions with honey
            if (bd1.getName().equals("blocker") &&
                bd2.getName().equals("honey")) {
                Blocker blocker = (Blocker) bd1.getObstacle().getUserData();
                blocker.setOnHoney(true);
            }

            if (bd2.getName().equals("blocker") &&
                bd1.getName().equals("honey")) {
                Blocker blocker = (Blocker) bd2.getObstacle().getUserData();
                blocker.setOnHoney(true);
            }

            // Handle boba collisions with helper
//            if (bd1.getName().equals("helper") && bd2.getName().equals("boba")) {
//                contact.setEnabled(false);
//                Boba boba = (Boba) bd2.getObstacle().getUserData();
//                boba.markForDestruction();
////                Helper helper = (Helper) bd1.getObstacle().getUserData();
////                helper.removeIngredient();
////                helper.setState(Helper.HelperFSMState.RETURN);
//            }
//            if (bd2.getName().equals("helper") && bd1.getName().equals("boba")) {
//                contact.setEnabled(false);
//                Boba boba = (Boba) bd1.getObstacle().getUserData();
//                boba.markForDestruction();
////                Helper helper = (Helper) bd2.getObstacle().getUserData();
////                helper.removeIngredient();
////                helper.setState(Helper.HelperFSMState.RETURN);
//            }

            // Handle boba collisions with surfaces
            /**
             if ((bd1.getName().startsWith("wall") ||
             bd1.getName().equals("crate") ||
             bd1.getName().equals("obstacle"))
             && bd2.getName().equals("boba")
             ) {

             Boba boba = (Boba) bd2.getObstacle().getUserData();
             System.out.println("bounce detected");
             boba.increaseBounces();
             }

             if ((bd2.getName().startsWith("wall") ||
             bd2.getName().equals("crate") ||
             bd2.getName().equals("obstacle"))
             && bd1.getName().equals("boba")
             ) {

             Boba boba = (Boba) bd1.getObstacle().getUserData();
             System.out.println("bounce detected");
             boba.increaseBounces();
             }
             */

            // Enemy collides w/ destination crate

            //this block does nothing??
            if (bd1.getName().equals("enemy") && bd2.getName().equals("crate") && !fix2.isSensor()) {
                Crate crate = (Crate) bd2.getObstacle().getUserData();
                Enemy enemy = (Enemy) bd1.getObstacle().getUserData();
                if (enemy.getState() == Enemy.EnemyFSMState.PICKUP &&
                    crate.equals(crateList.get(enemy.getTarget() - 1))) {
                    if (level.getAvailable(crate.getIngredient()) > 0) {
                        enemy.giveIngredient(crate.getIngredient());
                        level.takeFromCrate(crate.getIngredient());
                    }
                    enemy.setIsTouchingCrate(true);
                    // System.out.println("escape");
                }
            }


            //this is the right block
            if (bd2.getName().equals("enemy") && bd1.getName().equals("crate") && !fix1.isSensor()) {
                Crate crate = (Crate) bd1.getObstacle().getUserData();
                Enemy enemy = (Enemy) bd2.getObstacle().getUserData();
                if (enemy.getState() == Enemy.EnemyFSMState.PICKUP &&
                    crate.equals(crateList.get(enemy.getTarget() - 1))) {
                    enemy.setState(Enemy.EnemyFSMState.ESCAPE);
                    if (level.getAvailable(crate.getIngredient()) > 0) {
                        enemy.setAnimState(AnimState.PICKUP_ANIM);
                        enemy.lockAnim();
                        enemy.giveIngredient(crate.getIngredient());
                        level.takeFromCrate(crate.getIngredient());
                        enemy.setState(Enemy.EnemyFSMState.IDLE); // stop pathfinding immediately

                        enemy.setPendingState(Enemy.EnemyFSMState.ESCAPE);

                    }else{
                        enemy.setState(Enemy.EnemyFSMState.ESCAPE);

                    }
                    enemy.setIsTouchingCrate(true);
                    // System.out.println("escape");
                }
            }

            // Helper collides w/ destination crate

            //this block does nothing
            if (bd1.getName().equals("helper") && bd2.getName().equals("crate") && !fix1.isSensor() && !fix2.isSensor()) {
                Crate crate = (Crate) bd2.getObstacle().getUserData();
                Helper helper = (Helper) bd1.getObstacle().getUserData();
                helper.setCollidingCrateIdx(crate.getIndex());
                if (helper.canPickup() && helper.getState() == Helper.HelperFSMState.PICKUP &&
                    crate.equals(crateList.get(helper.getTarget() - 1))) {
                    helper.setState(Helper.HelperFSMState.IDLE);
                    if (helper.hasIngredient() && helper.getHeldIngredient() == crate.getIngredient()) {
                        helper.removeIngredient();
                        level.returnToCrate(crate.getIngredient());
                    } else if (!helper.hasIngredient() && level.getAvailable(crate.getIngredient()) > 0) {
                        helper.giveIngredient(crate.getIngredient());
                        level.takeFromCrate(crate.getIngredient());
                    }
                    // System.out.println("escape");
                }
            }

            //this block useful
            if (bd2.getName().equals("helper") && bd1.getName().equals("crate") && !fix1.isSensor() && !fix2.isSensor()) {
                Crate crate = (Crate) bd1.getObstacle().getUserData();
                Helper helper = (Helper) bd2.getObstacle().getUserData();
                helper.setCollidingCrateIdx(crate.getIndex());
                if (helper.canPickup() && helper.getState() == Helper.HelperFSMState.PICKUP &&
                    crate.equals(crateList.get(helper.getTarget() - 1))) {
                    if (helper.hasIngredient() && helper.getHeldIngredient() == crate.getIngredient()) {
                        helper.removeIngredient();
                        level.returnToCrate(crate.getIngredient());
                        helper.setState(Helper.HelperFSMState.IDLE);
                    } else if (!helper.hasIngredient() && level.getAvailable(crate.getIngredient()) > 0) {
                        if (!helper.isAnimLocked()){
                            helper.giveIngredient(crate.getIngredient());
                            level.takeFromCrate(crate.getIngredient());
                            //helper.setAnimState(Helper.AnimState.PICKUP_ANIM);
                            //helper.lockAnim();
                            helper.setState(Helper.HelperFSMState.IDLE); // stop movement during anim
                            //helper.setPendingState(Helper.HelperFSMState.IDLE); // or whatever comes next
                        }

                    }
                }
            }

            // Enemy collides w/ vent

            //does nothing
            if (bd1.getName().equals("enemy") && bd2.getName().equals("vent")) {
                SpawnerVent vent = (SpawnerVent) bd2.getObstacle().getUserData();
                Enemy enemy = (Enemy) bd1.getObstacle().getUserData();
                enemy.setIsOnVent(true);
                if (enemy.getVent() == vent.getIndex() && enemy.getState() == Enemy.EnemyFSMState.ESCAPE
                    && !enemy.getMark()) {
                    enemy.markForDestruction();
                    if (enemy.hasIngredient()) {
                        level.ingredientStolen(enemy.getHeldIngredient());
                    }
                }
            }

            //useful one
            if (bd2.getName().equals("enemy") && bd1.getName().equals("vent")) {
                SpawnerVent vent = (SpawnerVent) bd1.getObstacle().getUserData();
                Enemy enemy = (Enemy) bd2.getObstacle().getUserData();
                enemy.setIsOnVent(true);
                if (enemy.getVent() == vent.getIndex() && enemy.getState() == Enemy.EnemyFSMState.ESCAPE
                    && !enemy.getMark()) {
                    enemy.markForDestruction();
                    if (enemy.hasIngredient()) {
                        level.ingredientStolen(enemy.getHeldIngredient());
                    }
                }
            }

            // Helper collides w/ cup
            if (bd1.getName().equals("helper") && bd2.getName().equals("cup")) {
                Helper helper = (Helper) bd1.getObstacle().getUserData();
                // Only act on cup contact when the helper is actually delivering.
                if (helper.getState() == Helper.HelperFSMState.DROPOFF) {
                    if (helper.hasIngredient()) {
                        helper.setTouchingCup(true);
                    }
                    helper.setState(Helper.HelperFSMState.IDLE);
                }
            }
            if (bd2.getName().equals("helper") && bd1.getName().equals("cup")) {
                Helper helper = (Helper) bd2.getObstacle().getUserData();
                if (helper.getState() == Helper.HelperFSMState.DROPOFF) {
                    if (helper.hasIngredient()) {
                        helper.setTouchingCup(true);
                    }
                    helper.setState(Helper.HelperFSMState.IDLE);
                }
            }

            // Helper collides w/ its spawn
            if (bd1.getName().equals("helper") && bd2.getName().equals("station")) {
                HelperSpawn station = (HelperSpawn) bd2.getObstacle().getUserData();
                Helper helper = (Helper) bd1.getObstacle().getUserData();
                if (helper.getState() == Helper.HelperFSMState.RETURN &&
                    station.equals(helper.getSpawn())) {
                    helper.removeIngredient();
                    helper.setState(Helper.HelperFSMState.IDLE);
                }
            }
            if (bd2.getName().equals("helper") && bd1.getName().equals("station")) {
                HelperSpawn station = (HelperSpawn) bd1.getObstacle().getUserData();
                Helper helper = (Helper) bd2.getObstacle().getUserData();
                if (helper.getState() == Helper.HelperFSMState.RETURN &&
                    station.equals(helper.getSpawn())) {
                    helper.removeIngredient();
                    helper.setState(Helper.HelperFSMState.IDLE);
                }
            }

            // boba hits blocker
            if (bd1.getName().equals("boba") && bd2.getName().equals("blocker")) {

                Boba boba = (Boba) bd1.getObstacle().getUserData();
                Blocker blocker = (Blocker) bd2.getObstacle().getUserData();

                boba.increaseBounces();

                // blocker.getObstacle().markRemoved(true);
            }

            if (bd2.getName().equals("boba") && bd1.getName().equals("blocker")) {

                Boba boba = (Boba) bd2.getObstacle().getUserData();
                Blocker blocker = (Blocker) bd1.getObstacle().getUserData();

                boba.increaseBounces();

                // blocker.getObstacle().markRemoved(true);
            }

            // Helper collides w/ enemy
            if (bd1.getName().equals("helper") && bd2.getName().equals("enemy")) {
                Helper helper = (Helper) bd1.getObstacle().getUserData();
                Enemy enemy = (Enemy) bd2.getObstacle().getUserData();
                if (enemy.canAttack() && !helper.isStunned()) {
                    if (helper.hasIngredient()) {
                        level.returnToCrate(helper.getHeldIngredient());
                        helper.removeIngredient();
                    }
                    if (!helper.isStunned()) {
                        numStunned++;
                        helper.stun();
                    }
                    if (enemy.getState() == Enemy.EnemyFSMState.ATTACK) {
                        enemy.setHasAttacked(true);
                        enemy.setState(Enemy.EnemyFSMState.ESCAPE);
                    }
                    if (enemy.getIsOnVent()) {
                        enemy.markForDestruction();
                    }
                }

            }
            if (bd2.getName().equals("helper") && bd1.getName().equals("enemy")) {
                Helper helper = (Helper) bd2.getObstacle().getUserData();
                Enemy enemy = (Enemy) bd1.getObstacle().getUserData();
                if (enemy.canAttack()) {
                    if (helper.hasIngredient() && !helper.isStunned()) {
                        level.returnToCrate(helper.getHeldIngredient());
                        helper.removeIngredient();
                    }
                    if (!helper.isStunned()) {
                        numStunned++;
                        helper.stun();
                    }
                    if (enemy.getState() == Enemy.EnemyFSMState.ATTACK) {
                        enemy.setHasAttacked(true);
                        enemy.setState(Enemy.EnemyFSMState.ESCAPE);
                    }
                    if (enemy.getIsOnVent()) {
                        enemy.markForDestruction();
                    }
                }
            }
            // Helper collides w/ blocker
            if (bd1.getName().equals("helper") && bd2.getName().equals("blocker")) {
                Helper helper = (Helper) bd1.getObstacle().getUserData();
                if (!helper.isStunned()) {
                    if (helper.hasIngredient()) {
                        level.returnToCrate(helper.getHeldIngredient());
                        helper.removeIngredient();
                    }
                    numStunned++;
                    helper.stun();
                }
            }
            if (bd2.getName().equals("helper") && bd1.getName().equals("blocker")) {
                Helper helper = (Helper) bd2.getObstacle().getUserData();
                if (!helper.isStunned()) {
                    if (helper.hasIngredient()) {
                        level.returnToCrate(helper.getHeldIngredient());
                        helper.removeIngredient();
                    }
                    numStunned++;
                    helper.stun();
                }
            }

            // Slingshot
            if (fix1.getUserData() != null && fix1.getUserData().equals("slingshot_hitbox")) {
                if (bd2.getObstacle().getY() > bd1.getObstacle().getY()) {
                    Slingshot s = (Slingshot) bd1;
                    s.setAtTop(true);
                }
                if (bd2.getObstacle().getY() < bd1.getObstacle().getY()) {
                    Slingshot s = (Slingshot) bd1;
                    s.setAtBottom(true);
                }
            }
            if (fix2.getUserData() != null && fix2.getUserData().equals("slingshot_hitbox")) {
                if (bd1.getObstacle().getY() > bd2.getObstacle().getY()) {
                    Slingshot s = (Slingshot) bd2;
                    s.setAtTop(true);
                }
                if (bd1.getObstacle().getY() < bd2.getObstacle().getY()) {
                    Slingshot s = (Slingshot) bd2;
                    s.setAtBottom(true);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when two fixtures cease to touch.
     *
     * @param contact The two touching fixtures.
     */
    @Override
    public void endContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        try {
            ObstacleSprite bd1 = (ObstacleSprite) body1.getUserData();
            ObstacleSprite bd2 = (ObstacleSprite) body2.getUserData();

            // Enemy no longer collides w/ vent
            if (bd1.getName().equals("enemy") && bd2.getName().equals("vent")) {
                SpawnerVent vent = (SpawnerVent) bd2.getObstacle().getUserData();
                Enemy enemy = (Enemy) bd1.getObstacle().getUserData();
                enemy.setIsOnVent(false);
            }
            if (bd2.getName().equals("enemy") && bd1.getName().equals("vent")) {
                SpawnerVent vent = (SpawnerVent) bd1.getObstacle().getUserData();
                Enemy enemy = (Enemy) bd2.getObstacle().getUserData();
                enemy.setIsOnVent(false);
            }

            // Test whether player hitbox overlaps slingshot
            if (bd1 == playerRef && bd2.getName().equals("slingshot")) {
                playerRef.setCanEnterShooting(false);
            }

            if (bd2 == playerRef && bd1.getName().equals("slingshot")) {
                playerRef.setCanEnterShooting(false);
            }

            // Handle player/crate collisions
            if (bd1 == playerRef && bd2.getName().startsWith("crate")) {
                playerRef.setCanPickup(false);
                playerRef.setCrateTouching(null);
            }
            if (bd2 == playerRef && bd1.getName().startsWith("crate")) {
                playerRef.setCanPickup(false);
                playerRef.setCrateTouching(null);
            }
            // Handle player/cup collisions
            if (bd1 == playerRef && bd2.getName().equals("cup")) {
                playerRef.setTouchingCup(false);
            }
            if (bd2 == playerRef && bd1.getName().equals("cup")) {
                playerRef.setTouchingCup(false);
            }

            // Helper collides w/ cup
            if (bd1.getName().equals("helper") && bd2.getName().equals("cup")) {
                Helper helper = (Helper) bd1.getObstacle().getUserData();
                helper.setTouchingCup(false);
            }
            if (bd2.getName().equals("helper") && bd1.getName().equals("cup")) {
                Helper helper = (Helper) bd2.getObstacle().getUserData();
                helper.setTouchingCup(false);
            }

            // Helper no longer collides w/ blocker
            if (bd1.getName().equals("helper") && bd2.getName().equals("blocker")) {
                Helper helper = (Helper) bd1.getObstacle().getUserData();
                Blocker blocker = (Blocker) bd2.getObstacle().getUserData();
                helper.setCollidingBlocker(false);
                blocker.setCollidingHelper(false);
            }
            if (bd2.getName().equals("helper") && bd1.getName().equals("blocker")) {
                Helper helper = (Helper) bd2.getObstacle().getUserData();
                Blocker blocker = (Blocker) bd1.getObstacle().getUserData();
                helper.setCollidingBlocker(false);
                blocker.setCollidingHelper(false);
            }

            // Helper no longer collides w/ crate
            if (bd1.getName().equals("helper") && bd2.getName().equals("crate")) {
                Helper helper = (Helper) bd1.getObstacle().getUserData();
                helper.resetCollidingCrateIdx();
            }
            if (bd2.getName().equals("helper") && bd1.getName().equals("crate")) {
                Helper helper = (Helper) bd2.getObstacle().getUserData();
                helper.resetCollidingCrateIdx();
            }

            // Helper no longer collides w/ crate HITBOX
            if (bd1.getName().equals("helper") && fix2.getUserData() != null &&
                fix2.getUserData().equals("crate_hitbox")) {
                Helper helper = (Helper) bd1.getObstacle().getUserData();
                helper.setCollidingCrate(false);
            }
            if (bd2.getName().equals("helper") && fix1.getUserData() != null &&
                fix1.getUserData().equals("crate_hitbox")) {
                Helper helper = (Helper) bd2.getObstacle().getUserData();
                helper.setCollidingCrate(false);
            }

            if (bd1.getName().equals("helper") && fix2.getUserData() != null &&
                fix2.getUserData().equals("obstacle_hitbox")) {
                Helper helper = (Helper) bd1.getObstacle().getUserData();
                // Don't flag cup as a colliding crate — the cup has its own handler.
                if (!bd2.getName().equals("cup")) {
                    helper.setCollidingCrate(true);
                }
            }

            if (bd2.getName().equals("helper") && fix1.getUserData() != null &&
                fix1.getUserData().equals("obstacle_hitbox")) {
                Helper helper = (Helper) bd2.getObstacle().getUserData();
                helper.setCollidingCrate(false);
            }

            // Helper no longer collides w/ cobweb hitbox
            if (bd1.getName().equals("helper") && fix2.getUserData() != null &&
                fix2.getUserData().equals("cobweb_hitbox")) {
                Helper helper = (Helper) bd1.getObstacle().getUserData();
                helper.setCollidingCrate(false);
            }

            if (bd2.getName().equals("helper") && fix1.getUserData() != null &&
                fix1.getUserData().equals("cobweb_hitbox")) {
                Helper helper = (Helper) bd2.getObstacle().getUserData();
                helper.setCollidingCrate(false);
            }

            // Enemy no longer on honey
            if (fix1.getUserData() != null && fix1.getUserData().equals("enemy_honey_hitbox") &&
                bd2.getName().equals("honey")) {
                Enemy enemy = (Enemy) bd1.getObstacle().getUserData();
                enemy.setOnHoney(false);
            }

            if (fix2.getUserData() != null && fix2.getUserData().equals("enemy_honey_hitbox") &&
                bd1.getName().equals("honey")) {
                Enemy enemy = (Enemy) bd2.getObstacle().getUserData();
                enemy.setOnHoney(false);
            }

            // Blocker no longer on honey - restore normal speed
            if (bd1.getName().equals("blocker") &&
                bd2.getName().equals("honey")) {
                Blocker blocker = (Blocker) bd1.getObstacle().getUserData();
                blocker.setOnHoney(false);
            }

            if (bd2.getName().equals("blocker") &&
                bd1.getName().equals("honey")) {
                Blocker blocker = (Blocker) bd2.getObstacle().getUserData();
                blocker.setOnHoney(false);
            }

            // Enemy no longer collides w/ destination crate
            if (bd1.getName().equals("enemy") && bd2.getName().equals("crate")) {
                Crate crate = (Crate) bd2.getObstacle().getUserData();
                Enemy enemy = (Enemy) bd1.getObstacle().getUserData();
                if (crate.equals(crateList.get(enemy.getTarget() - 1))) {
                    enemy.setIsTouchingCrate(false);
                }
            }
            if (bd2.getName().equals("enemy") && bd1.getName().equals("crate")) {
                Crate crate = (Crate) bd1.getObstacle().getUserData();
                Enemy enemy = (Enemy) bd2.getObstacle().getUserData();
                if (crate.equals(crateList.get(enemy.getTarget() - 1))) {
                    enemy.setIsTouchingCrate(false);
                }
            }

            // Slingshot
            if (fix1.getUserData() != null && fix1.getUserData().equals("slingshot_hitbox")) {
                if (bd2.getObstacle().getY() > bd1.getObstacle().getY()) {
                    Slingshot s = (Slingshot) bd1;
                    s.setAtTop(false);
                }
                if (bd2.getObstacle().getY() < bd1.getObstacle().getY()) {
                    Slingshot s = (Slingshot) bd1;
                    s.setAtBottom(false);
                }
            }
            if (fix2.getUserData() != null && fix2.getUserData().equals("slingshot_hitbox")) {
                if (bd1.getObstacle().getY() > bd2.getObstacle().getY()) {
                    Slingshot s = (Slingshot) bd2;
                    s.setAtTop(false);
                }
                if (bd1.getObstacle().getY() < bd2.getObstacle().getY()) {
                    Slingshot s = (Slingshot) bd2;
                    s.setAtBottom(false);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    /**
//     * Handles portal collision when the player touches a portal.
//     * Queues teleportation to happen after physics update.
//     *
//     * @param player The player character
//     * @param portal The portal being touched
//     */
//    private void handlePortalCollision(Player player, Portal portal) {
//        try {
//            if (player == null) {
//                Gdx.app.error("Portal", "Player is null in handlePortalCollision");
//                return;
//            }
//            if (portal == null) {
//                Gdx.app.error("Portal", "Portal is null in handlePortalCollision");
//                return;
//            }
//
//            Gdx.app.log("Portal", "handlePortalCollision called");
//
//            if (!portal.canTeleport()) {
//                Gdx.app.log("Portal", "Portal on cooldown");
//                return;
//            }
//
//            Portal linkedPortal = portal.getLinkedPortal();
//            Gdx.app.log("Portal", "Linked portal: " + (linkedPortal != null ? "exists" : "null"));
//
//            if (linkedPortal != null) {
//                Gdx.app.log("Portal", "Queuing teleport");
//                // Queue the teleportation instead of doing it now
//                // This avoids modifying the physics world during collision detection
//                queuedTeleportPortal = portal;
//            }
//        } catch (Exception e) {
//            Gdx.app.error("Portal", "Exception in handlePortalCollision: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    /**
     * Returns the index of the crate at a world coordinate. Returns -1 if not selecting a crate or obstacle.
     * Returns -2 if selecting an obstacle, but not a crate.
     *
     * @return Index of selected crate
     */
    public int getSeletedCrate(float x, float y) {
        CrateQueryCallback callback = new CrateQueryCallback();
        world.QueryAABB(callback, x - 0.01f, y - 0.01f, x + 0.01f, y + 0.01f);
        return callback.crateIdx;
    }

    /**
     * Returns whether the cup was selected.
     *
     * @return Whether cup was selected by helperSelector
     */
    public boolean isSelectingCup(float x, float y) {
        CupQueryCallback callback = new CupQueryCallback();
        world.QueryAABB(callback, x - 0.01f, y - 0.01f, x + 0.01f, y + 0.01f);
        return callback.isSelected;
    }

    /**
     * Callback to find selected crate
     */
    private class CrateQueryCallback implements QueryCallback {
        public int crateIdx = -1;

        /**
         * Called for each fixture found in the query AABB.
         *
         * @param fixture fixture to check
         * @return false to terminate the query.
         */
        @Override
        public boolean reportFixture(Fixture fixture) {
            // System.out.println(fixture.getDensity());
            if (!fixture.isSensor()) {
                int idx = 0;
                if (
                    fixture.getBody() != null &&
                    fixture.getBody().getUserData() != null &&
                    ((ObstacleSprite) (fixture.getBody().getUserData())).getName() != null &&
                        (((ObstacleSprite) (fixture.getBody().getUserData())).getName().equals("obstacle") ||
                    ((ObstacleSprite) (fixture.getBody().getUserData())).getName().equals("cobweb"))) {
                    crateIdx = -2;
                }
                for (ObstacleSprite crate : crateList) {
                    if (crate.getObstacle().getBody().equals(fixture.getBody())) {
                        crateIdx = idx;
                        return false;
                    }
                    idx++;
                }
            }
            return true;
        }
    }

    ;

    /**
     * Callback to detect collision with cup
     */
    private class CupQueryCallback implements QueryCallback {
        public boolean isSelected = false;

        /**
         * Called for each fixture found in the query AABB.
         *
         * @param fixture fixture to check
         * @return false to terminate the query.
         */
        @Override
        public boolean reportFixture(Fixture fixture) {
            // System.out.println(fixture.getDensity());
            if (!fixture.isSensor()) {
                if (cupRef.getObstacle().getBody().equals(fixture.getBody())) {
                    isSelected = true;
                    return false;
                }
            }
            return true;
        }
    }

    ;


    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        try {
            ObstacleSprite bd1 = (ObstacleSprite) body1.getUserData();
            ObstacleSprite bd2 = (ObstacleSprite) body2.getUserData();

            if (fix1.getUserData() != null && fix1.getUserData().equals("blocker_phys_hitbox")
                && bd2.getName().equals("helper")) {
                contact.setEnabled(false);
            }
            if (fix2.getUserData() != null && fix2.getUserData().equals("blocker_phys_hitbox")
                && bd1.getName().equals("helper")) {
                contact.setEnabled(false);
            }

            // Handle boba collisions with helper
//            if (bd1.getName().equals("helper") && bd2.getName().equals("boba")) {
//                contact.setEnabled(false);
//            }
//            if (bd2.getName().equals("helper") && bd1.getName().equals("boba")) {
//                contact.setEnabled(false);
//            }

            // Let helper clip into crate while pathfinding // TODO
//            if (bd1.getName().equals("helper") && bd2.getName().equals("crate")
//            && (fix2.getUserData() == null || !fix2.getUserData().equals("circle_hitbox"))) {
//                contact.setEnabled(false);
//            }
//            if (bd2.getName().equals("helper") && bd1.getName().equals("crate")
//            && (fix1.getUserData() == null || !fix1.getUserData().equals("circle_hitbox"))) {
//                contact.setEnabled(false);
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {

    }
}

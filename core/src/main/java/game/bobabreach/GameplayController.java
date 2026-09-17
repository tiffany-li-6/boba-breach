//package game.bobabreach;
//
//import com.badlogic.gdx.utils.*;
//import com.badlogic.gdx.graphics.Texture;
//
//import edu.cornell.gdiac.assets.*;
//import edu.cornell.gdiac.graphics.*;
//import edu.cornell.gdiac.util.RandomGenerator;
//
///**
// * Controller to handle gameplay interactions.
// * </summary>
// * <remarks>
// * This controller also acts as the root class for all the models.
// */
//public class GameplayController {
//    // Graphics assets for the entities
//    /**
//     * Sprite sheet for all ships, as they look the same
//     */
//    private SpriteSheet playerSprite;
//
//    /**
//     * Gameplay constants
//     */
//    private JsonValue constants;
//
//    /**
//     * Reference to player
//     */
//    private Player player;
//
//    /**
//     * Creates a new GameplayController with no active elements.
//     */
//    public GameplayController(JsonValue constants, AssetDirectory directory) {
//        this.constants = constants;
//
//        player = null;
//        beetleSprite = directory.getEntry("beetle.animation", SpriteSheet.class);
//
//        Player.setConstants(constants.get("ship"));
//    }
//
//    /**
//     * Returns the list of the currently active (not destroyed) game objects
//     * <p>
//     * As this method returns a reference and Lists are mutable, other classes
//     * can technical modify this list. That is a very bad idea. Other classes
//     * should only mark objects as destroyed and leave list management to this
//     * class.
//     *
//     * @return the list of the currently active (not destroyed) game objects
//     */
//    public Array<GameObject> getObjects() {
//        return objects;
//    }
//
//    /**
//     * Returns a reference to the currently active player.
//     * <p>
//     * This property needs to be modified if you want multiple players.
//     *
//     * @return a reference to the currently active player.
//     */
//    public Player getPlayer() {
//        return player;
//    }
//
//    /**
//     * Returns true if the currently active player is alive.
//     * <p>
//     * This property needs to be modified if you want multiple players.
//     *
//     * @return true if the currently active player is alive.
//     */
//    public boolean isAlive() {
//        return player != null;
//    }
//
//    /**
//     * Returns the number of shells currently active on the screen.
//     *
//     * @return the number of shells currently active on the screen.
//     */
//    public int getShellCount() {
//        return shellCount;
//    }
//
//    /**
//     * Starts a new game.
//     * <p>
//     * This method creates a single player, but does nothing else.
//     *
//     * @param units The physics units to use
//     */
//    public void start(float units) {
//        // Create the player's ship
//        player = new Player(units, constants.get("Player"));
//        player.setSpriteSheet(beetleSprite);
//
//        // Player must be in object list.
//        objects.add(player);
//    }
//
//    /**
//     * Resets the game, deleting all objects.
//     */
//    public void reset() {
//        player = null;
//    }
//
//    /**
//     * Resolve the actions of all game objects (player and shells)
//     * <p>
//     * You will probably want to modify this heavily in Part 2.
//     *
//     * @param input Reference to the input controller
//     * @param delta Number of seconds since last animation frame
//     */
//    public void resolveActions(InputController input, float delta) {
//        // Process the player
//        if (player != null) {
//            resolvePlayer(input, delta);
//        }
//
//        // Process the other (non-ship) objects.
//        for (GameObject o : objects) {
//            o.update(delta);
//        }
//    }
//
//    /**
//     * Process the player's actions.
//     * <p>
//     * Notice that firing bullets allocates memory to the heap. If we were
//     * REALLY worried about performance, we would use a memory pool here.
//     *
//     * @param input Reference to the input controller
//     * @param delta Number of seconds since last animation frame
//     */
//    public void resolvePlayer(InputController input, float delta) {
//        player.setMovement(input.getMovement());
//        player.setFiring(input.didFire());
//        player.setJumping(input.didJump());
//        player.update(delta);
//        if (!player.isFiring()) {
//            return;
//        }
//
//        // Create a new bullet
//        Bullet b = new Bullet(player.getX(), player.getY() + player.getRadius());
//        b.setSpriteSheet(bulletSprite);
//        backing.add(b); // Bullet added NEXT frame.
//
//        // Prevent player from firing immediately afterwards.
//        player.resetCooldown();
//    }
//}
//

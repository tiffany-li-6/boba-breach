/*
 * InputController.java
 *
 * This class buffers in input from the devices and converts it into its
 * semantic meaning. If your game had an option that allows the player to
 * remap the control keys, you would store this information in this class.
 * That way, the main GameEngine does not have to keep track of the current
 * key mapping.
 *
 * Based on the original PhysicsDemo Lab by Don Holden, 2007
 *
 * Author:  Walker M. White
 * Version: 2/8/2025
 */
package game.bobabreach;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import edu.cornell.gdiac.util.Controllers;
import edu.cornell.gdiac.util.XBoxController;

/**
 * Class for reading player input.
 *
 * This supports both a keyboard and X-Box controller. In previous solutions,
 * we only detected the X-Box controller on start-up. This class allows us to
 * hot-swap in a controller on the fly.
 */
public class InputController {
    // Sensitivity for moving crosshair with gameplay
    private static final float GP_ACCELERATE = 1.0f;
    private static final float GP_MAX_SPEED  = 10.0f;
    private static final float GP_THRESHOLD  = 0.01f;

    /** The singleton instance of the input controller */
    private static InputController theController = null;

    private Viewport viewport;
    /**
     * Returns the singleton instance of the input controller
     *
     * @return the singleton instance of the input controller
     */
    public static InputController getInstance(Viewport viewport) {
        if (theController == null) {
            theController = new InputController(viewport);
        }
        return theController;
    }

    // Fields to manage buttons
    /** Whether the reset button was pressed. */
    private boolean resetPressed;
    private boolean resetPrevious;
    /** Whether the primary action button was pressed. */
    private boolean primePressed;
    private boolean primePrevious;
    /** Whether the secondary action button was pressed. */
    private boolean secondPressed;
    private boolean secondPrevious;
    /** Whether the teritiary action button was pressed. */
    private boolean tertiaryPressed;
    /** Whether the left mouse button was pressed */
    private boolean leftMouseClicked;
    private boolean leftMousePrev;
    /** Whether the right mouse button was pressed */
    private boolean rightMouseClicked;
    private boolean rightMousePrev;
    /** Whether the middle mouse button was pressed */
    private boolean middleMouseClicked;
    private boolean middleMousePrev;
    /** Whether the quaternary action button was pressed. */
    private boolean quaternaryPressed;
    private boolean quaternaryPrevious;
    /** Whether the 1 key was pressed (commands helper 1). */
    private boolean qPressed;
    private boolean qPrevious;
    /** Whether the 2 key was pressed (commands helper 2). */
    private boolean ePressed;
    private boolean ePrevious;
    /** Whether the 1 key was pressed. */
    private boolean onePressed;
    private boolean onePrevious;
    /** Whether the 2 key was pressed. */
    private boolean twoPressed;
    private boolean twoPrevious;
    /** Whether the 3 key was pressed. */
    private boolean threePressed;
    private boolean threePrevious;
    /** Whether the debug toggle was pressed. */
    private boolean debugPressed;
    private boolean debugPrevious;
    /** Whether the exit button was pressed. */
    private boolean exitPressed;
    private boolean exitPrevious;

    private boolean pausePressed = false;

    /** How much did we move horizontally? */
    private float horizontal;
    /** How much did we move vertically? */
    private float vertical;
    /** The crosshair position (for shooting) */
    private Vector2 crosshair;
    /** The crosshair cache (for using as a return value) */
    private Vector2 crosscache;
    /** For the gamepad crosshair control */
    private float momentum;

    /** An X-Box controller (if it is connected) */
    XBoxController xbox;

    /** Scroll wheel input */
    private float scrollInput = 0;

    /**
     * Custom input processor for scroll wheel detection
     */
    private class ScrollInputProcessor extends InputAdapter {
        @Override
        public boolean scrolled(float amountX, float amountY) {
            scrollInput = amountY;
            return true;
        }
    }

    /**
     * Returns the amount of sideways movement.
     *
     * -1 = left, 1 = right, 0 = still
     *
     * @return the amount of sideways movement.
     */
    public float getHorizontal() {
        return horizontal;
    }

    /**
     * Returns the amount of vertical movement.
     *
     * -1 = down, 1 = up, 0 = still
     *
     * @return the amount of vertical movement.
     */
    public float getVertical() {
        return vertical;
    }

    /**
     * Returns the current position of the crosshairs on the screen.
     *
     * This value does not return the actual reference to the crosshairs
     * position. That way this method can be called multiple times without any
     * fear that the position has been corrupted. However, it does return the
     * same object each time. So if you modify the object, the object will be
     * reset in a subsequent call to this getter.
     *
     * @return the current position of the crosshairs on the screen.
     */
    public Vector2 getCrossHair() {
        return crosscache.set(crosshair);
    }

    /**
     * Returns the scroll wheel input.
     *
     * @return the scroll wheel input (positive = up, negative = down)
     */
    public float getScrollInput() {
        return scrollInput;
    }

    /**
     * Clears the scroll input after reading it.
     */
    public void clearScrollInput() {
        scrollInput = 0;
    }

    /**
     * Returns true if the primary action button was pressed.
     *
     * This is a one-press button. It only returns true at the moment it was
     * pressed, and returns false at any frame afterwards.
     *
     * @return true if the primary action button was pressed.
     */
    public boolean didPrimary() {
        return primePressed && !primePrevious;
    }

    /**
     * Returns true if the secondary action button was pressed.
     *
     * This is a one-press button. It only returns true at the moment it was
     * pressed, and returns false at any frame afterwards.
     *
     * @return true if the secondary action button was pressed.
     */
    public boolean didSecondary() {
        return secondPressed && !secondPrevious;
    }

    /**
     * Returns true if the tertiary action button was pressed.
     *
     * This is a sustained button. It will returns true as long as the player
     * holds it down.
     *
     * @return true if the secondary action button was pressed.
     */
    public boolean didTertiary() {
        return tertiaryPressed;
    }

    /**
     * Returns true if the left mouse button was pressed.
     *
     * This is a one-press button. It only returns true at the moment it was
     * pressed, and returns false at any frame afterwards.
     *
     * @return true if the secondary action button was pressed.
     */
    public boolean didLeftClick() {
        return leftMouseClicked && !leftMousePrev;
    }

    /**
     * Returns true if the right mouse button was pressed.
     *
     * This is a one-press button. It only returns true at the moment it was
     * pressed, and returns false at any frame afterwards.
     *
     * @return true if the secondary action button was pressed.
     */
    public boolean didRightClick() {
        return rightMouseClicked && !rightMousePrev;
    }

    /**
     * Returns true if the middle mouse button was pressed.
     *
     * This is a one-press button. It only returns true at the moment it was
     * pressed, and returns false at any frame afterwards.
     *
     * @return true if the secondary action button was pressed.
     */
    public boolean didMiddleClick() {
        return middleMouseClicked && !middleMousePrev;
    }

    /**
     * Returns true if the quaternary action button was pressed.
     *
     * This is a one-press button. It only returns true at the moment it was
     * pressed, and returns false at any frame afterwards.
     *
     * @return true if the quaternary action button was pressed.
     */
    public boolean didQuarternary() {
        return quaternaryPressed && !quaternaryPrevious;
    }

    /**
     * Returns true if the 1 key was pressed (commands helper 1).
     *
     * This is a one-press button. It only returns true at the moment it was
     * pressed, and returns false at any frame afterwards.
     *
     * @return true if the 1 key was pressed.
     */
    public boolean didQ() {
        return qPressed && !qPrevious;
    }

    /**
     * Returns true if the 2 key was pressed (commands helper 2).
     *
     * This is a one-press button. It only returns true at the moment it was
     * pressed, and returns false at any frame afterwards.
     *
     * @return true if the 2 key was pressed.
     */
    public boolean didE() {
        return ePressed && !ePrevious;
    }

    /**
     * Returns true if the corresponding number key was pressed.
     *
     * This is a one-press button. It only returns true at the moment it was
     * pressed, and returns false at any frame afterwards.
     *
     * @return true if the quaternary action button was pressed.
     */
    public boolean didOne() {
        return onePressed && !onePrevious;
    }

    /**
     * Returns true if the corresponding number key was pressed.
     *
     * This is a one-press button. It only returns true at the moment it was
     * pressed, and returns false at any frame afterwards.
     *
     * @return true if the quaternary action button was pressed.
     */
    public boolean didTwo() {
        return twoPressed && !twoPrevious;
    }

    /**
     * Returns true if the corresponding number key was pressed.
     *
     * This is a one-press button. It only returns true at the moment it was
     * pressed, and returns false at any frame afterwards.
     *
     * @return true if the quaternary action button was pressed.
     */
    public boolean didThree() {
        return threePressed && !threePrevious;
    }

    /**
     * Returns true if the reset button was pressed.
     *
     * @return true if the reset button was pressed.
     */
    public boolean didReset() {
        return resetPressed && !resetPrevious;
    }


    /**
     * Returns true if the player wants to go toggle the debug mode.
     *
     * @return true if the player wants to go toggle the debug mode.
     */
    public boolean didDebug() {
        return debugPressed && !debugPrevious;
    }

    /**
     * Returns true if the exit button was pressed.
     *
     * @return true if the exit button was pressed.
     */
    public boolean didExit() {
        return exitPressed && !exitPrevious;
    }

    /**
     * Returns true if the pause button was pressed.
     *
     * @return true if the pause button was pressed.
     */
    public boolean didPause() { return pausePressed; }

    /**
     * Creates a new input controller
     *
     * The input controller attempts to connect to the X-Box controller at
     * device 0, if it exists. Otherwise, it falls back to the keyboard
     * control.
     */
    public InputController(Viewport vp) {
        // If we have a game-pad for id, then use it.
        Array<XBoxController> controllers = Controllers.get().getXBoxControllers();
        if (controllers.size > 0) {
            xbox = controllers.get( 0 );
        } else {
            xbox = null;
        }
        crosshair = new Vector2();
        crosscache = new Vector2();
        viewport = vp;

        // Set up scroll wheel input processor
        Gdx.input.setInputProcessor(new ScrollInputProcessor());
    }

    /**
     * Syncs the keyboard to the current animation frame.
     *
     * The method provides both the input bounds and the drawing scale. It needs
     * the drawing scale to convert screen coordinates to world coordinates.
     * The bounds are for the crosshair. They cannot go outside of this zone.
     *
     * @param bounds The input bounds for the crosshair.
     * @param scale  The drawing scale
     */
    public void sync(Rectangle bounds, Vector2 scale, OrthographicCamera camera) {
        // Copy state from last animation frame
        // Helps us ignore buttons that are held down
        primePrevious  = primePressed;
        secondPrevious = secondPressed;
        leftMousePrev = leftMouseClicked;
        rightMousePrev = rightMouseClicked;
        middleMousePrev = middleMouseClicked;
        quaternaryPrevious = quaternaryPressed;
        qPrevious = qPressed;
        ePrevious = ePressed;
        onePrevious = onePressed;
        twoPrevious = twoPressed;
        threePrevious = threePressed;
        resetPrevious  = resetPressed;
        debugPrevious  = debugPressed;
        exitPrevious = exitPressed;

        // Check to see if a GamePad is connected
        if (xbox != null && xbox.isConnected()) {
            readGamepad(bounds, scale);
            readKeyboard(bounds, scale, true); // Read as a back-up
        } else {
            readKeyboard(bounds, scale, false);
        }
    }

    /**
     * Reads input from an X-Box controller connected to this computer.
     *
     * The method provides both the input bounds and the drawing scale. It needs
     * the drawing scale to convert screen coordinates to world coordinates. The
     * bounds are for the crosshair. They cannot go outside of this zone.
     *
     * @param bounds The input bounds for the crosshair.
     * @param scale  The drawing scale
     */
    private void readGamepad(Rectangle bounds, Vector2 scale) {
        resetPressed = xbox.getStart();
        exitPressed  = xbox.getBack();
        primePressed = xbox.getA();
        debugPressed  = xbox.getY();

        quaternaryPressed = xbox.getRBumper();
        ePressed = xbox.getLBumper();
        onePressed = xbox.getDPadUp();
        twoPressed = xbox.getDPadRight();
        threePressed = xbox.getDPadDown();

        // Increase animation frame, but only if trying to move
        horizontal = xbox.getLeftX();
        vertical   = xbox.getLeftY();
        secondPressed = xbox.getRightTrigger() > 0.6f;

        // Move the crosshairs with the right stick.
        leftMouseClicked = xbox.getA();
        tertiaryPressed = xbox.getA();
        crosscache.set(xbox.getLeftX(), xbox.getLeftY());
        if (crosscache.len2() > GP_THRESHOLD) {
            momentum += GP_ACCELERATE;
            momentum = Math.min(momentum, GP_MAX_SPEED);
            crosscache.scl(momentum);
            crosscache.scl(1/scale.x,1/scale.y);
            crosshair.add(crosscache);
        } else {
            momentum = 0;
        }
        clampPosition(bounds);
    }

    /**
     * Reads input from the keyboard.
     *
     * This controller reads from the keyboard regardless of whether or not an
     * X-Box controller is connected. However, if a controller is connected,
     * this method gives priority to the X-Box controller.
     *
     * @param secondary true if the keyboard should give priority to a gamepad
     */
    private void readKeyboard(Rectangle bounds, Vector2 scale, boolean secondary) {
        // Give priority to gamepad results
        resetPressed = (secondary && resetPressed) || (Gdx.input.isKeyPressed(Input.Keys.R));
        debugPressed = (secondary && debugPressed) || (Gdx.input.isKeyPressed(Input.Keys.P));
        // debugPressed = false;
        primePressed = (secondary && primePressed) || (Gdx.input.isKeyPressed(Input.Keys.SPACE));
        secondPressed = (secondary && secondPressed) || (Gdx.input.isKeyPressed(Input.Keys.E));
        quaternaryPressed = (secondary && quaternaryPressed) || (Gdx.input.isKeyPressed(Input.Keys.Q));
        // Helper 1 = A key, Helper 2 = D
        qPressed = (secondary && qPressed) || (Gdx.input.isKeyPressed(Input.Keys.A));
        ePressed = (secondary && ePressed) || (Gdx.input.isKeyPressed(Input.Keys.D));
        onePressed = (secondary && onePressed) || (Gdx.input.isKeyPressed(Input.Keys.A));
        twoPressed = (secondary && twoPressed) || (Gdx.input.isKeyPressed(Input.Keys.D));
        threePressed = (secondary && threePressed) || (Gdx.input.isKeyPressed(Input.Keys.NUM_3));
        // exitPressed  = (secondary && exitPressed) || (Gdx.input.isKeyPressed(Input.Keys.ESCAPE));
        exitPressed = false;
        pausePressed = Gdx.input.isKeyPressed(Keys.ESCAPE);

        // Directional controls
//        horizontal = (secondary ? horizontal : 0.0f);
//        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
//            horizontal += 1.0f;
//        }
//        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
//            horizontal -= 1.0f;
//        }

        vertical = (secondary ? vertical : 0.0f);
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            vertical += 1.0f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            vertical -= 1.0f;
        }

        // Mouse results
        leftMouseClicked = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        tertiaryPressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        rightMouseClicked = Gdx.input.isButtonPressed(Input.Buttons.RIGHT);
        middleMouseClicked = Gdx.input.isButtonPressed(Input.Buttons.MIDDLE);

        //BELOW IS CHANGED because mac has logical vs physical pixels, which on windows are the same, but for mac physical is 2x the logical
        //mac cursor was misaligned before


//        float screenToLogicX = 2300f / viewport.getScreenWidth();
//        float screenToLogicY = 1600f / viewport.getScreenHeight();
//        int mouseX = Gdx.input.getX();
//        int mouseY = Gdx.input.getY();
//        // Make sure cursor doesn't go off screen
//
//        // Check and restrict X position
//        if (mouseX < 0) {
//            Gdx.input.setCursorPosition(0, mouseY);
//        } else if (mouseX > viewport.getScreenWidth()) {
//            Gdx.input.setCursorPosition(viewport.getScreenWidth(), mouseY);
//        }
//        // Check and restrict Y position
//        if (mouseY < 0) {
//            Gdx.input.setCursorPosition(mouseX, 0);
//        } else if (mouseY > Gdx.graphics.getHeight()) { // for some reason you had to use this magic number before (350)
//            Gdx.input.setCursorPosition(mouseX, viewport.getScreenHeight());
//        }
//        // System.out.println(mouseY);
//        crosshair.set(Gdx.input.getX() * screenToLogicX, Gdx.input.getY() * screenToLogicY);
//        crosshair.scl(1/scale.x,-1/scale.y);
//        crosshair.y += bounds.height;
//        clampPosition(bounds);

        float screenToLogicX = 2300f / Gdx.graphics.getBackBufferWidth();
        float screenToLogicY = 1600f / Gdx.graphics.getBackBufferHeight();
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.input.getY();

        // cursor clamping — also fix to use backbuffer dimensions
        if (mouseX < 0) {
            Gdx.input.setCursorPosition(0, mouseY);
        } else if (mouseX > Gdx.graphics.getBackBufferWidth()) {
            Gdx.input.setCursorPosition(Gdx.graphics.getBackBufferWidth(), mouseY);
        }
        if (mouseY < 0) {
            Gdx.input.setCursorPosition(mouseX, 0);
        } else if (mouseY > Gdx.graphics.getBackBufferHeight()) {
            Gdx.input.setCursorPosition(mouseX, Gdx.graphics.getBackBufferHeight());
        }

        crosshair.set(mouseX * screenToLogicX, mouseY * screenToLogicY);
        crosshair.scl(1/scale.x, -1/scale.y);
        crosshair.y += bounds.height;
        clampPosition(bounds);

    }

    /**
     * Clamps the cursor position so that it does not go outside the window
     *
     * While this is not usually a problem with mouse control, this is critical
     * for the gamepad controls.
     */
    private void clampPosition(Rectangle bounds) {
        crosshair.x = Math.max(bounds.x, Math.min(bounds.x+bounds.width, crosshair.x));
        crosshair.y = Math.max(bounds.y, Math.min(bounds.y+bounds.height, crosshair.y));
    }
}

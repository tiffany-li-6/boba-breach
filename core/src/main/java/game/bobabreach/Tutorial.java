package game.bobabreach;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextLayout;

public class Tutorial {

    private TextureRegion spaceToContinue;

    public enum AdvanceCondition {
        SPACE,
        SLINGSHOT_MOVED,
        BOBA_IN_CUP,
        BUG_SHOT,
        PICKUP,
        DROPOFF,
        MOVEMENT
    }

    private static class Step {

        final String overlayKey;

        final String popupKey;

        final AdvanceCondition condition;

        final boolean isBig;

        Step(String popupKey, String overlayKey, AdvanceCondition condition, boolean big){
            this.overlayKey = overlayKey;
            this.popupKey = popupKey;
            this.condition = condition;
            this.isBig = big;
        }
    }

    private final Array<Step> steps;
    private int currentStep;

    private final AssetDirectory directory;

    private final float logWidth;
    private final float logHeight;

    //texture regions for all the different tutorial popups
    private TextureRegion recipe;

    private float popupW;
    private float popupH;
    private float popupX;
    private float popupY;
    private float spaceX;
    private float spaceY;
    private float spaceW;
    private float spaceH;
    private float bigPopupW;
    private float bigPopupH;
    private float bigPopupX;
    private float bigPopupY;

    private boolean bobaInCupPending = false;
    private boolean bugShotPending = false;
    private boolean pickupPending = false;
    private boolean dropoffPending = false;

    public Tutorial(AssetDirectory directory, int levelSelected) {
        this.directory = directory;
        JsonValue tutConstants = directory.getEntry("constants", JsonValue.class).get("tutorials");
        spaceToContinue = new TextureRegion(directory.getEntry("spaceToContinue", Texture.class));
        logHeight = tutConstants.getFloat("logical height");
        logWidth = tutConstants.getFloat("logical width");
        popupW = tutConstants.getFloat("popup w");
        popupH = tutConstants.getFloat("popup h");
        popupX = tutConstants.getFloat("popup x");
        popupY = tutConstants.getFloat("popup y");
        bigPopupW = tutConstants.getFloat("big popup w");
        bigPopupH = tutConstants.getFloat("big popup h");
        bigPopupX = tutConstants.getFloat("big popup x");
        bigPopupY = tutConstants.getFloat("big popup y");
        spaceX = tutConstants.getFloat("space x");
        spaceY = tutConstants.getFloat("space y");
        spaceW = tutConstants.getFloat("space w");
        spaceH = tutConstants.getFloat("space h");

        this.steps = new Array<>();
        this.currentStep = 0;

        buildSteps(levelSelected);

        if (steps.size == 0){
            currentStep = 1;
        }

    }

    private void buildSteps(int levelSelected){
        switch(levelSelected) {
            case 1:
                steps.add(new Step("popup1", "overlay1", AdvanceCondition.SPACE, false));
                steps.add(new Step("slingshotPopup", "slingshotOverlay", AdvanceCondition.SLINGSHOT_MOVED, false));
                steps.add(new Step("slingshotAnglePopup", "cupOverlay", AdvanceCondition.BOBA_IN_CUP, false));
                steps.add(new Step("ventPopup", null, AdvanceCondition.BUG_SHOT, false));
                steps.add(new Step("keepGoingPopup", null, AdvanceCondition.SPACE, false));
                break;
            case 2:
                steps.add(new Step("helperPopup", null, AdvanceCondition.SPACE, false));
                steps.add(new Step("CJControl", null, AdvanceCondition.MOVEMENT, false));
                steps.add(new Step("ingredientPickup", null, AdvanceCondition.PICKUP, false));
                steps.add(new Step("ingredientDropoff", null, AdvanceCondition.DROPOFF, false));
                steps.add(new Step("cjKeepGoing", null, AdvanceCondition.SPACE, false));
                break;
            case 3:
                steps.add(new Step("limitedIngredients", "full overlay", AdvanceCondition.SPACE, true));
                steps.add(new Step("stealerPopup", "full overlay", AdvanceCondition.SPACE, true));
                break;
            case 4:
                steps.add(new Step("timerPopup", "timer overlay", AdvanceCondition.SPACE, false));
                steps.add(new Step("multitasking", "timer overlay", AdvanceCondition.SPACE, false));
                break;
            case 5:
                steps.add(new Step("trickShots", "full overlay", AdvanceCondition.SPACE, true));
                break;
            case 6:
                steps.add(new Step("cobwebPopup", "full overlay", AdvanceCondition.SPACE, true));
                break;
            case 7:
                steps.add(new Step("honeyPopup", "full overlay", AdvanceCondition.SPACE, true));
                break;
            case 8:
                steps.add(new Step("blockerPopup1", "full overlay", AdvanceCondition.SPACE, true));
                steps.add(new Step("blockerPopup2", "full overlay", AdvanceCondition.SPACE, true));
                break;
            case 9:
                steps.add(new Step("attackerPopup", "full overlay", AdvanceCondition.SPACE, true));
                break;
            case 10:
                steps.add(new Step("bubblePopup", "full overlay", AdvanceCondition.SPACE, true));
                break;
            case 11:
                steps.add(new Step("pipePopup", "full overlay", AdvanceCondition.SPACE, true));
                break;
            default:
                break;
        }
    }

    public void dismiss() {
        currentStep = steps.size;
    }

    public boolean isActive(){
        return currentStep < steps.size;
    }

    public void notifyBobaInCup(){
        if(currentStep >= 2) {
            bobaInCupPending = true;
        }
    }

    public void notifyBugShot(){
        if(currentStep >= 3) {
            bugShotPending = true;
        }
    }

    public void notifyPickup(){
        if(currentStep>=2) {
            pickupPending = true;
        }
    }

    public void notifyDropoff(){
        if(currentStep>=3){
            dropoffPending = true;
        }
    }

    public void update() {
        if (!isActive()) return;
        Step step = steps.get(currentStep);

        boolean shouldAdvance = false;
        switch(step.condition){
            case SPACE:
                shouldAdvance = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
                break;
            case SLINGSHOT_MOVED:
                shouldAdvance = Gdx.input.isKeyJustPressed(Input.Keys.W) ||
                    Gdx.input.isKeyJustPressed(Input.Keys.S);
                break;
            case BOBA_IN_CUP:
                shouldAdvance = bobaInCupPending;
                bobaInCupPending = false;
                break;
            case BUG_SHOT:
                shouldAdvance = bugShotPending;
                bugShotPending = false;
                break;
            case PICKUP:
                shouldAdvance = pickupPending;
                break;
            case DROPOFF:
                shouldAdvance = dropoffPending;
                break;
            case MOVEMENT:
                shouldAdvance = Gdx.input.isKeyJustPressed(Input.Keys.A);
                break;
        }


        if (shouldAdvance) {
            currentStep++;
        }
    }

    public void draw(SpriteBatch batch, com.badlogic.gdx.graphics.OrthographicCamera cam){
         if(!isActive()){
             return;
         }

         Step step = steps.get(currentStep);

         batch.begin(cam);
         Texture popup = directory.getEntry(step.popupKey, Texture.class);
         if(step.overlayKey != null){
             Texture overlay = directory.getEntry(step.overlayKey, Texture.class);
             batch.draw(overlay, 0, 0, logWidth, logHeight);
         }
         if(step.isBig){
             batch.draw(popup, bigPopupX, bigPopupY, bigPopupW, bigPopupH);
         }else{
             batch.draw(popup, popupX, popupY, popupW, popupH);
         }
         if(step.condition == AdvanceCondition.SPACE && !step.isBig){
             batch.draw(spaceToContinue, spaceX, spaceY, spaceW, spaceH);
         }

         batch.end();
    }



}

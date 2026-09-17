package game.bobabreach;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import edu.cornell.gdiac.assets.AssetDirectory;
import game.bobabreach.GameObjects.BugType;
import java.util.HashMap;
import java.util.Map;

public class AnimationLibrary {

    public static Map<BugType, BugAnimations> bugAnims = new HashMap<>();
    public static SlingshotAnimations slingshotAnims = new SlingshotAnimations();


    public static void init(AssetDirectory assets) {

        //all bugs (helper + enemy)
        TextureAtlas bugsAtlas = new TextureAtlas(Gdx.files.internal("atlases/bugs")); //atlases not stored in assets.json it won't work that way..

        // Orange Helperbug
        BugAnimations helper_orange = new BugAnimations();
        helper_orange.move = new DirectionalAnimation(bugsAtlas, "helper_orange_fly", 0.06f, 5, 5, 24, false); //flying
        //helper_orange.pickup = new DirectionalAnimation(bugsAtlas, "helper_orange_pickup", 0.05f, 5, 5, 24); //pick up ingredient

        // add more for other animation states
        bugAnims.put(BugType.HELPER_ORANGE, helper_orange);

        // Blue Helperbug
        BugAnimations helper_blue = new BugAnimations();
        helper_blue.move = new DirectionalAnimation(bugsAtlas, "helper_blue_fly", 0.12f, 4, 3, 12, false); //flying
        //helper_yellow.pickup = new DirectionalAnimation(bugsAtlas, "helper_yellow_pickup", 0.05f, 5, 5, 24); //pick up ingredient
        bugAnims.put(BugType.HELPER_BLUE, helper_blue);
        //System.out.println("bugAnims size after 2 helpers:" + bugAnims.size());

        // Attacker bug
        BugAnimations attacker = new BugAnimations();
        attacker.move = new DirectionalAnimation(bugsAtlas, "attacker_walk", 0.02f, 6, 5, 30, false); //walking
        attacker.death = new DirectionalAnimation(bugsAtlas, "attacker_death", 0.05f, 5, 5, 23, false); //death
        attacker.spawn = new DirectionalAnimation(bugsAtlas, "attacker_despawn", 0.11f, 4, 3, 9, true); //spawn
        //attacker.despawn = new DirectionalAnimation(bugsAtlas, "attacker_despawn", 0.07f, 4, 4, 13); //despawn

        bugAnims.put(BugType.ATTACKERBUG, attacker);
        //System.out.println("bugAnims size after attacker addition:" + bugAnims.size());


        // Stealer bug
        BugAnimations stealer = new BugAnimations();
        stealer.move = new DirectionalAnimation(bugsAtlas, "stealer_fly", 0.04f, 5, 5, 24, false); //flying
        stealer.pickup = new DirectionalAnimation(bugsAtlas, "stealer_pickup", 0.08f, 6, 5, 28, false); //pickup ingredient
        stealer.death = new DirectionalAnimation(bugsAtlas, "stealer_death", 0.05f, 5, 5, 21, false); //death
        stealer.spawn = new DirectionalAnimation(bugsAtlas, "stealer_despawn", 0.06f, 4, 4, 15, true); //spawn
        //stealer.despawn = new DirectionalAnimation(bugsAtlas, "stealer_despawn", 0.04f, 5, 4, 20); //despawn

        bugAnims.put(BugType.STEALERBUG, stealer);
        //System.out.println("bugAnims size after stealer addition:" + bugAnims.size());

        // Blocker bug
        BugAnimations blocker = new BugAnimations();
        blocker.move = new DirectionalAnimation(bugsAtlas, "blocker_walk", 0.06f, 5, 4, 18, false); //walking
        bugAnims.put(BugType.BLOCKERBUG, blocker);

        //slingshot
        TextureAtlas slingshotAtlas = new TextureAtlas(Gdx.files.internal("atlases/slingshot"));
        slingshotAnims.idle = new AngularAnimation(slingshotAtlas, "slingshot_turning", 6, 6, 36);
        slingshotAnims.shoot = new DirectionalAnimation(slingshotAtlas, "slingshot_shoot", 0.04f, 5, 5, 25, 37, 5);


        /*
        for (TextureAtlas.AtlasRegion r : bugsAtlas.getRegions()) {
            System.out.println(r.name);
        }

         */
    }
}

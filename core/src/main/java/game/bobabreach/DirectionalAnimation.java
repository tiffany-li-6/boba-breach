package game.bobabreach;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import java.util.EnumMap;

public class DirectionalAnimation {


    //bug constructor uses
    private final EnumMap<Direction, Animation<TextureRegion>> animations;

    //for slingshot shooting construSctor
    private TextureRegion[][] strips; // [angleIndex][frameIndex]
    private float frameDuration;
    private int buckets;

    //constructor for bugs (8 dir, one sheet per Direction enum like N, NE, etc.)
    //region format: baseName_E, baseName_NE, etc.
    public DirectionalAnimation(TextureAtlas atlas, String baseName, float frameDuration, int cols, int rows, int frameCount, boolean reversed) {

        this.frameDuration = frameDuration;
        this.buckets = 8;
        this.strips = null;
        this.animations = new EnumMap<>(Direction.class);

        for (Direction dir : Direction.values()) {

            String regionName = baseName + "_" + dir.name();

            TextureRegion sheet = atlas.findRegion(regionName);
            //debugging
            //System.out.println("Looking for: " + regionName + " -> " + sheet);

            if (sheet == null) {
                System.err.println("Missing region: " + regionName);
                continue;
            }
            //System.out.println("Region coords: " + sheet.getRegionX() + ", " + sheet.getRegionY());

            //System.out.println(regionName + " size = " + sheet.getRegionWidth() + " x " + sheet.getRegionHeight());

            // split ONCE
            TextureRegion[][] grid = sheet.split(
                sheet.getRegionWidth() / cols,
                sheet.getRegionHeight() / rows
            );

            Array<TextureRegion> frames = new Array<>(frameCount);

            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {

                    if (frames.size >= frameCount) break;

                    frames.add(grid[row][col]);
                }
            }
            if (reversed) {
                frames.reverse(); // LibGDX Array has a built-in reverse()
            }

            animations.put(
                dir,
                new Animation<>(frameDuration, frames, Animation.PlayMode.LOOP)
            );

            //System.out.println("Loaded " + frames.size + " frames for " + regionName);
        }
    }

    //new constructor for slingshot, with 36 angle buckets, using one sheet per angle, loaded VIA ATLAS INDEX, not angle degree on file
    public DirectionalAnimation(TextureAtlas atlas, String baseName, float frameDuration,
        int cols, int rows, int frameCount,
        int buckets, int angleStep) {
        this.frameDuration = frameDuration;
        this.buckets = buckets;
        this.animations = null;
        this.strips = new TextureRegion[buckets][];

        Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions(baseName);
        //System.out.println("Found " + regions.size + " regions for " + baseName);

        if (regions.size != buckets) {
            System.err.println("Warning: expected " + buckets + " regions, got " + regions.size);
        }

        for (int i = 0; i < regions.size && i < buckets; i++) {
            TextureAtlas.AtlasRegion sheet = regions.get(i);

            TextureRegion[][] grid = sheet.split(
                sheet.getRegionWidth() / cols,
                sheet.getRegionHeight() / rows
            );

            Array<TextureRegion> frames = new Array<>(frameCount);
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    if (frames.size >= frameCount) break;
                    frames.add(grid[row][col]);
                }
            }

            strips[i] = new TextureRegion[frames.size];
            for (int f = 0; f < frames.size; f++) strips[i][f] = frames.get(f);
            //System.out.println("Loaded strip " + i + " (atlas index " + sheet.index + ") with " + frames.size + " frames");
        }
    }

    //used for getting the correct direction of a bug animation
    public Animation<TextureRegion> get(Direction dir) {
        return animations.get(dir);
    }

    public TextureRegion getFrame(Direction dir, float stateTime) {
        Animation<TextureRegion> anim = animations.get(dir);
        if (anim == null) {
            anim = animations.get(Direction.E);
            if (anim == null){
                return null;
            }
        }
        TextureRegion frame = anim.getKeyFrame(stateTime, true);

//        System.out.println("getFrame dir=" + dir + " stateTime=" + stateTime
//            + " frameIndex=" + anim.getKeyFrameIndex(stateTime)
//            + " frame=" + frame.getRegionX() + "," + frame.getRegionY()
//            + " on texture=" + frame.getTexture().toString());

        return frame;
    }


    //used for getting animation of slingshot shooting
    //input is the angle in degrees
    public TextureRegion getFrame(float angle, float stateTime, int startFrame) {
        int index = angleToIndex(angle);
        if (strips[index].length == 0) return null;
        int totalFrames = strips[index].length;

        int frame = ((int)(stateTime / frameDuration) + startFrame) % totalFrames;
        return strips[index][frame];
    }
//slingshot
    public boolean isFinished(float stateTime, float angle, int startFrame) {
        int index = angleToIndex(angle);
        if (strips[index].length == 0) return true;
        int totalFrames = strips[index].length;
        // finishes when played through all frames starting from startFrame

        return stateTime >= frameDuration * strips[index].length;
    }

    private int angleToIndex(float angle) {
        // Normalize to 0-360
        angle = ((angle % 360f) + 360f) % 360f;

        // Clamp left half to nearest edge
        // 2nd quadrant (90 < angle < 180) → clamp to 180 (southernmost)
        // 3rd quadrant (180 <= angle < 270) → clamp to 0 (northernmost)
        if (angle > 90f && angle < 180f) {
            angle = 180f;
        } else if (angle >= 180f && angle < 270f) {
            angle = 0f;
        }

        // Correct for 90-degree rotation:
        // South (270°) → 0, East (0°/360°) → 90, North (90°) → 180
        float mapped;
        if (angle >= 270f) {
            mapped = angle - 270f; // 270→0, 360→90
        } else {
            mapped = angle + 90f;  // 0→90, 90→180
        }

        // mapped is [0, 180], snap to nearest 5-degree strip
        int index = Math.round(mapped / 5f);
        return Math.max(0, Math.min(index, buckets - 1));
    }

    //for bug animation states addition
    public boolean isFinished(float stateTime) {
        Animation<TextureRegion> anim = animations.get(Direction.E);
        System.out.println("isFinished: stateTime=" + stateTime
            + " anim null=" + (anim == null)
            + " finished=" + (anim != null && anim.isAnimationFinished(stateTime)));

        if (anim == null) return true;
        return anim.isAnimationFinished(stateTime);
    }


}

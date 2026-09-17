package game.bobabreach;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class AngularAnimation {

    private final TextureRegion[] frames;

    public AngularAnimation(TextureAtlas atlas, String regionName, int cols, int rows, int frameCount) {



        TextureRegion sheet = atlas.findRegion(regionName);
        //debugging
        //System.out.println("Looking for: " + regionName + " -> " + sheet);

        if (sheet == null) {
            throw new RuntimeException("Missing region: " + regionName);
        }
        /*
        System.out.println("Region coords: "
            + sheet.getRegionX() + ", " + sheet.getRegionY());

        System.out.println(regionName + " size = "
            + sheet.getRegionWidth() + " x "
            + sheet.getRegionHeight());

         */

        // Split sheet into grid
        TextureRegion[][] grid = sheet.split(
            sheet.getRegionWidth() / cols,
            sheet.getRegionHeight() / rows
        );

        Array<TextureRegion> tempFrames = new Array<>(frameCount);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {

                if (tempFrames.size >= frameCount) break;

                tempFrames.add(grid[row][col]);
            }
        }

        frames = new TextureRegion[tempFrames.size];
        for (int i = 0; i < tempFrames.size; i++) {
            frames[i] = tempFrames.get(i);
        }

        //System.out.println("Loaded " + frames.length + " angular frames for " + regionName);
    }
    public TextureRegion getFrame(float angle) {
        // Normalize to 0–360
        angle = angle % 360f;
        if (angle < 0) angle += 360f;

        // Collapse LEFT side
        if (angle > 90f && angle < 270f) {
            // Left half of circle
            return (angle >= 180f) ? frames[0] : frames[frames.length - 1];
        }

        // RIGHT side: map angles to frames
        // Convert so:
        // South (270°) → 0
        // East (0°) → mid
        // North (90°) → max

        float mapped;

        if (angle >= 270f) {
            mapped = angle - 270f; // 270 → 0
        } else {
            mapped = angle + 90f;  // 0 → 90, 90 → 180
        }

        // Now mapped is in [0,180]
        float percent = mapped / 180f;

        int index = (int)(percent * (frames.length - 1));
        index = Math.max(0, Math.min(index, frames.length - 1));

        return frames[index];
    }
    /*
    public TextureRegion getFrame(float angle) {
        float clamped = Math.min(Math.max(angle, 0f), 175f);

        int index = (int)(clamped / 5f);
        index = Math.min(index, frames.length - 1);

        return frames[index];
    }

     */
}

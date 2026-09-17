package game.bobabreach;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

public class SaveData {
    public static final int NUM_LEVELS = 15;
    private static final String SAVE_PATH = "progress.json";

    public static final int VOLUME_DEFAULT = 50;
    public static final int VOLUME_MIN = 0;
    public static final int VOLUME_MAX = 100;

    public static class LevelRecord {
        public int stars = 0;

        public float bestTime = -1f;

        public int bestStunned = -1;

        public int bestKills = -1;

        public float bestAccuracy = -1f;

        public boolean completed = false;
    }

    private int musicVolume = VOLUME_DEFAULT;
    private int sfxVolume = VOLUME_DEFAULT;

    private int unlockedUpTo;

    private LevelRecord[] records;

    public SaveData() {
        records = new LevelRecord[NUM_LEVELS];
        for(int i=0; i< NUM_LEVELS; i++){
            records[i] = new LevelRecord();
        }
        unlockedUpTo = 15;
        load();
    }

    public int getMusicVolume() {
        return musicVolume;
    }

    public int getSfxVolume() {
        return sfxVolume;
    }

    public float getMusicGain() {
        return musicVolume / 100f;
    }

    public float getSfxGain() {
        return sfxVolume / 100f;
    }

    public void setMusicVolume(int v) {
        musicVolume = Math.max(VOLUME_MIN, Math.min(VOLUME_MAX, v));
    }
    public void setSfxVolume(int v) {
        sfxVolume = Math.max(VOLUME_MIN, Math.min(VOLUME_MAX, v));
    }

    /**
     * Returns the index of the highest unlocked level (1-base)
     * @return the index of the highest unlocked level
     */
    public int getUnlockedUpTo(){
        return unlockedUpTo;
    }

    public void setUnlockedUpTo(int level){
        unlockedUpTo = Math.max(1, Math.min(level, NUM_LEVELS));
    }

    public boolean isUnlocked(int levelIndex){
        return levelIndex >= 1 && levelIndex <= unlockedUpTo;
    }

    public int getStars(int levelIndex){
        if(!validIndex(levelIndex)) return 0;

        return records[levelIndex-1].stars;
    }

    public float getBestTime(int levelIndex){
        if(!validIndex(levelIndex)) return -1f;
        return records[levelIndex -1].bestTime;
    }

    public int getBestStunned(int levelIndex){
        if(!validIndex(levelIndex)) return -1;
        return records[levelIndex-1].bestStunned;
    }

    public int getBestKills(int levelIndex){
        if(!validIndex(levelIndex)) return -1;
        return records[levelIndex-1].bestKills;
    }

    public float getBestAccuracy(int levelIndex){
        if(!validIndex(levelIndex)) return -1f;
        return records[levelIndex-1].bestAccuracy;
    }

    public boolean isCompleted(int levelIndex){
        if(!validIndex(levelIndex)) return false;
        return records[levelIndex-1].completed;
    }

    public void updateLevel(int levelIndex, int stars, float time, int numStunned, int bugsKilled, float accuracy){
        if(!validIndex(levelIndex)) return;

        LevelRecord rec = records[levelIndex - 1];
        rec.completed = true;
        if(stars > rec.stars){
            rec.stars = stars;
        }
        if(rec.bestTime < 0 || time < rec.bestTime){
            rec.bestTime = time;
        }
        if(rec.bestStunned < 0 || numStunned < rec.bestStunned){
            rec.bestStunned = numStunned;
        }
        if(rec.bestKills < 0 || bugsKilled > rec.bestKills){
            rec.bestKills = bugsKilled;
        }
        if(rec.bestAccuracy < 0 || accuracy > rec.bestAccuracy){
            rec.bestAccuracy = accuracy;
        }
        if(levelIndex >= unlockedUpTo && levelIndex < NUM_LEVELS){
            unlockedUpTo = levelIndex + 1;
        }

        save();
    }

    public void save() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"unlockedUpTo\": ").append(unlockedUpTo).append(",\n");
            sb.append("  \"musicVolume\": ").append(musicVolume).append(",\n");
            sb.append("  \"sfxVolume\": ").append(sfxVolume).append(",\n");
            sb.append("  \"levels\": [\n");
            for(int i=0; i<NUM_LEVELS; i++){
                LevelRecord r = records[i];
                sb.append("    {");
                sb.append("\"levelIndex\": ").append(i+1).append(", ");
                sb.append("\"stars\": ").append(r.stars).append(", ");
                sb.append("\"bestTime\": ").append(r.bestTime).append(", ");
                sb.append("\"bestStunned\": ").append(r.bestStunned).append(", ");
                sb.append("\"bestKills\": ").append(r.bestKills).append(", ");
                sb.append("\"bestAccuracy\": ").append(r.bestAccuracy).append(", ");
                sb.append("\"completed\": ").append(r.completed);
                sb.append("}");

                if(i<NUM_LEVELS -1){
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("  ]\n");
            sb.append("}\n");

            FileHandle file = Gdx.files.local(SAVE_PATH);
            file.writeString(sb.toString(), false);
        } catch (Exception e) {
            Gdx.app.error("SaveData", "Failed to save progress: " + e.getMessage());
        }
    }

    public void load(){
        try{
            FileHandle file = Gdx.files.local(SAVE_PATH);
            if(!file.exists()){
                Gdx.app.log("SaveData", "No save file found, starting fresh");
                return;
            }

            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(file.readString());

            unlockedUpTo = root.getInt("unlockedUpTo", 6);
            musicVolume = Math.max(VOLUME_MIN, Math.min(VOLUME_MAX, root.getInt("musicVolume", VOLUME_DEFAULT)));
            sfxVolume = Math.max(VOLUME_MIN, Math.min(VOLUME_MAX, root.getInt("sfxVolume", VOLUME_DEFAULT)));
            JsonValue levelsArr = root.get("levels");
            if(levelsArr != null){
                for(JsonValue entry = levelsArr.child; entry != null; entry = entry.next){
                    int idx = entry.getInt("levelIndex", -1);
                    if(idx<1 || idx > NUM_LEVELS) continue;
                    LevelRecord rec = records[idx-1];
                    rec.stars = entry.getInt("stars", 0);
                    rec.bestTime = entry.getFloat("bestTime", -1f);
                    rec.bestStunned = entry.getInt("bestStunned", -1);
                    rec.bestKills = entry.getInt("bestKills", -1);
                    rec.bestAccuracy = entry.getFloat("bestAccuracy", -1f);
                    rec.completed = entry.getBoolean("completed", false);
                }
            }
            Gdx.app.log("SaveData", "Loaded Progress - unlocked up to level " + unlockedUpTo);
        } catch (Exception e) {
            Gdx.app.error("SaveData", "Failed to laod save file, using defaults: " + e.getMessage());
        }
    }

    public void reset() {
        unlockedUpTo = 6;
        for(int i=0; i<NUM_LEVELS; i++){
            records[i] = new LevelRecord();
        }
        musicVolume = VOLUME_DEFAULT;
        sfxVolume = VOLUME_DEFAULT;
        save();
    }

    private boolean validIndex(int levelIndex){
        return levelIndex >= 1 && levelIndex <= NUM_LEVELS;
    }
}

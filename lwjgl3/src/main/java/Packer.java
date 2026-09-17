import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class Packer {
    public static void main(String[] args) {
        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.flattenPaths = true; // preserve folder names
        settings.paddingX = 2;
        settings.paddingY = 2;
        settings.maxHeight = 2048;
        settings.maxWidth = 2048;

        String input = "assets/raw/bugs";             // your raw frames folder
        String output = "assets/temp_atlases";      // output folder for atlas
        String atlasName = "bugs";          // atlas name

        TexturePacker.process(settings, input, output, atlasName);
        System.out.println("Atlas packed!");
    }
}

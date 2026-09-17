package game.bobabreach.GameObjects;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import game.bobabreach.WorldGraph;

public class GraphDebugRenderer {
    private ShapeRenderer shapeRenderer;

    public GraphDebugRenderer() {
        shapeRenderer = new ShapeRenderer();
    }

    public void render(WorldGraph graph, OrthographicCamera camera, float units) {

        shapeRenderer.setProjectionMatrix(camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        float size = graph.getSquareSize() * units;

        for (WorldGraph.IndexNode node : graph.getGraph().nodes) {

            float x = node.getXPos() * units - size/2;
            float y = node.getYPos() * units - size/2;

            if (node.getConnections().size == 0) {
                shapeRenderer.setColor(1, 0, 0, 1); // blocked
            } else {
                shapeRenderer.setColor(0, 1, 0, 1); // open
            }

            shapeRenderer.rect(x, y, size, size);
        }

        shapeRenderer.end();

//        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
//
//        for (WorldGraph.IndexNode node : graph.getGraph().nodes) {
//
//            Vector2 from = node.getPos();
//
//            for (Connection<WorldGraph.IndexNode> c : node.getConnections()) {
//
//                WorldGraph.IndexNode toNode = c.getToNode();
//
//                Vector2 to = toNode.getPos();
//
//                if (c.getCost() > 1000) {
//                    continue;
//                }
//
//                shapeRenderer.setColor(0.3f, 0.3f, 1f, 0.3f);
//
//                shapeRenderer.line(from, to);
//            }
//        }
//
//        shapeRenderer.end();
    }

    public void drawPath(GraphPath<WorldGraph.IndexNode> path) {

        if (path == null || path.getCount() < 2) {
            return;
        }

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        shapeRenderer.setColor(1, 1, 0, 1);

        for (int i = 0; i < path.getCount() - 1; i++) {

            Vector2 a = path.get(i).getPos();
            Vector2 b = path.get(i + 1).getPos();

            shapeRenderer.line(a, b);
        }

        shapeRenderer.end();
    }
}

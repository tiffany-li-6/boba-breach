package game.bobabreach;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultConnection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics2.Obstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.util.RandomGenerator;
import game.bobabreach.GameObjects.Crate;
import game.bobabreach.GameObjects.Cup;

import java.util.Arrays;
import java.util.Random;

/**
 * A model class to hold a graph representation of the world, to be used for pathfinding.
 * Code modelled from https://github.com/libgdx/gdx-ai/blob/master/gdx-ai/tests/com/badlogic/gdx/ai/pfa/indexed/IndexedAStarPathFinderTest.java
 */
public class WorldGraph {

    /** The initializing data (to avoid magic numbers) */
//    private final JsonValue data;

    /**
     * Reference to the world.
     * THIS GRAPH IS IMMUTABLE, SO MAKE SURE IT IS CREATED JUST AFTER WALLS & CRATES ARE ADDED
     */
    private final World world;

    /**
     * Width of the world.
     */
    private final float width;

    /**
     * Height of the world.
     */
    private final float height;

    /**
     * The size of a single grid square.
     */
    private final float squareSize;

    /**
     * Our pathfinding graph.
     */
    private final IndexGraph graph;

    /**
     * The crate to target
     */
    private Crate crate;

    private Cup cup;

    /**
     * Whether or not we're making a special graph where connections to tiles w/ targeted crates
     * have a large cost
     */
    boolean nonStandard;

    /**
     * Cost of current connection being made
     */
    int cost;

    int diagonalCost;

    private Random randGen;


    public WorldGraph(JsonValue data, World world, float width, float height) {
        // this.data = data;
        this.world = world;
        this.width = width;
        this.height = height;
        squareSize = data.getFloat("grid size");
        crate = null;
        cost = 10;
        diagonalCost = 14;
        graph = createGraph();
        randGen = new Random();
    }

    public WorldGraph(JsonValue data, World world, float width, float height, Crate crate, boolean special) {
        // this.data = data;
        this.world = world;
        this.width = width;
        this.height = height;
        squareSize = data.getFloat("grid size");
        this.crate = crate;
        cost = 10;
        diagonalCost = 14;
        graph = createGraph();
        nonStandard = special;
    }

    public WorldGraph(JsonValue data, World world, float width, float height, Cup cup) {
        // this.data = data;
        this.world = world;
        this.width = width;
        this.height = height;
        squareSize = data.getFloat("grid size");
        this.cup = cup;
        cost = 10;
        diagonalCost = 14;
        graph = createGraph();
    }

    /**
     * Return the IndexGraph to be given to the AI controller
     * @return pathfinding graph
     */
    public IndexGraph getGraph() {
        return graph;
    }

    /**
     * Return the size of a square in our graph in Box2D units
     * @return size of square in graph
     */
    public float getSquareSize() {
        return squareSize;
    }

    public static class NodeConnection<IndexNode> implements Connection<IndexNode> {

        IndexNode fromNode;
        IndexNode toNode;
        float cost;

        public NodeConnection(IndexNode from, IndexNode to, float cost) {
            this.fromNode = from;
            this.toNode = to;
            this.cost = cost;
        }

        /**
         * Returns the non-negative cost of this connection
         */
        @Override
        public float getCost() {
            return cost;
        }

        /**
         * Returns the node that this connection came from
         */
        @Override
        public IndexNode getFromNode() {
            return fromNode;
        }

        /**
         * Returns the node that this connection leads to
         */
        @Override
        public IndexNode getToNode() {
            return toNode;
        }
    }

    /**
     * A node in our graph.
     */
    public static class IndexNode {
        final int index;
        private final int x;
        private final int y;
        private final Array<NodeConnection<IndexNode>> connections;
        private final float squareSize;
        private boolean isOpen = true;
        private Vector2 pos;

        public IndexNode (final int index, final int x, final int y, final int capacity, final float squareSize) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.connections = new Array<>(capacity);
            this.squareSize = squareSize;
            pos = new Vector2((x + 0.5f) * squareSize, (y + 0.5f) * squareSize);
        }

        public int getIndex() {
            return index;
        }

        public int getX() { return x;};

        public int getY() { return y;};

        public Array<NodeConnection<IndexNode>> getConnections () {
            return connections;
        }

        public float getXPos() {
            return (x + 0.5f) * squareSize;
        }

        public float getYPos() {
            return (y + 0.5f) * squareSize;
        }

        public Vector2 getPos() {
            return pos;
        }

        public void setClosed() {
            isOpen = false;
        }

        public void setOpen() {
            isOpen = true;
        }

        public boolean isOpen() {
            return isOpen;
        }
    }

    /**
     * Our graph representation.
     */
    public static class IndexGraph implements IndexedGraph<IndexNode> {
        public Array<IndexNode> nodes;

        public IndexGraph (Array<IndexNode> nodes) {
            this.nodes = nodes;
        }

        @Override
        public int getIndex (IndexNode node) {
            return node.getIndex();
        }


        public Array<Connection<IndexNode>> getConnections (IndexNode fromNode) {
            Array<Connection<IndexNode>> ret = new Array<>();
            for(NodeConnection<IndexNode> c : fromNode.getConnections()) {
                ret.add(c);
            }
            return ret;
        }

        @Override
        public int getNodeCount () {
            return nodes.size;
        }
    }

    //subclass b2QueryCallback
    private class MyQueryCallback implements QueryCallback {
        public boolean obstacleFound = false;
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
                Body body1 = fixture.getBody();
                ObstacleSprite bd1 = (ObstacleSprite) body1.getUserData();
                if (bd1 != null &&
                    bd1.getName() != null &&
                    bd1.getName().startsWith("wall"))
                {
                    obstacleFound = true;
                    return false;
                }
                if (bd1 == null || bd1.getName() == null || !bd1.getName().startsWith("wall")) {
                    if (cup == null) {
                        if (crate == null || !crate.getObstacle().getBody().equals(fixture.getBody())) {
                            obstacleFound = true;
                            return false;
                        }
                    }
                    else {
                        if (!cup.getObstacle().getBody().equals(fixture.getBody())) {
                            obstacleFound = true;
                            return false;
                        }
                    }
                }
//                if (crate != null) {
//                    System.out.println("h");
//                    System.out.println(fixture.getBody());
//                }
//                if (crate != null && crate.getObstacle().getBody().equals(fixture.getBody())) {
//                    System.out.println(crate);
//                }
//                if (nonStandard && crate != null && crate.getObstacle().getBody().equals(fixture.getBody())) {
//                    cost = 999;
//                }
//                else {
//                    cost = 1;
//                }
            }
            return true;
        }
    };


    /**
     * Uses an AABB query to see if there's an obstacle at this grid square
     * @param x X index of square
     * @param y Y index of square
     * @return Whether square is OPEN or not
     */
    public boolean isReachableNode(int x, int y) {
//        if (y == 0 || y == (int) Math.ceil(height / squareSize) - 1 ||
//        x == 0 || x == (int) Math.ceil(width / squareSize) - 1) {
//            return true;
//        }
        float xPos = (x + 0.5f) * squareSize;
        float yPos = (y + 0.5f) * squareSize;
        MyQueryCallback callback = new MyQueryCallback();
        float radius = squareSize / 2 * 0.5f;
        world.QueryAABB(callback, xPos - radius, yPos - radius,
            xPos + radius, yPos + radius);
        return !callback.obstacleFound;
    }

    /**
     * Creates the pathfinding graph
     */
    private IndexGraph createGraph() {
        final int numRows = (int) Math.ceil(height / squareSize) + 1;
        final int numCols = (int) Math.ceil(width / squareSize) + 1;

        final IndexNode[][] nodes = new IndexNode[numCols][numRows];
        final Array<IndexNode> indexedNodes = new Array<>(numCols * numRows);

        int index = 0;
        for (int y = 0; y < numRows; y++) {
            for (int x = 0; x < numCols; x++, index++) {
                nodes[x][y] = new IndexNode(index, x, y, 8, squareSize);
                indexedNodes.add(nodes[x][y]);
            }
        }

        // Use AABB queries to detect whether a node is reachable
        for (int y = 0; y < numRows; y++, index++) {
            for (int x = 0; x < numCols; x++, index++) {
                boolean cutCorner = true;
//                System.out.print("X pos: " + x * squareSize + "; ");
//                System.out.print("Y pos: " + y * squareSize + "; ");
//                System.out.println(isReachableNode(x, y));
//
                if (!isReachableNode(x, y)) {
                    nodes[x][y].setClosed();
                    continue;
                }
//                if (x - 1 >= 0 && isReachableNode(x - 1, y)) {
//                    nodes[x][y].getConnections().add(new NodeConnection<>(nodes[x][y], nodes[x - 1][y], cost));
//                }
//
//                if (x + 1 < numCols && isReachableNode(x + 1, y)) {
//                    nodes[x][y].getConnections().add(new NodeConnection<>(nodes[x][y], nodes[x + 1][y], cost));
//                }
//
//                if (y - 1 >= 0 && isReachableNode(x , y - 1)) {
//                    nodes[x][y].getConnections().add(new NodeConnection<>(nodes[x][y], nodes[x][y - 1], cost));
//                }
//
//                if (y + 1 < numRows && isReachableNode(x , y + 1)) {
//                    nodes[x][y].getConnections().add(new NodeConnection<>(nodes[x][y], nodes[x][y + 1], cost));
//                }

                boolean leftOpen  = false;
                boolean rightOpen = false;
                boolean upOpen    = false;
                boolean downOpen  = false;

                if (x - 1 >= 0) {
                    // int cost = isReachableNode(x - 1, y) ? 10 : 99999;
                    if (isReachableNode(x - 1, y)) {
                        nodes[x][y].getConnections().add(new NodeConnection<>(nodes[x][y], nodes[x - 1][y], cost));
                        leftOpen = true;
                        // cutCorner = cost <= 10 && cutCorner;
                    }
                }

                if (x + 1 < numCols) {
                    // int cost = isReachableNode(x + 1, y) ? 10 : 99999;
                    if (isReachableNode(x + 1, y)) {
                        nodes[x][y].getConnections().add(new NodeConnection<>(nodes[x][y], nodes[x + 1][y], cost));
                        rightOpen = true;
                    }
                }

                if (y - 1 >= 0) {
                    // int cost = isReachableNode(x, y - 1) ? 10 : 99999;
                    if (isReachableNode(x, y - 1)) {
                        nodes[x][y].getConnections().add(new NodeConnection<>(nodes[x][y], nodes[x][y - 1], cost));
                        downOpen = true;
                    }
                }

                if (y + 1 < numRows) {
                    // int cost = isReachableNode(x, y + 1) ? 10 : 99999;
                    if (isReachableNode(x, y + 1)) {
                        nodes[x][y].getConnections().add(new NodeConnection<>(nodes[x][y], nodes[x][y + 1], cost));
                        upOpen = true;
                    }
                }

//                if (!cutCorner) {
//                    continue;
//                }

                // Diagonals
                if (x - 1 >= 0 && y - 1 >= 0) {
                    // int cost = isReachableNode(x - 1, y - 1) ? 14 : 99999;
                    if (isReachableNode(x - 1, y - 1) && leftOpen && downOpen) {
                        nodes[x][y].getConnections().add(new NodeConnection<>(nodes[x][y], nodes[x - 1][y - 1], diagonalCost));
                    }
                }

                if (x + 1 < numCols && y + 1 < numRows) {
                    // int cost = isReachableNode(x + 1, y + 1) ? 14 : 99999;
                    if (isReachableNode(x + 1, y + 1) && upOpen && rightOpen) {
                        nodes[x][y].getConnections().add(new NodeConnection<>(nodes[x][y], nodes[x + 1][y + 1], diagonalCost));
                    }
                }

                if (y - 1 >= 0 && x + 1 < numCols) {
                    // int cost = isReachableNode(x + 1, y - 1) ? 14 : 99999;
                    if (isReachableNode(x + 1, y - 1) && downOpen && rightOpen) {
                        nodes[x][y].getConnections().add(new NodeConnection<>(nodes[x][y], nodes[x + 1][y - 1], diagonalCost));
                    }
                }

                if (y + 1 < numRows && x - 1 >= 0) {
                    // int cost = isReachableNode(x - 1, y + 1) ? 14 : 99999;
                    if (isReachableNode(x - 1, y + 1) && leftOpen && upOpen) {
                        nodes[x][y].getConnections().add(new NodeConnection<>(nodes[x][y], nodes[x - 1][y + 1], diagonalCost));
                    }
                }

            }
        }

        return new IndexGraph(indexedNodes);
    }

    /**
     * Returns the x index of the node in the pathfinding graph containing this obstacle
     * @param obs The obstacle to query
     * @return x index of node containing obstacle
     */
    public int getNodeX(Obstacle obs) {
        return (int) (obs.getX() / squareSize);
    }

    /**
     * Returns the y index of the node in the pathfinding graph containing this obstacle
     * @param obs The obstacle to query
     * @return y index of node containing obstacle
     */
    public int getNodeY(Obstacle obs) {
        return (int) (obs.getY() / squareSize);
    }

    /**
     * Returns the node at which an obstacle is located
     * @param obs obstacle to locate node for
     * @return node
     */
    public IndexNode getNodeAtObs(Obstacle obs) {
        int nodeX = (int) (obs.getX() / squareSize);
        int nodeY = (int) (obs.getY() / squareSize);
        return getNodeAt(nodeX, nodeY);
    }

    /**
     * Returns the node at a specific graph index
     * @param x x index
     * @param y y index
     * @return node
     */
    public IndexNode getNodeAt(int x, int y) {
        int numRows = (int) Math.ceil(height / squareSize) + 1;
        int numCols = (int) Math.ceil(width / squareSize) + 1;
        if (x > numCols || y > numRows) {
            return null;
        }
        int indexRows = numCols * y;
        int index = indexRows + x;
        if (index < 0) {
            System.out.println(y);
        }
        if (index >= graph.nodes.size || index < 0) {
            return null;
        }
        return graph.nodes.get(index);
    }

    /**
     * Returns the node at a pair of world coordinates
     * @param x x coord
     * @param y y coord
     * @return node
     */
    public IndexNode getNodeAtPos(float x, float y) {
        int nodeX = (int) (x / squareSize);
        int nodeY = (int) (y / squareSize);
        return getNodeAt(nodeX, nodeY);
    }

    public int getNodeX(int index) {
        int numCols = (int) Math.ceil(width / squareSize) + 1;
        return index % numCols;
    }

    public int getNodeY(int index) {
        int numCols = (int) Math.ceil(width / squareSize) + 1;
        return index / numCols;
    }

    public IndexNode getRandomNode() {
        int attempts = 0;
        do {
            int x = randGen.nextInt(1, (int) Math.ceil(width/squareSize) - 1);
            int y = randGen.nextInt(1, (int) Math.ceil(height/squareSize) - 1);
//            int x = RandomGenerator.getInt(1, (int) Math.ceil(width / squareSize) - 1);
//            int y = RandomGenerator.getInt(1, (int) Math.ceil(height / squareSize) - 1);
            if (isReachableNode(x, y)) {
//                System.out.print("x: " + x + ", ");
//                System.out.print("y: " + y + ", ");
//                System.out.println(getNodeAt(x, y));
                return getNodeAt(x, y);
            }
            attempts++;
        } while (attempts < 10);
        return null;
    }

    public IndexNode findClosestReachableNode(Obstacle obs) {

        int startX = (int)(obs.getX() / squareSize);
        int startY = (int)(obs.getY() / squareSize);

        IndexNode start = getNodeAt(startX, startY);

        if (start != null && start.isOpen()) {
            return start;
        }

        int maxRadius = 5;

        for (int r = 1; r <= maxRadius; r++) {

            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {

                    // only check perimeter
                    if (Math.abs(dx) != r && Math.abs(dy) != r) {
                        continue;
                    }

                    int nx = startX + dx;
                    int ny = startY + dy;

                    IndexNode node = getNodeAt(nx, ny);

                    if (node == null) {
                        continue;
                    }

                    if (node.isOpen()) {
                        return node;
                    }
                }
            }
        }

        return null;
    }

    public IndexNode findClosestReachableNode(float x, float y) {

        int startX = (int)(x / squareSize);
        int startY = (int)(y / squareSize);

        IndexNode start = getNodeAt(startX, startY);

        if (start != null && start.isOpen()) {
            return start;
        }

        int maxRadius = 5;

        for (int r = 1; r <= maxRadius; r++) {

            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {

                    // only check perimeter
                    if (Math.abs(dx) != r && Math.abs(dy) != r) {
                        continue;
                    }

                    int nx = startX + dx;
                    int ny = startY + dy;

                    IndexNode node = getNodeAt(nx, ny);

                    if (node == null) {
                        continue;
                    }

                    if (node.isOpen()) {
                        return node;
                    }
                }
            }
        }

        return null;
    }
}

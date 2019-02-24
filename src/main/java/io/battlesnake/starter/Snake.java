package io.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.get;

import java.util.PriorityQueue;


/**
 * Snake server that deals with requests from the snake engine.
 * Just boiler plate code.  See the readme to get started.
 * It follows the spec here: https://github.com/battlesnakeio/docs/tree/master/apis/snake
 */
public class Snake {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Handler HANDLER = new Handler();
    private static final Logger LOG = LoggerFactory.getLogger(Snake.class);
   
    public static final double inf = Double.POSITIVE_INFINITY;
    public static int[][] globalBoard;
    public static int globalWidth;
    public static int globalHeight;
    /**
     * Main entry point.
     *
     * @param args are ignored.
     */
    public static void main(String[] args) {
        String port = System.getProperty("PORT");
        if (port != null) {
            LOG.info("Found system provided port: {}", port);
        } else {
            LOG.info("Using default port: {}", port);
            port = "8080";
        }
        port(Integer.parseInt(port));
        get("/", (req, res) -> "Battlesnake documentation can be found at " + 
            "<a href=\"https://docs.battlesnake.io\">https://docs.battlesnake.io</a>.");
        post("/start", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/ping", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/move", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/end", HANDLER::process, JSON_MAPPER::writeValueAsString);
    }

    /**
     * Handler class for dealing with the routes set up in the main method.
     */
    public static class Handler {

        /**
         * For the ping request
         */
        private static final Map<String, String> EMPTY = new HashMap<>();

        /**
         * Generic processor that prints out the request and response from the methods.
         *
         * @param req
         * @param res
         * @return
         */
        public Map<String, String> process(Request req, Response res) {
            try {
                JsonNode parsedRequest = JSON_MAPPER.readTree(req.body());
                String uri = req.uri();
                LOG.info("{} called with: {}", uri, req.body());
                Map<String, String> snakeResponse;
                if (uri.equals("/start")) {
                    snakeResponse = start(parsedRequest);
                } else if (uri.equals("/ping")) {
                    snakeResponse = ping();
                } else if (uri.equals("/move")) {
                    snakeResponse = move(parsedRequest);
                } else if (uri.equals("/end")) {
                    snakeResponse = end(parsedRequest);
                } else {
                    throw new IllegalAccessError("Strange call made to the snake: " + uri);
                }
                LOG.info("Responding with: {}", JSON_MAPPER.writeValueAsString(snakeResponse));
                return snakeResponse;
            } catch (Exception e) {
                LOG.warn("Something went wrong!", e);
                return null;
            }
        }

        /**
         * /ping is called by the play application during the tournament or on play.battlesnake.io to make sure your
         * snake is still alive.
         *
         * @param pingRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return an empty response.
         */
        public Map<String, String> ping() {
            return EMPTY;
        }

        /**
         * /start is called by the engine when a game is first run.
         *
         * @param startRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return a response back to the engine containing the snake setup values.
         */
        public Map<String, String> start(JsonNode startRequest) {
            Map<String, String> response = new HashMap<>();
            response.put("color", "#ff00ff");
            return response;
        }

        /**
         * /move is called by the engine for each turn the snake has.
         *
         * @param moveRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return a response back to the engine containing snake movement values.
         */
        public Map<String, String> move(JsonNode moveRequest) {
            Map<String, String> response = new HashMap<>();
            final int SAFE = 0;
            final int SNAKE = 1;
            final int FOOD = 2;
            final int OTHERHEAD = 3;
            final int MYHEAD = 4;

            int height = moveRequest.get("board").get("height").asInt();
            int width = moveRequest.get("board").get("width").asInt();
            int health = moveRequest.get("you").get("health").asInt();

            globalHeight = height;
            globalWidth = width;

            int[][] board = new int[width][height];
            globalBoard = board;

            for(JsonNode snake : moveRequest.get("board").get("snakes"))
            {
                for (JsonNode snakeBody : snake.get("body"))
                {
                    board[snakeBody.get("x").asInt()][snakeBody.get("y").asInt()] = SNAKE;
                }
                int otherHeadX = snake.get("body").elements().next().get("x").asInt();
                int otherHeadY = snake.get("body").elements().next().get("y").asInt();
                board[otherHeadX][otherHeadY] = OTHERHEAD;
            }

            for (JsonNode food : moveRequest.get("board").get("food"))
            {
                board[food.get("x").asInt()][food.get("y").asInt()] = FOOD;
            }

            //find head
            int headX = moveRequest.get("you").get("body").elements().next().get("x").asInt();
            int headY = moveRequest.get("you").get("body").elements().next().get("y").asInt();
            board[headX][headY] = MYHEAD;


            System.out.println();
            for(int x = 0; x < width; x++)
            {
                for(int y = 0; y < height; y++)
                {
                    System.out.print(board[y][x]);
                }
                System.out.println();
            }

            response.put("move", "right");
            return response;
        }

        /**
         * /end is called by the engine when a game is complete.
         *
         * @param endRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return responses back to the engine are ignored.
         */
        public Map<String, String> end(JsonNode endRequest) {
            Map<String, String> response = new HashMap<>();
            return response;
        }

        public static Set<Tuple> reconstructPath(Map<Tuple,Tuple> cameFrom, Tuple current)
        {
            Set<Tuple> totalPath = new Set<Tuple>();
            totalPath.add(current);
            while(cameFrom.contains(current))
            {
                current = cameFrom.get(current);
                totalPath.add(current);
            }
            return totalPath;

        }

        public static void AStar(Tuple start, Tuple goal)
        {

            Set<Tuple> closedSet = new HashSet<Tuple>();

            Set<Tuple> openSet = new HashSet<Tuple>();
            openSet.add(start);

            Map<Tuple,Tuple> cameFrom = new Map<Tuple,Tuple>();
            Map<Tuple,Double> gScore = new Map<Tuple,Double>();

            gScore.put(start,0);

            Map<Tuple,Double> fScore = new Map<Tuple,Double>();

            fscore.put(start,heuristic_cost_estimate(start,goal));

            while(!openSet.isEmpty())
            {
                HashSet< Map.Entry<Tuple,Double> > st = fscore.entrySet(); 
                double lowestScore = inf;
                Tuple lowestTuple;
                for(Map.Entry<Tuple,Double> me:st)
                {
                    if (me.getValue() < lowestScore)
                    {
                        lowestScore = me.getValue();
                        lowestTuple = me.getKey();
                    }
                }
                current = lowestTuple;
                if(current.equals(goal))
                {
                    return reconstructPath(cameFrom, current);
                }

                openSet.remove(current);
                closedSet.add(current);

                for(int i = -1; i < 2; i+=2)
                {
                    for(int j = -1; j < 2; j+=2)
                    {
                        Tuple neighbour = new Tuple(current.x+i,current.x+j);
                        if (closedSet.contains(neighbour))
                        {
                            continue;
                        }
                        tentativegScore = gScore.get(current) + distBetween(current,neighbour);

                        if (!openSet.contains(neighbour))
                        {
                            openSet.add(neighbour);
                        }
                        else if (tentativegScore >= gscore.get(neighbour))
                        {
                            continue;
                        }

                        cameFrom.put(neighbour,current);
                        gScore.put(neighbour,tentativegScore);
                        fScore.put(neighbour,gScore.get(neighbour)+heuristicCostEstimate(neighbour, goal));
                    }
                }
            }
        }
    }

    public static double distBetween(Tuple a, Tuple b)
    {
        return (Math.abs(a.x-b.x) + Math.abs(a.y-b.y));
    }

    public static double heuristicCostEstimate(Tuple a, Tuple b)
    {
        // final int SAFE = 0;
        // final int SNAKE = 1;
        // final int FOOD = 2;
        // final int OTHERHEAD = 3;
        // final int MYHEAD = 4;

    }

    public static class Tuple
    {
        int x;
        int y;

        public Tuple(int x, int y)
        {
            this.x = x;
            this.y = y;
        }

        public boolean equals(Tuple o)
        {
            return (this.x == o.x && this.y == o.y);
        }
    }

}

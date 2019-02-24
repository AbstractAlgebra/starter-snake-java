package io.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.*;
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
        private static final HashMap<String, String> EMPTY = new HashMap<>();

        /**
         * Generic processor that prints out the request and response from the methods.
         *
         * @param req
         * @param res
         * @return
         */
        public HashMap<String, String> process(Request req, Response res) {
            try {
                JsonNode parsedRequest = JSON_MAPPER.readTree(req.body());
                String uri = req.uri();
                LOG.info("{} called with: {}", uri, req.body());
                HashMap<String, String> snakeResponse;
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
         * @param pingRequest a HashMap containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return an empty response.
         */
        public HashMap<String, String> ping() {
            return EMPTY;
        }

        /**
         * /start is called by the engine when a game is first run.
         *
         * @param startRequest a HashMap containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return a response back to the engine containing the snake setup values.
         */
        public HashMap<String, String> start(JsonNode startRequest) {
            HashMap<String, String> response = new HashMap<>();
            response.put("color", "#ff00ff");
            return response;
        }

        /**
         * /move is called by the engine for each turn the snake has.
         *
         * @param moveRequest a HashMap containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return a response back to the engine containing snake movement values.
         */
        public HashMap<String, String> move(JsonNode moveRequest) {
            HashMap<String, String> response = new HashMap<>();
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

            TupleB food = null;
            TupleB meHead = null;
            System.out.println();
            for(int x = 0; x < width; x++)
            {
                for(int y = 0; y < height; y++)
                {
                    System.out.print(board[y][x]);
                    if(board[y][x] == FOOD)
                    {
                        food = new TupleB(x,y);
                    }
                    if(board[y][x] == MYHEAD)
                    {
                        meHead = new TupleB(x,y);
                    }
                }
                System.out.println();
            }
            System.out.println("Pre A*");
            TupleB nextSpot = AStar(meHead,food).iterator().next();
            System.out.println("Next spot is: "+ nextSpot.x + ", " + nextSpot.y);

            String responseString = "";
            if(nextSpot.x>meHead.x)
            {
                responseString= "right";
            }
            if(nextSpot.x<meHead.x)
            {
                responseString= "left";
            }
            if(nextSpot.y>meHead.y)
            {
                responseString= "down";
            }            
            if(nextSpot.y<meHead.y)
            {
                responseString= "up";
            }
            response.put("move", responseString);
            return response;
        }

        /**
         * /end is called by the engine when a game is complete.
         *
         * @param endRequest a HashMap containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return responses back to the engine are ignored.
         */
        public HashMap<String, String> end(JsonNode endRequest) {
            HashMap<String, String> response = new HashMap<>();
            return response;
        }

        public static LinkedHashSet<TupleB> reconstructPath(Map<TupleB,TupleB> cameFrom, TupleB current)
        {
            LinkedHashSet<TupleB> totalPath = new LinkedHashSet<TupleB>();
            totalPath.add(current);
            while(cameFrom.containsKey(current))
            {
                current = cameFrom.get(current);
                totalPath.add(current);
            }
            System.out.println("Reconstructing path @ " + current.x + ", " + current.y);
            return totalPath;

        }

        public static LinkedHashSet<TupleB> AStar(TupleB start, TupleB goal)
        {
            System.out.println("A*1");
            Set<TupleB> closedSet = new LinkedHashSet<TupleB>();

            Set<TupleB> openSet = new LinkedHashSet<TupleB>();
            openSet.add(start);

            HashMap<TupleB,TupleB> cameFrom = new HashMap<TupleB,TupleB>();
            HashMap<TupleB,Double> gScore = new HashMap<TupleB,Double>();

            gScore.put(start,(double)0);

            HashMap<TupleB,Double> fScore = new HashMap<TupleB,Double>();

            fScore.put(start,heuristicCostEstimate(start,goal));
            System.out.println("A*2");
            while(!openSet.isEmpty())
            {
                Set< HashMap.Entry<TupleB,Double> > st = fScore.entrySet(); 
                double lowestScore = inf;
                TupleB lowestTupleB = null;
                for(Map.Entry<TupleB,Double> me:st)
                {
                    if (me.getValue() < lowestScore)
                    {
                        lowestScore = me.getValue();
                        lowestTupleB = me.getKey();
                        System.out.println("Score is: " + lowestScore);
                        System.out.println("Tuple is: (" + lowestTupleB.x + ", " + lowestTupleB.y + ")");
                    }
                }
                TupleB current = lowestTupleB;
                if(current.equals(goal))
                {
                    return reconstructPath(cameFrom, current);
                }
                System.out.println("OS Length: " + openSet.size());
                System.out.println("CS Length: " + closedSet.size());
                openSet.remove(current);
                closedSet.add(current);
            System.out.println("A*3");
                for(int i = -1; i < 2; i+=2)
                {
                    for(int j = -1; j < 2; j+=2)
                    {
                        TupleB neighbour = new TupleB(current.x+i,current.y+j);
                        if (closedSet.contains(neighbour))
                        {
                            continue;
                        }
                        double tentativegScore = gScore.getOrDefault(current,inf) + distBetween(current,neighbour);
            System.out.println("A*4");
                        if (!openSet.contains(neighbour))
                        {
                            System.out.println("OS Contains: " + neighbour);
                            openSet.add(neighbour);
                        }
                        else if (tentativegScore >= gScore.getOrDefault(neighbour,inf))
                        {
                            continue;
                        }

                        cameFrom.put(neighbour,current);
                        gScore.put(neighbour,tentativegScore);
                        fScore.put(neighbour,gScore.getOrDefault(neighbour,inf)+heuristicCostEstimate(neighbour, goal));
                    }
                }
            }
            return new LinkedHashSet<TupleB>();
        }

    }

    public static double distBetween(TupleB a, TupleB b)
    {
        return (Math.abs(a.x-b.x) + Math.abs(a.y-b.y));
    }

    public static double heuristicCostEstimate(TupleB a, TupleB b)
    {
        final int SAFE = 0;
        final int SNAKE = 1;
        final int FOOD = 2;
        final int OTHERHEAD = 3;
        final int MYHEAD = 4;
        if (globalBoard[b.x][b.y] == SNAKE || b.x < 0 || b.x > globalWidth-1 || b.y < 0 || b.y > globalHeight-1)
        {
            return 100000;
        }
        else
        {
            return 1;
        }

    }


}
class TupleB
{
    public int x;
    public int y;

    public TupleB(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public boolean equals(TupleB o)
    {
        System.out.println("in equals");
        System.println(o);
        System.println(x == o.x && y == o.y);
        return (x == o.x && y == o.y);
    }
    public String toString()
    {
        return "(" + x + ", " + y + ")";
    }
}

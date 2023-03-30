import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import java.nio.file.Files;
import java.nio.file.Paths;


enum Type {
    CONSOLE,
    FILE
}


/**
 * Main class which processes input and output, and generates map or either performs static analysis
 */
public class Main {
    /**
     * @param type - file/console
     */
    static void processInvalidData(Type type) {
        if (type == Type.CONSOLE) {
            System.out.println("Invalid data, try again");
        } else if (type == Type.FILE) {
            System.out.println("Invalid data");
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        System.out.println("""
                Enter:
                1) To enter the perception in console with auto generated map
                2) To read the map and perception from file input.txt"""
        );

        boolean analysisWasDone = false;

        try {
            Scanner sc = new Scanner(System.in);

            int n = sc.nextInt();
            Map map = new Map();
            var analyser = new Analysis();

            switch (n) {
                // Choosing if whether we process input from console or from file
                case 1 -> {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(System.in));
                    boolean isCorrectInput = false;

                    while (!isCorrectInput) {
                        System.out.println("Enter the perception of scenario:");
                        String line = reader.readLine();

                        if (line.matches("^-?[1-2]$")) {
                            isCorrectInput = true;
                            int perception = Integer.parseInt(line);
                            map.setScenario(perception);
                        } else {
                            processInvalidData(Type.CONSOLE);
                        }
                    }
                    // Randomly generating map with given perception
                    map.generate();
                }
                case 2 -> {
                    List<String> lines = Files.readAllLines(Paths.get("input.txt"));
                    String enteredMap = lines.get(0);

                    if (lines.get(1).matches("^-?[1-2]$")) {
                        int perception = Integer.parseInt(lines.get(1));
                        map.setScenario(perception);
                    } else {
                        processInvalidData(Type.FILE);
                    }

                    enteredMap = enteredMap.replaceAll(" ", "");

                    // Generating map from input file with given perception
                    if (!map.generate(enteredMap)) {
                        processInvalidData(Type.FILE);
                    }
                }
                // Hidden case for performing statistical analysis
                // dec: 57005
                case 0xDEAD -> {
                    Locale.setDefault(Locale.US);
                    final int mapsToGenerate = 1000;

                    Solver.AStar shortestPathByAStar;
                    Solver.Backtracking shortestPathByBacktracking;

                    for (int i = 0; i < mapsToGenerate; i++) {
                        Map currentMap = new Map();
                        currentMap.generate();

                        var shortestPath = new Solver(currentMap);

                        shortestPathByAStar = shortestPath.new AStar();
                        shortestPathByBacktracking = shortestPath.new Backtracking();

                        currentMap.setScenario(1);
                        analyser.performAnalysis(shortestPathByAStar, 1);
                        analyser.performAnalysis(shortestPathByBacktracking, 1);

                        currentMap.setScenario(2);
                        analyser.performAnalysis(shortestPathByBacktracking, 2);
                        analyser.performAnalysis(shortestPathByAStar, 2);
                    }

                    for (int i = 1; i <= 2; i++) {
                        System.out.printf("A* with scenario %d:\n", i);
                        analyser.showResults("AStar", i);
                        System.out.printf("Backtracking with scenario %d:\n", i);
                        analyser.showResults("Backtracking", i);
                    }

                    analysisWasDone = true;
                }

                default -> processInvalidData(Type.CONSOLE);
            }

            if (analysisWasDone) System.exit(0);

            PrintWriter writerAStar = new PrintWriter("outputAStar.txt", StandardCharsets.UTF_8);
            PrintWriter writerBacktracking = new PrintWriter("outputBacktracking.txt", StandardCharsets.UTF_8);

            Solver shortestPath = new Solver(map);

            Solver.AStar shortestPathByAStar = shortestPath.new AStar();
            analyser.analyseSingleMap(shortestPathByAStar, writerAStar, map);

            Solver.Backtracking shortestPathByBactracking = shortestPath.new Backtracking();
            analyser.analyseSingleMap(shortestPathByBactracking, writerBacktracking, map);

        } catch (IOException e) {
            processInvalidData(Type.CONSOLE);
        }
    }
}


/**
 * Class to analyse algorithms on randomly generated maps
 */
class Analysis {
    public HashMap<Integer, HashMap<String, List<Double>>> executionMap;

    public HashMap<Integer, HashMap<String, Integer>> winsMap;
    public HashMap<Integer, HashMap<String, Integer>> losesMap;

    public Analysis() {
        executionMap = new HashMap<>();
        winsMap = new HashMap<>();
        losesMap = new HashMap<>();
    }

    /**
     * Method to analyse and immediately product output of single map that was either generated or manually typed
     * @param algorithm - Algorithm to analyse
     * @param writer - File to write output to
     * @param map - Given map
     */
    public void analyseSingleMap(Algorithm algorithm, PrintWriter writer, Map map) {
        long startTime = System.nanoTime();
        List<Point<Integer, Integer>> path = algorithm.findShortestPath();
        long stopTime = System.nanoTime();

        if (path == null) {
            writer.println("Loss");
        } else {
            double elapsedTime = ((stopTime - startTime) * Math.pow(10, -6));
            writer.println("Win");

            // Best path size
            writer.println(path.size() - 1);

            for (Point<Integer, Integer> point : path) {
                writer.printf("[%d,%d] ", point.getX(), point.getY());
            }

            writer.println();
            map.print(path, writer);
            // Execution time
            writer.println(elapsedTime + " ms");
            // Clear path from global map
            map.clearPath(path);
        }

        writer.close();
    }

    /**
     * Method to analyse an algorithm and store it in common HashMap to produce stats later
     * @param algorithm - Algorithm to analyse
     * @param scenario - Perception scenario
     */
    public void performAnalysis(Algorithm algorithm, int scenario) {
        List<Double> elapsed;
        String algorithmName = algorithm.getClass().getSimpleName();

        long startTime = System.nanoTime();
        boolean win = algorithm.findShortestPath() != null;
        long stopTime = System.nanoTime();

        double elapsedTime = ((stopTime - startTime) * Math.pow(10, -6));

        if (executionMap.get(scenario) == null) {
            executionMap.put(scenario, new HashMap<>() {{
                put(algorithmName, new ArrayList<>(List.of(elapsedTime)));
                }}
            );
            return;
        } else {
            if (executionMap.get(scenario).get(algorithmName) != null) {
                elapsed = executionMap.get(scenario).get(algorithmName);
                elapsed.add(elapsedTime);
            } else {
                elapsed = new ArrayList<>(List.of(elapsedTime));
            }
        }

        if (win) {
            if (winsMap.get(scenario) == null) {
                winsMap.put(scenario, new HashMap<>() {{
                    put(algorithmName, 1);
                }}
                );
            } else {
                int wins = winsMap.get(scenario).getOrDefault(algorithmName, 0);
                winsMap.get(scenario).put(algorithmName, ++wins);
            }
        } else {
            if (losesMap.get(scenario) != null) {
                int loses = losesMap.get(scenario).getOrDefault(algorithmName, 0);
                losesMap.get(scenario).put(algorithmName, ++loses);
            } else {
                losesMap.put(scenario, new HashMap<>() {{
                    put(algorithmName, 1);
                }
                });
            }
        }

        executionMap.get(scenario).put(algorithmName, elapsed);
    }

    public void showResults(String algorithmName, int scenario) {
        scenario = algorithmName.equals("Backtracking") ? 1 : scenario;
        List<Double> executionTimes = executionMap.get(scenario).get(algorithmName);
        Integer wins = winsMap.get(scenario).get(algorithmName);
        Integer loses = losesMap.get(scenario).get(algorithmName);

        System.out.printf("Mean execution time: %f ms\n", Analysis.mean(executionTimes));
        System.out.printf("Mode execution time: %s ms\n", Analysis.mode(executionTimes));
        System.out.printf("Median execution time: %f ms\n", Analysis.median(executionTimes));
        System.out.printf("Standard deviation execution time: %f ms\n", Analysis.deviation(executionTimes));
        System.out.printf("Wins: %d\n", wins);
        System.out.printf("Loses: %d\n", loses);
        System.out.printf("Wins percentage: %.2f\n", (wins / (double) (wins + loses)) * 100);
        System.out.printf("Loses percentage: %.2f\n", (loses / (double) (wins + loses)) * 100);
        System.out.println();
    }

    public static double mean(List<Double> array) {
        double sum = 0;
        for (double value : array) {
            sum += value;
        }
        return sum / array.size();
    }

    public static double median(List<Double> array) {
        int mid = array.size() / 2;
        Collections.sort(array);

        if (array.size() % 2 == 1) {
            return array.get(mid);
        } else {
            return (array.get(mid - 1) + array.get(mid)) / 2.0;
        }
    }

    public static String mode(List<Double> array) {
        HashMap<String, Integer> map = new HashMap<>();
        String maxValue = null;
        double maxCount = 1;

        for (Double value : array) {
            String a1 = String.format("%.2f", value);
            if (map.get(a1) != null) {
                int count = map.get(a1);
                map.put(a1, ++count);

                if (count > maxCount) {
                    maxCount = count;
                    maxValue = a1;
                }
            } else {
                map.put(a1, 1);
            }
        }

        return maxValue;
    }

    public static double deviation(List<Double> array) {
        final double mean = mean(array);
        double sum = 0;

        for (int index = 0; index != array.size(); ++index) {
            sum += Math.pow(Math.abs(mean - array.get(index)), 2);
        }

        return Math.sqrt(sum / (array.size() - 1));
    }
}

/**
 * A point class to conveniently print path after finding the path
 * @param x - x coordinate
 * @param y - x coordinate
 * @param <X> - Integer
 * @param <Y> - Integer
 */
record Point<X, Y>(X x, Y y) {

    public X getX() {
        return this.x;
    }

    public Y getY() {
        return this.y;
    }
}


/**
 * An interface for both pathfinding algorithms
 */
interface Algorithm {
    List<Point<Integer, Integer>> findShortestPath();
}


/**
 * Class for all objects on map
 */
class Agent {
    public boolean isDangerous;

    private final int x;
    private final int y;

    // The perception of each object - danger zone in case of dangerous enemy,
    // and perception zone in case of Jack Sparrow
    protected ArrayList<Point<Integer, Integer>> perception;

    // Fields below for the cells that will be occupied by agents' perception zone
    protected final Point<Integer, Integer> _default = new Point<>(0, 0);

    protected final Point<Integer, Integer> left = new Point<>(-1, 0);
    protected final Point<Integer, Integer> right = new Point<>(1, 0);

    protected final Point<Integer, Integer> top = new Point<>(0, -1);
    protected final Point<Integer, Integer> topLeft = new Point<>(-1, -1);
    protected final Point<Integer, Integer> topRight = new Point<>(1, -1);

    protected final Point<Integer, Integer> bottom = new Point<>(0, 1);
    protected final Point<Integer, Integer> bottomLeft = new Point<>(-1, 1);
    protected final Point<Integer, Integer> bottomRight = new Point<>(1, 1);

    /**
     * @param x - X coordinate
     * @param y - Y coordinate
     * @param dangerous - Field for dangerous objects - Kraken, Rock and Davy Jones
     */
    public Agent(int x, int y, boolean dangerous) {
        this.x = x;
        this.y = y;
        // By default, the perception zone of an agent is the agent himself
        this.perception = new ArrayList<>(List.of(_default));
        this.isDangerous = dangerous;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }


    /**
     * @param agent - Agent object
     * @return - True if agents position on map is the same
     */
    public boolean equals(Agent agent) {
        return this.x == agent.x && this.y == agent.y;
    }


    /**
     * @return Alias of each agent object
     */
    public char getAlias() {
        return '-';
    }
}


/**
 * Class for Davy Jones enemy
 */
class DavyJones extends Agent {

    public DavyJones(int x, int y, boolean dangerous) {
        super(x, y, dangerous);
        this.perception = new ArrayList<>(Arrays.asList(this.topLeft, this.top, this.topRight,
                this.left, this.right,
                this.bottomLeft, this.bottom, this.bottomRight)
        );
    }

    @Override
    public char getAlias() {
        return 'D';
    }
}

/**
 * Class for The Kraken enemy
 */
class Kraken extends Agent {

    public Kraken(int x, int y, boolean dangerous) {
        super(x, y, dangerous);
        this.perception = new ArrayList<>(Arrays.asList(this.top, this.left,
                this.right, this.bottom)
        );
    }

    @Override
    public char getAlias() {
        return 'K';
    }
}

/**
 * Class for The Rock enemy
 */
class Rock extends Agent {

    public Rock(int x, int y, boolean dangerous) {
        super(x, y, dangerous);
    }

    @Override
    public char getAlias() {
        return 'R';
    }
}


/**
 * Class for Dead Man's Chest - our destination point
 */
class Chest extends Agent {

    public Chest(int x, int y, boolean dangerous) {
        super(x, y, dangerous);
    }

    @Override
    public char getAlias() {
        return 'C';
    }
}

/**
 * Class for Tortuga - island with rum casks
 */
class Tortuga extends Agent {

    public Tortuga(int x, int y, boolean dangerous) {
        super(x, y, dangerous);
    }

    @Override
    public char getAlias() {
        return 'T';
    }
}

/**
 * Class for the Actor - Captain Jack Sparrow
 */
class JackSparrow extends Agent {

    public JackSparrow(int x, int y, boolean dangerous, int scenario) {
        super(x, y, dangerous);
        // Depending on the scenario, perception of the Jack Sparrow will change
        switch (scenario) {
            case 1 -> this.perception = new ArrayList<>(Arrays.asList(this.topLeft, this.top, this.topRight,
                    this.left, this.right,
                    this.bottomLeft, this.bottom, this.bottomRight)
            );

            case 2 -> {

                final Point<Integer, Integer> twiceUp = new Point<>(0, -2);
                final Point<Integer, Integer> twiceDown = new Point<>(0, 2);
                final Point<Integer, Integer> twiceLeft = new Point<>(-2, 0);
                final Point<Integer, Integer> twiceRight = new Point<>(2, 0);

                this.perception = new ArrayList<>(Arrays.asList(twiceUp,
                        this.topLeft, this.top, this.topRight,
                        twiceLeft, this.left, this.right, twiceRight,
                        this.bottomLeft, this.bottom, this.bottomRight,
                        twiceDown)
                );
            }
        }
    }

    @Override
    public char getAlias() {
        return 'J';
    }
}


/**
 * Class for Sea Map, on which all objects will be stored
 */
class Map {
    // Current perception of scenario
    int scenario = 1;

    // Agents that will be placed on map
    public Tortuga tortuga;
    public Chest chest;
    public Rock rock;
    public Kraken kraken;
    public DavyJones davyJones;

    public JackSparrow jack;

    List<Agent> agents;

    // Cells of our map
    public MapCell[][] cells;
    // 2D ascii representation of the map
    public char[][] asciiMap;

    /**
     *  Method to change the scenario of our map
     */
    public void setScenario(int scenario) {
        this.scenario = scenario;
    }


    /**
     * @param cell - Map cell
     * @return Set of neighbour cells on the map for the given cell
     */
    public HashSet<MapCell> getNeighbourCells(MapCell cell) {
        HashSet<MapCell> neighbourCells = new HashSet<>();
        switch(this.scenario) {
            case 1 -> {
                for (Point<Integer, Integer> perceptionPoint : this.jack.perception) {
                    int newX = perceptionPoint.getX() + cell.x;
                    int newY = perceptionPoint.getY() + cell.y;

                    if (newX >= 0 && newX <= 8 && newY >= 0 && newY <= 8) neighbourCells.add(cells[newX][newY]);
                }
            }
            case 2 -> {
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        if (!(i == 0 && j == 0)) {
                            int newX = cell.x + i;
                            int newY = cell.y + j;

                            if (newX >= 0 && newX <= 8 && newY >= 0 && newY <= 8) neighbourCells.add(cells[newX][newY]);
                        }
                    }
                }
            }
        }

        return neighbourCells;
    }

    /**
     * Method to fill map cells after generating the map
     */
    public void fillCells(boolean refill) {
        // Refill flag is needed to restore original cells' value of the map
        // after performing any of the algorithms, so they will not affect each other
        if (!refill) {
            cells = new MapCell[9][9];
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    cells[i][j] = new MapCell(i, j, true);
                }
            }
        } else {
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    cells[i][j].dangerLevel = 0;
                    cells[i][j].walkable = true;
                }
            }
        }


        HashSet<Point<Integer, Integer>> perceptionPoints = new HashSet<>();
        for (Agent agent : agents) {
            int x = agent.getX(), y = agent.getY();
            for (var perception : agent.perception) {
                if (agent instanceof JackSparrow) continue;

                Point<Integer, Integer> perceptionPoint = getPerceptionBounds(x, perception.getX(), y, perception.getY());
                if (agent.isDangerous) perceptionPoints.add(perceptionPoint);
            }

            for (var point : perceptionPoints) {
                if (agent.isDangerous) {
                    // If agent is dangerous, increment its danger level and make the cell unpassable
                    if (!(agent.getX() == point.getX() && agent.getY() == point.getY())) {
                        cells[point.getX()][point.getY()].changeWalkability(false);
                        cells[point.getX()][point.getY()].dangerLevel++;
                    }
                }
            }

            // Also, add agent to the common agents list for further purposes
            cells[x][y].agents.add(agent.getAlias());
            if (agent.isDangerous || agent instanceof Rock) {
                cells[x][y].changeWalkability(false);
                cells[x][y].dangerLevel++;
            }
            perceptionPoints.clear();
        }
    }

    /**
     * Method to randomly generate the map
     */
    public void generate() {
        Random random = new Random();
        do {
            this.jack = new JackSparrow(0, 0, false, this.scenario);
            this.davyJones = new DavyJones(random.nextInt(9), random.nextInt(9), true);
            this.kraken = new Kraken(random.nextInt(9), random.nextInt(9), true);
            this.rock = new Rock(random.nextInt(9), random.nextInt(9), false);
            this.chest = new Chest(random.nextInt(9), random.nextInt(9), false);
            this.tortuga = new Tortuga(random.nextInt(9), random.nextInt(9), false);
            this.agents = Arrays.asList(jack, davyJones, kraken, rock, chest, tortuga);
        } while (!this.isCorrect()); // we stop randomizing map only after when current generation is correct

        this.fillCells(false);
        this.makeAsciiMap();
    }

    /**
     * @param map - String representation of map
     * @return true if map was successfully generated
     */
    public boolean generate(String map) {
        // Since the positions of agents on input map representation are always the same,
        // we can create objects by roughly getting the indices of input string
        this.jack = new JackSparrow(Integer.parseInt(String.valueOf(map.charAt(1))), Integer.parseInt(String.valueOf(map.charAt(3))), false, this.scenario);
        this.davyJones = new DavyJones(Integer.parseInt(String.valueOf(map.charAt(6))), Integer.parseInt(String.valueOf(map.charAt(8))), true);
        this.kraken = new Kraken(Integer.parseInt(String.valueOf(map.charAt(11))), Integer.parseInt(String.valueOf(map.charAt(13))), true);
        this.rock = new Rock(Integer.parseInt(String.valueOf(map.charAt(16))), Integer.parseInt(String.valueOf(map.charAt(18))), false);
        this.chest = new Chest(Integer.parseInt(String.valueOf(map.charAt(21))), Integer.parseInt(String.valueOf(map.charAt(23))), false);
        this.tortuga = new Tortuga(Integer.parseInt(String.valueOf(map.charAt(26))), Integer.parseInt(String.valueOf(map.charAt(28))), false);

        this.agents = Arrays.asList(jack, davyJones, kraken, rock, chest, tortuga);

        if (this.isCorrect()) {
            // If the map from input file is correct, proceed
            this.fillCells(false);
            this.makeAsciiMap();
            return true;
        } else {
            return false;
        }
    }


    /**
     * @return true if objects are correctly placed on map
     */
    private boolean isCorrect() {
        boolean isCorrectMap = jack.getX() == 0 && jack.getY() == 0;

        for (Agent firstAgent : agents) {
            for (Agent secondAgent : agents) {
                // Skip agents when they repeat in 2nd loop
                if (firstAgent == secondAgent) continue;

                // Agents can not spawn in each other,
                // unless it is [Rock, Kraken] or [Tortuga, Jack]
                if (firstAgent.equals(secondAgent)) {
                    if ((!(firstAgent instanceof Rock && secondAgent instanceof Kraken)) &&
                            !(firstAgent instanceof Tortuga && secondAgent instanceof JackSparrow) &&
                            !(firstAgent instanceof JackSparrow && secondAgent instanceof Tortuga) &&
                            !(firstAgent instanceof Kraken && secondAgent instanceof Rock)) {
                        return false;
                    }
                }

                // Flag to check if we should compare danger zone of
                // one enemy to the spawn coordinates of another enemy
                boolean valid = (firstAgent.isDangerous && secondAgent instanceof Tortuga) ||
                        (firstAgent instanceof Tortuga && secondAgent.isDangerous) ||
                        (firstAgent.isDangerous && secondAgent instanceof Chest) ||
                        (firstAgent instanceof Chest && secondAgent.isDangerous);

                if (valid) {
                    for (var perception : firstAgent.perception) {
                        if ((perception.getX() + firstAgent.getX() == secondAgent.getX()) &&
                                (perception.getY() + firstAgent.getY() == secondAgent.getY())) {
                            return false;
                        }
                    }
                }
            }
        }

        return isCorrectMap;
    }

    /**
     * Clears path that was applied to the current map
     * @param path - Solution path
     */
    public void clearPath(List<Point<Integer, Integer>> path) {
        for (Point<Integer, Integer> point : path) {
            this.asciiMap[point.getX()][point.getY()] = '-';
        }
    }

    /**
     * @param path - points path that will be highlighted on the map
     * @param writer - output file to which the map will be printed
     */
    public void print(List<Point<Integer, Integer>> path, PrintWriter writer) {
        for (Point<Integer, Integer> point : path) {
            this.asciiMap[point.getX()][point.getY()] = '*';
        }

        _print(writer);
    }

    /**
     * @param writer - print ascii representation of map to the given file
     */
    private void _print(PrintWriter writer) {
        writer.println(" —————————————————————");
        writer.println("|   0 1 2 3 4 5 6 7 8 |");
        for (int i = 0; i < 9; i++) {
            writer.printf("| %d ", i);
            for (int j = 0; j < 9; j++) {
                writer.printf("%c ", this.asciiMap[i][j]);
            }
            writer.println("|");
        }
        writer.println(" —————————————————————");
    }


    /**
     * Method to generate ascii map after creating it
     */
    public void makeAsciiMap() {
        this.asciiMap = new char[][]{
                "---------".toCharArray(),
                "---------".toCharArray(),
                "---------".toCharArray(),
                "---------".toCharArray(),
                "---------".toCharArray(),
                "---------".toCharArray(),
                "---------".toCharArray(),
                "---------".toCharArray(),
                "---------".toCharArray(),
        };

        for (Agent agent : agents) {
            int x = agent.getX(), y = agent.getY();
            if (!(agent instanceof JackSparrow)) {
                for (var perception : agent.perception) {
                    Point<Integer, Integer> perceptionPoint = getPerceptionBounds(x, perception.getX(), y, perception.getY());

                    if (this.asciiMap[perceptionPoint.getX()][perceptionPoint.getY()] == '-')
                        this.asciiMap[perceptionPoint.getX()][perceptionPoint.getY()] = '$';
                }
            }
            if (this.asciiMap[x][y] == '-' || this.asciiMap[x][y] == '$') this.asciiMap[x][y] = agent.getAlias();
        }

    }


    /**
     * @param x1 - First X coordinate
     * @param x2 - Second X coordinate
     * @param y1 - First Y coordinate
     * @param y2 - Second Y coordinate
     * @return - return correct perception point, by checking boundaries
     */
    public Point<Integer, Integer> getPerceptionBounds(int x1, int x2, int y1, int y2) {
        int newX, newY;
        newX = x1 + x2;
        newY = y1 + y2;

        if (newX > 8) newX = 8;
        else if (newX < 0) newX = 0;

        if (newY > 8) newY = 8;
        else if (newY < 0) newY = 0;

        return new Point<>(newX, newY);
    }


    /**
     * Method to remove The Kraken and its danger zone on our map
     * @param cell - Cell on which The Kraken was spotted
     */
    public void killKraken(MapCell cell) {
        for (var perception : this.kraken.perception) {
            Point<Integer, Integer> perceptionPoint = this.getPerceptionBounds(cell.x,
                    perception.getX(), cell.y, perception.getY());

            if (!(perceptionPoint.getX() == cell.x && perceptionPoint.getY() == cell.y)) {
                if (--this.cells[perceptionPoint.getX()][perceptionPoint.getY()].dangerLevel == 0) {
                    this.cells[perceptionPoint.getX()][perceptionPoint.getY()].changeWalkability(true);
                }
            }
        }
        this.cells[cell.x][cell.y].dangerLevel--;
    }

    /**
     * Clear current best paths for all cells,
     * used when we divide solutions in backtracking algorithm in 2 cases:
     * 1) Jack -> Chest
     * or
     * 2) Jack -> Tortuga & Tortuga -> Chest
     *
     * if we do not clear current best paths after calculating first path, the errors will occur
     */
    public void clearPathsForAllCells() {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                this.cells[i][j].currentBestPath = Integer.MAX_VALUE;
            }
        }
    }
}


/**
 * Class to fill map with cell objects
 */
class MapCell {
    public boolean walkable;
    public int x;
    public int y;

    public List<Character> agents = new ArrayList<>();
    // Danger level of the cell
    // It is useful when danger zones of the enemies intersect, so we do not
    // make some cell walkable by mistake (for example, after killing kraken,
    // the rock might be still on it so danger level will not be 0)
    public int dangerLevel = 0;

    // Current best path of the cell, used by the Backtracking algorithm
    public int currentBestPath = Integer.MAX_VALUE;

    // G and H costs used by A* algorithm
    public int g;
    public int h;

    // Parent cell of the current cell, needed to restore path for A* algorithm
    public MapCell parent;

    /**
     * @param x - X coordinate
     * @param y - Y coordinate
     * @param walkable - true if the cell is walkable
     */
    public MapCell(int x, int y, boolean walkable) {
        this.x = x;
        this.y = y;
        this.walkable = walkable;
    }

    // Method to change walkability for the cell (used after killing The Kraken)
    public void changeWalkability(boolean walkable) {
        this.walkable = walkable;
    }

    // Get the heuristic of the cell for A* algorithm
    public int getHeuristic() {
        return g + h;
    }
}


/**
 * Common class for both Pathfinding algorithms
 */
class Solver {

    public Map map;

    /**
     * @param map - Current map instance
     */
    public Solver(Map map) {
        this.map = map;
    }

    public class AStar implements Algorithm {
        boolean tortugaPassed = false;
        boolean krakenPassed = false;

        /**
         * @return List of points which lay the shortest path to the Dead Man's Chest
         */
        @Override
        public List<Point<Integer, Integer>> findShortestPath() {
            int tortugaPath = Integer.MAX_VALUE;
            boolean tortugaPathValid = false;

            List<Point<Integer, Integer>> pointsPath = new ArrayList<>();

            Point<Integer, Integer> start = new Point<>(map.jack.getX(), map.jack.getY());
            Point<Integer, Integer> finish = new Point<>(map.chest.getX(), map.chest.getY());
            // First, we calculate the path straight to the end, without passing through the tortuga
            List<MapCell> straightEndPath = shortestPath(start, finish);

            boolean straightPathValid = straightEndPath != null;

            // Second, we calculate the path through Tortuga
            Point<Integer, Integer> passThroughTortuga = new Point<>(map.tortuga.getX(), map.tortuga.getY());
            List<MapCell> pathThroughTortuga = shortestPath(start, passThroughTortuga);
            List<MapCell> fromTortugaToEnd = new ArrayList<>();

            if (pathThroughTortuga != null) {
                // If we successfully passed through the Tortuga, mark the appropriate flag as true,
                // and try to calculate yet another path from Tortuga straight to the Dead Man's Chest
                this.tortugaPassed = true;
                fromTortugaToEnd = shortestPath(passThroughTortuga, finish);

                if (fromTortugaToEnd != null) {
                    // If the path through Tortuga to the Chest was successful,
                    // we calculate the total path and mark this path as valid
                    tortugaPath = pathThroughTortuga.size() + fromTortugaToEnd.size();
                    tortugaPathValid = true;
                }
            }

            // If none of the paths are valid, there's no path and therefore no answer
            if (!tortugaPathValid && !straightPathValid) return null;

            // Otherwise, we have some path and the initial Actor point will be immediately added to the result
            pointsPath.add(new Point<>(map.jack.getX(), map.jack.getY()));

            if ((tortugaPathValid && straightPathValid && (tortugaPath < straightEndPath.size())) ||
                    (tortugaPathValid && !straightPathValid)) {
                // If Tortuga path is shorter, we form its points path
                for (MapCell cell : pathThroughTortuga) {
                    pointsPath.add(new Point<>(cell.x, cell.y));
                }

                for (MapCell cell : fromTortugaToEnd) {
                    pointsPath.add(new Point<>(cell.x, cell.y));
                }
            } else {
                // Else, if straight path is shorter, we form its points path
                for (MapCell cell : straightEndPath) {
                    pointsPath.add(new Point<>(cell.x, cell.y));
                }
            }

            // We restore original cells of our map to not interfere will later run of Backtracking algorithm
            map.fillCells(true);

            return pointsPath;
        }

        /**
         * @param start - starting point of the algorithm
         * @param finish - finishing point of the algorithm
         * @return list of cells on the map which form the shortest path
         */
        private List<MapCell> shortestPath(Point<Integer, Integer> start, Point<Integer, Integer> finish) {
            MapCell startingCell = map.cells[start.getX()][start.getY()];
            MapCell finishingCell = map.cells[finish.getX()][finish.getY()];

            // We have separate lists for open cells and closed cells
            List<MapCell> openCells = new ArrayList<>();
            List<MapCell> closedCells = new ArrayList<>();
            // We start from opening the starting cell
            openCells.add(startingCell);

            while (!openCells.isEmpty()) {
                // Until we have cells to analyse, we get the first available from the list
                MapCell currentCell = openCells.get(0);

                for (MapCell cell : openCells) {
                    // If any other cell has better F-cost than the cell we already took,
                    // we switch the cell to the cell we just found
                    if (cell.getHeuristic() < currentCell.getHeuristic() ||
                            (cell.getHeuristic() == currentCell.getHeuristic() && cell.h < currentCell.h)) {
                        currentCell = cell;
                    }
                }

                // If we reached the destination, we trace the path
                if (finishingCell == currentCell) return tracePath(startingCell, finishingCell);

                // Since we already analysed current cell, we remove it from the open list
                // and it to the list of closed cells
                openCells.remove(currentCell);
                closedCells.add(currentCell);

                // Next, we retrieve neighbour cells of the current cell, based on the perception of Jack
                HashSet<MapCell> neighbourCells = map.getNeighbourCells(currentCell);
                // If we are currently calculating the path from Tortuga to the end, we need to check
                // if we stand on the diagonal cell of the Kraken, so we can destroy him with the rum casks
                if (this.tortugaPassed && !this.krakenPassed) {
                    for (MapCell neighbourCell : neighbourCells) {
                        // We found a Kraken with our perception zone
                        if (neighbourCell.agents.contains('K')) {
                            map.killKraken(neighbourCell);
                            this.krakenPassed = true;
                            if (!neighbourCell.agents.contains('R')) {
                                if (map.cells[neighbourCell.x][neighbourCell.y].dangerLevel == 0) {
                                    map.cells[neighbourCell.x][neighbourCell.y].changeWalkability(true);
                                }
                            }
                        }
                    }
                }

                // After that, we check neighbour cells once again to update costs
                for (MapCell neighbourCell : neighbourCells) {
                    // If current neighbour cells unreachable, we continue with the next neighbour
                    if (closedCells.contains(neighbourCell) || !neighbourCell.walkable) continue;
                    // New cost is calculated
                    int newCost = currentCell.g + getDistanceBetween(currentCell, neighbourCell);

                    if ((newCost < neighbourCell.g) || !openCells.contains(neighbourCell)) {
                        // Update costs
                        neighbourCell.g = newCost;
                        neighbourCell.h = getDistanceBetween(neighbourCell, finishingCell);
                        // Set the parent to traverse the path after completing
                        neighbourCell.parent = currentCell;
                        // Add neighbour cell to the open cells (if it is not here yet),
                        // so we can analyse it on the next iterations
                        if (!openCells.contains(neighbourCell)) openCells.add(neighbourCell);
                    }
                }
            }

            return null;
        }

        /**
         * Method to trace the shortest path after finding it
         * @param startingCell - starting point of the path
         * @param finishingCell - finishing point of the path
         * @return list of cells on the map which form the shortest path
         */
        private List<MapCell> tracePath(MapCell startingCell, MapCell finishingCell) {
            List<MapCell> path = new ArrayList<>();
            MapCell currentCell = finishingCell;

            // Starting from the finish, we go to the starting point
            // by traversing parents of cells
            while (currentCell != startingCell) {
                path.add(currentCell);
                currentCell = currentCell.parent;
            }

            Collections.reverse(path);
            return path;
        }

        private int getDistanceBetween(MapCell firstCell, MapCell secondCell) {
            // Heuristic to get the distance between two cells
            int deltaX = Math.abs(firstCell.x - secondCell.x);
            int deltaY = Math.abs(firstCell.y - secondCell.y);
            return Math.max(deltaX, deltaY);
        }
    }

    public class Backtracking implements Algorithm {
        private List<MapCell> bestPathToTortuga = new ArrayList<>();
        private List<MapCell> bestPathFromTortugaToEnd = new ArrayList<>();
        private List<MapCell> bestStraightPath = new ArrayList<>();
        // 2D array of visited cells to optimize the algorithm flow
        boolean[][] visited = new boolean[9][9];

        /**
         * @return List of points which lay the shortest path to the Dead Man's Chest
         */
        @Override
        public List<Point<Integer, Integer>> findShortestPath() {
            // Generally, this function works pretty much the same way as it does
            // so you can check comments for this function in A*
            
            int fromTortugaToEnd, tortugaPath = Integer.MAX_VALUE;
            boolean tortugaPathValid = false;

            List<Point<Integer, Integer>> pointsPath = new ArrayList<>();
            MapCell startingCell = map.cells[map.jack.getX()][map.jack.getY()];
            MapCell finishingCell = map.cells[map.chest.getX()][map.chest.getY()];

            MapCell tortugaCell = map.cells[map.tortuga.getX()][map.tortuga.getY()];

            int straightPathSolution = this.shortestPath(startingCell, finishingCell, Integer.MAX_VALUE, 0, false,true);

            boolean straightPathValid = straightPathSolution != Integer.MAX_VALUE;

            if (!straightPathValid) {
                map.fillCells(true);
            }

            int pathThroughTortuga = this.shortestPath(startingCell, tortugaCell, Integer.MAX_VALUE, 0, false,true);
            if (pathThroughTortuga != Integer.MAX_VALUE) {
                map.clearPathsForAllCells();
                map.fillCells(true);
                fromTortugaToEnd = this.shortestPath(tortugaCell, finishingCell, Integer.MAX_VALUE, 0, true, true);
                if (fromTortugaToEnd != Integer.MAX_VALUE) {
                    tortugaPath = pathThroughTortuga + fromTortugaToEnd;
                    tortugaPathValid = true;
                }
            }

            if (!straightPathValid && !tortugaPathValid) return null;

            if (tortugaPathValid && straightPathValid && (tortugaPath < straightPathSolution) ||
                    (tortugaPathValid && !straightPathValid)) {
                for (MapCell cell : this.bestPathToTortuga) {
                    if (cell == tortugaCell) continue;
                    pointsPath.add(new Point<>(cell.x, cell.y));
                }

                for (MapCell cell : this.bestPathFromTortugaToEnd) {
                    pointsPath.add(new Point<>(cell.x, cell.y));
                }
            } else {
                for (MapCell cell : this.bestStraightPath) {
                    pointsPath.add(new Point<>(cell.x, cell.y));
                }
            }

            return pointsPath;
        }
        
        private int shortestPath(MapCell currentCell, MapCell finishingCell, int bestPath, int currentPath,
                          boolean tortugaPassed, boolean krakenIsAlive) {
            // If we reach the destination, we update global array of Backtracking class
            // with appropriate path that we have found
            if (currentCell.x == finishingCell.x && currentCell.y == finishingCell.y) {
                bestPath = Math.min(bestPath, currentPath);
                if (finishingCell == map.cells[map.tortuga.getX()][map.tortuga.getY()]) {
                    this.bestPathToTortuga = new ArrayList<>(List.of(finishingCell));
                    tracePath(finishingCell, bestPath, this.bestPathToTortuga);
                } else if (finishingCell == map.cells[map.chest.getX()][map.chest.getY()]) {
                    if (tortugaPassed) {
                        this.bestPathFromTortugaToEnd = new ArrayList<>(List.of(finishingCell));
                        tracePath(finishingCell, bestPath, this.bestPathFromTortugaToEnd);
                    } else {
                        this.bestStraightPath = new ArrayList<>(List.of(finishingCell));
                        tracePath(finishingCell, bestPath, this.bestStraightPath);
                    }
                }
                return bestPath;
            }

            // Set the current best path for current cell
            currentCell.currentBestPath = currentPath;
            // We visited this cell, so we mark it
            this.visited[currentCell.x][currentCell.y] = true;

            if (tortugaPassed && krakenIsAlive) {
                if (hasUnvisitedNeighbours(currentCell)) {
                    for (MapCell neighbour : map.getNeighbourCells(currentCell)) {
                        if (!neighbour.agents.contains('K') || neighbour.dangerLevel == 0) continue;
                        // Kill kraken once we found it
                        map.killKraken(neighbour);
                        krakenIsAlive = false;

                        if (!neighbour.agents.contains('R')) {
                            if (map.cells[neighbour.x][neighbour.y].dangerLevel == 0) {
                                map.cells[neighbour.x][neighbour.y].changeWalkability(true);
                            }
                        }
                    }
                }
            }

            if (hasUnvisitedNeighbours(currentCell)) {
                for (MapCell neighbour : map.getNeighbourCells(currentCell)) {
                    // currentPath <= 25 is that because, based on generation of millions of maps, the maximum path
                    // I ever found was 24.
                    if (canWalkThrough(neighbour) && currentPath <= 25 && currentPath < neighbour.currentBestPath) {
                        neighbour.currentBestPath = currentPath + 1;
                        bestPath = shortestPath(neighbour, finishingCell, bestPath, currentPath + 1, tortugaPassed, krakenIsAlive);
                    }
                }
            }

            // Mark current cell as unvisited, so we can backtrack to it later
            this.visited[currentCell.x][currentCell.y] = false;

            return bestPath;
        }

        /**
         * @param cell - Map cell
         * @return true if this cell has neighbours that were not visited yet
         */
        private boolean hasUnvisitedNeighbours(MapCell cell) {
            for (MapCell neighbour : map.getNeighbourCells(cell)) {
                if (canWalkThrough(neighbour)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @param cell - Map cell
         * @return true if this cell is walkable and was not visited yet
         */
        private boolean canWalkThrough(MapCell cell) {
            return map.cells[cell.x][cell.y].walkable && !visited[cell.x][cell.y];
        }

        private void tracePath(MapCell finishingCell, int bestPath, List<MapCell> currentPath) {
            if (currentPath == null) {
                currentPath = new ArrayList<>(List.of(finishingCell));
            }

            if (bestPath == 0) {
                Collections.reverse(currentPath);
                return;
            }
            // We trace the path by traversing through the neighbours that have the currentBestPath - 1
            for (MapCell neighbour : map.getNeighbourCells(finishingCell)) {
                if (finishingCell.currentBestPath == neighbour.currentBestPath + 1) {
                    currentPath.add(neighbour);
                    tracePath(neighbour, bestPath - 1, currentPath);
                    break;
                }
            }
        }
    }
}

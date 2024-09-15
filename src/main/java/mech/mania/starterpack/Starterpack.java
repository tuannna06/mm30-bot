package mech.mania.starterpack;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import mech.mania.starterpack.game.GameState;
import mech.mania.starterpack.game.character.MoveAction;
import mech.mania.starterpack.game.character.action.AbilityAction;
import mech.mania.starterpack.game.character.action.AttackAction;
import mech.mania.starterpack.game.character.action.CharacterClassType;
import mech.mania.starterpack.network.Client;
import mech.mania.starterpack.network.Phase;
import mech.mania.starterpack.network.ReceivedMessage;
import mech.mania.starterpack.strategy.Strategy;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static mech.mania.starterpack.strategy.ChooseStrategy.chooseStrategy;

public class Starterpack {
    private static final String rawDebugEnv = System.getenv("DEBUG");
    private static final boolean DEBUG = rawDebugEnv != null && (rawDebugEnv.equals("1") || rawDebugEnv.equalsIgnoreCase("true"));

    private static final String MAIN_USAGE = """
            Usage: java -jar starterpack.jar [command]

            Command can be one of:
            run - runs your bot against an opponent
            serve - serves your bot on a port""";
    private static final String RUN_USAGE = """
            Usage: java -jar starterpack.jar run [opponent]

            Opponent can be one of: self, computerTeam0, computerTeam1""";
    private static final String SERVE_USAGE = """
            Usage: java -jar starterpack.jar serve [port]

            Port is the port to serve on""";

    private static final Map<RunOpponent, List<PrefixCommandPair>> COMMANDS_FOR_OPPONENT = Map.of(
            RunOpponent.SELF, List.of(
                    new PrefixCommandPair("Engine", "npm start 3000 3001"),
                    new PrefixCommandPair("Team 0", "java -jar build/libs/starterpack.jar serve 3000"),
                    new PrefixCommandPair("Team 1", "java -jar build/libs/starterpack.jar serve 3001")
            ),
            RunOpponent.COMPUTER_TEAM_0, List.of(
                    new PrefixCommandPair("Engine", "npm start 0 9001"),
                    new PrefixCommandPair("Team 1", "java -jar build/libs/starterpack.jar serve 9001")
            ),
            RunOpponent.COMPUTER_TEAM_1, List.of(
                    new PrefixCommandPair("Engine", "npm start engine/engine.jar 9001 0"),
                    new PrefixCommandPair("Team 0", "java -jar build/libs/starterpack.jar serve 9001")
            )
    );

    private record PrefixCommandPair(String prefix, String command) {}

    private enum RunOpponent {
        SELF("self"),
        COMPUTER_TEAM_0("computerTeam0"),
        COMPUTER_TEAM_1("computerTeam1");

        private final String value;

        RunOpponent(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private record RunAndOutputArgs(boolean isErr, long timeNs, int index, String line) {}

    private static void runAndOutput(InputStream io, int i, boolean isErr, List<RunAndOutputArgs> output) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(io))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(new RunAndOutputArgs(isErr, System.nanoTime(), i, line.trim()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void run(RunOpponent opponent) throws IOException, InterruptedException {
        ProcessBuilder runPython = new ProcessBuilder("python", "run.py", opponent.getValue());
        runPython.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        runPython.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process runPythonProcess = runPython.start();
        int exit = runPythonProcess.waitFor();
        System.exit(exit);
    }

    private static void serve(int port) throws IOException {
        System.out.printf("Connecting to server on port %d...\n", port);

        Client client = new Client(port);

        client.connect();

        System.out.println("Connected to server on port " + port);

        ObjectMapper objectMapper = new ObjectMapper();

        Strategy strategy;

        while (true) {
            String rawReceived = client.read();

            if (rawReceived != null && !rawReceived.isEmpty()) {
                try {
                    ReceivedMessage receivedMessage = objectMapper.readValue(rawReceived, ReceivedMessage.class);
                    Phase phase = receivedMessage.phase();
                    ObjectNode data = receivedMessage.data();

                    if (phase != Phase.FINISH) {
                        if (DEBUG) {
                            System.out.printf("[TURN %d]: Getting your bot's response to %s phase...\n", turn, phase);
                        }
                    }

                    if (phase == Phase.HELLO_WORLD) {
                        // todo
                        List<CharacterClassType> possibleClasses = objectMapper.readValue(
                                objectMapper.treeAsTokens(message.get("choices")),
                                new TypeReference<>() {}
                        );
                        int numToPick = message.get("numToPick").asInt();
                        int maxPerSameClass = message.get("maxPerSameClass").asInt();

                        Map<CharacterClassType, Integer> output = strategy.decideCharacterClasses(possibleClasses, numToPick, maxPerSameClass);

                        if (output == null) {
                            throw new RuntimeException("Your decideCharacterClasses strategy returned nothing (null)!");
                        }

                        String response = objectMapper.writeValueAsString(output);

                        strategy = new Strategy();

                        client.write(response);
                    } else if (phase.equals("MOVE")) {
                        // todo
                        Map<String, List<MoveAction>> possibleMoves = objectMapper.readValue(
                                objectMapper.treeAsTokens(message.get("possibleMoves")),
                                new TypeReference<>() {}
                        );

                        List<MoveAction> output = strategy.decideMoves(possibleMoves, gameState);

                        if (output == null) {
                            throw new RuntimeException("Your decideMoves strategy returned nothing (null)!");
                        }

                        String response = objectMapper.writeValueAsString(output);

                        client.write(response);
                    } else if ("ATTACK".equals(phase)) {
                        // todo
                        Map<String, List<AttackAction>> possibleAttacks = objectMapper.readValue(
                                objectMapper.treeAsTokens(message.get("possibleAttacks")),
                                new TypeReference<>() {}
                        );
                        List<AttackAction> output = strategy.decideAttacks(possibleAttacks, gameState);

                        if (output == null) {
                            throw new RuntimeException("Your decideAttacks strategy returned nothing (null)!");
                        }

                        String response = objectMapper.writeValueAsString(output);

                        client.write(response);
                    } else if (phase.equals("ABILITY")) {
                        // todo
                        Map<String, List<AbilityAction>> possibleAbilities = objectMapper.readValue(
                                objectMapper.treeAsTokens(message.get("possibleAbilities")),
                                new TypeReference<>() {}
                        );

                        List<AbilityAction> output = strategy.decideAbilities(possibleAbilities, gameState);

                        if (output == null) {
                            throw new RuntimeException("Your decideAbilities strategy returned nothing (null)!");
                        }

                        String response = objectMapper.writeValueAsString(output);

                        client.write(response);
                    } else if ("FINISH".equals(phase)) {
                        // todo
                        break;
                    } else {
                        throw new RuntimeException("Unknown phase type " + phase);
                    }

                    if (DEBUG) {
                        System.out.printf("[TURN %d]: Sent response to %s phase to server!\n", turn, phase);
                    }
                } catch (Exception e) {
                    System.err.println("Something went wrong running your bot: " + e.getMessage());
                    e.printStackTrace(System.err);
                    client.write("null");
                }
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.err.println(MAIN_USAGE);
            System.exit(1);
        }

        String command = args[0];

        if (command.equals("run")) {
            if (args.length != 2) {
                System.err.println(RUN_USAGE);
                System.exit(1);
            }

            String sOpponent = args[1];

            RunOpponent opponent = null;

            for (RunOpponent possible : RunOpponent.values()) {
                if (possible.getValue().equals(sOpponent)) {
                    opponent = possible;
                    break;
                }
            }

            if (opponent == null) {
                System.err.printf("Invalid opponent %s\n", opponent);
                System.err.println(RUN_USAGE);
                System.exit(1);
            }

            run(opponent);
        } else if (command.equals("serve")) {
            if (args.length != 2) {
                System.err.println(SERVE_USAGE);
                System.exit(1);
            }

            Integer port = null;

            try {
                port = Integer.decode(args[1]);
            } catch (Exception e) {
                // Do nothing
            }

            if (port == null) {
                System.err.printf("Invalid port %d\n", port);
                System.err.println(SERVE_USAGE);
                System.exit(1);
            }

            serve(port);
        } else {
            System.err.printf("Unknown command %s\n", command);
            System.err.println(MAIN_USAGE);
            System.exit(1);
        }

//        Option portOption = Option.builder()
//                .longOpt("port")
//                .desc("Port for the 'serve' command")
//                .hasArg()
//                .argName("port")
//                .type(Integer.class)
//                .build();
//
//        options.addOption(commandOption);
//        options.addOption(opponentOption);
//        options.addOption(portOption);
//
//        CommandLineParser parser = new DefaultParser();
//
//        try {
//            CommandLine cmd = parser.parse(options, args);
//
//            String command = cmd.getOptionValue("command");
//
//            if (command.equals("run")) {
//                String opponent = cmd.getOptionValue("opponent");
//                run(RunOpponent.valueOf(opponent.toUpperCase()));
//            } else if (command.equals("serve")) {
//                int port = Integer.parseInt(cmd.getOptionValue("port"));
//                serve(port);
//            } else {
//                System.err.println("Unknown command: " + command);
//                printHelp(options);
//            }
//        } catch (ParseException e) {
//            System.err.println("Error parsing command-line arguments: " + e.getMessage());
//            printHelp(options);
//        }
    }
}

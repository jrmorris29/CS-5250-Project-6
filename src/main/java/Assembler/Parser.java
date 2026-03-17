package Assembler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Parser {
    public enum CommandType {
        A_COMMAND,
        C_COMMAND,
        L_COMMAND
    }

    private final List<String> commands;
    private int currentIndex;
    private String currentCommand;

    public Parser(String filePath) throws IOException {
        List<String> rawLines = Files.readAllLines(Paths.get(filePath), StandardCharsets.US_ASCII);
        commands = new ArrayList<>();

        for (String line : rawLines) {
            String cleaned = cleanLine(line);
            if (!cleaned.isEmpty()) {
                commands.add(cleaned);
            }
        }

        currentIndex = -1;
        currentCommand = null;
    }

    public boolean hasMoreCommands() {
        return currentIndex + 1 < commands.size();
    }

    public void advance() {
        if (!hasMoreCommands()) {
            throw new IllegalStateException("No more commands to advance to.");
        }

        currentIndex++;
        currentCommand = commands.get(currentIndex);
    }

    public CommandType commandType() {
        ensureCurrentCommand();

        if (currentCommand.startsWith("@")) {
            return CommandType.A_COMMAND;
        }

        if (currentCommand.startsWith("(") && currentCommand.endsWith(")")) {
            return CommandType.L_COMMAND;
        }

        return CommandType.C_COMMAND;
    }

    public String symbol() {
        ensureCurrentCommand();

        CommandType type = commandType();

        if (type == CommandType.A_COMMAND) {
            return currentCommand.substring(1);
        }

        if (type == CommandType.L_COMMAND) {
            return currentCommand.substring(1, currentCommand.length() - 1);
        }

        throw new IllegalStateException("symbol() called on a C-command.");
    }

    public String dest() {
        ensureCurrentCommand();

        if (commandType() != CommandType.C_COMMAND) {
            throw new IllegalStateException("dest() called on a non-C-command.");
        }

        int equalsIndex = currentCommand.indexOf('=');

        if (equalsIndex == -1) {
            return "";
        }

        return currentCommand.substring(0, equalsIndex);
    }

    public String comp() {
        ensureCurrentCommand();

        if (commandType() != CommandType.C_COMMAND) {
            throw new IllegalStateException("comp() called on a non-C-command.");
        }

        int equalsIndex = currentCommand.indexOf('=');
        int semicolonIndex = currentCommand.indexOf(';');

        int start = (equalsIndex == -1) ? 0 : equalsIndex + 1;
        int end = (semicolonIndex == -1) ? currentCommand.length() : semicolonIndex;

        return currentCommand.substring(start, end);
    }

    public String jump() {
        ensureCurrentCommand();

        if (commandType() != CommandType.C_COMMAND) {
            throw new IllegalStateException("jump() called on a non-C-command.");
        }

        int semicolonIndex = currentCommand.indexOf(';');

        if (semicolonIndex == -1) {
            return "";
        }

        return currentCommand.substring(semicolonIndex + 1);
    }

    private void ensureCurrentCommand() {
        if (currentCommand == null) {
            throw new IllegalStateException("No current command. Call advance() first.");
        }
    }

    private static String cleanLine(String line) {
        int commentIndex = line.indexOf("//");

        if (commentIndex != -1) {
            line = line.substring(0, commentIndex);
        }

        return line.replaceAll("\\s+", "");
    }
}
package Assembler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final int VARIABLE_START_ADDRESS = 16;
    private static final int MAX_A_VALUE = 32767;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java assembler.Main <input.asm>");
            return;
        }

        Path inputPath = java.nio.file.Paths.get(args[0]);

        if (!Files.exists(inputPath) || !Files.isRegularFile(inputPath)) {
            System.out.println("Input file not found: " + inputPath.toAbsolutePath());
            return;
        }

        Path outputPath = getOutputPath(inputPath);

        try {
            assemble(inputPath, outputPath);
            System.out.println("Assembly successful.");
            System.out.println("Input : " + inputPath.toAbsolutePath());
            System.out.println("Output: " + outputPath.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("Assembly failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void assemble(Path inputPath, Path outputPath) throws IOException {
        SymbolTable symbolTable = new SymbolTable();

        firstPass(inputPath, symbolTable);
        secondPass(inputPath, outputPath, symbolTable);
    }

    private static void firstPass(Path inputPath, SymbolTable symbolTable) throws IOException {
        Parser parser = new Parser(inputPath.toString());
        int romAddress = 0;

        while (parser.hasMoreCommands()) {
            parser.advance();

            Parser.CommandType type = parser.commandType();

            if (type == Parser.CommandType.L_COMMAND) {
                String label = parser.symbol();

                if (symbolTable.contains(label)) {
                    throw new IllegalArgumentException("Duplicate or reserved label: " + label);
                }

                symbolTable.addEntry(label, romAddress);
            } else {
                romAddress++;
            }
        }
    }

    private static void secondPass(Path inputPath, Path outputPath, SymbolTable symbolTable) throws IOException {
        Parser parser = new Parser(inputPath.toString());
        int nextVariableAddress = VARIABLE_START_ADDRESS;
        List<String> binaryLines = new ArrayList<>();

        while (parser.hasMoreCommands()) {
            parser.advance();

            Parser.CommandType type = parser.commandType();

            switch (type) {
                case A_COMMAND:
                    String value = parser.symbol();
                    int address;

                    if (isNonNegativeInteger(value)) {
                        address = Integer.parseInt(value);

                        if (address < 0 || address > MAX_A_VALUE) {
                            throw new IllegalArgumentException("A-instruction value out of range: " + value);
                        }
                    } else {
                        if (!symbolTable.contains(value)) {
                            symbolTable.addEntry(value, nextVariableAddress);
                            nextVariableAddress++;
                        }

                        address = symbolTable.getAddress(value);
                    }

                    binaryLines.add(to16BitBinary(address));
                    break;

                case C_COMMAND:
                    String compBits = Code.comp(parser.comp());
                    String destBits = Code.dest(parser.dest());
                    String jumpBits = Code.jump(parser.jump());

                    binaryLines.add("111" + compBits + destBits + jumpBits);
                    break;

                case L_COMMAND:
                    break;

                default:
                    throw new IllegalStateException("Unexpected command type: " + type);
            }
        }

        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }

        Files.write(outputPath, binaryLines, StandardCharsets.US_ASCII);
    }

    private static boolean isNonNegativeInteger(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }

        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    private static String to16BitBinary(int value) {
        String binary = Integer.toBinaryString(value);
        return String.format("%16s", binary).replace(' ', '0');
    }

    private static Path getOutputPath(Path inputPath) {
        String fileName = inputPath.getFileName().toString();

        if (fileName.endsWith(".asm")) {
            String outputFileName = fileName.substring(0, fileName.length() - 4) + ".hack";
            return inputPath.resolveSibling(outputFileName);
        }

        return inputPath.resolveSibling(fileName + ".hack");
    }
}
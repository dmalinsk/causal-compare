/*
 * Copyright (C) 2019 University of Pittsburgh.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package edu.pitt.dbmi.causal.compare;

import edu.cmu.tetrad.algcomparison.Comparison;
import edu.cmu.tetrad.algcomparison.algorithm.Algorithms;
import edu.cmu.tetrad.algcomparison.simulation.Simulations;
import edu.cmu.tetrad.algcomparison.statistic.Statistics;
import edu.cmu.tetrad.data.simulation.LoadDataAndGraphs;
import edu.cmu.tetrad.util.Parameters;
import edu.pitt.dbmi.causal.compare.conf.Configuration;
import edu.pitt.dbmi.causal.compare.tetrad.AlgorithmModels;
import edu.pitt.dbmi.causal.compare.tetrad.ComparisonProperties;
import edu.pitt.dbmi.causal.compare.tetrad.ParameterModels;
import edu.pitt.dbmi.causal.compare.tetrad.SimulationModels;
import edu.pitt.dbmi.causal.compare.tetrad.StatisticModels;
import edu.pitt.dbmi.causal.compare.valid.ConfigurationValidations;
import edu.pitt.dbmi.causal.compare.valid.ValidationException;
import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 * Aug 14, 2019 4:55:44 PM
 *
 * @author Kevin V. Bui (kvb2@pitt.edu)
 */
public class CausalCompareApplication {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        args = cleanArgs(args);

        CmdArgs cmdArgs = new CmdArgs();
        try {
            CmdParser.parse(args, cmdArgs);
        } catch (CmdParserException exception) {
            System.err.println(exception.getCause().getMessage());
            Applications.showHelp(args, exception.getOptions());
            System.exit(-1);
        }

        try {
            ConfigurationValidations.validate(cmdArgs.getConfiguration());
        } catch (ValidationException exception) {
            System.err.println(exception.getLocalizedMessage());
            System.exit(-1);
        }

        try {
            runComparisonTool(cmdArgs);
        } catch (Exception exception) {
            System.err.println(exception.getLocalizedMessage());
            System.exit(-1);
        }
    }

    private static void runComparisonTool(CmdArgs cmdArgs) throws Exception {
        Configuration config = cmdArgs.getConfiguration();

        Algorithms algorithms = AlgorithmModels.getInstance().create(config.getAlgorithmConfigs());
        Simulations simulations = SimulationModels.getInstance().create(config.getSimulationConfigs());
        Statistics statistics = StatisticModels.getInstance().create(config.getStatistics());
        Parameters parameters = ParameterModels.getInstance().create(config.getParameters());

        String outDir = cmdArgs.getOutDirectory().toString();
        Path dirOut = Paths.get(outDir);
        if (Files.notExists(dirOut)) {
            Files.createDirectory(dirOut);
        }

        String prefix = cmdArgs.getFileNamePrefix();
        Path outTxtFile = Paths.get(outDir, String.format("%s.stdout.txt", prefix));
        try (PrintStream out = new PrintStream(new BufferedOutputStream(Files.newOutputStream(outTxtFile, StandardOpenOption.CREATE)), true)) {
            parameters.set("printStream", out);

            simulations.getSimulations().stream()
                    .filter(e -> e instanceof LoadDataAndGraphs)
                    .map(e -> (LoadDataAndGraphs) e)
                    .forEach(e -> e.setStdout(out));

            Comparison comparison = ComparisonProperties.getInstance().create(config.getComparisonProperties());
            comparison.compareFromSimulations(cmdArgs.getOutDirectory().toString(), simulations, String.format("%s.stat.txt", prefix), algorithms, statistics, parameters);
        }
    }

    private static String[] cleanArgs(String[] args) {
        return (args == null)
                ? new String[0]
                : Arrays.stream(args)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(e -> !e.isEmpty())
                        .toArray(String[]::new);
    }

}

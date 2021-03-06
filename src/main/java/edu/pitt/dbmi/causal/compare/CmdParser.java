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

import edu.pitt.dbmi.causal.compare.conf.Configuration;
import edu.pitt.dbmi.causal.compare.conf.Configurations;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.bind.JAXBException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * Aug 17, 2019 11:56:59 AM
 *
 * @author Kevin V. Bui (kvb2@pitt.edu)
 */
public final class CmdParser {

    private CmdParser() {
    }

    public static void parse(String[] args, CmdArgs cmdArgs) throws CmdParserException {
        Options options = CmdOptions.getInstance().getOptions();
        try {
            CommandLine cmd = (new DefaultParser()).parse(options, args);
            parseRequiredOptions(cmd, options, cmdArgs);
            parseOptionalOptions(cmd, options, cmdArgs);
        } catch (ParseException exception) {
            throw new CmdParserException(options, exception);
        }
    }

    private static void parseOptionalOptions(CommandLine cmd, Options options, CmdArgs cmdArgs) throws CmdParserException {
        cmdArgs.outDirectory = cmd.hasOption(CmdParams.DIR_OUT)
                ? Paths.get(cmd.getOptionValue(CmdParams.DIR_OUT))
                : Paths.get("causal_compare_results");

        cmdArgs.fileNamePrefix = getValidPrefix(cmd, cmdArgs);
    }

    private static void parseRequiredOptions(CommandLine cmd, Options options, CmdArgs cmdArgs) throws CmdParserException {
        String file = cmd.getOptionValue(CmdParams.CONFIG_FILE);
        Path configFile = Paths.get(file);
        if (Files.notExists(configFile)) {
            throw new CmdParserException(options, new FileNotFoundException("File " + file + " does not exist."));
        }
        if (!Files.isRegularFile(configFile)) {
            throw new CmdParserException(options, new FileNotFoundException("File " + file + " is not a file."));
        }

        try {
            cmdArgs.configuration = (Configuration) Configurations.umarshal(configFile);
        } catch (JAXBException exception) {
            exception.printStackTrace(System.err);
            throw new CmdParserException(options, exception);
        }
    }

    private static String getValidPrefix(CommandLine cmd, CmdArgs cmdArgs) {
        if (cmd.hasOption(CmdParams.FILENAME_PREFIX)) {
            return cmd.getOptionValue(CmdParams.FILENAME_PREFIX);
        } else {
            return "causal_compare";
        }
    }

}

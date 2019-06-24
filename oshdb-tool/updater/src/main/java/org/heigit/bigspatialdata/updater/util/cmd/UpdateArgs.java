package org.heigit.bigspatialdata.updater.util.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import java.io.File;
import java.net.URL;

/**
 * Special CMD-Parameters for Updater.
 */
public class UpdateArgs {

  /**
   * Common base parameters.
   */
  @ParametersDelegate
  public BaseArgs baseArgs = new BaseArgs();

  /**
   * The replication URL (e.g. https://planet.openstreetmap.org/replication/minute/)
   */
  @Parameter(names = {"-url"},
      description = "URL to take replication-files from e.g. https://planet.openstreetmap.org/replication/minute/",
      validateWith = URLValidator.class,
      required = true,
      order = 1)
  public URL baseURL;

  /**
   * The path to the config-file for the Kafka-Producer.
   */
  @Parameter(names = {"-kafka"},
      description = "Path to kafka Config",
      required = false,
      order = 4)
  public File kafka;

  /**
   * The JDBC-Settings for the keytables.
   */
  @Parameter(names = {"-keytables", "-k"},
      description = "Configuration of Keytables JDBC (parallel to jdbc)",
      required = true,
      order = 3)
  public String keytables;

}

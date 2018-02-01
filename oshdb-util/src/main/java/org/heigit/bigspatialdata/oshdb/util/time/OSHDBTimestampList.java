package org.heigit.bigspatialdata.oshdb.util.time;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTimestamp;

/**
 * Provider of a sorted list of (unix) timestamps.
 */
public interface OSHDBTimestampList extends Serializable {
  /**
   * Provides a sorted list of OSHDBTimestamps.
   *
   * @return a list of oshdb timestamps
   */
  List<OSHDBTimestamp> get();

  /**
   * Convenience method that converts the timestamp list into raw unix timestamps (long values)
   *
   * @return this list of timestamps as raw unix timestamps (measured in seconds)
   */
  default List<Long> getRawUnixTimestamps() {
    return this.get().stream().map(OSHDBTimestamp::getRawUnixTimestamp).collect(Collectors.toList());
  }
}
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.heigit.bigspatialdata.oshdb.api.tests;

import org.heigit.bigspatialdata.oshdb.api.db.OSHDB_Database;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDB_H2;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.MapReducer;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.OSMContributionView;
import org.heigit.bigspatialdata.oshdb.util.time.OSHDBTimestamps;
import org.heigit.bigspatialdata.oshdb.api.object.OSMContribution;
import org.heigit.bigspatialdata.oshdb.osm.OSMType;
import org.heigit.bigspatialdata.oshdb.util.BoundingBox;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TestMapAggregate {
  private final OSHDB_Database oshdb;

  private final BoundingBox bbox = new BoundingBox(8, 9, 49, 50);
  private final OSHDBTimestamps timestamps72 = new OSHDBTimestamps("2010-01-01", "2015-12-01", OSHDBTimestamps.Interval.MONTHLY);

  private final double DELTA = 1e-8;

  public TestMapAggregate() throws Exception {
    oshdb = new OSHDB_H2("./src/test/resources/test-data");
  }

  private MapReducer<OSMContribution> createMapReducerOSMContribution() throws Exception {
    return OSMContributionView.on(oshdb).osmTypes(OSMType.NODE).where("highway").areaOfInterest(bbox);
  }

  @Test
  public void test() throws Exception {
    SortedMap<Long, Set<Integer>> result = createMapReducerOSMContribution()
        .timestamps(timestamps72)
        .where(entity -> entity.getId() == 617308093)
        .aggregateBy(contribution -> contribution.getEntityAfter().getId())
        .map(OSMContribution::getContributorUserId)
        .reduce(
            HashSet::new,
            (x,y) -> { x.add(y); return x; },
            (x,y) -> { Set<Integer> ret = new HashSet<>(x); ret.addAll(y); return ret; }
        );

    assertEquals(1, result.entrySet().size());
    // should be 5: first version doesn't have the highway tag, remaining 7 versions have 5 different contributor user ids
    assertEquals(5, result.get(617308093L).size());
  }
}

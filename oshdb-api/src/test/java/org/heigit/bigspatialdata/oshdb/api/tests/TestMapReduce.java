/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.heigit.bigspatialdata.oshdb.api.tests;

import org.heigit.bigspatialdata.oshdb.OSHDB;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDB_Implementation;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDB_JDBC;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.MapReducer;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.OSMContributionView;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.OSMEntitySnapshotView;
import org.heigit.bigspatialdata.oshdb.api.utils.OSHDBTimestamps;
import org.heigit.bigspatialdata.oshdb.api.objects.OSMContribution;
import org.heigit.bigspatialdata.oshdb.api.objects.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.osm.OSMType;
import org.heigit.bigspatialdata.oshdb.util.BoundingBox;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 *
 */
abstract class TestMapReduce {
  final OSHDB_Implementation oshdb;
  OSHDB_JDBC keytables = null;

  private final BoundingBox bbox = new BoundingBox(8, 9, 49, 50);
  private final OSHDBTimestamps timestamps6 = new OSHDBTimestamps("2010-01-01", "2015-01-01", OSHDBTimestamps.Interval.YEARLY);
  private final OSHDBTimestamps timestamps72 = new OSHDBTimestamps("2010-01-01", "2015-12-01", OSHDBTimestamps.Interval.MONTHLY);

  TestMapReduce(OSHDB_Implementation oshdb) throws Exception {
    this.oshdb = oshdb;
  }

  private MapReducer<OSMContribution> createMapReducerOSMContribution() throws Exception {
    MapReducer<OSMContribution> mapRed = OSMContributionView.on(oshdb);
    if (this.keytables != null) mapRed = mapRed.keytables(this.keytables);
    return mapRed.osmTypes(OSMType.NODE).where("highway").areaOfInterest(bbox);
  }

  private MapReducer<OSMEntitySnapshot> createMapReducerOSMEntitySnapshot() throws Exception {
    MapReducer<OSMEntitySnapshot> mapRed = OSMEntitySnapshotView.on(oshdb);
    if (this.keytables != null) mapRed = mapRed.keytables(this.keytables);
    return mapRed.osmTypes(OSMType.NODE).where("highway").areaOfInterest(bbox);
  }

  @Test
  public void testOSMContributionView() throws Exception {
    // simple query
    Set<Integer> result = createMapReducerOSMContribution()
        .timestamps(timestamps72)
        .where(entity -> entity.getId() == 617308093)
        .map(OSMContribution::getContributorUserId)
        .uniq();

    /* should be 5: first version doesn't have the highway tag, remaining 7 versions have 5 different contributor user ids*/
    assertEquals(5, result.size());

    // "flatMap"
    result = createMapReducerOSMContribution()
        .timestamps(timestamps72)
        .where(entity -> entity.getId() == 617308093)
        .map(OSMContribution::getContributorUserId)
        .filter(uid -> uid > 0)
        .uniq();

    /* should be 5: first version doesn't have the highway tag, remaining 7 versions have 5 different contributor user ids*/
    assertEquals(5, result.size());
  }

  @Test
  public void testOSMEntitySnapshotView() throws Exception {
    // simple query
    Set<Integer> result = createMapReducerOSMEntitySnapshot()
        .timestamps(timestamps6)
        .where(entity -> entity.getId() == 617308093)
        .map(snapshot -> snapshot.getEntity().getUserId())
        .uniq();

    assertEquals(3, result.size());

    // "flatMap"
    result = createMapReducerOSMEntitySnapshot()
        .timestamps(timestamps6)
        .where(entity -> entity.getId() == 617308093)
        .map(snapshot -> snapshot.getEntity().getUserId())
        .filter(uid -> uid > 0)
        .uniq();

    assertEquals(3, result.size());
  }
}
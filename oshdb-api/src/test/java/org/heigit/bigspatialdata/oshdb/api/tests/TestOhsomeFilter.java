package org.heigit.bigspatialdata.oshdb.api.tests;

import org.heigit.bigspatialdata.oshdb.api.db.OSHDBH2;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBDatabase;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.MapReducer;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.OSMContributionView;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.OSMEntitySnapshotView;
import org.heigit.bigspatialdata.oshdb.api.object.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.osm.OSMEntity;
import org.heigit.bigspatialdata.oshdb.osm.OSMMember;
import org.heigit.bigspatialdata.oshdb.osm.OSMRelation;
import org.heigit.bigspatialdata.oshdb.util.OSHDBBoundingBox;
import org.heigit.bigspatialdata.oshdb.util.taginterpreter.TagInterpreter;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;
import org.heigit.bigspatialdata.oshdb.util.time.OSHDBTimestamps;
import org.heigit.bigspatialdata.oshdb.api.object.OSMContribution;
import org.heigit.bigspatialdata.oshdb.osm.OSMType;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;
import org.heigit.ohsome.filter.FilterParser;
import org.junit.Test;

import java.util.Set;
import java.util.SortedMap;

import static org.junit.Assert.assertEquals;

/**
 * Tests integration of ohsome-filter library.
 *
 * <p>
 *   Only basic "is it working at all" tests are done, since the library itself has its own set
 *   of unit tests.
 * </p>
 */
public class TestOhsomeFilter {
  private final OSHDBDatabase oshdb;
  private final FilterParser filterParser;

  private final OSHDBBoundingBox bbox = new OSHDBBoundingBox(8.651133,49.387611,8.6561,49.390513);

  public TestOhsomeFilter() throws Exception {
    OSHDBH2 oshdb = new OSHDBH2("./src/test/resources/test-data");
    filterParser = new FilterParser(new TagTranslator(oshdb.getConnection()));
    this.oshdb = oshdb;
  }

  private MapReducer<OSMEntitySnapshot> createMapReducer() throws Exception {
    return OSMEntitySnapshotView.on(oshdb)
        .areaOfInterest(bbox)
        .timestamps("2014-01-01");
  }

  @Test
  public void testFilterString() throws Exception {
    Number result = createMapReducer()
        .map(x -> 1)
        .filter("type:way and geometry:polygon and building=*")
        .sum();

    assertEquals(42, result.intValue());
  }

  @Test
  public void testFilterObject() throws Exception {
    MapReducer<OSMEntitySnapshot> mr = createMapReducer();
    Number result = mr
        .filter(filterParser.parse("type:way and geometry:polygon and building=*"))
        .count();

    assertEquals(42, result.intValue());
  }

  @Test
  public void testAggregateFilter() throws Exception {
    SortedMap<OSMType, Integer> result = createMapReducer()
        .aggregateBy(x -> x.getEntity().getType())
        .filter("(geometry:polygon or geometry:other) and building=*")
        .count();

    assertEquals(2, result.entrySet().size());
    assertEquals(42, result.get(OSMType.WAY).intValue());
    assertEquals(1, result.get(OSMType.RELATION).intValue());
  }

  @Test
  public void testAggregateFilterObject() throws Exception {
    MapReducer<OSMEntitySnapshot> mr = createMapReducer();
    SortedMap<OSMType, Integer> result = mr
        .aggregateBy(x -> x.getEntity().getType())
        .filter(filterParser.parse("(geometry:polygon or geometry:other) and building=*"))
        .count();

    assertEquals(42, result.get(OSMType.WAY).intValue());
  }
}
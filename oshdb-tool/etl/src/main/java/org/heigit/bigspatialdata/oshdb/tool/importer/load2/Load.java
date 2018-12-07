package org.heigit.bigspatialdata.oshdb.tool.importer.load2;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.heigit.bigspatialdata.oshdb.index.zfc.ZGrid;
import org.heigit.bigspatialdata.oshdb.osm.OSMNode;
import org.heigit.bigspatialdata.oshdb.osm.OSMType;
import org.heigit.bigspatialdata.oshdb.tool.importer.cli.validator.DirExistValidator;
import org.heigit.bigspatialdata.oshdb.tool.importer.load2.LoaderGrid.Grid;
import org.heigit.bigspatialdata.oshdb.tool.importer.load2.handler.OSHDBHandler;
import org.heigit.bigspatialdata.oshdb.tool.importer.osh.TransformOSHNode;
import org.heigit.bigspatialdata.oshdb.tool.importer.osh.TransformOSHRelation;
import org.heigit.bigspatialdata.oshdb.tool.importer.osh.TransformOSHWay;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Streams;

import it.unimi.dsi.fastutil.io.FastByteArrayOutputStream;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;

public class Load {

	public static String hRBC(long bytes) {
		final int unit = 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		final String pre = "" + "kMGTPE".charAt(exp - 1);
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	public static class LoadHandler extends OSHDBHandler {
		private final long MB = 1024L * 1024L;

		private final Roaring64NavigableMap invalidNodes;
		private final Roaring64NavigableMap invalidWays;

		private final GridWriter nodeWriter;
		private final GridWriter wayWriter;
		private final GridWriter relationWriter;

		private final FastByteArrayOutputStream out = new FastByteArrayOutputStream(1024);
		private long totalNodeBytes = 0;
		private long totalWayBytes = 0;
		private long totalRelBytes = 0;
		private long totalBytes = 0;
		
		private final Stopwatch stopwatch;

		private LoadHandler(GridWriter nodeWriter, GridWriter wayWriter, GridWriter relationWriter,
				Roaring64NavigableMap invalidNodes, Roaring64NavigableMap invalidWays) {
			this.nodeWriter = nodeWriter;
			this.wayWriter = wayWriter;
			this.relationWriter = relationWriter;
			this.invalidNodes = invalidNodes;
			this.invalidWays = invalidWays;
			
			this.stopwatch = Stopwatch.createStarted();
		}

		@Override
		public boolean loadNodeCondition(Grid grid) {
			if ((grid.countNodes() > 1000 && grid.sizeNodes() >= 2L * MB) || grid.sizeNodes() >= 4L * MB) {
				return true;
			}
			return false;
		}

		@Override
		public boolean loadWayCondition(Grid grid) {
			long size = grid.sizeNodes() + grid.sizeRefNodes() + grid.sizeWays();
			if ((grid.countWays() > 1000 && size >= 2L * MB) || (grid.countWays() >= 10 && size >= 4L * MB)) {
				return true;
			}
			return false;
		}

		@Override
		public boolean loadRelCondition(Grid grid) {
			long size = grid.size();
			if ((grid.countRelations() > 1000 && size >= 2L * MB) || (grid.countRelations() >=10 && size >= 4L * MB)) {
				return true;
			}
			return false;
		}

		private FastByteArrayOutputStream writeToOut(Object grid) throws IOException {
			out.reset();
			try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
				oos.writeObject(grid);
				oos.flush();
			}
			totalBytes += out.length;
			return out;
		}
	
		@Override
		public void handleNodeGrid(long zId, List<TransformOSHNode> nodes) throws IOException {
			String check = nodes.stream().limit(2).map(osh -> {
				return osh.stream().limit(2).map(osm -> String.format("%d[%d,%s] %.7f,%.7f", osm.getId(),osm.getVersion(),osm.isVisible(),osm.getLatitude(),osm.getLongitude())).collect(Collectors.joining("/","(",")"));
			}).collect(Collectors.joining("|"));
			System.out.println("check-> "+check);
			super.handleNodeGrid(zId, nodes);
		}
		
		@Override
		public void handleNodeGrid(long zId, int seq, int[] offsets, int size, byte[] data) throws IOException {
			long bytes = size*4+data.length;// nodeWriter.write(zId, seq, offsets, size, data);
			totalNodeBytes += bytes;
			totalBytes += bytes;

			System.out.printf("n %2d:%8d (%3d)[c:%4d] -> b:%10s - tN:%10s - t:%10s%n", ZGrid.getZoom(zId),
					ZGrid.getIdWithoutZoom(zId), seq, size, hRBC(bytes), hRBC(totalNodeBytes), hRBC(totalBytes));
		}

		@Override
		public void handleWayGrid(long zId, List<TransformOSHWay> ways, List<TransformOSHNode> nodes)
				throws IOException {
			String check = nodes.stream().limit(2).map(osh -> {
				return osh.stream().limit(2).map(osm -> String.format("%d[%d,%s] %.7f,%.7f", osm.getId(),osm.getVersion(),osm.isVisible(),osm.getLatitude(),osm.getLongitude())).collect(Collectors.joining("/","(",")"));
			}).collect(Collectors.joining("|"));
			System.out.println("check-> "+check);
			super.handleWayGrid(zId, ways, nodes);
		}

		@Override
		public void handleWayGrid(long zId, int seq, int[] offsets, int size, byte[] data) throws IOException {
			long bytes = size*4+data.length;// wayWriter.write(zId, seq, offsets, size, data);
			totalWayBytes += bytes;
			totalBytes += bytes;

			System.out.printf("w %2d:%8d (%3d)[c:%4d] -> b:%10s - tN:%10s - t:%10s%n", ZGrid.getZoom(zId), ZGrid.getIdWithoutZoom(zId), seq, size, hRBC(bytes), hRBC(totalWayBytes), hRBC(totalBytes));
			if(missingNodes.size() > 0){
				System.out.printf("missing nodes(%d) ->[%s,...]  %s%n",missingNodes.size(),Iterators.toString(Streams.stream(missingNodes.iterator()).limit(5).iterator()),stopwatch);
				missingNodes.clear();
			}

		}

		@Override
		public void handleRelationGrid(long zId, int seq, int[] offsets, int size, byte[] data) throws IOException {
			long bytes = size*4+data.length;//relationWriter.write(zId, seq, offsets, size, data);
			totalRelBytes += bytes;
			totalBytes += bytes;

			System.out.printf("r %2d:%8d (%3d)[c:%4d] -> b:%10s - tN:%10s - t:%10s%n", ZGrid.getZoom(zId), ZGrid.getIdWithoutZoom(zId), seq, size, hRBC(bytes), hRBC(totalRelBytes), hRBC(totalBytes));
			if(missingNodes.size() > 0){
				System.out.printf("missing nodes(%d) ->[%s,...]  %s%n",missingNodes.size(),Iterators.toString(Streams.stream(missingNodes.iterator()).limit(5).iterator()),stopwatch);
				missingNodes.clear();
			}
			if(missingWays.size() > 0){
				System.out.printf("missing ways(%d) ->[%s,...]   %s%n",missingWays.size(),Iterators.toString(Streams.stream(missingWays.iterator()).limit(10).iterator()),stopwatch);
				missingWays.clear();
			}

		}

		
		LongSortedSet missingNodes = new LongAVLTreeSet();
		@Override
		public void missingNode(long id) {
			if (invalidNodes.contains(id))
				return;
			missingNodes.add(id);
			return;
		}
		
		LongSortedSet missingWays = new LongAVLTreeSet();
		@Override
		public void missingWay(long id) {
			if (invalidWays.contains(id))
				return;
			missingWays.add(id);
			return;
		}
		
		@Override
		public boolean filterNode(TransformOSHNode osh) {
			for (OSMNode osm : osh) {
				if (osm.getRawTags().length > 0)
					return true;
			}
			return false;
		}
		
		@Override
		public boolean filterWay(TransformOSHWay osh) {
			return true;
		}
		
		@Override
		public boolean filterRelation(TransformOSHRelation osh) {
			return true;
		}
	}

	public static class Args {
		@Parameter(names = { "-workDir",
				"--workingDir" }, description = "path to store the result files.", validateWith = DirExistValidator.class, required = true, order = 10)
		public Path workDir;

		@Parameter(names = { "--out" }, description = "output path", required = true)
		public Path output;
	}

	private static PeekingIterator<CellData> merge(Path workDir, OSMType type, String glob) throws IOException {
		List<ReaderCellData> readers = Lists.newArrayList();
		for (Path path : Files.newDirectoryStream(workDir, glob)) {
			readers.add(new ReaderCellData(path, type));
		}
		return Iterators.peekingIterator(Iterators.mergeSorted(readers, (a, b) -> {
			int c = ZGrid.ORDER_DFS_BOTTOM_UP.compare(a.cellId, b.cellId);
			if (c == 0)
				c = Long.compare(a.id, b.id);
			return c;
		}));
	}
	
	private static PeekingIterator<CellBitmaps> merge(Path workDir, String glob) throws IOException {
		List<ReaderBitmap> readers = Lists.newArrayList();
		for (Path path : Files.newDirectoryStream(workDir, glob)) {
			readers.add(new ReaderBitmap(path));
		}
		return Iterators.peekingIterator(Iterators.mergeSorted(readers, (a, b) -> {
			int c = ZGrid.ORDER_DFS_TOP_DOWN.compare(a.cellId, b.cellId);
			return c;
		}));
	}

	private static Roaring64NavigableMap skipInvalid(PeekingIterator<CellData> reader) {
		Roaring64NavigableMap invalid = new Roaring64NavigableMap();
		while (reader.hasNext() && reader.peek().cellId < 0) {
			CellData cell = reader.next();
			invalid.add(cell.id);
		}
		return invalid;
	}

	public static void main(String[] args) throws IOException {
		Args config = new Args();
		JCommander jcom = JCommander.newBuilder().addObject(config).build();

		try {
			jcom.parse(args);
		} catch (ParameterException e) {
			System.out.println("");
			System.out.println(e.getLocalizedMessage());
			System.out.println("");
			jcom.usage();
			return;
		}

		final Path workDir = config.workDir;

		
		PeekingIterator<CellBitmaps> bitmapWayRefReader = merge(workDir, "transform_ref_way_*");
		PeekingIterator<CellBitmaps> bitmapRelRefReader = Iterators.peekingIterator(Collections.emptyIterator()); //  merge(workDir, "transfrom_ref_relation_*");
		
		PeekingIterator<CellBitmaps> bitmapReader = Iterators
				.peekingIterator(Iterators.mergeSorted(Lists.newArrayList(bitmapWayRefReader, bitmapRelRefReader), (a, b) -> {
					int c = ZGrid.ORDER_DFS_TOP_DOWN.compare(a.cellId, b.cellId);
					return c;
				}));
		
		while (bitmapReader.hasNext() && bitmapReader.peek().cellId == -1) {
			bitmapReader.next();
		}

		PeekingIterator<CellData> nodeReader = merge(workDir, OSMType.NODE, "transform_node_*");
		PeekingIterator<CellData> wayReader = merge(workDir, OSMType.WAY, "transform_way_*");
		PeekingIterator<CellData> relReader = Iterators.peekingIterator(Collections.emptyIterator()); //  merge(workDir, OSMType.RELATION, "transform_relation_*");

		Stopwatch stopwatch = Stopwatch.createUnstarted();
		stopwatch.reset().start();
		final Roaring64NavigableMap invalidNodes = skipInvalid(nodeReader);
		System.out.println("Skipped " + invalidNodes.getLongCardinality() + " invalid nodes in " + stopwatch);
		stopwatch.reset().start();
		final Roaring64NavigableMap invalidWays = skipInvalid(wayReader);
		System.out.println("Skipped " + invalidWays.getLongCardinality() + " invalid ways in " + stopwatch);
		stopwatch.reset().start();
		final Roaring64NavigableMap invalidRelations = skipInvalid(relReader);
		System.out.println("Skipped " + invalidRelations.getLongCardinality() + " invalid relations in " + stopwatch);

		PeekingIterator<CellData> entityReader = Iterators
				.peekingIterator(Iterators.mergeSorted(Lists.newArrayList(nodeReader, wayReader, relReader), (a, b) -> {
					int c = ZGrid.ORDER_DFS_BOTTOM_UP.compare(a.cellId, b.cellId);
					if (c == 0) {
						c = a.type.compareTo(b.type) * -1;
						if (c == 0) {
							c = Long.compare(a.id, b.id);
						}
					}
					return c;
				}));

		final String output = config.output.toString();
		try (GridWriter nodeWriter = new GridWriter(output + "_nodes");
				GridWriter wayWriter = new GridWriter(output + "_ways");
				GridWriter relWriter = new GridWriter(output + "_relations")) {

			LoadHandler handler = new LoadHandler(nodeWriter, wayWriter, relWriter, invalidNodes, invalidWays);
			LoaderGrid loader = new LoaderGrid(entityReader, bitmapReader, handler);
			loader.run();
		}
	}
}

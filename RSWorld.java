import java.util.*;

public class RSWorld
{
	public static final int B_AIR = 0;
	public static final int B_SOLID = 1;
	public static final int B_RWIRE = 2;
	public static final int B_RTORCH = 3;
	public static final int B_RDIODE = 4;

	private static final int[] WIRE_WALK_X = { 0,-1, 0, 1, 0, 0};
	private static final int[] WIRE_WALK_Y = { 0, 0, 0, 0,-1, 1};
	private static final int[] WIRE_WALK_Z = {-1, 0, 1, 0, 0, 0};

	private HashMap<RSPos, RSChunk> chunkMap = new HashMap<RSPos, RSChunk>();
	private HashSet<RSPos> chunksDirty = new HashSet<RSPos>();
	private HashSet<RSPos> chunksDirtyBack = new HashSet<RSPos>();
	private HashSet<RSPos> redWires = new HashSet<RSPos>();
	private HashSet<RSPos> redTorches = new HashSet<RSPos>();
	private HashSet<RSPos> redSources = new HashSet<RSPos>();
	private HashSet<RSPos> redCharges = new HashSet<RSPos>();
	private HashSet<RSPos> redSinks = new HashSet<RSPos>();

	private RSChunk genChunk(int x, int z)
	{
		return new RSChunk(this, x, z);
	}

	private RSChunk fetchChunk(int x, int z)
	{
		RSChunk chunk = genChunk(x, z);
		return chunk;
	}

	private RSChunk getChunk(int x, int z)
	{
		RSPos pos = new RSPos(x, 0, z);

		if(!chunkMap.containsKey(pos))
		{
			RSChunk chunk = fetchChunk(x, z);
			chunkMap.put(pos, chunk);
			chunksDirty.add(pos);
			return chunk;
		} else {
			return chunkMap.get(pos);
		}
	}

	private RSChunk getBlockChunk(int x, int z)
	{
		return getChunk(x >> 4, z >> 4);
	}

	private boolean isChunkLoaded(int x, int z)
	{
		return chunkMap.containsKey(new RSPos(x, 0, z));
	}

	public synchronized boolean isBlockChunkLoaded(int x, int z)
	{
		return isChunkLoaded(x >> 4, z >> 4);
	}

	public synchronized int getBlockType(int x, int y, int z)
	{
		return getBlockChunk(x, z).getBlockType(x&15, y, z&15);
	}

	public synchronized int getBlockMeta(int x, int y, int z)
	{
		return getBlockChunk(x, z).getBlockMeta(x&15, y, z&15);
	}

	public synchronized int getTopY(int x, int z)
	{
		return getBlockChunk(x, z).getTopY(x&15, z&15);
	}

	public synchronized boolean setBlock(int x, int y, int z, int id, int meta)
	{
		//posDirty.add(new RSPos(x, y, z));
		return getBlockChunk(x, z).setBlock(x&15, y, z&15, id, meta);
	}

	private synchronized void setBlockCharge(int x, int y, int z, boolean charged)
	{
		getBlockChunk(x, z).setBlockCharge(x&15, y, z&15, charged);
	}

	private void traceRedSolidEndTorch(int sx, int sy, int sz, int dir)
	{
		int type = getBlockType(sx, sy, sz);
		if(type != RSWorld.B_RTORCH) return;
		int meta = getBlockMeta(sx, sy, sz);

		// Torch must be of the correct direction
		int mdir = (meta & 0x07);
		if(mdir != dir) return;

		// Charge it!
		redSinks.add(new RSPos(sx, sy, sz));
	}

	private void traceRedSolidEnd(int sx, int sy, int sz, int dir)
	{
		int type = getBlockType(sx, sy, sz);
		if(type != RSWorld.B_SOLID) return;
		int meta = getBlockMeta(sx, sy, sz);

		// Add to sinks
		redSinks.add(new RSPos(sx, sy, sz));

		// Charge adjacent torches
		traceRedSolidEndTorch(sx + 0, sy + 0, sz - 1, 0);
		traceRedSolidEndTorch(sx - 1, sy + 0, sz + 0, 1);
		traceRedSolidEndTorch(sx + 0, sy + 0, sz + 1, 2);
		traceRedSolidEndTorch(sx + 1, sy + 0, sz + 0, 3);
		traceRedSolidEndTorch(sx + 0, sy + 1, sz + 0, 5);
	}

	private void traceRedWireToWire(int sx, int sy, int sz, int dir, int dist)
	{
		// Get info
		int type = getBlockType(sx, sy, sz);
		if(type != RSWorld.B_RWIRE) return;
		int meta = getBlockMeta(sx, sy, sz);

		// Handle 
		traceRedWire(sx, sy, sz, dir, dist);
	}

	private void traceRedWireToSolid(int sx, int sy, int sz, int dir)
	{
		traceRedSolidEnd(sx, sy, sz, dir);
	}

	private void traceRedWire(int sx, int sy, int sz, int dir, int dist)
	{
		// Skip if dist is 0
		if(dist <= 0) return;

		// Charge and step away
		redCharges.add(new RSPos(sx, sy, sz));
		dist--;

		// Trace neighbouring wires
		// NOTE: refactoring this so it only checks dir once
		// is probably not going to improve the speed enough to warrant it
		if(dir != 2) traceRedWireToWire(sx + 0, sy - 1, sz - 1, 0, dist);
		if(dir != 2) traceRedWireToWire(sx + 0, sy + 0, sz - 1, 0, dist);
		if(dir != 2) traceRedWireToWire(sx + 0, sy + 1, sz - 1, 0, dist);
		if(dir != 3) traceRedWireToWire(sx - 1, sy - 1, sz + 0, 1, dist);
		if(dir != 3) traceRedWireToWire(sx - 1, sy + 0, sz + 0, 1, dist);
		if(dir != 3) traceRedWireToWire(sx - 1, sy + 1, sz + 0, 1, dist);
		if(dir != 0) traceRedWireToWire(sx + 0, sy - 1, sz + 1, 2, dist);
		if(dir != 0) traceRedWireToWire(sx + 0, sy + 0, sz + 1, 2, dist);
		if(dir != 0) traceRedWireToWire(sx + 0, sy + 1, sz + 1, 2, dist);
		if(dir != 1) traceRedWireToWire(sx + 1, sy - 1, sz + 0, 3, dist);
		if(dir != 1) traceRedWireToWire(sx + 1, sy + 0, sz + 0, 3, dist);
		if(dir != 1) traceRedWireToWire(sx + 1, sy + 1, sz + 0, 3, dist);

		// Charge floor
		traceRedWireToSolid(sx + 0, sy - 1, sz + 0, 4);
	}

	private void traceRedTorchToWire(int sx, int sy, int sz, int dir)
	{
		int type = getBlockType(sx, sy, sz);
		if(type != RSWorld.B_RWIRE) return;
		int meta = getBlockMeta(sx, sy, sz);

		// Trace
		traceRedWire(sx, sy, sz, dir, 15);
	}

	private void traceRedTorchToSolid(int sx, int sy, int sz, int dir)
	{
		traceRedSolidEnd(sx, sy, sz, dir);
	}

	private void traceRedSolidToWire(int sx, int sy, int sz, int dir)
	{
		int type = getBlockType(sx, sy, sz);
		if(type != RSWorld.B_RWIRE) return;
		int meta = getBlockMeta(sx, sy, sz);

		// Trace
		traceRedWire(sx, sy, sz, dir, 15);
	}


	private void traceRedTorchStart(int sx, int sy, int sz, int meta)
	{
		// Trace to wires
		int dir = meta & 0x07;
		if(dir != 2) traceRedTorchToWire(sx + 0, sy + 0, sz - 1, 0);
		if(dir != 3) traceRedTorchToWire(sx - 1, sy + 0, sz + 0, 1);
		if(dir != 0) traceRedTorchToWire(sx + 0, sy + 0, sz + 1, 2);
		if(dir != 1) traceRedTorchToWire(sx + 1, sy + 0, sz + 0, 3);
		if(dir != 5) traceRedTorchToWire(sx + 0, sy - 1, sz + 0, 4);

		// Trace to solids
		traceRedTorchToSolid(sx + 0, sy + 1, sz + 0, 5);
	}

	private void traceRedSolidStart(int sx, int sy, int sz, int meta)
	{
		// Check charge of neighbours
		// TODO!

		// Trace to wires
		traceRedSolidToWire(sx + 0, sy + 0, sz - 1, 0);
		traceRedSolidToWire(sx - 1, sy + 0, sz + 0, 1);
		traceRedSolidToWire(sx + 0, sy + 0, sz + 1, 2);
		traceRedSolidToWire(sx + 1, sy + 0, sz + 0, 3);
		traceRedSolidToWire(sx + 0, sy - 1, sz + 0, 4);
		// TODO: Get behaviour more accurate,
		// THEN allow for charging top of block
		//traceRedSolidToWire(sx + 0, sy + 1, sz + 0, 5);

	}

	private void traceRedStart(int sx, int sy, int sz)
	{
		int type = getBlockType(sx, sy, sz);
		int meta = getBlockMeta(sx, sy, sz);

		switch(type)
		{
			case RSWorld.B_RTORCH:
				if(!redSources.contains(new RSPos(sx, sy, sz)))
					return;
				traceRedTorchStart(sx, sy, sz, meta);
				break;

			case RSWorld.B_SOLID:
				if(!redSources.contains(new RSPos(sx, sy, sz)))
					return;
				traceRedSolidStart(sx, sy, sz, meta);
				break;
		}

	}

	public synchronized void tick()
	{
		// Swap chunksDirty buffers
		HashSet<RSPos> cdtemp = chunksDirty;
		chunksDirty = chunksDirtyBack;
		chunksDirtyBack = cdtemp;

		// Update all dirty chunks
		for(RSPos p : chunksDirtyBack)
		{
			// Skip unloaded chunks
			if(!isBlockChunkLoaded(p.x, p.z))
				continue;

			// Add RS sources
			getChunk(p.x, p.z).addRedSources(
				redTorches, redWires, redSources);
		}

		// Clear back dirty chunk buffer
		chunksDirtyBack.clear();

		// Clear sinks
		redSinks.clear();

		// Discharge wires
		for(RSPos p : redCharges)
			setBlockCharge(p.x, p.y, p.z, false);

		redCharges.clear();

		// Lead sources to sinks
		for(RSPos p : redSources)
			traceRedStart(p.x, p.y, p.z);

		// Discharge sources
		for(RSPos p : redSources)
			setBlockCharge(p.x, p.y, p.z, false);

		// Charge sinks
		for(RSPos p : redSinks)
			setBlockCharge(p.x, p.y, p.z, true);

		// Charge wires
		for(RSPos p : redCharges)
			setBlockCharge(p.x, p.y, p.z, true);

		// Flip sinks for torches
		for(RSPos p : redTorches)
		{
			if(redSinks.contains(p))
				redSinks.remove(p);
			else
				redSinks.add(p);
		}


		// DEBUG: Stats
		System.out.printf("RS %6d -> %6d\n", redSources.size(), redSinks.size());
		System.out.printf("RT %6d\n", redTorches.size());

		// Swap RS sources and sinks
		HashSet<RSPos> rstemp = redSources;
		redSources = redSinks;
		redSinks = rstemp;

	}
}


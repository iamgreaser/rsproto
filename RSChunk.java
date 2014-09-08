import java.util.*;

public class RSChunk
{
	public static final int CHUNK_WIDTH_BITS = 4;
	public static final int CHUNK_HEIGHT_BITS = 8;
	public static final int CHUNK_WIDTH = 1<<CHUNK_WIDTH_BITS;
	public static final int CHUNK_HEIGHT = 1<<CHUNK_HEIGHT_BITS;

	private final int chunkX, chunkZ;
	private final RSWorld world;

	private boolean dirtyNet = true;
	private short[] blockIDs = new short[CHUNK_HEIGHT*CHUNK_WIDTH*CHUNK_WIDTH];
	private byte[] blockMeta = new byte[CHUNK_HEIGHT*CHUNK_WIDTH*CHUNK_WIDTH];

	public RSChunk(RSWorld world, int chunkX, int chunkZ)
	{
		this.world = world;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;

		for(int z = 0; z < CHUNK_WIDTH; z++)
		for(int x = 0; x < CHUNK_WIDTH; x++)
		for(int y = 0; y < CHUNK_HEIGHT; y++)
		{
			short id;
			byte meta;

			if(y < CHUNK_HEIGHT >> 1) id = RSWorld.B_SOLID;
			else id = RSWorld.B_AIR;

			meta = 0;

			blockIDs[((z+(y<<CHUNK_WIDTH_BITS))<<CHUNK_WIDTH_BITS)+x] = id;
			blockMeta[((z+(y<<CHUNK_WIDTH_BITS))<<CHUNK_WIDTH_BITS)+x] = meta;

		}
	}

	private int getPos(int x, int y, int z)
	{
		assert(x >= 0 && x < CHUNK_WIDTH);
		assert(z >= 0 && z < CHUNK_WIDTH);
		assert(y >= 0 && y < CHUNK_HEIGHT);

		return ((z+(y<<CHUNK_WIDTH_BITS))<<CHUNK_WIDTH_BITS)+x;
	}

	public int getBlockType(int x, int y, int z)
	{
		int pos = getPos(x, y, z);

		return (int)blockIDs[pos];
	}

	public int getBlockMeta(int x, int y, int z)
	{
		int pos = getPos(x, y, z);

		return (int)blockMeta[pos];
	}

	public int getTopY(int x, int z)
	{
		int y;
		int pos = getPos(x, CHUNK_HEIGHT-1, z);

		for(y = CHUNK_HEIGHT-1; y >= 0; y--, pos -= (1<<(CHUNK_WIDTH_BITS<<1)))
		{
			if(blockIDs[pos] != RSWorld.B_AIR)
				return y;
		}

		return -1;
	}

	public void addRedSources(HashSet<RSPos> redTorches, HashSet<RSPos> redWires,
		HashSet<RSPos> redSources)
	{
		if(!this.dirtyNet)
			return;

		for(int pos = 0, y = 0; y < CHUNK_HEIGHT; y++)
		for(int z = 0; z < CHUNK_WIDTH; z++)
		for(int x = 0; x < CHUNK_WIDTH; x++, pos++)
		{
			int type = (int)blockIDs[pos];
			int meta = (int)blockMeta[pos];
			RSPos p;

			int rx = x + (this.chunkX << CHUNK_WIDTH_BITS);
			int ry = y;
			int rz = z + (this.chunkZ << CHUNK_WIDTH_BITS);

			switch(type)
			{
				case RSWorld.B_AIR:
					// Air is never a source
					break;

				case RSWorld.B_RWIRE:
					// Add this to the wires AND charges
					p = new RSPos(rx, ry, rz);
					redWires.add(p);
					break;

				case RSWorld.B_RTORCH:
					// Add this to the torches AND charges AND sources
					p = new RSPos(rx, ry, rz);
					redTorches.add(p);
					System.out.printf("torch %d %d %d\n", rx, ry, rz);
					if((meta & 0x40) == 0) redSources.add(p);
					break;

				case RSWorld.B_SOLID:
					// Add this to the charges AND sources
					p = new RSPos(rx, ry, rz);
					if((meta & 0x40) != 0) redSources.add(p);
					break;

			}
		}

		this.dirtyNet = false;
	}

	public boolean setBlock(int x, int y, int z, int id, int meta)
	{
		int pos = getPos(x, y, z);

		blockIDs[pos] = (short)id;
		blockMeta[pos] = (byte)meta;

		this.dirtyNet = true;

		return true;
	}

	public void setBlockCharge(int x, int y, int z, boolean charged)
	{
		int pos = getPos(x, y, z);

		if(charged)
			blockMeta[pos] |= 0x40;
		else
			blockMeta[pos] &= ~0x40;
	}
}


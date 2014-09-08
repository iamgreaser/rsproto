import java.io.*;
import java.util.*;

public class Main
{
	public static void main(String[] args) throws Exception
	{
		// Create world
		RSWorld world = new RSWorld();

		// Make redstone
		Scanner fp = new Scanner(new File("circuit.txt"));
		for(int z = 0; fp.hasNextLine(); z++)
		{
			String s = fp.nextLine();
			for(int x = 0; x < s.length(); x++)
			{
				int y = world.getTopY(x, z);
				char c = s.charAt(x);
				switch(c)
				{
					case '.':
						break;

					case '-':
						world.setBlock(x, y + 1, z, RSWorld.B_RWIRE, 0x00);
						break;

					case '#':
						world.setBlock(x, y + 1, z, RSWorld.B_SOLID, 0x00);
						break;

					case '@':
						world.setBlock(x, y + 1, z, RSWorld.B_SOLID, 0x00);
						world.setBlock(x, y + 2, z, RSWorld.B_RWIRE, 0x00);
						break;

					case '^':
						world.setBlock(x, y + 1, z, RSWorld.B_RTORCH, 0x00);
						break;
					case '<':
						world.setBlock(x, y + 1, z, RSWorld.B_RTORCH, 0x01);
						break;
					case 'V':
						world.setBlock(x, y + 1, z, RSWorld.B_RTORCH, 0x02);
						break;
					case '>':
						world.setBlock(x, y + 1, z, RSWorld.B_RTORCH, 0x03);
						break;

					default:
						throw new RuntimeException(
							String.format("char %c not supported", c));
				}
			}
		}
		fp.close();

		// Simulate
		while(true)
		{
			// Clear screen
			//System.out.printf("\033[1;1H\033[2J");
			System.out.println();

			// Draw world
			for(int z = 0; z < 16; z++)
			{
				String s = "";
				for(int x = 0; x < 64; x++)
				{
					char c = '?';
					int y = 128;
					int type = world.getBlockType(x, y, z);
					int meta = world.getBlockMeta(x, y, z);

					switch(type)
					{
						case RSWorld.B_AIR:
							c = ' ';
							break;
						case RSWorld.B_SOLID:
							c = '#';
							break;
						case RSWorld.B_RWIRE:
							c = ((meta & 0x40) != 0 ? '=' : '-');
							break;
						case RSWorld.B_RTORCH:
							c = ((meta & 0x40) != 0 ? '\'' : '*');
							break;
					}

					s += c;
				}

				System.out.println(s);
			}

			// Sleep a bit and then simulate
			Thread.sleep(300);
			world.tick();
		}

	}
}

